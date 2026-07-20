package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminOwnerResponse(
        Long id,
        String fullName,
        String identityNo,
        String phone,
        String email,
        String status,
        List<Property> properties) {

    public record Property(
            Long ownerUnitId,
            Long unitId,
            Long projectId,
            String projectName,
            String address,
            String city,
            String building,
            String floorNo,
            String unitNo,
            String unitType,
            BigDecimal areaSqm,
            Integer bedroomCount,
            String listingStatus,
            String assetStage,
            LocalDate expectedHandoverDate,
            LocalDate actualHandoverDate,
            List<String> services,
            BigDecimal ownershipPercent,
            boolean primary,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal purchasePrice,
            BigDecimal paidAmount,
            BigDecimal remainingAmount,
            String paymentStatus) {
    }
}
