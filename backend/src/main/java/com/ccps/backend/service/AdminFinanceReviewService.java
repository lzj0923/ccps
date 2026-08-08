package com.ccps.backend.service;

import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminFinanceReviewResponse;
import com.ccps.backend.mapper.AdminFinanceReviewMapper;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.FinanceReviewRow;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.FinanceSummaryRow;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ProofFile;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReopenRecordContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReviewActionContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReserveRefundContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReserveRefundReopenContext;

@Service
public class AdminFinanceReviewService {
    private final AdminFinanceReviewMapper mapper;
    private final Path proofStorageRoot;

    public AdminFinanceReviewService(AdminFinanceReviewMapper mapper,
            @Value("${ccps.storage.payment-proofs:uploads/payment-proofs}") String proofStorageRoot) {
        this.mapper = mapper;
        this.proofStorageRoot = Path.of(proofStorageRoot).toAbsolutePath().normalize();
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
        boolean expense = "expense".equals(reviewType);
        long totalRows = zero(reserve
                ? mapper.countReservePage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus, startDate, endDate)
                : expense ? mapper.countExpensePage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus, startDate, endDate)
                : mapper.countPage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus, startDate, endDate));
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        List<FinanceReviewRow> sourceRows = reserve
                ? mapper.findReservePage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus,
                        startDate, endDate, pageSize, (page - 1) * pageSize)
                : expense ? mapper.findExpensePage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus,
                        startDate, endDate, pageSize, (page - 1) * pageSize)
                : mapper.findPage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus,
                        startDate, endDate, pageSize, (page - 1) * pageSize);
        List<AdminFinanceReviewResponse.Item> rows = sourceRows
                .stream().map(this::toItem).toList();
        FinanceSummaryRow source = reserve ? mapper.findReserveSummary() : expense ? mapper.findExpenseSummary() : mapper.findSummary();
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
                : "expense".equals(reviewType) ? mapper.findExpenseProjects() : mapper.findProjects();
    }

    @Transactional
    public void confirm(Long reviewerId, Long financeRecordId, String note) {
        confirmOne(reviewerId, financeRecordId, requiredNote(note));
    }

    @Transactional
    public void confirmBatch(Long reviewerId, List<Long> financeRecordIds, String note) {
        confirmBatch(reviewerId, financeRecordIds, note, null);
    }

    @Transactional
    public void confirmBatch(Long reviewerId, List<Long> financeRecordIds, String note, String referenceNo) {
        List<Long> uniqueIds = new LinkedHashSet<>(financeRecordIds).stream().toList();
        if (uniqueIds.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one payment");
        String reference = normalize(referenceNo);
        String reviewNote = normalize(note) == null ? "批量確認收款" : note.trim();
        if (reference != null) reviewNote = "批量编号：" + reference + "；" + reviewNote;
        for (Long financeRecordId : uniqueIds) confirmOne(reviewerId, financeRecordId, reviewNote);
    }

    private void confirmOne(Long reviewerId, Long financeRecordId, String note) {
        if ("reserve_refund".equals(mapper.lockRecordType(financeRecordId))) {
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
            mapper.insertNotification(refund.getUserId(), refund.getOwnerId(), financeRecordId,
                    "預備金已返還", "%s %s 預備金已返還 RM %s。".formatted(refund.getProjectName(), refund.getUnitNo(), refund.getAmount().setScale(2).toPlainString()), "normal");
            mapper.insertAudit(reviewerId, financeRecordId, "confirm_reserve_refund", "confirmed", note);
            return;
        }
        if ("property_expense".equals(mapper.lockRecordType(financeRecordId))) {
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
        if ("security_deposit".equals(mapper.lockRecordType(financeRecordId))) {
            if (mapper.confirmSecurityDeposit(financeRecordId, reviewerId) != 1
                    || mapper.confirmSecurityDepositEntry(financeRecordId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Security deposit has already been reviewed");
            }
            mapper.insertConfirmedSecurityDepositLedger(financeRecordId, reviewerId);
            mapper.insertAudit(reviewerId, financeRecordId, "confirm_security_deposit", "confirmed", note);
            return;
        }
        if ("security_deposit_forfeiture".equals(mapper.lockRecordType(financeRecordId))) {
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
        if ("reserve_refund".equals(mapper.lockRecordType(financeRecordId))) {
            if (mapper.rejectReserveRefund(financeRecordId, reviewerId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve refund has already been reviewed");
            }
            mapper.cancelTenantDepositRefund(financeRecordId);
            mapper.insertAudit(reviewerId, financeRecordId, "reject_reserve_refund", "rejected", reviewNote);
            return;
        }
        if ("property_expense".equals(mapper.lockRecordType(financeRecordId))) {
            if (mapper.rejectExpense(financeRecordId, reviewerId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Expense has already been reviewed");
            }
            mapper.insertAudit(reviewerId, financeRecordId, "reject_property_expense", "rejected", reviewNote);
            return;
        }
        if ("security_deposit".equals(mapper.lockRecordType(financeRecordId))) {
            if (mapper.rejectSecurityDeposit(financeRecordId, reviewerId) != 1
                    || mapper.rejectSecurityDepositEntry(financeRecordId) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Security deposit has already been reviewed");
            }
            mapper.insertAudit(reviewerId, financeRecordId, "reject_security_deposit", "rejected", reviewNote);
            return;
        }
        if ("security_deposit_forfeiture".equals(mapper.lockRecordType(financeRecordId))) {
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

        switch (record.getRecordType()) {
            case "property_payment" -> reopenPropertyPayment(financeRecordId, reopenNote);
            case "security_deposit" -> {
                if (mapper.reopenSecurityDepositEntry(financeRecordId) != 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Security deposit entry could not be reopened");
                }
                mapper.reopenSecurityDepositLedger(financeRecordId);
            }
            case "security_deposit_forfeiture" -> mapper.updateSecurityDepositForfeitureLedger(financeRecordId, "posted", "pending");
            case "reserve_refund" -> reopenReserveRefund(reviewerId, financeRecordId, reopenNote);
            case "property_expense" -> { /* No balance is posted until payment, so only the review state is reset. */ }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "This finance record type does not support reopening");
        }

        if (mapper.reopenFinanceRecord(financeRecordId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Finance record could not be returned to pending");
        }
        mapper.insertReopenAudit(reviewerId, financeRecordId, reopenNote);
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

    /**
     * Prints the finance document in the same A4 invoice/receipt structure as
     * the supplied reference: centred company header, customer box, document
     * metadata on the right, seven-column item table, total line and notes.
     */
    private void writePrintableFinanceDocument(java.io.OutputStream output, FinanceReviewRow row, String documentType) throws Exception {
        com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 30, 30, 24, 28);
        com.lowagie.text.pdf.PdfWriter.getInstance(document, output);
        document.open();
        com.lowagie.text.pdf.BaseFont base = com.lowagie.text.pdf.BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED);
        com.lowagie.text.Font companyFont = new com.lowagie.text.Font(base, 13, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font titleFont = new com.lowagie.text.Font(base, 14, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font bold = new com.lowagie.text.Font(base, 8, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font body = new com.lowagie.text.Font(base, 8);
        com.lowagie.text.Font small = new com.lowagie.text.Font(base, 7);

        com.lowagie.text.Paragraph company = new com.lowagie.text.Paragraph("HH CONSULTANTS (MM2H) SDN BHD  (202301030304 (1542427-K))", companyFont);
        company.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(company);
        com.lowagie.text.Paragraph registration = new com.lowagie.text.Paragraph("SO-32-05, MENARA 1, KL ECO CITY, NO.3, JALAN BANGSAR,", small);
        registration.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(registration);
        com.lowagie.text.Paragraph contact = new com.lowagie.text.Paragraph("59200 KUALA LUMPUR    (License No.: MM2H814)    Tel: 03-6415 1485    Email: hay@hmm2h.com", small);
        contact.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
        document.add(contact);
        document.add(financeRule());

        com.lowagie.text.pdf.PdfPTable partyAndMeta = new com.lowagie.text.pdf.PdfPTable(2);
        partyAndMeta.setWidthPercentage(100);
        partyAndMeta.setWidths(new float[] { 55f, 45f });
        com.lowagie.text.pdf.PdfPCell partyCell = new com.lowagie.text.pdf.PdfPCell();
        partyCell.setPadding(7); partyCell.setBorder(com.lowagie.text.Rectangle.BOX);
        partyCell.addElement(new com.lowagie.text.Paragraph("BIZCARE MANAGEMENT SDN BHD", bold));
        partyCell.addElement(new com.lowagie.text.Paragraph("SO-32-05, MENARA 1, KL ECO CITY", body));
        partyCell.addElement(new com.lowagie.text.Paragraph("NO. 3, JALAN BANGSAR", body));
        partyCell.addElement(new com.lowagie.text.Paragraph("59200 KUALA LUMPUR", body));
        partyCell.addElement(new com.lowagie.text.Paragraph(text(row.getPayerName()), body));
        partyCell.addElement(new com.lowagie.text.Paragraph(text(row.getProjectName()) + " / " + text(row.getUnitNo()), body));
        partyCell.addElement(new com.lowagie.text.Paragraph("Attn: ______________________________", small));
        partyCell.addElement(new com.lowagie.text.Paragraph("TEL: __________________   FAX: __________________", small));
        partyAndMeta.addCell(partyCell);

        com.lowagie.text.pdf.PdfPCell metaCell = new com.lowagie.text.pdf.PdfPCell();
        metaCell.setPadding(4); metaCell.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
        String documentNo = ("invoice".equals(documentType) ? "IV-" : "OR-") + text(row.getTransactionNo());
        com.lowagie.text.Paragraph documentTitle = new com.lowagie.text.Paragraph(
                ("invoice".equals(documentType) ? "INVOICE / 發票" : "OFFICIAL RECEIPT / 收據") + "  :  " + documentNo, titleFont);
        documentTitle.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT);
        metaCell.addElement(documentTitle);
        metaCell.addElement(new com.lowagie.text.Paragraph("Your Ref.       : __________________", body));
        metaCell.addElement(new com.lowagie.text.Paragraph("Our D/O No     : " + text(row.getTransactionNo()), body));
        metaCell.addElement(new com.lowagie.text.Paragraph("Terms           : N/A", body));
        metaCell.addElement(new com.lowagie.text.Paragraph("Date            : " + dateText(row.getTransactionDate()), body));
        metaCell.addElement(new com.lowagie.text.Paragraph("Page            : 1 of 1", body));
        partyAndMeta.addCell(metaCell);
        document.add(partyAndMeta);
        document.add(financeRule());

        com.lowagie.text.pdf.PdfPTable items = new com.lowagie.text.pdf.PdfPTable(7);
        items.setWidthPercentage(100);
        items.setWidths(new float[] { .45f, 1.05f, 3.3f, .55f, 1.0f, .8f, 1.15f });
        String[] headers = { "No", "Item Code", "Description", "Qty", "Price/Unit", "Discount", "Amount" };
        for (String header : headers) invoiceCell(items, header, bold, true, com.lowagie.text.Element.ALIGN_CENTER);
        invoiceCell(items, "1", body, false, com.lowagie.text.Element.ALIGN_CENTER);
        invoiceCell(items, text(row.getRecordType()), body, false, com.lowagie.text.Element.ALIGN_LEFT);
        invoiceCell(items, text(row.getMilestone()), body, false, com.lowagie.text.Element.ALIGN_LEFT);
        invoiceCell(items, "1", body, false, com.lowagie.text.Element.ALIGN_CENTER);
        invoiceCell(items, money(row.getAmount()), body, false, com.lowagie.text.Element.ALIGN_RIGHT);
        invoiceCell(items, "", body, false, com.lowagie.text.Element.ALIGN_RIGHT);
        invoiceCell(items, money(row.getAmount()), body, false, com.lowagie.text.Element.ALIGN_RIGHT);
        document.add(items);
        document.add(new com.lowagie.text.Paragraph(" ", body));
        document.add(financeRule());

        com.lowagie.text.pdf.PdfPTable total = new com.lowagie.text.pdf.PdfPTable(2);
        total.setWidthPercentage(100); total.setWidths(new float[] { 5.3f, 1.2f });
        invoiceCell(total, "Malaysian Ringgit : " + money(row.getAmount()) + " ONLY", bold, false, com.lowagie.text.Element.ALIGN_LEFT);
        invoiceCell(total, text(row.getCurrency()) + " " + money(row.getAmount()), bold, true, com.lowagie.text.Element.ALIGN_RIGHT);
        document.add(total);
        document.add(new com.lowagie.text.Paragraph(" ", body));
        document.add(new com.lowagie.text.Paragraph("Notes :", bold));
        document.add(new com.lowagie.text.Paragraph("1. All cheques should be crossed and made payable to:", small));
        document.add(new com.lowagie.text.Paragraph("   CCPS PROPERTY MANAGEMENT SDN. BHD.", small));
        document.add(new com.lowagie.text.Paragraph("2. All payments shall be remitted to the following bank account:", small));
        document.add(new com.lowagie.text.Paragraph("   Account Holder: CCPS PROPERTY MANAGEMENT SDN. BHD.", small));
        document.add(new com.lowagie.text.Paragraph("   Bank: ____________________    Account No.: ____________________", small));
        document.add(new com.lowagie.text.Paragraph("This is a computer generated document and no signature is required.", small));
        document.close();
    }

    private com.lowagie.text.pdf.PdfPTable financeRule() {
        com.lowagie.text.pdf.PdfPTable rule = new com.lowagie.text.pdf.PdfPTable(1);
        rule.setWidthPercentage(100);
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell();
        cell.setBorder(com.lowagie.text.Rectangle.TOP); cell.setBorderWidthTop(0.8f); cell.setPadding(0); cell.setFixedHeight(4f);
        rule.addCell(cell);
        return rule;
    }

    private void invoiceCell(com.lowagie.text.pdf.PdfPTable table, String value, com.lowagie.text.Font font, boolean boxed, int alignment) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(value == null ? "" : value, font));
        cell.setHorizontalAlignment(alignment); cell.setVerticalAlignment(com.lowagie.text.Element.ALIGN_MIDDLE); cell.setPadding(4);
        cell.setBorder(boxed ? com.lowagie.text.Rectangle.BOX : com.lowagie.text.Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private void writeFinanceDocument(java.io.OutputStream output, FinanceReviewRow row, String documentType) throws Exception {
        com.lowagie.text.Document document = new com.lowagie.text.Document();
        com.lowagie.text.pdf.PdfWriter.getInstance(document, output);
        document.open();
        com.lowagie.text.pdf.BaseFont base = com.lowagie.text.pdf.BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED);
        com.lowagie.text.Font title = new com.lowagie.text.Font(base, 15, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Font body = new com.lowagie.text.Font(base, 9);
        com.lowagie.text.Font bold = new com.lowagie.text.Font(base, 9, com.lowagie.text.Font.BOLD);
        com.lowagie.text.Paragraph company = new com.lowagie.text.Paragraph("CCPS PROPERTY MANAGEMENT SDN. BHD.", bold);
        company.setAlignment(com.lowagie.text.Element.ALIGN_CENTER); document.add(company);
        com.lowagie.text.Paragraph address = new com.lowagie.text.Paragraph("Property Management · Kuala Lumpur · Malaysia", body);
        address.setAlignment(com.lowagie.text.Element.ALIGN_CENTER); document.add(address);
        document.add(new com.lowagie.text.Paragraph(" ", body));
        String heading = "invoice".equals(documentType) ? "INVOICE / 發票" : "OFFICIAL RECEIPT / 收據";
        com.lowagie.text.Paragraph documentHeading = new com.lowagie.text.Paragraph(heading + "  :  " + text(row.getTransactionNo()), title);
        documentHeading.setAlignment(com.lowagie.text.Element.ALIGN_RIGHT); document.add(documentHeading);
        document.add(new com.lowagie.text.Paragraph(" ", body));
        com.lowagie.text.pdf.PdfPTable party = new com.lowagie.text.pdf.PdfPTable(2);
        party.setWidthPercentage(100); party.setWidths(new float[] { 1.2f, 2.8f });
        financeCell(party, "付款人 / Payer", row.getPayerName(), body);
        financeCell(party, "物業 / Property", text(row.getProjectName()) + " / " + text(row.getUnitNo()), body);
        financeCell(party, "交易日期 / Date", dateText(row.getTransactionDate()), body);
        financeCell(party, "付款方式 / Payment", paymentMethodText(row.getPaymentMethod()), body);
        financeCell(party, "付款參考 / Reference", row.getBankReference(), body);
        financeCell(party, "狀態 / Status", row.getConfirmationStatus(), body);
        document.add(party);
        document.add(new com.lowagie.text.Paragraph(" ", body));
        com.lowagie.text.pdf.PdfPTable items = new com.lowagie.text.pdf.PdfPTable(5);
        items.setWidthPercentage(100); items.setWidths(new float[] { .5f, 1.2f, 3.5f, .8f, 1.2f });
        String[] headers = { "No", "Item Code", "Description", "Qty", "Amount" };
        for (String header : headers) financeCell(items, header, "", bold);
        financeCell(items, "1", text(row.getRecordType()), text(row.getMilestone()), "1", money(row.getAmount()), body);
        document.add(items);
        document.add(new com.lowagie.text.Paragraph("\n", body));
        com.lowagie.text.pdf.PdfPTable total = new com.lowagie.text.pdf.PdfPTable(2);
        total.setWidthPercentage(100); total.setWidths(new float[] { 3.5f, 1.2f });
        financeCell(total, "Total (" + text(row.getCurrency()) + ")", money(row.getAmount()), bold);
        document.add(total);
        document.add(new com.lowagie.text.Paragraph(" ", body));
        document.add(new com.lowagie.text.Paragraph("This is a computer generated document and no signature is required.", body));
        document.close();
    }

    private void financeCell(com.lowagie.text.pdf.PdfPTable table, String label, String value, com.lowagie.text.Font font) {
        com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase(text(label) + (value == null || value.isBlank() ? "" : "  " + text(value)), font));
        cell.setPadding(5); table.addCell(cell);
    }

    private void financeCell(com.lowagie.text.pdf.PdfPTable table, String no, String code, String description, String qty, String amount, com.lowagie.text.Font font) {
        for (String value : new String[] { no, code, description, qty, amount }) financeCell(table, "", value, font);
    }

    private ReviewActionContext requirePendingReview(Long financeRecordId) {
        ReviewActionContext context = mapper.lockReview(financeRecordId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property payment review not found");
        if (!"pending".equals(context.getConfirmationStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment has already been reviewed");
        }
        return context;
    }

    private AdminFinanceReviewResponse.Item toItem(FinanceReviewRow row) {
        return new AdminFinanceReviewResponse.Item(
                row.getId(), row.getTransactionNo(), row.getRecordType(), row.getProjectName(), row.getUnitNo(),
                row.getPayerName(), zero(row.getAmount()), row.getCurrency(), row.getTransactionDate(),
                row.getPaymentMethod(), row.getPaymentStatus(), row.getConfirmationStatus(), row.getSyncStatus(),
                row.getReceiptId(), row.getReceiptNo(), row.getBankReference(), row.getSubmissionNote(), row.getReviewNote(),
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

    private String safeFileName(String value) {
        String name = value == null ? "payment-proof" : value.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\t]", "_");
        return name.isBlank() ? "payment-proof" : name;
    }

    private String text(String value) { return value == null || value.isBlank() ? "—" : value; }
    private String dateText(java.time.LocalDate value) { return value == null ? "—" : value.toString(); }
    private String money(BigDecimal value) { return zero(value).setScale(2).toPlainString(); }
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
