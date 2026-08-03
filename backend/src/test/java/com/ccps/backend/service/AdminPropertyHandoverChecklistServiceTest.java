package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.mapper.AdminPropertyHandoverChecklistMapper;
import com.ccps.backend.mapper.AdminPropertyHandoverChecklistMapper.Row;

@ExtendWith(MockitoExtension.class)
class AdminPropertyHandoverChecklistServiceTest {
    @Mock private AdminPropertyHandoverChecklistMapper mapper;

    @Test
    void firstListSeedsStandardHandoverItemsWithoutExistingReport() {
        when(mapper.ownsProperty(7L, 5L)).thenReturn(1);
        when(mapper.list(5L)).thenReturn(List.of());
        when(mapper.insert(any(Row.class))).thenAnswer(invocation -> { Row row = invocation.getArgument(0); row.setId((long) (100 + row.getSortOrder())); return 1; });
        AdminPropertyHandoverChecklistService service = new AdminPropertyHandoverChecklistService(mapper);

        var result = service.list(7L, 5L);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).category()).isEqualTo("鑰匙 Keys");
        assertThat(result).anyMatch(item -> item.itemName().contains("Main Door Entrance Key"));
        verify(mapper, times(result.size())).insert(any(Row.class));
    }

    @Test
    void completedReportSyncUpdatesQuantityAndDisablesRemovedItems() {
        Row old = row(10L, 5L, "廚房 Kitchen", "冰箱 / Refrigerator", "1", 0, true);
        Row removed = row(11L, 5L, "廚房 Kitchen", "微波爐 / Microwave", "1", 1, true);
        when(mapper.list(5L)).thenReturn(List.of(old, removed));
        AdminPropertyHandoverChecklistService service = new AdminPropertyHandoverChecklistService(mapper);

        service.syncCompletedReport(5L, List.of(
                new AdminPropertyHandoverChecklistService.SyncItem("廚房 Kitchen", "冰箱 / Refrigerator", "2")));

        assertThat(old.getDefaultQuantity()).isEqualTo("2");
        assertThat(old.isEnabled()).isTrue();
        assertThat(removed.isEnabled()).isFalse();
        verify(mapper).update(old);
        verify(mapper).update(removed);
    }

    @Test
    void completedReportSyncCreatesNewItems() {
        when(mapper.list(5L)).thenReturn(List.of());
        when(mapper.insert(any(Row.class))).thenAnswer(invocation -> { invocation.<Row>getArgument(0).setId(20L); return 1; });
        AdminPropertyHandoverChecklistService service = new AdminPropertyHandoverChecklistService(mapper);

        service.syncCompletedReport(5L, List.of(new AdminPropertyHandoverChecklistService.SyncItem("客廳 Living Room", "沙發 / Sofa", "1")));

        verify(mapper).insert(any(Row.class));
    }

    private Row row(Long id, Long ownerUnitId, String category, String name, String quantity, int sortOrder, boolean enabled) {
        Row row = new Row(); row.setId(id); row.setOwnerUnitId(ownerUnitId); row.setCategory(category); row.setItemName(name); row.setDefaultQuantity(quantity); row.setSortOrder(sortOrder); row.setEnabled(enabled); return row;
    }
}
