package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminPropertyOperationsSnapshotResponse(
        LeaseSummary lease,
        BillingSummary billing,
        List<ChargeItem> charges,
        List<WorkOrderItem> workOrders,
        List<LeaseSummary> activeLeases) {

    public AdminPropertyOperationsSnapshotResponse(LeaseSummary lease, BillingSummary billing,
            List<ChargeItem> charges, List<WorkOrderItem> workOrders) {
        this(lease, billing, charges, workOrders, lease == null ? List.of() : List.of(lease));
    }

    public record LeaseSummary(Long id, String leaseNo, Long tenantId, String tenantName,
            LocalDate startDate, LocalDate endDate, BigDecimal monthlyRent,
            Long rentalSpaceId, String rentalSpaceName, String rentalSpaceType) {
        public LeaseSummary(Long id, String leaseNo, Long tenantId, String tenantName,
                LocalDate startDate, LocalDate endDate, BigDecimal monthlyRent) {
            this(id, leaseNo, tenantId, tenantName, startDate, endDate, monthlyRent, null, null, null);
        }
    }

    public record BillingSummary(Long invoiceId, LocalDate billingMonth, LocalDate dueDate,
            BigDecimal amountDue, BigDecimal amountPaid, BigDecimal amountUnpaid, String status) {}

    public record ChargeItem(Long id, String chargeType, String description, BigDecimal amount,
            String payer, String sourceType, Long sourceId, Long financeRecordId,
            String confirmationStatus, LocalDateTime createdAt) {}

    public record WorkOrderItem(Long id, String workOrderNo, String category, String title,
            String description, LocalDateTime requestedAt, String status, BigDecimal estimatedAmount,
            BigDecimal actualAmount, Long vendorId, String vendorName) {}
}
