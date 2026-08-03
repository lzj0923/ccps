package com.ccps.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.ElectronicSignatureStartRequest;
import com.ccps.backend.dto.ElectronicSignatureStartResponse;
import com.ccps.backend.service.ElectronicSignatureService;

@RestController
@RequestMapping("/api/admin/e-signatures")
public class AdminElectronicSignatureController {
    private final ElectronicSignatureService service;
    public AdminElectronicSignatureController(ElectronicSignatureService service) { this.service = service; }

    @PostMapping("/leases/{leaseId}")
    public ElectronicSignatureStartResponse startLease(@PathVariable Long leaseId,
            @Valid @RequestBody ElectronicSignatureStartRequest request, HttpServletRequest servletRequest) {
        return service.startLease(AuthInterceptor.userId(servletRequest), leaseId, request);
    }

    @PostMapping("/rental-mandates/{mandateId}/documents/{documentId}")
    public ElectronicSignatureStartResponse startMandate(@PathVariable Long mandateId, @PathVariable Long documentId,
            @Valid @RequestBody ElectronicSignatureStartRequest request, HttpServletRequest servletRequest) {
        return service.startMandateDocument(AuthInterceptor.userId(servletRequest), mandateId, documentId, request);
    }
}
