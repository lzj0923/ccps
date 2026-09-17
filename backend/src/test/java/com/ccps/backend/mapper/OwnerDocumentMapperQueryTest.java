package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class OwnerDocumentMapperQueryTest {
    @Test
    void documentListResolvesPropertyFromUnitWorkOrderAndFinanceLinks() throws Exception {
        Select select = OwnerDocumentMapper.class.getMethod("findDocuments", Long.class)
                .getAnnotation(Select.class);
        String sql = String.join(" ", select.value()).toLowerCase();

        assertThat(sql).contains("maintenance_work_orders work_order");
        assertThat(sql).contains("finance_records finance_record");
        assertThat(sql).contains("coalesce(unit_link.entity_id, work_order.unit_id, finance_record.unit_id,");
        assertThat(sql).contains("cashflow_entries");
        assertThat(sql).contains("mandate_owner_unit.unit_id, archive_owner_unit.unit_id)");
        assertThat(sql).contains("o.user_id = #{userid}");
    }
}
