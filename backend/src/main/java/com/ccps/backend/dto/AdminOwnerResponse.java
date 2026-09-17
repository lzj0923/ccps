package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminOwnerResponse(
        Long id,
        String ownerNo,
        String fullName,
        String identityNo,
        String phone,
        String mobilePhone,
        String homePhone,
        String officePhone,
        String passportNo,
        String email,
        String mailingAddress,
        String status,
        List<AdminOwnerStaffOption> responsibleStaff,
        List<Property> properties) {

    public AdminOwnerResponse(Long id, String ownerNo, String fullName, String identityNo, String phone,
            String mobilePhone, String homePhone, String officePhone, String passportNo, String email,
            String mailingAddress, String status, List<Property> properties) {
        this(id, ownerNo, fullName, identityNo, phone, mobilePhone, homePhone, officePhone, passportNo,
                email, mailingAddress, status, List.of(), properties);
    }

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
            String paymentStatus,
            String electricityAccountNo,
            String waterAccountNo,
            String sewerageAccountNo,
            String gasAccountNo,
            String withholdingTaxAccountNo,
            String landTaxAccountNo,
            String assessmentTaxAccountNo) {
    }
}
