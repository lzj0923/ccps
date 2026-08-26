package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.mapper.WhatsAppNotificationMapper;
import com.ccps.backend.mapper.WhatsAppNotificationMapper.NewAttempt;
import com.ccps.backend.mapper.WhatsAppNotificationMapper.WhatsAppDeliveryRow;
import com.ccps.backend.service.WhatsAppGraphClient.SendResult;

@ExtendWith(MockitoExtension.class)
class WhatsAppNotificationServiceTest {
    @Mock
    private WhatsAppNotificationMapper mapper;
    @Mock
    private WhatsAppGraphClient graphClient;

    private WhatsAppNotificationService service;

    @BeforeEach
    void setUp() {
        WhatsAppTemplateCatalog templates = new WhatsAppTemplateCatalog(
                "zh_CN", "ccps_rent_first", "ccps_rent_second", "ccps_rent_final", "ccps_rent_termination",
                "ccps_lease_expiry_business");
        service = new WhatsAppNotificationService(mapper, graphClient, templates, "60");
    }

    @Test
    void sendsApprovedTemplateAndStoresProviderMessageId() {
        WhatsAppDeliveryRow delivery = delivery();
        when(mapper.claim(5L)).thenReturn(1);
        doAnswer(invocation -> {
            NewAttempt attempt = invocation.getArgument(3);
            attempt.setId(9L);
            return 1;
        }).when(mapper).insertAttempt(eq(5L), eq("ccps_rent_first"), eq("zh_CN"), any());
        when(graphClient.sendTemplate(eq("60123456789"), eq("ccps_rent_first"), eq("zh_CN"), any()))
                .thenReturn(new SendResult("wamid.test", "60123456789"));

        service.deliver(delivery);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> parameters = ArgumentCaptor.forClass(List.class);
        verify(graphClient).sendTemplate(eq("60123456789"), eq("ccps_rent_first"), eq("zh_CN"),
                parameters.capture());
        assertThat(parameters.getValue()).containsExactly(
                "测试租客", "测试建案 A-01", "2026-07", "RM 800.00", "2026-07-27", "14", "第一次提醒");
        verify(mapper).markAttemptAccepted(9L, "wamid.test", "60123456789");
        verify(mapper).markDeliveryAccepted(5L);
    }

    @Test
    void sendsLeaseExpiryTemplateToResponsibleBusinessUser() {
        WhatsAppDeliveryRow delivery = new WhatsAppDeliveryRow();
        delivery.setDeliveryId(6L);
        delivery.setDestination("+60 12-987 6543");
        delivery.setStage("lease_expiry_business");
        delivery.setTenantName("业务人员");
        delivery.setProjectName("测试建案");
        delivery.setUnitNo("B-08");
        delivery.setLeaseNo("L-2026-008");
        delivery.setDueDate(LocalDate.parse("2026-09-25"));
        when(mapper.claim(6L)).thenReturn(1);
        doAnswer(invocation -> {
            NewAttempt attempt = invocation.getArgument(3);
            attempt.setId(10L);
            return 1;
        }).when(mapper).insertAttempt(eq(6L), eq("ccps_lease_expiry_business"), eq("zh_CN"), any());
        when(graphClient.sendTemplate(eq("60129876543"), eq("ccps_lease_expiry_business"), eq("zh_CN"), any()))
                .thenReturn(new SendResult("wamid.lease", "60129876543"));

        service.deliver(delivery);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> parameters = ArgumentCaptor.forClass(List.class);
        verify(graphClient).sendTemplate(eq("60129876543"), eq("ccps_lease_expiry_business"), eq("zh_CN"),
                parameters.capture());
        assertThat(parameters.getValue()).containsExactly(
                "业务人员", "测试建案 B-08", "L-2026-008", "2026-09-25");
        verify(mapper).markAttemptAccepted(10L, "wamid.lease", "60129876543");
        verify(mapper).markDeliveryAccepted(6L);
    }

    private WhatsAppDeliveryRow delivery() {
        WhatsAppDeliveryRow row = new WhatsAppDeliveryRow();
        row.setDeliveryId(5L);
        row.setDestination("+60 12-345 6789");
        row.setStage("first_reminder");
        row.setTenantName("测试租客");
        row.setProjectName("测试建案");
        row.setUnitNo("A-01");
        row.setBillingMonth(LocalDate.parse("2026-07-01"));
        row.setDueDate(LocalDate.parse("2026-07-27"));
        row.setOutstandingAmount(new BigDecimal("800"));
        row.setOverdueDays(14);
        return row;
    }
}
