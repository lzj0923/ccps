package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class AdminPropertyArchiveMapperSqlTest {

    @Test
    void plainSelectAnnotationsUseSqlComparisonOperatorsInsteadOfXmlEntities() throws Exception {
        String thisMonthSql = selectSql("countThisMonth");
        String ownershipSql = selectSql("countRelistableOwnerships", Long.class);

        assertThat(thisMonthSql)
                .contains("rental_off_market_at >= DATE_FORMAT")
                .doesNotContain("&gt;", "&lt;");
        assertThat(ownershipSql)
                .contains("asset_stage<>'DISPOSED'")
                .doesNotContain("&gt;", "&lt;");
    }

    @Test
    void unitStateQueryUsesInstantiableResultType() throws Exception {
        Class<?> resultType = AdminPropertyArchiveMapper.class
                .getMethod("lockUnit", Long.class)
                .getReturnType();

        assertThat(resultType.isInterface()).isFalse();
        assertThat(resultType.getDeclaredConstructor()).isNotNull();
    }

    private String selectSql(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = AdminPropertyArchiveMapper.class.getMethod(methodName, parameterTypes);
        return String.join(" ", method.getAnnotation(Select.class).value()).replaceAll("\\s+", " ");
    }
}
