package com.ccps.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
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
import com.ccps.backend.dto.MaintenanceDetailResponse;
import com.ccps.backend.mapper.AdminMaintenanceMapper;
import com.ccps.backend.mapper.AdminMaintenanceMapper.CompletionContext;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewCashflow;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewFinance;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewExpenseCashflow;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewExpenseFinance;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewWorkOrder;
import com.ccps.backend.mapper.AdminMaintenanceMapper.UnitContext;

@Service
public class AdminMaintenanceService {
    private static final Set<String> SETTLEMENT_METHODS = Set.of("reserve", "direct_payment");

    private final AdminMaintenanceMapper mapper;
    private final OwnerExpenseMaintenanceService detailService;

    public AdminMaintenanceService(AdminMaintenanceMapper mapper, OwnerExpenseMaintenanceService detailService) {
        this.mapper = mapper;
        this.detailService = detailService;
    }

    @Transactional(readOnly = true)
    public AdminMaintenanceOptionsResponse options() {
        return new AdminMaintenanceOptionsResponse(mapper.findUnitOptions(), mapper.findVendorOptions());
    }

    @Transactional
    public AdminRecordCreateResponse createExpense(Long actorId, AdminExpenseCreateRequest request) {
        if (!Set.of("reserve", "direct_payment", "unpaid").contains(request.settlementMethod())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid settlement method");
        }
        UnitContext unit = unitContext(request.unitId());
        boolean reserve = "reserve".equals(request.settlementMethod());
        if (reserve && unit.getReserveAccountId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No reserve account is linked to this unit");
        }
        if (reserve && zero(unit.getReserveBalance()).compareTo(request.amount()) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient reserve balance");
        }

        String reference = reference("EXP");
        NewExpenseFinance finance = new NewExpenseFinance();
        finance.setTransactionNo(reference); finance.setUnitId(unit.getUnitId()); finance.setOwnerId(unit.getOwnerId());
        finance.setAmount(request.amount()); finance.setOccurredOn(request.occurredOn()); finance.setActorId(actorId);
        finance.setPaymentMethod(reserve ? "reserve_account" : "direct_payment".equals(request.settlementMethod()) ? "direct_payment" : null);
        finance.setPaymentStatus("unpaid".equals(request.settlementMethod()) ? "unpaid" : "paid");
        finance.setConfirmationStatus(reserve ? "confirmed"
                : "unpaid".equals(request.settlementMethod()) ? "pending" : "not_required");
        mapper.insertExpenseFinance(finance);

        NewExpenseCashflow cashflow = new NewExpenseCashflow();
        cashflow.setFinanceRecordId(finance.getId()); cashflow.setUnitId(unit.getUnitId()); cashflow.setOwnerId(unit.getOwnerId());
        cashflow.setCategory(request.category()); cashflow.setDescription(request.description());
        cashflow.setOccurredOn(request.occurredOn()); cashflow.setReserveAccountId(reserve ? unit.getReserveAccountId() : null);
        mapper.insertExpenseCashflow(cashflow);

        // Reserve expenses are deducted atomically by the database reserve-policy trigger
        // when the cashflow row is inserted. Direct payments use not_required so the
        // trigger cannot silently convert the administrator's choice into a reserve debit.
        mapper.insertCreateAudit(actorId, "create_expense", "cashflow_entry", cashflow.getId(),
                "{\"transactionNo\":\"" + reference + "\"}");
        return new AdminRecordCreateResponse(cashflow.getId(), reference);
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
    public AdminMaintenanceHandlingResponse handling(Long workOrderId) {
        CompletionContext context = context(workOrderId);
        return new AdminMaintenanceHandlingResponse(context.getId(), context.getStatus(),
                zero(context.getReserveBalance()), zero(context.getReserveDeductedAmount()),
                context.getReserveAccountId() != null,
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
        BigDecimal remainingDebit = amount.subtract(alreadyDeducted).max(BigDecimal.ZERO);
        if ("reserve".equals(request.settlementMethod())) {
            if (context.getReserveAccountId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No reserve account is linked to this unit");
            }
            if (zero(context.getReserveBalance()).compareTo(remainingDebit) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Insufficient reserve balance; choose direct payment instead");
            }
        }

        String paymentMethod = "reserve".equals(request.settlementMethod()) ? "reserve_account" : "direct_payment";
        Long financeRecordId = ensureFinance(context, actorId, amount, paymentMethod);
        Long reserveAccountId = "reserve".equals(request.settlementMethod()) ? context.getReserveAccountId() : null;
        Long cashflowEntryId = ensureCashflow(context, financeRecordId, reserveAccountId, request.completionNote());

        if ("reserve".equals(request.settlementMethod()) && remainingDebit.signum() > 0) {
            if (mapper.deductReserve(context.getReserveAccountId(), remainingDebit) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve balance changed; please retry");
            }
            BigDecimal balanceAfter = zero(mapper.findReserveBalance(context.getReserveAccountId()));
            mapper.insertReserveDebit(context.getReserveAccountId(), financeRecordId, workOrderId,
                    remainingDebit, balanceAfter, "維修工單 " + workOrderId + " 完成扣款", actorId);
        }

        mapper.completeWorkOrder(workOrderId, cashflowEntryId, amount);
        mapper.insertHistory(workOrderId, request.completionNote(), actorId);
        mapper.insertAudit(actorId, workOrderId, amount, request.settlementMethod());
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

    private Long ensureFinance(CompletionContext context, Long actorId, BigDecimal amount, String method) {
        if (context.getFinanceRecordId() != null) {
            mapper.updateFinance(context.getFinanceRecordId(), amount, method, actorId);
            return context.getFinanceRecordId();
        }
        NewFinance finance = new NewFinance();
        finance.setTransactionNo("EXP-MNT-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + context.getId());
        finance.setUnitId(context.getUnitId()); finance.setOwnerId(context.getOwnerId());
        finance.setAmount(amount); finance.setPaymentMethod(method); finance.setActorId(actorId);
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
