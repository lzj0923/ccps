package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.dto.OwnerRentIncomeResponse;
import com.ccps.backend.dto.RentReceiptConfirmationResponse;
import com.ccps.backend.dto.OwnerRentIncomeResponse.PropertyOption;
import com.ccps.backend.dto.OwnerRentIncomeResponse.YearlyTrend;
import com.ccps.backend.mapper.OwnerRentIncomeMapper;
import com.ccps.backend.mapper.OwnerRentIncomeMapper.PeriodTotals;

@ExtendWith(MockitoExtension.class)
class OwnerRentIncomeServiceTest {
    @Mock
    private OwnerRentIncomeMapper mapper;

    private OwnerRentIncomeService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneId.of("UTC"));
        service = new OwnerRentIncomeService(mapper, clock);
    }

    @Test
    void returnsOwnerScopedRentSummaryAndFilterOptions() {
        Long userId = 42L;
        Long projectId = 7L;
        when(mapper.findPeriodTotals(userId, projectId,
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-08-01")))
                .thenReturn(totals("10000.00", "8000.00", "2000.00"));
        when(mapper.findPeriodTotals(userId, projectId,
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-01")))
                .thenReturn(totals("8000.00", "4000.00", "4000.00"));
        when(mapper.findPeriodTotals(userId, projectId,
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-08-01")))
                .thenReturn(totals("50000.00", "40000.00", "10000.00"));
        when(mapper.findPeriodTotals(userId, projectId,
                LocalDate.parse("2025-01-01"), LocalDate.parse("2025-08-01")))
                .thenReturn(totals("40000.00", "32000.00", "8000.00"));
        when(mapper.findProperties(userId)).thenReturn(List.of(new PropertyOption(projectId, "Central Suites")));
        when(mapper.findYearlyTrend(userId, projectId)).thenReturn(List.of(
                new YearlyTrend(2026, new BigDecimal("40000.00")),
                new YearlyTrend(2025, new BigDecimal("32000.00"))));
        when(mapper.findRecords(userId, 2026, 7, projectId, "partial")).thenReturn(List.of());
        when(mapper.findRecentReceipts(userId, projectId)).thenReturn(List.of());

        OwnerRentIncomeResponse result = service.getRentIncome(userId, 2026, 7, projectId, "partial");

        assertThat(result.summary().monthlyAmountDue()).isEqualByComparingTo("10000.00");
        assertThat(result.summary().monthlyAmountPaid()).isEqualByComparingTo("8000.00");
        assertThat(result.summary().monthlyUnpaidAmount()).isEqualByComparingTo("2000.00");
        assertThat(result.summary().annualAmountPaid()).isEqualByComparingTo("40000.00");
        assertThat(result.summary().amountDueChangePercent()).isEqualByComparingTo("25.00");
        assertThat(result.summary().amountPaidChangePercent()).isEqualByComparingTo("100.00");
        assertThat(result.summary().unpaidChangePercent()).isEqualByComparingTo("-50.00");
        assertThat(result.summary().annualChangePercent()).isEqualByComparingTo("25.00");
        assertThat(result.availableYears()).containsExactly(2026, 2025);
        assertThat(result.properties()).extracting(PropertyOption::name).containsExactly("Central Suites");
    }

    @Test
    void defaultsToCurrentMonthAndHandlesZeroPreviousValues() {
        when(mapper.findPeriodTotals(42L, null,
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-08-01")))
                .thenReturn(totals("1200.00", "0.00", "1200.00"));
        when(mapper.findPeriodTotals(42L, null,
                LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-01")))
                .thenReturn(totals("0.00", "0.00", "0.00"));
        when(mapper.findPeriodTotals(42L, null,
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-08-01")))
                .thenReturn(totals("1200.00", "0.00", "1200.00"));
        when(mapper.findPeriodTotals(42L, null,
                LocalDate.parse("2025-01-01"), LocalDate.parse("2025-08-01")))
                .thenReturn(totals("0.00", "0.00", "0.00"));
        when(mapper.findProperties(42L)).thenReturn(List.of());
        when(mapper.findYearlyTrend(42L, null)).thenReturn(List.of());
        when(mapper.findRecords(42L, 2026, 7, null, null)).thenReturn(List.of());
        when(mapper.findRecentReceipts(42L, null)).thenReturn(List.of());

        OwnerRentIncomeResponse result = service.getRentIncome(42L, null, null, null, null);

        assertThat(result.summary().year()).isEqualTo(2026);
        assertThat(result.summary().month()).isEqualTo(7);
        assertThat(result.summary().amountDueChangePercent()).isEqualByComparingTo("100.00");
        assertThat(result.summary().amountPaidChangePercent()).isZero();
        assertThat(result.availableYears()).containsExactly(2026);
    }

    @Test
    void confirmsPendingReceiptForOwnerInvoice() {
        when(mapper.confirmPendingReceipt(42L, 88L)).thenReturn(1);

        RentReceiptConfirmationResponse result = service.confirmReceipt(42L, 88L);

        verify(mapper).confirmPendingReceipt(42L, 88L);
        assertThat(result.invoiceId()).isEqualTo(88L);
        assertThat(result.confirmedPaymentCount()).isEqualTo(1);
    }

    @Test
    void refusesInvoiceWithoutOwnerPendingReceipt() {
        when(mapper.confirmPendingReceipt(42L, 99L)).thenReturn(0);

        assertThatThrownBy(() -> service.confirmReceipt(42L, 99L))
                .hasMessageContaining("No pending rent receipt");
    }

    private PeriodTotals totals(String due, String paid, String unpaid) {
        PeriodTotals totals = new PeriodTotals();
        totals.setAmountDue(new BigDecimal(due));
        totals.setAmountPaid(new BigDecimal(paid));
        totals.setUnpaidAmount(new BigDecimal(unpaid));
        return totals;
    }
}
