package com.ccps.backend.dto;

import java.time.LocalDateTime;

public record AdminPropertyManagementHistoryResponse(Long id, String action, String reasonCode, String note,
        Long changedBy, String changedByName, LocalDateTime createdAt) {}
