package com.ccps.backend.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.mapper.MetaDataDeletionMapper.RequestRow;
import com.ccps.backend.service.MetaDataDeletionService;

// Existing /api/admin/system/** policy requires the SYSTEM_MANAGE permission.
@RestController
@RequestMapping("/api/admin/system/meta-deletion-requests")
public class AdminMetaDataDeletionController {
    private final MetaDataDeletionService service;
    public AdminMetaDataDeletionController(MetaDataDeletionService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<RequestRow>> list(@RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(limit, offset));
    }

    public record Review(String status, String note, boolean processingConfirmed) {}

    @PatchMapping("/{id}")
    public ResponseEntity<Void> review(@PathVariable Long id, @RequestBody Review body, HttpServletRequest request) {
        service.review(id, AuthInterceptor.userId(request), body.status(), body.note(), body.processingConfirmed());
        return ResponseEntity.noContent().build();
    }
}
