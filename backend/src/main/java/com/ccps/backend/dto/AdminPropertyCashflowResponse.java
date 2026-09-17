package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminPropertyCashflowResponse(
        Long id,
        Long financeRecordId,
        String transactionNo,
        String direction,
        String category,
        String description,
        String allocationNote,
        BigDecimal amount,
        String currency,
        LocalDate occurredOn,
        String paymentMethod,
        String paymentStatus,
        String confirmationStatus,
        String syncStatus,
        String source,
        boolean editable,
        Long workOrderId,
        Long attachmentId,
        String attachmentPath,
        String attachmentName,
        Long attachmentSize,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDate receiptDate,
        LocalDate paymentDate) {
}
