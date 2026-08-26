package com.ccps.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

class AdminReminderMapperSqlTest {

    @Test
    void retryingWhatsAppDeliveryRefreshesDestinationFromCurrentTenantConsent() throws Exception {
        Method method = AdminReminderMapper.class.getMethod("retryWhatsAppDelivery", Long.class);
        String sql = String.join(" ", method.getAnnotation(Update.class).value())
                .replaceAll("\\s+", " ");

        assertThat(sql)
                .contains("UPDATE notification_deliveries d")
                .contains("JOIN notifications n ON n.id = d.notification_id")
                .contains("JOIN rent_invoices ri ON ri.id = n.related_id")
                .contains("JOIN leases l ON l.id = ri.lease_id")
                .contains("JOIN tenant_whatsapp_subscriptions ws ON ws.tenant_id = l.tenant_id")
                .contains("d.destination = ws.destination")
                .contains("ws.enabled = 1")
                .contains("ws.opted_in_at IS NOT NULL");
    }

    @Test
    void leaseExpiryReminderTargetsResponsibleBusinessUserAndPhone() throws Exception {
        Method method = AdminReminderMapper.class.getMethod("findLeaseExpiryEvents", Long.class, int.class);
        String sql = String.join(" ", method.getAnnotation(Select.class).value())
                .replaceAll("\\s+", " ");

        assertThat(sql)
                .contains("JOIN rental_mandates rm ON rm.id = l.rental_mandate_id")
                .contains("JOIN users responsible ON responsible.id = COALESCE(rm.responsible_user_id, rm.created_by)")
                .contains("responsible.id AS recipient_user_id")
                .contains("responsible.phone AS whatsapp_destination")
                .contains("l.end_date >= CURDATE()")
                .contains("n.recipient_user_id = responsible.id")
                .doesNotContain("o.id AS recipient_owner_id");
    }

    @Test
    void retryingLeaseExpiryWhatsAppRefreshesResponsibleUserPhone() throws Exception {
        Method method = AdminReminderMapper.class.getMethod("retryLeaseExpiryWhatsAppDelivery", Long.class);
        String sql = String.join(" ", method.getAnnotation(Update.class).value())
                .replaceAll("\\s+", " ");

        assertThat(sql)
                .contains("n.related_type = 'lease'")
                .contains("responsible.id = COALESCE(rm.responsible_user_id, rm.created_by)")
                .contains("d.destination = responsible.phone")
                .contains("NULLIF(TRIM(responsible.phone), '') IS NOT NULL");
    }
}
