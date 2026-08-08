package com.ccps.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminMaintenanceCompleteRequest;
import com.ccps.backend.dto.AdminMaintenanceCreateRequest;
import com.ccps.backend.dto.AdminMaintenanceHandlingResponse;
import com.ccps.backend.dto.AdminMaintenanceOptionsResponse;
import com.ccps.backend.dto.AdminExpenseCreateRequest;
import com.ccps.backend.dto.AdminRecordCreateResponse;
import com.ccps.backend.dto.AdminPropertyMaintenanceResponse;
import com.ccps.backend.dto.AdminPropertyMaintenanceUpdateRequest;
import com.ccps.backend.dto.MaintenanceDetailResponse;
import com.ccps.backend.mapper.AdminMaintenanceMapper;
import com.ccps.backend.mapper.AdminMaintenanceMapper.CompletionContext;
import com.ccps.backend.mapper.AdminMaintenanceMapper.ExpenseContext;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewCashflow;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewFinance;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewExpenseCashflow;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewExpenseFinance;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewWorkOrder;
import com.ccps.backend.mapper.AdminMaintenanceMapper.PropertyContext;
import com.ccps.backend.mapper.AdminMaintenanceMapper.UnitContext;

@Service
public class AdminMaintenanceService {
    private static final Set<String> SETTLEMENT_METHODS = Set.of("reserve", "direct_payment");
    private static final Set<String> EDITABLE_STATUSES = Set.of("submitted", "assigned", "in_progress", "inspection");

    private final AdminMaintenanceMapper mapper;
    private final OwnerExpenseMaintenanceService detailService;
    private final AdminPropertyMaintenanceRecordService maintenanceRecordService;

    public AdminMaintenanceService(AdminMaintenanceMapper mapper, OwnerExpenseMaintenanceService detailService,
            AdminPropertyMaintenanceRecordService maintenanceRecordService) {
        this.mapper = mapper;
        this.detailService = detailService;
        this.maintenanceRecordService = maintenanceRecordService;
    }

    @Transactional(readOnly = true)
    public AdminMaintenanceOptionsResponse options() {
        return new AdminMaintenanceOptionsResponse(mapper.findUnitOptions(), mapper.findVendorOptions());
    }

    @Transactional(readOnly = true)
    public List<AdminPropertyMaintenanceResponse> listForProperty(Long ownerId, Long ownerUnitId) {
        PropertyContext property = propertyContext(ownerId, ownerUnitId);
        return mapper.findPropertyMaintenance(property.getUnitId());
    }

    @Transactional
    public AdminPropertyMaintenanceResponse createForProperty(Long actorId, Long ownerId, Long ownerUnitId,
            AdminMaintenanceCreateRequest request) {
        PropertyContext property = propertyContext(ownerId, ownerUnitId);
        validateVendor(request.vendorId());
        String reference = reference("MWO");
        NewWorkOrder workOrder = new NewWorkOrder();
        workOrder.setWorkOrderNo(reference); workOrder.setUnitId(property.getUnitId());
        workOrder.setOwnerId(property.getOwnerId()); workOrder.setVendorId(request.vendorId());
        workOrder.setCategory(request.category()); workOrder.setTitle(request.title());
        workOrder.setDescription(request.description()); workOrder.setRequestedAt(request.requestedAt());
        workOrder.setEstimatedAmount(request.estimatedAmount()); workOrder.setActorId(actorId);
        mapper.insertWorkOrder(workOrder);
        mapper.insertSubmittedHistory(workOrder.getId(), request.requestedAt(), actorId);
        mapper.insertCreateAudit(actorId, "create_maintenance", "maintenance_work_order", workOrder.getId(),
                "{\"workOrderNo\":\"" + reference + "\",\"source\":\"property_workspace\"}");
        return propertyMaintenance(property.getUnitId(), workOrder.getId());
    }

    @Transactional
    public AdminPropertyMaintenanceResponse updateForProperty(Long actorId, Long ownerId, Long ownerUnitId,
            Long workOrderId, AdminPropertyMaintenanceUpdateRequest request) {
        PropertyContext property = propertyContext(ownerId, ownerUnitId);
        AdminPropertyMaintenanceResponse existing = propertyMaintenance(property.getUnitId(), workOrderId);
        if ("completed".equals(existing.status()) || "cancelled".equals(existing.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Completed or cancelled maintenance records cannot be edited");
        }
        if (!EDITABLE_STATUSES.contains(request.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use the maintenance completion workflow to complete a work order");
        }
        validateVendor(request.vendorId());
        if (mapper.updatePropertyMaintenance(property.getUnitId(), workOrderId, request.vendorId(),
                request.category(), request.title(), request.description(), request.requestedAt(),
                request.estimatedAmount(), request.status()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Maintenance record changed; please reload");
        }
        if (!existing.status().equals(request.status())) {
            mapper.insertPropertyHistory(workOrderId, request.status(), "管理端於房產維護記錄更新狀態", actorId);
        }
        mapper.insertCreateAudit(actorId, "update_maintenance", "maintenance_work_order", workOrderId,
                "{\"status\":\"" + request.status() + "\"}");
        return propertyMaintenance(property.getUnitId(), workOrderId);
    }

    @Transactional
    public void cancelForProperty(Long actorId, Long ownerId, Long ownerUnitId, Long workOrderId) {
        PropertyContext property = propertyContext(ownerId, ownerUnitId);
        AdminPropertyMaintenanceResponse existing = propertyMaintenance(property.getUnitId(), workOrderId);
        if ("completed".equals(existing.status()) || existing.cashflowEntryId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Maintenance records linked to finance cannot be deleted");
        }
        if (mapper.cancelPropertyMaintenance(property.getUnitId(), workOrderId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Maintenance record changed; please reload");
        }
        mapper.insertPropertyHistory(workOrderId, "cancelled", "管理端於房產維護記錄刪除（保留審計）", actorId);
        mapper.insertCreateAudit(actorId, "cancel_maintenance", "maintenance_work_order", workOrderId,
                "{\"status\":\"cancelled\"}");
    }

    @Transactional
    public AdminRecordCreateResponse createExpense(Long actorId, AdminExpenseCreateRequest request) {
        validateExpenseSettlement(request.settlementMethod());
        UnitContext unit = unitContext(request.unitId());
        boolean reserve = "reserve".equals(request.settlementMethod());
        requireDirectPaymentAllowed(request.settlementMethod(), unit.isDirectPaymentAllowed());
        if (reserve && unit.getReserveAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No reserve account is linked to this unit");
        }

        String reference = reference("EXP");
        NewExpenseFinance finance = new NewExpenseFinance();
        finance.setTransactionNo(reference); finance.setUnitId(unit.getUnitId()); finance.setOwnerId(unit.getOwnerId());
        finance.setAmount(request.amount()); finance.setOccurredOn(request.occurredOn()); finance.setActorId(actorId);
        finance.setPaymentMethod(reserve ? "reserve_account" : "direct_payment".equals(request.settlementMethod()) ? "direct_payment" : null);
        finance.setPaymentStatus(reserve ? "paid" : "unpaid");
        finance.setConfirmationStatus(reserve ? "confirmed" : "pending");
        mapper.insertExpenseFinance(finance);

        NewExpenseCashflow cashflow = new NewExpenseCashflow();
        cashflow.setFinanceRecordId(finance.getId()); cashflow.setUnitId(unit.getUnitId()); cashflow.setOwnerId(unit.getOwnerId());
        cashflow.setCategory(request.category()); cashflow.setDescription(request.description());
        cashflow.setOccurredOn(request.occurredOn()); cashflow.setReserveAccountId(reserve ? unit.getReserveAccountId() : null);
        mapper.insertExpenseCashflow(cashflow);

        // Reserve expenses are deducted atomically by the database reserve-policy trigger.
        // Direct payments and unpaid expenses remain pending for finance review.
        mapper.insertCreateAudit(actorId, "create_expense", "cashflow_entry", cashflow.getId(),
                "{\"transactionNo\":\"" + reference + "\"}");
        return new AdminRecordCreateResponse(cashflow.getId(), reference);
    }

    @Transactional
    public AdminRecordCreateResponse updateExpense(Long actorId, Long cashflowId, AdminExpenseCreateRequest request) {
        validateExpenseSettlement(request.settlementMethod());
        ExpenseContext current = expenseContext(cashflowId);
        requireExpenseEditable(current);
        if (!current.getUnitId().equals(request.unitId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The unit cannot be changed while editing an expense; delete it and create a new record");
        }
        UnitContext unit = unitContext(request.unitId());
        boolean reserve = "reserve".equals(request.settlementMethod());
        requireDirectPaymentAllowed(request.settlementMethod(), unit.isDirectPaymentAllowed());
        if (reserve && unit.getReserveAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No reserve account is linked to this unit");
        }

        reverseExpenseReserve(actorId, current, "支出修改，回冲原预备金扣款");
        String paymentMethod = reserve ? "reserve_account"
                : "direct_payment".equals(request.settlementMethod()) ? "direct_payment" : null;
        String paymentStatus = reserve ? "paid" : "unpaid";
        String confirmationStatus = reserve ? "confirmed" : "pending";
        if (mapper.updateExpenseFinance(current.getFinanceRecordId(), request.amount(), request.occurredOn(),
                paymentMethod, paymentStatus, confirmationStatus, actorId) != 1
                || mapper.updateExpenseCashflow(cashflowId, request.category(), request.description(),
                        request.occurredOn(), reserve ? unit.getReserveAccountId() : null) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Expense record changed; please reload");
        }
        if (reserve) debitExpenseReserve(actorId, current.getFinanceRecordId(), unit.getReserveAccountId(), request.amount());
        mapper.insertCreateAudit(actorId, "update_expense", "cashflow_entry", cashflowId,
                "{\"settlementMethod\":\"" + request.settlementMethod() + "\"}");
        return new AdminRecordCreateResponse(cashflowId, current.getTransactionNo());
    }

    @Transactional
    public void deleteExpense(Long actorId, Long cashflowId) {
        ExpenseContext current = expenseContext(cashflowId);
        requireExpenseEditable(current);
        reverseExpenseReserve(actorId, current, "支出作废，回冲预备金扣款");
        mapper.clearExpenseReserve(cashflowId);
        if (mapper.voidExpenseFinance(current.getFinanceRecordId()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Expense record changed; please reload");
        }
        mapper.insertCreateAudit(actorId, "delete_expense", "cashflow_entry", cashflowId,
                "{\"status\":\"voided\"}");
    }

    @Transactional
    public AdminRecordCreateResponse createMaintenance(Long actorId, AdminMaintenanceCreateRequest request) {
        UnitContext unit = unitContext(request.unitId());
        String reference = reference("MWO");
        NewWorkOrder workOrder = new NewWorkOrder();
        workOrder.setWorkOrderNo(reference); workOrder.setUnitId(unit.getUnitId()); workOrder.setOwnerId(unit.getOwnerId());
        workOrder.setVendorId(request.vendorId()); workOrder.setCategory(request.category()); workOrder.setTitle(request.title());
        workOrder.setDescription(request.description()); workOrder.setRequestedAt(request.requestedAt());
        workOrder.setEstimatedAmount(request.estimatedAmount()); workOrder.setActorId(actorId);
        mapper.insertWorkOrder(workOrder);
        mapper.insertSubmittedHistory(workOrder.getId(), request.requestedAt(), actorId);
        mapper.insertCreateAudit(actorId, "create_maintenance", "maintenance_work_order", workOrder.getId(),
                "{\"workOrderNo\":\"" + reference + "\"}");
        return new AdminRecordCreateResponse(workOrder.getId(), reference);
    }

    @Transactional
    public AdminPropertyMaintenanceResponse updateMaintenance(Long actorId, Long workOrderId,
            AdminPropertyMaintenanceUpdateRequest request) {
        CompletionContext current = context(workOrderId);
        if (current.getOwnerId() == null || current.getOwnerUnitId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The maintenance record is not linked to an owner unit");
        }
        return updateForProperty(actorId, current.getOwnerId(), current.getOwnerUnitId(), workOrderId, request);
    }

    @Transactional
    public void deleteMaintenance(Long actorId, Long workOrderId) {
        CompletionContext current = context(workOrderId);
        if (current.getOwnerId() == null || current.getOwnerUnitId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The maintenance record is not linked to an owner unit");
        }
        cancelForProperty(actorId, current.getOwnerId(), current.getOwnerUnitId(), workOrderId);
    }

    @Transactional
    public AdminMaintenanceHandlingResponse handling(Long workOrderId) {
        CompletionContext context = context(workOrderId);
        return new AdminMaintenanceHandlingResponse(context.getId(), context.getStatus(),
                zero(context.getReserveBalance()), zero(context.getReserveDeductedAmount()),
                context.getReserveAccountId() != null, context.isDirectPaymentAllowed(),
                mapper.countPhotos(workOrderId, "before_photo"), mapper.countPhotos(workOrderId, "after_photo"));
    }

    @Transactional
    public MaintenanceDetailResponse complete(Long actorId, Long workOrderId, AdminMaintenanceCompleteRequest request) {
        CompletionContext context = context(workOrderId);
        if ("completed".equals(context.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Maintenance work order is already completed");
        }
        if ("cancelled".equals(context.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled work orders cannot be completed");
        }
        if (!SETTLEMENT_METHODS.contains(request.settlementMethod())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid settlement method");
        }
        requireDirectPaymentAllowed(request.settlementMethod(), context.isDirectPaymentAllowed());
        if (context.getOwnerId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The work order is not linked to an owner");
        }
        if (mapper.countPhotos(workOrderId, "before_photo") < 1 || mapper.countPhotos(workOrderId, "after_photo") < 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "At least one before photo and one after photo are required");
        }

        BigDecimal amount = request.actualAmount();
        BigDecimal alreadyDeducted = zero(context.getReserveDeductedAmount());
        if ("direct_payment".equals(request.settlementMethod()) && alreadyDeducted.signum() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This work order already has a reserve deduction and must use reserve settlement");
        }
        if ("reserve".equals(request.settlementMethod())) {
            if (context.getReserveAccountId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No reserve account is linked to this unit");
            }
        }

        String paymentMethod = "reserve".equals(request.settlementMethod()) ? "reserve_account" : "direct_payment";
        Long financeRecordId = ensureFinance(context, actorId, amount, paymentMethod);
        Long reserveAccountId = "reserve".equals(request.settlementMethod()) ? context.getReserveAccountId() : null;
        Long cashflowEntryId = ensureCashflow(context, financeRecordId, reserveAccountId, request.completionNote());

        if ("reserve".equals(request.settlementMethod())) {
            mapper.linkReserveDebitToWorkOrder(financeRecordId, workOrderId);
            BigDecimal deductedAmount = zero(mapper.findReserveDebitAmount(financeRecordId, workOrderId));
            if (deductedAmount.compareTo(amount) != 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Reserve deduction did not match the maintenance amount; transaction rolled back");
            }
        }

        mapper.completeWorkOrder(workOrderId, cashflowEntryId, amount);
        mapper.insertHistory(workOrderId, request.completionNote(), actorId);
        mapper.insertAudit(actorId, workOrderId, amount, request.settlementMethod());
        maintenanceRecordService.syncCompletedWorkOrder(actorId, workOrderId, request.completionNote());
        return detailService.getMaintenanceDetail(null, workOrderId);
    }

    private CompletionContext context(Long workOrderId) {
        if (workOrderId == null || workOrderId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid maintenance work order");
        }
        CompletionContext context = mapper.lockContext(workOrderId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance work order not found");
        return context;
    }

    private PropertyContext propertyContext(Long ownerId, Long ownerUnitId) {
        if (ownerId == null || ownerId <= 0 || ownerUnitId == null || ownerUnitId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid property");
        }
        PropertyContext context = mapper.findPropertyContext(ownerId, ownerUnitId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        return context;
    }

    private AdminPropertyMaintenanceResponse propertyMaintenance(Long unitId, Long workOrderId) {
        if (workOrderId == null || workOrderId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid maintenance record");
        }
        AdminPropertyMaintenanceResponse result = mapper.findPropertyMaintenanceById(unitId, workOrderId);
        if (result == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance record not found");
        return result;
    }

    private void validateVendor(Long vendorId) {
        if (vendorId != null && mapper.findVendorOptions().stream()
                .noneMatch(vendor -> vendorId.equals(vendor.id()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid maintenance vendor");
        }
    }

    private UnitContext unitContext(Long unitId) {
        if (unitId == null || unitId <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid unit");
        UnitContext context = mapper.lockUnitContext(unitId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Operating owner unit not found");
        return context;
    }

    private String reference(String prefix) {
        return prefix + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private void validateExpenseSettlement(String settlementMethod) {
        if (!Set.of("reserve", "direct_payment", "unpaid").contains(settlementMethod)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid settlement method");
        }
    }

    private void requireDirectPaymentAllowed(String settlementMethod, boolean allowed) {
        if ("direct_payment".equals(settlementMethod) && !allowed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "业主已解约，代付款已停用，不能再提交或确认出款");
        }
    }

    private ExpenseContext expenseContext(Long cashflowId) {
        if (cashflowId == null || cashflowId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid expense record");
        }
        ExpenseContext context = mapper.lockExpense(cashflowId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense record not found");
        return context;
    }

    private void requireExpenseEditable(ExpenseContext context) {
        if (context.getWorkOrderId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Maintenance expenses must be changed from the maintenance work order");
        }
        if ("voided".equals(context.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The expense record has already been deleted");
        }
        if ("exported".equals(context.getSyncStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Exported expense records cannot be modified; reopen them in finance first");
        }
    }

    private void reverseExpenseReserve(Long actorId, ExpenseContext context, String note) {
        BigDecimal amount = zero(context.getReserveDebitAmount());
        if (context.getReserveAccountId() == null || amount.signum() <= 0) return;
        if (mapper.restoreExpenseReserve(context.getReserveAccountId(), amount) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to restore reserve balance");
        }
        BigDecimal balanceAfter = zero(mapper.findExpenseReserveBalance(context.getReserveAccountId()));
        if (mapper.reverseExpenseReserveDebits(context.getFinanceRecordId()) < 1
                || mapper.insertExpenseReserveTransaction(context.getReserveAccountId(), context.getFinanceRecordId(),
                        "adjustment", amount, balanceAfter, note, actorId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to record reserve reversal");
        }
    }

    private void debitExpenseReserve(Long actorId, Long financeRecordId, Long reserveAccountId, BigDecimal amount) {
        if (mapper.debitExpenseReserve(reserveAccountId, amount) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to debit reserve balance");
        }
        BigDecimal balanceAfter = zero(mapper.findExpenseReserveBalance(reserveAccountId));
        if (mapper.insertExpenseReserveTransaction(reserveAccountId, financeRecordId, "debit", amount,
                balanceAfter, "支出由预备金扣除", actorId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to record reserve debit");
        }
    }

    private Long ensureFinance(CompletionContext context, Long actorId, BigDecimal amount, String method) {
        String confirmationStatus = "reserve_account".equals(method) ? "confirmed" : "pending";
        if (context.getFinanceRecordId() != null) {
            mapper.updateFinance(context.getFinanceRecordId(), amount, method, confirmationStatus, actorId);
            return context.getFinanceRecordId();
        }
        NewFinance finance = new NewFinance();
        finance.setTransactionNo("EXP-MNT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + context.getId());
        finance.setUnitId(context.getUnitId()); finance.setOwnerId(context.getOwnerId());
        finance.setAmount(amount); finance.setPaymentMethod(method);
        finance.setConfirmationStatus(confirmationStatus); finance.setActorId(actorId);
        mapper.insertFinance(finance);
        return finance.getId();
    }

    private Long ensureCashflow(CompletionContext context, Long financeRecordId,
            Long reserveAccountId, String description) {
        if (context.getCashflowEntryId() != null) {
            mapper.updateCashflow(context.getCashflowEntryId(), reserveAccountId, description);
            return context.getCashflowEntryId();
        }
        NewCashflow cashflow = new NewCashflow();
        cashflow.setFinanceRecordId(financeRecordId); cashflow.setUnitId(context.getUnitId());
        cashflow.setOwnerId(context.getOwnerId()); cashflow.setVendorId(context.getVendorId());
        cashflow.setDescription(description); cashflow.setReserveAccountId(reserveAccountId);
        mapper.insertCashflow(cashflow);
        return cashflow.getId();
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
