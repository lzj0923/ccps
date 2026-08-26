package com.ccps.backend.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class ReserveExpensePolicyMigrationTest {

    private static final Path MIGRATION = Path.of("..", "database", "migrate_reserve_expense_policy.sql");

    @Test
    void explicitlySelectedReserveExpenseCanCreateNegativeBalance() throws IOException {
        String sql = Files.readString(MIGRATION);
        String beforeInsert = section(sql, "CREATE TRIGGER trg_cashflow_expense_reserve_before",
                "CREATE TRIGGER trg_cashflow_expense_reserve_after");
        String afterInsert = section(sql, "CREATE TRIGGER trg_cashflow_expense_reserve_after",
                "CREATE TRIGGER trg_finance_expense_reserve_before_update");

        assertThat(beforeInsert)
                .contains("IF v_amount IS NULL OR v_balance IS NULL THEN")
                .doesNotContain("v_balance < v_amount");
        assertThat(afterInsert)
                .contains("SET v_after = v_balance - v_amount")
                .doesNotContain("v_balance >= v_amount");
    }

    @Test
    void historicalExplicitReserveExpenseIsBackfilledEvenWhenBalanceIsInsufficient() throws IOException {
        String sql = Files.readString(MIGRATION);
        String backfill = section(sql, "CREATE PROCEDURE ccps_allocate_existing_expense_reserves()",
                "DELIMITER ;");

        assertThat(backfill)
                .contains("fr.amount, fr.payment_method")
                .contains("v_payment_method = 'reserve_account' OR v_balance >= v_amount");
    }

    private static String section(String source, String start, String end) {
        int startIndex = source.indexOf(start);
        int endIndex = source.indexOf(end, startIndex + start.length());
        assertThat(startIndex).isGreaterThanOrEqualTo(0);
        assertThat(endIndex).isGreaterThan(startIndex);
        return source.substring(startIndex, endIndex);
    }
}
