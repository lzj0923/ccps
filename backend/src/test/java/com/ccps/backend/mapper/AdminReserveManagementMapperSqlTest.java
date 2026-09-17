package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class AdminReserveManagementMapperSqlTest {

    @Test
    void directTopupStartsAsPendingFinanceReview() throws Exception {
        Method method = AdminReserveManagementMapper.class
                .getMethod("insertDirectTopupFinance", AdminReserveManagementMapper.DirectTopupRecord.class);
        String sql = String.join(" ", method.getAnnotation(Insert.class).value())
                .replaceAll("\\s+", " ");

        assertThat(sql)
                .contains("'paid','pending','not_synced'")
                .doesNotContain("'confirmed'")
                .doesNotContain("NOW()");
    }

    @Test
    void reconciliationSystemBalanceUsesReceiptDateWithoutDuplicatingSplitRent() throws Exception {
        Method method = AdminReserveManagementMapper.class.getMethod("findReceivedTotalBalance");
        String sql = String.join(" ", method.getAnnotation(Select.class).value())
                .replaceAll("\\s+", " ");

        assertThat(sql)
                .contains("SUM(fr.amount)")
                .contains("COALESCE(fr.receipt_date,fr.transaction_date)<=CURRENT_DATE")
                .contains("EXISTS")
                .doesNotContain("SUM(rp.allocated_amount)")
                .doesNotContain("JOIN rent_payments");
    }
}
