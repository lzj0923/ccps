package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;
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

    @Test
    void financeScopesSeparateTenantChargesFromCashflowMaintenanceAndKeepSettlementsSeparate() throws Exception {
        Method expense = AdminFinanceReviewMapper.class.getMethod("findExpensePage", String.class, String.class,
                String.class, String.class, String.class, java.time.LocalDate.class, java.time.LocalDate.class, int.class, int.class);
        String expenseSql = String.join(" ", expense.getAnnotation(Select.class).value()).replaceAll("\\s+", " ");
        Method settlement = AdminFinanceReviewMapper.class.getMethod("findSettlementPage", String.class,
                String.class, String.class, String.class, String.class, java.time.LocalDate.class,
                java.time.LocalDate.class, int.class, int.class);
        String settlementSql = String.join(" ", settlement.getAnnotation(Select.class).value())
                .replaceAll("\\s+", " ");

        assertThat(expenseSql).contains("fr.record_type IN ('property_expense','tenant_charge','cashflow')")
                .contains("COALESCE(rii.charge_type,ce.category)")
                .contains("reviewType == 'cashflow_maintenance'")
                .contains("fr.record_type IN ('property_expense','cashflow')")
                .contains("reviewType == 'expense'")
                .contains("fr.record_type = 'tenant_charge'")
                .doesNotContain("security_deposit_forfeiture");
        assertThat(settlementSql).contains("reviewType == 'reserve_refund'")
                .contains("fr.tenant_id IS NULL")
                .contains("fr.tenant_id IS NOT NULL")
                .contains("security_deposit_forfeiture");
    }
}
