package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OwnerExpenseMaintenanceResponse(
        Summary summary,
        List<PropertyOption> properties,
        List<String> categories,
        List<ExpenseItem> expenses,
        List<MaintenanceItem> maintenance) {

    public record Summary(
            BigDecimal monthlyExpense,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal expenseChangePercent,
            BigDecimal monthlyMaintenanceExpense,
            BigDecimal maintenanceChangePercent,
            BigDecimal reserveDeductedAmount,
            int reserveDebitCount,
            int pendingMaintenanceCount) {
    }

    public record PropertyOption(Long id, String name) {
    }

    public record ExpenseItem(
            Long id,
            Long financeRecordId,
            Long unitId,
            Long projectId,
            String projectName,
            String state,
            String city,
            String unitNo,
            LocalDate occurredOn,
            String category,
            String description,
            BigDecimal amount,
            BigDecimal reserveDeductedAmount,
            String paymentStatus,
            String confirmationStatus,
            String paymentMethod,
            LocalDate paymentDate,
            Long workOrderId,
            int attachmentCount,
            boolean editable,
            String payerName,
            String bankName,
            String paymentAccountNo,
            String feeAccountKey,
            String feeAccountNo) {
    }

    public record MaintenanceItem(
            Long id,
            String workOrderNo,
            Long unitId,
            Long projectId,
            String projectName,
            String state,
            String city,
            String unitNo,
            Long vendorId,
            String category,
            String title,
            String description,
            LocalDateTime requestedAt,
            LocalDateTime completedAt,
            String status,
            BigDecimal estimatedAmount,
            BigDecimal amount,
            Long cashflowEntryId,
            String confirmationStatus,
            String paymentStatus,
            BigDecimal reserveDeductedAmount,
            int attachmentCount,
            boolean editable,
            String payerName,
            String bankName,
            String paymentAccountNo,
            String feeAccountKey,
            String feeAccountNo) {
    }
}
