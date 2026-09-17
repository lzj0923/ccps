package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentProgressResponse(
        Property property,
        Summary summary,
        List<Installment> installments,
        LatestPayment latestPayment) {

    public record Property(
            Long ownerUnitId,
            String projectName,
            String unitNo,
            String ownerName,
            String phone,
            String contractNo,
            String planName,
            LocalDate signedDate,
            String currency,
            String paymentStatus) {
    }

    public record Summary(
            BigDecimal purchasePrice,
            BigDecimal scheduledAmount,
            BigDecimal paidAmount,
            BigDecimal remainingAmount,
            int paidInstallmentCount,
            int totalInstallmentCount,
            LocalDate nextDueDate,
            BigDecimal nextDueAmount) {
    }

    public record Installment(
            Long id,
            int installmentNo,
            String milestone,
            LocalDate dueDate,
            BigDecimal amountDue,
            BigDecimal amountPaid,
            BigDecimal unpaidAmount,
            String status,
            LocalDate paymentDate,
            String confirmationStatus,
            String rejectionReason,
            int receiptCount,
            boolean hasProof,
            List<Long> proofDocumentIds) {
    }

    public record LatestPayment(
            Long receiptId,
            String receiptNo,
            LocalDate paymentDate,
            String paymentMethod,
            String confirmationStatus,
            String rejectionReason,
            Long proofDocumentId) {
    }
}
