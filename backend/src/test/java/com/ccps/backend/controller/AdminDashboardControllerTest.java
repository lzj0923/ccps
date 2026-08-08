package com.ccps.backend.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.ccps.backend.dto.AdminDashboardResponse;
import com.ccps.backend.service.AdminDashboardService;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {
    @Mock private AdminDashboardService service;

    @Test
    void exposesManagementDashboardAtExpectedApiRoute() throws Exception {
        AdminDashboardResponse response = new AdminDashboardResponse(null, null);
        when(service.dashboard()).thenReturn(response);
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new AdminDashboardController(service))
                .build();

        mvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk());

        verify(service).dashboard();
    }
}
