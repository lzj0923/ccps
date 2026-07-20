package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
        });
    }

    @Test
    void createRuleNormalizesCodeAndDeduplicatesChannels() {
        AdminReminderRuleRequest request = new AdminReminderRuleRequest(" rent_due_3d ", " 租金到期提醒 ",
                "rent_due", 3, List.of("email", "in_app", "email"), "owner", true);
        when(mapper.insertRule(any())).thenAnswer(invocation -> {
            RuleWrite write = invocation.getArgument(0); write.setId(11L); return 1;
        });
        when(mapper.findRule(11L)).thenReturn(rule(11L, "RENT_DUE_3D", "租金到期提醒", "rent_due",
                "[\"email\",\"in_app\"]", 3, true));

        AdminReminderResponse.Rule result = service.createRule(5L, request);

        ArgumentCaptor<RuleWrite> captor = ArgumentCaptor.forClass(RuleWrite.class);
        verify(mapper).insertRule(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("RENT_DUE_3D");
        assertThat(captor.getValue().getName()).isEqualTo("租金到期提醒");
        assertThat(captor.getValue().getChannelsJson()).isEqualTo("[\"email\",\"in_app\"]");
        assertThat(result.id()).isEqualTo(11L);
        verify(mapper).insertRuleAudit(5L, "create_reminder_rule", 11L, "RENT_DUE_3D", "租金到期提醒");
    }

    @Test
    void runningPaymentRuleCreatesNotificationAndChannelResults() {
        RuleRow rule = rule(7L, "PAYMENT_DUE_7D", "房款到期前七天", "payment_due",
                "[\"in_app\",\"email\"]", 7, true);
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
