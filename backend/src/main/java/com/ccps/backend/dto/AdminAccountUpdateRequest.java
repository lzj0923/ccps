package com.ccps.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminAccountUpdateRequest(
        @Size(max = 80) String username,
        @Size(min = 6, max = 120) String password,
        @Size(max = 120) String displayName,
        @Email @Size(max = 190) String email,
        @Size(max = 40) String phone,
        @Pattern(regexp = "ADMIN|OWNER") String accountType,
        @Pattern(regexp = "active|inactive") String status) {
}
