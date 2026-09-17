package com.ccps.backend.service;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import jakarta.servlet.http.Cookie;
import com.ccps.backend.dto.LoginResponse;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PortalSessionServiceTest {
    @Test void passwordRevocationOnlyRemovesThisOwnersSessions() {
        PortalSessionService service = new PortalSessionService();
        var first = login(service, "owner", 7L); var second = login(service, "owner", 7L);
        var other = login(service, "owner", 8L); var admin = login(service, "admin", 7L);
        assertTrue(service.currentUser(first, "owner").isPresent());
        service.revokeOwnerSessions(7L);
        assertTrue(service.currentUser(first, "owner").isEmpty());
        assertTrue(service.currentUser(second, "owner").isEmpty());
        assertTrue(service.currentUser(other, "owner").isPresent());
        assertTrue(service.currentUser(admin, "admin").isPresent());
    }
    MockHttpServletRequest login(PortalSessionService service, String portal, Long userId) {
        var request = new MockHttpServletRequest(); var response = new MockHttpServletResponse();
        service.create(portal, new LoginResponse(userId, "test", "", "Test", "active", portal.toUpperCase(), List.of(), List.of()), false, request, response);
        String cookie = response.getHeader("Set-Cookie").split(";", 2)[0];
        String[] pair = cookie.split("=", 2); request.setCookies(new Cookie(pair[0], pair[1])); return request;
    }
}
