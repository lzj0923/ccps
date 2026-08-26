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
import com.ccps.backend.service.AdminRentalAppointmentDetailsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/rental-mandates/{mandateId}/appointment-details")
public class AdminRentalAppointmentDetailsController {
    private final AdminRentalAppointmentDetailsService service;

    public AdminRentalAppointmentDetailsController(AdminRentalAppointmentDetailsService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Map<String, String>> find(@PathVariable("mandateId") Long mandateId) {
        return Map.of("fields", service.find(mandateId));
    }

    @PutMapping
    public Map<String, Map<String, String>> save(@PathVariable("mandateId") Long mandateId,
            @Valid @RequestBody AdminRentalAppointmentDetailsRequest request, HttpServletRequest servletRequest) {
        AuthInterceptor.userId(servletRequest);
        return Map.of("fields", service.save(mandateId, request.fields()));
    }
}
