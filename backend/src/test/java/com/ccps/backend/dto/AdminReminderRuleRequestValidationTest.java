package com.ccps.backend.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class AdminReminderRuleRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsBusinessRecipientForLeaseExpiryWhatsAppRule() {
        AdminReminderRuleRequest request = new AdminReminderRuleRequest(
                "LEASE_EXPIRY_BUSINESS_30D", "租约结束前一个月通知业务人员",
                "lease_expiry", 30, List.of("whatsapp"), "business", true);

        assertThat(validator.validate(request)).isEmpty();
    }
}
