package com.ccps.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ElectronicSignatureInvitationRequest(
        @NotBlank(message = "Signing token is required") String token,
        @NotBlank(message = "Recipient email is required") @Email(message = "Recipient email is invalid")
        @Size(max = 190, message = "Recipient email is too long") String recipientEmail) { }
