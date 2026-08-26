package com.ccps.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminReserveManagementResponse;
import com.ccps.backend.dto.AdminReserveSettingsRequest;
import com.ccps.backend.dto.AdminReserveDirectTopupRequest;
import com.ccps.backend.dto.AdminRecordCreateResponse;
import com.ccps.backend.dto.AdminReserveRefundRequest;
import com.ccps.backend.dto.AdminReserveBatchRefundRequest;
import com.ccps.backend.dto.AdminReserveReconciliationRequest;
import com.ccps.backend.dto.AdminReserveReconciliationResponse;
import com.ccps.backend.mapper.AdminReserveManagementMapper;
import com.ccps.backend.mapper.AdminReserveManagementMapper.SummaryRow;
import com.ccps.backend.mapper.AdminReserveManagementMapper.SettingsRow;
import com.ccps.backend.mapper.AdminReserveManagementMapper.DirectTopupContext;
import com.ccps.backend.mapper.AdminReserveManagementMapper.DirectTopupRecord;
import com.ccps.backend.mapper.AdminReserveManagementMapper.RefundBankContext;
import com.ccps.backend.mapper.AdminReserveManagementMapper.ReconciliationRow;

@Service
public class AdminReserveManagementService {
    private static final Set<String> PAYMENT_METHODS = Set.of("bank_transfer", "online_payment", "cash", "cheque", "other");
    private final AdminReserveManagementMapper mapper;
    private final ReserveTargetPolicyService reserveTargetPolicyService;
    public AdminReserveManagementService(AdminReserveManagementMapper mapper,
            ReserveTargetPolicyService reserveTargetPolicyService) {
        this.mapper = mapper;
        this.reserveTargetPolicyService = reserveTargetPolicyService;
    }

    @Transactional(readOnly = true)
    public AdminReserveManagementResponse overview() {
        SummaryRow row = mapper.findSummary();
        return new AdminReserveManagementResponse(
                new AdminReserveManagementResponse.Summary(zero(row == null ? null : row.getTotalBalance()),
                        zero(row == null ? null : row.getAccountingBalance()),
                        zero(row == null ? null : row.getMinimumBalance()), count(row == null ? null : row.getAccountCount()),
                        count(row == null ? null : row.getLowBalanceCount()), zero(row == null ? null : row.getMonthlyTopups()),
                        zero(row == null ? null : row.getMonthlyDebits()), count(row == null ? null : row.getPendingTopupCount()),
                        zero(row == null ? null : row.getPendingTopupAmount())),
                mapper.findProjects(), mapper.findAccounts(), mapper.findTransactions(), mapper.findRefundBankAccounts());
    }

    @Transactional
    public void updateSettings(Long actorId, Long accountId, AdminReserveSettingsRequest request) {
        SettingsRow before = mapper.findSettings(accountId);
        if (before == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserve account not found");
        boolean automatic = Boolean.TRUE.equals(request.automaticCalculation());
        if (!automatic && request.minimumBalance() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manual reserve minimum is required");
        }
        String mode = automatic ? "auto" : "manual";
        BigDecimal effectiveMinimum = automatic ? zero(before.getCalculatedMinimumBalance()) : request.minimumBalance();
        String remarks = normalizeRemarks(request.remarks());
        if (mapper.updateSettings(accountId, mode, effectiveMinimum, request.lowBalanceAlertEnabled(), remarks) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve account settings were not updated");
        }
        mapper.insertSettingsAudit(actorId, accountId, zero(before.getMinimumBalance()),
                before.getMinimumBalanceMode(), Boolean.TRUE.equals(before.getLowBalanceAlertEnabled()), effectiveMinimum,
                mode, request.lowBalanceAlertEnabled(), before.getRemarks(), remarks);
        if (automatic) reserveTargetPolicyService.recalculate(accountId);
    }

    @Transactional
    public AdminRecordCreateResponse directTopup(Long actorId, Long accountId, AdminReserveDirectTopupRequest request) {
        if (!PAYMENT_METHODS.contains(request.paymentMethod())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reserve top-up payment method");
        }
        if (request.paymentDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reserve top-up date cannot be in the future");
        }
        DirectTopupContext context = mapper.findDirectTopupContext(accountId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserve account not found");

        LocalDateTime now = LocalDateTime.now();
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String transactionNo = "RTU-ADM-" + timestamp + "-" + token;
        String receiptNo = "RTU-RCP-" + timestamp + "-" + token;
        BigDecimal projectedBalanceAfter = zero(context.getCurrentBalance()).add(request.amount());
        String payerName = blank(request.payerName()) ? context.getOwnerName() : request.payerName().trim();
        String bankReference = blank(request.bankReference()) ? null : request.bankReference().trim();
        String note = blank(request.note()) ? "管理員直接充值" : request.note().trim();

        DirectTopupRecord finance = new DirectTopupRecord();
        finance.setTransactionNo(transactionNo); finance.setUnitId(context.getUnitId());
        finance.setOwnerId(context.getOwnerId()); finance.setAmount(request.amount());
        finance.setPaymentDate(request.paymentDate()); finance.setPaymentMethod(request.paymentMethod());
        finance.setActorId(actorId);
        mapper.insertDirectTopupFinance(finance);
        mapper.insertDirectTopupReceipt(finance.getId(), receiptNo, payerName, bankReference, note);
        mapper.insertDirectTopupAudit(actorId, finance.getId(), accountId, request.amount(), projectedBalanceAfter, note);
        return new AdminRecordCreateResponse(finance.getId(), transactionNo);
    }

    @Transactional
    public AdminRecordCreateResponse createRefund(Long actorId, Long accountId, AdminReserveRefundRequest request) {
        if (!PAYMENT_METHODS.contains(request.paymentMethod())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reserve refund payment method");
        }
        DirectTopupContext context = mapper.findDirectTopupContext(accountId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserve account not found");
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String transactionNo = "RRF-ADM-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + token;
        DirectTopupRecord refund = new DirectTopupRecord();
        refund.setTransactionNo(transactionNo); refund.setUnitId(context.getUnitId()); refund.setOwnerId(context.getOwnerId());
        refund.setAmount(request.amount()); refund.setPaymentDate(request.paymentDate()); refund.setPaymentMethod(request.paymentMethod()); refund.setActorId(actorId);
        String note = blank(request.note()) ? "業主預備金返還" : request.note().trim();
        if (mapper.insertReserveRefundFinance(refund) != 1 || refund.getId() == null
                || mapper.insertReserveRefundCashflow(refund.getId(), context.getUnitId(), context.getOwnerId(), note,
                        requestDate(refund)) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve refund could not be created");
        }
        mapper.insertReserveRefundAudit(actorId, refund.getId(), accountId, request.amount(), note);
        return new AdminRecordCreateResponse(refund.getId(), transactionNo);
    }

    @Transactional
    public List<AdminRecordCreateResponse> createRefunds(Long actorId, AdminReserveBatchRefundRequest request) {
        if (!PAYMENT_METHODS.contains(request.paymentMethod())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reserve refund payment method");
        }
        String batchReference = "RRF-BATCH-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + token();
        List<AdminRecordCreateResponse> responses = new ArrayList<>();
        for (AdminReserveBatchRefundRequest.Item item : request.items()) {
            RefundBankContext context = mapper.findRefundBankContext(item.accountId(), item.bankAccountId());
            if (context == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Selected reserve account or bank account is unavailable");
            }
            BigDecimal transferLimit = positive(context.getTransferLimit()) ? context.getTransferLimit() : item.amount();
            int installmentCount = item.amount().divide(transferLimit, 0, RoundingMode.CEILING).intValueExact();
            if (installmentCount > 365) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Transfer limit is too small: refund would exceed 365 installments");
            }
            BigDecimal remaining = item.amount();
            for (int index = 1; index <= installmentCount; index++) {
                BigDecimal principal = remaining.min(transferLimit);
                LocalDate scheduledDate = request.paymentDate().plusDays(index - 1L);
                String scheduleNote = scheduledRefundNote(request.note(), batchReference, index, installmentCount,
                        scheduledDate);
                AdminRecordCreateResponse refund = createScheduledRefund(actorId, item.accountId(), context,
                        principal, request.paymentMethod(), scheduleNote, scheduledDate);
                Long feeFinanceRecordId = createOverseasFee(actorId, context, batchReference, index,
                        installmentCount, scheduledDate);
                BigDecimal fee = feeFinanceRecordId == null ? BigDecimal.ZERO : zero(context.getOverseasTransferFee());
                if (mapper.insertRefundTransfer(batchReference, item.accountId(), item.bankAccountId(), refund.id(),
                        feeFinanceRecordId, index, installmentCount, scheduledDate, principal, fee, actorId) != 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve refund schedule could not be created");
                }
                responses.add(refund);
                remaining = remaining.subtract(principal);
            }
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public List<AdminReserveReconciliationResponse> findReconciliations() {
        return mapper.findReconciliations().stream().map(this::toReconciliation).toList();
    }

    @Transactional
    public List<AdminReserveReconciliationResponse> saveReconciliation(Long actorId,
            AdminReserveReconciliationRequest request) {
        LocalDate month = request.reconciliationMonth().withDayOfMonth(1);
        BigDecimal systemBalance = zero(mapper.findPostedTotalBalance());
        BigDecimal difference = request.financeBalance().subtract(systemBalance);
        String status = request.confirmed() ? "confirmed" : "pending";
        String note = blank(request.note()) ? null : request.note().trim();
        mapper.saveReconciliation(month, systemBalance, request.financeBalance(), difference, status, note, actorId);
        mapper.insertReconciliationAudit(actorId, month, systemBalance, request.financeBalance(), difference, status, note);
        return findReconciliations();
    }

    private AdminReserveReconciliationResponse toReconciliation(ReconciliationRow row) {
        return new AdminReserveReconciliationResponse(row.getId(), row.getReconciliationMonth(),
                row.getSystemBalance(), row.getFinanceBalance(), row.getDifferenceAmount(), row.getStatus(),
                row.getNote(), row.getConfirmedByName(), row.getConfirmedAt());
    }

    private AdminRecordCreateResponse createScheduledRefund(Long actorId, Long accountId, RefundBankContext context,
            BigDecimal amount, String paymentMethod, String note, LocalDate scheduledDate) {
        String transactionNo = "RRF-ADM-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + token();
        DirectTopupRecord refund = new DirectTopupRecord();
        refund.setTransactionNo(transactionNo);
        refund.setUnitId(context.getUnitId());
        refund.setOwnerId(context.getOwnerId());
        refund.setAmount(amount);
        refund.setPaymentDate(scheduledDate);
        refund.setPaymentMethod(paymentMethod);
        refund.setActorId(actorId);
        if (mapper.insertReserveRefundFinance(refund) != 1 || refund.getId() == null
                || mapper.insertReserveRefundCashflow(refund.getId(), context.getUnitId(), context.getOwnerId(), note,
                        scheduledDate) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve refund could not be created");
        }
        mapper.insertReserveRefundAudit(actorId, refund.getId(), accountId, amount, note);
        return new AdminRecordCreateResponse(refund.getId(), transactionNo);
    }

    private Long createOverseasFee(Long actorId, RefundBankContext context, String batchReference,
            int installmentNo, int installmentCount, LocalDate scheduledDate) {
        BigDecimal fee = zero(context.getOverseasTransferFee());
        if (!Boolean.TRUE.equals(context.getOverseasBank()) || fee.signum() <= 0) return null;
        DirectTopupRecord feeRecord = new DirectTopupRecord();
        feeRecord.setTransactionNo("MANUAL-CF-BANK-FEE-" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "-" + token());
        feeRecord.setUnitId(context.getUnitId());
        feeRecord.setOwnerId(context.getOwnerId());
        feeRecord.setAmount(fee);
        feeRecord.setPaymentDate(scheduledDate);
        feeRecord.setActorId(actorId);
        if (mapper.insertOverseasFeeFinance(feeRecord) != 1 || feeRecord.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Overseas transfer fee could not be created");
        }
        String bankName = blank(context.getBankName()) ? "海外银行" : context.getBankName().trim();
        String description = "海外银行汇款手续费 · %s · 备用金返还 %d/%d · %s".formatted(
                bankName, installmentNo, installmentCount, batchReference);
        if (mapper.insertOverseasFeeCashflow(feeRecord.getId(), context.getUnitId(), context.getOwnerId(),
                description, scheduledDate) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Overseas transfer fee cashflow could not be created");
        }
        return feeRecord.getId();
    }

    private String scheduledRefundNote(String note, String batchReference, int installmentNo, int installmentCount,
            LocalDate scheduledDate) {
        String reason = blank(note) ? "业主备用金返还" : note.trim();
        return "%s（%s，第 %d/%d 笔，计划日期 %s）".formatted(reason, batchReference, installmentNo,
                installmentCount, scheduledDate);
    }

    private LocalDate requestDate(DirectTopupRecord record) {
        return record.getPaymentDate() == null ? LocalDate.now() : record.getPaymentDate();
    }

    private String token() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private boolean positive(BigDecimal value) { return value != null && value.signum() > 0; }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private long count(Long value) { return value == null ? 0 : value; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String normalizeRemarks(String value) { return blank(value) ? null : value.trim(); }
}
