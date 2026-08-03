package com.ccps.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public record AdminPropertyContractRecordResponse(
        Long id,
        Long ownerUnitId,
        Long leaseId,
        String leaseNo,
        String tenantName,
        LocalDate leaseStart,
        LocalDate leaseEnd,
        BigDecimal monthlyRent,
        String leaseStatus,
        String contractType,
        String contractNo,
        LocalDate signedDate,
        LocalDate validFrom,
        LocalDate validTo,
        String status,
        String notes,
        String originalName,
        String mimeType,
        Long fileSize,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
