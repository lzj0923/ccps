package com.ccps.backend.dto;

import java.time.LocalDateTime;

public record AdminTenantWhatsAppSubscriptionResponse(
        Long tenantId,
        String destination,
        boolean enabled,
        LocalDateTime optedInAt,
        LocalDateTime optedOutAt,
        String optInSource) {
}
