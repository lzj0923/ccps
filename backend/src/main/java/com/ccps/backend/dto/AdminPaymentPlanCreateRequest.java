package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminPaymentPlanCreateRequest(
        @NotNull Long purchaseContractId,
        @NotBlank @Size(max = 120) String planName,
        LocalDate startDate,
        @NotEmpty @Size(max = 100) List<@Valid Installment> installments) {

    public record Installment(
            @Size(max = 160) String milestone,
            @NotNull LocalDate dueDate,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amountDue) {
    }
}
