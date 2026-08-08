package com.ccps.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminSystemBackupResponse(List<Item> backups) {
    public record Item(String fileName, long size, LocalDateTime createdAt, String type) { }
    public record RestoreResult(String message, String safetyBackupFileName) { }
}
