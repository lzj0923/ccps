package com.ccps.backend.dto;

import java.time.LocalDateTime;

public record AdminPropertyPhotoResponse(
        Long id,
        Long ownerUnitId,
        Long leaseId,
        String rentalStage,
        String versionMonth,
        Long documentId,
        String title,
        String category,
        String description,
        Integer sortOrder,
        boolean cover,
        String originalName,
        String mimeType,
        Long fileSize,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
