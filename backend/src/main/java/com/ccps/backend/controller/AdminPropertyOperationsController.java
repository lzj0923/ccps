package com.ccps.backend.controller;

import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.dto.AdminPropertyOperationsSnapshotResponse;
import com.ccps.backend.dto.AdminPropertyOperationsChargeRequest;
import com.ccps.backend.dto.AdminPropertyOperationsWorkOrderRequest;
import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.service.AdminPropertyOperationsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/owners/{ownerId}/properties/{ownerUnitId}/operations")
public class AdminPropertyOperationsController {
    private final AdminPropertyOperationsService service;

    public AdminPropertyOperationsController(AdminPropertyOperationsService service) {
        this.service = service;
    }

    @GetMapping
    public AdminPropertyOperationsSnapshotResponse snapshot(@PathVariable Long ownerId,
            @PathVariable Long ownerUnitId, @RequestParam LocalDate month) {
        return service.snapshot(ownerId, ownerUnitId, month);
    }

    @PostMapping("/charges")
    public AdminPropertyOperationsSnapshotResponse addCharge(@PathVariable Long ownerId,
            @PathVariable Long ownerUnitId, @RequestParam LocalDate month,
            @Valid @org.springframework.web.bind.annotation.RequestBody AdminPropertyOperationsChargeRequest body,
            HttpServletRequest request) {
        return service.addCharge(AuthInterceptor.userId(request), ownerId, ownerUnitId, month, body);
    }

    @PostMapping("/work-orders")
    public AdminPropertyOperationsSnapshotResponse createWorkOrder(@PathVariable Long ownerId,
            @PathVariable Long ownerUnitId, @RequestParam LocalDate month,
            @Valid @org.springframework.web.bind.annotation.RequestBody AdminPropertyOperationsWorkOrderRequest body,
            HttpServletRequest request) {
        return service.createWorkOrder(AuthInterceptor.userId(request), ownerId, ownerUnitId, body, month);
    }
}
