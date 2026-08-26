package com.ccps.backend.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminRentCollectionWorkflowResponse;

@ExtendWith(MockitoExtension.class)
class AdminRentCollectionSchedulerTest {
    @Mock
    private AdminRentCollectionWorkflowService service;

    private AdminRentCollectionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AdminRentCollectionScheduler(service);
    }

    @Test
    void automaticallySendsEveryDueActiveNotice() {
        AdminRentCollectionWorkflowResponse.Item first = item(1L, "active", "first_reminder");
        AdminRentCollectionWorkflowResponse.Item finalNotice = item(2L, "active", "termination_notice");
        AdminRentCollectionWorkflowResponse response = response(first, finalNotice);
        when(service.overview()).thenReturn(response);

        scheduler.sendDueNotices();

        verify(service).sendStage(null, 1L, "first_reminder");
        verify(service).sendStage(null, 2L, "termination_notice");
    }

    @Test
    void skipsWaitingAndHeldCollections() {
        AdminRentCollectionWorkflowResponse.Item waiting = item(3L, "active", "waiting");
        AdminRentCollectionWorkflowResponse.Item held = item(4L, "on_hold", "on_hold");
        AdminRentCollectionWorkflowResponse response = response(waiting, held);
        when(service.overview()).thenReturn(response);

        scheduler.sendDueNotices();

        verify(service).overview();
        verifyNoMoreInteractions(service);
    }

    @Test
    void continuesWithOtherTenantsWhenOneNoticeFails() {
        AdminRentCollectionWorkflowResponse.Item first = item(5L, "active", "first_reminder");
        AdminRentCollectionWorkflowResponse.Item second = item(6L, "active", "second_reminder");
        AdminRentCollectionWorkflowResponse response = response(first, second);
        when(service.overview()).thenReturn(response);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "already sent"))
                .when(service).sendStage(null, 5L, "first_reminder");

        scheduler.sendDueNotices();

        verify(service).sendStage(null, 5L, "first_reminder");
        verify(service).sendStage(null, 6L, "second_reminder");
    }

    private AdminRentCollectionWorkflowResponse response(AdminRentCollectionWorkflowResponse.Item... items) {
        AdminRentCollectionWorkflowResponse response = mock(AdminRentCollectionWorkflowResponse.class);
        when(response.items()).thenReturn(List.of(items));
        return response;
    }

    private AdminRentCollectionWorkflowResponse.Item item(Long invoiceId, String workflowStatus, String stage) {
        AdminRentCollectionWorkflowResponse.Item item = mock(AdminRentCollectionWorkflowResponse.Item.class);
        lenient().when(item.invoiceId()).thenReturn(invoiceId);
        lenient().when(item.workflowStatus()).thenReturn(workflowStatus);
        lenient().when(item.currentStage()).thenReturn(stage);
        return item;
    }
}
