package com.ccps.backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminInstallmentUpdateRequest(
        @Size(max = 160) String milestone,
        @NotNull LocalDate dueDate) {
}
