package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OwnerRentIncomeResponse(
        Summary summary,
        List<PropertyOption> properties,
        List<Integer> availableYears,
        List<YearlyTrend> yearlyTrend,
        List<RentRecord> records,
        List<RecentReceipt> recentReceipts) {

    public record Summary(
            int year,
            int month,
            BigDecimal monthlyAmountDue,
            BigDecimal monthlyAmountPaid,
            BigDecimal monthlyUnpaidAmount,
            BigDecimal annualAmountPaid,
            BigDecimal amountDueChangePercent,
            BigDecimal amountPaidChangePercent,
            BigDecimal unpaidChangePercent,
            BigDecimal annualChangePercent) {
    }

    public record PropertyOption(Long id, String name) {
    }

    public record YearlyTrend(int year, BigDecimal amountPaid) {
    }

    public record RentRecord(
            Long invoiceId,
            Long projectId,
            String projectName,
            String unitNo,
            String tenantName,
            LocalDate billingMonth,
            LocalDate dueDate,
            BigDecimal amountDue,
            BigDecimal amountPaid,
            BigDecimal unpaidAmount,
            LocalDate receivedDate,
            String status,
            String confirmationStatus) {
    }

    public record RecentReceipt(
            Long transactionId,
            String projectName,
            String unitNo,
            String tenantName,
            LocalDate receivedDate,
            BigDecimal amount) {
    }
}
