package com.ccps.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccps.backend.dto.OwnerNotificationResponse.NotificationItem;
import com.ccps.backend.dto.OwnerPropertyInformationResponse;

@Service
public class OwnerPropertyInformationService {
    private final OwnerNotificationService notificationService;

    public OwnerPropertyInformationService(OwnerNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<OwnerPropertyInformationResponse> list(Long userId, Long ownerUnitId) {
        return notificationService.getPropertyNotifications(userId, ownerUnitId).stream()
                .map(this::response)
                .toList();
    }

    private OwnerPropertyInformationResponse response(NotificationItem notification) {
        return new OwnerPropertyInformationResponse(
                notification.id(), notification.category(), notification.title(), notification.body(),
                notification.priority(), notification.status(), notification.createdAt(),
                notification.projectName(), notification.unitNo(), notification.city(), notification.number(),
                notification.detail(), notification.message());
    }
}
