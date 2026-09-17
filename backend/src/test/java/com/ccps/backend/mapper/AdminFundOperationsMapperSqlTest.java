package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.LocalDate;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class AdminFundOperationsMapperSqlTest {
    @Test
    void transferLocksBothAccountsInStableOrder() throws Exception {
        Method method = AdminFundOperationsMapper.class.getMethod("lockTransferAccounts", Long.class, Long.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value()).replaceAll("\\s+", " ");
        assertThat(sql).contains("ORDER BY ra.id FOR UPDATE");
    }

    @Test
    void dueRemittanceExcludesHeldAndOpenBatchAccounts() throws Exception {
        Method method = AdminFundOperationsMapper.class.getMethod("lockDueRemittanceCandidates", LocalDate.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value()).replaceAll("\\s+", " ");
        assertThat(sql).contains("ors.hold_enabled=0").contains("NOT EXISTS")
                .contains("orb.status IN ('draft','submitted')").contains("FOR UPDATE");
    }
}
