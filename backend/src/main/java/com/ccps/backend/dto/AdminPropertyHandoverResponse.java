package com.ccps.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminPropertyHandoverResponse(
        Long id, Long mandateId, String mandateNo, Long ownerUnitId, String projectName, String unitNo,
        LocalDate handoverDate, String conditionSummary, Integer keyCount, Integer accessCardCount,
        String waterMeter, String electricityMeter, String inventory, String receivedBy, String notes,
        String status, Long completedBy, String completedByName, LocalDateTime completedAt) {}
