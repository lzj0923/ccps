package com.ccps.backend.dto;

public record ElectronicSignatureParticipantResponse(
        String signerRole,
        Integer signingOrder,
        String signerName,
        String signerEmail,
        Integer expiresInDays) {}
