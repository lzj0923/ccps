package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminReminderResponse;
import com.ccps.backend.dto.AdminReminderRuleRequest;
import com.ccps.backend.mapper.AdminReminderMapper;
import com.ccps.backend.mapper.AdminReminderMapper.EventContext;
import com.ccps.backend.mapper.AdminReminderMapper.NewNotification;
import com.ccps.backend.mapper.AdminReminderMapper.RuleRow;
import com.ccps.backend.mapper.AdminReminderMapper.RuleWrite;
import com.ccps.backend.mapper.AdminReminderMapper.SummaryRow;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AdminReminderServiceTest {
    @Mock private AdminReminderMapper mapper;
    private AdminReminderService service;

    @BeforeEach
    void setUp() {
        service = new AdminReminderService(mapper, new ObjectMapper());
    }

    @Test
    void overviewMapsDatabaseRulesAndSummary() {
        SummaryRow summary = new SummaryRow();
        summary.setRuleCount(2L); summary.setEnabledRuleCount(1L); summary.setNotificationCount(8L);
        summary.setPendingDeliveryCount(3L); summary.setFailedDeliveryCount(1L);
        RuleRow row = rule(7L, "PAYMENT_DUE_7D", "房款到期前七天", "payment_due", "[\"in_app\",\"email\"]", 7, true);
        when(mapper.findSummary()).thenReturn(summary);
        when(mapper.findRules()).thenReturn(List.of(row));
        when(mapper.findNotifications()).thenReturn(List.of());
        when(mapper.findDeliveries()).thenReturn(List.of());

        AdminReminderResponse result = service.overview();

        assertThat(result.summary().notificationCount()).isEqualTo(8);
        assertThat(result.rules()).singleElement().satisfies(rule -> {
            assertThat(rule.code()).isEqualTo("PAYMENT_DUE_7D");
            assertThat(rule.channels()).containsExactly("in_app", "email");
            assertThat(rule.daysBefore()).isEqualTo(7);
            assertThat(rule.systemManaged()).isFalse();
        });
    }

    @Test
    void createRuleNormalizesCodeAndDeduplicatesChannels() {
        AdminReminderRuleRequest request = new AdminReminderRuleRequest(" rent_due_3d ", " 租金到期提醒 ",
                "rent_due", 3, List.of("email", "whatsapp", "in_app", "email"), "owner", true);
        when(mapper.insertRule(any())).thenAnswer(invocation -> {
            RuleWrite write = invocation.getArgument(0); write.setId(11L); return 1;
        });
        when(mapper.findRule(11L)).thenReturn(rule(11L, "RENT_DUE_3D", "租金到期提醒", "rent_due",
                "[\"email\",\"whatsapp\",\"in_app\"]", 3, true));

        AdminReminderResponse.Rule result = service.createRule(5L, request);

        ArgumentCaptor<RuleWrite> captor = ArgumentCaptor.forClass(RuleWrite.class);
        verify(mapper).insertRule(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("RENT_DUE_3D");
        assertThat(captor.getValue().getName()).isEqualTo("租金到期提醒");
        assertThat(captor.getValue().getChannelsJson()).isEqualTo("[\"email\",\"whatsapp\",\"in_app\"]");
        assertThat(result.id()).isEqualTo(11L);
        verify(mapper).insertRuleAudit(5L, "create_reminder_rule", 11L, "RENT_DUE_3D", "租金到期提醒");
    }

    @Test
    void runningPaymentRuleCreatesNotificationAndChannelResults() {
        RuleRow rule = rule(7L, "PAYMENT_DUE_7D", "房款到期前七天", "payment_due",
                "[\"in_app\",\"email\",\"whatsapp\"]", 7, true);
        EventContext event = new EventContext();
        event.setRelatedId(99L); event.setRelatedType("payment_installment"); event.setRecipientOwnerId(4L);
        event.setRecipientName("Owner"); event.setRecipientEmail("owner@example.com");
        event.setProjectName("Meridian Park"); event.setUnitNo("B-12-05"); event.setLabel("建築結構款");
        event.setDueDate(LocalDate.now().plusDays(2)); event.setAmount(new BigDecimal("276000.00"));
        when(mapper.findRule(7L)).thenReturn(rule);
        when(mapper.findPaymentDueEvents(7L, 7)).thenReturn(List.of(event));
        when(mapper.insertNotification(any())).thenAnswer(invocation -> {
            NewNotification notification = invocation.getArgument(0); notification.setId(123L); return 1;
        });

        int created = service.runRule(5L, 7L);

        assertThat(created).isEqualTo(1);
        ArgumentCaptor<NewNotification> captor = ArgumentCaptor.forClass(NewNotification.class);
        verify(mapper).insertNotification(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("房款即將到期");
        assertThat(captor.getValue().getBody()).contains("Meridian Park B-12-05", "RM 276000.00");
        verify(mapper).insertInAppDelivery(123L);
        verify(mapper).insertUnavailableEmailDelivery(123L, "owner@example.com");
        verify(mapper).insertUnavailableWhatsAppDelivery(123L);
    }

    @Test
    void runningOverdueRentRuleQueuesWhatsAppForTheOptedInTenant() {
        RuleRow rule = rule(8L, "RENT_OVERDUE_WHATSAPP", "WhatsApp租金逾期提醒", "rent_due",
                "[\"whatsapp\"]", 0, true);
        rule.setRecipientRole("tenant");
        EventContext event = new EventContext();
        event.setRelatedId(101L); event.setRelatedType("rent_invoice");
        event.setRecipientUserId(31L); event.setTenantId(21L);
        event.setRecipientName("测试租客"); event.setRecipientEmail("tenant@example.com");
        event.setProjectName("测试建案"); event.setUnitNo("A-01"); event.setLabel("2026-08");
        event.setDueDate(LocalDate.now().minusDays(9)); event.setAmount(new BigDecimal("800.00"));
        event.setWhatsappEnabled(true); event.setWhatsappDestination("60123456789");
        when(mapper.findRule(8L)).thenReturn(rule);
        when(mapper.findRentDueEvents(8L, 0)).thenReturn(List.of(event));
        when(mapper.insertNotification(any())).thenAnswer(invocation -> {
            NewNotification notification = invocation.getArgument(0); notification.setId(124L); return 1;
        });
        when(mapper.insertWhatsAppDelivery(124L, 21L)).thenReturn(1);

        int created = service.runRule(5L, 8L);

        assertThat(created).isEqualTo(1);
        ArgumentCaptor<NewNotification> captor = ArgumentCaptor.forClass(NewNotification.class);
        verify(mapper).insertNotification(captor.capture());
        assertThat(captor.getValue().getRecipientUserId()).isEqualTo(31L);
        assertThat(captor.getValue().getRecipientOwnerId()).isNull();
        assertThat(captor.getValue().getTitle()).isEqualTo("租金逾期提醒");
        assertThat(captor.getValue().getBody()).contains("RM 800.00", "已逾期 9 天");
        verify(mapper).insertWhatsAppDelivery(124L, 21L);
    }

    @Test
    void scheduledLeaseExpiryRuleQueuesWhatsAppForResponsibleBusinessUser() {
        RuleRow rule = rule(9L, "LEASE_EXPIRY_BUSINESS_30D", "租约结束前一个月通知业务人员",
                "lease_expiry", "[\"whatsapp\"]", 30, true);
        rule.setRecipientRole("business");
        EventContext event = new EventContext();
        event.setRelatedId(102L); event.setRelatedType("lease");
        event.setRecipientUserId(41L); event.setRecipientName("业务人员");
        event.setProjectName("测试建案"); event.setUnitNo("B-08"); event.setLabel("L-2026-008");
        event.setDueDate(LocalDate.now().plusDays(30));
        event.setWhatsappEnabled(true); event.setWhatsappDestination("60129876543");
        when(mapper.findRules()).thenReturn(List.of(rule));
        when(mapper.findLeaseExpiryEvents(9L, 30)).thenReturn(List.of(event));
        when(mapper.insertNotification(any())).thenAnswer(invocation -> {
            NewNotification notification = invocation.getArgument(0); notification.setId(125L); return 1;
        });
        when(mapper.insertDirectWhatsAppDelivery(125L, "60129876543")).thenReturn(1);

        service.runScheduledRules();

        ArgumentCaptor<NewNotification> captor = ArgumentCaptor.forClass(NewNotification.class);
        verify(mapper).insertNotification(captor.capture());
        assertThat(captor.getValue().getRecipientUserId()).isEqualTo(41L);
        assertThat(captor.getValue().getRecipientOwnerId()).isNull();
        assertThat(captor.getValue().getTitle()).isEqualTo("租約即將到期");
        assertThat(captor.getValue().getBody()).contains("测试建案 B-08", "L-2026-008");
        verify(mapper).insertDirectWhatsAppDelivery(eq(125L), eq("60129876543"));
    }

    @Test
    void retryLeaseExpiryWhatsAppUsesCurrentResponsibleUserPhone() {
        when(mapper.findDeliveryChannel(301L)).thenReturn("whatsapp");
        when(mapper.retryWhatsAppDelivery(301L)).thenReturn(0);
        when(mapper.retryLeaseExpiryWhatsAppDelivery(301L)).thenReturn(1);

        service.retryDelivery(301L);

        verify(mapper).retryLeaseExpiryWhatsAppDelivery(301L);
    }

    @Test
    void scheduledRunAutomaticallyProcessesSystemLeaseExpiryRule() {
        RuleRow rule = rule(9L, "LEASE_EXPIRY_BUSINESS_30D", "租约结束前一个月通知业务人员",
                "lease_expiry", "[\"whatsapp\"]", 30, true);
        rule.setRecipientRole("business");
        EventContext event = new EventContext();
        event.setRelatedId(102L); event.setRelatedType("lease");
        event.setRecipientUserId(41L); event.setRecipientName("业务人员");
        event.setProjectName("测试建案"); event.setUnitNo("B-08"); event.setLabel("L-2026-008");
        event.setDueDate(LocalDate.now().plusDays(30));
        event.setWhatsappEnabled(true); event.setWhatsappDestination("60129876543");
        when(mapper.findRules()).thenReturn(List.of(rule));
        when(mapper.findLeaseExpiryEvents(9L, 30)).thenReturn(List.of(event));
        when(mapper.insertNotification(any())).thenAnswer(invocation -> {
            NewNotification notification = invocation.getArgument(0); notification.setId(125L); return 1;
        });
        when(mapper.insertDirectWhatsAppDelivery(125L, "60129876543")).thenReturn(1);

        service.runScheduledRules();

        verify(mapper).findLeaseExpiryEvents(9L, 30);
        verify(mapper).insertDirectWhatsAppDelivery(125L, "60129876543");
    }

    @Test
    void systemLeaseExpiryRuleCannotBeRunOrMaintainedManually() {
        RuleRow rule = rule(9L, "LEASE_EXPIRY_BUSINESS_30D", "租约结束前一个月通知业务人员",
                "lease_expiry", "[\"whatsapp\"]", 30, true);
        when(mapper.findRule(9L)).thenReturn(rule);

        assertSystemRuleConflict(() -> service.runRule(5L, 9L));
        assertSystemRuleConflict(() -> service.setEnabled(5L, 9L, false));
        assertSystemRuleConflict(() -> service.deleteRule(5L, 9L));
        assertSystemRuleConflict(() -> service.updateRule(5L, 9L,
                new AdminReminderRuleRequest("LEASE_EXPIRY_BUSINESS_30D", "changed",
                        "lease_expiry", 10, List.of("email"), "business", false)));
    }

    @Test
    void manualRunAllSkipsSystemAutomaticRule() {
        RuleRow rule = rule(9L, "LEASE_EXPIRY_BUSINESS_30D", "租约结束前一个月通知业务人员",
                "lease_expiry", "[\"whatsapp\"]", 30, true);
        when(mapper.findRules()).thenReturn(List.of(rule));

        assertThat(service.runEnabledRules(5L)).isZero();

        verify(mapper, never()).findLeaseExpiryEvents(9L, 30);
        verify(mapper).insertRuleAudit(5L, "run_all_reminder_rules", 0L, "ALL", "全部啟用規則");
    }

    @Test
    void overviewMarksLeaseExpiryRuleAsSystemManaged() {
        RuleRow rule = rule(9L, "LEASE_EXPIRY_BUSINESS_30D", "租约结束前一个月通知业务人员",
                "lease_expiry", "[\"whatsapp\"]", 30, true);
        when(mapper.findSummary()).thenReturn(new SummaryRow());
        when(mapper.findRules()).thenReturn(List.of(rule));
        when(mapper.findNotifications()).thenReturn(List.of());
        when(mapper.findDeliveries()).thenReturn(List.of());

        assertThat(service.overview().rules()).singleElement()
                .extracting(AdminReminderResponse.Rule::systemManaged)
                .isEqualTo(true);
    }

    private void assertSystemRuleConflict(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    private RuleRow rule(Long id, String code, String name, String eventType, String channels,
                         int daysBefore, boolean enabled) {
        RuleRow row = new RuleRow();
        row.setId(id); row.setCode(code); row.setName(name); row.setEventType(eventType); row.setChannelsJson(channels);
        row.setDaysBefore(daysBefore); row.setRecipientRole("owner"); row.setEnabled(enabled);
        row.setCreatedAt(LocalDateTime.now()); row.setUpdatedAt(LocalDateTime.now());
        return row;
    }
}
