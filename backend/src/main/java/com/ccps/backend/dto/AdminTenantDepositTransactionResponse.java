package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminTenantDepositTransactionResponse(
        Long id,
        Long leaseId,
        String leaseNo,
        Long tenantId,
        Long unitId,
        String projectName,
        String unitNo,
        Long financeRecordId,
        String transactionType,
        String direction,
        BigDecimal amount,
        BigDecimal balanceAfter,
        LocalDate occurredOn,
        String description,
        String status) { }
