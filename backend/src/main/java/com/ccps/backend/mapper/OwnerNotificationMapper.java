package com.ccps.backend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OwnerNotificationMapper {

    @Select("""
            SELECT user_id, destination, enabled, verified_at, verification_code_hash,
                   verification_expires_at, last_verification_sent_at
            FROM notification_subscriptions
            WHERE user_id = #{userId} AND channel = 'email'
            """)
    EmailSubscriptionRow findEmailSubscription(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO notification_subscriptions
              (user_id, channel, destination, enabled, verification_code_hash,
               verification_expires_at, last_verification_sent_at)
            VALUES
              (#{userId}, 'email', #{destination}, 0, #{codeHash},
               DATE_ADD(NOW(), INTERVAL 10 MINUTE), NOW())
            ON DUPLICATE KEY UPDATE destination = VALUES(destination), enabled = 0,
              verified_at = NULL, verification_code_hash = VALUES(verification_code_hash),
              verification_expires_at = VALUES(verification_expires_at),
              last_verification_sent_at = NOW()
            """)
    int saveEmailVerification(@Param("userId") Long userId,
                              @Param("destination") String destination,
                              @Param("codeHash") String codeHash);

    @Update("""
            UPDATE notification_subscriptions
            SET enabled = 1, verified_at = NOW(), verification_code_hash = NULL,
                verification_expires_at = NULL
            WHERE user_id = #{userId} AND channel = 'email' AND destination = #{destination}
            """)
    int verifyEmailSubscription(@Param("userId") Long userId,
                                @Param("destination") String destination);

    @Update("""
            UPDATE notification_subscriptions
            SET enabled = #{enabled}
            WHERE user_id = #{userId} AND channel = 'email' AND verified_at IS NOT NULL
            """)
    int toggleEmailSubscription(@Param("userId") Long userId,
                                @Param("enabled") boolean enabled);

    @Select("""
            SELECT DISTINCT n.id AS notification_id, s.user_id, s.destination, n.title, n.body
            FROM notification_subscriptions s
            JOIN notifications n ON n.recipient_user_id = s.user_id
              OR n.recipient_owner_id IN (SELECT id FROM owners WHERE user_id = s.user_id)
            LEFT JOIN notification_deliveries d
              ON d.notification_id = n.id AND d.channel = 'email'
            WHERE s.channel = 'email' AND s.enabled = 1 AND s.verified_at IS NOT NULL
              AND (n.created_at >= s.verified_at OR d.status = 'pending')
              AND (d.id IS NULL OR d.status = 'pending' OR (d.status = 'failed' AND d.attempt_count < 3))
            ORDER BY n.id
            LIMIT 50
            """)
    List<EmailDeliveryRow> findPendingEmailDeliveries();

    @Insert("""
            INSERT INTO notification_deliveries
              (notification_id, channel, destination, status, attempt_count)
            VALUES (#{notificationId}, 'email', #{destination}, 'pending', 0)
            ON DUPLICATE KEY UPDATE destination = VALUES(destination), status = 'pending'
            """)
    int claimEmailDelivery(@Param("notificationId") Long notificationId,
                           @Param("destination") String destination);

    @Update("""
            UPDATE notification_deliveries
            SET status = 'sent', attempt_count = attempt_count + 1,
                sent_at = NOW(), failed_at = NULL, failure_reason = NULL
            WHERE notification_id = #{notificationId} AND channel = 'email'
            """)
    int markEmailDeliverySent(@Param("notificationId") Long notificationId);

    @Update("""
            UPDATE notification_deliveries
            SET status = 'failed', attempt_count = attempt_count + 1,
                failed_at = NOW(), failure_reason = LEFT(#{reason}, 500)
            WHERE notification_id = #{notificationId} AND channel = 'email'
            """)
    int markEmailDeliveryFailed(@Param("notificationId") Long notificationId,
                                @Param("reason") String reason);

    @Select("""
            SELECT n.id, n.title, n.body, n.priority, n.status, n.created_at,
                   n.related_type, n.related_id,
                   p.name AS project_name, u.unit_no, p.city
            FROM notifications n
            LEFT JOIN units u ON n.related_type = 'unit' AND n.related_id = u.id
            LEFT JOIN projects p ON p.id = u.project_id
            WHERE n.recipient_user_id = #{userId}
               OR n.recipient_owner_id IN (SELECT id FROM owners WHERE user_id = #{userId})
            ORDER BY n.created_at DESC, n.id DESC
            """)
    List<NotificationRow> findNotifications(@Param("userId") Long userId);

    @Select("""
            SELECT p.name AS project_name, p.city, u.unit_no
            FROM owner_units ou
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE o.user_id = #{userId} AND ou.status = 'active'
            ORDER BY p.name, u.unit_no
            """)
    List<UnitReference> findOwnerUnits(@Param("userId") Long userId);

    @Update("""
            UPDATE notifications
            SET status = 'read', read_at = COALESCE(read_at, NOW())
            WHERE id = #{notificationId}
              AND (recipient_user_id = #{userId}
                   OR recipient_owner_id IN (SELECT id FROM owners WHERE user_id = #{userId}))
            """)
    int markRead(@Param("userId") Long userId, @Param("notificationId") Long notificationId);

    @Update("""
            UPDATE notifications
            SET status = 'read', read_at = COALESCE(read_at, NOW())
            WHERE status <> 'read'
              AND (recipient_user_id = #{userId}
                   OR recipient_owner_id IN (SELECT id FROM owners WHERE user_id = #{userId}))
            """)
    int markAllRead(@Param("userId") Long userId);

    class NotificationRow {
        private Long id;
        private String title;
        private String body;
        private String priority;
        private String status;
        private java.time.LocalDateTime createdAt;
        private String relatedType;
        private Long relatedId;
        private String projectName;
        private String unitNo;
        private String city;

        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public String getTitle() { return title; }
        public void setTitle(String value) { title = value; }
        public String getBody() { return body; }
        public void setBody(String value) { body = value; }
        public String getPriority() { return priority; }
        public void setPriority(String value) { priority = value; }
        public String getStatus() { return status; }
        public void setStatus(String value) { status = value; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime value) { createdAt = value; }
        public String getRelatedType() { return relatedType; }
        public void setRelatedType(String value) { relatedType = value; }
        public Long getRelatedId() { return relatedId; }
        public void setRelatedId(Long value) { relatedId = value; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String value) { projectName = value; }
        public String getUnitNo() { return unitNo; }
        public void setUnitNo(String value) { unitNo = value; }
        public String getCity() { return city; }
        public void setCity(String value) { city = value; }
    }

    class UnitReference {
        private String projectName;
        private String city;
        private String unitNo;

        public String getProjectName() { return projectName; }
        public void setProjectName(String value) { projectName = value; }
        public String getCity() { return city; }
        public void setCity(String value) { city = value; }
        public String getUnitNo() { return unitNo; }
        public void setUnitNo(String value) { unitNo = value; }
    }

    class EmailSubscriptionRow {
        private Long userId;
        private String destination;
        private Boolean enabled;
        private java.time.LocalDateTime verifiedAt;
        private String verificationCodeHash;
        private java.time.LocalDateTime verificationExpiresAt;
        private java.time.LocalDateTime lastVerificationSentAt;

        public Long getUserId() { return userId; }
        public void setUserId(Long value) { userId = value; }
        public String getDestination() { return destination; }
        public void setDestination(String value) { destination = value; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean value) { enabled = value; }
        public java.time.LocalDateTime getVerifiedAt() { return verifiedAt; }
        public void setVerifiedAt(java.time.LocalDateTime value) { verifiedAt = value; }
        public String getVerificationCodeHash() { return verificationCodeHash; }
        public void setVerificationCodeHash(String value) { verificationCodeHash = value; }
        public java.time.LocalDateTime getVerificationExpiresAt() { return verificationExpiresAt; }
        public void setVerificationExpiresAt(java.time.LocalDateTime value) { verificationExpiresAt = value; }
        public java.time.LocalDateTime getLastVerificationSentAt() { return lastVerificationSentAt; }
        public void setLastVerificationSentAt(java.time.LocalDateTime value) { lastVerificationSentAt = value; }
    }

    class EmailDeliveryRow {
        private Long notificationId;
        private Long userId;
        private String destination;
        private String title;
        private String body;

        public Long getNotificationId() { return notificationId; }
        public void setNotificationId(Long value) { notificationId = value; }
        public Long getUserId() { return userId; }
        public void setUserId(Long value) { userId = value; }
        public String getDestination() { return destination; }
        public void setDestination(String value) { destination = value; }
        public String getTitle() { return title; }
        public void setTitle(String value) { title = value; }
        public String getBody() { return body; }
        public void setBody(String value) { body = value; }
    }
}
