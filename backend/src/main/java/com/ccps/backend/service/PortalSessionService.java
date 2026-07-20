package com.ccps.backend.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.ccps.backend.dto.LoginResponse;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class PortalSessionService {
    public static final String ADMIN_PORTAL = "admin";
    public static final String OWNER_PORTAL = "owner";
    public static final String ADMIN_COOKIE = "CCPS_ADMIN_SESSION";
    public static final String OWNER_COOKIE = "CCPS_OWNER_SESSION";

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);
    private static final Duration REMEMBERED_TIMEOUT = Duration.ofDays(7);

    private final Map<String, PortalSession> adminSessions = new ConcurrentHashMap<>();
    private final Map<String, PortalSession> ownerSessions = new ConcurrentHashMap<>();

    public void create(String portal, LoginResponse user, boolean rememberMe,
            HttpServletRequest request, HttpServletResponse response) {
        String normalizedPortal = normalizePortal(portal);
        Duration timeout = rememberMe ? REMEMBERED_TIMEOUT : DEFAULT_TIMEOUT;
        String token = UUID.randomUUID() + "." + UUID.randomUUID();
        sessions(normalizedPortal).put(token, new PortalSession(user, timeout, Instant.now().plus(timeout)));

        ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(cookieName(normalizedPortal), token)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/");
        if (rememberMe) cookie.maxAge(timeout);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
    }

    public Optional<LoginResponse> currentUser(HttpServletRequest request, String portal) {
        String normalizedPortal = normalizePortal(portal);
        String token = cookieValue(request, cookieName(normalizedPortal));
        if (token == null) return Optional.empty();

        PortalSession session = sessions(normalizedPortal).get(token);
        if (session == null) return Optional.empty();
        if (session.expiresAt().isBefore(Instant.now())) {
            sessions(normalizedPortal).remove(token);
            return Optional.empty();
        }

        sessions(normalizedPortal).put(token, session.refresh());
        return Optional.of(session.user());
    }

    public void logout(String portal, HttpServletRequest request, HttpServletResponse response) {
        String normalizedPortal = normalizePortal(portal);
        String cookieName = cookieName(normalizedPortal);
        String token = cookieValue(request, cookieName);
        if (token != null) sessions(normalizedPortal).remove(token);
        expireCookie(cookieName, request, response);
    }

    public void logoutAll(HttpServletRequest request, HttpServletResponse response) {
        logout(ADMIN_PORTAL, request, response);
        logout(OWNER_PORTAL, request, response);
    }

    private void expireCookie(String name, HttpServletRequest request, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private Map<String, PortalSession> sessions(String portal) {
        return ADMIN_PORTAL.equals(portal) ? adminSessions : ownerSessions;
    }

    private String cookieName(String portal) {
        return ADMIN_PORTAL.equals(portal) ? ADMIN_COOKIE : OWNER_COOKIE;
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private String normalizePortal(String portal) {
        if (ADMIN_PORTAL.equalsIgnoreCase(portal)) return ADMIN_PORTAL;
        if (OWNER_PORTAL.equalsIgnoreCase(portal)) return OWNER_PORTAL;
        throw new IllegalArgumentException("Unsupported portal: " + portal);
    }

    private record PortalSession(LoginResponse user, Duration timeout, Instant expiresAt) {
        private PortalSession refresh() {
            return new PortalSession(user, timeout, Instant.now().plus(timeout));
        }
    }
}
