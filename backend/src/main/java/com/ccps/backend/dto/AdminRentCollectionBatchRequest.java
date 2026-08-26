package com.ccps.backend.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonAlias;

public record AdminRentCollectionBatchRequest(
        @NotEmpty @Size(max = 100) List<Long> invoiceIds,
        @NotNull @JsonAlias("paymentDate") LocalDate receivedDate,
        @NotNull LocalDate postingDate,
        @NotBlank @Size(max = 40) String paymentMethod,
        @Size(max = 120) String paymentReference,
        @Size(max = 500) String note,
        @Size(max = 500) String allocationNote,
        Boolean reuseAllocationNote) {
    public AdminRentCollectionBatchRequest {
        if (postingDate == null) postingDate = receivedDate;
    }

    public AdminRentCollectionBatchRequest(List<Long> invoiceIds,LocalDate receivedDate,String paymentMethod,
            String paymentReference,String note) {
        this(invoiceIds,receivedDate,receivedDate,paymentMethod,paymentReference,note,null,null);
    }
}
