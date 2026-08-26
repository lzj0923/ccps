package com.ccps.backend.dto;

import java.time.LocalDateTime;

public record ElectronicSignaturePublicResponse(Long requestId, String documentName, String signerName,
        String signerEmailMasked, String status, LocalDateTime expiresAt, LocalDateTime signedAt,
        boolean canSign, boolean canDownloadSigned, String documentKind, String signerRole) { }
