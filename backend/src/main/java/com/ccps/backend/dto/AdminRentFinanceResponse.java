package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminRentFinanceResponse(Summary summary, List<Item> rows, Page page) {
    public record Summary(long totalCount, long withProofCount, long missingProofCount, long pendingSyncCount,
            BigDecimal totalAmount, BigDecimal monthAmount) { }
    public record Page(long totalRows, int page, int pageSize, int totalPages) { }
    public record Item(Long id, String transactionNo, String tenantName, String projectName, String unitNo,
            Long leaseId, String leaseNo, Long invoiceId, LocalDate billingMonth, LocalDate dueDate,
            BigDecimal invoiceAmount, BigDecimal invoicePaid, BigDecimal amount, String currency,
            LocalDate transactionDate, String paymentMethod, String confirmationStatus, String syncStatus,
            Long proofDocumentId, String proofName, String proofMimeType, Long proofSize, String receiptNo,
            String reviewNote, String confirmedByName, LocalDateTime confirmedAt, LocalDateTime submittedAt) { }
}
