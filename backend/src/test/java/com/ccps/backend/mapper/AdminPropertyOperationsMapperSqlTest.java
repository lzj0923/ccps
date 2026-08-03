package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class AdminPropertyOperationsMapperSqlTest {
    @Test
    void operationsQueriesAreLeaseScopedAndIncludeMonthlyCharges() throws Exception {
        String leaseSql = selectSql("findActiveLease");
        String invoiceSql = selectSql("findInvoice");
        String workOrderSql = selectSql("findWorkOrders");

        assertThat(leaseSql).contains("l.status = 'active'", "ou.owner_id = #{ownerId}", "ou.id = #{ownerUnitId}");
        assertThat(invoiceSql).contains("lease_id = #{leaseId}", "billing_month = #{billingMonth}");
        assertThat(workOrderSql).contains("mwo.lease_id = #{leaseId}");
    }

    @Test
    void operationsWriteScriptsParseAsMyBatisSql() throws Exception {
        assertScriptParses("insertCharge", Insert.class);
        assertScriptParses("increaseInvoiceAmount", Update.class);
        assertScriptParses("insertWorkOrder", Insert.class);
    }

    private String selectSql(String methodName) throws Exception {
        Method method = method(methodName);
        return String.join(" ", method.getAnnotation(Select.class).value());
    }

    private void assertScriptParses(String methodName, Class<? extends java.lang.annotation.Annotation> annotationType) throws Exception {
        Method method = method(methodName);
        String script;
        if (annotationType == Insert.class) {
            script = String.join(" ", method.getAnnotation(Insert.class).value());
        } else {
            script = String.join(" ", method.getAnnotation(Update.class).value());
        }
        assertDoesNotThrow(() -> new XMLLanguageDriver().createSqlSource(new Configuration(), script, Object.class));
    }

    private Method method(String methodName) {
        return java.util.Arrays.stream(AdminPropertyOperationsMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst().orElseThrow();
    }
}
