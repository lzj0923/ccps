package com.ccps.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminTenantCreateRequest(
        @NotBlank @Size(max = 160) String fullName,
        @Size(max = 120) String identityNo,
        @Size(max = 40) String phone,
        @Email @Size(max = 190) String email,
        @NotBlank @Pattern(regexp = "active|inactive") String status) { }
