package com.ccps.backend.dto;

import java.math.BigDecimal;

public record AdminPaymentPlanCreateResponse(
        Long id,
        Long purchaseContractId,
        String planName,
        int installmentCount,
        BigDecimal totalAmount) {
}
