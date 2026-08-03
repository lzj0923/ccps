package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminPropertyCreateRequest(
        Long projectId,
        @Size(max = 80) String building,
        @Size(max = 20) String floorNo,
        @NotBlank @Size(max = 40) String unitNo,
        @Size(max = 80) String unitType,
        @DecimalMin(value = "0.01") BigDecimal areaSqm,
        @Min(0) @Max(50) Integer bedroomCount,
        @NotBlank @Pattern(regexp = "available|reserved|sold|rented|inactive") String listingStatus,
        @NotBlank @Pattern(regexp = "PRE_HANDOVER|OPERATING") String assetStage,
        LocalDate expectedHandoverDate,
        LocalDate actualHandoverDate,
        List<@Pattern(regexp = "RENTAL|RESALE|MANAGEMENT") String> services,
        @NotNull @DecimalMin(value = "0.00") BigDecimal purchasePrice,
        @NotNull @DecimalMin(value = "0.01") @DecimalMax(value = "100.00") BigDecimal ownershipPercent,
        boolean primary,
        LocalDate startDate,
        @Size(max = 160) String projectName) {

    /**
     * Keeps the original constructor used by existing callers/tests while allowing
     * the property form to submit a new project name in the same request.
     */
    public AdminPropertyCreateRequest(Long projectId,
                                      String building,
                                      String floorNo,
                                      String unitNo,
                                      String unitType,
                                      BigDecimal areaSqm,
                                      Integer bedroomCount,
                                      String listingStatus,
                                      String assetStage,
                                      LocalDate expectedHandoverDate,
                                      LocalDate actualHandoverDate,
                                      List<String> services,
                                      BigDecimal purchasePrice,
                                      BigDecimal ownershipPercent,
                                      boolean primary,
                                      LocalDate startDate) {
        this(projectId, building, floorNo, unitNo, unitType, areaSqm, bedroomCount,
                listingStatus, assetStage, expectedHandoverDate, actualHandoverDate,
                services, purchasePrice, ownershipPercent, primary, startDate, null);
    }
}
