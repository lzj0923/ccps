package com.ccps.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminTenantWhatsAppSubscriptionRequest;
import com.ccps.backend.dto.AdminTenantWhatsAppSubscriptionResponse;
import com.ccps.backend.mapper.TenantWhatsAppSubscriptionMapper;
import com.ccps.backend.mapper.TenantWhatsAppSubscriptionMapper.SubscriptionRow;
import com.ccps.backend.mapper.TenantWhatsAppSubscriptionMapper.TenantRow;

@Service
public class TenantWhatsAppSubscriptionService {
    static final String TERMS_SOURCE = "lease_or_onboarding_terms";
    private final TenantWhatsAppSubscriptionMapper mapper;
    private final String defaultCountryCode;

    public TenantWhatsAppSubscriptionService(
            TenantWhatsAppSubscriptionMapper mapper,
            @Value("${ccps.whatsapp.default-country-code:60}") String defaultCountryCode) {
        this.mapper = mapper;
        this.defaultCountryCode = defaultCountryCode;
    }

    @Transactional
    public AdminTenantWhatsAppSubscriptionResponse update(
            Long actorId, Long tenantId, AdminTenantWhatsAppSubscriptionRequest request) {
        TenantRow tenant = mapper.findTenant(tenantId);
        if (tenant == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");

        String source = TERMS_SOURCE;
        String destination = null;
        if (request.enabled()) {
            if (!"active".equals(tenant.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "WhatsApp notifications can only be enabled for an active tenant");
            }
            try {
                destination = WhatsAppPhoneNumbers.normalize(
                        text(request.destination(), tenant.getPhone()), defaultCountryCode);
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
            }
            mapper.enable(tenantId, destination, source);
        } else {
            SubscriptionRow current = mapper.findSubscription(tenantId);
            destination = current == null ? null : current.getDestination();
            mapper.disable(tenantId);
        }
        mapper.insertAudit(actorId, tenantId,
                request.enabled() ? "enable_tenant_whatsapp_notifications" : "disable_tenant_whatsapp_notifications",
                request.enabled(), destination, source);
        return response(mapper.findSubscription(tenantId), tenantId);
    }

    /** Records the agreed lease/onboarding terms; ordinary edits never reverse an opt-out. */
    @Transactional
    public void synchronizeTenant(Long tenantId, String phone, String status) {
        synchronizeTenant(tenantId, phone, status, null);
    }

    @Transactional
    public void synchronizeTenant(Long tenantId, String phone, String status, Boolean requestedEnabled) {
        if (Boolean.FALSE.equals(requestedEnabled)) {
            mapper.disable(tenantId);
            return;
        }
        String normalized = phone == null ? "" : phone.trim();
        boolean valid = normalized.matches("\\+[1-9][0-9]{7,14}");
        mapper.synchronizeFromTerms(tenantId, valid ? normalized.substring(1) : "",
                valid && "active".equals(status), TERMS_SOURCE, Boolean.TRUE.equals(requestedEnabled));
    }

    @Transactional
    public void synchronizeTenant(Long tenantId) {
        TenantRow tenant = mapper.findTenant(tenantId);
        if (tenant != null) synchronizeTenant(tenantId, tenant.getPhone(), tenant.getStatus());
    }

    @Transactional
    public void deleteForTenant(Long tenantId) {
        mapper.deleteForTenant(tenantId);
    }

    private AdminTenantWhatsAppSubscriptionResponse response(SubscriptionRow row, Long tenantId) {
        if (row == null) return new AdminTenantWhatsAppSubscriptionResponse(tenantId, null, false, null, null, null);
        return new AdminTenantWhatsAppSubscriptionResponse(row.getTenantId(), row.getDestination(),
                Boolean.TRUE.equals(row.getEnabled()), row.getOptedInAt(), row.getOptedOutAt(), row.getOptInSource());
    }

    private String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
