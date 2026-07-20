package com.ccps.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminProjectCreateRequest(
        @NotBlank @Size(max = 40) String projectCode,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 255) String address,
        @Size(max = 100) String city,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String countryCode,
        @NotBlank @Pattern(regexp = "active|inactive") String status) {
}
