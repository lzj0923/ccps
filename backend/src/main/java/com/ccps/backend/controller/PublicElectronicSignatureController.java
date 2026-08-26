package com.ccps.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.dto.ElectronicSignaturePublicResponse;
import com.ccps.backend.dto.ElectronicSignatureSignRequest;
import com.ccps.backend.service.ElectronicSignatureService;
import com.ccps.backend.service.ElectronicSignatureService.Download;
import com.ccps.backend.web.DownloadContentDisposition;

@RestController
@RequestMapping("/api/public/signatures")
public class PublicElectronicSignatureController {
    private final ElectronicSignatureService service;
    public PublicElectronicSignatureController(ElectronicSignatureService service) { this.service = service; }

    @GetMapping("/{token}") public ElectronicSignaturePublicResponse view(@PathVariable String token) { return service.publicView(token); }
    @PostMapping("/{token}/verification-code") public ResponseEntity<Void> resend(@PathVariable String token, HttpServletRequest request) {
        service.resendCode(token, remoteIp(request), request.getHeader("User-Agent")); return ResponseEntity.noContent().build();
    }
    @PostMapping("/{token}/sign") public ElectronicSignaturePublicResponse sign(@PathVariable String token,
            @Valid @RequestBody ElectronicSignatureSignRequest payload, HttpServletRequest request) {
        return service.sign(token, payload, remoteIp(request), request.getHeader("User-Agent"));
    }
    @GetMapping("/{token}/document") public ResponseEntity<FileSystemResource> document(@PathVariable String token) { return file(service.downloadOriginal(token), false); }
    @GetMapping("/{token}/signed-document") public ResponseEntity<FileSystemResource> signedDocument(@PathVariable String token) { return file(service.downloadSigned(token), true); }

    private ResponseEntity<FileSystemResource> file(Download file, boolean attachment) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).contentLength(file.path().toFile().length())
                .header(HttpHeaders.CONTENT_DISPOSITION, DownloadContentDisposition.value(
                        attachment ? "attachment" : "inline", file.originalName()))
                .body(new FileSystemResource(file.path()));
    }
    private String remoteIp(HttpServletRequest request) { String forwarded = request.getHeader("X-Forwarded-For"); return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim(); }
}
