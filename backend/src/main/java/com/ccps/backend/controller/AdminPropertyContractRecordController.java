package com.ccps.backend.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
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

import com.ccps.backend.dto.AdminPropertyContractRecordResponse;
import com.ccps.backend.dto.AdminPropertyLeaseOptionResponse;
import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.service.AdminPropertyContractRecordService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/owners/{ownerId}/properties/{ownerUnitId}/contracts")
public class AdminPropertyContractRecordController {
    private final AdminPropertyContractRecordService service;

    public AdminPropertyContractRecordController(AdminPropertyContractRecordService service) { this.service = service; }

    @GetMapping
    public List<AdminPropertyContractRecordResponse> list(@PathVariable Long ownerId, @PathVariable Long ownerUnitId) {
        return service.list(ownerId, ownerUnitId);
    }

    @GetMapping("/lease-options")
    public List<AdminPropertyLeaseOptionResponse> leaseOptions(@PathVariable Long ownerId, @PathVariable Long ownerUnitId) {
        return service.leaseOptions(ownerId, ownerUnitId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminPropertyContractRecordResponse create(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @RequestParam(required = false) Long leaseId, @RequestParam String contractType, @RequestParam String contractNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate signedDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validTo,
            @RequestParam(defaultValue = "active") String status, @RequestParam(required = false) String notes,
            @RequestPart("file") MultipartFile file, HttpServletRequest request) {
        return service.create(AuthInterceptor.userId(request), ownerId, ownerUnitId, leaseId, contractType, contractNo,
                signedDate, validFrom, validTo, status, notes, file);
    }

    @PutMapping(value = "/{contractId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminPropertyContractRecordResponse update(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @PathVariable Long contractId, @RequestParam(required = false) Long leaseId,
            @RequestParam String contractType, @RequestParam String contractNo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate signedDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validTo,
            @RequestParam String status, @RequestParam(required = false) String notes,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return service.update(ownerId, ownerUnitId, contractId, leaseId, contractType, contractNo,
                signedDate, validFrom, validTo, status, notes, file);
    }

    @DeleteMapping("/{contractId}")
    public ResponseEntity<Void> delete(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @PathVariable Long contractId) {
        service.delete(ownerId, ownerUnitId, contractId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{contractId}/file")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @PathVariable Long contractId) throws IOException {
        var file = service.download(ownerId, ownerUnitId, contractId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(file.mimeType())).contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encode(file.originalName()))
                .body(new InputStreamResource(Files.newInputStream(file.path())));
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }
}
