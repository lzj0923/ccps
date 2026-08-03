package com.ccps.backend.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record AdminLeaseInvoiceCreateRequest(@NotNull LocalDate billingMonth) {}
