package com.ccps.backend.dto;

import java.time.LocalDateTime;

public record AdminPropertyBankAccountResponse(
        Long id, Long ownerUnitId, String itemName, String paymentName,
        String accountNo, String remarks, Long createdBy, String createdByName,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
