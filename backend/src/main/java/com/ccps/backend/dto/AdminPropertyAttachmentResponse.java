package com.ccps.backend.dto;

import java.time.LocalDateTime;

public record AdminPropertyAttachmentResponse(
        Long id,
        Long ownerUnitId,
        Long documentId,
        String title,
        String filePath,
        String originalName,
        String mimeType,
        Long fileSize,
        String remarks,
        boolean enabled,
        Long createdBy,
        String createdByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
