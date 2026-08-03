package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.mapper.AdminFinanceReviewMapper;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReviewActionContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReserveRefundContext;

@ExtendWith(MockitoExtension.class)
class AdminFinanceReviewServiceTest {
    @Mock private AdminFinanceReviewMapper mapper;
    @TempDir Path storageRoot;
    private AdminFinanceReviewService service;

    @BeforeEach
    void setUp() {
        service = new AdminFinanceReviewService(mapper, storageRoot.toString());
    }

    @Test
    void confirmsPaymentAndAllocatesItToTheInstallment() {
        ReviewActionContext context = context("pending", "5000.00", "1000.00", "2000.00", "2000.00");
        when(mapper.lockReview(11L)).thenReturn(context);
        when(mapper.confirmFinanceRecord(11L, 99L)).thenReturn(1);
        when(mapper.allocateConfirmedPayment(31L, new BigDecimal("2000.00"))).thenReturn(1);
        when(mapper.updateReceiptReview(21L, "Bank checked")).thenReturn(1);
        when(mapper.reviewDocument(41L, "approved", 99L, "Bank checked")).thenReturn(1);

        service.confirm(99L, 11L, "Bank checked");

        verify(mapper).allocateConfirmedPayment(31L, new BigDecimal("2000.00"));
        verify(mapper).insertNotification(18L, 8L, 11L, "房款已確認",
                "Pavilion Square A-01 第 2 期已確認收款 RM 2000.00。", "normal");
        verify(mapper).insertAudit(99L, 11L, "confirm_property_payment", "confirmed", "Bank checked");
    }

    @Test
    void rejectsProofWithoutIncreasingInstallmentPaidAmount() {
        ReviewActionContext context = context("pending", "5000.00", "1000.00", "2000.00", "2000.00");
        when(mapper.lockReview(11L)).thenReturn(context);
        when(mapper.rejectFinanceRecord(11L, 99L)).thenReturn(1);
        when(mapper.updateReceiptReview(21L, "Reference is unreadable")).thenReturn(1);
        when(mapper.reviewDocument(41L, "needs_changes", 99L, "Reference is unreadable")).thenReturn(1);

        service.reject(99L, 11L, "Reference is unreadable");

        verify(mapper, never()).allocateConfirmedPayment(31L, new BigDecimal("2000.00"));
        verify(mapper).insertNotification(18L, 8L, 11L, "付款憑證退回補件",
                "Pavilion Square A-01 第 2 期付款憑證需要補件：Reference is unreadable", "high");
    }

    @Test
    void rejectsConfirmationWhenAllocationExceedsRemainingInstallmentBalance() {
        ReviewActionContext context = context("pending", "5000.00", "4500.00", "1000.00", "1000.00");
        when(mapper.lockReview(11L)).thenReturn(context);

        assertThatThrownBy(() -> service.confirm(99L, 11L, "Bank checked"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("exceeds the installment balance");

        verify(mapper, never()).confirmFinanceRecord(11L, 99L);
    }

    @Test
    void preventsReviewingTheSamePaymentTwice() {
        when(mapper.lockReview(11L)).thenReturn(context("confirmed", "5000.00", "2000.00", "2000.00", "2000.00"));

        assertThatThrownBy(() -> service.reject(99L, 11L, "Duplicate"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already been reviewed");

        verify(mapper, never()).rejectFinanceRecord(11L, 99L);
        verify(mapper, never()).updateReceiptReview(org.mockito.ArgumentMatchers.anyLong(), anyString());
    }

    @Test
    void confirmsReserveRefundThenDeductsTheReserveBalance() {
        ReserveRefundContext refund = new ReserveRefundContext();
        refund.setReserveAccountId(71L); refund.setAmount(new BigDecimal("1200.00"));
        refund.setCurrentBalance(new BigDecimal("5000.00")); refund.setOwnerId(8L);
        refund.setUserId(18L); refund.setProjectName("Pavilion Square"); refund.setUnitNo("A-01");
        when(mapper.lockRecordType(15L)).thenReturn("reserve_refund");
        when(mapper.lockReserveRefund(15L)).thenReturn(refund);
        when(mapper.confirmReserveRefund(15L, 99L)).thenReturn(1);
        when(mapper.debitReserveBalance(71L, new BigDecimal("1200.00"))).thenReturn(1);
        when(mapper.insertReserveRefundTransaction(71L, 15L, new BigDecimal("1200.00"), new BigDecimal("3800.00"), 99L)).thenReturn(1);

        service.confirm(99L, 15L, "已完成匯款");

        verify(mapper).debitReserveBalance(71L, new BigDecimal("1200.00"));
        verify(mapper).insertReserveRefundTransaction(71L, 15L, new BigDecimal("1200.00"), new BigDecimal("3800.00"), 99L);
        verify(mapper).insertAudit(99L, 15L, "confirm_reserve_refund", "confirmed", "已完成匯款");
    }

    private ReviewActionContext context(String status, String due, String paid, String financeAmount, String allocated) {
        ReviewActionContext context = new ReviewActionContext();
        context.setFinanceRecordId(11L);
        context.setConfirmationStatus(status);
        context.setAmount(new BigDecimal(financeAmount));
        context.setOwnerId(8L);
        context.setUserId(18L);
        context.setReceiptId(21L);
        context.setProofDocumentId(41L);
        context.setInstallmentId(31L);
        context.setAllocatedAmount(new BigDecimal(allocated));
        context.setAmountDue(new BigDecimal(due));
        context.setAmountPaid(new BigDecimal(paid));
        context.setProjectName("Pavilion Square");
        context.setUnitNo("A-01");
        context.setInstallmentNo(2);
        context.setMilestone("Foundation");
        return context;
    }
}
