package com.ccps.backend.dto;

import jakarta.validation.constraints.Pattern;

public record AdminRentalModeRequest(
        @Pattern(regexp = "whole_unit|shared") String rentalMode) { }
