package com.ccps.backend.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.mapper.AdminPropertyCashflowMapper;
import com.ccps.backend.mapper.AdminPropertyCashflowMapper.CashflowRow;
import com.ccps.backend.mapper.AdminPropertyCashflowMapper.PropertyContext;
import com.ccps.backend.mapper.AdminPropertyCashflowMapper.ReserveDebit;

@ExtendWith(MockitoExtension.class)
class AdminPropertyCashflowServiceTest {
    @Mock private AdminPropertyCashflowMapper mapper;
    @TempDir java.nio.file.Path tempDir;
    private AdminPropertyCashflowService service;

    @BeforeEach
    void setUp(){service=new AdminPropertyCashflowService(mapper,tempDir.toString());}

    @Test
    void voidsConfirmedManualExpenseAndReversesReserveInsteadOfPhysicallyDeleting(){
        PropertyContext context=new PropertyContext();context.setOwnerId(1L);context.setOwnerUnitId(10L);context.setUnitId(12L);
        CashflowRow row=new CashflowRow();row.setId(27L);row.setFinanceRecordId(45L);row.setDirection("expense");row.setPaymentStatus("paid");row.setConfirmationStatus("confirmed");row.setEditable(true);
        ReserveDebit debit=new ReserveDebit();debit.setId(32L);debit.setReserveAccountId(5L);debit.setAmount(new BigDecimal("1.00"));
        when(mapper.findProperty(1L,10L)).thenReturn(context);
        when(mapper.find(12L,27L)).thenReturn(row);
        when(mapper.findReserveDebit(45L)).thenReturn(debit);
        when(mapper.restoreReserveBalance(5L,new BigDecimal("1.00"))).thenReturn(1);
        when(mapper.findReserveBalance(5L)).thenReturn(new BigDecimal("5000.00"));
        when(mapper.insertReserveReversal(5L,45L,new BigDecimal("1.00"),new BigDecimal("5000.00"),1L)).thenReturn(1);
        when(mapper.voidFinance(45L)).thenReturn(1);
        service.delete(1L,1L,10L,27L);

        verify(mapper).restoreReserveBalance(5L,new BigDecimal("1.00"));
        verify(mapper).insertReserveReversal(5L,45L,new BigDecimal("1.00"),new BigDecimal("5000.00"),1L);
        verify(mapper).voidFinance(45L);
        verify(mapper,never()).deleteCashflow(27L);
        verify(mapper,never()).deleteFinance(45L);
    }
}
