package com.ccps.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.LoginRequest;
import com.ccps.backend.dto.LoginResponse;
import com.ccps.backend.service.AuthService;
import com.ccps.backend.service.PortalSessionService;

import jakarta.servlet.http.Cookie;

class AuthControllerPortalIsolationTest {

    @Test
    void adminAndOwnerLoginsUseIndependentCookiesAndLogoutStates() {
        AuthService authService = mock(AuthService.class);
        LoginRequest adminRequest = new LoginRequest("admin", "123456", true);
        LoginRequest ownerRequest = new LoginRequest("owner", "123456", true);
        LoginResponse admin = user(1L, "admin", "ADMIN");
        LoginResponse owner = user(2L, "owner", "OWNER");
        when(authService.login(adminRequest, AuthService.ADMIN_ROLE)).thenReturn(admin);
        when(authService.login(ownerRequest, AuthService.OWNER_ROLE)).thenReturn(owner);

        AuthController controller = new AuthController(authService, new PortalSessionService());
        MockHttpServletResponse adminLoginResponse = new MockHttpServletResponse();
        controller.adminLogin(adminRequest, new MockHttpServletRequest(), adminLoginResponse);
        Cookie adminCookie = cookie(adminLoginResponse, PortalSessionService.ADMIN_COOKIE);

        MockHttpServletRequest ownerLoginRequest = new MockHttpServletRequest();
        ownerLoginRequest.setCookies(adminCookie);
        MockHttpServletResponse ownerLoginResponse = new MockHttpServletResponse();
        controller.ownerLogin(ownerRequest, ownerLoginRequest, ownerLoginResponse);
        Cookie ownerCookie = cookie(ownerLoginResponse, PortalSessionService.OWNER_COOKIE);

        MockHttpServletRequest bothLoggedIn = new MockHttpServletRequest();
        bothLoggedIn.setCookies(adminCookie, ownerCookie);
        assertThat(controller.currentAdminSession(bothLoggedIn).role()).isEqualTo("ADMIN");
        assertThat(controller.currentOwnerSession(bothLoggedIn).role()).isEqualTo("OWNER");

        controller.ownerLogout(bothLoggedIn, new MockHttpServletResponse());

        assertThat(controller.currentAdminSession(bothLoggedIn).role()).isEqualTo("ADMIN");
        assertThatThrownBy(() -> controller.currentOwnerSession(bothLoggedIn))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    private LoginResponse user(Long id, String username, String role) {
        return new LoginResponse(id, username, username + "@example.com", username, "ACTIVE", role,
                List.of(role));
    }

    private Cookie cookie(MockHttpServletResponse response, String name) {
        String header = response.getHeaders("Set-Cookie").stream()
                .filter(value -> value.startsWith(name + "="))
                .findFirst()
                .orElseThrow();
        String value = header.substring(name.length() + 1, header.indexOf(';'));
        return new Cookie(name, value);
    }
}
