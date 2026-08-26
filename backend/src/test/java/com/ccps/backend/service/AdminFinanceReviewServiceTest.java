package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.mapper.AdminFinanceReviewMapper;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.FinanceAllocationNoteContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReviewActionContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReserveRefundContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReopenRecordContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReserveTopupReopenContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.RentCreditAllocationReopenContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.RentPaymentReopenContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.TenantChargeReviewContext;

@ExtendWith(MockitoExtension.class)
class AdminFinanceReviewServiceTest {
    private static final LocalDate ACCOUNTING_DATE = LocalDate.of(2026, 8, 20);
    private static final LocalDate RECEIPT_DATE = LocalDate.of(2026, 8, 18);
    @Mock private AdminFinanceReviewMapper mapper;
    @TempDir Path storageRoot;
    private AdminFinanceReviewService service;

    @BeforeEach
    void setUp() {
        service = new AdminFinanceReviewService(mapper, storageRoot.toString());
        lenient().when(mapper.lockRecordType(anyLong())).thenReturn("property_payment");
        lenient().when(mapper.setFinanceConfirmedDate(anyLong(), any(LocalDate.class), nullable(LocalDate.class)))
                .thenReturn(1);
    }

    @Test
    void confirmsPaymentAndAllocatesItToTheInstallment() {
        ReviewActionContext context = context("pending", "5000.00", "1000.00", "2000.00", "2000.00");
        when(mapper.lockReview(11L)).thenReturn(context);
        when(mapper.confirmFinanceRecord(11L, 99L)).thenReturn(1);
        when(mapper.allocateConfirmedPayment(31L, new BigDecimal("2000.00"))).thenReturn(1);
        when(mapper.updateReceiptReview(21L, "Bank checked")).thenReturn(1);
        when(mapper.reviewDocument(41L, "approved", 99L, "Bank checked")).thenReturn(1);

        service.confirm(99L, 11L, ACCOUNTING_DATE, RECEIPT_DATE, "Bank checked");

        verify(mapper).setFinanceConfirmedDate(11L, ACCOUNTING_DATE, RECEIPT_DATE);
        verify(mapper).syncCashflowDate(11L, ACCOUNTING_DATE);
        verify(mapper).syncTenantDepositDate(11L, ACCOUNTING_DATE);
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
    void returnsRejectedMaintenanceWorkOrderToPendingHandling() {
        when(mapper.lockRecordType(18L)).thenReturn("property_expense");
        when(mapper.rejectExpense(18L, 99L)).thenReturn(1);
        when(mapper.findMaintenanceWorkOrderId(18L)).thenReturn(84L);
        when(mapper.resetMaintenanceAfterFinanceRejection(84L)).thenReturn(1);

        service.reject(99L, 18L, "金额需要补充说明");

        verify(mapper).resetMaintenanceAfterFinanceRejection(84L);
        verify(mapper).insertMaintenanceRejectionHistory(84L, 99L,
                "财务退回：请处理维修工单后再次提交确认。金额需要补充说明");
        verify(mapper).insertAudit(99L, 18L, "reject_property_expense", "rejected", "金额需要补充说明");
    }

    @Test
    void rejectsConfirmationWhenAllocationExceedsRemainingInstallmentBalance() {
        ReviewActionContext context = context("pending", "5000.00", "4500.00", "1000.00", "1000.00");
        when(mapper.lockReview(11L)).thenReturn(context);

        assertThatThrownBy(() -> service.confirm(99L, 11L, ACCOUNTING_DATE, RECEIPT_DATE, "Bank checked"))
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

        service.confirm(99L, 15L, ACCOUNTING_DATE, null, "已完成匯款");

        verify(mapper).debitReserveBalance(71L, new BigDecimal("1200.00"));
        verify(mapper).insertReserveRefundTransaction(71L, 15L, new BigDecimal("1200.00"), new BigDecimal("3800.00"), 99L);
        verify(mapper).insertAudit(99L, 15L, "confirm_reserve_refund", "confirmed", "已完成匯款");
    }

    @Test
    void blocksDirectPaymentConfirmationAfterOwnerTerminatesMandate() {
        when(mapper.lockRecordType(18L)).thenReturn("property_expense");
        when(mapper.countDirectPaymentBlockedByTerminatedMandate(18L)).thenReturn(1);

        assertThatThrownBy(() -> service.confirm(99L, 18L, ACCOUNTING_DATE, null, "确认代付款"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("业主已解约");

        verify(mapper, never()).confirmExpense(18L, 99L);
    }

    @Test
    void allowsDepositRefundToMakeOwnerReserveNegative() {
        ReserveRefundContext refund = new ReserveRefundContext();
        refund.setReserveAccountId(71L); refund.setAmount(new BigDecimal("1200.00"));
        refund.setCurrentBalance(new BigDecimal("800.00")); refund.setOwnerId(8L);
        refund.setUserId(18L); refund.setProjectName("Pavilion Square"); refund.setUnitNo("A-01");
        when(mapper.lockRecordType(15L)).thenReturn("reserve_refund");
        when(mapper.lockReserveRefund(15L)).thenReturn(refund);
        when(mapper.confirmReserveRefund(15L, 99L)).thenReturn(1);
        when(mapper.debitReserveBalance(71L, new BigDecimal("1200.00"))).thenReturn(1);
        when(mapper.insertReserveRefundTransaction(71L, 15L, new BigDecimal("1200.00"),
                new BigDecimal("-400.00"), 99L)).thenReturn(1);

        service.confirm(99L, 15L, ACCOUNTING_DATE, null, "确认返还");

        verify(mapper).debitReserveBalance(71L, new BigDecimal("1200.00"));
        verify(mapper).insertReserveRefundTransaction(71L, 15L, new BigDecimal("1200.00"),
                new BigDecimal("-400.00"), 99L);
    }

    @Test
    void confirmsSecurityDepositWithoutTreatingItAsRentInstallment() {
        when(mapper.lockRecordType(16L)).thenReturn("security_deposit");
        when(mapper.confirmSecurityDeposit(16L, 99L)).thenReturn(1);
        when(mapper.confirmSecurityDepositEntry(16L)).thenReturn(1);

        service.confirm(99L, 16L, ACCOUNTING_DATE, RECEIPT_DATE, "已核对租客押金");

        verify(mapper).confirmSecurityDeposit(16L, 99L);
        verify(mapper).confirmSecurityDepositEntry(16L);
        verify(mapper).insertAudit(99L, 16L, "confirm_security_deposit", "confirmed", "已核对租客押金");
        verify(mapper, never()).lockReview(16L);
    }

    @Test
    void confirmsTenantChargeThenAddsItToTheTenantInvoice() {
        when(mapper.lockRecordType(17L)).thenReturn("tenant_charge");
        TenantChargeReviewContext charge = new TenantChargeReviewContext();
        charge.setInvoiceId(91L); charge.setAmount(new BigDecimal("120.00"));
        when(mapper.lockTenantChargeReview(17L)).thenReturn(charge);
        when(mapper.confirmTenantCharge(17L, 99L)).thenReturn(1);
        when(mapper.increaseTenantChargeInvoice(91L, new BigDecimal("120.00"))).thenReturn(1);

        service.confirm(99L, 17L, ACCOUNTING_DATE, null, "租客账单费用核对正确");

        verify(mapper).increaseTenantChargeInvoice(91L, new BigDecimal("120.00"));
        verify(mapper).insertAudit(99L, 17L, "confirm_tenant_charge", "confirmed", "租客账单费用核对正确");
    }

    @Test
    void rejectsTenantChargeWithoutChangingTheTenantInvoice() {
        when(mapper.lockRecordType(17L)).thenReturn("tenant_charge");
        when(mapper.rejectTenantCharge(17L, 99L)).thenReturn(1);

        service.reject(99L, 17L, "金额录入错误");

        verify(mapper, never()).increaseTenantChargeInvoice(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(BigDecimal.class));
        verify(mapper).insertAudit(99L, 17L, "reject_tenant_charge", "rejected", "金额录入错误");
    }

    @Test
    void reopensReserveTopupAndRemovesItFromTheAccountBalance() {
        when(mapper.lockReopenRecord(21L)).thenReturn(reopen("reserve_topup"));
        ReserveTopupReopenContext topup = new ReserveTopupReopenContext();
        topup.setTransactionId(81L); topup.setReserveAccountId(71L); topup.setCurrentBalance(new BigDecimal("1800.00"));
        topup.setAmount(new BigDecimal("1000.00"));
        when(mapper.lockReserveTopupReopen(21L)).thenReturn(topup);
        when(mapper.reverseReserveTopupBalance(71L, new BigDecimal("1000.00"))).thenReturn(1);
        when(mapper.deleteReserveTopupTransaction(21L)).thenReturn(1);
        when(mapper.reopenFinanceRecord(21L)).thenReturn(1);

        service.reopen(99L, 21L, "收款资料录入错误");

        verify(mapper).reverseReserveTopupBalance(71L, new BigDecimal("1000.00"));
        verify(mapper).deleteReserveTopupTransaction(21L);
        verify(mapper).shiftLaterReserveBalances(71L, 81L, new BigDecimal("1000.00"));
        verify(mapper).reopenReserveTopupDocuments(21L, "收款资料录入错误");
        verify(mapper).insertReopenAudit(99L, 21L, "收款资料录入错误");
    }

    @Test
    void reopensRentPaymentIncludingPrepaymentAndDepositDeduction() {
        when(mapper.lockReopenRecord(22L)).thenReturn(reopen("rent_payment"));
        RentPaymentReopenContext rent = new RentPaymentReopenContext();
        rent.setRentInvoiceId(31L); rent.setRentCreditId(41L);
        rent.setAmount(new BigDecimal("1800.00")); rent.setCreditAmount(new BigDecimal("600.00"));
        rent.setPaymentMethod("security_deposit");
        RentCreditAllocationReopenContext allocation = new RentCreditAllocationReopenContext();
        allocation.setRentInvoiceId(32L); allocation.setAmount(new BigDecimal("600.00"));
        when(mapper.lockRentPaymentReopen(22L)).thenReturn(rent);
        when(mapper.lockRentCreditAllocations(22L)).thenReturn(List.of(allocation));
        when(mapper.reverseRentInvoicePayment(31L, new BigDecimal("1200.00"))).thenReturn(1);
        when(mapper.reverseRentInvoicePayment(32L, new BigDecimal("600.00"))).thenReturn(1);
        when(mapper.deleteRentCredit(22L)).thenReturn(1);
        when(mapper.voidReopenedRentPayment(22L)).thenReturn(1);

        service.reopen(99L, 22L, "租金收款录入错误");

        verify(mapper).deleteRentCreditAllocations(22L);
        verify(mapper).cancelRentDepositDeduction(22L);
        verify(mapper).voidReopenedRentPayment(22L);
        verify(mapper).insertRentReopenAudit(99L, 22L, "租金收款录入错误");
        verify(mapper, never()).reopenFinanceRecord(22L);
    }

    @Test
    void reopensSelectedRentPaymentsAsOneBatch() {
        when(mapper.lockReopenRecord(22L)).thenReturn(reopen("rent_payment"));
        when(mapper.lockReopenRecord(23L)).thenReturn(reopen("rent_payment"));
        RentPaymentReopenContext first = new RentPaymentReopenContext();
        first.setRentInvoiceId(31L); first.setAmount(new BigDecimal("1000.00"));
        first.setCreditAmount(BigDecimal.ZERO); first.setPaymentMethod("bank_transfer");
        RentPaymentReopenContext second = new RentPaymentReopenContext();
        second.setRentInvoiceId(32L); second.setAmount(new BigDecimal("800.00"));
        second.setCreditAmount(BigDecimal.ZERO); second.setPaymentMethod("cash");
        when(mapper.lockRentPaymentReopen(22L)).thenReturn(first);
        when(mapper.lockRentPaymentReopen(23L)).thenReturn(second);
        when(mapper.lockRentCreditAllocations(22L)).thenReturn(List.of());
        when(mapper.lockRentCreditAllocations(23L)).thenReturn(List.of());
        when(mapper.reverseRentInvoicePayment(31L, new BigDecimal("1000.00"))).thenReturn(1);
        when(mapper.reverseRentInvoicePayment(32L, new BigDecimal("800.00"))).thenReturn(1);
        when(mapper.voidReopenedRentPayment(22L)).thenReturn(1);
        when(mapper.voidReopenedRentPayment(23L)).thenReturn(1);

        service.reopenBatch(99L, List.of(22L, 23L), "批量录入错误");

        verify(mapper).voidReopenedRentPayment(22L);
        verify(mapper).voidReopenedRentPayment(23L);
        verify(mapper).insertRentReopenAudit(99L, 22L, "批量录入错误");
        verify(mapper).insertRentReopenAudit(99L, 23L, "批量录入错误");
    }

    @Test
    void updatesUniversalAllocationNoteAndKeepsItForTheSameFinanceType() {
        FinanceAllocationNoteContext context = new FinanceAllocationNoteContext();
        context.setFinanceRecordId(22L); context.setUnitId(8L); context.setRecordType("rent_payment");
        context.setDirection("income"); context.setCategory("rent");
        when(mapper.lockAllocationNoteContext(22L)).thenReturn(context);
        when(mapper.updateAllocationNote(22L,"租户 RM 800；业主 RM 200")).thenReturn(1);

        service.updateAllocationNote(99L,22L,"租户 RM 800；业主 RM 200",true);

        verify(mapper).updateLinkedCashflowAllocationNote(22L,"租户 RM 800；业主 RM 200");
        verify(mapper).upsertAllocationNoteDefault(8L,"rent_payment","租户 RM 800；业主 RM 200",99L);
        verify(mapper).insertAllocationNoteAudit(99L,22L,"租户 RM 800；业主 RM 200",true);
    }

    private ReopenRecordContext reopen(String type) {
        ReopenRecordContext context = new ReopenRecordContext();
        context.setFinanceRecordId(1L); context.setRecordType(type);
        context.setConfirmationStatus("confirmed"); context.setSyncStatus("pending");
        return context;
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
