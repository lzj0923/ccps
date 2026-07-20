package com.ccps.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminRentalMandateStatusRequest(
        @NotBlank @Pattern(regexp = "active|suspended|terminated") String status,
        @Size(max = 500) String reason) {
}
