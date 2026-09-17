package com.ccps.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminFundOperationsRequest.GenerateRemittanceBatch;
import com.ccps.backend.dto.AdminFundOperationsRequest.InternalTransfer;
import com.ccps.backend.dto.AdminFundOperationsRequest.RemittanceSetting;
import com.ccps.backend.dto.AdminFundOperationsRequest.Review;
import com.ccps.backend.dto.AdminFundOperationsResponse;
import com.ccps.backend.service.AdminFundOperationsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/reserve/fund-operations")
public class AdminFundOperationsController {
    private final AdminFundOperationsService service;

    public AdminFundOperationsController(AdminFundOperationsService service) {
        this.service = service;
    }

    @GetMapping
    public AdminFundOperationsResponse overview() {
        return service.overview();
    }

    @PostMapping("/internal-transfers")
    public AdminFundOperationsResponse createTransfer(@Valid @RequestBody InternalTransfer request,
            HttpServletRequest servletRequest) {
        return service.createTransfer(AuthInterceptor.userId(servletRequest), request);
    }

    @PostMapping("/internal-transfers/{id}/approve")
    public AdminFundOperationsResponse approveTransfer(@PathVariable Long id,
            @RequestBody(required = false) Review request, HttpServletRequest servletRequest) {
        return service.approveTransfer(AuthInterceptor.userId(servletRequest), id, safe(request));
    }

    @PostMapping("/internal-transfers/{id}/reject")
    public AdminFundOperationsResponse rejectTransfer(@PathVariable Long id,
            @RequestBody(required = false) Review request, HttpServletRequest servletRequest) {
        return service.rejectTransfer(AuthInterceptor.userId(servletRequest), id, safe(request));
    }

    @PostMapping("/internal-transfers/{id}/reverse")
    public AdminFundOperationsResponse reverseTransfer(@PathVariable Long id,
            @RequestBody(required = false) Review request, HttpServletRequest servletRequest) {
        return service.reverseTransfer(AuthInterceptor.userId(servletRequest), id, safe(request));
    }

    @PutMapping("/remittance-settings/{accountId}")
    public AdminFundOperationsResponse saveRemittanceSetting(@PathVariable Long accountId,
            @Valid @RequestBody RemittanceSetting request, HttpServletRequest servletRequest) {
        return service.saveRemittanceSetting(AuthInterceptor.userId(servletRequest), accountId, request);
    }

    @PostMapping("/remittance-batches/generate")
    public AdminFundOperationsResponse generateRemittanceBatch(
            @Valid @RequestBody GenerateRemittanceBatch request, HttpServletRequest servletRequest) {
        return service.generateRemittanceBatch(AuthInterceptor.userId(servletRequest), request);
    }

    @PostMapping("/remittance-batches/{id}/submit")
    public AdminFundOperationsResponse submitRemittanceBatch(@PathVariable Long id,
            @RequestBody(required = false) Review request, HttpServletRequest servletRequest) {
        return service.submitRemittanceBatch(AuthInterceptor.userId(servletRequest), id, safe(request));
    }

    @PostMapping("/remittance-batches/{id}/reject")
    public AdminFundOperationsResponse rejectRemittanceBatch(@PathVariable Long id,
            @RequestBody(required = false) Review request, HttpServletRequest servletRequest) {
        return service.rejectRemittanceBatch(AuthInterceptor.userId(servletRequest), id, safe(request));
    }

    private Review safe(Review request) {
        return request == null ? new Review(null) : request;
    }
}
