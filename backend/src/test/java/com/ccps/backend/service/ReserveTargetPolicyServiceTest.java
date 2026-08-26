package com.ccps.backend.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.mapper.ReserveTargetPolicyMapper;
import com.ccps.backend.mapper.ReserveTargetPolicyMapper.TargetContext;

@ExtendWith(MockitoExtension.class)
class ReserveTargetPolicyServiceTest {
    @Mock private ReserveTargetPolicyMapper mapper;
    private ReserveTargetPolicyService service;

    @BeforeEach
    void setUp() {
        service = new ReserveTargetPolicyService(mapper);
    }

    @Test
    void calculatesOnlyTwoMonthBuildingManagementFee() {
        TargetContext row = context("manual", "2500.00", "0.00", "0.00", 3,
                "1800.00", "1200.00", 4);
        row.setBuildingManagementFee(new BigDecimal("2500.00"));
        when(mapper.lockTarget(10L)).thenReturn(row);
        when(mapper.updateCalculation(10L, new BigDecimal("5000.00"), new BigDecimal("1800.00"),
                new BigDecimal("300.00"), 3)).thenReturn(1);

        service.recalculate(10L);

        verify(mapper).insertCalculationAudit(10L, new BigDecimal("2500.00"), "manual",
                new BigDecimal("5000.00"), new BigDecimal("1800.00"), new BigDecimal("300.00"), 3);
    }

    @Test
    void doesNotApplyTwoThousandRinggitFloor() {
        TargetContext row = context("auto", "0.00", "0.00", "0.00", 3,
                "1800.00", "1200.00", 4);
        row.setBuildingManagementFee(new BigDecimal("900.00"));
        when(mapper.lockTarget(11L)).thenReturn(row);
        when(mapper.updateCalculation(11L, new BigDecimal("1800.00"), new BigDecimal("1800.00"),
                new BigDecimal("300.00"), 3)).thenReturn(1);

        service.recalculate(11L);

        verify(mapper).insertCalculationAudit(11L, new BigDecimal("0.00"), "auto",
                new BigDecimal("1800.00"), new BigDecimal("1800.00"), new BigDecimal("300.00"), 3);
    }

    @Test
    void appliesTwoMonthBuildingManagementFeeFloorPerUnit() throws Exception {
        TargetContext row = context("auto", "0.00", "0.00", "0.00", 3,
                "1800.00", "1200.00", 4);
        Method setter = TargetContext.class.getMethod("setBuildingManagementFee", BigDecimal.class);
        setter.invoke(row, new BigDecimal("2500.00"));
        when(mapper.lockTarget(13L)).thenReturn(row);
        when(mapper.updateCalculation(13L, new BigDecimal("5000.00"), new BigDecimal("1800.00"),
                new BigDecimal("300.00"), 3)).thenReturn(1);

        service.recalculate(13L);

        verify(mapper).insertCalculationAudit(13L, new BigDecimal("0.00"), "auto",
                new BigDecimal("5000.00"), new BigDecimal("1800.00"), new BigDecimal("300.00"), 3);
    }

    @Test
    void unchangedInputsDoNotCreateRepeatedAuditRows() {
        TargetContext row = context("auto", "2700.00", "2700.00", "1800.00", 3,
                "1800.00", "1200.00", 4);
        row.setBuildingManagementFee(new BigDecimal("1350.00"));
        row.setMonthlyExpenseAverage(new BigDecimal("300.00"));
        when(mapper.lockTarget(12L)).thenReturn(row);
        when(mapper.updateCalculation(12L, new BigDecimal("2700.00"), new BigDecimal("1800.00"),
                new BigDecimal("300.00"), 3)).thenReturn(1);

        service.recalculate(12L);

        verify(mapper, never()).insertCalculationAudit(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt());
    }

    private TargetContext context(String mode, String minimum, String calculated, String rentBuffer,
            int expenseMonths, String monthlyRent, String expenseTotal, int trackedMonths) {
        TargetContext row = new TargetContext();
        row.setAccountId(1L);
        row.setMinimumBalanceMode(mode);
        row.setMinimumBalance(new BigDecimal(minimum));
        row.setCalculatedMinimumBalance(new BigDecimal(calculated));
        row.setRentBufferAmount(new BigDecimal(rentBuffer));
        row.setMonthlyExpenseAverage(BigDecimal.ZERO);
        row.setExpenseBufferMonths(expenseMonths);
        row.setMonthlyRent(new BigDecimal(monthlyRent));
        row.setExpenseTotal(new BigDecimal(expenseTotal));
        row.setTrackedMonths(trackedMonths);
        return row;
    }
}
