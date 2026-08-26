package com.ccps.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ElectronicSignatureStartResponse(
        Long requestId,
        String signingUrl,
        LocalDateTime expiresAt,
        List<ElectronicSignatureLinkResponse> signingLinks) {
    public ElectronicSignatureStartResponse {
        signingLinks = signingLinks == null ? List.of() : List.copyOf(signingLinks);
    }
}
