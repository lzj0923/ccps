package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminFinanceReviewResponse(Summary summary, List<Item> rows, Page page) {
    public record Summary(
            long pendingCount,
            long confirmedCount,
            long rejectedCount,
            long pendingSyncCount,
            BigDecimal pendingAmount,
            BigDecimal confirmedMonthAmount) {
    }

    public record Page(long totalRows, int page, int pageSize, int totalPages) {
    }

    public record Item(
            Long id,
            String transactionNo,
            String recordType,
            String sourceType,
            Long sourceId,
            String projectName,
            String unitNo,
            String payerName,
            BigDecimal amount,
            String currency,
            LocalDate transactionDate,
            String paymentMethod,
            String paymentStatus,
            String confirmationStatus,
            String syncStatus,
            Long receiptId,
            String receiptNo,
            String bankReference,
            String submissionNote,
            String reviewNote,
            String allocationNote,
            Long proofDocumentId,
            String proofName,
            String proofMimeType,
            Long proofSize,
            Long installmentId,
            Integer installmentNo,
            String milestone,
            LocalDate dueDate,
            BigDecimal installmentAmount,
            BigDecimal installmentPaid,
            BigDecimal allocatedAmount,
            BigDecimal accountBalance,
            BigDecimal accountMinimumBalance,
            String confirmedByName,
            LocalDateTime confirmedAt,
            LocalDateTime submittedAt) {
    }
}
