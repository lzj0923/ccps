package com.ccps.backend.dto;

import jakarta.validation.constraints.Size;

public record AdminPropertyRelistRequest(@Size(max = 500) String note) {}
