package com.ccps.backend.dto;

import java.time.LocalDate;

public record AdminAccountResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String phone,
        String accountType,
        String staffRole,
        String status,
        Long ownerId,
        String employeeNo,
        String department,
        String jobTitle,
        LocalDate hireDate,
        LocalDate leaveDate,
        String employmentStatus) {
    public AdminAccountResponse(Long id, String username, String email, String displayName, String phone,
            String accountType, String staffRole, String status, Long ownerId) {
        this(id, username, email, displayName, phone, accountType, staffRole, status, ownerId,
                null, null, null, null, null, null);
    }
}
