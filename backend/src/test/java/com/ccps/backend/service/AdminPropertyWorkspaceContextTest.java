package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AdminPropertyWorkspaceContextTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(AdminOwnerService.class, () -> mock(AdminOwnerService.class))
            .withBean(AdminPropertyOwnershipService.class, () -> mock(AdminPropertyOwnershipService.class))
            .withBean(AdminPropertyContractRecordService.class, () -> mock(AdminPropertyContractRecordService.class))
            .withBean(PropertyExpensePostingService.class, () -> mock(PropertyExpensePostingService.class))
            .withBean(AdminPropertyWorkspaceService.class);

    @Test
    void springCanConstructPropertyWorkspaceService() {
        contextRunner.run(context -> {
            assertThat(context.getStartupFailure()).isNull();
            assertThat(context).hasSingleBean(AdminPropertyWorkspaceService.class);
        });
    }
}
