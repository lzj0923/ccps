package com.ccps.backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminPropertyMaintenanceRecordRequest(
        @NotBlank @Size(max = 60) String category,
        @NotBlank @Size(max = 180) String title,
        @NotNull LocalDate maintenanceDate,
        @NotNull @Min(0) Integer durationMinutes,
        @Size(max = 1000) String details,
        @Size(max = 500) String resultSummary,
        LocalDate nextMaintenanceDate) {
}
