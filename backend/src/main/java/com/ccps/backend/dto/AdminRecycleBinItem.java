package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRecycleBinItem(
        Long id,
        String entityType,
        Long entityId,
        String referenceNo,
        String title,
        String projectName,
        String unitNo,
        BigDecimal amount,
        String status,
        LocalDateTime deletedAt,
        LocalDateTime expiresAt) {
}
