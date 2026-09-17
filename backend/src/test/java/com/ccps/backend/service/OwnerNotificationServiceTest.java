package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.OwnerNotificationResponse;
import com.ccps.backend.mapper.OwnerNotificationMapper;
import com.ccps.backend.mapper.OwnerNotificationMapper.NotificationRow;

@ExtendWith(MockitoExtension.class)
class OwnerNotificationServiceTest {
    @Mock private OwnerNotificationMapper mapper;
    private OwnerNotificationService service;

    @BeforeEach
    void setUp() {
        service = new OwnerNotificationService(mapper);
    }

    @Test
    void returnsScopedCountsAndDerivesCategoriesFromNotificationContent() {
        NotificationRow rent = row(1L, "租金已到賬", "Central Suites B-0602 租金已完成確認。", "normal", "unread");
        NotificationRow payment = row(2L, "房款即將到期", "Central Suites B-0602 下一期房款將於近期到期。", "high", "unread");
        NotificationRow reserve = row(3L, "預備金帳戶已更新", "預備金餘額已完成同步。", "normal", "read");
        OwnerNotificationMapper.UnitReference unit = new OwnerNotificationMapper.UnitReference();
        unit.setProjectName("ADMIN Test Central Suites");
        unit.setCity("Petaling Jaya");
        unit.setUnitNo("ADMIN-B-0602");
        when(mapper.findNotifications(42L)).thenReturn(List.of(rent, payment, reserve));
        when(mapper.findOwnerUnits(42L)).thenReturn(List.of(unit));

        OwnerNotificationResponse result = service.getNotifications(42L);

        assertThat(result.summary().totalCount()).isEqualTo(3);
        assertThat(result.summary().unreadCount()).isEqualTo(2);
        assertThat(result.summary().importantCount()).isEqualTo(1);
        assertThat(result.categories()).extracting(OwnerNotificationResponse.Category::key)
                .contains("payment", "rent", "reserve");
        assertThat(result.notifications().get(0).unitNo()).isEqualTo("ADMIN-B-0602");
        assertThat(result.tasks()).hasSize(2);
    }

    @Test
    void marksOnlyNotificationsOwnedByTheCurrentUser() {
        when(mapper.markRead(42L, 7L)).thenReturn(1);
        service.markRead(42L, 7L);
        service.markAllRead(42L);
        verify(mapper).markRead(42L, 7L);
        verify(mapper).markAllRead(42L);
    }

    @Test
    void returnsOnlyNotificationCenterItemsBelongingToSelectedProperty() {
        NotificationRow selectedNotification = row(
                11L, "租金已到账", "Central Suites B-0602 租金已完成确认。", "normal", "unread");
        NotificationRow otherNotification = row(
                12L, "维修进度更新", "Harbour View A-0101 维修已完成。", "high", "read");
        OwnerNotificationMapper.UnitReference selectedProperty = unit("Central Suites", "Petaling Jaya", "B-0602");
        OwnerNotificationMapper.UnitReference otherProperty = unit("Harbour View", "Johor Bahru", "A-0101");
        when(mapper.findOwnerUnit(42L, 7L)).thenReturn(selectedProperty);
        when(mapper.findNotifications(42L)).thenReturn(List.of(selectedNotification, otherNotification));
        when(mapper.findOwnerUnits(42L)).thenReturn(List.of(selectedProperty, otherProperty));

        List<OwnerNotificationResponse.NotificationItem> result = service.getPropertyNotifications(42L, 7L);

        assertThat(result).extracting(OwnerNotificationResponse.NotificationItem::id).containsExactly(11L);
        assertThat(result.get(0).projectName()).isEqualTo("Central Suites");
        assertThat(result.get(0).unitNo()).isEqualTo("B-0602");
    }

    @Test
    void rejectsPropertyThatDoesNotBelongToCurrentOwner() {
        when(mapper.findOwnerUnit(42L, 99L)).thenReturn(null);

        assertThatThrownBy(() -> service.getPropertyNotifications(42L, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    private NotificationRow row(Long id, String title, String body, String priority, String status) {
        NotificationRow row = new NotificationRow();
        row.setId(id);
        row.setTitle(title);
        row.setBody(body);
        row.setPriority(priority);
        row.setStatus(status);
        row.setCreatedAt(LocalDateTime.now());
        return row;
    }

    @Test
    void duplicateUnitNumbersMustPreferFullProjectAndAmbiguousNoticesStayUnlinked() {
        var first = unit("团结小区", "Shah Alam", "102");
        var second = unit("翻斗花园", "KL", "102");
        when(mapper.findOwnerUnits(42L)).thenReturn(List.of(first, second));
        when(mapper.findNotifications(42L)).thenReturn(List.of(
                row(1L, "预备金提醒", "翻斗花园 102 的预备金余额不足", "high", "unread"),
                row(2L, "预备金提醒", "102 的预备金余额不足", "high", "unread"),
                row(3L, "预备金提醒", "翻斗花园 1102 的预备金余额不足", "high", "unread")));
        var result = service.getNotifications(42L).notifications();
        assertThat(result.get(0).projectName()).isEqualTo("翻斗花园");
        assertThat(result.get(1).projectName()).isNull();
        assertThat(result.get(2).projectName()).isNull();
    }

    private OwnerNotificationMapper.UnitReference unit(String projectName, String city, String unitNo) {
        OwnerNotificationMapper.UnitReference unit = new OwnerNotificationMapper.UnitReference();
        unit.setProjectName(projectName);
        unit.setCity(city);
        unit.setUnitNo(unitNo);
        return unit;
    }
}
