package com.ccps.backend.mapper;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TenantWhatsAppSubscriptionMapper {
    @Select("SELECT id, phone, status FROM tenants WHERE id = #{tenantId}")
    TenantRow findTenant(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT tenant_id, destination, enabled, opted_in_at, opted_out_at, opt_in_source
            FROM tenant_whatsapp_subscriptions WHERE tenant_id = #{tenantId}
            """)
    SubscriptionRow findSubscription(@Param("tenantId") Long tenantId);

    @Insert("""
            INSERT INTO tenant_whatsapp_subscriptions
              (tenant_id, destination, enabled, opted_in_at, opted_out_at, opt_in_source)
            VALUES (#{tenantId}, #{destination}, 1, NOW(), NULL, #{source})
            ON DUPLICATE KEY UPDATE destination = VALUES(destination),
              opted_in_at = CASE WHEN enabled = 0 THEN NOW() ELSE opted_in_at END, enabled = 1,
              opted_out_at = NULL, opt_in_source = VALUES(opt_in_source)
            """)
    int enable(@Param("tenantId") Long tenantId, @Param("destination") String destination,
               @Param("source") String source);

    @Update("""
            UPDATE tenant_whatsapp_subscriptions
            SET enabled = 0, opted_out_at = NOW()
            WHERE tenant_id = #{tenantId}
            """)
    int disable(@Param("tenantId") Long tenantId);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, after_data)
            VALUES (#{actorId}, #{action}, 'tenant', #{tenantId},
              JSON_OBJECT('channel', 'whatsapp', 'enabled', #{enabled},
                          'destination', #{destination}, 'source', #{source}))
            """)
    int insertAudit(@Param("actorId") Long actorId, @Param("tenantId") Long tenantId,
                    @Param("action") String action, @Param("enabled") boolean enabled,
                    @Param("destination") String destination, @Param("source") String source);

    class TenantRow {
        private Long id;
        private String phone;
        private String status;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public String getPhone() { return phone; }
        public void setPhone(String value) { phone = value; }
        public String getStatus() { return status; }
        public void setStatus(String value) { status = value; }
    }

    class SubscriptionRow {
        private Long tenantId;
        private String destination;
        private Boolean enabled;
        private LocalDateTime optedInAt;
        private LocalDateTime optedOutAt;
        private String optInSource;
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long value) { tenantId = value; }
        public String getDestination() { return destination; }
        public void setDestination(String value) { destination = value; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean value) { enabled = value; }
        public LocalDateTime getOptedInAt() { return optedInAt; }
        public void setOptedInAt(LocalDateTime value) { optedInAt = value; }
        public LocalDateTime getOptedOutAt() { return optedOutAt; }
        public void setOptedOutAt(LocalDateTime value) { optedOutAt = value; }
        public String getOptInSource() { return optInSource; }
        public void setOptInSource(String value) { optInSource = value; }
    }
}
