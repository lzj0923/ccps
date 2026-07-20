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

import com.ccps.backend.dto.OwnerReserveResponse;
import com.ccps.backend.dto.OwnerReserveResponse.TransactionItem;
import com.ccps.backend.mapper.OwnerReserveMapper;
import com.ccps.backend.mapper.OwnerReserveMapper.SummaryRow;

@ExtendWith(MockitoExtension.class)
class OwnerReserveServiceTest {
    @Mock private OwnerReserveMapper mapper;
    private OwnerReserveService service;

    @BeforeEach
    void setUp() {
        service = new OwnerReserveService(mapper,
                Clock.fixed(Instant.parse("2026-07-16T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void returnsRealSummaryAndMergesPendingTopupsInTimeOrder() {
        SummaryRow summary = new SummaryRow();
        summary.setTotalBalance(new BigDecimal("12380.50"));
        summary.setMinimumBalance(new BigDecimal("8000.00"));
        summary.setTotalTopups(new BigDecimal("9000.00"));
        summary.setTopupCount(3);
        summary.setTotalDebits(new BigDecimal("480.00"));
        summary.setDebitCount(1);
        summary.setLowBalanceCount(0);
        summary.setAccountCount(4);
        when(mapper.findSummary(42L)).thenReturn(summary);
        LocalDate start = LocalDate.parse("2026-02-01");
        LocalDate end = LocalDate.parse("2026-07-17");
        TransactionItem debit = item(1L, LocalDateTime.parse("2026-07-12T09:00:00"), "debit", "confirmed");
        TransactionItem pending = item(-9L, LocalDateTime.parse("2026-07-15T10:00:00"), "topup", "pending");
        when(mapper.findTransactions(42L, null, null, start, end)).thenReturn(List.of(debit));
        when(mapper.findPendingTopups(42L, null, start, end)).thenReturn(List.of(pending));
        when(mapper.findAccounts(42L)).thenReturn(List.of());
        when(mapper.findProperties(42L)).thenReturn(List.of());
        when(mapper.findNotifications(42L)).thenReturn(List.of());
        when(mapper.findDocuments(42L)).thenReturn(List.of());

        OwnerReserveResponse result = service.getReserve(42L, null, null, start, LocalDate.parse("2026-07-16"));

        assertThat(result.summary().totalBalance()).isEqualByComparingTo("12380.50");
        assertThat(result.summary().accountCount()).isEqualTo(4);
        assertThat(result.transactions()).extracting(TransactionItem::id).containsExactly(-9L, 1L);
    }

    @Test
    void rejectsUnsupportedTypeAndInvalidDates() {
        assertThatThrownBy(() -> service.getReserve(42L, null, "refund", null, null))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.getReserve(42L, null, null,
                LocalDate.parse("2026-08-01"), LocalDate.parse("2026-07-01")))
                .isInstanceOf(ResponseStatusException.class);
    }

    private TransactionItem item(Long id, LocalDateTime time, String type, String status) {
        return new TransactionItem(id, Math.abs(id), 1L, 2L, "Central", "B-01", time,
                type, "test", new BigDecimal("100.00"), new BigDecimal("500.00"), status, status, 1);
    }
}
