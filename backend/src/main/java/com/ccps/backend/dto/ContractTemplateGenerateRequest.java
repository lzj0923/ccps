package com.ccps.backend.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContractTemplateGenerateRequest(@NotBlank String templateType,
        @NotNull Map<String, String> fields) { }
