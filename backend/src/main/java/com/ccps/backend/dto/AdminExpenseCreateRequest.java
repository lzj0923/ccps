package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminExpenseCreateRequest(
        @NotNull Long unitId,
        @NotBlank @Size(max = 60) String category,
        @NotBlank @Size(max = 500) String description,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate occurredOn,
        @NotBlank String settlementMethod) { }
