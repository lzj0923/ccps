package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminPropertyMaintenanceUpdateRequest(
        Long vendorId,
        @NotBlank @Size(max = 60) String category,
        @NotBlank @Size(max = 180) String title,
        @Size(max = 1000) String description,
        @NotNull LocalDateTime requestedAt,
        BigDecimal estimatedAmount,
        @NotBlank String status,
        @Size(max = 160) String payerName,
        @Size(max = 120) String bankName,
        @Size(max = 120) String paymentAccountNo,
        @Size(max = 40) String feeAccountKey,
        @Size(max = 120) String feeAccountNo) {

    public AdminPropertyMaintenanceUpdateRequest(Long vendorId, String category, String title,
            String description, LocalDateTime requestedAt, BigDecimal estimatedAmount, String status) {
        this(vendorId, category, title, description, requestedAt, estimatedAmount, status,
                null, null, null, null, null);
    }
}
