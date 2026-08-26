package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminReserveRefundRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal amount,
        @NotNull LocalDate paymentDate,
        @NotBlank @Size(max = 40) String paymentMethod,
        @Size(max = 500) String note) { }
