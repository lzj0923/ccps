package com.ccps.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPropertyHandoverChecklistItemRequest(
        @NotBlank @Size(max = 120) String category,
        @NotBlank @Size(max = 255) String itemName,
        @Size(max = 80) String defaultQuantity,
        @Size(max = 500) String notes,
        Integer sortOrder,
        Boolean enabled) {
}
