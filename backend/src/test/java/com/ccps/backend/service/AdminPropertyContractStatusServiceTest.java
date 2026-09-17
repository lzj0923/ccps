package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.mapper.AdminPropertyContractRecordMapper;
import com.ccps.backend.mapper.AdminPropertyContractRecordMapper.Row;

@ExtendWith(MockitoExtension.class)
class AdminPropertyContractStatusServiceTest {
    @Mock private AdminPropertyContractRecordMapper mapper;
    @TempDir Path storage;

    @Test
    void lostOrVoidedStatusRequiresReason() {
        when(mapper.ownsProperty(1L, 2L)).thenReturn(1);
        AdminPropertyContractRecordService service = new AdminPropertyContractRecordService(mapper, storage.toString());

        assertThatThrownBy(() -> service.create(9L, 1L, 2L, null, "B_AGENCY", "C-001",
                null, null, null, "voided", null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("require a reason");
    }

    @Test
    void nonDraftContractCannotBePhysicallyDeleted() {
        when(mapper.ownsProperty(1L, 2L)).thenReturn(1);
        Row row = new Row(); row.setId(3L); row.setStatus("archived"); row.setStorageKey("archive.pdf");
        when(mapper.find(2L, 3L)).thenReturn(row);
        AdminPropertyContractRecordService service = new AdminPropertyContractRecordService(mapper, storage.toString());

        assertThatThrownBy(() -> service.delete(9L, 1L, 2L, 3L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only draft contracts");
    }
}
