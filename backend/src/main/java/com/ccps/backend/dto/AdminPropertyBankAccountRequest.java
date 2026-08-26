package com.ccps.backend.dto;

import java.math.BigDecimal;

public record AdminPropertyBankAccountRequest(
        String itemName,
        String paymentName,
        String accountNo,
        String bankAddress,
        String branchCode,
        String swiftCode,
        BigDecimal transferLimit,
        Boolean overseasBank,
        BigDecimal overseasTransferFee,
        String remarks) {}
