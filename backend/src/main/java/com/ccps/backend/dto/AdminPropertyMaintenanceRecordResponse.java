package com.ccps.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminPropertyMaintenanceRecordResponse(
        Long id,
        Long ownerUnitId,
        Long workOrderId,
        String recordNo,
        String category,
        String title,
        LocalDate maintenanceDate,
        Integer durationMinutes,
        String details,
        String resultSummary,
        LocalDate nextMaintenanceDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
