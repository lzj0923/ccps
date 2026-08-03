package com.ccps.backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AdminLeaseCloseRequest(
        @NotNull LocalDate endDate,
        @NotBlank @Pattern(regexp = "normal_expiry|early_termination") String reason,
        String notes) {}
