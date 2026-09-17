package com.ccps.backend.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminRentalAppointmentDetailsRequest;
import com.ccps.backend.service.AdminLeaseAgreementDetailsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/tenancy/leases/{leaseId}/agreement-details")
public class AdminLeaseAgreementDetailsController {
    private final AdminLeaseAgreementDetailsService service;
    public AdminLeaseAgreementDetailsController(AdminLeaseAgreementDetailsService service) { this.service = service; }

    @GetMapping
    public Map<String, Map<String, String>> find(@PathVariable Long leaseId) {
        return Map.of("fields", service.find(leaseId));
    }

    @PutMapping
    public Map<String, Map<String, String>> save(@PathVariable Long leaseId,
            @Valid @RequestBody AdminRentalAppointmentDetailsRequest request, HttpServletRequest servletRequest) {
        AuthInterceptor.userId(servletRequest);
        return Map.of("fields", service.save(leaseId, request.fields()));
    }
}
