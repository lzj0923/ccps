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
        assertScriptParses("findIncomeExpenseRows");
        assertScriptParses("findMaintenanceRows");
        assertScriptParses("findReserveRows");
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
    void reserveRefundIsAcceptedAsAGeneratedReportType() throws Exception {
        Pattern reportType = AdminReportGenerateRequest.class.getDeclaredMethod("reportType").getAnnotation(Pattern.class);

        assertThat(reportType.regexp()).contains("reserve_refund");
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
