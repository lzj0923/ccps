package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AdminLeaseUpdateRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @DecimalMin("0.01") BigDecimal monthlyRent,
        @NotNull @DecimalMin("0.00") BigDecimal depositAmount,
        @Min(1) @Max(31) int paymentDay,
        @Pattern(regexp = "daily_prorated") String rentCalculationMethod) {
    public AdminLeaseUpdateRequest(LocalDate startDate, LocalDate endDate, BigDecimal monthlyRent,
            BigDecimal depositAmount, int paymentDay) {
        this(startDate, endDate, monthlyRent, depositAmount, paymentDay, "daily_prorated");
    }
}
