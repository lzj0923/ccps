package com.ccps.backend.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class AdminTenantCreateRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsShortOrLocalOnlyPhoneNumbers() {
        AdminTenantCreateRequest request = new AdminTenantCreateRequest(
                "WhatsApp测试租客", "WA-TEST-001", "100", "tenant@example.com", "active");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("phone")
                        && violation.getMessage().equals("Tenant phone number must use E.164 format"));
    }

    @Test
    void acceptsE164AndOptionalEmptyPhoneNumbers() {
        AdminTenantCreateRequest chinese = new AdminTenantCreateRequest(
                "中国租客", "WA-CN-001", "+8613800138000", "cn@example.com", "active");
        AdminTenantCreateRequest empty = new AdminTenantCreateRequest(
                "无电话租客", "WA-NO-PHONE", "", "none@example.com", "active");

        assertThat(validator.validate(chinese)).isEmpty();
        assertThat(validator.validate(empty)).isEmpty();
    }
}
