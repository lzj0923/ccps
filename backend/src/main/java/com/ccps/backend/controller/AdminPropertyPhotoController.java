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
import com.ccps.backend.dto.AdminPropertyPhotoResponse;
import com.ccps.backend.service.AdminPropertyPhotoService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/admin/owners/{ownerId}/properties/{ownerUnitId}/photos")
public class AdminPropertyPhotoController {
    private final AdminPropertyPhotoService service;

    public AdminPropertyPhotoController(AdminPropertyPhotoService service) { this.service = service; }

    @GetMapping
    public List<AdminPropertyPhotoResponse> list(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @RequestParam(required = false) Long leaseId) {
        return service.list(ownerId, ownerUnitId, leaseId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminPropertyPhotoResponse create(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @RequestParam String title, @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "0") Integer sortOrder,
            @RequestParam(defaultValue = "false") boolean cover,
            @RequestParam(required=false) Long leaseId,@RequestParam(required=false) String rentalStage,
            @RequestPart("file") MultipartFile file, HttpServletRequest request) {
        return service.create(AuthInterceptor.userId(request), ownerId, ownerUnitId, title, category,
                description, sortOrder, cover,leaseId,rentalStage, file);
    }

    @PutMapping(value = "/{photoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminPropertyPhotoResponse update(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @PathVariable Long photoId, @RequestParam String title, @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "0") Integer sortOrder,
            @RequestParam(defaultValue = "false") boolean cover,
            @RequestParam(required=false) Long leaseId,@RequestParam(required=false) String rentalStage,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return service.update(ownerId, ownerUnitId, photoId, title, category, description, sortOrder, cover,leaseId,rentalStage, file);
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> delete(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @PathVariable Long photoId) {
        service.delete(ownerId, ownerUnitId, photoId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{photoId}/content")
    public ResponseEntity<InputStreamResource> content(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @PathVariable Long photoId) throws IOException {
        AdminPropertyPhotoService.Download download = service.content(ownerId, ownerUnitId, photoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.mimeType()))
                .contentLength(download.size())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(download.originalName()).build().toString())
                .body(new InputStreamResource(Files.newInputStream(download.path())));
    }
}
