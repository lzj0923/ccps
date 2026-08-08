package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminLeaseRenewalRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @DecimalMin("0.01") BigDecimal monthlyRent,
        @NotNull @DecimalMin("0.00") BigDecimal depositAmount,
        @NotNull @Min(1) @Max(31) Integer paymentDay,
        String rentCalculationMethod) {
}
