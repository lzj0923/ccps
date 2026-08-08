package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class OwnerIncomeAccountingPolicyTest {

    @Test
    void ordinarySecurityDepositIsExcludedFromOwnerIncomeTotals() throws Exception {
        String financeTotalSql = selectSql(OwnerExpenseMaintenanceMapper.class,
                "findCashflowTotals", Long.class, Long.class);
        String propertyMonthlySql = selectSql(OwnerDashboardMapper.class,
                "findPropertiesByUserId", Long.class);

        assertThat(financeTotalSql).contains("fr.record_type <> 'security_deposit'");
        assertThat(propertyMonthlySql).contains("fr.record_type <> 'security_deposit'");
        assertThat(financeTotalSql).doesNotContain("security_deposit_forfeiture'");
    }

    private String selectSql(Class<?> mapperType, String methodName, Class<?>... parameterTypes)
            throws Exception {
        Method method = mapperType.getMethod(methodName, parameterTypes);
        Select select = method.getAnnotation(Select.class);
        assertThat(select).isNotNull();
        return String.join(" ", Arrays.asList(select.value())).replaceAll("\\s+", " ");
    }
}
