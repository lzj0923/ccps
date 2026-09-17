package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AdminFundOperationsRequest {
    private AdminFundOperationsRequest() { }

    public record InternalTransfer(
            @NotNull Long sourceReserveAccountId,
            @NotNull Long targetReserveAccountId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal amount,
            @NotNull LocalDate requestedDate,
            @NotBlank @Size(max = 500) String reason) { }

    public record Review(@Size(max = 500) String note) { }

    public record RemittanceSetting(
            @NotBlank @Pattern(regexp = "monthly|quarterly|semiannual|manual") String cycle,
            LocalDate nextRemittanceDate,
            @NotNull Boolean enabled,
            @NotNull Boolean holdEnabled,
            @Size(max = 500) String holdReason,
            LocalDate holdUntil,
            @NotNull @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal retainedAmount,
            @NotNull @DecimalMin("0.00") @Digits(integer = 16, fraction = 2) BigDecimal taxRetainedAmount,
            Long defaultBankAccountId) { }

    public record GenerateRemittanceBatch(
            @NotNull LocalDate scheduledDate,
            @Size(max = 500) String note) { }
}
