package com.ccps.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRentDecisionRequest(@NotBlank @Size(max = 500) String note) { }
