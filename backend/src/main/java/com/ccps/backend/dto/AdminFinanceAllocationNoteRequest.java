package com.ccps.backend.dto;

import jakarta.validation.constraints.Size;

public record AdminFinanceAllocationNoteRequest(
        @Size(max = 500) String note,
        Boolean reuseEnabled) { }
