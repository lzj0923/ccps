package com.ccps.backend.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.dto.OwnerAccountResponse;
import com.ccps.backend.service.OwnerAccountService;

class OwnerAccountControllerTest {
    @Test void accountGetRouteExistsAndUsesAuthenticatedOwner() throws Exception {
        var service=mock(OwnerAccountService.class);
        when(service.get(42L)).thenReturn(new OwnerAccountResponse("Test Owner","","","",""));
        var mvc=MockMvcBuilders.standaloneSetup(new OwnerAccountController(service)).build();
        mvc.perform(get("/api/owner/account").requestAttr(AuthInterceptor.REQUEST_USER_ID,42L))
                .andExpect(status().isOk()).andExpect(jsonPath("fullName").value("Test Owner"));
        verify(service).get(42L);
    }
    @Test void missingIdentityCannotReadAccount() throws Exception {
        var service=mock(OwnerAccountService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new OwnerAccountController(service)).build();
        mvc.perform(get("/api/owner/account")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
