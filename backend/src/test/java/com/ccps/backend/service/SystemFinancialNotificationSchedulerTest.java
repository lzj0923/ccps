package com.ccps.backend.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.service.SystemFinancialNotificationService.BuildingTask;

@ExtendWith(MockitoExtension.class)
class SystemFinancialNotificationSchedulerTest {
    @Mock private SystemFinancialNotificationService service;

    @Test
    void processesEveryDueItemEvenWhenOneIsSkipped() {
        when(service.dueBuildingTasks()).thenReturn(List.of(
                new BuildingTask(1L, "due_7d"), new BuildingTask(2L, "due_today")));
        when(service.dueReserveAccounts()).thenReturn(List.of(3L));
        Mockito.doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "已发送"))
                .when(service).sendBuildingNotice(1L, "due_7d");

        new SystemFinancialNotificationScheduler(service).sendDueFinancialNotifications();

        InOrder order = Mockito.inOrder(service);
        order.verify(service).sendBuildingNotice(1L, "due_7d");
        order.verify(service).sendBuildingNotice(2L, "due_today");
        order.verify(service).sendReserveNotice(3L);
        verify(service).dueReserveAccounts();
    }
}
