package com.ccps.backend.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class AdminDashboardMapperSqlTest {
    @Test
    void dashboardGroupsMetricsByStateAndFallsBackToCity() throws Exception {
        Method method = AdminDashboardMapper.class.getMethod("findRegions");
        String sql = String.join("\n", method.getAnnotation(Select.class).value());

        assertTrue(sql.contains("p.state_name"));
        assertTrue(sql.contains("NULLIF(TRIM(p.city),'')"));
        assertTrue(sql.contains("CURRENT_DATE BETWEEN l.start_date AND l.end_date"));
        assertFalse(sql.contains("COALESCE(NULLIF(TRIM(p.city),''),'未设置区域')"));
    }
}
