package com.ccps.backend.service;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminFinanceReviewResponse;
import com.ccps.backend.mapper.AdminFinanceReviewMapper;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.FinanceReviewRow;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.FinanceAllocationNoteContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.FinanceSummaryRow;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.FinanceSourceLink;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ProofFile;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReopenRecordContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.RentCreditAllocationReopenContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.RentPaymentReopenContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReviewActionContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.TenantChargeReviewContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReserveRefundContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReserveRefundReopenContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReserveTopupReopenContext;

@Service
public class AdminFinanceReviewService {
    private final AdminFinanceReviewMapper mapper;
    private final ReserveTopupService reserveTopupService;
    private final Path proofStorageRoot;
    private final FinanceDocumentSettings financeDocumentSettings;

    @Autowired
    public AdminFinanceReviewService(AdminFinanceReviewMapper mapper,
            ReserveTopupService reserveTopupService,
            @Value("${ccps.storage.payment-proofs:uploads/payment-proofs}") String proofStorageRoot,
            @Value("${ccps.finance-documents.company-name:CCPS PROPERTY MANAGEMENT SDN. BHD.}") String companyName,
            @Value("${ccps.finance-documents.company-registration:}") String companyRegistration,
            @Value("${ccps.finance-documents.address-line-1:}") String addressLine1,
            @Value("${ccps.finance-documents.address-line-2:}") String addressLine2,
            @Value("${ccps.finance-documents.bank-account-holder:CCPS PROPERTY MANAGEMENT SDN. BHD.}") String bankAccountHolder,
            @Value("${ccps.finance-documents.bank-name:}") String bankName,
            @Value("${ccps.finance-documents.bank-account-no:}") String bankAccountNo) {
        this.mapper = mapper;
        this.reserveTopupService = reserveTopupService;
        this.proofStorageRoot = Path.of(proofStorageRoot).toAbsolutePath().normalize();
        this.financeDocumentSettings = new FinanceDocumentSettings(companyName, companyRegistration,
                addressLine1, addressLine2, bankAccountHolder, bankName, bankAccountNo);
    }

    /** Kept for isolated unit tests that do not exercise reserve top-ups. */
    public AdminFinanceReviewService(AdminFinanceReviewMapper mapper,
            @Value("${ccps.storage.payment-proofs:uploads/payment-proofs}") String proofStorageRoot) {
        this.mapper = mapper;
        this.reserveTopupService = null;
        this.proofStorageRoot = Path.of(proofStorageRoot).toAbsolutePath().normalize();
        this.financeDocumentSettings = FinanceDocumentSettings.defaults();
    }

    @Transactional(readOnly = true)
    public AdminFinanceReviewResponse findReviews(int requestedPage, int requestedPageSize,
            String keyword, String projectName, String status, LocalDate startDate, LocalDate endDate) {
        return findReviews("property", requestedPage, requestedPageSize, keyword, projectName, status, startDate, endDate);
    }

    @Transactional(readOnly = true)
    public AdminFinanceReviewResponse findReviews(String reviewType, int requestedPage, int requestedPageSize,
            String keyword, String projectName, String status, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must not be before start date");
        }
        int pageSize = Math.max(1, Math.min(requestedPageSize, 100));
        String confirmationStatus = normalizeConfirmationStatus(status);
        String syncStatus = normalizeSyncStatus(status);
        boolean reserve = "reserve".equals(reviewType);
        boolean expense = "expense".equals(reviewType) || "cashflow_maintenance".equals(reviewType);
        boolean tenantCharge = "tenant_charge".equals(reviewType);
        boolean settlement = "reserve_refund".equals(reviewType) || "tenant_deposit".equals(reviewType);
        long totalRows = zero(reserve
                ? mapper.countReservePage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus, startDate, endDate)
                : settlement ? mapper.countSettlementPage(reviewType, normalize(keyword), normalize(projectName), confirmationStatus, syncStatus, startDate, endDate)
                : tenantCharge ? mapper.countTenantChargePage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus, startDate, endDate)
                : expense ? mapper.countExpensePage(reviewType, normalize(keyword), normalize(projectName), confirmationStatus, syncStatus, startDate, endDate)
                : mapper.countPage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus, startDate, endDate));
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        List<FinanceReviewRow> sourceRows = reserve
                ? mapper.findReservePage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus,
                        startDate, endDate, pageSize, (page - 1) * pageSize)
                : settlement ? mapper.findSettlementPage(reviewType, normalize(keyword), normalize(projectName), confirmationStatus, syncStatus,
                        startDate, endDate, pageSize, (page - 1) * pageSize)
                : tenantCharge ? mapper.findTenantChargePage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus,
                        startDate, endDate, pageSize, (page - 1) * pageSize)
                : expense ? mapper.findExpensePage(reviewType, normalize(keyword), normalize(projectName), confirmationStatus, syncStatus,
                        startDate, endDate, pageSize, (page - 1) * pageSize)
                : mapper.findPage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus,
                        startDate, endDate, pageSize, (page - 1) * pageSize);
        Map<Long, FinanceSourceLink> sourceLinks = new HashMap<>();
        if (!sourceRows.isEmpty()) {
            mapper.findSourceLinks(sourceRows.stream().map(FinanceReviewRow::getId).toList())
                    .forEach(link -> sourceLinks.putIfAbsent(link.getFinanceRecordId(), link));
        }
        List<AdminFinanceReviewResponse.Item> rows = sourceRows
                .stream().map(row -> toItem(row, sourceLinks.get(row.getId()))).toList();
        FinanceSummaryRow source = reserve ? mapper.findReserveSummary()
                : settlement ? mapper.findSettlementSummary(reviewType)
                : tenantCharge ? mapper.findTenantChargeSummary()
                : expense ? mapper.findExpenseSummary(reviewType) : mapper.findSummary();
        AdminFinanceReviewResponse.Summary summary = new AdminFinanceReviewResponse.Summary(
                source == null ? 0 : zero(source.getPendingCount()),
                source == null ? 0 : zero(source.getConfirmedCount()),
                source == null ? 0 : zero(source.getRejectedCount()),
                source == null ? 0 : zero(source.getPendingSyncCount()),
                source == null ? BigDecimal.ZERO : zero(source.getPendingAmount()),
                source == null ? BigDecimal.ZERO : zero(source.getConfirmedMonthAmount()));
        return new AdminFinanceReviewResponse(summary, rows,
                new AdminFinanceReviewResponse.Page(totalRows, page, pageSize, totalPages));
    }

    @Transactional(readOnly = true)
    public List<String> findProjects() {
        return mapper.findProjects();
    }

    @Transactional(readOnly = true)
    public List<String> findProjects(String reviewType) {
        return "reserve".equals(reviewType) ? mapper.findReserveProjects()
                : ("reserve_refund".equals(reviewType) || "tenant_deposit".equals(reviewType)) ? mapper.findSettlementProjects(reviewType)
                : "tenant_charge".equals(reviewType) ? mapper.findTenantChargeProjects()
                : ("expense".equals(reviewType) || "cashflow_maintenance".equals(reviewType))
                        ? mapper.findExpenseProjects(reviewType) : mapper.findProjects();
    }

    @Transactional
    public void confirm(Long reviewerId, Long financeRecordId, LocalDate transactionDate,
            LocalDate receiptDate, String note) {
        confirmOne(reviewerId, financeRecordId, requiredDate(transactionDate), optionalPastDate(receiptDate), requiredNote(note));
    }

    @Transactional
    public void confirmBatch(Long reviewerId, List<Long> financeRecordIds, LocalDate transactionDate,
            LocalDate receiptDate, String note) {
        confirmBatch(reviewerId, financeRecordIds, transactionDate, receiptDate, note, null);
    }

    @Transactional
    public void confirmBatch(Long reviewerId, List<Long> financeRecordIds, LocalDate transactionDate,
            LocalDate receiptDate, String note, String referenceNo) {
        List<Long> uniqueIds = new LinkedHashSet<>(financeRecordIds).stream().toList();
        if (uniqueIds.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one payment");
        String reference = normalize(referenceNo);
        String reviewNote = normalize(note) == null ? "批量確認收款" : note.trim();
        if (reference != null) reviewNote = "批量编号：" + reference + "；" + reviewNote;
        LocalDate confirmedDate = requiredDate(transactionDate);
        LocalDate receivedDate = optionalPastDate(receiptDate);
        for (Long financeRecordId : uniqueIds) confirmOne(reviewerId, financeRecordId, confirmedDate, receivedDate, reviewNote);
    }

    @Transactional
    public void updateAllocationNote(Long actorId,Long financeRecordId,String value,boolean reuseEnabled) {
        FinanceAllocationNoteContext context=mapper.lockAllocationNoteContext(financeRecordId);
        if(context==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Finance record was not found");
        String note=normalize(value);
        if(note!=null&&note.length()>500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Allocation note is too long");
        if(mapper.updateAllocationNote(financeRecordId,note)!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Allocation note could not be updated");
        mapper.updateLinkedCashflowAllocationNote(financeRecordId,note);
        if(reuseEnabled&&note!=null) mapper.upsertAllocationNoteDefault(context.getUnitId(),context.getRecordType(),note,actorId);
        else if(reuseEnabled) mapper.deleteAllocationNoteDefault(context.getUnitId(),context.getRecordType());
        mapper.insertAllocationNoteAudit(actorId,financeRecordId,note,reuseEnabled);
    }

    private void confirmOne(Long reviewerId, Long financeRecordId, LocalDate transactionDate,
            LocalDate receiptDate, String note) {
        String recordType = mapper.lockRecordType(financeRecordId);
        if (recordType == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Finance record was not found");
        if (mapper.setFinanceConfirmedDate(financeRecordId, transactionDate, receiptDate) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Finance record has already been reviewed");
        }
        mapper.syncCashflowDate(financeRecordId, transactionDate);
        mapper.syncTenantDepositDate(financeRecordId, transactionDate);
        if ("reserve_topup".equals(recordType)) {
            if (reserveTopupService == null) throw new IllegalStateException("Reserve top-up confirmation adapter is unavailable");
            reserveTopupService.reviewFromFinance(reviewerId, financeRecordId, true, transactionDate, note);
            return;
        }
        if ("tenant_charge".equals(recordType)) {
            TenantChargeReviewContext charge = mapper.lockTenantChargeReview(financeRecordId);
            if (charge == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant charge has already been reviewed");
            }
            if (mapper.confirmTenantCharge(financeRecordId, reviewerId) != 1
                    || mapper.increaseTenantChargeInvoice(charge.getInvoiceId(), charge.getAmount()) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant charge could not be confirmed");
            }
            mapper.insertAudit(reviewerId, financeRecordId, "confirm_tenant_charge", "confirmed", note);
            return;
        }
        if ("reserve_refund".equals(recordType)) {
            ReserveRefundContext refund = mapper.lockReserveRefund(financeRecordId);
            if (refund == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve refund was not found");
            BigDecimal balanceAfter = refund.getCurrentBalance().subtract(refund.getAmount());
            if (mapper.confirmReserveRefund(financeRecordId, reviewerId) != 1
                    || mapper.debitReserveBalance(refund.getReserveAccountId(), refund.getAmount()) != 1
                    || mapper.insertReserveRefundTransaction(refund.getReserveAccountId(), financeRecordId,
                            refund.getAmount(), balanceAfter, reviewerId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve refund could not be completed");
            }
            mapper.postTenantDepositRefund(financeRecordId);
            mapper.updateReserveRefundTransferStatus(financeRecordId, "completed");
            mapper.refreshOwnerRemittanceBatchStatus(financeRecordId);
            mapper.insertNotification(refund.getUserId(), refund.getOwnerId(), financeRecordId,
                    "預備金已返還", "%s %s 預備金已返還 RM %s。".formatted(refund.getProjectName(), refund.getUnitNo(), refund.getAmount().setScale(2).toPlainString()), "normal");
            mapper.insertAudit(reviewerId, financeRecordId, "confirm_reserve_refund", "confirmed", note);
            return;
        }
        if ("property_expense".equals(recordType)) {
            if (mapper.countDirectPaymentBlockedByTerminatedMandate(financeRecordId) > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "业主已解约，该代付款已撤出，不能确认出款");
            }
            if (mapper.confirmExpense(financeRecordId, reviewerId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Expense has already been reviewed");
            }
            mapper.insertAudit(reviewerId, financeRecordId, "confirm_property_expense", "confirmed", note);
            return;
        }
        if ("cashflow".equals(recordType)) {
            if (mapper.confirmCashflow(financeRecordId, reviewerId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cashflow has already been reviewed");
            }
            mapper.insertAudit(reviewerId, financeRecordId, "confirm_cashflow", "confirmed", note);
            return;
        }
        if ("security_deposit".equals(recordType)) {
            if (mapper.confirmSecurityDeposit(financeRecordId, reviewerId) != 1
                    || mapper.confirmSecurityDepositEntry(financeRecordId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Security deposit has already been reviewed");
            }
            mapper.insertConfirmedSecurityDepositLedger(financeRecordId, reviewerId);
            mapper.insertAudit(reviewerId, financeRecordId, "confirm_security_deposit", "confirmed", note);
            return;
        }
        if ("security_deposit_forfeiture".equals(recordType)) {
            if (mapper.confirmSecurityDepositForfeiture(financeRecordId, reviewerId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Security deposit forfeiture has already been reviewed");
            }
            mapper.updateSecurityDepositForfeitureLedger(financeRecordId, "pending", "posted");
            mapper.insertAudit(reviewerId, financeRecordId, "confirm_security_deposit_forfeiture", "confirmed", note);
            return;
        }
        ReviewActionContext context = requirePendingReview(financeRecordId);
        if (context.getProofDocumentId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment proof is required before confirmation");
        }
        BigDecimal allocated = zero(context.getAllocatedAmount());
        BigDecimal available = zero(context.getAmountDue()).subtract(zero(context.getAmountPaid())).max(BigDecimal.ZERO);
        if (allocated.signum() <= 0 || allocated.compareTo(zero(context.getAmount())) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment allocation does not match the submitted amount");
        }
        if (allocated.compareTo(available) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment amount exceeds the installment balance");
        }
        if (mapper.confirmFinanceRecord(financeRecordId, reviewerId) != 1
                || mapper.allocateConfirmedPayment(context.getInstallmentId(), allocated) != 1
                || mapper.updateReceiptReview(context.getReceiptId(), note) != 1
                || mapper.reviewDocument(context.getProofDocumentId(), "approved", reviewerId, note) != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment confirmation could not be completed");
        }
        String body = "%s %s 第 %s 期已確認收款 RM %s。".formatted(
                context.getProjectName(), context.getUnitNo(), context.getInstallmentNo(),
                allocated.setScale(2).toPlainString());
        mapper.insertNotification(context.getUserId(), context.getOwnerId(), financeRecordId,
                "房款已確認", body, "normal");
        mapper.insertAudit(reviewerId, financeRecordId, "confirm_property_payment", "confirmed", note);
    }

    @Transactional
    public void reject(Long reviewerId, Long financeRecordId, String note) {
        String reviewNote = requiredNote(note);
        String recordType = mapper.lockRecordType(financeRecordId);
        if (recordType == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Finance record was not found");
        if ("reserve_topup".equals(recordType)) {
            if (reserveTopupService == null) throw new IllegalStateException("Reserve top-up confirmation adapter is unavailable");
            reserveTopupService.reviewFromFinance(reviewerId, financeRecordId, false, null, reviewNote);
            return;
        }
        if ("tenant_charge".equals(recordType)) {
            if (mapper.rejectTenantCharge(financeRecordId, reviewerId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant charge has already been reviewed");
            }
            mapper.insertAudit(reviewerId, financeRecordId, "reject_tenant_charge", "rejected", reviewNote);
            return;
        }
        if ("reserve_refund".equals(recordType)) {
            if (mapper.rejectReserveRefund(financeRecordId, reviewerId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve refund has already been reviewed");
            }
            mapper.cancelTenantDepositRefund(financeRecordId);
            mapper.updateReserveRefundTransferStatus(financeRecordId, "failed");
            mapper.refreshOwnerRemittanceBatchStatus(financeRecordId);
            mapper.insertAudit(reviewerId, financeRecordId, "reject_reserve_refund", "rejected", reviewNote);
            return;
        }
        if ("property_expense".equals(recordType)) {
            if (mapper.rejectExpense(financeRecordId, reviewerId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Expense has already been reviewed");
            }
            Long workOrderId = mapper.findMaintenanceWorkOrderId(financeRecordId);
            if (workOrderId != null && mapper.resetMaintenanceAfterFinanceRejection(workOrderId) == 1) {
                mapper.insertMaintenanceRejectionHistory(workOrderId, reviewerId,
                        "财务退回：请处理维修工单后再次提交确认。" + reviewNote);
            }
            mapper.insertAudit(reviewerId, financeRecordId, "reject_property_expense", "rejected", reviewNote);
            return;
        }
        if ("cashflow".equals(recordType)) {
            if (mapper.rejectCashflow(financeRecordId, reviewerId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cashflow has already been reviewed");
            }
            mapper.insertAudit(reviewerId, financeRecordId, "reject_cashflow", "rejected", reviewNote);
            return;
        }
        if ("security_deposit".equals(recordType)) {
            if (mapper.rejectSecurityDeposit(financeRecordId, reviewerId) != 1
                    || mapper.rejectSecurityDepositEntry(financeRecordId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Security deposit has already been reviewed");
            }
            mapper.insertAudit(reviewerId, financeRecordId, "reject_security_deposit", "rejected", reviewNote);
            return;
        }
        if ("security_deposit_forfeiture".equals(recordType)) {
            if (mapper.rejectSecurityDepositForfeiture(financeRecordId, reviewerId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Security deposit forfeiture has already been reviewed");
            }
            mapper.updateSecurityDepositForfeitureLedger(financeRecordId, "pending", "cancelled");
            mapper.insertAudit(reviewerId, financeRecordId, "reject_security_deposit_forfeiture", "rejected", reviewNote);
            return;
        }
        ReviewActionContext context = requirePendingReview(financeRecordId);
        if (mapper.rejectFinanceRecord(financeRecordId, reviewerId) != 1
                || mapper.updateReceiptReview(context.getReceiptId(), reviewNote) != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment rejection could not be completed");
        }
        if (context.getProofDocumentId() != null
                && mapper.reviewDocument(context.getProofDocumentId(), "needs_changes", reviewerId, reviewNote) != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment proof could not be returned");
        }
        String body = "%s %s 第 %s 期付款憑證需要補件：%s".formatted(
                context.getProjectName(), context.getUnitNo(), context.getInstallmentNo(), reviewNote);
        mapper.insertNotification(context.getUserId(), context.getOwnerId(), financeRecordId,
                "付款憑證退回補件", body, "high");
        mapper.insertAudit(reviewerId, financeRecordId, "reject_property_payment", "rejected", reviewNote);
    }

    @Transactional
    public void reopen(Long reviewerId, Long financeRecordId, String note) {
        String reopenNote = requiredNote(note);
        ReopenRecordContext record = mapper.lockReopenRecord(financeRecordId);
        if (record == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Finance record not found");
        if (!"confirmed".equals(record.getConfirmationStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only confirmed records can be returned to pending");
        }
        if ("synced".equals(record.getSyncStatus()) || record.getSyncBatchId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This record has entered an export or sync batch and cannot be reopened");
        }

        // Reversal removes derived postings and prepayment allocations. Preserve their
        // original values before any mutation, in the same transaction as the reversal.
        if (mapper.insertFinanceReopenSnapshot(reviewerId, financeRecordId, reopenNote) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The original finance snapshot could not be saved; reopening was cancelled");
        }

        switch (record.getRecordType()) {
            case "property_payment" -> reopenPropertyPayment(financeRecordId, reopenNote);
            case "rent_payment" -> {
                reopenRentPayment(financeRecordId);
                if (mapper.voidReopenedRentPayment(financeRecordId) != 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Rent payment could not be reopened");
                }
                mapper.insertRentReopenAudit(reviewerId, financeRecordId, reopenNote);
                return;
            }
            case "reserve_topup" -> reopenReserveTopup(financeRecordId, reopenNote);
            case "security_deposit" -> {
                if (mapper.reopenSecurityDepositEntry(financeRecordId) != 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Security deposit entry could not be reopened");
                }
                mapper.reopenSecurityDepositLedger(financeRecordId);
            }
            case "security_deposit_forfeiture" -> mapper.updateSecurityDepositForfeitureLedger(financeRecordId, "posted", "pending");
            case "reserve_refund" -> reopenReserveRefund(reviewerId, financeRecordId, reopenNote);
            case "property_expense" -> { /* No balance is posted until payment, so only the review state is reset. */ }
            case "tenant_charge" -> {
                TenantChargeReviewContext charge = mapper.lockConfirmedTenantCharge(financeRecordId);
                if (charge == null || mapper.reverseTenantChargeInvoice(charge.getInvoiceId(), charge.getAmount()) != 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Tenant charge cannot be reopened after the invoice has been paid");
                }
            }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This finance record type does not support reopening");
        }

        if (mapper.reopenFinanceRecord(financeRecordId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Finance record could not be returned to pending");
        }
        if ("reserve_refund".equals(record.getRecordType())) {
            mapper.refreshOwnerRemittanceBatchStatus(financeRecordId);
        }
        mapper.insertReopenAudit(reviewerId, financeRecordId, reopenNote);
    }

    @Transactional
    public void reopenBatch(Long reviewerId, List<Long> financeRecordIds, String note) {
        if (financeRecordIds == null || financeRecordIds.isEmpty() || financeRecordIds.size() > 100
                || financeRecordIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Select between 1 and 100 valid finance records");
        }
        String reopenNote = requiredNote(note);
        for (Long financeRecordId : new java.util.LinkedHashSet<>(financeRecordIds)) {
            reopen(reviewerId, financeRecordId, reopenNote);
        }
    }

    private void reopenReserveTopup(Long financeRecordId, String note) {
        ReserveTopupReopenContext topup = mapper.lockReserveTopupReopen(financeRecordId);
        if (topup == null || zero(topup.getAmount()).signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Confirmed reserve top-up transaction was not found");
        }
        if (mapper.reverseReserveTopupBalance(topup.getReserveAccountId(), topup.getAmount()) != 1
                || mapper.deleteReserveTopupTransaction(financeRecordId) < 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve top-up balance could not be reversed");
        }
        mapper.shiftLaterReserveBalances(topup.getReserveAccountId(), topup.getTransactionId(), topup.getAmount());
        mapper.reopenReserveTopupDocuments(financeRecordId, note);
    }

    private void reopenRentPayment(Long financeRecordId) {
        RentPaymentReopenContext rent = mapper.lockRentPaymentReopen(financeRecordId);
        if (rent == null || zero(rent.getAmount()).signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Confirmed rent payment was not found");
        }
        BigDecimal creditAmount = zero(rent.getCreditAmount());
        BigDecimal currentInvoiceAmount = zero(rent.getAmount()).subtract(creditAmount);
        if (currentInvoiceAmount.signum() > 0
                && mapper.reverseRentInvoicePayment(rent.getRentInvoiceId(), currentInvoiceAmount) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Current rent invoice could not be reversed");
        }
        List<RentCreditAllocationReopenContext> allocations = mapper.lockRentCreditAllocations(financeRecordId);
        for (RentCreditAllocationReopenContext allocation : allocations) {
            if (zero(allocation.getAmount()).signum() > 0
                    && mapper.reverseRentInvoicePayment(allocation.getRentInvoiceId(), allocation.getAmount()) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Prepaid rent allocation could not be reversed");
            }
        }
        if (rent.getRentCreditId() != null && rent.getRentCreditId() > 0) {
            mapper.deleteRentCreditAllocations(financeRecordId);
            if (mapper.deleteRentCredit(financeRecordId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Prepaid rent balance could not be removed");
            }
        }
        if ("security_deposit".equals(rent.getPaymentMethod())) {
            mapper.cancelRentDepositDeduction(financeRecordId);
        }
    }

    private void reopenPropertyPayment(Long financeRecordId, String note) {
        ReviewActionContext context = mapper.lockReview(financeRecordId);
        if (context == null || context.getInstallmentId() == null || context.getReceiptId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment allocation could not be found");
        }
        BigDecimal allocated = zero(context.getAllocatedAmount());
        if (allocated.signum() <= 0
                || mapper.reverseConfirmedPayment(context.getInstallmentId(), allocated) != 1
                || mapper.updateReceiptReview(context.getReceiptId(), "退回待确认：" + note) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Confirmed payment could not be reversed");
        }
        if (context.getProofDocumentId() != null
                && mapper.reopenDocument(context.getProofDocumentId(), note) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment proof could not be reopened");
        }
    }

    private void reopenReserveRefund(Long reviewerId, Long financeRecordId, String note) {
        ReserveRefundReopenContext refund = mapper.lockReserveRefundReopen(financeRecordId);
        if (refund == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Confirmed reserve refund transaction was not found");
        }
        BigDecimal balanceAfter = zero(refund.getCurrentBalance()).add(zero(refund.getAmount()));
        if (mapper.restoreReserveBalance(refund.getReserveAccountId(), refund.getAmount()) != 1
                || mapper.insertReserveRefundReversal(refund.getReserveAccountId(), financeRecordId,
                        refund.getAmount(), balanceAfter, "撤销确认 · " + note, reviewerId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve refund balance could not be restored");
        }
        mapper.updateReserveRefundTransferStatus(financeRecordId, "pending");
        mapper.reopenTenantDepositRefund(financeRecordId);
    }

    @Transactional(readOnly = true)
    public Download downloadProof(Long documentId) {
        ProofFile file = mapper.findProofFile(documentId);
        if (file == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment proof not found");
        Path target = proofStorageRoot.resolve(file.getStorageKey()).normalize();
        if (!target.startsWith(proofStorageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payment proof path");
        }
        if (!Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment proof file is unavailable");
        }
        return new Download(target, safeFileName(file.getOriginalName()),
                normalize(file.getMimeType()) == null ? "application/octet-stream" : file.getMimeType(),
                file.getFileSize() == null ? 0 : file.getFileSize());
    }

    @Transactional(readOnly = true)
    public Download downloadFinancialDocument(Long financeRecordId, String documentType) {
        String type = normalize(documentType);
        if (!"invoice".equals(type) && !"receipt".equals(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document type must be invoice or receipt");
        }
        validateFinanceDocumentSettings(type);
        FinanceReviewRow row = mapper.findDocumentRow(financeRecordId);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Finance record not found");
        Path directory = proofStorageRoot.resolve("finance-documents").normalize();
        String transactionNo = safeFileName(row.getTransactionNo() == null ? String.valueOf(financeRecordId) : row.getTransactionNo());
        String prefix = "invoice".equals(type) ? "INVOICE-" : "OFFICIAL-RECEIPT-";
        Path target = directory.resolve(prefix + transactionNo + ".pdf").normalize();
        if (!target.startsWith(directory) || !directory.startsWith(proofStorageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid finance document path");
        }
        try {
            Files.createDirectories(directory);
            try (java.io.OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                writePrintableFinanceDocument(output, row, type);
            }
            String downloadName = prefix + transactionNo + ".pdf";
            return new Download(target, downloadName, "application/pdf", Files.size(target));
        } catch (Exception exception) {
            try { Files.deleteIfExists(target); } catch (Exception ignored) { }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate finance document", exception);
        }
    }

    @Transactional(readOnly = true)
    public Download downloadFinancialDocuments(List<Long> financeRecordIds, String documentType) {
        String type = normalize(documentType);
        if (!"invoice".equals(type) && !"receipt".equals(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document type must be invoice or receipt");
        }
        validateFinanceDocumentSettings(type);
        List<Long> ids = new LinkedHashSet<>(financeRecordIds == null ? List.of() : financeRecordIds).stream()
                .filter(id -> id != null).toList();
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one finance record");
        Path directory = proofStorageRoot.resolve("finance-documents").normalize();
        String prefix = "invoice".equals(type) ? "INVOICES-" : "OFFICIAL-RECEIPTS-";
        Path target = directory.resolve(prefix + System.currentTimeMillis() + ".zip").normalize();
        if (!target.startsWith(directory) || !directory.startsWith(proofStorageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid finance document path");
        }
        int generated = 0;
        try {
            Files.createDirectories(directory);
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                for (Long id : ids) {
                    FinanceReviewRow row = mapper.findDocumentRow(id);
                    if (row == null) continue;
                    ByteArrayOutputStream pdf = new ByteArrayOutputStream();
                    writePrintableFinanceDocument(pdf, row, type);
                    String transactionNo = safeFileName(row.getTransactionNo() == null ? String.valueOf(id) : row.getTransactionNo());
                    String entryPrefix = "invoice".equals(type) ? "INVOICE-" : "OFFICIAL-RECEIPT-";
                    zip.putNextEntry(new ZipEntry(entryPrefix + transactionNo + ".pdf"));
                    zip.write(pdf.toByteArray());
                    zip.closeEntry();
                    generated++;
                }
            }
            if (generated == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No finance records found");
            return new Download(target, prefix + generated + ".zip", "application/zip", Files.size(target));
        } catch (ResponseStatusException exception) {
            try { Files.deleteIfExists(target); } catch (Exception ignored) { }
            throw exception;
        } catch (Exception exception) {
            try { Files.deleteIfExists(target); } catch (Exception ignored) { }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate finance documents", exception);
        }
    }

    /** Prints every finance invoice/receipt through the shared reference layout. */
    private void writePrintableFinanceDocument(java.io.OutputStream output, FinanceReviewRow row, String documentType) throws Exception {
        FinanceDocumentPdfRenderer.write(output, printableFinanceDocumentData(row, documentType));
    }

    FinanceDocumentPdfRenderer.Data printableFinanceDocumentData(FinanceReviewRow row, String documentType) {
        boolean invoice = "invoice".equals(documentType);
        String documentNo = (invoice ? "IV-" : "OR-") + text(row.getTransactionNo());
        List<String> paymentNotes = new java.util.ArrayList<>();
        paymentNotes.add("Notes:");
        paymentNotes.add("1. All cheques should be crossed and made payable to: "
                + text(financeDocumentSettings.bankAccountHolder()) + ".");
        if (hasText(financeDocumentSettings.bankName()) && hasText(financeDocumentSettings.bankAccountNo())) {
            paymentNotes.add("2. All payments shall be remitted to the following bank account:");
            paymentNotes.add("Account Holder: " + financeDocumentSettings.bankAccountHolder());
            paymentNotes.add("Bank: " + financeDocumentSettings.bankName()
                    + "    Account No.: " + financeDocumentSettings.bankAccountNo());
        } else {
            paymentNotes.add("2. Please confirm the official payment account with CCPS before remittance.");
        }
        return new FinanceDocumentPdfRenderer.Data(
                invoice,
                financeDocumentSettings.companyName(),
                financeDocumentSettings.companyRegistration(),
                List.of(financeDocumentSettings.addressLine1(), financeDocumentSettings.addressLine2()),
                List.of(
                        text(row.getPayerName()),
                        text(row.getProjectName()) + " / " + text(row.getUnitNo())),
                documentNo,
                row.getTransactionDate(),
                text(row.getMilestone()),
                List.of(
                        new FinanceDocumentPdfRenderer.Detail("Transaction No.", text(row.getTransactionNo())),
                        new FinanceDocumentPdfRenderer.Detail("Item Code", text(row.getRecordType())),
                        new FinanceDocumentPdfRenderer.Detail("Property", text(row.getProjectName()) + " / " + text(row.getUnitNo())),
                        new FinanceDocumentPdfRenderer.Detail("Payment Method", paymentMethodText(row.getPaymentMethod())),
                        new FinanceDocumentPdfRenderer.Detail("Payment Reference", text(row.getBankReference())),
                        new FinanceDocumentPdfRenderer.Detail("Status", text(row.getConfirmationStatus()))),
                zero(row.getAmount()),
                row.getCurrency(),
                paymentNotes);
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }

    void validateFinanceDocumentSettings(String documentType) {
        List<String> missing = new java.util.ArrayList<>();
        if (!hasText(financeDocumentSettings.companyRegistration())) missing.add("公司注册号");
        if (!hasText(financeDocumentSettings.addressLine1())) missing.add("公司地址");
        if ("invoice".equals(documentType)) {
            if (!hasText(financeDocumentSettings.bankAccountHolder())) missing.add("收款账户名称");
            if (!hasText(financeDocumentSettings.bankName())) missing.add("收款银行");
            if (!hasText(financeDocumentSettings.bankAccountNo())) missing.add("收款账号");
        }
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "财务文件公司资料未配置完整：" + String.join("、", missing));
        }
    }

    record FinanceDocumentSettings(String companyName, String companyRegistration, String addressLine1,
            String addressLine2, String bankAccountHolder, String bankName, String bankAccountNo) {
        FinanceDocumentSettings {
            companyName = companyName == null || companyName.isBlank()
                    ? "CCPS PROPERTY MANAGEMENT SDN. BHD." : companyName.trim();
            companyRegistration = companyRegistration == null ? "" : companyRegistration.trim();
            addressLine1 = addressLine1 == null ? "" : addressLine1.trim();
            addressLine2 = addressLine2 == null ? "" : addressLine2.trim();
            bankAccountHolder = bankAccountHolder == null || bankAccountHolder.isBlank()
                    ? companyName : bankAccountHolder.trim();
            bankName = bankName == null ? "" : bankName.trim();
            bankAccountNo = bankAccountNo == null ? "" : bankAccountNo.trim();
        }

        static FinanceDocumentSettings defaults() {
            return new FinanceDocumentSettings("CCPS PROPERTY MANAGEMENT SDN. BHD.", "", "", "",
                    "CCPS PROPERTY MANAGEMENT SDN. BHD.", "", "");
        }
    }

    private ReviewActionContext requirePendingReview(Long financeRecordId) {
        ReviewActionContext context = mapper.lockReview(financeRecordId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property payment review not found");
        if (!"pending".equals(context.getConfirmationStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment has already been reviewed");
        }
        return context;
    }

    private AdminFinanceReviewResponse.Item toItem(FinanceReviewRow row, FinanceSourceLink source) {
        return new AdminFinanceReviewResponse.Item(
                row.getId(), row.getTransactionNo(), row.getRecordType(),
                source == null ? null : source.getSourceType(), source == null ? null : source.getSourceId(),
                row.getProjectName(), row.getUnitNo(),
                row.getPayerName(), zero(row.getAmount()), row.getCurrency(), row.getTransactionDate(),
                row.getPaymentMethod(), row.getPaymentStatus(), row.getConfirmationStatus(), row.getSyncStatus(),
                row.getReceiptId(), row.getReceiptNo(), row.getBankReference(), row.getSubmissionNote(), row.getReviewNote(), row.getAllocationNote(),
                row.getProofDocumentId(), row.getProofName(), row.getProofMimeType(), row.getProofSize(),
                row.getInstallmentId(), row.getInstallmentNo(), row.getMilestone(), row.getDueDate(),
                zero(row.getInstallmentAmount()), zero(row.getInstallmentPaid()), zero(row.getAllocatedAmount()),
                zero(row.getAccountBalance()), zero(row.getAccountMinimumBalance()),
                row.getConfirmedByName(), row.getConfirmedAt(), row.getSubmittedAt());
    }

    private String normalizeConfirmationStatus(String value) {
        String normalized = normalize(value);
        return normalized != null && switch (normalized) {
            case "pending", "confirmed", "rejected", "history" -> true;
            default -> false;
        } ? normalized : null;
    }

    private String normalizeSyncStatus(String value) {
        String normalized = normalize(value);
        if ("sync_pending".equals(normalized)) return "pending";
        if ("sync_failed".equals(normalized)) return "failed";
        if ("not_synced".equals(normalized) || "synced".equals(normalized)) return normalized;
        return null;
    }

    private String requiredNote(String value) {
        String note = normalize(value);
        if (note == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review note is required");
        return note;
    }

    private LocalDate requiredDate(LocalDate value) {
        if (value == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finance confirmation date is required");
        if (value.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Finance confirmation date cannot be in the future");
        }
        return value;
    }

    private LocalDate optionalPastDate(LocalDate value) {
        if (value != null && value.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Receipt date cannot be in the future");
        }
        return value;
    }

    private String safeFileName(String value) {
        String name = value == null ? "payment-proof" : value.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\t]", "_");
        return name.isBlank() ? "payment-proof" : name;
    }

    private String text(String value) { return value == null || value.isBlank() ? "—" : value; }
    private String paymentMethodText(String value) {
        return switch (value == null ? "" : value) {
            case "bank_transfer" -> "Bank Transfer";
            case "online_payment", "online_banking" -> "Online Payment";
            case "cash" -> "Cash";
            case "cheque" -> "Cheque";
            default -> text(value);
        };
    }

    private String normalize(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private long zero(Long value) { return value == null ? 0 : value; }

    public record Download(Path path, String originalName, String mimeType, long size) { }
}
