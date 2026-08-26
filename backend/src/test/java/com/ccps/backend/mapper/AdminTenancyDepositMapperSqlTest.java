package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminTenancyDepositMapperSqlTest {

    @Test
    void depositAccountSelectSupportsMultipleCollectionsAndPrioritizesPendingEntry() {
        assertThat(AdminTenancyMapper.DEPOSIT_ACCOUNT_SELECT)
                .contains("sde.id=(")
                .contains("sde2.lease_id=l.id")
                .contains("sde2.status<>'rejected'")
                .contains("ORDER BY (sde2.status='pending') DESC,sde2.id DESC LIMIT 1")
                .doesNotContain("LEFT JOIN security_deposit_entries sde ON sde.lease_id=l.id")
                .doesNotContain("&lt;", "&gt;");
    }
}
