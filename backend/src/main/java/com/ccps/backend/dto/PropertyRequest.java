package com.ccps.backend.dto;

import java.math.BigDecimal;

import com.ccps.backend.model.PropertyStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PropertyRequest(
        @NotBlank(message = "物業名稱不可為空") String name,
        @NotBlank(message = "建案名稱不可為空") String projectName,
        @NotBlank(message = "地址不可為空") String address,
        @NotNull(message = "價格不可為空") @DecimalMin(value = "0", inclusive = false, message = "價格必須大於 0") BigDecimal price,
        @NotNull(message = "面積不可為空") @Min(value = 1, message = "面積必須大於 0") Integer area,
        @NotNull(message = "房間數不可為空") @Min(value = 0, message = "房間數不可小於 0") Integer bedrooms,
        @NotNull(message = "物業狀態不可為空") PropertyStatus status
) {
}
