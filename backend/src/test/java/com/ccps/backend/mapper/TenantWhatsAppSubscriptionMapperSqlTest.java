package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

class TenantWhatsAppSubscriptionMapperSqlTest {
    @Test void defaultEnrollmentPreservesOptOutAndExistingConsentTimestamp() throws Exception {
        String sql = String.join(" ", TenantWhatsAppSubscriptionMapper.class.getMethod("synchronizeFromTerms",
                Long.class, String.class, boolean.class, String.class, boolean.class).getAnnotation(Insert.class).value());
        assertThat(sql).contains("opted_out_at IS NULL OR #{resubscribe}")
                .contains("ELSE 0 END")
                .contains("COALESCE(opted_in_at, NOW())")
                .contains("CASE WHEN #{resubscribe} THEN NULL ELSE opted_out_at END");
    }

    @Test void optingOutCreatesATombstoneForPreviouslyMissingSubscription() throws Exception {
        String sql = String.join(" ", TenantWhatsAppSubscriptionMapper.class.getMethod("disable", Long.class)
                .getAnnotation(Insert.class).value());
        assertThat(sql).contains("VALUES (#{tenantId}, '', 0, NOW())")
                .contains("ON DUPLICATE KEY UPDATE enabled = 0, opted_out_at = NOW()");
    }

    @Test void queuedRentNotificationsRecheckConsentAndCurrentPhoneBeforeClaim() throws Exception {
        String select = String.join(" ", WhatsAppNotificationMapper.class.getMethod("findPendingDeliveries")
                .getAnnotation(Select.class).value());
        String claim = String.join(" ", WhatsAppNotificationMapper.class.getMethod("claim", Long.class, String.class)
                .getAnnotation(Update.class).value());
        for (String sql : new String[]{select, claim}) {
            assertThat(sql).contains("ws.enabled = 1").contains("ws.opted_in_at IS NOT NULL")
                    .contains("ws.opted_out_at IS NULL").contains("ws.destination REGEXP");
        }
        assertThat(claim).contains("ws.destination = #{destination}").contains("t.status = 'active'");
    }
}
