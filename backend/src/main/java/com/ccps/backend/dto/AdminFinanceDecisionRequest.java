package com.ccps.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminFinanceDecisionRequest(
        @NotBlank @Size(max = 500) String note) {
}
