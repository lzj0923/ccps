package com.ccps.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminSyncResponse(Summary summary, List<Batch> batches, List<Item> items) {
    public record Summary(long eligibleCount, long exportedCount, long failedCount,
                          long batchCount, long itemCount) {
    }
    public record Batch(Long id, String batchNo, String sourceModule, String triggerMode,
                        String status, int totalCount, int successCount, int failureCount,
                        LocalDateTime startedAt, LocalDateTime completedAt,
                        String createdByName, LocalDateTime createdAt) {
    }
    public record Item(Long id, Long batchId, String batchNo, String entityType, Long entityId,
                       String operation, String status, String externalId, String errorMessage,
                       String transactionNo, LocalDateTime syncedAt) {
    }
}
