package com.ccps.backend.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.mapper.WhatsAppNotificationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookServiceTest {
    private static final String SECRET = "meta-app-secret";

    @Mock
    private WhatsAppNotificationMapper mapper;

    @Test
    void appliesDeliveredStatusToAttemptAndDelivery() throws Exception {
        byte[] payload = ("""
                {"object":"whatsapp_business_account","entry":[{"changes":[{"value":{"statuses":[{
                  "id":"wamid.test","status":"delivered","timestamp":"1787364000"
                }]}}]}]}
                """).getBytes(StandardCharsets.UTF_8);
        when(mapper.updateDeliveryStatus(eq("wamid.test"), eq("delivered"),
                org.mockito.ArgumentMatchers.any(), eq(null))).thenReturn(1);
        WhatsAppWebhookService service = new WhatsAppWebhookService(
                new ObjectMapper(), mapper, "verify-token", SECRET);

        service.receive(payload, signature(payload));

        ArgumentCaptor<LocalDateTime> timestamp = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).updateAttemptStatus(eq("wamid.test"), eq("delivered"), timestamp.capture(),
                eq(null), eq(null));
        verify(mapper).updateDeliveryStatus(eq("wamid.test"), eq("delivered"),
                eq(timestamp.getValue()), eq(null));
        LocalDateTime expected = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(1787364000L), ZoneId.systemDefault());
        org.assertj.core.api.Assertions.assertThat(timestamp.getValue()).isEqualTo(expected);
    }

    private String signature(byte[] payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload));
    }
}
