package com.ccps.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminRentalMandateCreateRequest;
import com.ccps.backend.dto.AdminRentalMandateResponse;
import com.ccps.backend.dto.AdminRentalMandateResponse.History;
import com.ccps.backend.dto.AdminRentalMandateResponse.Options;
import com.ccps.backend.dto.AdminRentalMandateReviewRequest;
import com.ccps.backend.dto.AdminRentalMandateStatusRequest;
import com.ccps.backend.service.AdminRentalMandateService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/rental-mandates")
public class AdminRentalMandateController {
    private final AdminRentalMandateService service;

    public AdminRentalMandateController(AdminRentalMandateService service) { this.service = service; }

    @GetMapping
    public AdminRentalMandateResponse find(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return service.find(page, pageSize, keyword, status);
    }

    @GetMapping("/options") public Options options() { return service.options(); }

    @GetMapping("/{mandateId}")
    public AdminRentalMandateResponse.Item detail(@PathVariable Long mandateId) { return service.detail(mandateId); }

    @PutMapping("/{mandateId}")
    public AdminRentalMandateResponse.Item update(@PathVariable Long mandateId,
            @Valid @RequestBody AdminRentalMandateCreateRequest body, HttpServletRequest request) {
        return service.update(AuthInterceptor.userId(request), mandateId, body);
    }

    @GetMapping("/{mandateId}/history")
    public List<History> history(@PathVariable Long mandateId) { return service.history(mandateId); }

    @PostMapping
    public ResponseEntity<AdminRentalMandateResponse.Item> create(@Valid @RequestBody AdminRentalMandateCreateRequest body,
            HttpServletRequest request) {
        return ResponseEntity.ok(service.create(AuthInterceptor.userId(request), body));
    }

    @PostMapping("/{mandateId}/submit")
    public AdminRentalMandateResponse.Item submit(@PathVariable Long mandateId, HttpServletRequest request) {
        return service.submit(AuthInterceptor.userId(request), mandateId);
    }

    @PostMapping("/{mandateId}/review")
    public AdminRentalMandateResponse.Item review(@PathVariable Long mandateId,
            @Valid @RequestBody AdminRentalMandateReviewRequest body, HttpServletRequest request) {
        return service.review(AuthInterceptor.userId(request), mandateId, body);
    }

    @PutMapping("/{mandateId}/status")
    public AdminRentalMandateResponse.Item status(@PathVariable Long mandateId,
            @Valid @RequestBody AdminRentalMandateStatusRequest body, HttpServletRequest request) {
        return service.updateStatus(AuthInterceptor.userId(request), mandateId, body);
    }
}
