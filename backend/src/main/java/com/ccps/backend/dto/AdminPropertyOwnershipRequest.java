package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AdminPropertyOwnershipRequest(
        @NotNull Long ownerId,
        @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal ownershipPercent,
        boolean primary,
        LocalDate startDate,
        LocalDate endDate) {
}
