package com.ccps.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.dto.AdminPropertyWorkspaceResponse;
import com.ccps.backend.dto.AdminPropertyBasicSaveRequest;
import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.service.AdminPropertyWorkspaceService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminPropertyWorkspaceController {
    private final AdminPropertyWorkspaceService service;

    public AdminPropertyWorkspaceController(AdminPropertyWorkspaceService service) {
        this.service = service;
    }

    @GetMapping("/owners/{ownerId}/properties/{ownerUnitId}/workspace")
    public AdminPropertyWorkspaceResponse load(@PathVariable Long ownerId, @PathVariable Long ownerUnitId) {
        return service.load(ownerId, ownerUnitId);
    }

    @GetMapping("/properties/{unitId}/workspace")
    public AdminPropertyWorkspaceResponse loadByUnitId(@PathVariable Long unitId) {
        return service.loadByUnitId(unitId);
    }

    @PutMapping("/properties/{unitId}/workspace/basic")
    public AdminPropertyWorkspaceResponse saveBasicByUnitId(@PathVariable Long unitId,
            @Valid @RequestBody AdminPropertyBasicSaveRequest request,
            HttpServletRequest httpRequest) {
        return service.saveBasicByUnitId(unitId, request, AuthInterceptor.userId(httpRequest));
    }
}
