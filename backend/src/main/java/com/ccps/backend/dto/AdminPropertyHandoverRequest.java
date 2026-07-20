package com.ccps.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record AdminPropertyHandoverRequest(
        @NotNull LocalDate handoverDate,
        @Size(max = 2000) String conditionSummary,
        @Min(0) Integer keyCount,
        @Min(0) Integer accessCardCount,
        @Size(max = 120) String waterMeter,
        @Size(max = 120) String electricityMeter,
        String inventory,
        @Size(max = 160) String receivedBy,
        @Size(max = 1000) String notes,
        @NotNull Boolean completed) {}
