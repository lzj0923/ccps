package com.ccps.backend.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminTenantDepositTransactionDeleteRequest(
        @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> transactionIds) { }
