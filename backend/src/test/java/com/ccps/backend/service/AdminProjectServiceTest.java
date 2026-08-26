package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminProjectCreateRequest;
import com.ccps.backend.dto.AdminProjectManagementResponse;
import com.ccps.backend.mapper.AdminProjectMapper;
import com.ccps.backend.mapper.AdminProjectMapper.ProjectRow;
import com.ccps.backend.mapper.AdminProjectMapper.ProjectWrite;
import com.ccps.backend.mapper.AdminProjectMapper.SummaryRow;

@ExtendWith(MockitoExtension.class)
class AdminProjectServiceTest {
    @Mock private AdminProjectMapper mapper;
    private AdminProjectService service;

    @BeforeEach
    void setUp() {
        service = new AdminProjectService(mapper);
    }

    @Test
    void findAllReturnsSummaryAndFiveItemPagination() {
        SummaryRow summary = new SummaryRow();
        summary.setTotalCount(8L); summary.setActiveCount(6L); summary.setInactiveCount(2L); summary.setUnitCount(24L);
        ProjectRow row = row(2L, "CCPS-02", "Central Park", 3L);
        when(mapper.countRows(null, "active")).thenReturn(6L);
        when(mapper.findRows(null, "active", 5, 5)).thenReturn(List.of(row));
        when(mapper.findSummary()).thenReturn(summary);
        when(mapper.findCities()).thenReturn(List.of("Kuala Lumpur", "Shah Alam"));

        AdminProjectManagementResponse result = service.findAll(2, 5, " ", "active");

        assertThat(result.summary().unitCount()).isEqualTo(24);
        assertThat(result.rows()).singleElement().satisfies(project -> {
            assertThat(project.projectCode()).isEqualTo("CCPS-02");
            assertThat(project.unitCount()).isEqualTo(3);
        });
        assertThat(result.page().page()).isEqualTo(2);
        assertThat(result.page().pageSize()).isEqualTo(5);
        assertThat(result.cities()).containsExactly("Kuala Lumpur", "Shah Alam");
    }

    @Test
    void createNormalizesProjectMasterData() {
        AdminProjectCreateRequest request = new AdminProjectCreateRequest(
                " ccps-01 ", " Central Park ", " Jalan Ampang ", " Kuala Lumpur ", " Bukit Bintang ", "MY", "active");
        when(mapper.insert(any())).thenAnswer(invocation -> {
            ProjectWrite write = invocation.getArgument(0); write.setId(7L); return 1;
        });
        when(mapper.findById(7L)).thenReturn(row(7L, "CCPS-01", "Central Park", 0L));

        AdminProjectManagementResponse.Project result = service.create(request);

        ArgumentCaptor<ProjectWrite> captor = ArgumentCaptor.forClass(ProjectWrite.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getProjectCode()).isEqualTo("CCPS-01");
        assertThat(captor.getValue().getName()).isEqualTo("Central Park");
        assertThat(captor.getValue().getState()).isEqualTo("Kuala Lumpur");
        assertThat(captor.getValue().getCity()).isEqualTo("Bukit Bintang");
        assertThat(result.id()).isEqualTo(7L);
    }

    @Test
    void projectCodeAvailabilityNormalizesInputBeforeCheckingDuplicates() {
        when(mapper.countProjectCode("CCPS-01", null)).thenReturn(1);

        assertThat(service.isProjectCodeAvailable(" ccps-01 ")).isFalse();
        verify(mapper).countProjectCode("CCPS-01", null);
    }

    @Test
    void deleteRejectsProjectsThatStillHaveUnits() {
        when(mapper.findById(9L)).thenReturn(row(9L, "CCPS-09", "Linked Project", 2L));
        when(mapper.countReferences(9L)).thenReturn(2);

        assertThatThrownBy(() -> service.delete(9L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(mapper, never()).delete(9L);
    }

    private ProjectRow row(Long id, String code, String name, Long unitCount) {
        ProjectRow row = new ProjectRow();
        row.setId(id); row.setProjectCode(code); row.setName(name); row.setCountryCode("MY");
        row.setStatus("active"); row.setUnitCount(unitCount); row.setOwnerCount(0L);
        return row;
    }
}
