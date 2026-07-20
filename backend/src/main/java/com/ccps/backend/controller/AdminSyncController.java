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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminSyncPreviewResponse;
import com.ccps.backend.dto.AdminSyncRequest;
import com.ccps.backend.dto.AdminSyncResponse;
import com.ccps.backend.service.AdminSyncService;
import com.ccps.backend.service.AdminSyncService.Download;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/sync")
public class AdminSyncController {
    private final AdminSyncService service;
    public AdminSyncController(AdminSyncService service) { this.service = service; }

    @GetMapping public AdminSyncResponse overview() { return service.overview(); }
    @PostMapping("/preview") public AdminSyncPreviewResponse preview(@Valid @RequestBody AdminSyncRequest body) { return service.preview(body); }
    @PostMapping("/batches") public AdminSyncResponse.Batch create(@Valid @RequestBody AdminSyncRequest body, HttpServletRequest request) { return service.createBatch(AuthInterceptor.userId(request), body); }
    @PostMapping("/batches/{batchId}/retry") public AdminSyncResponse.Batch retry(@PathVariable Long batchId, HttpServletRequest request) { return service.retryBatch(AuthInterceptor.userId(request), batchId); }

    @GetMapping("/batches/{batchId}/file")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long batchId) {
        Download file = service.download(batchId);
        ContentDisposition disposition = ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .contentLength(file.size()).header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(file.path()));
    }
}
