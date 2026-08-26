package com.ccps.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminTenancyOptionsResponse(List<String> projects, List<Tenant> tenants, List<Unit> units,
        List<RentalSpace> rentalSpaces) {
    public record Tenant(Long id, String name) { }
    public record Unit(Long id, String projectName, String unitNo, String rentalMode,
            LocalDate mandateStartDate, LocalDate mandateEndDate) { }
    public record RentalSpace(Long id, Long unitId, String spaceCode, String spaceName, String spaceType,
            String status, Long currentLeaseId, LocalDate currentLeaseEnd) { }
}
