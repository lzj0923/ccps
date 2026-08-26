package com.ccps.backend.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ccps.backend.exception.GlobalExceptionHandler;
import com.ccps.backend.service.WhatsAppWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;

class WhatsAppWebhookControllerTest {
    private static final String VERIFY_TOKEN = "local-meta-verify-token";
    private static final String APP_SECRET = "meta-app-secret";

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        WhatsAppWebhookService service = new WhatsAppWebhookService(new ObjectMapper(), VERIFY_TOKEN, APP_SECRET);
        mvc = MockMvcBuilders.standaloneSetup(new WhatsAppWebhookController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsChallengeWhenMetaVerificationTokenMatches() throws Exception {
        mvc.perform(get("/api/webhooks/whatsapp")
                        .queryParam("hub.mode", "subscribe")
                        .queryParam("hub.verify_token", VERIFY_TOKEN)
                        .queryParam("hub.challenge", "123456789"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("123456789"));
    }

    @Test
    void rejectsVerificationWhenTokenDoesNotMatch() throws Exception {
        mvc.perform(get("/api/webhooks/whatsapp")
                        .queryParam("hub.mode", "subscribe")
                        .queryParam("hub.verify_token", "wrong-token")
                        .queryParam("hub.challenge", "123456789"))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptsSignedWebhookPayload() throws Exception {
        byte[] payload = ("{\"object\":\"whatsapp_business_account\",\"entry\":[],"
                + "\"note\":\"中文通知\"}").getBytes(StandardCharsets.UTF_8);

        mvc.perform(post("/api/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", signature(payload))
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsWebhookPayloadWithInvalidSignature() throws Exception {
        mvc.perform(post("/api/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", "sha256=00")
                        .content("{\"object\":\"whatsapp_business_account\",\"entry\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWebhookEventsUntilMetaAppSecretIsConfigured() throws Exception {
        WhatsAppWebhookService service = new WhatsAppWebhookService(new ObjectMapper(), VERIFY_TOKEN, "");
        MockMvc unconfiguredMvc = MockMvcBuilders.standaloneSetup(new WhatsAppWebhookController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        unconfiguredMvc.perform(post("/api/webhooks/whatsapp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"object\":\"whatsapp_business_account\",\"entry\":[]}"))
                .andExpect(status().isServiceUnavailable());
    }

    private String signature(byte[] payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));
    }
}
