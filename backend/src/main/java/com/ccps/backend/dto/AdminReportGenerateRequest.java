package com.ccps.backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AdminReportGenerateRequest(
        @NotBlank @Pattern(regexp = "property_payment|rent_collection|income_expense|maintenance|reserve|finance|sync") String reportType,
        @NotNull LocalDate dateStart,
        @NotNull LocalDate dateEnd,
        Long projectId,
        @NotBlank @Pattern(regexp = "PDF|XLSX") String outputFormat) {
}
