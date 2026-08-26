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
public interface AdminRentCollectionWorkflowMapper {

    @Select("""
            SELECT ri.id AS invoice_id, l.id AS lease_id, t.id AS tenant_id,
                   t.user_id AS tenant_user_id, t.full_name AS tenant_name,
                   t.phone AS tenant_phone, t.email AS tenant_email,
                   COALESCE(ws.enabled, 0) AS whatsapp_enabled,
                   ws.destination AS whatsapp_destination,
                   p.name AS project_name, u.unit_no, l.lease_no, l.status AS lease_status,
                   ri.billing_month, ri.due_date, ri.amount_due, ri.amount_paid,
                   COALESCE(w.status, 'active') AS workflow_status, w.hold_reason,
                   (SELECT GROUP_CONCAT(a.stage ORDER BY a.threshold_days SEPARATOR ',')
                      FROM rent_collection_actions a
                     WHERE a.invoice_id = ri.id AND a.status = 'sent') AS sent_stages,
                   (SELECT a.stage FROM rent_collection_actions a
                     WHERE a.invoice_id = ri.id AND a.status = 'sent'
                     ORDER BY a.acted_at DESC, a.id DESC LIMIT 1) AS last_sent_stage,
                   (SELECT a.acted_at FROM rent_collection_actions a
                     WHERE a.invoice_id = ri.id AND a.status = 'sent'
                     ORDER BY a.acted_at DESC, a.id DESC LIMIT 1) AS last_sent_at
            FROM rent_invoices ri
            JOIN leases l ON l.id = ri.lease_id AND l.status = 'active'
            JOIN tenants t ON t.id = l.tenant_id AND t.status = 'active'
            JOIN units u ON u.id = l.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN tenant_whatsapp_subscriptions ws ON ws.tenant_id = t.id
            LEFT JOIN rent_collection_workflows w ON w.invoice_id = ri.id
            WHERE ri.amount_due > ri.amount_paid AND ri.due_date < #{today}
            ORDER BY ri.due_date, p.name, u.unit_no
            """)
    List<CollectionRow> findOutstanding(@Param("today") LocalDate today);

    @Select("""
            SELECT ri.id AS invoice_id, l.id AS lease_id, t.id AS tenant_id,
                   t.user_id AS tenant_user_id, t.full_name AS tenant_name,
                   t.phone AS tenant_phone, t.email AS tenant_email,
                   COALESCE(ws.enabled, 0) AS whatsapp_enabled,
                   ws.destination AS whatsapp_destination,
                   p.name AS project_name, u.unit_no, l.lease_no, l.status AS lease_status,
                   ri.billing_month, ri.due_date, ri.amount_due, ri.amount_paid,
                   COALESCE(w.status, 'active') AS workflow_status, w.hold_reason,
                   (SELECT GROUP_CONCAT(a.stage ORDER BY a.threshold_days SEPARATOR ',')
                      FROM rent_collection_actions a
                     WHERE a.invoice_id = ri.id AND a.status = 'sent') AS sent_stages,
                   (SELECT a.stage FROM rent_collection_actions a
                     WHERE a.invoice_id = ri.id AND a.status = 'sent'
                     ORDER BY a.acted_at DESC, a.id DESC LIMIT 1) AS last_sent_stage,
                   (SELECT a.acted_at FROM rent_collection_actions a
                     WHERE a.invoice_id = ri.id AND a.status = 'sent'
                     ORDER BY a.acted_at DESC, a.id DESC LIMIT 1) AS last_sent_at
            FROM rent_invoices ri
            JOIN leases l ON l.id = ri.lease_id
            JOIN tenants t ON t.id = l.tenant_id
            JOIN units u ON u.id = l.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN tenant_whatsapp_subscriptions ws ON ws.tenant_id = t.id
            LEFT JOIN rent_collection_workflows w ON w.invoice_id = ri.id
            WHERE ri.id = #{invoiceId}
            FOR UPDATE
            """)
    CollectionRow lockInvoice(@Param("invoiceId") Long invoiceId);

    @Insert("""
            INSERT INTO rent_collection_workflows (invoice_id, status)
            VALUES (#{invoiceId}, 'active')
            ON DUPLICATE KEY UPDATE invoice_id = VALUES(invoice_id)
            """)
    int ensureWorkflow(@Param("invoiceId") Long invoiceId);

    @Update("""
            UPDATE rent_collection_workflows
            SET status = 'on_hold', hold_reason = #{reason}, held_by = #{actorId},
                held_at = NOW(), resolved_at = NULL
            WHERE invoice_id = #{invoiceId}
            """)
    int hold(@Param("invoiceId") Long invoiceId, @Param("actorId") Long actorId,
             @Param("reason") String reason);

    @Update("""
            UPDATE rent_collection_workflows
            SET status = 'active', hold_reason = NULL, held_by = NULL, held_at = NULL
            WHERE invoice_id = #{invoiceId}
            """)
    int resume(@Param("invoiceId") Long invoiceId);

    @Select("""
            SELECT COUNT(*) FROM rent_collection_actions
            WHERE invoice_id = #{invoiceId} AND stage = #{stage} AND status = 'sent'
            """)
    int countSentStage(@Param("invoiceId") Long invoiceId, @Param("stage") String stage);

    @Insert("""
            INSERT INTO notifications
              (recipient_user_id, title, body, related_type, related_id, priority, status)
            VALUES
              (#{recipientUserId}, #{title}, #{body}, 'rent_invoice', #{invoiceId}, #{priority}, 'unread')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertNotification(NewNotification row);

    @Insert("""
            INSERT INTO notification_deliveries
              (notification_id, channel, destination, status, attempt_count, sent_at)
            SELECT #{notificationId}, 'in_app', NULL, 'sent', 1, NOW()
            FROM notifications WHERE id = #{notificationId} AND recipient_user_id IS NOT NULL
            """)
    int insertInAppDelivery(@Param("notificationId") Long notificationId);

    @Insert("""
            INSERT INTO notification_deliveries
              (notification_id, channel, destination, status, attempt_count)
            VALUES (#{notificationId}, 'email', #{destination}, 'pending', 0)
            ON DUPLICATE KEY UPDATE destination = VALUES(destination), status = 'pending'
            """)
    int insertEmailDelivery(@Param("notificationId") Long notificationId,
                            @Param("destination") String destination);

    @Insert("""
            INSERT INTO notification_deliveries
              (notification_id, channel, destination, status, attempt_count)
            SELECT #{notificationId}, 'whatsapp', destination, 'pending', 0
            FROM tenant_whatsapp_subscriptions
            WHERE tenant_id = #{tenantId} AND enabled = 1 AND opted_in_at IS NOT NULL
            ON DUPLICATE KEY UPDATE destination = VALUES(destination), status = 'pending',
              failed_at = NULL, failure_reason = NULL
            """)
    int insertWhatsAppDelivery(@Param("notificationId") Long notificationId,
                               @Param("tenantId") Long tenantId);

    @Insert("""
            INSERT INTO rent_collection_actions
              (invoice_id, stage, threshold_days, scheduled_date, status,
               title, body, notification_id, acted_by, acted_at)
            VALUES
              (#{invoiceId}, #{stage}, #{thresholdDays}, #{scheduledDate}, 'sent',
               #{title}, #{body}, #{notificationId}, #{actorId}, NOW())
            """)
    int insertAction(@Param("invoiceId") Long invoiceId, @Param("stage") String stage,
                     @Param("thresholdDays") int thresholdDays,
                     @Param("scheduledDate") LocalDate scheduledDate,
                     @Param("title") String title, @Param("body") String body,
                     @Param("notificationId") Long notificationId,
                     @Param("actorId") Long actorId);

    @Insert("""
            INSERT INTO audit_logs
              (actor_user_id, action, entity_type, entity_id, after_data)
            VALUES
              (#{actorId}, #{action}, 'rent_invoice', #{invoiceId},
               JSON_OBJECT('stage', #{stage}, 'detail', #{detail}))
            """)
    int insertAudit(@Param("actorId") Long actorId, @Param("invoiceId") Long invoiceId,
                    @Param("action") String action, @Param("stage") String stage,
                    @Param("detail") String detail);

    class NewNotification {
        private Long id, recipientUserId, invoiceId;
        private String title, body, priority;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public Long getRecipientUserId() { return recipientUserId; } public void setRecipientUserId(Long value) { recipientUserId = value; }
        public Long getInvoiceId() { return invoiceId; } public void setInvoiceId(Long value) { invoiceId = value; }
        public String getTitle() { return title; } public void setTitle(String value) { title = value; }
        public String getBody() { return body; } public void setBody(String value) { body = value; }
        public String getPriority() { return priority; } public void setPriority(String value) { priority = value; }
    }

    class CollectionRow {
        private Long invoiceId, leaseId, tenantId, tenantUserId;
        private String tenantName, tenantPhone, tenantEmail, whatsappDestination, projectName, unitNo, leaseNo, leaseStatus;
        private Boolean whatsappEnabled;
        private String workflowStatus, holdReason, sentStages, lastSentStage;
        private LocalDate billingMonth, dueDate;
        private LocalDateTime lastSentAt;
        private BigDecimal amountDue, amountPaid;
        public Long getInvoiceId() { return invoiceId; } public void setInvoiceId(Long v) { invoiceId = v; }
        public Long getLeaseId() { return leaseId; } public void setLeaseId(Long v) { leaseId = v; }
        public Long getTenantId() { return tenantId; } public void setTenantId(Long v) { tenantId = v; }
        public Long getTenantUserId() { return tenantUserId; } public void setTenantUserId(Long v) { tenantUserId = v; }
        public String getTenantName() { return tenantName; } public void setTenantName(String v) { tenantName = v; }
        public String getTenantPhone() { return tenantPhone; } public void setTenantPhone(String v) { tenantPhone = v; }
        public String getTenantEmail() { return tenantEmail; } public void setTenantEmail(String v) { tenantEmail = v; }
        public String getWhatsappDestination() { return whatsappDestination; } public void setWhatsappDestination(String v) { whatsappDestination = v; }
        public Boolean getWhatsappEnabled() { return whatsappEnabled; } public void setWhatsappEnabled(Boolean v) { whatsappEnabled = v; }
        public String getProjectName() { return projectName; } public void setProjectName(String v) { projectName = v; }
        public String getUnitNo() { return unitNo; } public void setUnitNo(String v) { unitNo = v; }
        public String getLeaseNo() { return leaseNo; } public void setLeaseNo(String v) { leaseNo = v; }
        public String getLeaseStatus() { return leaseStatus; } public void setLeaseStatus(String v) { leaseStatus = v; }
        public String getWorkflowStatus() { return workflowStatus; } public void setWorkflowStatus(String v) { workflowStatus = v; }
        public String getHoldReason() { return holdReason; } public void setHoldReason(String v) { holdReason = v; }
        public String getSentStages() { return sentStages; } public void setSentStages(String v) { sentStages = v; }
        public String getLastSentStage() { return lastSentStage; } public void setLastSentStage(String v) { lastSentStage = v; }
        public LocalDate getBillingMonth() { return billingMonth; } public void setBillingMonth(LocalDate v) { billingMonth = v; }
        public LocalDate getDueDate() { return dueDate; } public void setDueDate(LocalDate v) { dueDate = v; }
        public LocalDateTime getLastSentAt() { return lastSentAt; } public void setLastSentAt(LocalDateTime v) { lastSentAt = v; }
        public BigDecimal getAmountDue() { return amountDue; } public void setAmountDue(BigDecimal v) { amountDue = v; }
        public BigDecimal getAmountPaid() { return amountPaid; } public void setAmountPaid(BigDecimal v) { amountPaid = v; }
    }
}
