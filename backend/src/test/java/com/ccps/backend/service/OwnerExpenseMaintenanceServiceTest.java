package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.MaintenanceDetailResponse;
import com.ccps.backend.dto.MaintenanceDetailResponse.Attachment;
import com.ccps.backend.dto.MaintenanceDetailResponse.StatusEvent;
import com.ccps.backend.dto.OwnerExpenseMaintenanceResponse;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper.ExpenseTotals;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper.MaintenanceHeader;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper.ReserveTotals;

@ExtendWith(MockitoExtension.class)
class OwnerExpenseMaintenanceServiceTest {
    @Mock
    private OwnerExpenseMaintenanceMapper mapper;

    private OwnerExpenseMaintenanceService service;

    @BeforeEach
    void setUp() {
        service = new OwnerExpenseMaintenanceService(mapper,
                Clock.fixed(Instant.parse("2026-07-16T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void returnsCurrentMonthSummaryAndRequestedRangeRecords() {
        when(mapper.findTotals(42L, 7L, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-08-01")))
                .thenReturn(totals("2546.40", "830.00"));
        when(mapper.findTotals(42L, 7L, LocalDate.parse("2026-06-01"), LocalDate.parse("2026-07-01")))
                .thenReturn(totals("2000.00", "400.00"));
        when(mapper.findReserveTotals(42L, 7L, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-08-01")))
                .thenReturn(reserve("480.00", 1));
        when(mapper.countPendingMaintenance(42L, 7L)).thenReturn(2);
        when(mapper.findProperties(42L)).thenReturn(List.of());
        when(mapper.findCategories(42L)).thenReturn(List.of("maintenance"));
        when(mapper.findExpenses(42L, 7L, "maintenance", LocalDate.parse("2026-07-05"), LocalDate.parse("2026-07-16")))
                .thenReturn(List.of());
        when(mapper.findMaintenance(42L, 7L, "maintenance", "in_progress",
                LocalDate.parse("2026-07-05"), LocalDate.parse("2026-07-16"))).thenReturn(List.of());

        OwnerExpenseMaintenanceResponse result = service.getOverview(42L, 7L, "maintenance", "in_progress",
                LocalDate.parse("2026-07-05"), LocalDate.parse("2026-07-15"));

        assertThat(result.summary().monthlyExpense()).isEqualByComparingTo("2546.40");
        assertThat(result.summary().expenseChangePercent()).isEqualByComparingTo("27.32");
        assertThat(result.summary().maintenanceChangePercent()).isEqualByComparingTo("107.50");
        assertThat(result.summary().reserveDeductedAmount()).isEqualByComparingTo("480.00");
        assertThat(result.summary().reserveDebitCount()).isEqualTo(1);
        assertThat(result.summary().pendingMaintenanceCount()).isEqualTo(2);
    }

    @Test
    void mapsMaintenanceDetailAndRejectsUnknownOrder() {
        MaintenanceHeader header = new MaintenanceHeader();
        header.setId(9L);
        header.setWorkOrderNo("MNT-9");
        header.setProjectName("Central Suites");
        header.setUnitNo("B-0602");
        header.setTitle("Pipe repair");
        header.setStatus("in_progress");
        header.setRequestedAt(LocalDateTime.parse("2026-07-10T10:30:00"));
        header.setActualAmount(new BigDecimal("480.00"));
        header.setReserveDeductedAmount(new BigDecimal("480.00"));
        StatusEvent event = new StatusEvent(1L, "submitted", LocalDateTime.parse("2026-07-10T10:30:00"), "Created");
        Attachment attachment = new Attachment(2L, "before.png", "image/png", 10L,
                "before_photo", LocalDateTime.parse("2026-07-10T10:31:00"));
        when(mapper.findMaintenanceHeader(42L, 9L)).thenReturn(header);
        when(mapper.findStatusHistory(9L)).thenReturn(List.of(event));
        when(mapper.findAttachments(9L)).thenReturn(List.of(attachment));

        MaintenanceDetailResponse result = service.getMaintenanceDetail(42L, 9L);

        assertThat(result.workOrderNo()).isEqualTo("MNT-9");
        assertThat(result.actualAmount()).isEqualByComparingTo("480.00");
        assertThat(result.history()).containsExactly(event);
        assertThat(result.attachments()).containsExactly(attachment);

        assertThatThrownBy(() -> service.getMaintenanceDetail(42L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void rejectsInvalidDateRangeAndStatus() {
        assertThatThrownBy(() -> service.getOverview(42L, null, null, "open",
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-31")))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.getOverview(42L, null, null, null,
                LocalDate.parse("2026-08-01"), LocalDate.parse("2026-07-31")))
                .isInstanceOf(ResponseStatusException.class);
    }

    private ExpenseTotals totals(String expense, String maintenance) {
        ExpenseTotals value = new ExpenseTotals();
        value.setExpenseAmount(new BigDecimal(expense));
        value.setMaintenanceAmount(new BigDecimal(maintenance));
        return value;
    }

    private ReserveTotals reserve(String amount, int count) {
        ReserveTotals value = new ReserveTotals();
        value.setAmount(new BigDecimal(amount));
        value.setEntryCount(count);
        return value;
    }
}
