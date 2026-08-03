package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminLeasePaymentUpdateRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate paymentDate,
        @NotBlank @Pattern(regexp="cash|bank_transfer|online_payment") String paymentMethod,
        @Size(max=160) String payerName,
        @Size(max=120) String paymentReference,
        @Size(max=500) String note) {}
