package com.ccps.backend.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.dto.AdminAccountCreateRequest;
import com.ccps.backend.dto.AdminAccountResponse;
import com.ccps.backend.dto.AdminAccountUpdateRequest;
import com.ccps.backend.service.AdminAccountService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {
    private final AdminAccountService service;

    public AdminAccountController(AdminAccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminAccountResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AdminAccountResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<AdminAccountResponse> create(@Valid @RequestBody AdminAccountCreateRequest request) {
        AdminAccountResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/admin/accounts/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public AdminAccountResponse update(@PathVariable Long id, @Valid @RequestBody AdminAccountUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
