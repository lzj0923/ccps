package com.ccps.backend.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.service.AdminRentalMandateService;

/** HTTP routing contract only; mocked services never write business data. */
class AdminRentalMandateEditRouteTest {
    @Test
    void editShortcutDetailHasGetRouteAndUsesExactMandateId() throws Exception {
        var service = mock(AdminRentalMandateService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminRentalMandateController(service)).build();
        mvc.perform(get("/api/admin/rental-mandates/35")).andExpect(status().isOk());
        verify(service).detail(35L);
    }

    @Test
    void editFormHasPutRouteAndPreservesActorAndMandateId() throws Exception {
        var service = mock(AdminRentalMandateService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminRentalMandateController(service)).build();
        mvc.perform(put("/api/admin/rental-mandates/35")
                .requestAttr(AuthInterceptor.REQUEST_USER_ID, 7L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ownerUnitId":21,"mandateType":"management","startDate":"2026-09-01",
                     "endDate":null,"managementFee":100,"commissionPercent":0,"responsibleUserId":null}
                    """))
                .andExpect(status().isOk());
        verify(service).update(eq(7L), eq(35L), any());
    }

    @Test
    void malformedRecordIdIsRejectedBeforeCallingService() throws Exception {
        var service = mock(AdminRentalMandateService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminRentalMandateController(service)).build();
        mvc.perform(get("/api/admin/rental-mandates/undefined")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
