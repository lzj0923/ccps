package com.ccps.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import com.ccps.backend.mapper.AdminReserveManagementMapper.ReconciliationRow;

@Service
public class AdminReserveManagementService {
    private static final Set<String> PAYMENT_METHODS = Set.of("bank_transfer", "online_payment", "cash", "cheque", "other");
    private final AdminReserveManagementMapper mapper;
    public AdminReserveManagementService(AdminReserveManagementMapper mapper) { this.mapper = mapper; }

    @Transactional(readOnly = true)
    public AdminReserveManagementResponse overview() {
        SummaryRow row = mapper.findSummary();
        return new AdminReserveManagementResponse(
                new AdminReserveManagementResponse.Summary(zero(row == null ? null : row.getTotalBalance()),
                        zero(row == null ? null : row.getMinimumBalance()), count(row == null ? null : row.getAccountCount()),
                        count(row == null ? null : row.getLowBalanceCount()), zero(row == null ? null : row.getMonthlyTopups()),
                        zero(row == null ? null : row.getMonthlyDebits()), count(row == null ? null : row.getPendingTopupCount()),
                        zero(row == null ? null : row.getPendingTopupAmount())),
                mapper.findProjects(), mapper.findAccounts(), mapper.findTransactions());
    }

    @Transactional
    public void updateSettings(Long actorId, Long accountId, AdminReserveSettingsRequest request) {
        SettingsRow before = mapper.findSettings(accountId);
        if (before == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserve account not found");
        if (mapper.updateSettings(accountId, request.minimumBalance(), request.lowBalanceAlertEnabled()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve account settings were not updated");
        }
        mapper.insertSettingsAudit(actorId, accountId, zero(before.getMinimumBalance()),
                Boolean.TRUE.equals(before.getLowBalanceAlertEnabled()), request.minimumBalance(),
                request.lowBalanceAlertEnabled());
    }

    @Transactional
    public AdminRecordCreateResponse directTopup(Long actorId, Long accountId, AdminReserveDirectTopupRequest request) {
        if (!PAYMENT_METHODS.contains(request.paymentMethod())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reserve top-up payment method");
        }
        if (request.paymentDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reserve top-up date cannot be in the future");
        }
        if (!"cash".equals(request.paymentMethod()) && blank(request.bankReference())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment reference is required for non-cash top-ups");
        }
        DirectTopupContext context = mapper.findDirectTopupContext(accountId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserve account not found");

        LocalDateTime now = LocalDateTime.now();
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String transactionNo = "RTU-ADM-" + timestamp + "-" + token;
        String receiptNo = "RTU-RCP-" + timestamp + "-" + token;
        BigDecimal balanceAfter = zero(context.getCurrentBalance()).add(request.amount());
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
        mapper.insertDirectTopupTransaction(accountId, finance.getId(), request.amount(),
                request.paymentDate().atStartOfDay(), balanceAfter, note, actorId);
        if (mapper.updateDirectTopupBalance(accountId, balanceAfter) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve balance was not updated");
        }
        mapper.insertDirectTopupNotification(context.getUserId(), context.getOwnerId(), finance.getId(),
                "%s %s 預備金已由管理員充值 RM %s。".formatted(context.getProjectName(), context.getUnitNo(),
                        request.amount().setScale(2).toPlainString()));
        mapper.insertDirectTopupAudit(actorId, finance.getId(), accountId, request.amount(), balanceAfter, note);
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
        refund.setAmount(request.amount()); refund.setPaymentDate(LocalDate.now()); refund.setPaymentMethod(request.paymentMethod()); refund.setActorId(actorId);
        String note = blank(request.note()) ? "業主預備金返還" : request.note().trim();
        if (mapper.insertReserveRefundFinance(refund) != 1 || refund.getId() == null
                || mapper.insertReserveRefundCashflow(refund.getId(), context.getUnitId(), context.getOwnerId(), note) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve refund could not be created");
        }
        mapper.insertReserveRefundAudit(actorId, refund.getId(), accountId, request.amount(), note);
        return new AdminRecordCreateResponse(refund.getId(), transactionNo);
    }

    @Transactional
    public List<AdminRecordCreateResponse> createRefunds(Long actorId, AdminReserveBatchRefundRequest request) {
        return request.items().stream()
                .map(item -> createRefund(actorId, item.accountId(),
                        new AdminReserveRefundRequest(item.amount(), request.paymentMethod(), request.note())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminReserveReconciliationResponse> findReconciliations() {
        return mapper.findReconciliations().stream().map(this::toReconciliation).toList();
    }

    @Transactional
    public List<AdminReserveReconciliationResponse> saveReconciliation(Long actorId,
            AdminReserveReconciliationRequest request) {
        LocalDate month = request.reconciliationMonth().withDayOfMonth(1);
        BigDecimal systemBalance = zero(mapper.findCurrentTotalBalance());
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

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private long count(Long value) { return value == null ? 0 : value; }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
