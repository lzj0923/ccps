package com.ccps.backend.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ccps.backend.mapper.WhatsAppNotificationMapper;

@Service
public class WhatsAppWebhookService {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookService.class);
    private static final String SUBSCRIBE_MODE = "subscribe";
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final Set<String> DELIVERY_STATUSES = Set.of("sent", "delivered", "read", "failed");

    private final ObjectMapper objectMapper;
    private final WhatsAppNotificationMapper notificationMapper;
    private final String verifyToken;
    private final String appSecret;

    @Autowired
    public WhatsAppWebhookService(
            ObjectMapper objectMapper,
            WhatsAppNotificationMapper notificationMapper,
            @Value("${ccps.whatsapp.webhook.verify-token:}") String verifyToken,
            @Value("${ccps.whatsapp.webhook.app-secret:}") String appSecret) {
        this.objectMapper = objectMapper;
        this.notificationMapper = notificationMapper;
        this.verifyToken = normalize(verifyToken);
        this.appSecret = normalize(appSecret);
        if (this.appSecret.isBlank()) {
            log.warn("WhatsApp webhook signature verification is disabled because META_APP_SECRET is not configured");
        }
    }

    public WhatsAppWebhookService(ObjectMapper objectMapper, String verifyToken, String appSecret) {
        this(objectMapper, null, verifyToken, appSecret);
    }

    public String verifySubscription(String mode, String requestToken, String challenge) {
        if (verifyToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "WhatsApp webhook verify token is not configured");
        }
        if (!SUBSCRIBE_MODE.equals(mode) || !secureEquals(verifyToken, normalize(requestToken))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Webhook verification failed");
        }
        if (challenge == null || challenge.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook challenge is missing");
        }
        return challenge;
    }

    public void receive(byte[] payload, String signatureHeader) {
        byte[] requestBody = payload == null ? new byte[0] : payload;
        verifySignature(requestBody, signatureHeader);

        try {
            JsonNode root = objectMapper.readTree(requestBody);
            if (root == null || !root.isObject()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook payload must be a JSON object");
            }
            int entryCount = root.path("entry").isArray() ? root.path("entry").size() : 0;
            applyDeliveryStatuses(root);
            log.info("Received WhatsApp webhook object={}, entries={}", root.path("object").asText("unknown"), entryCount);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Webhook payload is not valid JSON");
        }
    }

    private void applyDeliveryStatuses(JsonNode root) {
        if (notificationMapper == null || !root.path("entry").isArray()) return;
        for (JsonNode entry : root.path("entry")) {
            if (!entry.path("changes").isArray()) continue;
            for (JsonNode change : entry.path("changes")) {
                JsonNode statuses = change.path("value").path("statuses");
                if (!statuses.isArray()) continue;
                for (JsonNode providerStatus : statuses) applyDeliveryStatus(providerStatus);
            }
        }
    }

    private void applyDeliveryStatus(JsonNode providerStatus) {
        String messageId = providerStatus.path("id").asText("");
        String status = providerStatus.path("status").asText("");
        if (messageId.isBlank() || !DELIVERY_STATUSES.contains(status)) return;

        LocalDateTime statusAt = timestamp(providerStatus.path("timestamp").asText(""));
        JsonNode error = providerStatus.path("errors").path(0);
        Integer errorCode = error.has("code") ? error.path("code").asInt() : null;
        String errorDetails = providerError(error);
        notificationMapper.updateAttemptStatus(messageId, status, statusAt, errorCode, errorDetails);
        int updated = notificationMapper.updateDeliveryStatus(messageId, status, statusAt, errorDetails);
        if (updated == 0) log.warn("Received WhatsApp status for an unknown provider message id");
    }

    private LocalDateTime timestamp(String epochSeconds) {
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(epochSeconds)), ZoneId.systemDefault());
        } catch (RuntimeException exception) {
            return LocalDateTime.now();
        }
    }

    private String providerError(JsonNode error) {
        if (error == null || error.isMissingNode()) return null;
        String details = error.path("error_data").path("details").asText("");
        if (details.isBlank()) details = error.path("title").asText("");
        if (details.isBlank()) details = error.path("message").asText("");
        if (details.isBlank()) return null;
        details = details.replaceAll("[\\r\\n]+", " ").trim();
        return details.length() <= 500 ? details : details.substring(0, 500);
    }

    private void verifySignature(byte[] payload, String signatureHeader) {
        if (appSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "WhatsApp webhook app secret is not configured");
        }
        if (signatureHeader == null
                || !signatureHeader.startsWith(SIGNATURE_PREFIX)
                || signatureHeader.length() != SIGNATURE_PREFIX.length() + 64) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook signature is missing or invalid");
        }

        byte[] suppliedSignature;
        try {
            suppliedSignature = HexFormat.of().parseHex(signatureHeader.substring(SIGNATURE_PREFIX.length()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook signature is missing or invalid");
        }

        byte[] expectedSignature = hmacSha256(payload, appSecret);
        if (!MessageDigest.isEqual(expectedSignature, suppliedSignature)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook signature is missing or invalid");
        }
    }

    private byte[] hmacSha256(byte[] payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is unavailable", exception);
        }
    }

    private boolean secureEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
