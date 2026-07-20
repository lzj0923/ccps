package com.ccps.backend.controller;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.service.MaintenanceAttachmentService;
import com.ccps.backend.service.OwnerExpenseMaintenanceService;

@ExtendWith(MockitoExtension.class)
class OwnerExpenseMaintenanceControllerAuthTest {
    @Mock private OwnerExpenseMaintenanceService service;
    @Mock private MaintenanceAttachmentService attachmentService;
    @InjectMocks private OwnerExpenseMaintenanceController controller;

    @Test
    void readsAuthenticatedUserFromRequestAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthInterceptor.REQUEST_USER_ID, 42L);

        controller.overview(null, null, null, null, null, request);

        verify(service).getOverview(42L, null, null, null, null, null);
    }
}
