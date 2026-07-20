package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminReserveManagementResponse(Summary summary, List<String> projects,
        List<Account> accounts, List<Transaction> transactions) {
    public record Summary(BigDecimal totalBalance, BigDecimal minimumBalance, long accountCount,
            long lowBalanceCount, BigDecimal monthlyTopups, BigDecimal monthlyDebits,
            long pendingTopupCount, BigDecimal pendingTopupAmount) { }

    public record Account(Long id, Long ownerId, String ownerName, Long projectId, String projectName,
            Long unitId, String unitNo, BigDecimal minimumBalance, BigDecimal currentBalance,
            BigDecimal shortageAmount, BigDecimal totalTopups, BigDecimal totalDebits,
            LocalDateTime lastMovementAt, boolean lowBalanceAlertEnabled, String balanceStatus,
            long pendingTopupCount, BigDecimal pendingTopupAmount) { }

    public record Transaction(Long id, Long reserveAccountId, Long financeRecordId, Long workOrderId,
            String transactionType, BigDecimal amount, BigDecimal balanceAfter, String note,
            LocalDateTime occurredAt, String transactionNo, String workOrderNo, String createdByName) { }
}
