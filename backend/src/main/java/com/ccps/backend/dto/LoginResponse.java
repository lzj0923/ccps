package com.ccps.backend.dto;

import java.util.List;

public record LoginResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String status,
        String role,
        List<String> roles) {
}
