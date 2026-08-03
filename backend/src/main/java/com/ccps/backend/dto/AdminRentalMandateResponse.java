package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminRentalMandateResponse(List<Item> rows, Summary summary, Page page) {
    public record Item(Long id, String mandateNo, Long ownerUnitId, Long ownerId, String ownerName, String ownerIdentity, String ownerEmail,
            Long projectId, String projectName, String unitNo, String mandateType,
            LocalDate startDate, LocalDate endDate, BigDecimal managementFee,
            BigDecimal commissionPercent, Long responsibleUserId, String responsibleUserName,
            String status, String reviewNote, String terminationReason,
            LocalDateTime submittedAt, LocalDateTime reviewedAt, String reviewedByName,
            LocalDateTime createdAt) { }

    public record Summary(long total, long draft, long pendingReview, long active,
            long suspended, long terminated) { }

    public record Page(long totalRows, int page, int pageSize, int totalPages) { }

    public record Option(Long id, String label, Long ownerId, String ownerName,
            Long projectId, String projectName, String unitNo) { }

    public record UserOption(Long id, String name) { }

    public record Options(List<Option> units, List<UserOption> users) { }

    public record History(Long id, Long mandateId, String fromStatus, String toStatus,
            String reason, Long changedBy, String changedByName, LocalDateTime changedAt) { }
}
