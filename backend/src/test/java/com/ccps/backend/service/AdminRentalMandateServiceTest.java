package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.mapper.AdminRentalMandateMapper;
import com.ccps.backend.mapper.AdminRentalMandateMapper.ExpiredMandateRow;
import com.ccps.backend.dto.AdminRentalMandateStatusRequest;
import com.ccps.backend.dto.AdminRentalMandateReviewRequest;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminRentalMandateServiceTest {
    @Mock AdminRentalMandateMapper mapper;

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

        new AdminRentalMandateService(mapper).updateStatus(7L, 12L,
                new AdminRentalMandateStatusRequest("terminated", "结束委托"));

        verify(mapper).updateRentalService(12L, "terminated");
    }

    @Test
    void blocksActivationUntilSignedOwnerAuthorizationIsUploaded() {
        AdminRentalMandateMapper.MandateRow mandate = new AdminRentalMandateMapper.MandateRow();
        mandate.setId(13L);
        mandate.setStatus("pending_review");
        when(mapper.findById(13L)).thenReturn(mandate);
        when(mapper.lockStatus(13L)).thenReturn("pending_review");
        when(mapper.countAuthorizationDocuments(13L)).thenReturn(0);

        assertThatThrownBy(() -> new AdminRentalMandateService(mapper).review(7L, 13L,
                new AdminRentalMandateReviewRequest(true, "通過審核")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("授权委托书");
        verify(mapper, never()).updateStatus(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void allowsReviewToActivateDraftMandateWhenSignedOwnerAuthorizationIsUploaded() {
        AdminRentalMandateMapper.MandateRow mandate = new AdminRentalMandateMapper.MandateRow();
        mandate.setId(14L);
        mandate.setStatus("draft");
        when(mapper.findById(14L)).thenReturn(mandate);
        when(mapper.lockStatus(14L)).thenReturn("draft");
        when(mapper.countAuthorizationDocuments(14L)).thenReturn(1);
        when(mapper.updateStatus(eq(14L), eq("active"), eq(7L), org.mockito.ArgumentMatchers.any(), eq("审核通过"), eq(null)))
                .thenReturn(1);

        new AdminRentalMandateService(mapper).review(7L, 14L,
                new AdminRentalMandateReviewRequest(true, "审核通过"));

        verify(mapper).insertHistory(14L, "draft", "active", "审核通过", 7L);
        verify(mapper).activateRentalService(14L);
    }
}
