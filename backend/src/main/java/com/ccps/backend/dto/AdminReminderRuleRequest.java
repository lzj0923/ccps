package com.ccps.backend.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminReminderRuleRequest(
        @NotBlank @Size(max = 80) @Pattern(regexp = "[A-Za-z0-9_-]+") String code,
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Pattern(regexp = "payment_due|rent_due|lease_expiry|reserve_low|document_expiry") String eventType,
        @Min(0) @Max(365) int daysBefore,
        @NotEmpty List<@Pattern(regexp = "in_app|email|line|whatsapp") String> channels,
        @NotBlank @Pattern(regexp = "owner|tenant|business") String recipientRole,
        boolean enabled) {
}
