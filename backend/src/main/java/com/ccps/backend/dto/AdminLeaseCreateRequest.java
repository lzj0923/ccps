package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AdminLeaseCreateRequest(
        @NotNull Long tenantId,
        @NotNull Long unitId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @DecimalMin(value = "0.01") BigDecimal monthlyRent,
        @NotNull @DecimalMin(value = "0.00") BigDecimal depositAmount,
        @Min(1) @Max(31) int paymentDay,
        @Pattern(regexp = "daily_prorated") String rentCalculationMethod,
        Long rentalMandateId,
        Long rentalSpaceId) {
    public AdminLeaseCreateRequest(Long tenantId, Long unitId, LocalDate startDate, LocalDate endDate,
            BigDecimal monthlyRent, BigDecimal depositAmount, int paymentDay) {
        this(tenantId, unitId, startDate, endDate, monthlyRent, depositAmount, paymentDay, "daily_prorated", null, null);
    }

    public AdminLeaseCreateRequest(Long tenantId, Long unitId, LocalDate startDate, LocalDate endDate,
            BigDecimal monthlyRent, BigDecimal depositAmount, int paymentDay, String rentCalculationMethod) {
        this(tenantId, unitId, startDate, endDate, monthlyRent, depositAmount, paymentDay, rentCalculationMethod, null, null);
    }

    public AdminLeaseCreateRequest(Long tenantId, Long unitId, LocalDate startDate, LocalDate endDate,
            BigDecimal monthlyRent, BigDecimal depositAmount, int paymentDay, String rentCalculationMethod,
            Long rentalMandateId) {
        this(tenantId, unitId, startDate, endDate, monthlyRent, depositAmount, paymentDay,
                rentCalculationMethod, rentalMandateId, null);
    }
}
