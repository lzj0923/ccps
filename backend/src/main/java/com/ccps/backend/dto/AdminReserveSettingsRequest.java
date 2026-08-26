package com.ccps.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

public record AdminReserveSettingsRequest(
        @DecimalMin(value = "0.00", message = "Minimum reserve balance cannot be negative")
        @Digits(integer = 16, fraction = 2, message = "Minimum reserve balance supports up to two decimal places")
        BigDecimal minimumBalance,
        boolean lowBalanceAlertEnabled,
        Boolean automaticCalculation,
        @Size(max = 500) String remarks) { }
