package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminPropertyContractResponse(Long id, Long ownerUnitId, String contractNo,
        BigDecimal purchasePrice, String currency, LocalDate signedDate, LocalDate handoverDate,
        String status) {}
