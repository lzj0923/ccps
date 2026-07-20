package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OwnerReserveResponse(
        Summary summary,
        List<Account> accounts,
        List<PropertyOption> properties,
        List<TransactionItem> transactions,
        List<NotificationItem> notifications,
        List<DocumentItem> documents) {

    public record Summary(
            BigDecimal totalBalance,
            BigDecimal minimumBalance,
            BigDecimal totalTopups,
            int topupCount,
            BigDecimal totalDebits,
            int debitCount,
            int lowBalanceCount,
            int accountCount) { }

    public record Account(
            Long id,
            Long ownerUnitId,
            Long projectId,
            String projectName,
            String unitNo,
            BigDecimal minimumBalance,
            BigDecimal currentBalance,
            String balanceStatus,
            boolean lowBalanceAlertEnabled) { }

    public record PropertyOption(Long id, String name) { }

    public record TransactionItem(
            Long id,
            Long financeRecordId,
            Long reserveAccountId,
            Long projectId,
            String projectName,
            String unitNo,
            LocalDateTime occurredAt,
            String transactionType,
            String description,
            BigDecimal amount,
            BigDecimal balanceAfter,
            String status,
            String confirmationStatus,
            int attachmentCount) { }

    public record NotificationItem(
            Long id,
            String title,
            String body,
            String priority,
            String status,
            LocalDateTime createdAt) { }

    public record DocumentItem(
            Long id,
            String name,
            String mimeType,
            long size,
            String documentType,
            String status,
            LocalDateTime uploadedAt) { }
}
