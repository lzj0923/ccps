package com.ccps.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ElectronicSignatureSignRequest(
        @NotBlank(message = "Signer name is required") String signerName,
        @NotBlank(message = "Handwritten signature is required") String signatureDataUrl,
        @Size(max = 120, message = "Identity number is too long") String identityNo,
        @NotNull(message = "Consent is required") Boolean consent) { }
