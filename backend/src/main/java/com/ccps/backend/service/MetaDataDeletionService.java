package com.ccps.backend.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.ccps.backend.mapper.MetaDataDeletionMapper;
import com.ccps.backend.mapper.MetaDataDeletionMapper.RequestRow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MetaDataDeletionService {
    private final MetaDataDeletionMapper mapper;
    private final AdminAuditService audit;
    private final ObjectMapper json;
    private final String appSecret;
    private final String publicBase;
    private final boolean enabled;
    private final SecureRandom random = new SecureRandom();

    public MetaDataDeletionService(MetaDataDeletionMapper mapper, AdminAuditService audit, ObjectMapper json,
            @Value("${META_DATA_DELETION_ENABLED:false}") boolean enabled,
            @Value("${META_APP_SECRET:}") String appSecret,
            @Value("${META_DATA_DELETION_PUBLIC_BASE:}") String publicBase) {
        this.mapper = mapper;
        this.audit = audit;
        this.json = json;
        this.enabled = enabled;
        this.appSecret = appSecret == null ? "" : appSecret.trim();
        this.publicBase = publicBase == null ? "" : publicBase.trim().replaceAll("/+$", "");
    }

    @Transactional
    public Map<String, String> receive(String signedRequest) {
        requireConfigured();
        VerifiedRequest verified = verify(signedRequest);
        byte[] token = new byte[32];
        random.nextBytes(token);
        String candidateCode = HexFormat.of().formatHex(token);
        mapper.insertIfAbsent(verified.hash(), candidateCode, verified.userId());
        RequestRow request = mapper.byHash(verified.hash());
        if (request == null) throw new IllegalStateException("Deletion request was not persisted");
        if (candidateCode.equals(request.confirmationCode())) {
            audit.record(null, "meta_deletion_requested", "meta_data_deletion_request", request.id(),
                    null, "{\"status\":\"received\"}");
        }
        // Acknowledges durable RECEIPT only. No user ID is treated as a CCPS user ID.
        return Map.of("url", publicBase + "/meta/data-deletion/status/" + request.confirmationCode(),
                "confirmation_code", request.confirmationCode());
    }

    public String publicStatus(String code) {
        if (code == null || !code.matches("[a-f0-9]{64}")) throw notFound();
        RequestRow request = mapper.byCode(code);
        if (request == null) throw notFound();
        return switch (request.status()) {
            case "received" -> "Request received; awaiting review. 申请已收到，等待核验，尚未完成删除。";
            case "under_review" -> "Request under review. 申请正在核验处理中，尚未完成删除。";
            case "completed" -> "Review and applicable deletion processing completed. 已完成核验及适用资料的删除处理；依法必须保留的记录可能仍予保留。";
            case "rejected" -> "Request could not be fulfilled. 申请未能执行，请联系 CCPS 支持了解详情。";
            default -> "Request awaiting review. 申请等待核验。";
        };
    }

    public List<RequestRow> list(int limit, int offset) {
        return mapper.list(Math.max(1, Math.min(100, limit)), Math.max(0, offset));
    }

    @Transactional
    public void review(Long id, Long actor, String target, String note, boolean processingConfirmed) {
        if (actor == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (!Set.of("under_review", "completed", "rejected").contains(target == null ? "" : target)
                || note == null || note.isBlank() || note.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid status and a review note (1-2000 characters) are required");
        }
        RequestRow request = mapper.lockById(id);
        if (request == null) throw notFound();
        boolean allowed = ("received".equals(request.status()) && "under_review".equals(target))
                || ("under_review".equals(request.status()) && Set.of("completed", "rejected").contains(target));
        if (!allowed) throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid deletion review transition");
        if ("completed".equals(target) && !processingConfirmed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirm that the applicable data processing was actually performed before completing this request");
        }
        mapper.review(id, target, note.trim(), actor);
        // Notes remain private. They must describe identity mapping, actions and retained-data reasons.
        audit.record(actor, "meta_deletion_reviewed", "meta_data_deletion_request", id,
                encode(Map.of("status", request.status())), encode(Map.of("status", target, "note", note.trim())));
    }

    private void requireConfigured() {
        boolean validBase = false;
        try {
            URI uri = URI.create(publicBase);
            validBase = "https".equals(uri.getScheme()) && uri.getHost() != null
                    && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null
                    && (uri.getPath() == null || uri.getPath().isEmpty());
        } catch (IllegalArgumentException ignored) { }
        if (!enabled || appSecret.isBlank() || !validBase) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Meta deletion callback is not configured");
        }
    }

    private record VerifiedRequest(String userId, String hash) {}

    private VerifiedRequest verify(String signedRequest) {
        if (signedRequest == null || signedRequest.isBlank()) throw badRequest();
        if (signedRequest.length() > 16384) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        String[] parts = signedRequest.split("\\.", -1);
        if (parts.length != 2 || !parts[0].matches("[A-Za-z0-9_-]+={0,2}")
                || !parts[1].matches("[A-Za-z0-9_-]+={0,2}")) throw badRequest();
        byte[] supplied;
        try { supplied = Base64.getUrlDecoder().decode(parts[0]); }
        catch (IllegalArgumentException ex) { throw badRequest(); }
        byte[] expected = hmac(parts[1]);
        if (supplied.length != 32 || !MessageDigest.isEqual(expected, supplied)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Meta request signature");
        }
        try {
            JsonNode data = json.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (data == null || !data.isObject() || !"HMAC-SHA256".equals(data.path("algorithm").asText())
                    || !data.path("user_id").isTextual() || !data.path("user_id").asText().matches("[0-9]{1,64}")) {
                throw badRequest();
            }
            JsonNode issuedAt = data.get("issued_at");
            // Do not reject old signed deliveries: Meta may retry. The payload hash makes retries idempotent.
            if (issuedAt != null && (!issuedAt.isIntegralNumber() || !issuedAt.canConvertToLong()
                    || issuedAt.asLong() <= 0 || issuedAt.asLong() > Instant.now().getEpochSecond() + 300)) {
                throw badRequest();
            }
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(parts[1].getBytes(StandardCharsets.US_ASCII)));
            return new VerifiedRequest(data.path("user_id").asText(), hash);
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw badRequest(); }
    }

    private byte[] hmac(String encodedPayload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(encodedPayload.getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException ex) { throw new IllegalStateException("HMAC-SHA256 unavailable", ex); }
    }

    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception ex) { throw new IllegalStateException("Unable to create review audit", ex); }
    }

    private ResponseStatusException badRequest() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Meta signed_request");
    }
    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Deletion request not found");
    }
}
