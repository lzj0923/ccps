package com.ccps.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminPropertyOperationsChargeRequest;
import com.ccps.backend.dto.AdminPropertyOperationsWorkOrderRequest;
import com.ccps.backend.dto.AdminPropertyOperationsSnapshotResponse;
import com.ccps.backend.dto.AdminPropertySharedChargeRequest;
import com.ccps.backend.mapper.AdminPropertyOperationsMapper;
import com.ccps.backend.mapper.AdminPropertyOperationsMapper.ChargeRow;
import com.ccps.backend.mapper.AdminPropertyOperationsMapper.InvoiceRow;
import com.ccps.backend.mapper.AdminPropertyOperationsMapper.LeaseRow;
import com.ccps.backend.mapper.AdminPropertyOperationsMapper.WorkOrderRow;
import com.ccps.backend.mapper.AdminPropertyOperationsMapper.ChargeWriteRow;
import com.ccps.backend.mapper.AdminPropertyOperationsMapper.WorkOrderWriteRow;

@Service
public class AdminPropertyOperationsService {
    private final AdminPropertyOperationsMapper mapper;

    public AdminPropertyOperationsService(AdminPropertyOperationsMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public AdminPropertyOperationsSnapshotResponse snapshot(Long ownerId, Long ownerUnitId, LocalDate billingMonth) {
        return snapshot(ownerId, ownerUnitId, billingMonth, null);
    }

    @Transactional(readOnly = true)
    public AdminPropertyOperationsSnapshotResponse snapshot(Long ownerId, Long ownerUnitId, LocalDate billingMonth,
            Long requestedLeaseId) {
        List<LeaseRow> activeLeases = findActiveLeases(ownerId, ownerUnitId);
        LeaseRow lease = selectLease(activeLeases, requestedLeaseId);
        if (lease == null) {
            return new AdminPropertyOperationsSnapshotResponse(null, null, List.of(), List.of(), List.of());
        }
        LocalDate month = billingMonth.withDayOfMonth(1);
        InvoiceRow invoice = mapper.findInvoice(lease.getId(), month);
        List<ChargeRow> charges = invoice == null ? List.of() : mapper.findCharges(invoice.getId());
        List<WorkOrderRow> workOrders = mapper.findWorkOrders(lease.getId());
        return new AdminPropertyOperationsSnapshotResponse(
                new AdminPropertyOperationsSnapshotResponse.LeaseSummary(lease.getId(), lease.getLeaseNo(),
                        lease.getTenantId(), lease.getTenantName(), lease.getStartDate(), lease.getEndDate(), lease.getMonthlyRent(),
                        lease.getRentalSpaceId(), lease.getRentalSpaceName(), lease.getRentalSpaceType()),
                invoice == null ? null : new AdminPropertyOperationsSnapshotResponse.BillingSummary(invoice.getId(),
                        invoice.getBillingMonth(), invoice.getDueDate(), invoice.getAmountDue(), invoice.getAmountPaid(),
                        invoice.getAmountUnpaid(), invoice.getStatus()),
                charges.stream().map(this::charge).toList(),
                workOrders.stream().map(this::workOrder).toList(),
                activeLeases.stream().map(this::leaseSummary).toList());
    }

    @Transactional
    public AdminPropertyOperationsSnapshotResponse addCharge(Long actorId, Long ownerId, Long ownerUnitId,
            LocalDate billingMonth, AdminPropertyOperationsChargeRequest request) {
        return addCharge(actorId, ownerId, ownerUnitId, billingMonth, null, request);
    }

    @Transactional
    public AdminPropertyOperationsSnapshotResponse addCharge(Long actorId, Long ownerId, Long ownerUnitId,
            LocalDate billingMonth, Long requestedLeaseId, AdminPropertyOperationsChargeRequest request) {
        LeaseRow lease = selectLease(findActiveLeases(ownerId, ownerUnitId), requestedLeaseId);
        if (lease == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active lease not found");
        InvoiceRow invoice = mapper.findInvoice(lease.getId(), billingMonth.withDayOfMonth(1));
        if (invoice == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Create the monthly invoice before adding charges");
        if (mapper.lockInvoice(lease.getId(), invoice.getId()) == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice changed; reload and try again");
        }
        ChargeWriteRow row = new ChargeWriteRow();
        row.setInvoiceId(invoice.getId()); row.setChargeType(request.chargeType()); row.setDescription(request.description().trim());
        row.setAmount(request.amount()); row.setPayer(request.payer()); row.setSourceType(request.sourceType());
        prepareChargeReview(row, actorId, ownerId, lease, billingMonth);
        row.setSourceId(request.sourceId());
        if (mapper.insertChargeFinanceReview(row) != 1 || row.getFinanceRecordId() == null
                || mapper.insertCharge(row) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to add charge");
        }
        return snapshot(ownerId, ownerUnitId, billingMonth, lease.getId());
    }

    @Transactional
    public AdminPropertyOperationsSnapshotResponse createWorkOrder(Long actorId, Long ownerId, Long ownerUnitId,
            AdminPropertyOperationsWorkOrderRequest request, LocalDate billingMonth) {
        LeaseRow lease = selectLease(findActiveLeases(ownerId, ownerUnitId), request.leaseId());
        if (lease == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Work order must belong to the current lease");
        }
        WorkOrderWriteRow row = new WorkOrderWriteRow();
        row.setWorkOrderNo("MWO-OPS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        row.setUnitId(lease.getUnitId());
        row.setLeaseId(lease.getId()); row.setOwnerId(ownerId); row.setTenantId(lease.getTenantId()); row.setVendorId(request.vendorId());
        row.setCategory(request.category()); row.setTitle(request.title().trim()); row.setDescription(request.description());
        row.setRequestedAt(request.requestedAt()); row.setEstimatedAmount(request.estimatedAmount()); row.setActorId(actorId);
        if (mapper.insertWorkOrder(row) != 1 || row.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create work order");
        }
        mapper.insertWorkOrderHistory(row.getId(), request.requestedAt(), actorId);
        mapper.insertWorkOrderAudit(actorId, row.getId(), "{\"leaseId\":" + lease.getId() + "}");
        return snapshot(ownerId, ownerUnitId, billingMonth, lease.getId());
    }

    @Transactional
    public AdminPropertyOperationsSnapshotResponse addSharedCharge(Long actorId, Long ownerId, Long ownerUnitId,
            LocalDate billingMonth, Long selectedLeaseId, AdminPropertySharedChargeRequest request) {
        LocalDate month = billingMonth.withDayOfMonth(1);
        LocalDate monthEnd = month.plusMonths(1).minusDays(1);
        List<LeaseRow> roomLeases = findActiveLeases(ownerId, ownerUnitId).stream()
                .filter(lease -> "room".equals(lease.getRentalSpaceType()))
                .filter(lease -> !lease.getStartDate().isAfter(monthEnd) && !lease.getEndDate().isBefore(month))
                .toList();
        if (roomLeases.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Shared charge requires at least two occupied rooms in the selected month");
        }
        List<InvoiceRow> invoices = new ArrayList<>();
        for (LeaseRow lease : roomLeases) {
            InvoiceRow invoice = mapper.findInvoice(lease.getId(), month);
            if (invoice == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Create every room's monthly invoice before allocating a shared charge");
            }
            invoices.add(invoice);
        }
        BigDecimal cents = request.totalAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal base = cents.divide(BigDecimal.valueOf(roomLeases.size()), 2, RoundingMode.DOWN);
        int extraCents = cents.subtract(base.multiply(BigDecimal.valueOf(roomLeases.size())))
                .movePointRight(2).intValueExact();
        for (int index = 0; index < roomLeases.size(); index++) {
            LeaseRow lease = roomLeases.get(index);
            InvoiceRow invoice = invoices.get(index);
            if (mapper.lockInvoice(lease.getId(), invoice.getId()) == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice changed; reload and try again");
            }
            BigDecimal allocated = index < extraCents ? base.add(new BigDecimal("0.01")) : base;
            ChargeWriteRow row = new ChargeWriteRow();
            row.setInvoiceId(invoice.getId()); row.setChargeType(request.chargeType());
            row.setDescription(request.description().trim() + "（合租均分 " + (index + 1) + "/" + roomLeases.size() + "）");
            row.setAmount(allocated); row.setPayer(request.payer()); row.setSourceType("shared_allocation");
            prepareChargeReview(row, actorId, ownerId, lease, month);
            if (mapper.insertChargeFinanceReview(row) != 1 || row.getFinanceRecordId() == null
                    || mapper.insertCharge(row) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to allocate shared charge");
            }
        }
        Long targetLeaseId = selectedLeaseId != null ? selectedLeaseId : roomLeases.get(0).getId();
        return snapshot(ownerId, ownerUnitId, month, targetLeaseId);
    }

    private List<LeaseRow> findActiveLeases(Long ownerId, Long ownerUnitId) {
        List<LeaseRow> rows = mapper.findActiveLeases(ownerId, ownerUnitId);
        if (rows != null && !rows.isEmpty()) return rows;
        LeaseRow legacy = mapper.findActiveLease(ownerId, ownerUnitId);
        return legacy == null ? List.of() : List.of(legacy);
    }

    private LeaseRow selectLease(List<LeaseRow> leases, Long requestedLeaseId) {
        if (leases == null || leases.isEmpty()) return null;
        if (requestedLeaseId == null) return leases.get(0);
        return leases.stream().filter(item -> requestedLeaseId.equals(item.getId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Selected lease does not belong to this property"));
    }

    private AdminPropertyOperationsSnapshotResponse.LeaseSummary leaseSummary(LeaseRow lease) {
        return new AdminPropertyOperationsSnapshotResponse.LeaseSummary(lease.getId(), lease.getLeaseNo(),
                lease.getTenantId(), lease.getTenantName(), lease.getStartDate(), lease.getEndDate(),
                lease.getMonthlyRent(), lease.getRentalSpaceId(), lease.getRentalSpaceName(), lease.getRentalSpaceType());
    }

    private AdminPropertyOperationsSnapshotResponse.ChargeItem charge(ChargeRow row) {
        return new AdminPropertyOperationsSnapshotResponse.ChargeItem(row.getId(), row.getChargeType(),
                row.getDescription(), row.getAmount(), row.getPayer(), row.getSourceType(), row.getSourceId(),
                row.getFinanceRecordId(), row.getConfirmationStatus(), row.getCreatedAt());
    }

    private void prepareChargeReview(ChargeWriteRow row, Long actorId, Long ownerId, LeaseRow lease,
            LocalDate billingMonth) {
        row.setActorId(actorId);
        row.setOwnerId(ownerId);
        row.setUnitId(lease.getUnitId());
        row.setTenantId(lease.getTenantId());
        row.setTransactionDate(billingMonth.withDayOfMonth(1));
        row.setTransactionNo("TC-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
    }

    private AdminPropertyOperationsSnapshotResponse.WorkOrderItem workOrder(WorkOrderRow row) {
        return new AdminPropertyOperationsSnapshotResponse.WorkOrderItem(row.getId(), row.getWorkOrderNo(),
                row.getCategory(), row.getTitle(), row.getDescription(), row.getRequestedAt(), row.getStatus(),
                row.getEstimatedAmount(), row.getActualAmount(), row.getVendorId(), row.getVendorName());
    }
}
