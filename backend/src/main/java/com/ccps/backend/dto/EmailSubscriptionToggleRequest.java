package com.ccps.backend.dto;

import jakarta.validation.constraints.NotNull;

public record EmailSubscriptionToggleRequest(@NotNull Boolean enabled) {
}
