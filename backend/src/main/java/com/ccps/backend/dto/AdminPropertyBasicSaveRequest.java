package com.ccps.backend.dto;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Atomic write model for the property workspace basic-information form.
 */
public record AdminPropertyBasicSaveRequest(
        @NotNull @Valid AdminPropertyUpdateRequest property,
        @NotNull @Valid AdminOwnerUpdateRequest owner,
        @NotNull Map<String, Object> profile) {
}
