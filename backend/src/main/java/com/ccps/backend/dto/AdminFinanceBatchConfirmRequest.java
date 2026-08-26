package com.ccps.backend.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminFinanceBatchConfirmRequest(
        @NotEmpty @Size(max = 100) List<Long> ids,
        @NotNull LocalDate transactionDate,
        LocalDate receiptDate,
        @Size(max = 500) String note,
        @Size(max = 120) String referenceNo) {
    public AdminFinanceBatchConfirmRequest(List<Long> ids, String note) {
        this(ids, null, null, note, null);
    }
}
