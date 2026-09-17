package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.mapper.AdminLeaseAgreementDetailsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AdminLeaseAgreementDetailsServiceTest {
    @Mock private AdminLeaseAgreementDetailsMapper mapper;
    private AdminLeaseAgreementDetailsService service;

    @BeforeEach
    void setUp() {
        service = new AdminLeaseAgreementDetailsService(mapper, new ObjectMapper());
    }

    @Test
    void savesTrimmedAgreementFieldsForLaterRegeneration() {
        when(mapper.countLease(7L)).thenReturn(1);
        Map<String, String> submitted = new LinkedHashMap<>();
        submitted.put("ownerSignerName", " Jane Owner ");
        submitted.put("tenantWitnessSignerName", " Tenant Witness ");
        submitted.put("bankAccount", null);

        Map<String, String> saved = service.save(7L, submitted);

        assertThat(saved).containsEntry("ownerSignerName", "Jane Owner")
                .containsEntry("tenantWitnessSignerName", "Tenant Witness")
                .containsEntry("bankAccount", "");
        verify(mapper).saveDetails(7L,
                "{\"ownerSignerName\":\"Jane Owner\",\"tenantWitnessSignerName\":\"Tenant Witness\",\"bankAccount\":\"\"}");
    }

    @Test
    void loadsPreviouslySavedAgreementFields() {
        when(mapper.countLease(7L)).thenReturn(1);
        when(mapper.findDetails(7L)).thenReturn("{\"ownerSignerName\":\"Jane Owner\",\"renewalOption\":\"One year\"}");

        assertThat(service.find(7L)).containsEntry("ownerSignerName", "Jane Owner")
                .containsEntry("renewalOption", "One year");
    }

    @Test
    void rejectsUnknownLease() {
        when(mapper.countLease(99L)).thenReturn(0);

        assertThatThrownBy(() -> service.find(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("租约不存在");
    }
}
