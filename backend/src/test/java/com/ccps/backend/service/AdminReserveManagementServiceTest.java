package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.dto.AdminReserveSettingsRequest;
import com.ccps.backend.dto.AdminReserveBatchRefundRequest;
import com.ccps.backend.dto.AdminReserveDirectTopupRequest;
import com.ccps.backend.dto.AdminReserveRefundRequest;
import com.ccps.backend.mapper.AdminReserveManagementMapper;
import com.ccps.backend.mapper.AdminReserveManagementMapper.DirectTopupContext;
import com.ccps.backend.mapper.AdminReserveManagementMapper.DirectTopupRecord;
import com.ccps.backend.mapper.AdminReserveManagementMapper.RefundBankContext;
import com.ccps.backend.mapper.AdminReserveManagementMapper.SettingsRow;

@ExtendWith(MockitoExtension.class)
class AdminReserveManagementServiceTest {
    @Mock private AdminReserveManagementMapper mapper;
    @Mock private ReserveTargetPolicyService targetPolicyService;
    private AdminReserveManagementService service;

    @BeforeEach
    void setUp() {
        service = new AdminReserveManagementService(mapper, targetPolicyService);
    }

    @Test
    void manualSettingRemainsEffective() {
        SettingsRow before = settings("auto", "2000.00", "2500.00", true);
        when(mapper.findSettings(1L)).thenReturn(before);
        when(mapper.updateSettings(1L, "manual", new BigDecimal("3200.00"), true, "业主要求结算前联系")).thenReturn(1);

        service.updateSettings(9L, 1L,
                new AdminReserveSettingsRequest(new BigDecimal("3200.00"), true, false, "  业主要求结算前联系  "));

        verify(mapper).insertSettingsAudit(9L, 1L, new BigDecimal("2000.00"), "auto", true,
                new BigDecimal("3200.00"), "manual", true, null, "业主要求结算前联系");
    }

    @Test
    void automaticModeUsesCalculatedTargetAndImmediatelyRecalculates() {
        SettingsRow before = settings("manual", "3200.00", "2500.00", true);
        when(mapper.findSettings(2L)).thenReturn(before);
        when(mapper.updateSettings(2L, "auto", new BigDecimal("2500.00"), true, null)).thenReturn(1);

        service.updateSettings(9L, 2L, new AdminReserveSettingsRequest(null, true, true, null));

        verify(targetPolicyService).recalculate(2L);
        verify(mapper).insertSettingsAudit(9L, 2L, new BigDecimal("3200.00"), "manual", true,
                new BigDecimal("2500.00"), "auto", true, null, null);
    }

    @Test
    void directTopupCreatesPendingReviewWithoutPostingReserveBalance() {
        DirectTopupContext context = new DirectTopupContext();
        context.setOwnerId(3L); context.setUserId(4L); context.setUnitId(5L);
        context.setCurrentBalance(new BigDecimal("100.00")); context.setOwnerName("业主");
        context.setProjectName("建案"); context.setUnitNo("A-01");
        when(mapper.findDirectTopupContext(8L)).thenReturn(context);
        when(mapper.insertDirectTopupFinance(org.mockito.ArgumentMatchers.any(DirectTopupRecord.class))).thenAnswer(invocation -> {
            DirectTopupRecord record = invocation.getArgument(0); record.setId(18L); return 1;
        });
        service.directTopup(9L, 8L, new AdminReserveDirectTopupRequest(
                new BigDecimal("50.00"), LocalDate.now(), "bank_transfer", "业主", null, null));

        verify(mapper).insertDirectTopupReceipt(org.mockito.ArgumentMatchers.eq(18L),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("业主"),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq("管理員直接充值"));
        verify(mapper, never()).insertDirectTopupTransaction(anyLong(), anyLong(), any(), any(), any(), any(), anyLong());
        verify(mapper, never()).updateDirectTopupBalance(anyLong(), any());
        verify(mapper, never()).insertDirectTopupNotification(anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    void singleRefundUsesSelectedPaymentDate() {
        DirectTopupContext context = new DirectTopupContext();
        context.setOwnerId(3L); context.setUnitId(5L);
        when(mapper.findDirectTopupContext(8L)).thenReturn(context);
        when(mapper.insertReserveRefundFinance(any(DirectTopupRecord.class))).thenAnswer(invocation -> {
            invocation.<DirectTopupRecord>getArgument(0).setId(101L); return 1;
        });
        when(mapper.insertReserveRefundCashflow(eq(101L), eq(5L), eq(3L), any(), any(LocalDate.class)))
                .thenReturn(1);
        LocalDate selectedDate = LocalDate.of(2026, 9, 3);

        service.createRefund(9L, 8L, new AdminReserveRefundRequest(
                new BigDecimal("1200.00"), selectedDate, "bank_transfer", "终止管理返还"));

        ArgumentCaptor<DirectTopupRecord> refundCaptor = ArgumentCaptor.forClass(DirectTopupRecord.class);
        verify(mapper).insertReserveRefundFinance(refundCaptor.capture());
        assertThat(refundCaptor.getValue().getPaymentDate()).isEqualTo(selectedDate);
        verify(mapper).insertReserveRefundCashflow(eq(101L), eq(5L), eq(3L), any(), eq(selectedDate));
    }

    @Test
    void batchRefundUsesDailyLimitAndCreatesEditableOverseasFees() {
        RefundBankContext context = new RefundBankContext();
        context.setReserveAccountId(8L); context.setOwnerId(3L); context.setUnitId(5L);
        context.setBankAccountId(21L); context.setBankName("海外银行");
        context.setTransferLimit(new BigDecimal("10000.00")); context.setOverseasBank(true);
        context.setOverseasTransferFee(new BigDecimal("25.00"));
        when(mapper.findRefundBankContext(8L, 21L)).thenReturn(context);
        AtomicLong principalId = new AtomicLong(100L);
        when(mapper.insertReserveRefundFinance(any(DirectTopupRecord.class))).thenAnswer(invocation -> {
            invocation.<DirectTopupRecord>getArgument(0).setId(principalId.incrementAndGet()); return 1;
        });
        when(mapper.insertReserveRefundCashflow(anyLong(), eq(5L), eq(3L), any(), any(LocalDate.class))).thenReturn(1);
        AtomicLong feeId = new AtomicLong(200L);
        when(mapper.insertOverseasFeeFinance(any(DirectTopupRecord.class))).thenAnswer(invocation -> {
            invocation.<DirectTopupRecord>getArgument(0).setId(feeId.incrementAndGet()); return 1;
        });
        when(mapper.insertOverseasFeeCashflow(anyLong(), eq(5L), eq(3L), contains("海外银行汇款手续费"),
                any(LocalDate.class))).thenReturn(1);
        when(mapper.insertRefundTransfer(any(), eq(8L), eq(21L), anyLong(), anyLong(), any(Integer.class),
                any(Integer.class), any(LocalDate.class), any(BigDecimal.class), any(BigDecimal.class), eq(9L)))
                .thenReturn(1);

        LocalDate selectedDate = LocalDate.of(2026, 9, 3);
        var result = service.createRefunds(9L, new AdminReserveBatchRefundRequest(
                List.of(new AdminReserveBatchRefundRequest.Item(8L, 21L, new BigDecimal("12000.00"))),
                selectedDate, "bank_transfer", "终止管理返还"));

        assertThat(result).hasSize(2);
        ArgumentCaptor<DirectTopupRecord> principalCaptor = ArgumentCaptor.forClass(DirectTopupRecord.class);
        verify(mapper, org.mockito.Mockito.times(2)).insertReserveRefundFinance(principalCaptor.capture());
        assertThat(principalCaptor.getAllValues()).extracting(DirectTopupRecord::getAmount)
                .containsExactly(new BigDecimal("10000.00"), new BigDecimal("2000.00"));
        assertThat(principalCaptor.getAllValues()).extracting(DirectTopupRecord::getPaymentDate)
                .containsExactly(selectedDate, selectedDate.plusDays(1));
        ArgumentCaptor<DirectTopupRecord> feeCaptor = ArgumentCaptor.forClass(DirectTopupRecord.class);
        verify(mapper, org.mockito.Mockito.times(2)).insertOverseasFeeFinance(feeCaptor.capture());
        assertThat(feeCaptor.getAllValues()).allSatisfy(record -> {
            assertThat(record.getAmount()).isEqualByComparingTo("25.00");
            assertThat(record.getTransactionNo()).startsWith("MANUAL-CF-BANK-FEE-");
        });
        verify(mapper).insertRefundTransfer(any(), eq(8L), eq(21L), eq(101L), eq(201L), eq(1), eq(2),
                eq(selectedDate), eq(new BigDecimal("10000.00")), eq(new BigDecimal("25.00")), eq(9L));
        verify(mapper).insertRefundTransfer(any(), eq(8L), eq(21L), eq(102L), eq(202L), eq(2), eq(2),
                eq(selectedDate.plusDays(1)), eq(new BigDecimal("2000.00")), eq(new BigDecimal("25.00")), eq(9L));
    }

    private SettingsRow settings(String mode, String minimum, String calculated, boolean alert) {
        SettingsRow row = new SettingsRow();
        row.setId(1L);
        row.setMinimumBalanceMode(mode);
        row.setMinimumBalance(new BigDecimal(minimum));
        row.setCalculatedMinimumBalance(new BigDecimal(calculated));
        row.setLowBalanceAlertEnabled(alert);
        return row;
    }
}
