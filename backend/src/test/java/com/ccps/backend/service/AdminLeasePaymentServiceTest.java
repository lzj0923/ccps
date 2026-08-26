package com.ccps.backend.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.dto.AdminLeasePaymentUpdateRequest;
import com.ccps.backend.mapper.AdminLeasePaymentMapper;
import com.ccps.backend.mapper.AdminLeasePaymentMapper.PaymentRow;

@ExtendWith(MockitoExtension.class)
class AdminLeasePaymentServiceTest {
    @Mock private AdminLeasePaymentMapper mapper;
    private AdminLeasePaymentService service;
    private PaymentRow payment;

    @BeforeEach
    void setUp(){
        service=new AdminLeasePaymentService(mapper,Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"),ZoneOffset.UTC));
        payment=new PaymentRow();
        payment.setPaymentId(7L);payment.setFinanceRecordId(17L);payment.setInvoiceId(27L);payment.setLeaseId(37L);
        payment.setLeaseNo("LS-2026-001");payment.setAmount(new BigDecimal("100.00"));payment.setPaymentStatus("paid");
        payment.setPayerName("租客");payment.setPaymentDate(LocalDate.of(2026,7,20));payment.setPaymentMethod("cash");
    }

    @Test
    void updatesPaymentAndAdjustsInvoiceByAmountDifference(){
        var request=new AdminLeasePaymentUpdateRequest(new BigDecimal("150.00"),LocalDate.of(2026,7,21),"bank_transfer","租客","BANK-01","補收租金");
        when(mapper.lock(37L,7L)).thenReturn(payment);
        when(mapper.adjustInvoice(27L,new BigDecimal("50.00"))).thenReturn(1);
        when(mapper.updateAllocatedAmount(7L,new BigDecimal("150.00"))).thenReturn(1);
        when(mapper.updateFinance(17L,new BigDecimal("150.00"),LocalDate.of(2026,7,21),LocalDate.of(2026,7,21),"bank_transfer",1L)).thenReturn(1);
        when(mapper.list(37L)).thenReturn(List.of(payment));

        service.update(1L,37L,7L,request);

        verify(mapper).adjustInvoice(27L,new BigDecimal("50.00"));
        verify(mapper).updateReceipt(17L,"租客","BANK-01","補收租金");
        verify(mapper).updateCashflow(17L,LocalDate.of(2026,7,21),"租金收款 · LS-2026-001");
        verify(mapper).audit(1L,"update_rent_payment",17L,"{\"amount\":100.00}","{\"amount\":150.00}");
    }

    @Test
    void allowsBankTransferWithoutOptionalPaymentReference(){
        var request=new AdminLeasePaymentUpdateRequest(new BigDecimal("100.00"),LocalDate.of(2026,7,21),"bank_transfer","租客",null,"已核对到账");
        when(mapper.lock(37L,7L)).thenReturn(payment);
        when(mapper.updateFinance(17L,new BigDecimal("100.00"),LocalDate.of(2026,7,21),LocalDate.of(2026,7,21),"bank_transfer",1L)).thenReturn(1);
        when(mapper.list(37L)).thenReturn(List.of(payment));

        service.update(1L,37L,7L,request);

        verify(mapper).updateReceipt(17L,"租客",null,"已核对到账");
    }

    @Test
    void voidsPaymentAndReversesItsInvoiceAmount(){
        when(mapper.lock(37L,7L)).thenReturn(payment);
        when(mapper.adjustInvoice(27L,new BigDecimal("-100.00"))).thenReturn(1);
        when(mapper.voidFinance(17L,1L)).thenReturn(1);

        service.delete(1L,37L,7L);

        verify(mapper).adjustInvoice(27L,new BigDecimal("-100.00"));
        verify(mapper).voidFinance(17L,1L);
        verify(mapper).audit(1L,"void_rent_payment",17L,"{\"amount\":100.00}","{\"status\":\"voided\"}");
    }
}
