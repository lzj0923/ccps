package com.ccps.backend.config;

import java.io.IOException;

import com.ccps.backend.service.AuthService;
import com.ccps.backend.service.AdminPermissionService;
import com.ccps.backend.service.PortalSessionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AdminAuthInterceptor extends AuthInterceptor {
    private final AdminPermissionService permissionService;

    public AdminAuthInterceptor(PortalSessionService portalSessionService) {
        this(portalSessionService, null);
    }

    public AdminAuthInterceptor(PortalSessionService portalSessionService, AdminPermissionService permissionService) {
        super(portalSessionService, AuthService.ADMIN_ROLE);
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!super.preHandle(request, response, handler)) return false;
        if (permissionService == null || "OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        if (permissionService.isAllowed(userId(request), request.getMethod(), request.getRequestURI())) return true;
        writeError(response, HttpServletResponse.SC_FORBIDDEN, "Permission denied for this administrator role");
        return false;
    }
}
