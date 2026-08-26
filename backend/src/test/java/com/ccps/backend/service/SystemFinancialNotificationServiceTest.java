package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.mapper.SystemFinancialNotificationMapper;
import com.ccps.backend.mapper.SystemFinancialNotificationMapper.BuildingPaymentRow;
import com.ccps.backend.mapper.SystemFinancialNotificationMapper.NewNotification;
import com.ccps.backend.mapper.SystemFinancialNotificationMapper.ReserveRow;

@ExtendWith(MockitoExtension.class)
class SystemFinancialNotificationServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 10);

    @Mock private SystemFinancialNotificationMapper mapper;
    private SystemFinancialNotificationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-10T02:00:00Z"), ZoneId.of("Asia/Shanghai"));
        service = new SystemFinancialNotificationService(mapper, clock);
    }

    @Test
    void selectsOnlyCurrentBuildingPaymentStage() {
        BuildingPaymentRow upcoming = building(11L, TODAY.plusDays(7), null);
        BuildingPaymentRow overdue = building(12L, TODAY.minusDays(20), null);
        BuildingPaymentRow alreadySent = building(13L, TODAY, "due_today");
        when(mapper.findBuildingPaymentsDue(TODAY)).thenReturn(List.of(upcoming, overdue, alreadySent));

        assertThat(service.dueBuildingTasks())
                .containsExactly(
                        new SystemFinancialNotificationService.BuildingTask(11L, "due_7d"),
                        new SystemFinancialNotificationService.BuildingTask(12L, "overdue_1d"));
    }

    @Test
    void sendsBuildingNoticeAndRecordsAuditHistory() {
        BuildingPaymentRow row = building(21L, TODAY, null);
        when(mapper.lockBuildingPayment(21L)).thenReturn(row);
        when(mapper.countSentAction("building_payment", 21L, "due_today", "2026-08-10")).thenReturn(0);
        when(mapper.insertNotification(any())).thenAnswer(invocation -> {
            NewNotification notification = invocation.getArgument(0);
            notification.setId(101L);
            return 1;
        });
        when(mapper.insertAction(eq("building_payment"), eq(21L), eq("due_today"), eq("2026-08-10"),
                eq(TODAY), eq(101L), eq("房款今日到期"), any())).thenReturn(1);

        service.sendBuildingNotice(21L, "due_today");

        verify(mapper).insertInAppDelivery(101L);
        verify(mapper).insertEmailDelivery(101L, "owner@example.com");
        verify(mapper).insertAudit("payment_installment", 21L, "due_today", "2026-08-10", "房款今日到期");
    }

    @Test
    void reserveNoticeRepeatsOnlyAfterSevenDays() {
        ReserveRow recent = reserve(31L, LocalDateTime.of(2026, 8, 5, 9, 0));
        ReserveRow due = reserve(32L, LocalDateTime.of(2026, 8, 3, 9, 0));
        ReserveRow first = reserve(33L, null);
        when(mapper.findLowReserves()).thenReturn(List.of(recent, due, first));

        assertThat(service.dueReserveAccounts()).containsExactly(32L, 33L);
    }

    @Test
    void doesNotQueueEmailWhenOwnerHasNoEmail() {
        ReserveRow row = reserve(41L, null);
        row.setRecipientEmail(null);
        when(mapper.lockReserve(41L)).thenReturn(row);
        when(mapper.countSentAction("reserve", 41L, "reserve_low", "2026-08-10")).thenReturn(0);
        when(mapper.insertNotification(any())).thenAnswer(invocation -> {
            NewNotification notification = invocation.getArgument(0);
            notification.setId(202L);
            return 1;
        });
        when(mapper.insertAction(eq("reserve"), eq(41L), eq("reserve_low"), eq("2026-08-10"),
                eq(TODAY), eq(202L), eq("预备金低于最低标准"), any())).thenReturn(1);

        service.sendReserveNotice(41L);

        verify(mapper).insertInAppDelivery(202L);
        verify(mapper, never()).insertEmailDelivery(any(), any());
        verify(mapper).insertAudit("reserve_account", 41L, "reserve_low", "2026-08-10", "预备金低于最低标准");
    }

    private BuildingPaymentRow building(Long id, LocalDate dueDate, String sentStages) {
        BuildingPaymentRow row = new BuildingPaymentRow();
        row.setRelatedId(id); row.setDueDate(dueDate); row.setSentStages(sentStages);
        row.setAmountDue(new BigDecimal("1200.00")); row.setAmountPaid(new BigDecimal("200.00"));
        row.setRecipientUserId(5L); row.setRecipientOwnerId(6L); row.setRecipientName("测试业主");
        row.setRecipientEmail("OWNER@EXAMPLE.COM"); row.setProjectName("测试建案");
        row.setUnitNo("A-01"); row.setLabel("第 1 期");
        return row;
    }

    private ReserveRow reserve(Long id, LocalDateTime lastSentAt) {
        ReserveRow row = new ReserveRow();
        row.setRelatedId(id); row.setLastSentAt(lastSentAt); row.setLowBalanceAlertEnabled(true);
        row.setCurrentBalance(new BigDecimal("100.00")); row.setMinimumBalance(new BigDecimal("500.00"));
        row.setRecipientUserId(5L); row.setRecipientOwnerId(6L); row.setRecipientName("测试业主");
        row.setRecipientEmail("owner@example.com"); row.setProjectName("测试建案"); row.setUnitNo("A-01");
        return row;
    }
}
