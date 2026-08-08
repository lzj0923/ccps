package com.ccps.backend.controller;

import java.nio.charset.StandardCharsets;
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

import com.ccps.backend.dto.AdminSystemBackupResponse;
import com.ccps.backend.dto.AdminSystemBackupResponse.Item;
import com.ccps.backend.dto.AdminSystemBackupResponse.RestoreResult;
import com.ccps.backend.service.AdminSystemBackupService;
import com.ccps.backend.service.AdminSystemBackupService.BackupArtifact;

@RestController
@RequestMapping("/api/admin/system/backups")
public class AdminSystemBackupController {
    private final AdminSystemBackupService service;
    public AdminSystemBackupController(AdminSystemBackupService service) { this.service = service; }

    @GetMapping
    public AdminSystemBackupResponse backups() {
        List<Item> rows = service.listBackups();
        return new AdminSystemBackupResponse(rows);
    }

    @PostMapping
    public ResponseEntity<FileSystemResource> createBackup() {
        return download(service.createManualBackup());
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<FileSystemResource> downloadBackup(@PathVariable String fileName) {
        return download(service.requireBackup(fileName));
    }

    @PostMapping(value = "/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RestoreResult restore(@RequestParam MultipartFile file, @RequestParam String confirmation) {
        return service.restore(file, confirmation);
    }

    private ResponseEntity<FileSystemResource> download(BackupArtifact backup) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(backup.fileName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .contentLength(backup.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new FileSystemResource(backup.path()));
    }
}
