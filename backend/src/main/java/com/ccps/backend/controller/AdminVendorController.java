package com.ccps.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminVendorRequest;
import com.ccps.backend.dto.AdminVendorResponse;
import com.ccps.backend.service.AdminVendorService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/vendors")
public class AdminVendorController {
    private final AdminVendorService service;

    public AdminVendorController(AdminVendorService service) { this.service = service; }

    @GetMapping
    public List<AdminVendorResponse> list() { return service.list(); }

    @PostMapping
    public AdminVendorResponse create(@Valid @RequestBody AdminVendorRequest body, HttpServletRequest request) {
        return service.create(AuthInterceptor.userId(request), body);
    }

    @PutMapping("/{id}")
    public AdminVendorResponse update(@PathVariable Long id, @Valid @RequestBody AdminVendorRequest body,
            HttpServletRequest request) {
        return service.update(AuthInterceptor.userId(request), id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id, HttpServletRequest request) {
        service.deactivate(AuthInterceptor.userId(request), id);
        return ResponseEntity.noContent().build();
    }
}
