package com.ccps.backend.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.mapper.PropertyExpensePostingMapper;
import com.ccps.backend.mapper.PropertyExpensePostingMapper.PropertyContext;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PropertyExpensePostingServiceTest {
    @Mock PropertyExpensePostingMapper mapper;

    @Test
    void postsConfiguredServiceOnlyInItsSelectedBillingMonth() {
        PropertyExpensePostingService service = new PropertyExpensePostingService(mapper, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC));
        PropertyContext context = new PropertyContext();
        context.setUnitId(8L); context.setOwnerId(9L);
        when(mapper.lockPosting(10L, "rental-service-service-1", "2026-08")).thenReturn(null);
        when(mapper.findContext(10L)).thenReturn(context);
        when(mapper.insertFinance(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            PropertyExpensePostingMapper.FinanceWrite row = invocation.getArgument(0);
            row.setId(55L);
            return 1;
        });

        service.syncCurrent(10L, Map.of(
                "rentalServiceFees", List.of(Map.of(
                        "id", "service-1", "name", "租客筛选服务", "amount", 180,
                        "billingMode", "months", "billingMonths", List.of(2, 8))),
                "salesServiceFee", 0,
                "generalServiceFee", 0), 7L);

        verify(mapper).insertFinance(org.mockito.ArgumentMatchers.argThat(row ->
                new BigDecimal("180.00").compareTo(row.getAmount()) == 0
                        && LocalDate.parse("2026-08-20").equals(row.getOccurredOn())));
        verify(mapper).insertPosting(10L, "rental-service-service-1", "租客筛选服务", "2026-08", 55L);
    }

    @Test
    void doesNotPostRecurringServiceOutsideItsSelectedMonths() {
        PropertyExpensePostingService service = new PropertyExpensePostingService(mapper, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC));

        service.syncCurrent(10L, Map.of(
                "rentalServiceFees", List.of(Map.of(
                        "id", "service-1", "name", "租客筛选服务", "amount", 180,
                        "billingMode", "months", "billingMonths", List.of(2, 8))),
                "salesServiceFee", 0,
                "generalServiceFee", 0), 7L);

        verifyNoInteractions(mapper);
    }
}
