package com.ccps.backend.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record AdminRentalAppointmentDetailsRequest(@NotNull Map<String, String> fields) { }
