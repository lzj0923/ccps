package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Database-backed building and payment-plan data used by the admin workspace. */
public record AdminBuildingPaymentResponse(
        Summary summary,
        List<Installment> rows,
        Page page) {

    public record Summary(
            BigDecimal propertyTotal,
            BigDecimal paidTotal,
            BigDecimal unpaidTotal,
            int totalInstallments,
            int paidInstallments,
            int remainingInstallments,
            BigDecimal nextAmount,
            LocalDate nextDueDate) {
    }

    public record Page(
            long totalRows,
            int page,
            int pageSize,
            int totalPages) {
    }

    public record Installment(
            Long id,
            Long paymentPlanId,
            Long contractId,
            String projectName,
            String unitNo,
            String ownerName,
            String contractNo,
            String planName,
            Integer installmentNo,
            String milestone,
            LocalDate dueDate,
            BigDecimal amountDue,
            BigDecimal amountPaid,
            BigDecimal unpaidAmount,
            LocalDate paymentDate,
            String status,
            String receiptNo,
            BigDecimal purchasePrice,
            Long financeRecordId,
            String confirmationStatus,
            String paymentMethod,
            String bankReference,
            String submissionNote,
            Long proofDocumentId,
            BigDecimal submittedAmount) {
    }
}
