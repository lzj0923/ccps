package com.ccps.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminRentalMandateReviewRequest(
        @NotNull Boolean approved,
        @Size(max = 500) String note) {
}
