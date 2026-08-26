package com.ccps.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminAccountCreateRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Size(min = 6, max = 120) String password,
        @NotBlank @Size(max = 120) String displayName,
        @Email @Size(max = 190) String email,
        @Size(max = 40) String phone,
        @NotBlank @Pattern(regexp = "ADMIN|OWNER") String accountType,
        @Pattern(regexp = "SUPER_ADMIN|FINANCE|BUSINESS|CUSTOMER_SERVICE|ADMINISTRATION") String staffRole,
        @NotBlank @Pattern(regexp = "active|inactive") String status) {
}
