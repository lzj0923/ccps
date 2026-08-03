package com.ccps.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminPropertyOperationsChargeRequest;
import com.ccps.backend.dto.AdminPropertyOperationsWorkOrderRequest;
import com.ccps.backend.dto.AdminPropertyOperationsSnapshotResponse;
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
        LeaseRow lease = mapper.findActiveLease(ownerId, ownerUnitId);
        if (lease == null) {
            return new AdminPropertyOperationsSnapshotResponse(null, null, List.of(), List.of());
        }
        LocalDate month = billingMonth.withDayOfMonth(1);
        InvoiceRow invoice = mapper.findInvoice(lease.getId(), month);
        List<ChargeRow> charges = invoice == null ? List.of() : mapper.findCharges(invoice.getId());
        List<WorkOrderRow> workOrders = mapper.findWorkOrders(lease.getId());
        return new AdminPropertyOperationsSnapshotResponse(
                new AdminPropertyOperationsSnapshotResponse.LeaseSummary(lease.getId(), lease.getLeaseNo(),
                        lease.getTenantId(), lease.getTenantName(), lease.getStartDate(), lease.getEndDate(), lease.getMonthlyRent()),
                invoice == null ? null : new AdminPropertyOperationsSnapshotResponse.BillingSummary(invoice.getId(),
                        invoice.getBillingMonth(), invoice.getDueDate(), invoice.getAmountDue(), invoice.getAmountPaid(),
                        invoice.getAmountUnpaid(), invoice.getStatus()),
                charges.stream().map(this::charge).toList(),
                workOrders.stream().map(this::workOrder).toList());
    }

    @Transactional
    public AdminPropertyOperationsSnapshotResponse addCharge(Long actorId, Long ownerId, Long ownerUnitId,
            LocalDate billingMonth, AdminPropertyOperationsChargeRequest request) {
        LeaseRow lease = mapper.findActiveLease(ownerId, ownerUnitId);
        if (lease == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active lease not found");
        InvoiceRow invoice = mapper.findInvoice(lease.getId(), billingMonth.withDayOfMonth(1));
        if (invoice == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Create the monthly invoice before adding charges");
        if (mapper.lockInvoice(lease.getId(), invoice.getId()) == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice changed; reload and try again");
        }
        ChargeWriteRow row = new ChargeWriteRow();
        row.setInvoiceId(invoice.getId()); row.setChargeType(request.chargeType()); row.setDescription(request.description().trim());
        row.setAmount(request.amount()); row.setPayer(request.payer()); row.setSourceType(request.sourceType());
        row.setSourceId(request.sourceId()); row.setActorId(actorId);
        if (mapper.insertCharge(row) != 1 || mapper.increaseInvoiceAmount(invoice.getId(), request.amount()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to add charge");
        }
        return snapshot(ownerId, ownerUnitId, billingMonth);
    }

    @Transactional
    public AdminPropertyOperationsSnapshotResponse createWorkOrder(Long actorId, Long ownerId, Long ownerUnitId,
            AdminPropertyOperationsWorkOrderRequest request, LocalDate billingMonth) {
        LeaseRow lease = mapper.findActiveLease(ownerId, ownerUnitId);
        if (lease == null || !lease.getId().equals(request.leaseId())) {
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
        return snapshot(ownerId, ownerUnitId, billingMonth);
    }

    private AdminPropertyOperationsSnapshotResponse.ChargeItem charge(ChargeRow row) {
        return new AdminPropertyOperationsSnapshotResponse.ChargeItem(row.getId(), row.getChargeType(),
                row.getDescription(), row.getAmount(), row.getPayer(), row.getSourceType(), row.getSourceId(), row.getCreatedAt());
    }

    private AdminPropertyOperationsSnapshotResponse.WorkOrderItem workOrder(WorkOrderRow row) {
        return new AdminPropertyOperationsSnapshotResponse.WorkOrderItem(row.getId(), row.getWorkOrderNo(),
                row.getCategory(), row.getTitle(), row.getDescription(), row.getRequestedAt(), row.getStatus(),
                row.getEstimatedAmount(), row.getActualAmount(), row.getVendorId(), row.getVendorName());
    }
}
