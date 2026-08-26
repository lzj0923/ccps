package com.ccps.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.ccps.backend.dto.LoginResponse;
import com.ccps.backend.service.PortalSessionService;

import jakarta.servlet.http.Cookie;

class PortalAuthInterceptorTest {
    private final Object handler = new Object();
    private final PortalSessionService sessions = new PortalSessionService();

    @Test
    void adminInterceptorRejectsMissingLoginWithUnauthorized() throws Exception {
        MockHttpServletRequest request = request("/api/admin/reserve");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = new AdminAuthInterceptor(sessions).preHandle(request, response, handler);

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Authentication required");
    }

    @Test
    void adminInterceptorDoesNotAcceptOwnerCookie() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("/api/admin/reserve", "owner");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = new AdminAuthInterceptor(sessions).preHandle(request, response, handler);

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void adminInterceptorAllowsAdminCookie() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("/api/admin/reserve", "admin");

        assertThat(new AdminAuthInterceptor(sessions).preHandle(request, new MockHttpServletResponse(), handler))
                .isTrue();
        assertThat(AuthInterceptor.userId(request)).isEqualTo(7L);
    }

    @Test
    void ownerInterceptorDoesNotAcceptAdminCookie() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("/api/owner/dashboard", "admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = new OwnerAuthInterceptor(sessions).preHandle(request, response, handler);

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void ownerInterceptorAllowsOwnerCookie() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("/api/owner/dashboard", "owner");

        assertThat(new OwnerAuthInterceptor(sessions).preHandle(request, new MockHttpServletResponse(), handler))
                .isTrue();
        assertThat(AuthInterceptor.userId(request)).isEqualTo(7L);
    }

    @Test
    void preflightRequestIsAlwaysAllowed() throws Exception {
        MockHttpServletRequest request = request("/api/admin/reserve");
        request.setMethod("OPTIONS");

        assertThat(new AdminAuthInterceptor(sessions)
                .preHandle(request, new MockHttpServletResponse(), handler)).isTrue();
    }

    private MockHttpServletRequest authenticatedRequest(String uri, String portal) {
        MockHttpServletRequest loginRequest = new MockHttpServletRequest();
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        String role = "admin".equals(portal) ? "ADMIN" : "OWNER";
        sessions.create(portal, new LoginResponse(7L, portal, portal + "@example.com", portal,
                "ACTIVE", role, List.of(role), List.of()), false, loginRequest, loginResponse);

        String cookieName = "admin".equals(portal)
                ? PortalSessionService.ADMIN_COOKIE : PortalSessionService.OWNER_COOKIE;
        String header = loginResponse.getHeaders("Set-Cookie").stream()
                .filter(value -> value.startsWith(cookieName + "="))
                .findFirst().orElseThrow();
        String value = header.substring(cookieName.length() + 1, header.indexOf(';'));
        MockHttpServletRequest request = request(uri);
        request.setCookies(new Cookie(cookieName, value));
        return request;
    }

    private MockHttpServletRequest request(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }
}
