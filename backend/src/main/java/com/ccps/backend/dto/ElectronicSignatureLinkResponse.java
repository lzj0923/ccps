package com.ccps.backend.dto;

import java.time.LocalDateTime;

public record ElectronicSignatureLinkResponse(
        Long requestId,
        String signerRole,
        String signerName,
        String signerEmail,
        String signingUrl,
        LocalDateTime expiresAt) { }
