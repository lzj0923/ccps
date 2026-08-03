package com.ccps.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminPropertyImportantMessageResponse(
        Long id, Long ownerUnitId, String subject, String content,
        LocalDate announcementStartDate, LocalDate announcementEndDate,
        String importance, boolean read, Long createdBy, String createdByName,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
