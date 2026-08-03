package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record AdminPropertyContractRequest(@NotBlank String contractNo, @DecimalMin("0.00") BigDecimal purchasePrice,
        LocalDate signedDate, LocalDate handoverDate) {}
