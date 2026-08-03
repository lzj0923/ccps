package com.ccps.backend.dto;

import java.time.LocalDateTime;

public record AdminPropertyHandoverChecklistItemResponse(
        Long id,
        Long ownerUnitId,
        String category,
        String itemName,
        String defaultQuantity,
        String notes,
        Integer sortOrder,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
