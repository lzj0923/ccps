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

@Mapper
public interface SystemFinancialNotificationMapper {

    String BUILDING_COLUMNS = """
            SELECT pi.id AS related_id, pi.due_date, pi.amount_due, pi.amount_paid,
                   o.user_id AS recipient_user_id, o.id AS recipient_owner_id,
                   o.full_name AS recipient_name, o.email AS recipient_email,
                   p.name AS project_name, u.unit_no,
                   COALESCE(pi.milestone, CONCAT('第 ', pi.installment_no, ' 期')) AS label,
                   (SELECT GROUP_CONCAT(a.stage ORDER BY a.sent_at SEPARATOR ',')
                      FROM system_financial_notification_actions a
                     WHERE a.action_type = 'building_payment' AND a.related_id = pi.id
                       AND a.period_key = DATE_FORMAT(pi.due_date, '%Y-%m-%d')
                       AND a.status = 'sent') AS sent_stages
            FROM payment_installments pi
            JOIN payment_plans pp ON pp.id = pi.payment_plan_id AND pp.status = 'active'
            JOIN purchase_contracts pc ON pc.id = pp.purchase_contract_id AND pc.status = 'active'
            JOIN owner_units ou ON ou.id = pc.owner_unit_id AND ou.status = 'active'
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id AND p.status = 'active'
            """;

    @Select(BUILDING_COLUMNS + """
            WHERE pi.due_date IS NOT NULL AND pi.amount_due > pi.amount_paid
              AND pi.due_date <= DATE_ADD(#{today}, INTERVAL 7 DAY)
            ORDER BY pi.due_date, pi.id
            """)
    List<BuildingPaymentRow> findBuildingPaymentsDue(@Param("today") LocalDate today);

    @Select(BUILDING_COLUMNS + " WHERE pi.id = #{relatedId} FOR UPDATE")
    BuildingPaymentRow lockBuildingPayment(@Param("relatedId") Long relatedId);

    String RESERVE_COLUMNS = """
            SELECT ra.id AS related_id, ra.current_balance, ra.minimum_balance,
                   ra.low_balance_alert_enabled,
                   o.user_id AS recipient_user_id, o.id AS recipient_owner_id,
                   o.full_name AS recipient_name, o.email AS recipient_email,
                   p.name AS project_name, u.unit_no,
                   (SELECT MAX(a.sent_at) FROM system_financial_notification_actions a
                     WHERE a.action_type = 'reserve' AND a.related_id = ra.id
                       AND a.stage = 'reserve_low' AND a.status = 'sent') AS last_sent_at
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id = ra.owner_unit_id AND ou.status = 'active'
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id AND p.status = 'active'
            """;

    @Select(RESERVE_COLUMNS + """
            WHERE ra.status = 'active' AND ra.low_balance_alert_enabled = 1
              AND ra.current_balance < ra.minimum_balance
            ORDER BY p.name, u.unit_no, ra.id
            """)
    List<ReserveRow> findLowReserves();

    @Select(RESERVE_COLUMNS + " WHERE ra.id = #{relatedId} FOR UPDATE")
    ReserveRow lockReserve(@Param("relatedId") Long relatedId);

    @Select("""
            SELECT COUNT(*) FROM system_financial_notification_actions
            WHERE action_type = #{actionType} AND related_id = #{relatedId}
              AND stage = #{stage} AND period_key = #{periodKey} AND status = 'sent'
            """)
    int countSentAction(@Param("actionType") String actionType, @Param("relatedId") Long relatedId,
                        @Param("stage") String stage, @Param("periodKey") String periodKey);

    @Insert("""
            INSERT INTO notifications
              (recipient_user_id, recipient_owner_id, title, body,
               related_type, related_id, priority, status)
            VALUES
              (#{recipientUserId}, #{recipientOwnerId}, #{title}, #{body},
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
            VALUES (#{notificationId}, 'email', #{destination}, 'pending', 0)
            ON DUPLICATE KEY UPDATE destination = VALUES(destination), status = 'pending'
            """)
    int insertEmailDelivery(@Param("notificationId") Long notificationId,
                            @Param("destination") String destination);

    @Insert("""
            INSERT INTO system_financial_notification_actions
              (action_type, related_id, stage, period_key, scheduled_date,
               status, notification_id, title, body, sent_at)
            VALUES
              (#{actionType}, #{relatedId}, #{stage}, #{periodKey}, #{scheduledDate},
               'sent', #{notificationId}, #{title}, #{body}, NOW())
            """)
    int insertAction(@Param("actionType") String actionType, @Param("relatedId") Long relatedId,
                     @Param("stage") String stage, @Param("periodKey") String periodKey,
                     @Param("scheduledDate") LocalDate scheduledDate,
                     @Param("notificationId") Long notificationId,
                     @Param("title") String title, @Param("body") String body);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, after_data)
            VALUES (NULL, 'send_system_financial_notification', #{entityType}, #{relatedId},
                    JSON_OBJECT('stage', #{stage}, 'periodKey', #{periodKey}, 'title', #{title}))
            """)
    int insertAudit(@Param("entityType") String entityType, @Param("relatedId") Long relatedId,
                    @Param("stage") String stage, @Param("periodKey") String periodKey,
                    @Param("title") String title);

    class BuildingPaymentRow {
        private Long relatedId, recipientUserId, recipientOwnerId;
        private LocalDate dueDate;
        private BigDecimal amountDue, amountPaid;
        private String recipientName, recipientEmail, projectName, unitNo, label, sentStages;
        public Long getRelatedId() { return relatedId; } public void setRelatedId(Long v) { relatedId = v; }
        public Long getRecipientUserId() { return recipientUserId; } public void setRecipientUserId(Long v) { recipientUserId = v; }
        public Long getRecipientOwnerId() { return recipientOwnerId; } public void setRecipientOwnerId(Long v) { recipientOwnerId = v; }
        public LocalDate getDueDate() { return dueDate; } public void setDueDate(LocalDate v) { dueDate = v; }
        public BigDecimal getAmountDue() { return amountDue; } public void setAmountDue(BigDecimal v) { amountDue = v; }
        public BigDecimal getAmountPaid() { return amountPaid; } public void setAmountPaid(BigDecimal v) { amountPaid = v; }
        public String getRecipientName() { return recipientName; } public void setRecipientName(String v) { recipientName = v; }
        public String getRecipientEmail() { return recipientEmail; } public void setRecipientEmail(String v) { recipientEmail = v; }
        public String getProjectName() { return projectName; } public void setProjectName(String v) { projectName = v; }
        public String getUnitNo() { return unitNo; } public void setUnitNo(String v) { unitNo = v; }
        public String getLabel() { return label; } public void setLabel(String v) { label = v; }
        public String getSentStages() { return sentStages; } public void setSentStages(String v) { sentStages = v; }
    }

    class ReserveRow {
        private Long relatedId, recipientUserId, recipientOwnerId;
        private BigDecimal currentBalance, minimumBalance;
        private Boolean lowBalanceAlertEnabled;
        private LocalDateTime lastSentAt;
        private String recipientName, recipientEmail, projectName, unitNo;
        public Long getRelatedId() { return relatedId; } public void setRelatedId(Long v) { relatedId = v; }
        public Long getRecipientUserId() { return recipientUserId; } public void setRecipientUserId(Long v) { recipientUserId = v; }
        public Long getRecipientOwnerId() { return recipientOwnerId; } public void setRecipientOwnerId(Long v) { recipientOwnerId = v; }
        public BigDecimal getCurrentBalance() { return currentBalance; } public void setCurrentBalance(BigDecimal v) { currentBalance = v; }
        public BigDecimal getMinimumBalance() { return minimumBalance; } public void setMinimumBalance(BigDecimal v) { minimumBalance = v; }
        public Boolean getLowBalanceAlertEnabled() { return lowBalanceAlertEnabled; } public void setLowBalanceAlertEnabled(Boolean v) { lowBalanceAlertEnabled = v; }
        public LocalDateTime getLastSentAt() { return lastSentAt; } public void setLastSentAt(LocalDateTime v) { lastSentAt = v; }
        public String getRecipientName() { return recipientName; } public void setRecipientName(String v) { recipientName = v; }
        public String getRecipientEmail() { return recipientEmail; } public void setRecipientEmail(String v) { recipientEmail = v; }
        public String getProjectName() { return projectName; } public void setProjectName(String v) { projectName = v; }
        public String getUnitNo() { return unitNo; } public void setUnitNo(String v) { unitNo = v; }
    }

    class NewNotification {
        private Long id, recipientUserId, recipientOwnerId, relatedId;
        private String title, body, relatedType, priority;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public Long getRecipientUserId() { return recipientUserId; } public void setRecipientUserId(Long v) { recipientUserId = v; }
        public Long getRecipientOwnerId() { return recipientOwnerId; } public void setRecipientOwnerId(Long v) { recipientOwnerId = v; }
        public Long getRelatedId() { return relatedId; } public void setRelatedId(Long v) { relatedId = v; }
        public String getTitle() { return title; } public void setTitle(String v) { title = v; }
        public String getBody() { return body; } public void setBody(String v) { body = v; }
        public String getRelatedType() { return relatedType; } public void setRelatedType(String v) { relatedType = v; }
        public String getPriority() { return priority; } public void setPriority(String v) { priority = v; }
    }
}
