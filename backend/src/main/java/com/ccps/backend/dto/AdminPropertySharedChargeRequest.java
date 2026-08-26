package com.ccps.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminPropertySharedChargeRequest(
        @NotBlank @Pattern(regexp = "management|utilities|maintenance|other") String chargeType,
        @NotBlank @Size(max = 255) String description,
        @NotNull @DecimalMin(value = "0.01") BigDecimal totalAmount,
        @NotBlank @Pattern(regexp = "tenant|owner|agency") String payer) {}
