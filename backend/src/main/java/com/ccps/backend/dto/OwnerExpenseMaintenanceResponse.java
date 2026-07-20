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
            Long projectId,
            String projectName,
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
            int attachmentCount) {
    }

    public record MaintenanceItem(
            Long id,
            String workOrderNo,
            Long projectId,
            String projectName,
            String unitNo,
            String category,
            String title,
            LocalDateTime requestedAt,
            LocalDateTime completedAt,
            String status,
            BigDecimal amount,
            BigDecimal reserveDeductedAmount,
            int attachmentCount) {
    }
}
