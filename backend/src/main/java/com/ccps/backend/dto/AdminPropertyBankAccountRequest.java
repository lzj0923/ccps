package com.ccps.backend.dto;

public record AdminPropertyBankAccountRequest(
        String itemName,
        String paymentName,
        String accountNo,
        String remarks) {}
