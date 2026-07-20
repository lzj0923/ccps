package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminSyncPreviewResponse(int eligibleCount, BigDecimal totalAmount,
                                       List<String> warnings, List<Item> items) {
    public record Item(Long id, String transactionNo, String recordType, LocalDate transactionDate,
                       String partyName, String projectName, String unitNo, BigDecimal amount,
                       String currency, String syncStatus) {
    }
}
