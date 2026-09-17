package com.ccps.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ElectronicSignatureParticipantRequest(
        @NotBlank(message = "Signer role is required") String signerRole,
        @NotBlank(message = "Signer name is required") String signerName,
        @Email(message = "Signer email is invalid") @Size(max = 190, message = "Signer email is too long")
        String signerEmail) {}
