package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminAccountCreateRequest;
import com.ccps.backend.mapper.AdminAccountMapper;
import com.ccps.backend.mapper.AdminAccountMapper.AccountRecord;

@ExtendWith(MockitoExtension.class)
class AdminAccountStaffProfileServiceTest {
    @Mock private AdminAccountMapper mapper;

    @Test
    void rejectsLeaveDateBeforeHireDate() {
        when(mapper.insert(any(AccountRecord.class))).thenAnswer(invocation -> {
            invocation.<AccountRecord>getArgument(0).setId(20L); return 1;
        });
        when(mapper.insertRole(anyLong(), anyString())).thenReturn(1);
        AdminAccountService service = new AdminAccountService(mapper);
        AdminAccountCreateRequest request = new AdminAccountCreateRequest(
                "staff.one", "123456", "员工一", "staff.one@example.com", null,
                "ADMIN", "ADMINISTRATION", "active", "EMP-001", "行政部", "专员",
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1), "left");

        assertThatThrownBy(() -> service.create(9L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Leave date");
    }
}
