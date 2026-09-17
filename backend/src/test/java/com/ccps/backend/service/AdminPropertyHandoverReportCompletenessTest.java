package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.mapper.AdminPropertyHandoverReportMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AdminPropertyHandoverReportCompletenessTest {
    @Mock private AdminPropertyHandoverReportMapper mapper;
    @TempDir Path tempDir;

    @Test
    void completedReportRejectsMissingRoomTypePeopleChecklistAndPhotos() {
        when(mapper.ownsProperty(1L, 2L)).thenReturn(1);
        AdminPropertyHandoverReportService service =
                new AdminPropertyHandoverReportService(mapper, new ObjectMapper(), tempDir.toString());

        assertThatThrownBy(() -> service.create(9L, 1L, 2L, "交接报告", LocalDate.of(2026, 9, 1),
                null, null, "", "{\"sections\":[],\"photos\":[]}", true, null, null, null))
                .isInstanceOfSatisfying(ResponseStatusException.class, error -> {
                    org.assertj.core.api.Assertions.assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    org.assertj.core.api.Assertions.assertThat(error.getReason())
                            .contains("房型", "交接人", "接收人", "交接清单", "照片");
                });
    }
}
