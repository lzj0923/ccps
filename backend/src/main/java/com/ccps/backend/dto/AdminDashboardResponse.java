package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(Summary summary, List<Region> regions) {
    public record Summary(long unitCount, long occupiedCount, BigDecimal occupancyRate,
            BigDecimal totalRent, BigDecimal averageRent, BigDecimal tenantDeposit,
            BigDecimal reserveBalance) { }
    public record Region(String regionName, long unitCount, long occupiedCount, BigDecimal occupancyRate,
            BigDecimal totalRent, BigDecimal averageRent, BigDecimal tenantDeposit,
            BigDecimal reserveBalance) { }
}
