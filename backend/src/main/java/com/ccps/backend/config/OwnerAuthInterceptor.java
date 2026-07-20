package com.ccps.backend.config;

import com.ccps.backend.service.AuthService;
import com.ccps.backend.service.PortalSessionService;

public class OwnerAuthInterceptor extends AuthInterceptor {
    public OwnerAuthInterceptor(PortalSessionService portalSessionService) {
        super(portalSessionService, AuthService.OWNER_ROLE);
    }
}
