package com.ccps.backend.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class AdminOwnerPhoneValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsAPlainLocalNumberWithoutAnInternationalCountryCode() {
        var request = new AdminOwnerCreateRequest(
                null, "Test Owner", null, "100", "100",
                null, null, null, null, "active");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getMessage()
                        .equals("Owner phone number must use E.164 format"));
    }

    @Test
    void acceptsANormalizedMalaysianOwnerPhone() {
        var request = new AdminOwnerCreateRequest(
                null, "Test Owner", null, "+60123456789", "+60123456789",
                null, null, null, null, "active");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void validatesOwnerPhoneUpdatesToo() {
        var request = new AdminOwnerUpdateRequest(
                null, "Test Owner", null, "123456", "123456",
                null, null, null, null, null, "active");

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getMessage()
                        .equals("Owner phone number must use E.164 format"));
    }
}
