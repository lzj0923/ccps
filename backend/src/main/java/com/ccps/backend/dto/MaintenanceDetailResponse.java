package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MaintenanceDetailResponse(
        Long id,
        String workOrderNo,
        String projectName,
        String unitNo,
        String category,
        String title,
        String description,
        String vendorName,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        String status,
        BigDecimal estimatedAmount,
        BigDecimal actualAmount,
        BigDecimal reserveDeductedAmount,
        String paymentStatus,
        String confirmationStatus,
        String paymentMethod,
        LocalDate paymentDate,
        List<StatusEvent> history,
        List<Attachment> attachments,
        String payerName,
        String bankName,
        String paymentAccountNo) {

    public record StatusEvent(Long id, String status, LocalDateTime occurredAt, String note) {
    }

    public record Attachment(
            Long documentId,
            String name,
            String mimeType,
            long size,
            String relationType,
            LocalDateTime uploadedAt) {
    }
}
