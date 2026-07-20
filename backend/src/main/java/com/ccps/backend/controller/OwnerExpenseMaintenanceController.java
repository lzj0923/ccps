package com.ccps.backend.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.MaintenanceDetailResponse;
import com.ccps.backend.dto.MaintenanceDetailResponse.Attachment;
import com.ccps.backend.dto.OwnerExpenseMaintenanceResponse;
import com.ccps.backend.service.MaintenanceAttachmentService;
import com.ccps.backend.service.MaintenanceAttachmentService.Download;
import com.ccps.backend.service.OwnerExpenseMaintenanceService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/owner/expenses")
public class OwnerExpenseMaintenanceController {
    private final OwnerExpenseMaintenanceService service;
    private final MaintenanceAttachmentService attachmentService;

    public OwnerExpenseMaintenanceController(OwnerExpenseMaintenanceService service,
            MaintenanceAttachmentService attachmentService) {
        this.service = service;
        this.attachmentService = attachmentService;
    }

    @GetMapping
    public OwnerExpenseMaintenanceResponse overview(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            HttpServletRequest request) {
        return service.getOverview(AuthInterceptor.userId(request), projectId, category, status, startDate, endDate);
    }

    @GetMapping("/maintenance/{workOrderId}")
    public MaintenanceDetailResponse detail(@PathVariable Long workOrderId, HttpServletRequest request) {
        return service.getMaintenanceDetail(AuthInterceptor.userId(request), workOrderId);
    }

    @PostMapping(value = "/maintenance/{workOrderId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Attachment> upload(
            @PathVariable Long workOrderId,
            @RequestParam String relationType,
            @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest request) {
        return attachmentService.upload(AuthInterceptor.userId(request), workOrderId, relationType, files);
    }

    @GetMapping("/attachments/{documentId}")
    public ResponseEntity<FileSystemResource> attachment(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean download,
            HttpServletRequest request) {
        Download file = attachmentService.download(AuthInterceptor.userId(request), documentId);
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(file.mimeType()); }
        catch (IllegalArgumentException ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        ContentDisposition disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(file.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(file.path()));
    }

}
