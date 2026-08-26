package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class OwnerExpenseMaintenanceMapperSqlTest {

    @Test
    void expenseProjectionMatchesExpenseItemConstructorOrder() throws Exception {
        String sql = selectSql("findExpenses");

        assertThat(sql).containsSubsequence(
                "fr.transaction_date AS payment_date",
                "mwo.id AS work_order_id",
                "AS attachment_count",
                "END AS editable",
                "pr.payer_name AS payer_name",
                "AS bank_name",
                "AS payment_account_no");
    }

    @Test
    void maintenanceProjectionMatchesMaintenanceItemConstructorOrder() throws Exception {
        String sql = selectSql("findMaintenance");

        assertThat(sql).containsSubsequence(
                "fr.payment_status",
                "AS reserve_deducted_amount",
                "AS attachment_count",
                "END AS editable",
                "pr.payer_name AS payer_name",
                "AS bank_name",
                "AS payment_account_no");
    }

    private String selectSql(String methodName) throws Exception {
        Method method = Arrays.stream(OwnerExpenseMaintenanceMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        Select select = method.getAnnotation(Select.class);
        assertThat(select).isNotNull();
        return String.join(" ", select.value()).replaceAll("\\s+", " ");
    }
}
