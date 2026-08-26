package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OwnerPropertyCashflowResponse(
        Long ownerUnitId,
        String projectName,
        String unitNo,
        List<CashflowItem> records) {

    public record CashflowItem(
            String key,
            Long sourceId,
            String source,
            String direction,
            String category,
            String description,
            BigDecimal amount,
            LocalDate occurredOn,
            String status,
            BigDecimal balanceAfter) {
    }
}
