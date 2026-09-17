package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.ccps.backend.mapper.WhatsAppNotificationMapper;
import com.ccps.backend.mapper.WhatsAppNotificationMapper.NewAttempt;
import com.ccps.backend.mapper.WhatsAppNotificationMapper.WhatsAppDeliveryRow;
import com.fasterxml.jackson.databind.ObjectMapper;

/** In-process service/HTTP/signature integration; no network, database or real credentials. */
class WhatsAppNotificationPipelineTest {
    private static final String PHONE_ID = "fixture-phone";
    private static final String DESTINATION = "8613800138000";
    private static final String SECRET = "fixture-webhook-secret";
    private final ObjectMapper json = new ObjectMapper();

    @ParameterizedTest
    @CsvSource({"first_reminder,第一次提醒", "second_reminder,第二次提醒",
            "final_reminder,最终提醒", "termination_notice,终止通知"})
    void rentStagesUseSharedSevenParameterTemplateAndApplySignedDeliveryReceipt(String stage, String label) throws Exception {
        verifyPipeline(stage, "ccps_rent_collection_notice", List.of(
                "测试收件人", "虚拟建案 A-01", "2026-09", "RM 800.00", "2026-09-01", "15", label));
    }

    @Test
    void leaseExpiryUsesFourParametersAndAppliesSignedDeliveryReceipt() throws Exception {
        verifyPipeline("lease_expiry_business", "ccps_lease_expiry_business_notice", List.of(
                "测试收件人", "虚拟建案 A-01", "TEST-NO-ACTION", "2026-09-01"));
    }

    private void verifyPipeline(String stage, String template, List<String> parameters) throws Exception {
        WhatsAppNotificationMapper mapper = mock(WhatsAppNotificationMapper.class);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        WhatsAppGraphClient client = new WhatsAppGraphClient(builder, true, "v26.0", PHONE_ID, "fixture-token", "");
        WhatsAppNotificationService service = service(mapper, client);
        WhatsAppDeliveryRow row = row(stage);
        when(mapper.claim(5L, row.getDestination())).thenReturn(1);
        doAnswer(invocation -> { ((NewAttempt) invocation.getArgument(3)).setId(9L); return 1; })
                .when(mapper).insertAttempt(eq(5L), eq(template), eq("zh_CN"), any());
        String expected = json.writeValueAsString(Map.of(
                "messaging_product", "whatsapp", "recipient_type", "individual", "to", DESTINATION, "type", "template",
                "template", Map.of("name", template, "language", Map.of("code", "zh_CN"),
                        "components", List.of(Map.of("type", "body", "parameters", parameters.stream()
                                .map(value -> Map.of("type", "text", "text", value)).toList())))));
        server.expect(requestTo("https://graph.facebook.com/v26.0/" + PHONE_ID + "/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer fixture-token"))
                .andExpect(content().json(expected, true))
                .andRespond(withSuccess("{\"contacts\":[{\"wa_id\":\"" + DESTINATION + "\"}],\"messages\":[{\"id\":\"wamid.fixture\"}]}", MediaType.APPLICATION_JSON));
        service.deliver(row);
        server.verify();
        verify(mapper).markAttemptAccepted(9L, "wamid.fixture", DESTINATION);
        verify(mapper).markDeliveryAccepted(5L);
        // Acceptance alone must not manufacture a delivered receipt.
        verify(mapper, never()).updateDeliveryStatus(anyString(), eq("delivered"), any(), any());

        when(mapper.updateDeliveryStatus(eq("wamid.fixture"), eq("delivered"), any(), isNull())).thenReturn(1);
        byte[] receipt = json.writeValueAsBytes(Map.of("object", "whatsapp_business_account", "entry", List.of(
                Map.of("id", "fixture-waba", "changes", List.of(Map.of("field", "messages", "value", Map.of(
                        "statuses", List.of(Map.of("id", "wamid.fixture", "status", "delivered", "timestamp", "1789549200")))))))));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        new WhatsAppWebhookService(json, mapper, "fixture-verify", SECRET)
                .receive(receipt, "sha256=" + HexFormat.of().formatHex(mac.doFinal(receipt)));
        verify(mapper).updateAttemptStatus(eq("wamid.fixture"), eq("delivered"), any(), isNull(), isNull());
        verify(mapper).updateDeliveryStatus(eq("wamid.fixture"), eq("delivered"), any(), isNull());
    }

    @Test
    void disabledSenderDoesNotScanQueues() {
        WhatsAppNotificationMapper mapper = mock(WhatsAppNotificationMapper.class);
        WhatsAppGraphClient client = new WhatsAppGraphClient(RestClient.builder(), false, "v26.0", PHONE_ID, "fixture-token", "");
        assertThat(client.configured()).isFalse();
        service(mapper, client).deliverPendingNotifications();
        verifyNoInteractions(mapper);
    }

    @Test
    void unclaimedNotificationNeverCallsGraphOrCreatesAttempt() {
        WhatsAppNotificationMapper mapper = mock(WhatsAppNotificationMapper.class);
        WhatsAppGraphClient client = mock(WhatsAppGraphClient.class);
        service(mapper, client).deliver(row("first_reminder"));
        verify(mapper).claim(5L, "+8613800138000");
        verify(mapper, never()).insertAttempt(anyLong(), anyString(), anyString(), any());
        verifyNoInteractions(client);
    }

    private WhatsAppNotificationService service(WhatsAppNotificationMapper mapper, WhatsAppGraphClient client) {
        return new WhatsAppNotificationService(mapper, client, new WhatsAppTemplateCatalog("zh_CN",
                "ccps_rent_collection_notice", "ccps_rent_collection_notice", "ccps_rent_collection_notice",
                "ccps_rent_collection_notice", "ccps_lease_expiry_business_notice"), "60");
    }

    private WhatsAppDeliveryRow row(String stage) {
        WhatsAppDeliveryRow row = new WhatsAppDeliveryRow();
        row.setDeliveryId(5L); row.setDestination("+" + DESTINATION); row.setStage(stage);
        row.setTenantName("测试收件人"); row.setProjectName("虚拟建案"); row.setUnitNo("A-01");
        row.setLeaseNo("TEST-NO-ACTION"); row.setBillingMonth(LocalDate.parse("2026-09-01"));
        row.setDueDate(LocalDate.parse("2026-09-01")); row.setOutstandingAmount(new BigDecimal("800"));
        row.setOverdueDays(15);
        return row;
    }
}
