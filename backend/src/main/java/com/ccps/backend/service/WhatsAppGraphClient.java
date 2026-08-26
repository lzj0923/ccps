package com.ccps.backend.service;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class WhatsAppGraphClient {
    private final RestClient restClient;
    private final boolean enabled;
    private final String graphApiVersion;
    private final String phoneNumberId;
    private final String accessToken;

    public WhatsAppGraphClient(
            RestClient.Builder builder,
            @Value("${ccps.whatsapp.enabled:false}") boolean enabled,
            @Value("${ccps.whatsapp.graph-api-version:v26.0}") String graphApiVersion,
            @Value("${ccps.whatsapp.phone-number-id:}") String phoneNumberId,
            @Value("${ccps.whatsapp.access-token:}") String accessToken,
            @Value("${ccps.whatsapp.proxy-url:}") String proxyUrl) {
        RestClient.Builder clientBuilder = builder.baseUrl("https://graph.facebook.com");
        Proxy proxy = proxy(proxyUrl);
        if (proxy != Proxy.NO_PROXY) {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setProxy(proxy);
            clientBuilder.requestFactory(requestFactory);
        }
        this.restClient = clientBuilder.build();
        this.enabled = enabled;
        this.graphApiVersion = normalize(graphApiVersion);
        this.phoneNumberId = normalize(phoneNumberId);
        this.accessToken = normalize(accessToken);
    }

    static Proxy proxy(String value) {
        if (value == null || value.isBlank()) return Proxy.NO_PROXY;
        try {
            URI uri = URI.create(value.trim());
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("Proxy host is missing");
            }
            int port = uri.getPort();
            if (port < 1) port = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
            return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(uri.getHost(), port));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid WhatsApp proxy URL", exception);
        }
    }

    public boolean configured() {
        return enabled && !graphApiVersion.isBlank() && !phoneNumberId.isBlank() && !accessToken.isBlank();
    }

    public SendResult sendTemplate(String destination, String templateName, String language,
                                   List<String> bodyParameters) {
        if (!configured()) throw new WhatsAppGraphException("WhatsApp sending is not configured", null, null, null, false);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messaging_product", "whatsapp");
        body.put("recipient_type", "individual");
        body.put("to", destination);
        body.put("type", "template");
        body.put("template", Map.of(
                "name", templateName,
                "language", Map.of("code", language),
                "components", List.of(Map.of(
                        "type", "body",
                        "parameters", bodyParameters.stream()
                                .map(value -> Map.of("type", "text", "text", value == null ? "" : value))
                                .toList()))));
        try {
            JsonNode response = restClient.post()
                    .uri("/{version}/{phoneNumberId}/messages", graphApiVersion, phoneNumberId)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String messageId = response == null ? "" : response.path("messages").path(0).path("id").asText("");
            String waId = response == null ? "" : response.path("contacts").path(0).path("wa_id").asText("");
            if (messageId.isBlank()) {
                throw new WhatsAppGraphException("Meta accepted the request without returning a message id",
                        null, null, null, false);
            }
            return new SendResult(messageId, waId);
        } catch (RestClientResponseException exception) {
            throw graphError(exception);
        } catch (WhatsAppGraphException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new WhatsAppGraphException("WhatsApp request outcome is unknown: " + safe(exception.getMessage()),
                    null, null, null, true);
        }
    }

    private WhatsAppGraphException graphError(RestClientResponseException exception) {
        Integer code = null;
        Integer subcode = null;
        String traceId = null;
        String message = "Meta Graph API returned HTTP " + exception.getStatusCode().value();
        try {
            JsonNode error = exception.getResponseBodyAs(JsonNode.class).path("error");
            if (error.has("code")) code = error.path("code").asInt();
            if (error.has("error_subcode")) subcode = error.path("error_subcode").asInt();
            traceId = error.path("fbtrace_id").asText(null);
            String details = error.path("error_data").path("details").asText("");
            String providerMessage = error.path("message").asText("");
            if (!details.isBlank()) message += ": " + details;
            else if (!providerMessage.isBlank()) message += ": " + providerMessage;
        } catch (RuntimeException ignored) {
            // Keep only the HTTP status when Meta does not return its normal error schema.
        }
        // An HTTP response from Meta is a known rejection. Only transport errors are
        // marked uncertain because Meta may have accepted them before the connection failed.
        return new WhatsAppGraphException(safe(message), code, subcode, traceId, false);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        String result = value == null ? "Unknown error" : value.replaceAll("[\\r\\n]+", " ").trim();
        return result.length() <= 500 ? result : result.substring(0, 500);
    }

    public record SendResult(String messageId, String waId) {
    }

    public static class WhatsAppGraphException extends RuntimeException {
        private final Integer code;
        private final Integer subcode;
        private final String traceId;
        private final boolean uncertain;

        WhatsAppGraphException(String message, Integer code, Integer subcode, String traceId, boolean uncertain) {
            super(message);
            this.code = code;
            this.subcode = subcode;
            this.traceId = traceId;
            this.uncertain = uncertain;
        }

        public Integer code() { return code; }
        public Integer subcode() { return subcode; }
        public String traceId() { return traceId; }
        public boolean uncertain() { return uncertain; }
    }
}
