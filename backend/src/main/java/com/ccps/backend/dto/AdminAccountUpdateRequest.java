package com.ccps.backend.dto;

import java.time.LocalDate;

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
        @Pattern(regexp = "SUPER_ADMIN|FINANCE|BUSINESS|CUSTOMER_SERVICE|ADMINISTRATION") String staffRole,
        @Pattern(regexp = "active|inactive") String status,
        @Size(max = 50) String employeeNo,
        @Size(max = 120) String department,
        @Size(max = 120) String jobTitle,
        LocalDate hireDate,
        LocalDate leaveDate,
        @Pattern(regexp = "active|left") String employmentStatus) {
    public AdminAccountUpdateRequest(String username, String password, String displayName, String email,
            String phone, String accountType, String staffRole, String status) {
        this(username, password, displayName, email, phone, accountType, staffRole, status,
                null, null, null, null, null, null);
    }
}
