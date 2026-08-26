package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPropertyMaintenanceResponse(
        Long id,
        String workOrderNo,
        Long vendorId,
        String vendorName,
        String category,
        String title,
        String description,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        String status,
        BigDecimal estimatedAmount,
        BigDecimal actualAmount,
        Long cashflowEntryId,
        int attachmentCount,
        LocalDateTime updatedAt,
        boolean editable) {
}
