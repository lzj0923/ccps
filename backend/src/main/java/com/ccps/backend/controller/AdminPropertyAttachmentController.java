package com.ccps.backend.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminPropertyAttachmentResponse;
import com.ccps.backend.service.AdminPropertyAttachmentService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/owners/{ownerId}/properties/{ownerUnitId}/attachments")
public class AdminPropertyAttachmentController {
    private final AdminPropertyAttachmentService service;

    public AdminPropertyAttachmentController(AdminPropertyAttachmentService service) { this.service = service; }

    @GetMapping
    public List<AdminPropertyAttachmentResponse> list(@PathVariable Long ownerId, @PathVariable Long ownerUnitId) {
        return service.list(ownerId, ownerUnitId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminPropertyAttachmentResponse create(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @RequestParam String title, @RequestParam(required = false) String remarks,
            @RequestParam(defaultValue = "true") boolean enabled, @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {
        return service.create(AuthInterceptor.userId(request), ownerId, ownerUnitId, title, remarks, enabled, file);
    }

    @PutMapping(value = "/{attachmentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminPropertyAttachmentResponse update(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @PathVariable Long attachmentId, @RequestParam String title,
            @RequestParam(required = false) String remarks, @RequestParam(defaultValue = "true") boolean enabled,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return service.update(ownerId, ownerUnitId, attachmentId, title, remarks, enabled, file);
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @PathVariable Long attachmentId) {
        service.delete(ownerId, ownerUnitId, attachmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{attachmentId}/file")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @PathVariable Long attachmentId) throws IOException {
        AdminPropertyAttachmentService.Download download = service.download(ownerId, ownerUnitId, attachmentId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(download.mimeType()))
                .contentLength(download.size())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(download.originalName()).build().toString())
                .body(new InputStreamResource(Files.newInputStream(download.path())));
    }
}
