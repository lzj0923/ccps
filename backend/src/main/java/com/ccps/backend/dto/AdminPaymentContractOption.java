package com.ccps.backend.dto;

import java.math.BigDecimal;

public record AdminPaymentContractOption(
        Long contractId,
        Long ownerUnitId,
        Long projectId,
        String projectName,
        String unitNo,
        String ownerName,
        String contractNo,
        BigDecimal purchasePrice,
        String currency,
        boolean hasActivePlan) {
}
