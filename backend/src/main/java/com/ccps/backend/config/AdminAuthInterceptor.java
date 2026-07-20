package com.ccps.backend.config;

import com.ccps.backend.service.AuthService;
import com.ccps.backend.service.PortalSessionService;

public class AdminAuthInterceptor extends AuthInterceptor {
    public AdminAuthInterceptor(PortalSessionService portalSessionService) {
        super(portalSessionService, AuthService.ADMIN_ROLE);
    }
}
