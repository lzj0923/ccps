package com.ccps.backend.dto;

public record AdminVendorResponse(Long id, String vendorCode, String name, String contactName,
        String phone, String email, String status) {
}
