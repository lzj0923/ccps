package com.ccps.backend.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class AdminOwnerMapperSqlTest {
    @Test
    void propertyPaginationScriptsAreValidMyBatisXml() throws Exception {
        assertScriptParses("findPropertyPage");
        assertScriptParses("countPropertyPage");
    }

    private void assertScriptParses(String methodName) throws Exception {
        Method method = java.util.Arrays.stream(AdminOwnerMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst().orElseThrow();
        String script = String.join(" ", method.getAnnotation(Select.class).value());
        assertDoesNotThrow(() -> new XMLLanguageDriver().createSqlSource(new Configuration(), script, Object.class));
    }
}
