package com.ccps.backend.dto;

import jakarta.validation.constraints.Size;

public record AdminTenantWhatsAppSubscriptionRequest(
        boolean enabled,
        boolean consentConfirmed,
        @Size(max = 40) String destination,
        @Size(max = 120) String optInSource) {
}
