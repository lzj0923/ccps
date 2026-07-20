package com.ccps.backend.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminSyncRequest(
        @NotBlank @Pattern(regexp = "all|property_payment|rent_payment|reserve|cashflow") String sourceModule,
        List<Long> recordIds) {
}
