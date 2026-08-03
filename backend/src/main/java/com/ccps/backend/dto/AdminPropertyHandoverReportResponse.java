package com.ccps.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminPropertyHandoverReportResponse(
        Long id,
        Long ownerUnitId,
        Long documentId,
        String title,
        LocalDate reportDate,
        LocalDate trackingStartDate,
        LocalDate trackingEndDate,
        String repairAttachmentPath,
        String originalName,
        String mimeType,
        Long fileSize,
        String remarks,
        String contentJson,
        boolean completed,
        Long createdBy,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
