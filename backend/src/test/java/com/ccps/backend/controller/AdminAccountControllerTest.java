package com.ccps.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.AdminAccountCreateRequest;
import com.ccps.backend.service.AdminAccountService;
import com.ccps.backend.service.AdminPermissionService;

class AdminAccountControllerTest {
    private final AdminAccountService accountService = mock(AdminAccountService.class);
    private final AdminPermissionService permissionService = mock(AdminPermissionService.class);
    private final AdminAccountController controller = new AdminAccountController(accountService, permissionService);

    @Test
    void nonSuperAdminCannotCreateSuperAdmin() {
        MockHttpServletRequest servletRequest = requestFor(8L);
        when(permissionService.isSuperAdmin(8L)).thenReturn(false);
        AdminAccountCreateRequest request = new AdminAccountCreateRequest(
                "another.admin", "123456", "Another admin", null, null,
                "ADMIN", "SUPER_ADMIN", "active");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.create(request, servletRequest));

        assertEquals(403, error.getStatusCode().value());
        verify(accountService, never()).create(request);
    }

    @Test
    void administratorCannotDeactivateOwnAccount() {
        MockHttpServletRequest servletRequest = requestFor(8L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.delete(8L, servletRequest));

        assertEquals(400, error.getStatusCode().value());
        verify(accountService, never()).delete(8L);
    }

    private MockHttpServletRequest requestFor(Long userId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthInterceptor.REQUEST_USER_ID, userId);
        return request;
    }
}
