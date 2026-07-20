package com.ccps.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record AdminReserveSettingsRequest(
        @NotNull(message = "Minimum reserve balance is required")
        @DecimalMin(value = "0.00", message = "Minimum reserve balance cannot be negative")
        @Digits(integer = 16, fraction = 2, message = "Minimum reserve balance supports up to two decimal places")
        BigDecimal minimumBalance,
        boolean lowBalanceAlertEnabled) { }
