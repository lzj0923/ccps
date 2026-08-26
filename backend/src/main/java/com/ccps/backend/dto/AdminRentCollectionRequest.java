package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonAlias;

public record AdminRentCollectionRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 16, fraction = 2) BigDecimal amount,
        @NotNull @JsonAlias("paymentDate") LocalDate receivedDate,
        @NotNull LocalDate postingDate,
        @NotBlank @Size(max = 40) String paymentMethod,
        @Size(max = 160) String payerName,
        @Size(max = 120) String paymentReference,
        @Size(max = 500) String note,
        @Size(max = 500) String allocationNote,
        Boolean reuseAllocationNote,
        Boolean convertExcessToPrepayment) {
    public AdminRentCollectionRequest {
        if (postingDate == null) postingDate = receivedDate;
    }

    public AdminRentCollectionRequest(BigDecimal amount,LocalDate receivedDate,String paymentMethod,String payerName,
            String paymentReference,String note,Boolean convertExcessToPrepayment) {
        this(amount,receivedDate,receivedDate,paymentMethod,payerName,paymentReference,note,null,null,convertExcessToPrepayment);
    }
}
