package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

class PaymentProgressMapperSqlTest {

    @Test
    void installmentConfirmationUsesLatestSubmissionInsteadOfAnyHistoricalRejection() throws Exception {
        Select select = PaymentProgressMapper.class
                .getMethod("findInstallments", Long.class)
                .getAnnotation(Select.class);
        String sql = String.join(" ", select.value()).replaceAll("\\s+", " ");

        assertThat(sql)
                .contains("ORDER BY fr_latest.created_at DESC, fr_latest.id DESC")
                .contains("CASE WHEN fr_latest.confirmation_status = 'rejected' THEN pr_latest.review_note END")
                .doesNotContain("SUM(CASE WHEN fr.confirmation_status = 'rejected'");
    }
}
