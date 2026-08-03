package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.dto.AdminAuditResponse;
import com.ccps.backend.mapper.AdminAuditMapper;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceTest {
    @Mock private AdminAuditMapper mapper;
    private AdminAuditService service;

    @BeforeEach
    void setUp() { service = new AdminAuditService(mapper); }

    @Test
    void filtersAuditEventsByStaffActionAndDate() {
        AdminAuditResponse.Item item = new AdminAuditResponse.Item(8L, 3L, "文員 A", "update_lease",
                "lease", 44L, "{\"startDate\":\"2026-05-01\"}", "{\"startDate\":\"2026-07-01\"}",
                "127.0.0.1", LocalDateTime.parse("2026-07-23T14:20:00"));
        when(mapper.count("44", "update_lease", 3L, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-31"))).thenReturn(1L);
        when(mapper.find("44", "update_lease", 3L, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-31"), 20, 0)).thenReturn(List.of(item));

        AdminAuditResponse result = service.list(1, 20, "44", "update_lease", 3L,
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-31"));

        assertThat(result.items()).containsExactly(item);
        assertThat(result.page().total()).isEqualTo(1L);
        verify(mapper).find("44", "update_lease", 3L, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-31"), 20, 0);
    }
}
