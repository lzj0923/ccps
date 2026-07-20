package com.ccps.backend.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.dto.AdminBuildingPaymentResponse;
import com.ccps.backend.dto.AdminPaymentContractOption;
import com.ccps.backend.dto.AdminInstallmentUpdateRequest;
import com.ccps.backend.dto.AdminPaymentPlanCreateRequest;
import com.ccps.backend.dto.AdminPaymentPlanCreateResponse;
import com.ccps.backend.dto.AdminProjectCreateRequest;
import com.ccps.backend.dto.AdminProjectOption;
import com.ccps.backend.service.AdminBuildingPaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/buildings")
public class AdminBuildingPaymentController {
    private final AdminBuildingPaymentService service;

    public AdminBuildingPaymentController(AdminBuildingPaymentService service) {
        this.service = service;
    }

    @GetMapping("/payment-progress")
    public AdminBuildingPaymentResponse findPaymentProgress(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) String status) {
        return service.findPaymentProgress(page, pageSize, keyword, projectName, status);
    }

    @PostMapping("/projects")
    public ResponseEntity<AdminProjectOption> createProject(@Valid @RequestBody AdminProjectCreateRequest request) {
        AdminProjectOption response = service.createProject(request);
        return ResponseEntity.created(URI.create("/api/admin/buildings/projects/" + response.id())).body(response);
    }

    @GetMapping("/payment-contracts")
    public List<AdminPaymentContractOption> findPaymentContracts() {
        return service.findPaymentContracts();
    }

    @PostMapping("/payment-plans")
    public ResponseEntity<AdminPaymentPlanCreateResponse> createPaymentPlan(
            @Valid @RequestBody AdminPaymentPlanCreateRequest request) {
        AdminPaymentPlanCreateResponse response = service.createPaymentPlan(request);
        return ResponseEntity.created(URI.create("/api/admin/buildings/payment-plans/" + response.id())).body(response);
    }

    @PutMapping("/installments/{installmentId}")
    public ResponseEntity<Void> updateInstallment(@PathVariable Long installmentId,
                                                   @Valid @RequestBody AdminInstallmentUpdateRequest request) {
        service.updateInstallment(installmentId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/installments/{installmentId}/reminders")
    public ResponseEntity<Void> sendPaymentReminder(@PathVariable Long installmentId) {
        service.sendPaymentReminder(installmentId);
        return ResponseEntity.noContent().build();
    }
}
