package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

class AdminFinanceReviewMapperSqlTest {

    @Test
    void confirmingPropertyExpenseMarksItAsPaidForExpenseMaintenanceList() throws Exception {
        Method method = AdminFinanceReviewMapper.class.getMethod("confirmExpense", Long.class, Long.class);
        Update update = method.getAnnotation(Update.class);
        String sql = String.join(" ", update.value()).replaceAll("\\s+", " ");

        assertThat(sql).contains("confirmation_status='confirmed'");
        assertThat(sql).contains("payment_status='paid'");
    }

    @Test
    void schemaRepairsAlreadyConfirmedExpensesThatStillLookUnpaid() throws Exception {
        try (var input = getClass().getResourceAsStream("/schema.sql")) {
            assertThat(input).isNotNull();
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ");
            assertThat(schema).contains("SET payment_status = 'paid' WHERE record_type = 'property_expense' AND confirmation_status = 'confirmed' AND payment_status = 'unpaid'");
        }
    }

    @Test
    void schemaWithdrawsLegacyPendingDirectPaymentsForTerminatedOwners() throws Exception {
        try (var input = getClass().getResourceAsStream("/schema.sql")) {
            assertThat(input).isNotNull();
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ");
            assertThat(schema).contains("ous.service_type = 'RENTAL' AND ous.status = 'ended'");
            assertThat(schema).contains("fr.payment_method = 'direct_payment'");
            assertThat(schema).contains("fr.payment_status = 'voided'");
            assertThat(schema).contains("fr.confirmation_status = 'rejected'");
        }
    }

    @Test
    void reserveDebitsDoNotRequireANonNegativeRemainingBalance() throws Exception {
        Method refund = AdminFinanceReviewMapper.class.getMethod("debitReserveBalance", Long.class,
                java.math.BigDecimal.class);
        String refundSql = String.join(" ", refund.getAnnotation(Update.class).value())
                .replaceAll("\\s+", " ");
        Method expense = AdminMaintenanceMapper.class.getMethod("debitExpenseReserve", Long.class,
                java.math.BigDecimal.class);
        String expenseSql = String.join(" ", expense.getAnnotation(Update.class).value())
                .replaceAll("\\s+", " ");

        assertThat(refundSql).doesNotContain("current_balance>=");
        assertThat(expenseSql).doesNotContain("current_balance >=");
    }
}
