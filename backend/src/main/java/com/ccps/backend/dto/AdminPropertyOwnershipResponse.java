package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminPropertyOwnershipResponse(
        Long ownershipId,
        Long unitId,
        Long ownerId,
        String ownerNo,
        String ownerName,
        String identityNo,
        String mobilePhone,
        String email,
        BigDecimal ownershipPercent,
        boolean primary,
        LocalDate startDate,
        LocalDate endDate,
        String status) {
}
