package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record AdminMaintenanceOptionsResponse(List<UnitOption> units, List<VendorOption> vendors) {
    public record UnitOption(Long unitId, Long ownerId, String ownerName, String projectName,
            String unitNo, Long reserveAccountId, BigDecimal reserveBalance) { }
    public record VendorOption(Long id, String name, String contactName, String phone) { }
}
