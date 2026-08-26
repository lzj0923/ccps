package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminReserveManagementResponse(Summary summary, List<String> projects,
        List<Account> accounts, List<Transaction> transactions, List<RefundBankAccount> refundBankAccounts) {
    public record Summary(BigDecimal totalBalance, BigDecimal accountingBalance, BigDecimal minimumBalance, long accountCount,
            long lowBalanceCount, BigDecimal monthlyTopups, BigDecimal monthlyDebits,
            long pendingTopupCount, BigDecimal pendingTopupAmount) { }

    public record Account(Long id, Long ownerId, String ownerName, Long projectId, String projectName,
            Long unitId, String unitNo, LocalDate leaseStartDate, LocalDate leaseEndDate, String remarks, BigDecimal minimumBalance, BigDecimal currentBalance,
            BigDecimal accountingBalance,
            String minimumBalanceMode, BigDecimal calculatedMinimumBalance, BigDecimal rentBufferAmount,
            BigDecimal monthlyExpenseAverage, int expenseBufferMonths, LocalDateTime minimumBalanceCalculatedAt,
            BigDecimal shortageAmount, BigDecimal totalTopups, BigDecimal totalDebits,
            LocalDateTime lastMovementAt, boolean lowBalanceAlertEnabled, String balanceStatus,
            long pendingTopupCount, BigDecimal pendingTopupAmount) { }

    public record Transaction(Long id, Long reserveAccountId, Long financeRecordId, Long workOrderId,
            String transactionType, BigDecimal amount, BigDecimal balanceAfter, String note,
            LocalDateTime occurredAt, String transactionNo, String workOrderNo, String createdByName) { }

    public record RefundBankAccount(Long id, Long reserveAccountId, String itemName, String paymentName,
            String accountNo, BigDecimal transferLimit, boolean overseasBank,
            BigDecimal overseasTransferFee) { }
}
