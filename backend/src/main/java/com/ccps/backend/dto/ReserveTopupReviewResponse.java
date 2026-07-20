package com.ccps.backend.dto;

import java.math.BigDecimal;

public record ReserveTopupReviewResponse(
        Long financeRecordId,
        String confirmationStatus,
        BigDecimal balanceAfter) { }
