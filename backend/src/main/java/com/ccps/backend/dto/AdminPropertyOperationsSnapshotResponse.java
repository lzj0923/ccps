package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminPropertyOperationsSnapshotResponse(
        LeaseSummary lease,
        BillingSummary billing,
        List<ChargeItem> charges,
        List<WorkOrderItem> workOrders) {

    public record LeaseSummary(Long id, String leaseNo, Long tenantId, String tenantName,
            LocalDate startDate, LocalDate endDate, BigDecimal monthlyRent) {}

    public record BillingSummary(Long invoiceId, LocalDate billingMonth, LocalDate dueDate,
            BigDecimal amountDue, BigDecimal amountPaid, BigDecimal amountUnpaid, String status) {}

    public record ChargeItem(Long id, String chargeType, String description, BigDecimal amount,
            String payer, String sourceType, Long sourceId, LocalDateTime createdAt) {}

    public record WorkOrderItem(Long id, String workOrderNo, String category, String title,
            String description, LocalDateTime requestedAt, String status, BigDecimal estimatedAmount,
            BigDecimal actualAmount, Long vendorId, String vendorName) {}
}
