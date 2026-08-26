package com.ccps.backend.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import com.ccps.backend.dto.AdminReportGenerateRequest;
import jakarta.validation.constraints.Pattern;

class AdminReportMapperSqlTest {
    @Test
    void scopedReportScriptsAreValidMyBatisXml() throws Exception {
        assertScriptParses("findFinanceReportRows");
        assertScriptParses("findPropertyPaymentRows");
        assertScriptParses("findRentCollectionRows");
        assertScriptParses("findIncomeExpenseRows");
        assertScriptParses("findTenantStatementRows");
        assertScriptParses("findMaintenanceRows");
        assertScriptParses("findReserveRows");
        assertScriptParses("findSyncRows");
    }

    @Test
    void everyReportQuerySupportsOpenDateBounds() throws Exception {
        for (String method : java.util.List.of("findFinanceReportRows", "findPropertyPaymentRows",
                "findRentCollectionRows", "findTenantStatementRows", "findIncomeExpenseRows",
                "findMaintenanceRows", "findReserveRows", "findSyncRows")) {
            assertThat(script(method))
                    .as(method)
                    .contains("start != null")
                    .contains("end != null");
        }
    }

    @Test
    void incomeExpenseQueryUsesMySql57CompatibleBalanceCalculation() throws Exception {
        assertThat(script("findIncomeExpenseRows")).doesNotContain(" OVER (");
    }

    @Test
    void incomeExpenseQueryBuildsTheCombinedItemAndFullBankInfoFields() throws Exception {
        assertThat(script("findIncomeExpenseRows"))
                .contains("THEN '支出' ELSE '收入' END")
                .contains("NULLIF(ce.description,'')")
                .contains("CONCAT_WS(CHAR(10)");
    }

    @Test
    void receivableReportsStartFromBillsSoUnpaidItemsAreNotDropped() throws Exception {
        assertThat(script("findPropertyPaymentRows"))
                .contains("FROM payment_installments pi")
                .contains("pi.amount_paid")
                .contains("'unpaid'");
        assertThat(script("findRentCollectionRows"))
                .contains("FROM rent_invoices ri")
                .contains("ri.amount_paid")
                .contains("'unpaid'");
    }

    @Test
    void propertyPaymentReportIncludesContractsThatHaveNoInstallments() throws Exception {
        assertThat(script("findPropertyPaymentRows"))
                .contains("UNION ALL")
                .contains("FROM purchase_contracts fallback_pc")
                .contains("fallback_pc.purchase_price AS amount")
                .contains("NOT EXISTS")
                .contains("missing_pp.purchase_contract_id = fallback_pc.id");
    }

    @Test
    void reserveRefundIsAcceptedAsAGeneratedReportType() throws Exception {
        Pattern reportType = AdminReportGenerateRequest.class.getDeclaredMethod("reportType").getAnnotation(Pattern.class);

        assertThat(reportType.regexp()).contains("reserve_refund");
    }

    @Test
    void individualStatementsAreAcceptedAsGeneratedReportTypes() throws Exception {
        Pattern reportType = AdminReportGenerateRequest.class.getDeclaredMethod("reportType").getAnnotation(Pattern.class);

        assertThat(reportType.regexp()).contains("owner_statement").contains("tenant_statement");
    }

    private void assertScriptParses(String methodName) throws Exception {
        assertDoesNotThrow(() -> new XMLLanguageDriver().createSqlSource(
                new Configuration(), script(methodName), Object.class));
    }

    private String script(String methodName) throws Exception {
        Method method = java.util.Arrays.stream(AdminReportMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst().orElseThrow();
        return String.join(" ", method.getAnnotation(Select.class).value());
    }
}
