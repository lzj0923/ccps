package com.ccps.backend.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.ReserveTopupReviewRequest;
import com.ccps.backend.dto.ReserveTopupReviewResponse;
import com.ccps.backend.dto.AdminFinanceBatchConfirmRequest;
import com.ccps.backend.dto.AdminReserveManagementResponse;
import com.ccps.backend.dto.AdminReserveSettingsRequest;
import com.ccps.backend.dto.AdminReserveDirectTopupRequest;
import com.ccps.backend.dto.AdminReserveRefundRequest;
import com.ccps.backend.dto.AdminRecordCreateResponse;
import com.ccps.backend.service.AdminReserveManagementService;
import com.ccps.backend.service.ReserveTopupService;
import com.ccps.backend.service.ReserveTopupService.Download;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/reserve")
public class AdminReserveController {
    private final ReserveTopupService service;
    private final AdminReserveManagementService managementService;

    public AdminReserveController(ReserveTopupService service, AdminReserveManagementService managementService) {
        this.service = service;
        this.managementService = managementService;
    }

    @GetMapping
    public AdminReserveManagementResponse overview() {
        return managementService.overview();
    }

    @PutMapping("/accounts/{accountId}/settings")
    public ResponseEntity<Void> updateSettings(@PathVariable Long accountId,
            @Valid @RequestBody AdminReserveSettingsRequest request, HttpServletRequest servletRequest) {
        managementService.updateSettings(AuthInterceptor.userId(servletRequest), accountId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/accounts/{accountId}/direct-topups")
    public AdminRecordCreateResponse directTopup(@PathVariable Long accountId,
            @Valid @RequestBody AdminReserveDirectTopupRequest request, HttpServletRequest servletRequest) {
        return managementService.directTopup(AuthInterceptor.userId(servletRequest), accountId, request);
    }

    @PostMapping("/accounts/{accountId}/refunds")
    public AdminRecordCreateResponse createRefund(@PathVariable Long accountId,
            @Valid @RequestBody AdminReserveRefundRequest request, HttpServletRequest servletRequest) {
        return managementService.createRefund(AuthInterceptor.userId(servletRequest), accountId, request);
    }

    @PostMapping("/topups/{financeRecordId}/review")
    public ReserveTopupReviewResponse review(@PathVariable Long financeRecordId,
            @RequestBody ReserveTopupReviewRequest request, HttpServletRequest servletRequest) {
        return service.review(AuthInterceptor.userId(servletRequest), financeRecordId,
                request.approved(), request.note());
    }

    @PostMapping("/topups/batch-confirm")
    public ResponseEntity<Void> confirmBatch(@RequestBody AdminFinanceBatchConfirmRequest request,
            HttpServletRequest servletRequest) {
        service.confirmBatch(AuthInterceptor.userId(servletRequest), request.ids(), request.note());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/proofs/{documentId}")
    public ResponseEntity<FileSystemResource> proof(@PathVariable Long documentId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "false") boolean download) {
        Download file = service.downloadAdmin(documentId);
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(file.mimeType()); }
        catch (IllegalArgumentException ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        ContentDisposition disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(file.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(mediaType).contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(file.path()));
    }
}
