package com.ccps.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminTenantStatusUpdateRequest(
        @NotBlank @Pattern(regexp = "active|inactive") String status) { }
