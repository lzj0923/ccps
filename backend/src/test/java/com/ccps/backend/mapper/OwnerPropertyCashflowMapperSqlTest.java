package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class OwnerPropertyCashflowMapperSqlTest {

    @Test
    void reserveDebitLinkedToCashflowIsNotReturnedAsASecondOwnerExpense() throws Exception {
        Method method = OwnerPropertyCashflowMapper.class
                .getMethod("findReserveTransactions", Long.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value())
                .replaceAll("\\s+", " ");

        assertThat(sql).contains(
                "NOT EXISTS ( SELECT 1 FROM cashflow_entries linked_ce WHERE linked_ce.finance_record_id = rt.finance_record_id )");
    }
}
