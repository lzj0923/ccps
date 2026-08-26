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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.mapper.PropertyExpensePostingMapper;
import com.ccps.backend.mapper.PropertyExpensePostingMapper.FinanceWrite;
import com.ccps.backend.mapper.PropertyExpensePostingMapper.MandateFeeRow;
import com.ccps.backend.mapper.PropertyExpensePostingMapper.PropertyContext;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PropertyExpensePostingServiceTest {
    @Mock PropertyExpensePostingMapper mapper;

    @Test
    void postsMandateFeeToCashflowAndPendingFinanceReview() {
        PropertyExpensePostingService service = new PropertyExpensePostingService(mapper, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC));
        PropertyContext context = new PropertyContext();
        context.setUnitId(8L); context.setOwnerId(9L);
        when(mapper.lockPosting(10L, "rental-mandate-31", "2026-08")).thenReturn(null);
        when(mapper.findContext(10L)).thenReturn(context);
        when(mapper.insertFinance(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            FinanceWrite row = invocation.getArgument(0);
            row.setId(56L);
            return 1;
        });

        service.syncMandateFee(10L, 31L, new BigDecimal("188.00"), 7L,
                LocalDate.parse("2026-08-20"));

        verify(mapper).insertFinance(org.mockito.ArgumentMatchers.argThat(row ->
                new BigDecimal("188.00").compareTo(row.getAmount()) == 0
                        && Long.valueOf(7L).equals(row.getActorId())));
        verify(mapper).insertCashflow(56L, 8L, 9L, "service_fee",
                "代管服务费（帳期 2026-08）", LocalDate.parse("2026-08-20"));
        verify(mapper).insertPosting(10L, "rental-mandate-31", "代管服务费", "2026-08", 56L);
    }

    @Test
    void backfillsCurrentFeeForExistingActiveMandate() {
        PropertyExpensePostingService service = new PropertyExpensePostingService(mapper, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC));
        MandateFeeRow mandate = new MandateFeeRow();
        mandate.setMandateId(31L); mandate.setOwnerUnitId(10L);
        mandate.setManagementFee(new BigDecimal("188.00"));
        PropertyContext context = new PropertyContext();
        context.setUnitId(8L); context.setOwnerId(9L);
        when(mapper.findProfiles()).thenReturn(List.of());
        when(mapper.findActiveMandateFees(LocalDate.parse("2026-08-20"), LocalDate.parse("2026-08-01"))).thenReturn(List.of(mandate));
        when(mapper.lockPosting(10L, "rental-mandate-31", "2026-08")).thenReturn(null);
        when(mapper.findContext(10L)).thenReturn(context);
        when(mapper.insertFinance(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            FinanceWrite row = invocation.getArgument(0);
            row.setId(57L);
            return 1;
        });

        service.postDueExpensesOnStartup();

        verify(mapper).insertPosting(10L, "rental-mandate-31", "代管服务费", "2026-08", 57L);
    }

    @Test
    void percentageTakesPriorityOverFixedFeeAndUsesAllRentInvoicesInTheMandate() {
        PropertyExpensePostingService service = new PropertyExpensePostingService(mapper, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC));
        MandateFeeRow mandate = new MandateFeeRow();
        mandate.setMandateId(31L); mandate.setOwnerUnitId(10L);
        mandate.setManagementFee(new BigDecimal("188.00"));
        mandate.setCommissionPercent(new BigDecimal("8.00"));
        mandate.setRentBase(new BigDecimal("2258.06"));
        PropertyContext context = new PropertyContext();
        context.setUnitId(8L); context.setOwnerId(9L);
        when(mapper.findMandateFee(31L, LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-31")))
                .thenReturn(mandate);
        when(mapper.lockPosting(10L, "rental-mandate-31", "2026-08")).thenReturn(null);
        when(mapper.findContext(10L)).thenReturn(context);
        when(mapper.insertFinance(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            FinanceWrite row = invocation.getArgument(0); row.setId(58L); return 1;
        });

        service.syncMandateFee(31L, 7L, LocalDate.parse("2026-08-01"));

        verify(mapper).insertFinance(org.mockito.ArgumentMatchers.argThat(row ->
                new BigDecimal("180.64").compareTo(row.getAmount()) == 0));
        verify(mapper).insertPosting(10L, "rental-mandate-31", "代管服务费", "2026-08", 58L);
    }

    @Test
    void vacantMonthUsesFixedFeeEvenWhenPercentageIsConfigured() {
        PropertyExpensePostingService service = new PropertyExpensePostingService(mapper, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC));
        MandateFeeRow mandate = new MandateFeeRow();
        mandate.setMandateId(31L); mandate.setOwnerUnitId(10L);
        mandate.setManagementFee(new BigDecimal("188.00"));
        mandate.setCommissionPercent(new BigDecimal("8.00"));
        mandate.setRentBase(BigDecimal.ZERO);
        when(mapper.findMandateFee(31L, LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-31")))
                .thenReturn(mandate);

        PropertyContext context = new PropertyContext();
        context.setUnitId(8L); context.setOwnerId(9L);
        when(mapper.lockPosting(10L, "rental-mandate-31", "2026-08")).thenReturn(null);
        when(mapper.findContext(10L)).thenReturn(context);
        when(mapper.insertFinance(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            FinanceWrite row = invocation.getArgument(0); row.setId(59L); return 1;
        });

        service.syncMandateFee(31L, 7L, LocalDate.parse("2026-08-01"));

        verify(mapper).insertFinance(org.mockito.ArgumentMatchers.argThat(row ->
                new BigDecimal("188.00").compareTo(row.getAmount()) == 0));
        verify(mapper).insertPosting(10L, "rental-mandate-31", "代管服务费", "2026-08", 59L);
    }

    @Test
    void doesNotPostOwnerManagementFeeBeforeTheRentalMandateExists() {
        PropertyExpensePostingService service = new PropertyExpensePostingService(mapper, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-08-20T00:00:00Z"), ZoneOffset.UTC));

        service.syncCurrent(10L, Map.of(
                "rentalServiceEnabled", true,
                "managementFeeAmount", 188,
                "managementFeeBillingMode", "months",
                "managementFeeBillingMonths", List.of(8)), 7L);

        verifyNoInteractions(mapper);
    }

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

    @ParameterizedTest
    @ValueSource(ints = {2, 8})
    void defaultAssessmentTaxPostsTwiceAYear(int month) {
        LocalDate occurredOn = LocalDate.of(2026, month, 20);
        String period = occurredOn.toString().substring(0, 7);
        PropertyExpensePostingService service = new PropertyExpensePostingService(mapper, new ObjectMapper(),
                Clock.fixed(occurredOn.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        PropertyContext context = new PropertyContext();
        context.setUnitId(8L); context.setOwnerId(9L);
        when(mapper.lockPosting(10L, "assessment-tax", period)).thenReturn(null);
        when(mapper.findContext(10L)).thenReturn(context);
        when(mapper.insertFinance(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            FinanceWrite row = invocation.getArgument(0); row.setId(60L); return 1;
        });

        service.syncCurrent(10L, Map.of("assessmentTaxFee", 240), 7L);

        verify(mapper).insertCashflow(60L, 8L, 9L, "tax",
                "門牌稅（帳期 " + period + "）", occurredOn);
        verify(mapper).insertPosting(10L, "assessment-tax", "門牌稅", period, 60L);
    }

    @Test
    void defaultAssessmentTaxDoesNotPostOutsideFebruaryAndAugust() {
        PropertyExpensePostingService service = new PropertyExpensePostingService(mapper, new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC));

        service.syncCurrent(10L, Map.of("assessmentTaxFee", 240), 7L);

        verifyNoInteractions(mapper);
    }
}
