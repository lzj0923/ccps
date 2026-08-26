package com.ccps.backend.config;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import com.ccps.backend.dto.LoginResponse;
import com.ccps.backend.service.AuthService;
import com.ccps.backend.service.PortalSessionService;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AuthInterceptor implements HandlerInterceptor {
    public static final String REQUEST_USER_ID = "CCPS_AUTH_USER_ID";
    public static final String REQUEST_USER = "CCPS_AUTH_USER";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PortalSessionService portalSessionService;
    private final String requiredRole;

    public AuthInterceptor(PortalSessionService portalSessionService) {
        this(portalSessionService, null);
    }

    protected AuthInterceptor(PortalSessionService portalSessionService, String requiredRole) {
        this.portalSessionService = portalSessionService;
        this.requiredRole = requiredRole;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        LoginResponse user = authenticatedUser(request);
        if (user == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return false;
        }
        request.setAttribute(REQUEST_USER_ID, user.id());
        request.setAttribute(REQUEST_USER, user);
        return true;
    }

    private LoginResponse authenticatedUser(HttpServletRequest request) {
        if (AuthService.ADMIN_ROLE.equals(requiredRole)) {
            return portalSessionService.currentUser(request, PortalSessionService.ADMIN_PORTAL).orElse(null);
        }
        if (AuthService.OWNER_ROLE.equals(requiredRole)) {
            return portalSessionService.currentUser(request, PortalSessionService.OWNER_PORTAL).orElse(null);
        }
        return portalSessionService.currentUser(request, PortalSessionService.ADMIN_PORTAL)
                .or(() -> portalSessionService.currentUser(request, PortalSessionService.OWNER_PORTAL))
                .orElse(null);
    }

    public static Long userId(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_USER_ID);
        if (value instanceof Long userId) return userId;
        throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Authentication required");
    }

    protected void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "status", status,
                "message", message));
    }
}
