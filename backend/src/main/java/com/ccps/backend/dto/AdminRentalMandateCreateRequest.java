package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminRentalMandateCreateRequest(
        @NotNull Long ownerUnitId,
        @NotBlank @Size(max = 30) String mandateType,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @DecimalMin("0.00") BigDecimal managementFee,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal commissionPercent,
        Long responsibleUserId) {
}
