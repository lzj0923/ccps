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
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.dto.AdminMaintenanceCompleteRequest;
import com.ccps.backend.dto.AdminMaintenanceResubmitRequest;
import com.ccps.backend.dto.AdminExpenseCreateRequest;
import com.ccps.backend.dto.AdminPropertyMaintenanceResponse;
import com.ccps.backend.dto.AdminPropertyMaintenanceUpdateRequest;
import com.ccps.backend.mapper.AdminMaintenanceMapper;
import com.ccps.backend.mapper.AdminMaintenanceMapper.CompletionContext;
import com.ccps.backend.mapper.AdminMaintenanceMapper.ExpenseContext;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewCashflow;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewFinance;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewExpenseCashflow;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewExpenseFinance;
import com.ccps.backend.mapper.AdminMaintenanceMapper.PropertyContext;
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
    void reserveCompletionWaitsForCentralFinanceConfirmationBeforeDeducting() {
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
        doAnswer(invocation -> { invocation.<NewFinance>getArgument(0).setId(91L); return 1; })
                .when(mapper).insertFinance(any(NewFinance.class));
        doAnswer(invocation -> { invocation.<NewCashflow>getArgument(0).setId(101L); return 1; })
                .when(mapper).insertCashflow(any(NewCashflow.class));
        when(mapper.completeWorkOrder(81L, 101L, new BigDecimal("100.00"))).thenReturn(1);

        service.complete(1L, 81L,
                new AdminMaintenanceCompleteRequest(new BigDecimal("100.00"), "reserve", "完成維修"));

        ArgumentCaptor<NewFinance> financeCaptor = ArgumentCaptor.forClass(NewFinance.class);
        verify(mapper).insertFinance(financeCaptor.capture());
        assertThat(financeCaptor.getValue().getConfirmationStatus()).isEqualTo("pending");
        verify(mapper, never()).linkReserveDebitToWorkOrder(anyLong(), anyLong());
        verify(mapper, never()).findReserveDebitAmount(anyLong(), anyLong());
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
    void resubmitsRejectedMaintenanceFinanceWithoutCreatingAnotherWorkOrder() {
        CompletionContext context = new CompletionContext();
        context.setId(84L);
        context.setStatus("completed");
        context.setFinanceRecordId(94L);
        context.setPaymentStatus("voided");
        context.setConfirmationStatus("rejected");
        context.setSyncStatus("not_synced");
        when(mapper.lockContext(84L)).thenReturn(context);
        when(mapper.resetRejectedMaintenanceWorkOrder(84L)).thenReturn(1);

        service.resubmitFinance(1L, 84L, new AdminMaintenanceResubmitRequest("补充说明"));

        verify(mapper).resetRejectedMaintenanceWorkOrder(84L);
        verify(mapper).insertPropertyHistory(84L, "submitted", "补充说明", 1L);
        verify(mapper).insertResubmitFinanceAudit(1L, 84L, "补充说明");
        verify(mapper, never()).insertFinance(any(NewFinance.class));
        verify(mapper, never()).insertCashflow(any(NewCashflow.class));
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
        when(mapper.insertRecycleBin(any(AdminMaintenanceMapper.RecycleBinCreate.class))).thenAnswer(invocation -> {
            invocation.<AdminMaintenanceMapper.RecycleBinCreate>getArgument(0).setId(501L); return 1;
        });
        when(mapper.voidExpenseFinance(93L)).thenReturn(1);

        service.deleteExpense(1L, 103L);

        verify(mapper).clearExpenseReserve(103L);
        verify(mapper).voidExpenseFinance(93L);
        verify(mapper).insertCreateAudit(1L, "delete_expense", "cashflow_entry", 103L,
                "{\"status\":\"voided\",\"recycleBinId\":501}");
    }

    @Test
    void expenseDeleteCreatesAThirtyDayRecycleBinSnapshot() {
        ExpenseContext current = new ExpenseContext();
        current.setId(103L);
        current.setFinanceRecordId(93L);
        current.setUnitId(12L);
        current.setPaymentStatus("unpaid");
        current.setConfirmationStatus("pending");
        current.setSyncStatus("not_synced");
        when(mapper.lockExpense(103L)).thenReturn(current);
        when(mapper.insertRecycleBin(any(AdminMaintenanceMapper.RecycleBinCreate.class))).thenAnswer(invocation -> {
            invocation.<AdminMaintenanceMapper.RecycleBinCreate>getArgument(0).setId(501L); return 1;
        });
        when(mapper.voidExpenseFinance(93L)).thenReturn(1);

        service.deleteExpense(1L, 103L);

        ArgumentCaptor<AdminMaintenanceMapper.RecycleBinCreate> recycleCaptor = ArgumentCaptor.forClass(AdminMaintenanceMapper.RecycleBinCreate.class);
        verify(mapper).insertRecycleBin(recycleCaptor.capture());
        assertThat(recycleCaptor.getValue().getEntityType()).isEqualTo("expense");
        assertThat(recycleCaptor.getValue().getExpiresInDays()).isEqualTo(30);
    }

    @Test
    void restoresAnExpenseFromTheRecycleBin() {
        AdminMaintenanceMapper.RecycleBinContext deleted = new AdminMaintenanceMapper.RecycleBinContext();
        deleted.setId(501L); deleted.setEntityType("expense"); deleted.setEntityId(103L); deleted.setFinanceRecordId(93L);
        deleted.setPreviousPaymentStatus("unpaid"); deleted.setPreviousConfirmationStatus("pending");
        deleted.setPreviousSyncStatus("not_synced"); deleted.setPreviousPaymentMethod("direct_payment");
        when(mapper.lockRecycleBin(501L)).thenReturn(deleted);
        when(mapper.restoreExpenseFinance(93L, "unpaid", "pending", "not_synced", "direct_payment")).thenReturn(1);
        when(mapper.restoreExpenseCashflow(103L, null)).thenReturn(1);
        when(mapper.markRecycleBinRestored(501L, 1L)).thenReturn(1);

        service.restoreRecycleBin(1L, 501L);

        verify(mapper).restoreExpenseFinance(93L, "unpaid", "pending", "not_synced", "direct_payment");
        verify(mapper).restoreExpenseCashflow(103L, null);
        verify(mapper).markRecycleBinRestored(501L, 1L);
    }

    @Test
    void pendingMaintenanceCanBeDeletedAndArchived() {
        CompletionContext current = maintenanceContext(85L, "submitted", "pending");
        when(mapper.lockContext(85L)).thenReturn(current);
        stubPropertyMaintenance(12L, 3L, 85L, true, "submitted");
        when(mapper.insertRecycleBin(any(AdminMaintenanceMapper.RecycleBinCreate.class))).thenAnswer(invocation -> {
            invocation.<AdminMaintenanceMapper.RecycleBinCreate>getArgument(0).setId(502L);
            return 1;
        });
        when(mapper.cancelPropertyMaintenance(12L, 85L)).thenReturn(1);

        assertThat(service.deleteMaintenance(1L, 85L)).isEqualTo(502L);

        ArgumentCaptor<AdminMaintenanceMapper.RecycleBinCreate> recycleCaptor = ArgumentCaptor.forClass(AdminMaintenanceMapper.RecycleBinCreate.class);
        verify(mapper).insertRecycleBin(recycleCaptor.capture());
        assertThat(recycleCaptor.getValue().getPreviousReserveAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(mapper).cancelPropertyMaintenance(12L, 85L);
    }

    @Test
    void pendingMaintenanceCanBeEdited() {
        stubPropertyMaintenance(12L, 3L, 87L, true, "submitted");
        when(mapper.updatePropertyMaintenance(12L, 87L, null, "maintenance", "更新后的维修",
                "更新说明", LocalDateTime.of(2026, 8, 25, 10, 0), new BigDecimal("120.00"), "submitted",
                null, null, null, null, null))
                .thenReturn(1);

        AdminPropertyMaintenanceResponse result = service.updateForProperty(1L, 3L, 30L, 87L,
                new AdminPropertyMaintenanceUpdateRequest(null, "maintenance", "更新后的维修", "更新说明",
                        LocalDateTime.of(2026, 8, 25, 10, 0), new BigDecimal("120.00"), "submitted"));

        assertThat(result.editable()).isTrue();
        verify(mapper).updatePropertyMaintenance(12L, 87L, null, "maintenance", "更新后的维修",
                "更新说明", LocalDateTime.of(2026, 8, 25, 10, 0), new BigDecimal("120.00"), "submitted",
                null, null, null, null, null);
    }

    @Test
    void confirmedMaintenanceCannotBeDeletedOrCreateRecycleSnapshot() {
        CompletionContext current = maintenanceContext(86L, "completed", "confirmed");
        when(mapper.lockContext(86L)).thenReturn(current);

        assertThatThrownBy(() -> service.deleteMaintenance(1L, 86L))
                .hasMessageContaining("财务已确认");

        verify(mapper, never()).insertRecycleBin(any(AdminMaintenanceMapper.RecycleBinCreate.class));
        verify(mapper, never()).cancelPropertyMaintenance(anyLong(), anyLong());
    }

    private CompletionContext maintenanceContext(Long workOrderId, String status, String confirmationStatus) {
        CompletionContext context = new CompletionContext();
        context.setId(workOrderId);
        context.setStatus(status);
        context.setUnitId(12L);
        context.setOwnerId(3L);
        context.setOwnerUnitId(30L);
        context.setConfirmationStatus(confirmationStatus);
        context.setPaymentStatus("pending");
        context.setSyncStatus("not_synced");
        return context;
    }

    private void stubPropertyMaintenance(Long unitId, Long ownerId, Long workOrderId,
            boolean editable, String status) {
        PropertyContext property = new PropertyContext();
        property.setUnitId(unitId);
        property.setOwnerId(ownerId);
        property.setOwnerUnitId(30L);
        when(mapper.findPropertyContext(ownerId, 30L)).thenReturn(property);
        when(mapper.findPropertyMaintenanceById(unitId, workOrderId))
                .thenReturn(new AdminPropertyMaintenanceResponse(workOrderId, "MWO-" + workOrderId,
                        null, null, "maintenance", "测试维修", "", null, null, status,
                        BigDecimal.ONE, BigDecimal.ONE, null, 0, null, editable));
    }
}
