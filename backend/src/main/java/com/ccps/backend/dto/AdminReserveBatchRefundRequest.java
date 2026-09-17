package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminReserveBatchRefundRequest(
        @NotEmpty @Size(max = 100) List<@Valid Item> items,
        @NotNull LocalDate paymentDate,
        @NotBlank @Size(max = 40) String paymentMethod,
        @Size(max = 500) String note) {
    public record Item(
            @NotNull Long accountId,
            Long bankAccountId,
            @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal amount,
            @Size(max = 40) String paymentMethod) {
        public Item(Long accountId, Long bankAccountId, BigDecimal amount) {
            this(accountId, bankAccountId, amount, null);
        }
    }
}
