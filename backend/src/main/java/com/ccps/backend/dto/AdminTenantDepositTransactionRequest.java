package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminTenantDepositTransactionRequest(
        @NotBlank @Pattern(regexp = "tenant_advance|tenant_repayment|refund|forfeiture|adjustment_credit|adjustment_debit") String transactionType,
        @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal amount,
        @NotNull LocalDate occurredOn,
        @Size(max = 500) String description) { }
