package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ccps.backend.dto.OwnerPropertyCashflowResponse;
import com.ccps.backend.mapper.OwnerPropertyCashflowMapper;
import com.ccps.backend.mapper.OwnerPropertyCashflowMapper.CashflowRow;
import com.ccps.backend.mapper.OwnerPropertyCashflowMapper.PropertyContext;

class OwnerPropertyCashflowServiceTest {

    private final OwnerPropertyCashflowMapper mapper = mock(OwnerPropertyCashflowMapper.class);
    private final OwnerPropertyCashflowService service = new OwnerPropertyCashflowService(mapper);

    @Test
    void listsIncomeExpensesAndReserveForOneOwnerUnit() {
        PropertyContext property = new PropertyContext();
        property.setOwnerUnitId(26L);
        property.setUnitId(18L);
        property.setOwnerId(9L);
        property.setProjectName("团结小区");
        property.setUnitNo("102");
        when(mapper.findProperty(42L, 26L)).thenReturn(property);

        CashflowRow rent = row(61L, "rent", "income", "rent", "租金收款",
                "8064.52", LocalDate.of(2026, 8, 10), LocalDateTime.of(2026, 8, 10, 10, 0));
        CashflowRow expense = row(57L, "cashflow", "expense", "service_fee", "代管服务费",
                "10.00", LocalDate.of(2026, 8, 10), LocalDateTime.of(2026, 8, 10, 10, 8));
        rent.setDocumentIds("21,22,21");
        expense.setDocumentIds("32");
        CashflowRow reserve = row(1L, "reserve", "expense", "reserve", "支出由預備金自動扣除",
                "1000.00", LocalDate.of(2026, 8, 15), LocalDateTime.of(2026, 8, 15, 9, 0));
        when(mapper.findCashflows(18L, 9L)).thenReturn(List.of(rent, expense));
        when(mapper.findReserveTransactions(26L)).thenReturn(List.of(reserve));

        OwnerPropertyCashflowResponse response = service.list(42L, 26L);

        assertThat(response.ownerUnitId()).isEqualTo(26L);
        assertThat(response.records().get(0).documentIds()).isEmpty();
        assertThat(response.records().get(1).documentIds()).containsExactly(32L);
        assertThat(response.records().get(2).documentIds()).containsExactly(21L, 22L);
        assertThat(response.records()).extracting(item -> item.source())
                .containsExactly("reserve", "cashflow", "rent");
        assertThat(response.records()).extracting(item -> item.direction())
                .contains("income", "expense");
        assertThat(response.records()).extracting(item -> item.balanceAfter())
                .containsExactly(new BigDecimal("7054.52"), new BigDecimal("8054.52"), new BigDecimal("8064.52"));
    }

    @Test
    void cannotReadCashflowsOrAttachmentsForAnotherOwner() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.list(43L, 26L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        org.mockito.Mockito.verify(mapper, org.mockito.Mockito.never()).findCashflows(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(mapper, org.mockito.Mockito.never()).findReserveTransactions(
                org.mockito.ArgumentMatchers.any());
    }

    private CashflowRow row(Long id, String source, String direction, String category,
            String description, String amount, LocalDate occurredOn, LocalDateTime occurredAt) {
        CashflowRow row = new CashflowRow();
        row.setId(id);
        row.setSource(source);
        row.setDirection(direction);
        row.setCategory(category);
        row.setDescription(description);
        row.setAmount(new BigDecimal(amount));
        row.setOccurredOn(occurredOn);
        row.setOccurredAt(occurredAt);
        row.setStatus("confirmed");
        return row;
    }
}
