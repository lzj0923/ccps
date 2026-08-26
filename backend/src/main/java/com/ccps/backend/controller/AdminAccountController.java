package com.ccps.backend.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
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
import com.ccps.backend.service.AdminPermissionService;
import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.security.AdminPermissionCodes;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {
    private final AdminAccountService service;
    private final AdminPermissionService permissionService;

    public AdminAccountController(AdminAccountService service, AdminPermissionService permissionService) {
        this.service = service;
        this.permissionService = permissionService;
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
    public ResponseEntity<AdminAccountResponse> create(@Valid @RequestBody AdminAccountCreateRequest request,
            HttpServletRequest servletRequest) {
        Long actorId = AuthInterceptor.userId(servletRequest);
        requireSuperAdminWhenNeeded(actorId, request.staffRole(), null);
        AdminAccountResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/admin/accounts/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public AdminAccountResponse update(@PathVariable Long id, @Valid @RequestBody AdminAccountUpdateRequest request,
            HttpServletRequest servletRequest) {
        Long actorId = AuthInterceptor.userId(servletRequest);
        AdminAccountResponse current = service.findById(id);
        requireSuperAdminWhenNeeded(actorId, request.staffRole(), current.staffRole());
        if (actorId.equals(id) && ("inactive".equals(request.status())
                || (request.staffRole() != null && !request.staffRole().equals(current.staffRole())))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot deactivate or change your own administrator role");
        }
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest servletRequest) {
        Long actorId = AuthInterceptor.userId(servletRequest);
        if (actorId.equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot deactivate your own account");
        }
        AdminAccountResponse current = service.findById(id);
        requireSuperAdminWhenNeeded(actorId, null, current.staffRole());
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void requireSuperAdminWhenNeeded(Long actorId, String requestedRole, String currentRole) {
        boolean touchesSuperAdmin = AdminPermissionCodes.SUPER_ADMIN.equals(requestedRole)
                || AdminPermissionCodes.SUPER_ADMIN.equals(currentRole);
        if (touchesSuperAdmin && !permissionService.isSuperAdmin(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only a super administrator can manage super administrator accounts");
        }
    }
}
