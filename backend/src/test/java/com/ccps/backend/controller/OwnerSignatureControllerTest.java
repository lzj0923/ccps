package com.ccps.backend.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.service.OwnerSignatureService;

class OwnerSignatureControllerTest {
    @Test void adminRecipientLookupUsesThePublishedFrontendRoute() throws Exception {
        var service=mock(OwnerSignatureService.class);
        when(service.recipients(123L)).thenReturn(List.of());
        var mvc=MockMvcBuilders.standaloneSetup(new AdminOwnerSignatureController(service)).build();
        mvc.perform(get("/api/admin/e-signatures/requests/123/owner-recipients")
                .requestAttr(AuthInterceptor.REQUEST_USER_ID,7L))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).recipients(123L);
        verifyNoMoreInteractions(service);
    }
    @Test void missingSessionCannotListOrAccessSigningDocuments() throws Exception {
        var service=mock(OwnerSignatureService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new OwnerSignatureController(service)).build();
        mvc.perform(get("/api/owner/signatures")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/owner/signatures/1/file")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void ignoresSpoofedUserAndUsesServerAuthenticatedSession() throws Exception {
        var service=mock(OwnerSignatureService.class); when(service.tasks(20L)).thenReturn(List.of());
        var mvc=MockMvcBuilders.standaloneSetup(new OwnerSignatureController(service)).build();
        mvc.perform(get("/api/owner/signatures").param("userId","999").requestAttr(AuthInterceptor.REQUEST_USER_ID,20L))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).tasks(20L); verifyNoMoreInteractions(service);
    }
    @Test void missingSignatureBodyIsRejectedBeforeService() throws Exception {
        var service=mock(OwnerSignatureService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new OwnerSignatureController(service)).build();
        mvc.perform(post("/api/owner/signatures/1/sign").requestAttr(AuthInterceptor.REQUEST_USER_ID,20L)
            .contentType("application/json").content("{\"signerName\":\"Test\",\"consent\":true}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void adminDispatchRequiresExplicitRecipient() throws Exception {
        var service=mock(OwnerSignatureService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new AdminOwnerSignatureController(service)).build();
        mvc.perform(post("/api/admin/e-signatures/requests/1/owner-app").requestAttr(AuthInterceptor.REQUEST_USER_ID,7L)
            .contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/e-signatures/requests/1/owner-app").requestAttr(AuthInterceptor.REQUEST_USER_ID,7L)
            .contentType("application/json").content("{\"ownerId\":10}"))
            .andExpect(status().isNoContent());
        verify(service).dispatch(7L,1L,10L); verifyNoMoreInteractions(service);
    }
}
