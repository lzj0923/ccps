package com.ccps.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.OwnerPropertyCashflowResponse;
import com.ccps.backend.service.OwnerPropertyCashflowService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/owner/properties/{ownerUnitId}/cashflows")
public class OwnerPropertyCashflowController {
    private final OwnerPropertyCashflowService service;

    public OwnerPropertyCashflowController(OwnerPropertyCashflowService service) {
        this.service = service;
    }

    @GetMapping
    public OwnerPropertyCashflowResponse list(
            @PathVariable Long ownerUnitId, HttpServletRequest request) {
        return service.list(AuthInterceptor.userId(request), ownerUnitId);
    }
}
