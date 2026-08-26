package com.ccps.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminPropertyOffMarketRequest(
        @NotBlank @Size(max = 40) String reasonCode,
        @Size(max = 500) String note) {}
