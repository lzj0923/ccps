package com.ccps.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminRentalSpaceRequest(
        @NotBlank @Size(max = 40) String spaceCode,
        @NotBlank @Size(max = 100) String spaceName,
        @Min(1) Integer capacity,
        @DecimalMin("0.00") BigDecimal areaSqm,
        @DecimalMin("0.00") BigDecimal recommendedRent,
        @Pattern(regexp = "active|disabled") String status) { }
