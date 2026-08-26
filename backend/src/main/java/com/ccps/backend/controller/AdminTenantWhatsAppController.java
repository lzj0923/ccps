package com.ccps.backend.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminTenantWhatsAppSubscriptionRequest;
import com.ccps.backend.dto.AdminTenantWhatsAppSubscriptionResponse;
import com.ccps.backend.service.TenantWhatsAppSubscriptionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/tenancy/tenants")
public class AdminTenantWhatsAppController {
    private final TenantWhatsAppSubscriptionService service;

    public AdminTenantWhatsAppController(TenantWhatsAppSubscriptionService service) {
        this.service = service;
    }

    @PutMapping("/{tenantId}/whatsapp-subscription")
    public AdminTenantWhatsAppSubscriptionResponse update(
            @PathVariable Long tenantId,
            @Valid @RequestBody AdminTenantWhatsAppSubscriptionRequest body,
            HttpServletRequest request) {
        return service.update(AuthInterceptor.userId(request), tenantId, body);
    }
}
