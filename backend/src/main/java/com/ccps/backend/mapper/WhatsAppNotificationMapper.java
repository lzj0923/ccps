package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WhatsAppNotificationMapper {
    @Select("""
            SELECT d.id AS delivery_id, d.notification_id, ws.destination, d.attempt_count,
                    COALESCE(rca.stage, 'first_reminder') AS stage,
                    t.full_name AS tenant_name, p.name AS project_name, u.unit_no,
                   ri.billing_month, ri.due_date,
                   GREATEST(ri.amount_due - ri.amount_paid, 0) AS outstanding_amount,
                   GREATEST(DATEDIFF(CURDATE(), ri.due_date), 0) AS overdue_days
            FROM notification_deliveries d
            JOIN notifications n ON n.id = d.notification_id
            LEFT JOIN rent_collection_actions rca ON rca.notification_id = n.id
            LEFT JOIN notification_rules nr ON nr.id = n.rule_id
            JOIN rent_invoices ri ON ri.id = COALESCE(rca.invoice_id,
                 CASE WHEN n.related_type = 'rent_invoice' THEN n.related_id END)
            JOIN leases l ON l.id = ri.lease_id
            JOIN tenants t ON t.id = l.tenant_id
            JOIN tenant_whatsapp_subscriptions ws ON ws.tenant_id = t.id
              AND ws.enabled = 1 AND ws.opted_in_at IS NOT NULL AND ws.opted_out_at IS NULL
              AND ws.destination REGEXP '^[1-9][0-9]{7,14}$'
            JOIN units u ON u.id = l.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE d.channel = 'whatsapp' AND d.status = 'pending'
              AND t.status = 'active' AND l.status = 'active'
              AND (rca.id IS NOT NULL OR nr.event_type = 'rent_due')
            ORDER BY d.id
            LIMIT 50
            """)
    List<WhatsAppDeliveryRow> findPendingDeliveries();

    @Select("""
            SELECT d.id AS delivery_id, d.notification_id, d.destination, d.attempt_count,
                   'lease_expiry_business' AS stage,
                   responsible.display_name AS tenant_name, p.name AS project_name, u.unit_no,
                   l.lease_no, l.end_date AS due_date
            FROM notification_deliveries d
            JOIN notifications n ON n.id = d.notification_id
              AND n.related_type = 'lease'
            JOIN notification_rules nr ON nr.id = n.rule_id AND nr.event_type = 'lease_expiry'
            JOIN leases l ON l.id = n.related_id AND l.status = 'active'
            JOIN rental_mandates rm ON rm.id = l.rental_mandate_id
              AND rm.status IN ('active', 'suspended')
            JOIN users responsible ON responsible.id = COALESCE(rm.responsible_user_id, rm.created_by)
              AND responsible.id = n.recipient_user_id AND responsible.status = 'active'
              AND responsible.account_type = 'ADMIN'
            JOIN units u ON u.id = l.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE d.channel = 'whatsapp' AND d.status = 'pending'
            ORDER BY d.id
            LIMIT 50
            """)
    List<WhatsAppDeliveryRow> findPendingLeaseExpiryDeliveries();

    @Update("""
            UPDATE notification_deliveries d
            JOIN notifications n ON n.id = d.notification_id
            LEFT JOIN rent_collection_actions rca ON rca.notification_id = n.id
            LEFT JOIN rent_invoices ri ON ri.id = COALESCE(rca.invoice_id,
                CASE WHEN n.related_type = 'rent_invoice' THEN n.related_id END)
            LEFT JOIN leases l ON l.id = ri.lease_id
            LEFT JOIN tenants t ON t.id = l.tenant_id
            LEFT JOIN tenant_whatsapp_subscriptions ws ON ws.tenant_id = t.id
            SET d.status = 'sending', d.attempt_count = d.attempt_count + 1,
                d.failed_at = NULL, d.failure_reason = NULL,
                d.destination = CASE WHEN ri.id IS NOT NULL THEN ws.destination ELSE d.destination END
            WHERE d.id = #{deliveryId} AND d.channel = 'whatsapp' AND d.status = 'pending'
              AND ((n.related_type <> 'rent_invoice' AND rca.id IS NULL)
                   OR (t.status = 'active' AND l.status = 'active'
                       AND ws.enabled = 1 AND ws.opted_in_at IS NOT NULL AND ws.opted_out_at IS NULL
                       AND ws.destination = #{destination}
                       AND ws.destination REGEXP '^[1-9][0-9]{7,14}$'))
            """)
    int claim(@Param("deliveryId") Long deliveryId, @Param("destination") String destination);

    @Insert("""
            INSERT INTO whatsapp_delivery_attempts
              (delivery_id, attempt_number, template_name, template_language, status, status_at)
            SELECT id, attempt_count, #{templateName}, #{templateLanguage}, 'sending', NOW()
            FROM notification_deliveries WHERE id = #{deliveryId} AND status = 'sending'
            """)
    @Options(useGeneratedKeys = true, keyProperty = "attempt.id")
    int insertAttempt(@Param("deliveryId") Long deliveryId,
                      @Param("templateName") String templateName,
                      @Param("templateLanguage") String templateLanguage,
                      @Param("attempt") NewAttempt attempt);

    @Update("""
            UPDATE whatsapp_delivery_attempts
            SET provider_message_id = #{messageId}, provider_wa_id = #{waId},
                status = 'accepted', status_at = NOW()
            WHERE id = #{attemptId}
            """)
    int markAttemptAccepted(@Param("attemptId") Long attemptId, @Param("messageId") String messageId,
                            @Param("waId") String waId);

    @Update("""
            UPDATE notification_deliveries
            SET status = 'sent', sent_at = COALESCE(sent_at, NOW()), failed_at = NULL, failure_reason = NULL
            WHERE id = #{deliveryId} AND channel = 'whatsapp' AND status = 'sending'
            """)
    int markDeliveryAccepted(@Param("deliveryId") Long deliveryId);

    @Update("""
            UPDATE whatsapp_delivery_attempts
            SET status = #{status}, status_at = NOW(), meta_error_code = #{errorCode},
                meta_error_subcode = #{errorSubcode}, meta_error_details = #{details}, fbtrace_id = #{traceId}
            WHERE id = #{attemptId}
            """)
    int markAttemptFailed(@Param("attemptId") Long attemptId, @Param("status") String status,
                          @Param("errorCode") Integer errorCode, @Param("errorSubcode") Integer errorSubcode,
                          @Param("details") String details, @Param("traceId") String traceId);

    @Update("""
            UPDATE notification_deliveries
            SET status = #{status}, failed_at = NOW(), failure_reason = #{reason}
            WHERE id = #{deliveryId} AND channel = 'whatsapp' AND status = 'sending'
            """)
    int markDeliveryFailed(@Param("deliveryId") Long deliveryId, @Param("status") String status,
                           @Param("reason") String reason);

    @Update("""
            UPDATE whatsapp_delivery_attempts
            SET status = CASE
                  WHEN #{status} = 'failed' THEN 'failed'
                  WHEN #{status} = 'read' THEN 'read'
                  WHEN #{status} = 'delivered' AND status NOT IN ('read','failed') THEN 'delivered'
                  WHEN #{status} = 'sent' AND status IN ('accepted','sending') THEN 'sent'
                  ELSE status END,
                status_at = CASE WHEN status_at IS NULL OR status_at <= #{statusAt} THEN #{statusAt} ELSE status_at END,
                delivered_at = CASE WHEN #{status} = 'delivered' THEN COALESCE(delivered_at, #{statusAt}) ELSE delivered_at END,
                read_at = CASE WHEN #{status} = 'read' THEN COALESCE(read_at, #{statusAt}) ELSE read_at END,
                meta_error_code = COALESCE(#{errorCode}, meta_error_code),
                meta_error_details = COALESCE(#{errorDetails}, meta_error_details)
            WHERE provider_message_id = #{messageId}
              AND (status_at IS NULL OR status_at <= #{statusAt})
            """)
    int updateAttemptStatus(@Param("messageId") String messageId, @Param("status") String status,
                            @Param("statusAt") LocalDateTime statusAt, @Param("errorCode") Integer errorCode,
                            @Param("errorDetails") String errorDetails);

    @Update("""
            UPDATE notification_deliveries d
            JOIN whatsapp_delivery_attempts a ON a.delivery_id = d.id
            SET d.status = CASE
                  WHEN #{status} = 'failed' THEN 'failed'
                  WHEN #{status} = 'read' THEN 'read'
                  WHEN #{status} = 'delivered' AND d.status NOT IN ('read','failed') THEN 'delivered'
                  WHEN #{status} = 'sent' AND d.status IN ('sending','sent') THEN 'sent'
                  ELSE d.status END,
                d.failed_at = CASE WHEN #{status} = 'failed' THEN #{statusAt} ELSE d.failed_at END,
                d.failure_reason = CASE WHEN #{status} = 'failed' THEN #{errorDetails} ELSE d.failure_reason END
            WHERE a.provider_message_id = #{messageId}
              AND (a.status_at IS NULL OR a.status_at <= #{statusAt})
            """)
    int updateDeliveryStatus(@Param("messageId") String messageId, @Param("status") String status,
                             @Param("statusAt") LocalDateTime statusAt,
                             @Param("errorDetails") String errorDetails);

    class NewAttempt {
        private Long id;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
    }

    class WhatsAppDeliveryRow {
        private Long deliveryId;
        private Long notificationId;
        private Integer attemptCount;
        private String destination;
        private String stage;
        private String tenantName;
        private String projectName;
        private String unitNo;
        private String leaseNo;
        private LocalDate billingMonth;
        private LocalDate dueDate;
        private BigDecimal outstandingAmount;
        private Integer overdueDays;
        public Long getDeliveryId() { return deliveryId; }
        public void setDeliveryId(Long value) { deliveryId = value; }
        public Long getNotificationId() { return notificationId; }
        public void setNotificationId(Long value) { notificationId = value; }
        public Integer getAttemptCount() { return attemptCount; }
        public void setAttemptCount(Integer value) { attemptCount = value; }
        public String getDestination() { return destination; }
        public void setDestination(String value) { destination = value; }
        public String getStage() { return stage; }
        public void setStage(String value) { stage = value; }
        public String getTenantName() { return tenantName; }
        public void setTenantName(String value) { tenantName = value; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String value) { projectName = value; }
        public String getUnitNo() { return unitNo; }
        public void setUnitNo(String value) { unitNo = value; }
        public String getLeaseNo() { return leaseNo; }
        public void setLeaseNo(String value) { leaseNo = value; }
        public LocalDate getBillingMonth() { return billingMonth; }
        public void setBillingMonth(LocalDate value) { billingMonth = value; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate value) { dueDate = value; }
        public BigDecimal getOutstandingAmount() { return outstandingAmount; }
        public void setOutstandingAmount(BigDecimal value) { outstandingAmount = value; }
        public Integer getOverdueDays() { return overdueDays; }
        public void setOverdueDays(Integer value) { overdueDays = value; }
    }
}
