package com.ccps.backend.controller;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.service.OwnerPropertyInformationService;

@ExtendWith(MockitoExtension.class)
class OwnerPropertyInformationControllerAuthTest {
    @Mock private OwnerPropertyInformationService service;
    @InjectMocks private OwnerPropertyInformationController controller;

    @Test
    void scopesInformationToAuthenticatedOwnerUser() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(AuthInterceptor.REQUEST_USER_ID, 42L);

        controller.list(7L, request);

        verify(service).list(42L, 7L);
    }
}
