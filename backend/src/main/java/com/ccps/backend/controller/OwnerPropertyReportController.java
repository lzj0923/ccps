package com.ccps.backend.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.OwnerPropertyHandoverReportResponse;
import com.ccps.backend.service.AdminPropertyHandoverReportService.Download;
import com.ccps.backend.service.OwnerPropertyReportService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/owner/properties/{ownerUnitId}")
public class OwnerPropertyReportController {
    private final OwnerPropertyReportService service;

    public OwnerPropertyReportController(OwnerPropertyReportService service) {
        this.service = service;
    }

    @GetMapping("/handover-reports")
    public List<OwnerPropertyHandoverReportResponse> handoverReports(
            @PathVariable Long ownerUnitId, HttpServletRequest request) {
        return service.handoverReports(AuthInterceptor.userId(request), ownerUnitId);
    }

    @GetMapping("/handover-reports/{reportId}/file")
    public ResponseEntity<?> handoverReportFile(
            @PathVariable Long ownerUnitId, @PathVariable Long reportId,
            @RequestParam(defaultValue = "false") boolean download, HttpServletRequest request) throws IOException {
        Download file = service.downloadHandoverReport(AuthInterceptor.userId(request), ownerUnitId, reportId);
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(file.mimeType()); }
        catch (IllegalArgumentException ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        ContentDisposition disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(file.originalName(), StandardCharsets.UTF_8).build();
        var response = ResponseEntity.ok().contentType(mediaType).contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        if (file.bytes() != null) return response.body(new ByteArrayResource(file.bytes()));
        return response.body(new InputStreamResource(Files.newInputStream(file.path())));
    }
}
