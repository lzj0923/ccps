package com.ccps.backend.service;

import java.net.InetSocketAddress;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;

import okhttp3.OkHttpClient;

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
        ProxySettings proxySettings = proxySettings(proxyUrl);
        if (proxySettings.proxy() != Proxy.NO_PROXY) {
            if (proxySettings.proxy().type() == Proxy.Type.SOCKS) {
                // OkHttp keeps the target unresolved for SOCKS routes, so the proxy
                // performs DNS resolution. This is required for mainland hosts where
                // resolving graph.facebook.com locally can produce an unusable route.
                OkHttpClient okHttpClient = new OkHttpClient.Builder()
                        .proxy(proxySettings.proxy())
                        .build();
                clientBuilder.requestFactory(new OkHttp3ClientHttpRequestFactory(okHttpClient));
            } else {
                SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                requestFactory.setProxy(proxySettings.proxy());
                clientBuilder.requestFactory(requestFactory);
            }
            installProxyAuthenticator(proxySettings);
        }
        this.restClient = clientBuilder.build();
        this.enabled = enabled;
        this.graphApiVersion = normalize(graphApiVersion);
        this.phoneNumberId = normalize(phoneNumberId);
        this.accessToken = normalize(accessToken);
    }

    static Proxy proxy(String value) {
        return proxySettings(value).proxy();
    }

    static ProxySettings proxySettings(String value) {
        if (value == null || value.isBlank()) return ProxySettings.direct();
        try {
            URI uri = URI.create(value.trim());
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("Proxy host is missing");
            }
            String scheme = uri.getScheme() == null ? "http" : uri.getScheme().toLowerCase();
            Proxy.Type type = switch (scheme) {
                case "http", "https" -> Proxy.Type.HTTP;
                case "socks", "socks5", "socks5h" -> Proxy.Type.SOCKS;
                default -> throw new IllegalArgumentException("Unsupported proxy scheme: " + scheme);
            };
            int port = uri.getPort();
            if (port < 1) port = switch (scheme) {
                case "https" -> 443;
                case "socks", "socks5", "socks5h" -> 1080;
                default -> 80;
            };
            String username = "";
            String password = "";
            String userInfo = uri.getUserInfo();
            if (userInfo != null && !userInfo.isBlank()) {
                int separator = userInfo.indexOf(':');
                username = separator < 0 ? userInfo : userInfo.substring(0, separator);
                password = separator < 0 ? "" : userInfo.substring(separator + 1);
            }
            return new ProxySettings(new Proxy(type, new InetSocketAddress(uri.getHost(), port)),
                    uri.getHost(), port, username, password);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid WhatsApp proxy URL", exception);
        }
    }

    private static void installProxyAuthenticator(ProxySettings settings) {
        if (settings.username().isBlank()) return;
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                boolean proxyAuthentication = getRequestorType() == RequestorType.PROXY;
                boolean socksAuthentication = getRequestorType() == RequestorType.SERVER
                        && getRequestingProtocol() != null
                        && getRequestingProtocol().toUpperCase().startsWith("SOCKS");
                if ((proxyAuthentication || socksAuthentication)
                        && settings.host().equalsIgnoreCase(getRequestingHost())
                        && settings.port() == getRequestingPort()) {
                    return new PasswordAuthentication(settings.username(), settings.password().toCharArray());
                }
                return null;
            }
        });
    }

    record ProxySettings(Proxy proxy, String host, int port, String username, String password) {
        static ProxySettings direct() {
            return new ProxySettings(Proxy.NO_PROXY, "", -1, "", "");
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
