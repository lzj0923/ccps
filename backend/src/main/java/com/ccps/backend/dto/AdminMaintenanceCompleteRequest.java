package com.ccps.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminMaintenanceCompleteRequest(
        @NotNull @DecimalMin("0.01") BigDecimal actualAmount,
        @NotBlank String settlementMethod,
        @NotBlank @Size(max = 500) String completionNote,
        @Size(max = 160) String payerName,
        @Size(max = 120) String bankName,
        @Size(max = 120) String paymentAccountNo,
        @Size(max = 40) String feeAccountKey,
        @Size(max = 120) String feeAccountNo) {

    /** Backwards-compatible constructor for existing callers and tests. */
    public AdminMaintenanceCompleteRequest(BigDecimal actualAmount, String settlementMethod,
            String completionNote) {
        this(actualAmount, settlementMethod, completionNote, null, null, null, null, null);
    }
}
