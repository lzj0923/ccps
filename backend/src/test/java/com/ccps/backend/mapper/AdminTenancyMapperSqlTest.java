package com.ccps.backend.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class AdminTenancyMapperSqlTest {
    @Test
    void tenantInvoicePaginationScriptsAreValidMyBatisXml() throws Exception {
        assertScriptParses("findPage");
        assertScriptParses("countPage");
        assertInsertScriptParses("insertInvoice");
        assertInsertScriptParses("insertSecurityDepositFinance");
        assertInsertScriptParses("insertSecurityDepositCashflow");
    }

    @Test
    void tenancyQueriesExposeCurrentAndHistoricalUnpaidAmountsPerLease() throws Exception {
        String listSql = selectSql("findPage");
        String summarySql = selectSql("findSummary");

        assertThat(listSql).contains("AS total_unpaid", "ri_total.lease_id = l.id",
                "ri_total.billing_month &lt;= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')");
        assertThat(summarySql).contains("AS total_unpaid",
                "ri_all.billing_month <= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')")
                .doesNotContain("ri_all.billing_month &lt;=");
    }

    @Test
    void tenancyRowsExposeTheActivePrimaryOwnerForContractPrefill() throws Exception {
        String listSql = selectSql("findPage");

        assertThat(listSql).contains("owner_units", "owners", "is_primary = 1",
                "COALESCE(NULLIF(TRIM(primary_owner.identity_no), ''), NULLIF(TRIM(primary_owner.passport_no), '')) AS owner_identity",
                "primary_owner.full_name AS owner_name");
    }

    @Test
    void rentOverdueStatusUsesSevenDayGraceFromLeaseStart() throws Exception {
        String listSql = selectSql("findPage");
        String summarySql = selectSql("findSummary");
        String collectionSql = selectSql("findRentCollections");

        assertThat(listSql).contains("CURRENT_DATE &gt; GREATEST(ri.due_date, DATE_ADD(l.start_date, INTERVAL 7 DAY))");
        assertThat(summarySql).contains("CURRENT_DATE > GREATEST(ri.due_date, DATE_ADD(l.start_date, INTERVAL 7 DAY))");
        assertThat(collectionSql).contains("CURRENT_DATE &gt; GREATEST(ri.due_date, DATE_ADD(l.start_date, INTERVAL 7 DAY))");
    }

    @Test
    void tenantDepositLedgerRemainsCompatibleWithMysql57() throws Exception {
        String depositSql = selectSql("findTenantDepositTransactions");

        assertThat(depositSql)
                .doesNotContain(" OVER ")
                .contains("prior.tenant_id = tdt.tenant_id",
                        "prior.occurred_on < tdt.occurred_on",
                        "prior.id <= tdt.id");
    }

    @Test
    void tenantDirectoryIncludesTheCurrentLeaseDepositBalanceAndFinanceStatus() throws Exception {
        String directorySql = selectSql("findTenantDirectoryPage");

        assertThat(directorySql).contains("AS current_deposit_amount",
                "tenant_deposit_transactions",
                "tdt.status IN ('posted','pending')",
                "AS current_deposit_balance",
                "AS current_deposit_status",
                "LEFT JOIN security_deposit_entries",
                "LEFT JOIN finance_records");
    }

    private String selectSql(String methodName) throws Exception {
        Method method = java.util.Arrays.stream(AdminTenancyMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst().orElseThrow();
        return String.join(" ", method.getAnnotation(Select.class).value());
    }

    private void assertScriptParses(String methodName) throws Exception {
        String script = selectSql(methodName);
        assertDoesNotThrow(() -> new XMLLanguageDriver().createSqlSource(new Configuration(), script, Object.class));
    }

    private void assertInsertScriptParses(String methodName) throws Exception {
        Method method = java.util.Arrays.stream(AdminTenancyMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst().orElseThrow();
        String script = String.join(" ", method.getAnnotation(org.apache.ibatis.annotations.Insert.class).value());
        assertDoesNotThrow(() -> new XMLLanguageDriver().createSqlSource(new Configuration(), script, Object.class));
    }
}
