package com.ccps.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminMaintenanceCreateRequest;
import com.ccps.backend.dto.AdminPropertyMaintenanceRecordRequest;
import com.ccps.backend.dto.AdminPropertyMaintenanceRecordResponse;
import com.ccps.backend.dto.AdminPropertyMaintenanceResponse;
import com.ccps.backend.dto.AdminPropertyMaintenanceUpdateRequest;
import com.ccps.backend.service.AdminMaintenanceService;
import com.ccps.backend.service.AdminPropertyMaintenanceRecordService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/owners/{ownerId}/properties/{ownerUnitId}")
public class AdminPropertyMaintenanceController {
    private final AdminMaintenanceService repairService;
    private final AdminPropertyMaintenanceRecordService recordService;

    public AdminPropertyMaintenanceController(AdminMaintenanceService repairService,
            AdminPropertyMaintenanceRecordService recordService) {
        this.repairService = repairService;
        this.recordService = recordService;
    }

    @GetMapping("/maintenance-records")
    public List<AdminPropertyMaintenanceRecordResponse> records(@PathVariable Long ownerId,
            @PathVariable Long ownerUnitId) {
        return recordService.list(ownerId, ownerUnitId);
    }

    @PostMapping("/maintenance-records")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminPropertyMaintenanceRecordResponse createRecord(@PathVariable Long ownerId,
            @PathVariable Long ownerUnitId, @Valid @RequestBody AdminPropertyMaintenanceRecordRequest body,
            HttpServletRequest request) {
        return recordService.create(AuthInterceptor.userId(request), ownerId, ownerUnitId, body);
    }

    @PutMapping("/maintenance-records/{recordId}")
    public AdminPropertyMaintenanceRecordResponse updateRecord(@PathVariable Long ownerId,
            @PathVariable Long ownerUnitId, @PathVariable Long recordId,
            @Valid @RequestBody AdminPropertyMaintenanceRecordRequest body, HttpServletRequest request) {
        return recordService.update(AuthInterceptor.userId(request), ownerId, ownerUnitId, recordId, body);
    }

    @DeleteMapping("/maintenance-records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecord(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @PathVariable Long recordId, HttpServletRequest request) {
        recordService.delete(AuthInterceptor.userId(request), ownerId, ownerUnitId, recordId);
    }

    @GetMapping("/repair-reports")
    public List<AdminPropertyMaintenanceResponse> repairReports(@PathVariable Long ownerId,
            @PathVariable Long ownerUnitId) {
        return repairService.listForProperty(ownerId, ownerUnitId);
    }

    @PostMapping("/repair-reports")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminPropertyMaintenanceResponse createRepairReport(@PathVariable Long ownerId,
            @PathVariable Long ownerUnitId, @Valid @RequestBody AdminMaintenanceCreateRequest body,
            HttpServletRequest request) {
        return repairService.createForProperty(AuthInterceptor.userId(request), ownerId, ownerUnitId, body);
    }

    @PutMapping("/repair-reports/{workOrderId}")
    public AdminPropertyMaintenanceResponse updateRepairReport(@PathVariable Long ownerId,
            @PathVariable Long ownerUnitId, @PathVariable Long workOrderId,
            @Valid @RequestBody AdminPropertyMaintenanceUpdateRequest body, HttpServletRequest request) {
        return repairService.updateForProperty(AuthInterceptor.userId(request), ownerId, ownerUnitId, workOrderId, body);
    }

    @DeleteMapping("/repair-reports/{workOrderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRepairReport(@PathVariable Long ownerId, @PathVariable Long ownerUnitId,
            @PathVariable Long workOrderId, HttpServletRequest request) {
        repairService.cancelForProperty(AuthInterceptor.userId(request), ownerId, ownerUnitId, workOrderId);
    }
}
