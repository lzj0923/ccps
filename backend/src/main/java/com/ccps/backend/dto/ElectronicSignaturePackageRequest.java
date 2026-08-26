package com.ccps.backend.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

public record ElectronicSignaturePackageRequest(
        @NotEmpty(message = "Signers are required") List<@Valid ElectronicSignatureParticipantRequest> signers,
        @Min(value = 1, message = "Expiry must be at least one day")
        @Max(value = 30, message = "Expiry cannot exceed 30 days") Integer expiresInDays) {}
