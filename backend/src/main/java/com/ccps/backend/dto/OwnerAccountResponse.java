package com.ccps.backend.dto;

public record OwnerAccountResponse(String fullName, String mobilePhone, String homePhone,
        String officePhone, String mailingAddress) { }
