package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.mapper.AdminPropertyHandoverReportMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AdminPropertyHandoverReportServiceOwnerAccessTest {
    @Mock private AdminPropertyHandoverReportMapper mapper;

    @Test
    void rejectsReportsForPropertyOutsideAuthenticatedOwner(@TempDir Path tempDir) {
        AdminPropertyHandoverReportService service = service(tempDir);
        when(mapper.ownsPropertyForUser(42L, 7L)).thenReturn(0);

        assertThatThrownBy(() -> service.listForOwnerUser(42L, 7L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Property not found");
        verify(mapper, never()).list(7L);
    }

    @Test
    void listsReportsAfterOwnerAccessCheck(@TempDir Path tempDir) {
        AdminPropertyHandoverReportService service = service(tempDir);
        when(mapper.ownsPropertyForUser(42L, 7L)).thenReturn(1);
        when(mapper.list(7L)).thenReturn(List.of());

        service.listForOwnerUser(42L, 7L);

        verify(mapper).list(7L);
    }

    private AdminPropertyHandoverReportService service(Path tempDir) {
        return new AdminPropertyHandoverReportService(mapper, new ObjectMapper(), tempDir.toString());
    }
}
