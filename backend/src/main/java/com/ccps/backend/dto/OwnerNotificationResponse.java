package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OwnerNotificationResponse(
        Summary summary,
        List<Category> categories,
        List<NotificationItem> notifications,
        List<PendingTask> tasks,
        List<Channel> channels) {

    public record Summary(
            int unreadCount,
            int pendingCount,
            int monthSystemCount,
            int importantCount,
            int totalCount) {
    }

    public record Category(String key, String label, int count) {
    }

    public record NotificationItem(
            Long id,
            String category,
            String title,
            String body,
            String priority,
            String status,
            LocalDateTime createdAt,
            String projectName,
            String unitNo,
            String city,
            String number,
            BigDecimal amount,
            LocalDate dueDate,
            String detail,
            String message) {
    }

    public record PendingTask(
            String type,
            String title,
            String detail,
            int count,
            String priority) {
    }

    public record Channel(String key, String label, String status, String destination, boolean verified) {
    }
}
