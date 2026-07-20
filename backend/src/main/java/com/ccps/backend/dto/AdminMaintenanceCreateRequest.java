package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminMaintenanceCreateRequest(
        @NotNull Long unitId,
        Long vendorId,
        @NotBlank @Size(max = 60) String category,
        @NotBlank @Size(max = 180) String title,
        @Size(max = 1000) String description,
        @NotNull LocalDateTime requestedAt,
        @NotNull @DecimalMin("0.01") BigDecimal estimatedAmount) { }
