package com.ccps.backend.dto;

import java.time.LocalDate;

public record AdminPropertyImportantMessageRequest(
        String subject,
        String content,
        LocalDate announcementStartDate,
        LocalDate announcementEndDate,
        String importance,
        boolean read) {}
