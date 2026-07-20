package com.ccps.backend.service;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

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
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReviewActionContext;

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
        long totalRows = zero(reserve
                ? mapper.countReservePage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus, startDate, endDate)
                : mapper.countPage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus, startDate, endDate));
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        List<FinanceReviewRow> sourceRows = reserve
                ? mapper.findReservePage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus,
                        startDate, endDate, pageSize, (page - 1) * pageSize)
                : mapper.findPage(normalize(keyword), normalize(projectName), confirmationStatus, syncStatus,
                        startDate, endDate, pageSize, (page - 1) * pageSize);
        List<AdminFinanceReviewResponse.Item> rows = sourceRows
                .stream().map(this::toItem).toList();
        FinanceSummaryRow source = reserve ? mapper.findReserveSummary() : mapper.findSummary();
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
        return "reserve".equals(reviewType) ? mapper.findReserveProjects() : mapper.findProjects();
    }

    @Transactional
    public void confirm(Long reviewerId, Long financeRecordId, String note) {
        confirmOne(reviewerId, financeRecordId, requiredNote(note));
    }

    @Transactional
    public void confirmBatch(Long reviewerId, List<Long> financeRecordIds, String note) {
        List<Long> uniqueIds = new LinkedHashSet<>(financeRecordIds).stream().toList();
        if (uniqueIds.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one payment");
        String reviewNote = normalize(note) == null ? "批量確認收款" : note.trim();
        for (Long financeRecordId : uniqueIds) confirmOne(reviewerId, financeRecordId, reviewNote);
    }

    private void confirmOne(Long reviewerId, Long financeRecordId, String note) {
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

    private String normalize(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private long zero(Long value) { return value == null ? 0 : value; }

    public record Download(Path path, String originalName, String mimeType, long size) { }
}
