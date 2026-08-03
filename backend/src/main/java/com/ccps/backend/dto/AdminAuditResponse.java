package com.ccps.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminAuditResponse(List<Item> items, Page page) {
    public record Item(Long id, Long actorUserId, String actorName, String action, String entityType,
            Long entityId, String beforeData, String afterData, String ipAddress, LocalDateTime createdAt) { }
    public record Page(long total, int page, int pageSize, int totalPages) { }
}
