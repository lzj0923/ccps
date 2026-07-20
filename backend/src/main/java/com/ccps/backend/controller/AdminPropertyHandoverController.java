package com.ccps.backend.controller;

import com.ccps.backend.dto.AdminPropertyHandoverRequest;
import com.ccps.backend.dto.AdminPropertyHandoverResponse;
import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.service.AdminPropertyHandoverService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/rental-mandates/{mandateId}/handover")
public class AdminPropertyHandoverController {
    private final AdminPropertyHandoverService service;
    public AdminPropertyHandoverController(AdminPropertyHandoverService service) { this.service = service; }
    @GetMapping public AdminPropertyHandoverResponse find(@PathVariable Long mandateId) { return service.find(mandateId); }
    @PutMapping public AdminPropertyHandoverResponse save(@PathVariable Long mandateId, @Valid @RequestBody AdminPropertyHandoverRequest request, HttpServletRequest httpRequest) { return service.save(mandateId, request, AuthInterceptor.userId(httpRequest)); }
}
