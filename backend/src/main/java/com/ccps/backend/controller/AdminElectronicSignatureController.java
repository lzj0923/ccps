package com.ccps.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.ElectronicSignatureStartRequest;
import com.ccps.backend.dto.ElectronicSignatureStartResponse;
import com.ccps.backend.dto.ElectronicSignaturePackageRequest;
import com.ccps.backend.dto.ElectronicSignatureParticipantResponse;
import com.ccps.backend.dto.ElectronicSignatureInvitationRequest;
import com.ccps.backend.service.ElectronicSignatureService;

@RestController
@RequestMapping("/api/admin/e-signatures")
public class AdminElectronicSignatureController {
    private final ElectronicSignatureService service;
    public AdminElectronicSignatureController(ElectronicSignatureService service) { this.service = service; }

    @GetMapping("/leases/{leaseId}/participants")
    public List<ElectronicSignatureParticipantResponse> leaseParticipants(@PathVariable Long leaseId) {
        return service.leaseParticipants(leaseId);
    }

    @PostMapping("/leases/{leaseId}/package")
    public ElectronicSignatureStartResponse startLeasePackage(@PathVariable Long leaseId,
            @Valid @RequestBody ElectronicSignaturePackageRequest request, HttpServletRequest servletRequest) {
        return service.startLeasePackage(AuthInterceptor.userId(servletRequest), leaseId, request);
    }

    @PostMapping("/rental-mandates/{mandateId}/documents/{documentId}")
    public ElectronicSignatureStartResponse startMandate(@PathVariable Long mandateId, @PathVariable Long documentId,
            @Valid @RequestBody ElectronicSignatureStartRequest request, HttpServletRequest servletRequest) {
        return service.startMandateDocument(AuthInterceptor.userId(servletRequest), mandateId, documentId, request);
    }

    @GetMapping("/rental-mandates/{mandateId}/documents/{documentId}/participants")
    public List<ElectronicSignatureParticipantResponse> mandateParticipants(@PathVariable Long mandateId,
            @PathVariable Long documentId) {
        return service.mandateParticipants(mandateId, documentId);
    }

    @PostMapping("/rental-mandates/{mandateId}/documents/{documentId}/package")
    public ElectronicSignatureStartResponse startMandatePackage(@PathVariable Long mandateId,
            @PathVariable Long documentId, @Valid @RequestBody ElectronicSignaturePackageRequest request,
            HttpServletRequest servletRequest) {
        return service.startMandatePackage(AuthInterceptor.userId(servletRequest), mandateId, documentId, request);
    }

    @PostMapping("/requests/{requestId}/email")
    public ResponseEntity<Void> sendInvitationEmail(@PathVariable Long requestId,
            @Valid @RequestBody ElectronicSignatureInvitationRequest request, HttpServletRequest servletRequest) {
        service.sendInvitationEmail(AuthInterceptor.userId(servletRequest), requestId, request.token(),
                request.recipientEmail());
        return ResponseEntity.noContent().build();
    }
}
