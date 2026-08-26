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
        String signatureStatus,
        Long rentalSpaceId,
        String rentalSpaceName,
        String rentalSpaceType,
        String tenantIdentity,
        String tenantPhone,
        String tenantEmail) {
    public AdminPropertyLeaseOptionResponse(Long leaseId, Long rentalMandateId, String leaseNo,
            Long tenantId, String tenantName, LocalDate startDate, LocalDate endDate,
            BigDecimal monthlyRent, BigDecimal depositAmount, Integer paymentDay, String status,
            boolean linked, String signatureStatus) {
        this(leaseId, rentalMandateId, leaseNo, tenantId, tenantName, startDate, endDate,
                monthlyRent, depositAmount, paymentDay, status, linked, signatureStatus,
                null, null, null, null, null, null);
    }

    public AdminPropertyLeaseOptionResponse(Long leaseId, Long rentalMandateId, String leaseNo,
            Long tenantId, String tenantName, LocalDate startDate, LocalDate endDate,
            BigDecimal monthlyRent, BigDecimal depositAmount, Integer paymentDay, String status,
            boolean linked, String signatureStatus, Long rentalSpaceId, String rentalSpaceName,
            String rentalSpaceType) {
        this(leaseId, rentalMandateId, leaseNo, tenantId, tenantName, startDate, endDate,
                monthlyRent, depositAmount, paymentDay, status, linked, signatureStatus,
                rentalSpaceId, rentalSpaceName, rentalSpaceType, null, null, null);
    }
}
