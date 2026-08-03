package com.ccps.backend.dto;

import java.time.LocalDateTime;

public record ElectronicSignatureStartResponse(Long requestId, String signingUrl, LocalDateTime expiresAt) { }
