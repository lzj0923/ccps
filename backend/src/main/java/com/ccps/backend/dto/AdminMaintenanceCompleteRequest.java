package com.ccps.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminMaintenanceCompleteRequest(
        @NotNull @DecimalMin("0.01") BigDecimal actualAmount,
        @NotBlank String settlementMethod,
        @NotBlank @Size(max = 500) String completionNote) {
}
