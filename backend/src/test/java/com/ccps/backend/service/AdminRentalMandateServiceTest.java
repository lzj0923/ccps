package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.mapper.AdminRentalMandateMapper;
import com.ccps.backend.mapper.AdminRentalMandateMapper.ExpiredMandateRow;
import com.ccps.backend.dto.AdminRentalMandateStatusRequest;
import com.ccps.backend.dto.AdminRentalMandateReviewRequest;
import com.ccps.backend.dto.AdminRentalMandateCreateRequest;
import com.ccps.backend.mapper.AdminRentalMandateMapper.MandateRow;
import com.ccps.backend.mapper.AdminRentalMandateMapper.NewMandate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminRentalMandateServiceTest {
    @Mock AdminRentalMandateMapper mapper;
    @Mock PropertyExpensePostingService propertyExpensePostingService;

    @Test
    void createsCurrentManagementFeeInExpenseAndFinanceReviewModules() throws Exception {
        var constructor = java.util.Arrays.stream(AdminRentalMandateService.class.getDeclaredConstructors())
                .filter(candidate -> java.util.Arrays.equals(candidate.getParameterTypes(),
                        new Class<?>[] { AdminRentalMandateMapper.class, PropertyExpensePostingService.class }))
                .findFirst().orElse(null);
        assertThat(constructor).as("租管委托服务必须连接统一费用入账服务").isNotNull();
        constructor.setAccessible(true);
        AdminRentalMandateService service = (AdminRentalMandateService) constructor
                .newInstance(mapper, propertyExpensePostingService);
        when(mapper.countOperatingUnit(21L)).thenReturn(1);
        when(mapper.countOverlapping(21L, LocalDate.of(2026, 8, 1), LocalDate.of(2027, 7, 31))).thenReturn(0);
        doAnswer(invocation -> { invocation.getArgument(0, NewMandate.class).setId(31L); return 1; })
                .when(mapper).insertMandate(any(NewMandate.class));
        MandateRow created = new MandateRow(); created.setId(31L); created.setStatus("active");
        when(mapper.findById(31L)).thenReturn(created);

        service.create(7L, new AdminRentalMandateCreateRequest(21L, "management",
                LocalDate.of(2026, 8, 1), LocalDate.of(2027, 7, 31),
                new BigDecimal("188.00"), BigDecimal.ZERO, null));

        verify(propertyExpensePostingService).syncMandateFee(eq(21L), eq(31L),
                eq(new BigDecimal("188.00")), eq(7L), any(LocalDate.class));
    }

    @Test
    void createsMandateAsActiveWithoutReview() {
        when(mapper.countOperatingUnit(21L)).thenReturn(1);
        when(mapper.countOverlapping(21L, LocalDate.of(2026, 8, 7), LocalDate.of(2028, 10, 7))).thenReturn(0);
        doAnswer(invocation -> { invocation.getArgument(0, NewMandate.class).setId(31L); return 1; })
                .when(mapper).insertMandate(org.mockito.ArgumentMatchers.any(NewMandate.class));
        MandateRow created = new MandateRow();
        created.setId(31L);
        created.setStatus("active");
        when(mapper.findById(31L)).thenReturn(created);

        new AdminRentalMandateService(mapper).create(7L, new AdminRentalMandateCreateRequest(
                21L, "management", LocalDate.of(2026, 8, 7), LocalDate.of(2028, 10, 7),
                BigDecimal.ZERO, BigDecimal.ZERO, null));

        verify(mapper).insertHistory(31L, null, "active", "建立并启用租管委托", 7L);
        verify(mapper).activateRentalService(31L);
    }

    @Test
    void expiresEndedMandateAndEndsRentalService() {
        ExpiredMandateRow row = new ExpiredMandateRow();
        row.setId(9L);
        row.setStatus("active");
        when(mapper.findExpiredMandates()).thenReturn(List.of(row));
        when(mapper.expireMandate(9L, "active")).thenReturn(1);

        int expired = new AdminRentalMandateService(mapper).expireEndedMandates();

        assertThat(expired).isEqualTo(1);
        verify(mapper).insertHistory(9L, "active", "expired", "委托期限已到期", null);
        verify(mapper).updateRentalService(9L, "terminated");
        verify(mapper).insertAudit(null, "auto_expire", 9L,
                "{\"status\":\"active\"}",
                "{\"status\":\"expired\",\"reason\":\"委托期限已到期\"}");
    }

    @Test
    void ignoresCandidateChangedByAnotherRequest() {
        ExpiredMandateRow row = new ExpiredMandateRow();
        row.setId(10L);
        row.setStatus("suspended");
        when(mapper.findExpiredMandates()).thenReturn(List.of(row));
        when(mapper.expireMandate(10L, "suspended")).thenReturn(0);

        int expired = new AdminRentalMandateService(mapper).expireEndedMandates();

        assertThat(expired).isZero();
        verify(mapper, never()).insertHistory(10L, "suspended", "expired", "委托期限已到期", null);
        verify(mapper, never()).updateRentalService(10L, "terminated");
        verify(mapper, never()).insertAudit(null, "auto_expire", 10L,
                "{\"status\":\"suspended\"}",
                "{\"status\":\"expired\",\"reason\":\"委托期限已到期\"}");
    }

    @Test
    void doesNothingWhenThereAreNoEndedMandates() {
        when(mapper.findExpiredMandates()).thenReturn(List.of());

        assertThat(new AdminRentalMandateService(mapper).expireEndedMandates()).isZero();
        verify(mapper, never()).updateRentalService(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void blocksTerminationWhenUnitHasActiveLease() {
        AdminRentalMandateMapper.MandateRow mandate = new AdminRentalMandateMapper.MandateRow();
        mandate.setId(11L);
        mandate.setStatus("active");
        when(mapper.findById(11L)).thenReturn(mandate);
        when(mapper.lockStatus(11L)).thenReturn("active");
        when(mapper.countActiveLeasesForMandate(11L)).thenReturn(1);

        assertThatThrownBy(() -> new AdminRentalMandateService(mapper).updateStatus(7L, 11L,
                new AdminRentalMandateStatusRequest("terminated", "结束委托")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("有效租約");
        verify(mapper, never()).updateStatus(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
        verify(mapper, never()).updateRentalService(11L, "terminated");
    }

    @Test
    void allowsTerminationWhenUnitHasNoActiveLease() {
        AdminRentalMandateMapper.MandateRow mandate = new AdminRentalMandateMapper.MandateRow();
        mandate.setId(12L);
        mandate.setStatus("active");
        when(mapper.findById(12L)).thenReturn(mandate);
        when(mapper.lockStatus(12L)).thenReturn("active");
        when(mapper.countActiveLeasesForMandate(12L)).thenReturn(0);
        when(mapper.updateStatus(eq(12L), eq("terminated"), eq(7L), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(), eq("结束委托")))
                .thenReturn(1);
        when(mapper.withdrawPendingDirectPayments(12L, 7L)).thenReturn(2);

        new AdminRentalMandateService(mapper).updateStatus(7L, 12L,
                new AdminRentalMandateStatusRequest("terminated", "结束委托"));

        verify(mapper).updateRentalService(12L, "terminated");
        verify(mapper).withdrawPendingDirectPayments(12L, 7L);
        verify(mapper).insertAudit(7L, "withdraw_pending_direct_payments", 12L,
                null, "{\"withdrawnCount\":2}");
    }

    @Test
    void allowsActivationWithoutSignedOwnerAuthorization() {
        AdminRentalMandateMapper.MandateRow mandate = new AdminRentalMandateMapper.MandateRow();
        mandate.setId(13L);
        mandate.setStatus("pending_review");
        when(mapper.findById(13L)).thenReturn(mandate);
        when(mapper.lockStatus(13L)).thenReturn("pending_review");
        when(mapper.updateStatus(eq(13L), eq("active"), eq(7L), org.mockito.ArgumentMatchers.any(), eq("通過審核"), eq(null)))
                .thenReturn(1);

        new AdminRentalMandateService(mapper).review(7L, 13L,
                new AdminRentalMandateReviewRequest(true, "通過審核"));

        verify(mapper).insertHistory(13L, "pending_review", "active", "通過審核", 7L);
        verify(mapper).activateRentalService(13L);
    }

    @Test
    void allowsReviewToActivateDraftMandateWhenSignedOwnerAuthorizationIsUploaded() {
        AdminRentalMandateMapper.MandateRow mandate = new AdminRentalMandateMapper.MandateRow();
        mandate.setId(14L);
        mandate.setStatus("draft");
        when(mapper.findById(14L)).thenReturn(mandate);
        when(mapper.lockStatus(14L)).thenReturn("draft");
        when(mapper.updateStatus(eq(14L), eq("active"), eq(7L), org.mockito.ArgumentMatchers.any(), eq("审核通过"), eq(null)))
                .thenReturn(1);

        new AdminRentalMandateService(mapper).review(7L, 14L,
                new AdminRentalMandateReviewRequest(true, "审核通过"));

        verify(mapper).insertHistory(14L, "draft", "active", "审核通过", 7L);
        verify(mapper).activateRentalService(14L);
    }
}
