package com.ccps.backend.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminOwnerCreateRequest(
        @Size(max = 30) String ownerNo,
        @NotBlank @Size(max = 160) String fullName,
        @Size(max = 120) String identityNo,
        @Size(max = 40) @Pattern(regexp = "^$|^\\+[1-9][0-9]{7,14}$", message = "Owner phone number must use E.164 format") String phone,
        @Size(max = 40) @Pattern(regexp = "^$|^\\+[1-9][0-9]{7,14}$", message = "Owner phone number must use E.164 format") String mobilePhone,
        @Size(max = 40) String homePhone,
        @Size(max = 40) String officePhone,
        @Size(max = 80) String passportNo,
        @Email @Size(max = 190) String email,
        @NotBlank @Pattern(regexp = "active|inactive") String status,
        List<Long> responsibleUserIds) {

    public AdminOwnerCreateRequest(String fullName, String identityNo, String phone, String email, String status) {
        this(null, fullName, identityNo, phone, phone, null, null, null, email, status, null);
    }

    public AdminOwnerCreateRequest(String ownerNo, String fullName, String identityNo, String phone,
            String mobilePhone, String homePhone, String officePhone, String passportNo, String email,
            String status) {
        this(ownerNo, fullName, identityNo, phone, mobilePhone, homePhone, officePhone, passportNo,
                email, status, null);
    }
}
