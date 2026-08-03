package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminReminderMapper {

    @Select("""
            SELECT (SELECT COUNT(*) FROM notification_rules) AS rule_count,
                   (SELECT COUNT(*) FROM notification_rules WHERE enabled = 1) AS enabled_rule_count,
                   (SELECT COUNT(*) FROM notifications WHERE rule_id IS NOT NULL) AS notification_count,
                   (SELECT COUNT(*) FROM notification_deliveries d JOIN notifications n ON n.id = d.notification_id
                    WHERE n.rule_id IS NOT NULL AND d.status = 'pending') AS pending_delivery_count,
                   (SELECT COUNT(*) FROM notification_deliveries d JOIN notifications n ON n.id = d.notification_id
                    WHERE n.rule_id IS NOT NULL AND d.status = 'failed') AS failed_delivery_count
            """)
    SummaryRow findSummary();

    @Select("""
            SELECT id, code, name, event_type, CAST(channels AS CHAR) AS channels_json,
                   days_before, recipient_role, enabled, created_at, updated_at
            FROM notification_rules
            ORDER BY enabled DESC, updated_at DESC, id DESC
            """)
    List<RuleRow> findRules();

    @Select("""
            SELECT id, code, name, event_type, CAST(channels AS CHAR) AS channels_json,
                   days_before, recipient_role, enabled, created_at, updated_at
            FROM notification_rules WHERE id = #{ruleId}
            """)
    RuleRow findRule(@Param("ruleId") Long ruleId);

    @Select("SELECT COUNT(*) FROM notification_rules WHERE code = #{code} AND (#{excludeId} IS NULL OR id <> #{excludeId})")
    int countRuleCode(@Param("code") String code, @Param("excludeId") Long excludeId);

    @Insert("""
            INSERT INTO notification_rules
              (code, name, event_type, days_before, channels, recipient_role, enabled, created_by)
            VALUES
              (#{code}, #{name}, #{eventType}, #{daysBefore}, CAST(#{channelsJson} AS JSON),
               #{recipientRole}, #{enabled}, #{createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRule(RuleWrite row);

    @Update("""
            UPDATE notification_rules
            SET code = #{code}, name = #{name}, event_type = #{eventType},
                days_before = #{daysBefore}, channels = CAST(#{channelsJson} AS JSON),
                recipient_role = #{recipientRole}, enabled = #{enabled}
            WHERE id = #{id}
            """)
    int updateRule(RuleWrite row);

    @Update("UPDATE notification_rules SET enabled = #{enabled} WHERE id = #{ruleId}")
    int setRuleEnabled(@Param("ruleId") Long ruleId, @Param("enabled") boolean enabled);

    @Delete("DELETE FROM notification_rules WHERE id = #{ruleId} AND NOT EXISTS (SELECT 1 FROM notifications WHERE rule_id = #{ruleId})")
    int deleteUnusedRule(@Param("ruleId") Long ruleId);

    @Select("""
            SELECT n.id, n.rule_id, r.name AS rule_name, r.event_type, n.title, n.body,
                   o.full_name AS recipient_name, COALESCE(s.destination, o.email, u.email) AS recipient_email,
                   n.related_type, n.related_id, n.priority, n.status AS notice_status,
                   COALESCE(GROUP_CONCAT(CONCAT(d.channel, ':', d.status) ORDER BY d.channel SEPARATOR ','), 'in_app:sent') AS delivery_summary,
                   MAX(d.failure_reason) AS failure_reason, n.created_at
            FROM notifications n
            JOIN notification_rules r ON r.id = n.rule_id
            LEFT JOIN owners o ON o.id = n.recipient_owner_id
            LEFT JOIN users u ON u.id = n.recipient_user_id
            LEFT JOIN notification_subscriptions s ON s.user_id = COALESCE(n.recipient_user_id, o.user_id)
              AND s.channel = 'email'
            LEFT JOIN notification_deliveries d ON d.notification_id = n.id
            GROUP BY n.id, n.rule_id, r.name, r.event_type, n.title, n.body, o.full_name,
                     s.destination, o.email, u.email, n.related_type, n.related_id, n.priority,
                     n.status, n.created_at
            ORDER BY n.created_at DESC, n.id DESC
            LIMIT 200
            """)
    List<NotificationRow> findNotifications();

    @Select("""
            SELECT d.id, d.notification_id, r.name AS rule_name, n.title,
                   o.full_name AS recipient_name, d.channel, d.destination, d.status,
                   d.attempt_count, d.sent_at, d.failed_at, d.failure_reason
            FROM notification_deliveries d
            JOIN notifications n ON n.id = d.notification_id
            LEFT JOIN notification_rules r ON r.id = n.rule_id
            LEFT JOIN owners o ON o.id = n.recipient_owner_id
            WHERE n.rule_id IS NOT NULL
            ORDER BY COALESCE(d.sent_at, d.failed_at, n.created_at) DESC, d.id DESC
            LIMIT 300
            """)
    List<DeliveryRow> findDeliveries();

    @Select("""
            SELECT pi.id AS related_id, 'payment_installment' AS related_type,
                   o.user_id AS recipient_user_id, o.id AS recipient_owner_id,
                   o.full_name AS recipient_name, o.email AS recipient_email,
                   p.name AS project_name, u.unit_no, pi.due_date,
                   GREATEST(pi.amount_due - pi.amount_paid, 0) AS amount,
                   COALESCE(pi.milestone, CONCAT('第 ', pi.installment_no, ' 期')) AS label
            FROM payment_installments pi
            JOIN payment_plans pp ON pp.id = pi.payment_plan_id AND pp.status = 'active'
            JOIN purchase_contracts pc ON pc.id = pp.purchase_contract_id AND pc.status = 'active'
            JOIN owner_units ou ON ou.id = pc.owner_unit_id AND ou.status = 'active'
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE pi.due_date IS NOT NULL AND pi.amount_due > pi.amount_paid
              AND pi.due_date <= DATE_ADD(CURDATE(), INTERVAL #{daysBefore} DAY)
              AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.rule_id = #{ruleId}
                  AND n.related_type = 'payment_installment' AND n.related_id = pi.id
                  AND n.recipient_owner_id = o.id)
            """)
    List<EventContext> findPaymentDueEvents(@Param("ruleId") Long ruleId, @Param("daysBefore") int daysBefore);

    @Select("""
            SELECT ri.id AS related_id, 'rent_invoice' AS related_type,
                   o.user_id AS recipient_user_id, o.id AS recipient_owner_id,
                   o.full_name AS recipient_name, o.email AS recipient_email,
                   p.name AS project_name, u.unit_no,
                   GREATEST(ri.due_date, DATE_ADD(l.start_date, INTERVAL 7 DAY)) AS due_date,
                   GREATEST(ri.amount_due - ri.amount_paid, 0) AS amount,
                   DATE_FORMAT(ri.billing_month, '%Y-%m') AS label
            FROM rent_invoices ri
            JOIN leases l ON l.id = ri.lease_id AND l.status = 'active'
            JOIN units u ON u.id = l.unit_id
            JOIN projects p ON p.id = u.project_id
            JOIN owner_units ou ON ou.unit_id = u.id AND ou.status = 'active'
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            WHERE ri.due_date IS NOT NULL AND ri.amount_due > ri.amount_paid
              AND GREATEST(ri.due_date, DATE_ADD(l.start_date, INTERVAL 7 DAY)) <= DATE_ADD(CURDATE(), INTERVAL #{daysBefore} DAY)
              AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.rule_id = #{ruleId}
                  AND n.related_type = 'rent_invoice' AND n.related_id = ri.id
                  AND n.recipient_owner_id = o.id)
            """)
    List<EventContext> findRentDueEvents(@Param("ruleId") Long ruleId, @Param("daysBefore") int daysBefore);

    @Select("""
            SELECT l.id AS related_id, 'lease' AS related_type,
                   o.user_id AS recipient_user_id, o.id AS recipient_owner_id,
                   o.full_name AS recipient_name, o.email AS recipient_email,
                   p.name AS project_name, u.unit_no, l.end_date AS due_date,
                   l.monthly_rent AS amount, l.lease_no AS label
            FROM leases l
            JOIN units u ON u.id = l.unit_id
            JOIN projects p ON p.id = u.project_id
            JOIN owner_units ou ON ou.unit_id = u.id AND ou.status = 'active'
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            WHERE l.status = 'active' AND l.end_date IS NOT NULL
              AND l.end_date <= DATE_ADD(CURDATE(), INTERVAL #{daysBefore} DAY)
              AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.rule_id = #{ruleId}
                  AND n.related_type = 'lease' AND n.related_id = l.id
                  AND n.recipient_owner_id = o.id)
            """)
    List<EventContext> findLeaseExpiryEvents(@Param("ruleId") Long ruleId, @Param("daysBefore") int daysBefore);

    @Select("""
            SELECT ra.id AS related_id, 'reserve_account' AS related_type,
                   o.user_id AS recipient_user_id, o.id AS recipient_owner_id,
                   o.full_name AS recipient_name, o.email AS recipient_email,
                   p.name AS project_name, u.unit_no, CURDATE() AS due_date,
                   ra.current_balance AS amount, CAST(ra.minimum_balance AS CHAR) AS label
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id = ra.owner_unit_id AND ou.status = 'active'
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE ra.status = 'active' AND ra.low_balance_alert_enabled = 1
              AND ra.current_balance < ra.minimum_balance
              AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.rule_id = #{ruleId}
                  AND n.related_type = 'reserve_account' AND n.related_id = ra.id
                  AND n.recipient_owner_id = o.id
                  AND n.created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY))
            """)
    List<EventContext> findReserveLowEvents(@Param("ruleId") Long ruleId);

    @Select("""
            SELECT DISTINCT d.id AS related_id, 'document' AS related_type,
                   o.user_id AS recipient_user_id, o.id AS recipient_owner_id,
                   o.full_name AS recipient_name, o.email AS recipient_email,
                   p.name AS project_name, u.unit_no, DATE(d.expires_at) AS due_date,
                   NULL AS amount, d.original_name AS label
            FROM documents d
            JOIN document_links dl ON dl.document_id = d.id
            LEFT JOIN leases l ON dl.entity_type = 'lease' AND l.id = dl.entity_id
            LEFT JOIN units u ON u.id = CASE WHEN dl.entity_type = 'unit' THEN dl.entity_id ELSE l.unit_id END
            LEFT JOIN projects p ON p.id = u.project_id
            LEFT JOIN owner_units ou ON ou.unit_id = u.id AND ou.status = 'active'
            JOIN owners o ON o.id = CASE WHEN dl.entity_type = 'owner' THEN dl.entity_id ELSE ou.owner_id END
              AND o.status = 'active'
            WHERE d.status NOT IN ('archived', 'superseded', 'rejected') AND d.expires_at IS NOT NULL
              AND DATE(d.expires_at) <= DATE_ADD(CURDATE(), INTERVAL #{daysBefore} DAY)
              AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.rule_id = #{ruleId}
                  AND n.related_type = 'document' AND n.related_id = d.id
                  AND n.recipient_owner_id = o.id)
            """)
    List<EventContext> findDocumentExpiryEvents(@Param("ruleId") Long ruleId, @Param("daysBefore") int daysBefore);

    @Insert("""
            INSERT INTO notifications
              (rule_id, recipient_user_id, recipient_owner_id, title, body,
               related_type, related_id, priority, status)
            VALUES
              (#{ruleId}, #{recipientUserId}, #{recipientOwnerId}, #{title}, #{body},
               #{relatedType}, #{relatedId}, #{priority}, 'unread')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertNotification(NewNotification row);

    @Insert("""
            INSERT INTO notification_deliveries
              (notification_id, channel, destination, status, attempt_count, sent_at)
            VALUES (#{notificationId}, 'in_app', NULL, 'sent', 1, NOW())
            ON DUPLICATE KEY UPDATE status = 'sent', sent_at = COALESCE(sent_at, NOW())
            """)
    int insertInAppDelivery(@Param("notificationId") Long notificationId);

    @Insert("""
            INSERT INTO notification_deliveries
              (notification_id, channel, destination, status, attempt_count)
            SELECT #{notificationId}, 'email', destination, 'pending', 0
            FROM notification_subscriptions
            WHERE user_id = #{userId} AND channel = 'email' AND enabled = 1 AND verified_at IS NOT NULL
            ON DUPLICATE KEY UPDATE destination = VALUES(destination), status = 'pending'
            """)
    int insertEmailDelivery(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

    @Insert("""
            INSERT INTO notification_deliveries
              (notification_id, channel, destination, status, attempt_count, failed_at, failure_reason)
            VALUES (#{notificationId}, 'email', #{destination}, 'failed', 0, NOW(),
                    '收件人尚未啟用並驗證郵件通知')
            ON DUPLICATE KEY UPDATE status = 'failed', failed_at = NOW(),
              failure_reason = VALUES(failure_reason)
            """)
    int insertUnavailableEmailDelivery(@Param("notificationId") Long notificationId,
                                       @Param("destination") String destination);

    @Insert("""
            INSERT INTO notification_deliveries
              (notification_id, channel, destination, status, attempt_count, failed_at, failure_reason)
            VALUES (#{notificationId}, 'line', NULL, 'failed', 0, NOW(), 'LINE 通知尚未設定')
            ON DUPLICATE KEY UPDATE status = 'failed', failed_at = NOW(), failure_reason = VALUES(failure_reason)
            """)
    int insertUnavailableLineDelivery(@Param("notificationId") Long notificationId);

    @Select("SELECT channel FROM notification_deliveries WHERE id = #{deliveryId}")
    String findDeliveryChannel(@Param("deliveryId") Long deliveryId);

    @Select("""
            SELECT COUNT(*)
            FROM notification_deliveries d
            JOIN notifications n ON n.id = d.notification_id
            LEFT JOIN owners o ON o.id = n.recipient_owner_id
            JOIN notification_subscriptions s ON s.user_id = COALESCE(n.recipient_user_id, o.user_id)
              AND s.channel = 'email' AND s.enabled = 1 AND s.verified_at IS NOT NULL
            WHERE d.id = #{deliveryId} AND d.channel = 'email'
            """)
    int isEmailDeliveryReady(@Param("deliveryId") Long deliveryId);

    @Update("""
            UPDATE notification_deliveries SET status = 'pending', failed_at = NULL, failure_reason = NULL
            WHERE id = #{deliveryId} AND channel = 'email' AND status = 'failed'
            """)
    int retryEmailDelivery(@Param("deliveryId") Long deliveryId);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, after_data)
            VALUES (#{actorId}, #{action}, 'notification_rule', #{ruleId},
                    JSON_OBJECT('code', #{code}, 'name', #{name}))
            """)
    int insertRuleAudit(@Param("actorId") Long actorId, @Param("action") String action,
                        @Param("ruleId") Long ruleId, @Param("code") String code,
                        @Param("name") String name);

    class SummaryRow {
        private Long ruleCount, enabledRuleCount, notificationCount, pendingDeliveryCount, failedDeliveryCount;
        public Long getRuleCount() { return ruleCount; } public void setRuleCount(Long v) { ruleCount = v; }
        public Long getEnabledRuleCount() { return enabledRuleCount; } public void setEnabledRuleCount(Long v) { enabledRuleCount = v; }
        public Long getNotificationCount() { return notificationCount; } public void setNotificationCount(Long v) { notificationCount = v; }
        public Long getPendingDeliveryCount() { return pendingDeliveryCount; } public void setPendingDeliveryCount(Long v) { pendingDeliveryCount = v; }
        public Long getFailedDeliveryCount() { return failedDeliveryCount; } public void setFailedDeliveryCount(Long v) { failedDeliveryCount = v; }
    }

    class RuleRow {
        private Long id; private String code, name, eventType, channelsJson, recipientRole;
        private Integer daysBefore; private Boolean enabled; private LocalDateTime createdAt, updatedAt;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public String getCode() { return code; } public void setCode(String v) { code = v; }
        public String getName() { return name; } public void setName(String v) { name = v; }
        public String getEventType() { return eventType; } public void setEventType(String v) { eventType = v; }
        public String getChannelsJson() { return channelsJson; } public void setChannelsJson(String v) { channelsJson = v; }
        public String getRecipientRole() { return recipientRole; } public void setRecipientRole(String v) { recipientRole = v; }
        public Integer getDaysBefore() { return daysBefore; } public void setDaysBefore(Integer v) { daysBefore = v; }
        public Boolean getEnabled() { return enabled; } public void setEnabled(Boolean v) { enabled = v; }
        public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt = v; }
        public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
    }

    class RuleWrite {
        private Long id, createdBy; private String code, name, eventType, channelsJson, recipientRole;
        private int daysBefore; private boolean enabled;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public Long getCreatedBy() { return createdBy; } public void setCreatedBy(Long v) { createdBy = v; }
        public String getCode() { return code; } public void setCode(String v) { code = v; }
        public String getName() { return name; } public void setName(String v) { name = v; }
        public String getEventType() { return eventType; } public void setEventType(String v) { eventType = v; }
        public String getChannelsJson() { return channelsJson; } public void setChannelsJson(String v) { channelsJson = v; }
        public String getRecipientRole() { return recipientRole; } public void setRecipientRole(String v) { recipientRole = v; }
        public int getDaysBefore() { return daysBefore; } public void setDaysBefore(int v) { daysBefore = v; }
        public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { enabled = v; }
    }

    class EventContext {
        private Long relatedId, recipientUserId, recipientOwnerId;
        private String relatedType, recipientName, recipientEmail, projectName, unitNo, label;
        private LocalDate dueDate; private BigDecimal amount;
        public Long getRelatedId() { return relatedId; } public void setRelatedId(Long v) { relatedId = v; }
        public Long getRecipientUserId() { return recipientUserId; } public void setRecipientUserId(Long v) { recipientUserId = v; }
        public Long getRecipientOwnerId() { return recipientOwnerId; } public void setRecipientOwnerId(Long v) { recipientOwnerId = v; }
        public String getRelatedType() { return relatedType; } public void setRelatedType(String v) { relatedType = v; }
        public String getRecipientName() { return recipientName; } public void setRecipientName(String v) { recipientName = v; }
        public String getRecipientEmail() { return recipientEmail; } public void setRecipientEmail(String v) { recipientEmail = v; }
        public String getProjectName() { return projectName; } public void setProjectName(String v) { projectName = v; }
        public String getUnitNo() { return unitNo; } public void setUnitNo(String v) { unitNo = v; }
        public String getLabel() { return label; } public void setLabel(String v) { label = v; }
        public LocalDate getDueDate() { return dueDate; } public void setDueDate(LocalDate v) { dueDate = v; }
        public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { amount = v; }
    }

    class NewNotification {
        private Long id, ruleId, recipientUserId, recipientOwnerId, relatedId;
        private String title, body, relatedType, priority;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public Long getRuleId() { return ruleId; } public void setRuleId(Long v) { ruleId = v; }
        public Long getRecipientUserId() { return recipientUserId; } public void setRecipientUserId(Long v) { recipientUserId = v; }
        public Long getRecipientOwnerId() { return recipientOwnerId; } public void setRecipientOwnerId(Long v) { recipientOwnerId = v; }
        public Long getRelatedId() { return relatedId; } public void setRelatedId(Long v) { relatedId = v; }
        public String getTitle() { return title; } public void setTitle(String v) { title = v; }
        public String getBody() { return body; } public void setBody(String v) { body = v; }
        public String getRelatedType() { return relatedType; } public void setRelatedType(String v) { relatedType = v; }
        public String getPriority() { return priority; } public void setPriority(String v) { priority = v; }
    }

    class NotificationRow {
        private Long id, ruleId, relatedId; private String ruleName, eventType, title, body,
                recipientName, recipientEmail, relatedType, priority, noticeStatus,
                deliverySummary, failureReason; private LocalDateTime createdAt;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public Long getRuleId() { return ruleId; } public void setRuleId(Long v) { ruleId = v; }
        public Long getRelatedId() { return relatedId; } public void setRelatedId(Long v) { relatedId = v; }
        public String getRuleName() { return ruleName; } public void setRuleName(String v) { ruleName = v; }
        public String getEventType() { return eventType; } public void setEventType(String v) { eventType = v; }
        public String getTitle() { return title; } public void setTitle(String v) { title = v; }
        public String getBody() { return body; } public void setBody(String v) { body = v; }
        public String getRecipientName() { return recipientName; } public void setRecipientName(String v) { recipientName = v; }
        public String getRecipientEmail() { return recipientEmail; } public void setRecipientEmail(String v) { recipientEmail = v; }
        public String getRelatedType() { return relatedType; } public void setRelatedType(String v) { relatedType = v; }
        public String getPriority() { return priority; } public void setPriority(String v) { priority = v; }
        public String getNoticeStatus() { return noticeStatus; } public void setNoticeStatus(String v) { noticeStatus = v; }
        public String getDeliverySummary() { return deliverySummary; } public void setDeliverySummary(String v) { deliverySummary = v; }
        public String getFailureReason() { return failureReason; } public void setFailureReason(String v) { failureReason = v; }
        public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    }

    class DeliveryRow {
        private Long id, notificationId; private String ruleName, title, recipientName, channel,
                destination, status, failureReason; private Integer attemptCount;
        private LocalDateTime sentAt, failedAt;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public Long getNotificationId() { return notificationId; } public void setNotificationId(Long v) { notificationId = v; }
        public String getRuleName() { return ruleName; } public void setRuleName(String v) { ruleName = v; }
        public String getTitle() { return title; } public void setTitle(String v) { title = v; }
        public String getRecipientName() { return recipientName; } public void setRecipientName(String v) { recipientName = v; }
        public String getChannel() { return channel; } public void setChannel(String v) { channel = v; }
        public String getDestination() { return destination; } public void setDestination(String v) { destination = v; }
        public String getStatus() { return status; } public void setStatus(String v) { status = v; }
        public String getFailureReason() { return failureReason; } public void setFailureReason(String v) { failureReason = v; }
        public Integer getAttemptCount() { return attemptCount; } public void setAttemptCount(Integer v) { attemptCount = v; }
        public LocalDateTime getSentAt() { return sentAt; } public void setSentAt(LocalDateTime v) { sentAt = v; }
        public LocalDateTime getFailedAt() { return failedAt; } public void setFailedAt(LocalDateTime v) { failedAt = v; }
    }
}
