package com.ccps.backend.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.mapper.AdminDashboardMapper;

@ExtendWith(MockitoExtension.class)
class AdminDashboardDateRangeServiceTest {
    @Mock
    private AdminDashboardMapper mapper;

    @Test
    void dashboardPassesSelectedDateRangeToMapper() {
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        when(mapper.findRegionsByDateRange(startDate, endDate)).thenReturn(List.of());

        new AdminDashboardService(mapper).dashboard(startDate, endDate);

        verify(mapper).findRegionsByDateRange(startDate, endDate);
    }
}
