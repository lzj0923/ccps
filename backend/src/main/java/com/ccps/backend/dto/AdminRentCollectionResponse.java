package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminRentCollectionResponse(Summary summary, List<Item> rows, Page page) {
    public record Summary(long outstandingCount, long unpaidCount, long partialCount, long overdueCount,
            BigDecimal outstandingAmount, BigDecimal monthReceived) { }
    public record Page(long totalRows, int page, int pageSize, int totalPages) { }
    public record Item(Long invoiceId, Long leaseId, String leaseNo, String tenantName,
            String projectName, String unitNo, LocalDate billingMonth, LocalDate dueDate,
            BigDecimal amountDue, BigDecimal amountPaid, BigDecimal outstandingAmount,
            String collectionStatus, long overdueDays, Long latestFinanceRecordId,
            String latestTransactionNo, BigDecimal latestPaymentAmount, LocalDate latestPaymentDate,
            String latestPaymentMethod, Long latestProofDocumentId, String latestProofName,
            String latestProofMimeType, Long latestProofSize, LocalDateTime latestConfirmedAt,
            BigDecimal monthlyRent, LocalDate leaseStartDate, LocalDate leaseEndDate,
            String rentCalculationMethod) { }
}
