package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminReserveReconciliationResponse(
        Long id, LocalDate reconciliationMonth, BigDecimal systemBalance,
        BigDecimal financeBalance, BigDecimal differenceAmount, String status,
        String note, String confirmedByName, LocalDateTime confirmedAt) { }
