package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminTenancyResponse(Summary summary, List<Item> rows, Page page) {
    public record Summary(long tenantCount, long activeLeaseCount, BigDecimal currentDue,
            BigDecimal currentPaid, BigDecimal currentUnpaid, BigDecimal totalUnpaid, long partialCount,
            long overdueCount, long pendingReviewCount) { }

    public record Item(Long tenantId, String tenantName, String identityNo, String phone, String email,
            String tenantStatus, Long leaseId, String leaseNo, Long projectId, String projectName,
            Long unitId, String unitNo, LocalDate leaseStart, LocalDate leaseEnd,
            BigDecimal monthlyRent, BigDecimal depositAmount, Integer paymentDay, String rentCalculationMethod, String leaseStatus,
            Long contractDocumentId, String contractDocumentName, String contractDocumentMimeType,
            Long contractDocumentSize, Long invoiceId, LocalDate billingMonth, LocalDate dueDate,
            BigDecimal amountDue, BigDecimal amountPaid, BigDecimal amountUnpaid, BigDecimal totalUnpaid, BigDecimal prepaidRentBalance, String rentStatus,
            Long financeRecordId, String transactionNo, String confirmationStatus,
            String paymentMethod, LocalDate paymentDate, String reviewNote,
            String confirmedByName, LocalDateTime confirmedAt,
            Long ownerId, String ownerName, String ownerIdentity) { }

    public record Page(long totalRows, int page, int pageSize, int totalPages) { }
}
