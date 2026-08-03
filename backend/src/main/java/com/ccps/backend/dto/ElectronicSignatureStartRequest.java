package com.ccps.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ElectronicSignatureStartRequest(
        @NotBlank(message = "Signer name is required") String signerName,
        @NotBlank(message = "Signer email is required") @Email(message = "Signer email is invalid") String signerEmail,
        @Min(value = 1, message = "Expiry must be at least one day") @Max(value = 30, message = "Expiry cannot exceed 30 days")
        Integer expiresInDays) { }
