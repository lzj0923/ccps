package com.ccps.backend.service;

import static org.mockito.Mockito.*;

import com.ccps.backend.dto.AdminTenantWhatsAppSubscriptionRequest;
import com.ccps.backend.mapper.TenantWhatsAppSubscriptionMapper;
import com.ccps.backend.mapper.TenantWhatsAppSubscriptionMapper.TenantRow;
import org.junit.jupiter.api.Test;

class TenantWhatsAppSubscriptionServiceTest {
    private final TenantWhatsAppSubscriptionMapper mapper = mock(TenantWhatsAppSubscriptionMapper.class);
    private final TenantWhatsAppSubscriptionService service = new TenantWhatsAppSubscriptionService(mapper, "60");

    @Test void enrollsActiveTenantUsingTermsWithoutIndividualConfirmation() {
        service.synchronizeTenant(7L, "+8613012345678", "active");
        verify(mapper).synchronizeFromTerms(7L, "8613012345678", true, "lease_or_onboarding_terms", false);
    }

    @Test void defaultPhoneEditCannotOverrideOptOut() {
        service.synchronizeTenant(7L, "+60123456789", "active", null);
        verify(mapper).synchronizeFromTerms(7L, "60123456789", true, "lease_or_onboarding_terms", false);
        verify(mapper, never()).enable(anyLong(), anyString(), anyString());
    }

    @Test void explicitFalsePersistsOptOutEvenWithoutAPreviousSubscription() {
        service.synchronizeTenant(7L, null, "active", false);
        verify(mapper).disable(7L);
        verifyNoMoreInteractions(mapper);
    }

    @Test void onlyExplicitTrueCanRestoreOptOut() {
        service.synchronizeTenant(7L, "+60123456789", "active", true);
        verify(mapper).synchronizeFromTerms(7L, "60123456789", true, "lease_or_onboarding_terms", true);
    }

    @Test void missingInvalidAndAmbiguousPhonesDoNotEnableSending() {
        for (String phone : new String[]{null, "", "13012345678", "+60 abc", "123"}) {
            service.synchronizeTenant(7L, phone, "active");
        }
        verify(mapper, times(5)).synchronizeFromTerms(7L, "", false, "lease_or_onboarding_terms", false);
    }

    @Test void inactiveTenantStaysDisabledEvenWhenExplicitlyEnabled() {
        service.synchronizeTenant(7L, "+60123456789", "inactive", true);
        verify(mapper).synchronizeFromTerms(7L, "60123456789", false, "lease_or_onboarding_terms", true);
    }

    @Test void legacySubscriptionEndpointUsesServerTermsAndDoesNotDemandCheckbox() {
        TenantRow tenant = new TenantRow();
        tenant.setId(7L); tenant.setPhone("+60123456789"); tenant.setStatus("active");
        when(mapper.findTenant(7L)).thenReturn(tenant);
        service.update(1L, 7L, new AdminTenantWhatsAppSubscriptionRequest(true, false, null, "client_claim"));
        verify(mapper).enable(7L, "60123456789", "lease_or_onboarding_terms");
        verify(mapper).insertAudit(1L, 7L, "enable_tenant_whatsapp_notifications", true,
                "60123456789", "lease_or_onboarding_terms");
    }
}
