package com.ccps.backend.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class LeaseExpiryWhatsAppConfigurationTest {

    @Test
    void startupSchemaSeedsEnabledThirtyDayBusinessWhatsAppRule() throws Exception {
        try (var stream = getClass().getResourceAsStream("/schema.sql")) {
            assertThat(stream).isNotNull();
            String sql = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                    .replaceAll("\\s+", " ");
            assertThat(sql)
                    .contains("LEASE_EXPIRY_BUSINESS_30D")
                    .contains("'lease_expiry', 30")
                    .contains("JSON_ARRAY('whatsapp'), 'business', 1")
                    .contains("ON DUPLICATE KEY UPDATE")
                    .contains("days_before = 30")
                    .contains("recipient_role = 'business'")
                    .contains("enabled = 1");
        }
    }
}
