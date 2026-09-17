package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import com.ccps.backend.dto.AdminRentalMandateCreateRequest;
import com.ccps.backend.mapper.AdminRentalMandateMapper;
import com.ccps.backend.mapper.AdminPropertyCashflowMapper;

class AdminTodayAdjustmentsTest {
    private AdminRentalMandateCreateRequest request(Long ownerUnitId) {
        return new AdminRentalMandateCreateRequest(ownerUnitId, "management", LocalDate.now(),
                LocalDate.now().plusYears(1), new BigDecimal("188"), BigDecimal.ZERO, null);
    }

    @Test void editingMandateUpdatesExactRecordWithoutRepostingFees() {
        var mapper = mock(AdminRentalMandateMapper.class);
        var fees = mock(PropertyExpensePostingService.class);
        var row = new AdminRentalMandateMapper.MandateRow();
        row.setId(500L); row.setOwnerUnitId(21L); row.setStatus("active");
        when(mapper.findById(500L)).thenReturn(row);
        when(mapper.lockStatus(500L)).thenReturn("active");
        when(mapper.updateDetails(eq(500L), any())).thenReturn(1);
        var request = request(21L);
        assertThat(new AdminRentalMandateService(mapper, fees).update(7L, 500L, request).id()).isEqualTo(500L);
        verify(mapper).updateDetails(500L, request);
        verify(mapper).insertHistory(eq(500L), eq("active"), eq("active"), anyString(), eq(7L));
        verifyNoInteractions(fees);
    }

    @Test void editingCannotMoveMandateToAnotherProperty() {
        var mapper = mock(AdminRentalMandateMapper.class);
        var row = new AdminRentalMandateMapper.MandateRow(); row.setOwnerUnitId(21L);
        when(mapper.findById(500L)).thenReturn(row); when(mapper.lockStatus(500L)).thenReturn("active");
        assertThatThrownBy(() -> new AdminRentalMandateService(mapper).update(7L,500L,request(22L)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Cannot change the property");
        verify(mapper, never()).updateDetails(anyLong(), any());
    }

    @Test void mandateDatesMustStillCoverExistingLeases() {
        var mapper = mock(AdminRentalMandateMapper.class);
        var row = new AdminRentalMandateMapper.MandateRow(); row.setOwnerUnitId(21L);
        when(mapper.findById(500L)).thenReturn(row); when(mapper.lockStatus(500L)).thenReturn("active");
        when(mapper.countLeasesOutsideTerm(eq(500L), any(), any())).thenReturn(1);
        assertThatThrownBy(() -> new AdminRentalMandateService(mapper).update(7L,500L,request(21L)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("cover existing leases");
        verify(mapper, never()).updateDetails(anyLong(), any());
    }

    @Test void terminatedMandatesCannotBeEdited() {
        var mapper = mock(AdminRentalMandateMapper.class);
        when(mapper.findById(500L)).thenReturn(new AdminRentalMandateMapper.MandateRow());
        when(mapper.lockStatus(500L)).thenReturn("terminated");
        assertThatThrownBy(() -> new AdminRentalMandateService(mapper).update(7L,500L,request(21L)))
                .isInstanceOf(ResponseStatusException.class);
        verify(mapper, never()).updateDetails(anyLong(), any());
    }

    @Test void historyReadUsesReadOnlyOwnershipLookupAndExclusiveMonthEnd() {
        var mapper = mock(AdminPropertyCashflowMapper.class);
        var context = new AdminPropertyCashflowMapper.PropertyContext(); context.setUnitId(8L);
        when(mapper.findPropertyForRead(1L,21L)).thenReturn(context);
        var row = new AdminPropertyCashflowMapper.CashflowRow();
        row.setId(99L); row.setReceiptDate(LocalDate.of(2026, 7, 31)); row.setOccurredOn(LocalDate.of(2026, 8, 1));
        when(mapper.listMonthly(8L, LocalDate.of(2026,8,1), LocalDate.of(2026,9,1))).thenReturn(List.of(row));
        var rows = new AdminPropertyCashflowService(mapper, "target/test-cashflow").list(1L,21L,"2026-08");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).receiptDate()).isEqualTo(LocalDate.of(2026,7,31));
        verify(mapper, never()).findProperty(anyLong(),anyLong());
    }

    @Test void invalidMonthDoesNotLoadAllHistory() {
        var mapper = mock(AdminPropertyCashflowMapper.class);
        when(mapper.findPropertyForRead(1L,21L)).thenReturn(new AdminPropertyCashflowMapper.PropertyContext());
        assertThatThrownBy(() -> new AdminPropertyCashflowService(mapper,"target/test-cashflow").list(1L,21L,"2026-13"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Invalid month");
        verify(mapper, never()).list(any());
        verify(mapper, never()).listMonthly(any(), any(), any());
    }
}
