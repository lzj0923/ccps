package com.ccps.backend.dto;

import java.math.BigDecimal;

public record AdminMaintenanceHandlingResponse(
        Long workOrderId,
        String status,
        BigDecimal reserveBalance,
        BigDecimal reserveDeductedAmount,
        boolean reserveAccountAvailable,
        int beforePhotoCount,
        int afterPhotoCount) {
}
