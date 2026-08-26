package com.ccps.backend.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class StartupSchemaMysql57CompatibilityTest {

    @Test
    void startupSchemaAddsFinanceDateColumnWithoutMysql8OnlySyntax() throws Exception {
        try (var stream = getClass().getResourceAsStream("/schema.sql")) {
            assertThat(stream).isNotNull();
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ");

            assertThat(sql)
                    .doesNotContain("ADD COLUMN IF NOT EXISTS requested_transaction_date")
                    .contains("information_schema.COLUMNS")
                    .contains("COLUMN_NAME = 'requested_transaction_date'")
                    .contains("PREPARE add_requested_transaction_date_stmt")
                    .contains("EXECUTE add_requested_transaction_date_stmt")
                    .contains("DEALLOCATE PREPARE add_requested_transaction_date_stmt");
        }
    }
}
