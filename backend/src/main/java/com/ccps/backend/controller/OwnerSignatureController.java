package com.ccps.backend.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.ElectronicSignaturePublicResponse;
import com.ccps.backend.dto.ElectronicSignatureSignRequest;
import com.ccps.backend.mapper.OwnerSignatureMapper.Task;
import com.ccps.backend.service.OwnerSignatureService;
import com.ccps.backend.web.DownloadContentDisposition;

@RestController
@RequestMapping("/api/owner/signatures")
public class OwnerSignatureController {
    private final OwnerSignatureService service;
    public OwnerSignatureController(OwnerSignatureService service) { this.service = service; }
    @GetMapping public List<Task> tasks(HttpServletRequest request) {
        return service.tasks(AuthInterceptor.userId(request));
    }
    @GetMapping("/{id}") public ElectronicSignaturePublicResponse view(@PathVariable Long id, HttpServletRequest request) {
        return service.view(AuthInterceptor.userId(request), id);
    }
    @PostMapping("/{id}/sign") public ElectronicSignaturePublicResponse sign(@PathVariable Long id,
            @Valid @RequestBody ElectronicSignatureSignRequest payload, HttpServletRequest request) {
        return service.sign(AuthInterceptor.userId(request), id, payload, request.getRemoteAddr(), request.getHeader("User-Agent"));
    }
    @GetMapping("/{id}/file") public ResponseEntity<FileSystemResource> file(@PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean signed, HttpServletRequest request) {
        var file = service.file(AuthInterceptor.userId(request), id, signed);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).contentLength(file.path().toFile().length())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, DownloadContentDisposition.value("inline", file.originalName()))
            .body(new FileSystemResource(file.path()));
    }
}
