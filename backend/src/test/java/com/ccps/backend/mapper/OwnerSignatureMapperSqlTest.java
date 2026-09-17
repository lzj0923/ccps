package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class OwnerSignatureMapperSqlTest {
    @Test void mapperStatementsParseAndOwnershipRequiresAssignmentActiveAccountAndHolding() throws Exception {
        Configuration configuration=new Configuration();
        new MapperAnnotationBuilder(configuration,OwnerSignatureMapper.class).parse();
        String sql=String.join(" ",OwnerSignatureMapper.class.getMethod("authorized",Long.class,Long.class).getAnnotation(Select.class).value());
        assertThat(sql).contains("t.recipient_user_id=#{userId}","o.user_id=t.recipient_user_id","ou.status='active'",
            "sr.signer_role IN ('owner','second_owner')","d.status NOT IN ('superseded','voided')",
            "root.status NOT IN ('superseded','voided')","rm.owner_unit_id=ou.id","l.unit_id=ou.unit_id");
    }
}
