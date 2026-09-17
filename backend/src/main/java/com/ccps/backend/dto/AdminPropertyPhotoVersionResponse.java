package com.ccps.backend.dto;

import java.time.LocalDateTime;

public record AdminPropertyPhotoVersionResponse(
        String versionMonth,
        int photoCount,
        Long coverPhotoId,
        LocalDateTime updatedAt) {
}
