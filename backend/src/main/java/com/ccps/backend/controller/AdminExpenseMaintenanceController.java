package com.ccps.backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import com.ccps.backend.dto.MaintenanceDetailResponse;
import com.ccps.backend.dto.MaintenanceDetailResponse.Attachment;
import com.ccps.backend.dto.AdminMaintenanceCompleteRequest;
import com.ccps.backend.dto.AdminExpenseCreateRequest;
import com.ccps.backend.dto.AdminMaintenanceCreateRequest;
import com.ccps.backend.dto.AdminMaintenanceHandlingResponse;
import com.ccps.backend.dto.AdminMaintenanceOptionsResponse;
import com.ccps.backend.dto.AdminRecordCreateResponse;
import com.ccps.backend.dto.OwnerExpenseMaintenanceResponse;
import com.ccps.backend.service.AdminMaintenanceService;
import com.ccps.backend.service.MaintenanceAttachmentService;
import com.ccps.backend.service.OwnerExpenseMaintenanceService;
import com.ccps.backend.config.AuthInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/admin/expenses")
public class AdminExpenseMaintenanceController {
    private final OwnerExpenseMaintenanceService service;
    private final AdminMaintenanceService adminMaintenanceService;
    private final MaintenanceAttachmentService attachmentService;

    public AdminExpenseMaintenanceController(OwnerExpenseMaintenanceService service,
            AdminMaintenanceService adminMaintenanceService, MaintenanceAttachmentService attachmentService) {
        this.service = service;
        this.adminMaintenanceService = adminMaintenanceService;
        this.attachmentService = attachmentService;
    }

    @GetMapping
    public OwnerExpenseMaintenanceResponse overview(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return service.getOverview(null, projectId, category, status, startDate, endDate);
    }

    @GetMapping("/options")
    public AdminMaintenanceOptionsResponse options() {
        return adminMaintenanceService.options();
    }

    @PostMapping("/records")
    public AdminRecordCreateResponse createExpense(@Valid @RequestBody AdminExpenseCreateRequest body,
            HttpServletRequest request) {
        return adminMaintenanceService.createExpense(AuthInterceptor.userId(request), body);
    }

    @PostMapping("/maintenance")
    public AdminRecordCreateResponse createMaintenance(@Valid @RequestBody AdminMaintenanceCreateRequest body,
            HttpServletRequest request) {
        return adminMaintenanceService.createMaintenance(AuthInterceptor.userId(request), body);
    }

    @GetMapping("/maintenance/{workOrderId}")
    public MaintenanceDetailResponse detail(@PathVariable Long workOrderId) {
        return service.getMaintenanceDetail(null, workOrderId);
    }

    @GetMapping("/maintenance/{workOrderId}/handling")
    public AdminMaintenanceHandlingResponse handling(@PathVariable Long workOrderId) {
        return adminMaintenanceService.handling(workOrderId);
    }

    @PostMapping(value = "/maintenance/{workOrderId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Attachment> uploadPhotos(@PathVariable Long workOrderId,
            @RequestParam String relationType, @RequestParam("files") List<MultipartFile> files,
            HttpServletRequest request) {
        return attachmentService.uploadAdmin(AuthInterceptor.userId(request), workOrderId, relationType, files);
    }

    @PostMapping("/maintenance/{workOrderId}/complete")
    public MaintenanceDetailResponse complete(@PathVariable Long workOrderId,
            @Valid @RequestBody AdminMaintenanceCompleteRequest body, HttpServletRequest request) {
        return adminMaintenanceService.complete(AuthInterceptor.userId(request), workOrderId, body);
    }
}
