package com.ccps.backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Finance is the sole authority that determines the final accounting date. */
public record AdminFinanceConfirmRequest(
        @NotNull LocalDate transactionDate,
        LocalDate receiptDate,
        @Size(max = 500) String note) {
}
