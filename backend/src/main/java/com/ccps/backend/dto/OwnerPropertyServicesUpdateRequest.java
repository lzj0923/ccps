package com.ccps.backend.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OwnerPropertyServicesUpdateRequest(
        @NotEmpty(message = "At least one property service is required")
        @Size(max = 3)
        List<@Pattern(regexp = "RENTAL|RESALE|MANAGEMENT") String> services) {
}
