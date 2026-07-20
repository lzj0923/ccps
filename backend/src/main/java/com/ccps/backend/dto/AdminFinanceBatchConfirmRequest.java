package com.ccps.backend.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record AdminFinanceBatchConfirmRequest(
        @NotEmpty @Size(max = 100) List<Long> ids,
        @Size(max = 500) String note) {
}
