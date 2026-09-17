package com.ccps.backend.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockMultipartFile;

import com.ccps.backend.config.AuthInterceptor;
import com.ccps.backend.service.AdminPropertyPhotoService;

@ExtendWith(MockitoExtension.class)
class AdminPropertyPhotoControllerTest {
    @Mock private AdminPropertyPhotoService service;

    @Test
    void exposesPhotoVersionsAsGetRouteInsteadOfTreatingVersionsAsPhotoId() throws Exception {
        when(service.versions(5L, 31L)).thenReturn(List.of());
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new AdminPropertyPhotoController(service))
                .build();

        mvc.perform(get("/api/admin/owners/5/properties/31/photos/versions"))
                .andExpect(status().isOk());

        verify(service).versions(5L, 31L);
    }

    @Test
    void acceptsPhotoUploadWithoutTitleParameter() throws Exception {
        MockMvc mvc = MockMvcBuilders
                .standaloneSetup(new AdminPropertyPhotoController(service))
                .build();
        MockMultipartFile file = new MockMultipartFile("file", "living-room.png", "image/png", new byte[] { 1 });

        mvc.perform(multipart("/api/admin/owners/5/properties/31/photos")
                        .file(file)
                        .param("category", "interior")
                        .param("versionMonth", "2026-08")
                        .requestAttr(AuthInterceptor.REQUEST_USER_ID, 9L))
                .andExpect(status().isOk());

        verify(service).create(9L, 5L, 31L, null, "interior", null, 0, false,
                null, null, "2026-08", file);
    }
}
