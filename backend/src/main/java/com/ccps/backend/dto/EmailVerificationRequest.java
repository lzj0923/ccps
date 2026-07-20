package com.ccps.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailVerificationRequest(
        @NotBlank @Email @Size(max = 190) String email,
        @NotBlank @Pattern(regexp = "\\d{6}") String code) {
}
