package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.ccps.backend.model.PropertyStatus;

public record PropertyResponse(
        Long id,
        String name,
        String projectName,
        String address,
        BigDecimal price,
        Integer area,
        Integer bedrooms,
        PropertyStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
