package com.ccps.backend.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.FileSystemResource;
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
import com.ccps.backend.dto.OwnerDocumentResponse;
import com.ccps.backend.service.OwnerDocumentService;
import com.ccps.backend.service.OwnerDocumentService.Download;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/owner/documents")
public class OwnerDocumentController {
    private final OwnerDocumentService service;

    public OwnerDocumentController(OwnerDocumentService service) {
        this.service = service;
    }

    @GetMapping
    public OwnerDocumentResponse overview(HttpServletRequest request) {
        return service.getDocuments(AuthInterceptor.userId(request));
    }

    @GetMapping("/{documentId}/file")
    public ResponseEntity<FileSystemResource> file(@PathVariable Long documentId,
            @RequestParam(defaultValue = "false") boolean download,
            @RequestParam(defaultValue = "document") String source, HttpServletRequest request) {
        Download document = service.download(AuthInterceptor.userId(request), documentId, source);
        MediaType mediaType;
        try { mediaType = MediaType.parseMediaType(document.mimeType()); }
        catch (IllegalArgumentException ignored) { mediaType = MediaType.APPLICATION_OCTET_STREAM; }
        ContentDisposition disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                .filename(document.originalName(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(mediaType).contentLength(document.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(document.path()));
    }

}
