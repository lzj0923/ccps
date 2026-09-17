package com.ccps.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ElectronicSignatureStartRequest(
        @NotBlank(message = "Signer name is required") String signerName,
        @Email(message = "Signer email is invalid") @Size(max = 190, message = "Signer email is too long")
        String signerEmail,
        @Min(value = 1, message = "Expiry must be at least one day") @Max(value = 30, message = "Expiry cannot exceed 30 days")
        Integer expiresInDays,
        String signerRole) {
    public ElectronicSignatureStartRequest(String signerName, String signerEmail, Integer expiresInDays) {
        this(signerName, signerEmail, expiresInDays, null);
    }
}
