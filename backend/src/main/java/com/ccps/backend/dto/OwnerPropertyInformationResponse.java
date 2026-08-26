package com.ccps.backend.dto;

import java.time.LocalDateTime;

public record OwnerPropertyInformationResponse(
        Long id,
        String category,
        String title,
        String body,
        String priority,
        String status,
        LocalDateTime createdAt,
        String projectName,
        String unitNo,
        String city,
        String number,
        String detail,
        String message) {
}
