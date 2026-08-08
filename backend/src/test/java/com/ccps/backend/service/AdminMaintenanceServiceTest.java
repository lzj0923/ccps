package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.dto.AdminMaintenanceCompleteRequest;
import com.ccps.backend.dto.AdminExpenseCreateRequest;
import com.ccps.backend.mapper.AdminMaintenanceMapper;
import com.ccps.backend.mapper.AdminMaintenanceMapper.CompletionContext;
import com.ccps.backend.mapper.AdminMaintenanceMapper.ExpenseContext;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewCashflow;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewFinance;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewExpenseCashflow;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewExpenseFinance;
import com.ccps.backend.mapper.AdminMaintenanceMapper.UnitContext;

@ExtendWith(MockitoExtension.class)
class AdminMaintenanceServiceTest {
    @Mock private AdminMaintenanceMapper mapper;
    @Mock private OwnerExpenseMaintenanceService detailService;
    @Mock private AdminPropertyMaintenanceRecordService maintenanceRecordService;

    private AdminMaintenanceService service;

    @BeforeEach
    void setUp() {
        service = new AdminMaintenanceService(mapper, detailService, maintenanceRecordService);
    }

    @Test
    void reserveCompletionReliesOnDatabasePolicyInsteadOfDeductingTwice() {
        CompletionContext context = new CompletionContext();
        context.setId(81L);
        context.setStatus("inspection");
        context.setUnitId(12L);
        context.setOwnerId(3L);
        context.setVendorId(7L);
        context.setReserveAccountId(4L);
        context.setReserveBalance(new BigDecimal("1000.00"));
        context.setReserveDeductedAmount(BigDecimal.ZERO);
        context.setDirectPaymentAllowed(true);
        when(mapper.lockContext(81L)).thenReturn(context);
        when(mapper.countPhotos(81L, "before_photo")).thenReturn(1);
        when(mapper.countPhotos(81L, "after_photo")).thenReturn(1);
        doAnswer(invocation -> { invocation.<NewFinance>getArgument(0).setId(91L); return 1; })
                .when(mapper).insertFinance(any(NewFinance.class));
        doAnswer(invocation -> { invocation.<NewCashflow>getArgument(0).setId(101L); return 1; })
                .when(mapper).insertCashflow(any(NewCashflow.class));
        when(mapper.linkReserveDebitToWorkOrder(91L, 81L)).thenReturn(1);
        when(mapper.findReserveDebitAmount(91L, 81L)).thenReturn(new BigDecimal("100.00"));
        when(mapper.completeWorkOrder(81L, 101L, new BigDecimal("100.00"))).thenReturn(1);

        service.complete(1L, 81L,
                new AdminMaintenanceCompleteRequest(new BigDecimal("100.00"), "reserve", "完成維修"));

        verify(mapper).linkReserveDebitToWorkOrder(91L, 81L);
        verify(mapper).completeWorkOrder(eq(81L), eq(101L), eq(new BigDecimal("100.00")));
    }

    @Test
    void directPaymentCannotBeConvertedIntoAReserveDeduction() {
        CompletionContext context = new CompletionContext();
        context.setId(82L);
        context.setStatus("inspection");
        context.setUnitId(12L);
        context.setOwnerId(3L);
        context.setVendorId(7L);
        context.setReserveAccountId(4L);
        context.setReserveBalance(new BigDecimal("1000.00"));
        context.setReserveDeductedAmount(BigDecimal.ZERO);
        context.setDirectPaymentAllowed(true);
        when(mapper.lockContext(82L)).thenReturn(context);
        when(mapper.countPhotos(82L, "before_photo")).thenReturn(1);
        when(mapper.countPhotos(82L, "after_photo")).thenReturn(1);
        doAnswer(invocation -> { invocation.<NewFinance>getArgument(0).setId(92L); return 1; })
                .when(mapper).insertFinance(any(NewFinance.class));
        doAnswer(invocation -> { invocation.<NewCashflow>getArgument(0).setId(102L); return 1; })
                .when(mapper).insertCashflow(any(NewCashflow.class));
        when(mapper.completeWorkOrder(82L, 102L, new BigDecimal("100.00"))).thenReturn(1);

        service.complete(1L, 82L,
                new AdminMaintenanceCompleteRequest(new BigDecimal("100.00"), "direct_payment", "現金支付"));

        ArgumentCaptor<NewFinance> financeCaptor = ArgumentCaptor.forClass(NewFinance.class);
        ArgumentCaptor<NewCashflow> cashflowCaptor = ArgumentCaptor.forClass(NewCashflow.class);
        verify(mapper).insertFinance(financeCaptor.capture());
        verify(mapper).insertCashflow(cashflowCaptor.capture());
        assertThat(financeCaptor.getValue().getConfirmationStatus()).isEqualTo("pending");
        assertThat(cashflowCaptor.getValue().getReserveAccountId()).isNull();
        verify(mapper, never()).linkReserveDebitToWorkOrder(anyLong(), anyLong());
        verify(mapper, never()).findReserveDebitAmount(anyLong(), anyLong());
    }

    @Test
    void directPaymentExpenseIsSubmittedForFinanceReview() {
        UnitContext unit = new UnitContext();
        unit.setUnitId(12L);
        unit.setOwnerId(3L);
        unit.setDirectPaymentAllowed(true);
        when(mapper.lockUnitContext(12L)).thenReturn(unit);
        doAnswer(invocation -> { invocation.<NewExpenseFinance>getArgument(0).setId(93L); return 1; })
                .when(mapper).insertExpenseFinance(any(NewExpenseFinance.class));
        doAnswer(invocation -> { invocation.<NewExpenseCashflow>getArgument(0).setId(103L); return 1; })
                .when(mapper).insertExpenseCashflow(any(NewExpenseCashflow.class));

        service.createExpense(1L, new AdminExpenseCreateRequest(12L, "utilities", "代付水费",
                new BigDecimal("88.00"), LocalDate.of(2026, 8, 7), "direct_payment"));

        ArgumentCaptor<NewExpenseFinance> financeCaptor = ArgumentCaptor.forClass(NewExpenseFinance.class);
        verify(mapper).insertExpenseFinance(financeCaptor.capture());
        assertThat(financeCaptor.getValue().getPaymentMethod()).isEqualTo("direct_payment");
        assertThat(financeCaptor.getValue().getPaymentStatus()).isEqualTo("unpaid");
        assertThat(financeCaptor.getValue().getConfirmationStatus()).isEqualTo("pending");
    }

    @Test
    void reserveExpenseCanExceedCurrentReserveBalance() {
        UnitContext unit = new UnitContext();
        unit.setUnitId(12L); unit.setOwnerId(3L); unit.setReserveAccountId(4L);
        unit.setReserveBalance(new BigDecimal("20.00"));
        when(mapper.lockUnitContext(12L)).thenReturn(unit);
        doAnswer(invocation -> { invocation.<NewExpenseFinance>getArgument(0).setId(94L); return 1; })
                .when(mapper).insertExpenseFinance(any(NewExpenseFinance.class));
        doAnswer(invocation -> { invocation.<NewExpenseCashflow>getArgument(0).setId(104L); return 1; })
                .when(mapper).insertExpenseCashflow(any(NewExpenseCashflow.class));

        service.createExpense(1L, new AdminExpenseCreateRequest(12L, "utilities", "代付水费",
                new BigDecimal("88.00"), LocalDate.of(2026, 8, 8), "reserve"));

        ArgumentCaptor<NewExpenseCashflow> cashflowCaptor = ArgumentCaptor.forClass(NewExpenseCashflow.class);
        verify(mapper).insertExpenseCashflow(cashflowCaptor.capture());
        assertThat(cashflowCaptor.getValue().getReserveAccountId()).isEqualTo(4L);
    }

    @Test
    void directPaymentExpenseCanBeCorrectedBeforeFinanceReview() {
        ExpenseContext current = new ExpenseContext();
        current.setId(103L);
        current.setFinanceRecordId(93L);
        current.setUnitId(12L);
        current.setTransactionNo("EXP-20260807-ABC123");
        current.setPaymentStatus("unpaid");
        current.setConfirmationStatus("pending");
        current.setSyncStatus("not_synced");
        when(mapper.lockExpense(103L)).thenReturn(current);
        UnitContext unit = new UnitContext();
        unit.setUnitId(12L);
        unit.setOwnerId(3L);
        unit.setDirectPaymentAllowed(true);
        when(mapper.lockUnitContext(12L)).thenReturn(unit);
        LocalDate correctedDate = LocalDate.of(2026, 8, 8);
        when(mapper.updateExpenseFinance(93L, new BigDecimal("98.00"), correctedDate,
                "direct_payment", "unpaid", "pending", 1L)).thenReturn(1);
        when(mapper.updateExpenseCashflow(103L, "utilities", "修正后的代付水费", correctedDate, null))
                .thenReturn(1);

        service.updateExpense(1L, 103L, new AdminExpenseCreateRequest(12L, "utilities", "修正后的代付水费",
                new BigDecimal("98.00"), correctedDate, "direct_payment"));

        verify(mapper).updateExpenseFinance(93L, new BigDecimal("98.00"), correctedDate,
                "direct_payment", "unpaid", "pending", 1L);
    }

    @Test
    void blocksDirectPaymentExpenseAfterOwnerTerminatesMandate() {
        UnitContext unit = new UnitContext();
        unit.setUnitId(12L); unit.setOwnerId(3L); unit.setDirectPaymentAllowed(false);
        when(mapper.lockUnitContext(12L)).thenReturn(unit);

        assertThatThrownBy(() -> service.createExpense(1L,
                new AdminExpenseCreateRequest(12L, "utilities", "代付水费",
                        new BigDecimal("88.00"), LocalDate.of(2026, 8, 8), "direct_payment")))
                .hasMessageContaining("业主已解约");

        verify(mapper, never()).insertExpenseFinance(any(NewExpenseFinance.class));
    }

    @Test
    void blocksMaintenanceDirectPaymentAfterOwnerTerminatesMandate() {
        CompletionContext context = new CompletionContext();
        context.setId(83L); context.setStatus("inspection"); context.setUnitId(12L);
        context.setOwnerId(3L); context.setDirectPaymentAllowed(false);
        when(mapper.lockContext(83L)).thenReturn(context);

        assertThatThrownBy(() -> service.complete(1L, 83L,
                new AdminMaintenanceCompleteRequest(new BigDecimal("100.00"),
                        "direct_payment", "完成维修")))
                .hasMessageContaining("业主已解约");

        verify(mapper, never()).insertFinance(any(NewFinance.class));
    }

    @Test
    void expenseDeleteVoidsTheFinanceRecordAndKeepsAuditHistory() {
        ExpenseContext current = new ExpenseContext();
        current.setId(103L);
        current.setFinanceRecordId(93L);
        current.setUnitId(12L);
        current.setPaymentStatus("unpaid");
        current.setSyncStatus("not_synced");
        when(mapper.lockExpense(103L)).thenReturn(current);
        when(mapper.voidExpenseFinance(93L)).thenReturn(1);

        service.deleteExpense(1L, 103L);

        verify(mapper).clearExpenseReserve(103L);
        verify(mapper).voidExpenseFinance(93L);
        verify(mapper).insertCreateAudit(1L, "delete_expense", "cashflow_entry", 103L,
                "{\"status\":\"voided\"}");
    }
}
