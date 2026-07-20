package com.ccps.backend.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ccps.backend.dto.LoginRequest;
import com.ccps.backend.dto.LoginResponse;
import com.ccps.backend.service.AuthService;
import com.ccps.backend.service.PortalSessionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final PortalSessionService portalSessionService;

    public AuthController(AuthService authService, PortalSessionService portalSessionService) {
        this.authService = authService;
        this.portalSessionService = portalSessionService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        LoginResponse user = authService.login(request);
        String portal = user.role().toUpperCase().contains(AuthService.ADMIN_ROLE)
                ? PortalSessionService.ADMIN_PORTAL : PortalSessionService.OWNER_PORTAL;
        portalSessionService.create(portal, user, request.rememberMe(), servletRequest, servletResponse);
        return user;
    }

    @PostMapping("/admin/login")
    public LoginResponse adminLogin(@Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        return login(request, servletRequest, servletResponse, AuthService.ADMIN_ROLE,
                PortalSessionService.ADMIN_PORTAL);
    }

    @PostMapping("/owner/login")
    public LoginResponse ownerLogin(@Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        return login(request, servletRequest, servletResponse, AuthService.OWNER_ROLE,
                PortalSessionService.OWNER_PORTAL);
    }

    private LoginResponse login(LoginRequest request, HttpServletRequest servletRequest,
            HttpServletResponse servletResponse, String requiredRole, String portal) {
        LoginResponse response = authService.login(request, requiredRole);
        portalSessionService.create(portal, response, request.rememberMe(), servletRequest, servletResponse);
        return response;
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        portalSessionService.logoutAll(request, response);
    }

    @GetMapping("/session")
    public LoginResponse currentSession(HttpServletRequest request) {
        return portalSessionService.currentUser(request, PortalSessionService.ADMIN_PORTAL)
                .or(() -> portalSessionService.currentUser(request, PortalSessionService.OWNER_PORTAL))
                .orElseThrow(AuthController::unauthorized);
    }

    @PostMapping("/admin/logout")
    public void adminLogout(HttpServletRequest request, HttpServletResponse response) {
        portalSessionService.logout(PortalSessionService.ADMIN_PORTAL, request, response);
    }

    @PostMapping("/owner/logout")
    public void ownerLogout(HttpServletRequest request, HttpServletResponse response) {
        portalSessionService.logout(PortalSessionService.OWNER_PORTAL, request, response);
    }

    @GetMapping("/admin/session")
    public LoginResponse currentAdminSession(HttpServletRequest request) {
        return portalSessionService.currentUser(request, PortalSessionService.ADMIN_PORTAL)
                .orElseThrow(AuthController::unauthorized);
    }

    @GetMapping("/owner/session")
    public LoginResponse currentOwnerSession(HttpServletRequest request) {
        return portalSessionService.currentUser(request, PortalSessionService.OWNER_PORTAL)
                .orElseThrow(AuthController::unauthorized);
    }

    private static org.springframework.web.server.ResponseStatusException unauthorized() {
        return new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
