package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.dto.AdminMaintenanceCompleteRequest;
import com.ccps.backend.mapper.AdminMaintenanceMapper;
import com.ccps.backend.mapper.AdminMaintenanceMapper.CompletionContext;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewCashflow;
import com.ccps.backend.mapper.AdminMaintenanceMapper.NewFinance;

@ExtendWith(MockitoExtension.class)
class AdminMaintenanceServiceTest {
    @Mock private AdminMaintenanceMapper mapper;
    @Mock private OwnerExpenseMaintenanceService detailService;
    @Mock private AdminPropertyMaintenanceRecordService maintenanceRecordService;

    private AdminMaintenanceService service;

    @BeforeEach
    void setUp() {
        service = new AdminMaintenanceService(mapper, detailService, maintenanceRecordService);
    }

    @Test
    void reserveCompletionReliesOnDatabasePolicyInsteadOfDeductingTwice() {
        CompletionContext context = new CompletionContext();
        context.setId(81L);
        context.setStatus("inspection");
        context.setUnitId(12L);
        context.setOwnerId(3L);
        context.setVendorId(7L);
        context.setReserveAccountId(4L);
        context.setReserveBalance(new BigDecimal("1000.00"));
        context.setReserveDeductedAmount(BigDecimal.ZERO);
        when(mapper.lockContext(81L)).thenReturn(context);
        when(mapper.countPhotos(81L, "before_photo")).thenReturn(1);
        when(mapper.countPhotos(81L, "after_photo")).thenReturn(1);
        doAnswer(invocation -> { invocation.<NewFinance>getArgument(0).setId(91L); return 1; })
                .when(mapper).insertFinance(any(NewFinance.class));
        doAnswer(invocation -> { invocation.<NewCashflow>getArgument(0).setId(101L); return 1; })
                .when(mapper).insertCashflow(any(NewCashflow.class));
        when(mapper.linkReserveDebitToWorkOrder(91L, 81L)).thenReturn(1);
        when(mapper.findReserveDebitAmount(91L, 81L)).thenReturn(new BigDecimal("100.00"));
        when(mapper.completeWorkOrder(81L, 101L, new BigDecimal("100.00"))).thenReturn(1);

        service.complete(1L, 81L,
                new AdminMaintenanceCompleteRequest(new BigDecimal("100.00"), "reserve", "完成維修"));

        verify(mapper).linkReserveDebitToWorkOrder(91L, 81L);
        verify(mapper).completeWorkOrder(eq(81L), eq(101L), eq(new BigDecimal("100.00")));
    }

    @Test
    void directPaymentCannotBeConvertedIntoAReserveDeduction() {
        CompletionContext context = new CompletionContext();
        context.setId(82L);
        context.setStatus("inspection");
        context.setUnitId(12L);
        context.setOwnerId(3L);
        context.setVendorId(7L);
        context.setReserveAccountId(4L);
        context.setReserveBalance(new BigDecimal("1000.00"));
        context.setReserveDeductedAmount(BigDecimal.ZERO);
        when(mapper.lockContext(82L)).thenReturn(context);
        when(mapper.countPhotos(82L, "before_photo")).thenReturn(1);
        when(mapper.countPhotos(82L, "after_photo")).thenReturn(1);
        doAnswer(invocation -> { invocation.<NewFinance>getArgument(0).setId(92L); return 1; })
                .when(mapper).insertFinance(any(NewFinance.class));
        doAnswer(invocation -> { invocation.<NewCashflow>getArgument(0).setId(102L); return 1; })
                .when(mapper).insertCashflow(any(NewCashflow.class));
        when(mapper.completeWorkOrder(82L, 102L, new BigDecimal("100.00"))).thenReturn(1);

        service.complete(1L, 82L,
                new AdminMaintenanceCompleteRequest(new BigDecimal("100.00"), "direct_payment", "現金支付"));

        ArgumentCaptor<NewFinance> financeCaptor = ArgumentCaptor.forClass(NewFinance.class);
        ArgumentCaptor<NewCashflow> cashflowCaptor = ArgumentCaptor.forClass(NewCashflow.class);
        verify(mapper).insertFinance(financeCaptor.capture());
        verify(mapper).insertCashflow(cashflowCaptor.capture());
        assertThat(financeCaptor.getValue().getConfirmationStatus()).isEqualTo("not_required");
        assertThat(cashflowCaptor.getValue().getReserveAccountId()).isNull();
        verify(mapper, never()).linkReserveDebitToWorkOrder(anyLong(), anyLong());
        verify(mapper, never()).findReserveDebitAmount(anyLong(), anyLong());
    }
}
