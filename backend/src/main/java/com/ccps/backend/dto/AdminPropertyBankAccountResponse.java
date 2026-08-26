package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPropertyBankAccountResponse(
        Long id, Long ownerUnitId, String itemName, String paymentName,
        String accountNo, String bankAddress, String branchCode, String swiftCode,
        BigDecimal transferLimit, boolean overseasBank, BigDecimal overseasTransferFee,
        String remarks, Long createdBy, String createdByName,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
