package com.ccps.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ElectronicSignatureParticipantRequest(
        @NotBlank(message = "Signer role is required") String signerRole,
        @NotBlank(message = "Signer name is required") String signerName,
        @NotBlank(message = "Signer email is required") @Email(message = "Signer email is invalid") String signerEmail) {}
