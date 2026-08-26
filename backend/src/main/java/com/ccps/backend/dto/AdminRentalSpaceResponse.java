package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminRentalSpaceResponse(
        Long id, Long unitId, String spaceCode, String spaceName, String spaceType,
        Integer capacity, BigDecimal areaSqm, BigDecimal recommendedRent, String status,
        Long currentLeaseId, String currentLeaseNo, String tenantName,
        LocalDate leaseStart, LocalDate leaseEnd) { }
