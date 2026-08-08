package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminDepositAccountDetailResponse(
        Account account,
        Bill bill,
        Reserve reserve,
        List<AdminTenantDepositTransactionResponse> transactions,
        List<String> allowedActions) {

    public record Account(Long leaseId, String leaseNo, Long tenantId, String tenantName, String tenantPhone,
            String projectName, String unitNo, String leaseStatus, LocalDate startDate, LocalDate endDate,
            BigDecimal expectedDeposit, BigDecimal postedBalance, BigDecimal availableBalance,
            String collectionStatus, String accountStatus) { }

    public record Bill(Long financeRecordId, String transactionNo, BigDecimal amount, LocalDate billDate,
            String entryStatus, String confirmationStatus, String paymentStatus) { }

    public record Reserve(Long ownerId, String ownerName, Long reserveAccountId, BigDecimal currentBalance) { }
}
