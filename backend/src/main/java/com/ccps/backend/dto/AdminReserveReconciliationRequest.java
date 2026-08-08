package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminReserveReconciliationRequest(
        @NotNull LocalDate reconciliationMonth,
        @NotNull @Digits(integer = 16, fraction = 2) BigDecimal financeBalance,
        boolean confirmed,
        @Size(max = 500) String note) { }
