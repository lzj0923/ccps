package com.ccps.backend.dto;

public record AdminAccountResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String phone,
        String accountType,
        String staffRole,
        String status,
        Long ownerId) {
}
