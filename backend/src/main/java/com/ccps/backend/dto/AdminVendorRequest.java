package com.ccps.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminVendorRequest(
        @NotBlank(message = "Vendor name is required") String name,
        String contactName,
        String phone,
        @Email(message = "Email format is invalid") String email,
        String status) {
}
