package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminLeasePeriodResponse(
        Long id,
        Long leaseId,
        Integer periodNo,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyRent,
        BigDecimal depositAmount,
        Integer paymentDay,
        String rentCalculationMethod,
        String status,
        Long contractDocumentId,
        String contractDocumentName) {
}
