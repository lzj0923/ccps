package com.ccps.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.OwnerDashboardResponse;
import com.ccps.backend.dto.OwnerPropertyServicesResponse;
import com.ccps.backend.dto.OwnerPropertyServicesUpdateRequest;
import com.ccps.backend.service.OwnerDashboardService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/owner/dashboard")
public class OwnerDashboardController {
    private final OwnerDashboardService service;

    public OwnerDashboardController(OwnerDashboardService service) {
        this.service = service;
    }

    @GetMapping
    public OwnerDashboardResponse currentOwnerDashboard(HttpServletRequest request) {
        return service.getDashboard(AuthInterceptor.userId(request));
    }

    @PutMapping("/properties/{ownerUnitId}/services")
    public OwnerPropertyServicesResponse updatePropertyServices(
            @PathVariable Long ownerUnitId,
            @Valid @RequestBody OwnerPropertyServicesUpdateRequest updateRequest,
            HttpServletRequest request) {
        return service.updatePropertyServices(AuthInterceptor.userId(request), ownerUnitId, updateRequest);
    }
}
