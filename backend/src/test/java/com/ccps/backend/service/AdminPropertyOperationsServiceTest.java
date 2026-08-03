package com.ccps.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ccps.backend.dto.AdminPropertyOperationsSnapshotResponse;
import com.ccps.backend.dto.AdminPropertyOperationsChargeRequest;
import com.ccps.backend.dto.AdminPropertyOperationsWorkOrderRequest;
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
    void addsAChargeToTheSelectedMonthInvoiceAndReloadsTheSnapshot() {
        AdminPropertyOperationsMapper mapper = mock(AdminPropertyOperationsMapper.class);
        AdminPropertyOperationsService service = new AdminPropertyOperationsService(mapper);
        LeaseRow lease = new LeaseRow(); lease.setId(88L);
        InvoiceRow invoice = new InvoiceRow(); invoice.setId(91L);
        when(mapper.findActiveLease(7L, 11L)).thenReturn(lease);
        when(mapper.findInvoice(88L, LocalDate.of(2026, 8, 1))).thenReturn(invoice);
        when(mapper.lockInvoice(88L, 91L)).thenReturn(invoice);
        when(mapper.insertCharge(any(ChargeWriteRow.class))).thenReturn(1);
        when(mapper.increaseInvoiceAmount(91L, new BigDecimal("120.00"))).thenReturn(1);
        when(mapper.findCharges(91L)).thenReturn(List.of());
        when(mapper.findWorkOrders(88L)).thenReturn(List.of());

        service.addCharge(5L, 7L, 11L, LocalDate.of(2026, 8, 12),
                new AdminPropertyOperationsChargeRequest("utilities", "电费", new BigDecimal("120.00"), "tenant", "manual", null));

        verify(mapper).insertCharge(any(ChargeWriteRow.class));
        verify(mapper).increaseInvoiceAmount(91L, new BigDecimal("120.00"));
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
