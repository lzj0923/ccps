package com.ccps.backend.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReserveTargetPolicySchedulerTest {
    @Mock private ReserveTargetPolicyService service;

    @Test
    void oneFailedAccountDoesNotBlockOtherAccounts() {
        when(service.activeAccountIds()).thenReturn(List.of(1L, 2L));
        Mockito.doThrow(new IllegalStateException("failed")).when(service).recalculate(1L);

        new ReserveTargetPolicyScheduler(service).recalculateAll();

        InOrder order = inOrder(service);
        order.verify(service).recalculate(1L);
        order.verify(service).recalculate(2L);
    }
}
