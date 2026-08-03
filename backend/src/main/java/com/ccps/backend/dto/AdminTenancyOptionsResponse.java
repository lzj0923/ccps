package com.ccps.backend.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminTenancyOptionsResponse(List<String> projects, List<Tenant> tenants, List<Unit> units) {
    public record Tenant(Long id, String name) { }
    public record Unit(Long id, String projectName, String unitNo, LocalDate mandateStartDate, LocalDate mandateEndDate) { }
}
