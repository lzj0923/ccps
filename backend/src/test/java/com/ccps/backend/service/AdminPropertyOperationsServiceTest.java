package com.ccps.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ccps.backend.dto.AdminPropertyOperationsSnapshotResponse;
import com.ccps.backend.dto.AdminPropertyOperationsChargeRequest;
import com.ccps.backend.dto.AdminPropertyOperationsWorkOrderRequest;
import com.ccps.backend.dto.AdminPropertySharedChargeRequest;
import com.ccps.backend.mapper.AdminPropertyOperationsMapper;
import com.ccps.backend.mapper.AdminPropertyOperationsMapper.ChargeRow;
import com.ccps.backend.mapper.AdminPropertyOperationsMapper.InvoiceRow;
import com.ccps.backend.mapper.AdminPropertyOperationsMapper.LeaseRow;
import com.ccps.backend.mapper.AdminPropertyOperationsMapper.WorkOrderRow;
import com.ccps.backend.mapper.AdminPropertyOperationsMapper.ChargeWriteRow;
import com.ccps.backend.mapper.AdminPropertyOperationsMapper.WorkOrderWriteRow;

class AdminPropertyOperationsServiceTest {
    @Test
    void returnsOnlyTheCurrentLeasesMonthlyBillingChargesAndWorkOrders() {
        AdminPropertyOperationsMapper mapper = mock(AdminPropertyOperationsMapper.class);
        AdminPropertyOperationsService service = new AdminPropertyOperationsService(mapper);
        LeaseRow lease = new LeaseRow();
        lease.setId(88L); lease.setLeaseNo("LEASE-88"); lease.setTenantId(31L); lease.setTenantName("陈伟明");
        lease.setStartDate(LocalDate.of(2026, 7, 30)); lease.setEndDate(LocalDate.of(2027, 7, 31));
        lease.setMonthlyRent(new BigDecimal("2800.00"));
        InvoiceRow invoice = new InvoiceRow();
        invoice.setId(91L); invoice.setBillingMonth(LocalDate.of(2026, 8, 1)); invoice.setDueDate(LocalDate.of(2026, 8, 5));
        invoice.setAmountDue(new BigDecimal("3000.00")); invoice.setAmountPaid(new BigDecimal("1000.00"));
        invoice.setAmountUnpaid(new BigDecimal("2000.00")); invoice.setStatus("partial");
        ChargeRow charge = new ChargeRow(); charge.setId(1L); charge.setChargeType("utilities");
        charge.setDescription("水电费"); charge.setAmount(new BigDecimal("200.00")); charge.setPayer("tenant");
        WorkOrderRow workOrder = new WorkOrderRow(); workOrder.setId(7L); workOrder.setWorkOrderNo("MWO-7");
        workOrder.setCategory("plumbing"); workOrder.setTitle("厨房水管维修"); workOrder.setStatus("submitted");
        when(mapper.findActiveLease(7L, 11L)).thenReturn(lease);
        when(mapper.findInvoice(88L, LocalDate.of(2026, 8, 1))).thenReturn(invoice);
        when(mapper.findCharges(91L)).thenReturn(List.of(charge));
        when(mapper.findWorkOrders(88L)).thenReturn(List.of(workOrder));

        AdminPropertyOperationsSnapshotResponse result = service.snapshot(7L, 11L, LocalDate.of(2026, 8, 20));

        assertEquals(88L, result.lease().id());
        assertEquals(new BigDecimal("2000.00"), result.billing().amountUnpaid());
        assertEquals("utilities", result.charges().get(0).chargeType());
        assertEquals(7L, result.workOrders().get(0).id());
    }

    @Test
    void returnsEmptyOperationsWhenThereIsNoActiveLeaseForTheProperty() {
        AdminPropertyOperationsMapper mapper = mock(AdminPropertyOperationsMapper.class);
        when(mapper.findActiveLease(7L, 11L)).thenReturn(null);

        AdminPropertyOperationsSnapshotResponse result = new AdminPropertyOperationsService(mapper)
                .snapshot(7L, 11L, LocalDate.of(2026, 8, 20));

        assertEquals(null, result.lease());
        assertEquals(List.of(), result.charges());
        assertEquals(List.of(), result.workOrders());
    }

    @Test
    void selectsTheRequestedRoomLeaseInsteadOfSilentlyUsingTheFirstLease() {
        AdminPropertyOperationsMapper mapper = mock(AdminPropertyOperationsMapper.class);
        LeaseRow roomA = new LeaseRow(); roomA.setId(88L); roomA.setRentalSpaceId(101L);
        roomA.setRentalSpaceName("主卧 A"); roomA.setRentalSpaceType("room"); roomA.setTenantName("租客甲");
        LeaseRow roomB = new LeaseRow(); roomB.setId(89L); roomB.setRentalSpaceId(102L);
        roomB.setRentalSpaceName("次卧 B"); roomB.setRentalSpaceType("room"); roomB.setTenantName("租客乙");
        when(mapper.findActiveLeases(7L, 11L)).thenReturn(List.of(roomA, roomB));
        when(mapper.findWorkOrders(89L)).thenReturn(List.of());

        AdminPropertyOperationsSnapshotResponse result = new AdminPropertyOperationsService(mapper)
                .snapshot(7L, 11L, LocalDate.of(2026, 8, 20), 89L);

        assertEquals(89L, result.lease().id());
        assertEquals("次卧 B", result.lease().rentalSpaceName());
        assertEquals(2, result.activeLeases().size());
    }

    @Test
    void allocatesSharedChargeAcrossOccupiedRoomsWithoutLosingRoundingCents() {
        AdminPropertyOperationsMapper mapper = mock(AdminPropertyOperationsMapper.class);
        LeaseRow roomA = new LeaseRow(); roomA.setId(88L); roomA.setUnitId(12L); roomA.setTenantId(31L); roomA.setRentalSpaceType("room");
        roomA.setStartDate(LocalDate.of(2026, 1, 1)); roomA.setEndDate(LocalDate.of(2026, 12, 31));
        LeaseRow roomB = new LeaseRow(); roomB.setId(89L); roomB.setUnitId(12L); roomB.setTenantId(32L); roomB.setRentalSpaceType("room");
        roomB.setStartDate(LocalDate.of(2026, 1, 1)); roomB.setEndDate(LocalDate.of(2026, 12, 31));
        InvoiceRow invoiceA = new InvoiceRow(); invoiceA.setId(201L);
        InvoiceRow invoiceB = new InvoiceRow(); invoiceB.setId(202L);
        when(mapper.findActiveLeases(7L, 11L)).thenReturn(List.of(roomA, roomB));
        when(mapper.findInvoice(88L, LocalDate.of(2026, 8, 1))).thenReturn(invoiceA);
        when(mapper.findInvoice(89L, LocalDate.of(2026, 8, 1))).thenReturn(invoiceB);
        when(mapper.lockInvoice(88L, 201L)).thenReturn(invoiceA);
        when(mapper.lockInvoice(89L, 202L)).thenReturn(invoiceB);
        when(mapper.insertChargeFinanceReview(any(ChargeWriteRow.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, ChargeWriteRow.class).setFinanceRecordId(501L);
            return 1;
        });
        when(mapper.insertCharge(any(ChargeWriteRow.class))).thenReturn(1);
        when(mapper.findWorkOrders(88L)).thenReturn(List.of());

        new AdminPropertyOperationsService(mapper).addSharedCharge(5L, 7L, 11L,
                LocalDate.of(2026, 8, 1), 88L,
                new AdminPropertySharedChargeRequest("utilities", "公共电费", new BigDecimal("100.01"), "tenant"));

        verify(mapper, never()).increaseInvoiceAmount(201L, new BigDecimal("50.01"));
        verify(mapper, never()).increaseInvoiceAmount(202L, new BigDecimal("50.00"));
    }

    @Test
    void submitsAChargeForFinanceReviewWithoutIncreasingTheInvoice() {
        AdminPropertyOperationsMapper mapper = mock(AdminPropertyOperationsMapper.class);
        AdminPropertyOperationsService service = new AdminPropertyOperationsService(mapper);
        LeaseRow lease = new LeaseRow(); lease.setId(88L); lease.setUnitId(12L); lease.setTenantId(31L);
        InvoiceRow invoice = new InvoiceRow(); invoice.setId(91L);
        when(mapper.findActiveLease(7L, 11L)).thenReturn(lease);
        when(mapper.findInvoice(88L, LocalDate.of(2026, 8, 1))).thenReturn(invoice);
        when(mapper.lockInvoice(88L, 91L)).thenReturn(invoice);
        when(mapper.insertChargeFinanceReview(any(ChargeWriteRow.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, ChargeWriteRow.class).setFinanceRecordId(501L);
            return 1;
        });
        when(mapper.insertCharge(any(ChargeWriteRow.class))).thenReturn(1);
        when(mapper.findCharges(91L)).thenReturn(List.of());
        when(mapper.findWorkOrders(88L)).thenReturn(List.of());

        service.addCharge(5L, 7L, 11L, LocalDate.of(2026, 8, 12),
                new AdminPropertyOperationsChargeRequest("utilities", "电费", new BigDecimal("120.00"), "tenant", "manual", null));

        verify(mapper).insertCharge(any(ChargeWriteRow.class));
        verify(mapper).insertChargeFinanceReview(any(ChargeWriteRow.class));
        verify(mapper, never()).increaseInvoiceAmount(91L, new BigDecimal("120.00"));
    }

    @Test
    void createsAWorkOrderOnlyForTheCurrentLease() {
        AdminPropertyOperationsMapper mapper = mock(AdminPropertyOperationsMapper.class);
        AdminPropertyOperationsService service = new AdminPropertyOperationsService(mapper);
        LeaseRow lease = new LeaseRow(); lease.setId(88L); lease.setUnitId(12L);
        when(mapper.findActiveLease(7L, 11L)).thenReturn(lease);
        when(mapper.insertWorkOrder(any(WorkOrderWriteRow.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, WorkOrderWriteRow.class).setId(701L);
            return 1;
        });
        when(mapper.findWorkOrders(88L)).thenReturn(List.of());

        service.createWorkOrder(5L, 7L, 11L,
                new AdminPropertyOperationsWorkOrderRequest(88L, null, "plumbing", "厨房漏水",
                        "水管接头渗水", java.time.LocalDateTime.of(2026, 8, 2, 10, 0), new BigDecimal("300.00")),
                LocalDate.of(2026, 8, 1));

        verify(mapper).insertWorkOrder(any(WorkOrderWriteRow.class));
        verify(mapper).insertWorkOrderHistory(701L, java.time.LocalDateTime.of(2026, 8, 2, 10, 0), 5L);
        verify(mapper).insertWorkOrderAudit(5L, 701L, "{\"leaseId\":88}");
    }
}
