package com.ccps.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OwnerDocumentResponse(
        Summary summary,
        List<Category> categories,
        List<DocumentItem> documents) {

    public record Summary(
            int totalCount,
            int pendingSignatureCount,
            int monthNewCount,
            int expiringCount) {
    }

    public record Category(String key, String label, int count) {
    }

    public record DocumentItem(
            Long id,
            String number,
            String name,
            String category,
            String typeLabel,
            String status,
            String mimeType,
            Long fileSize,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDate expiresAt,
            String uploaderName,
            String projectName,
            String city,
            String unitNo,
            boolean downloadable,
            String source,
            Long ownerUnitId) {
    }
}
