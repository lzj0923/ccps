package com.ccps.backend.dto;

import java.time.LocalDateTime;

public record AdminRentalMandateDocumentResponse(
        Long id,
        String documentNo,
        String originalName,
        String documentType,
        String relationType,
        String mimeType,
        Long fileSize,
        LocalDateTime createdAt,
        String signatureStatus,
        String signatureDocumentKind,
        String signatureSignerRole,
        Integer signatureSigningOrder,
        String signatureSignerName,
        String signatureSignerEmail,
        LocalDateTime signatureRequestedAt,
        LocalDateTime signatureExpiresAt,
        LocalDateTime signatureSignedAt) {}
