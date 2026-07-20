package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PaymentProofSubmissionResponse(
        Long receiptId,
        String receiptNo,
        Long installmentId,
        BigDecimal amount,
        LocalDate paymentDate,
        String paymentMethod,
        String payerName,
        String confirmationStatus,
        LocalDateTime submittedAt,
        List<FileItem> files) {

    public record FileItem(Long documentId, String name, String mimeType, long size) {}
}
