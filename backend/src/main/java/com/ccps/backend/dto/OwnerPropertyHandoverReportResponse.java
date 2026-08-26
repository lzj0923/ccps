package com.ccps.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OwnerPropertyHandoverReportResponse(
        Long id,
        String title,
        LocalDate reportDate,
        String fileName,
        String mimeType,
        Long fileSize,
        boolean completed,
        boolean downloadable,
        LocalDateTime updatedAt) {
}
