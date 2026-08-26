package com.ccps.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminReminderResponse(
        Summary summary,
        List<Rule> rules,
        List<NotificationItem> notifications,
        List<DeliveryItem> deliveries) {

    public record Summary(long ruleCount, long enabledRuleCount, long notificationCount,
                          long pendingDeliveryCount, long failedDeliveryCount) {
    }

    public record Rule(Long id, String code, String name, String eventType, int daysBefore,
                       List<String> channels, String recipientRole, boolean enabled,
                       boolean systemManaged, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record NotificationItem(Long id, Long ruleId, String ruleName, String eventType,
                                   String title, String body, String recipientName,
                                   String recipientEmail, String relatedType, Long relatedId,
                                   String priority, String noticeStatus, String deliverySummary,
                                   String failureReason, LocalDateTime createdAt) {
    }

    public record DeliveryItem(Long id, Long notificationId, String ruleName, String title,
                               String recipientName, String channel, String destination,
                               String status, int attemptCount, LocalDateTime sentAt,
                               LocalDateTime failedAt, String failureReason) {
    }
}
