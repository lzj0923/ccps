package com.ccps.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ElectronicSignatureSignRequest(
        @NotBlank(message = "Signer name is required") String signerName,
        @NotBlank(message = "Verification code is required") String verificationCode,
        @NotBlank(message = "Handwritten signature is required") String signatureDataUrl,
        @NotNull(message = "Consent is required") Boolean consent) { }
