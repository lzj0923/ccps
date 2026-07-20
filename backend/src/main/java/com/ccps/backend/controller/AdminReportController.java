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
import com.ccps.backend.dto.AdminReportGenerateRequest;
import com.ccps.backend.dto.AdminReportResponse;
import com.ccps.backend.service.AdminReportService;
import com.ccps.backend.service.AdminReportService.Download;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {
    private final AdminReportService service;
    public AdminReportController(AdminReportService service) { this.service = service; }
    @GetMapping public AdminReportResponse overview() { return service.overview(); }
    @PostMapping("/runs") public AdminReportResponse.Run generate(@Valid @RequestBody AdminReportGenerateRequest body, HttpServletRequest request) { return service.generate(AuthInterceptor.userId(request), body); }
    @GetMapping("/runs/{runId}/file") public ResponseEntity<FileSystemResource> download(@PathVariable Long runId) {
        Download file = service.download(runId);
        String type = file.filename().toLowerCase().endsWith(".pdf") ? "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        ContentDisposition disposition = ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString()).body(new FileSystemResource(file.path()));
    }
}
