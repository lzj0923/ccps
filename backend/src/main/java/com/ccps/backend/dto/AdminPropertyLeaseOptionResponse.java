package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminPropertyLeaseOptionResponse(
        Long leaseId,
        Long rentalMandateId,
        String leaseNo,
        Long tenantId,
        String tenantName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyRent,
        BigDecimal depositAmount,
        Integer paymentDay,
        String status,
        boolean linked,
        String signatureStatus) {
}
