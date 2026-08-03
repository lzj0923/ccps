package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.ccps.backend.dto.AdminOwnerResponse.Property;

/**
 * Read model for the property secondary workspace. Business modules remain the
 * source of truth; this response only assembles them for one screen.
 */
public record AdminPropertyWorkspaceResponse(
        Property property,
        AdminOwnerResponse owner,
        Map<String, Object> profile,
        List<AdminPropertyOwnershipResponse> ownerships,
        List<AdminPropertyContractRecordResponse> contracts,
        List<AdminPropertyLeaseOptionResponse> leases,
        Intelligence intelligence) {

    public record Intelligence(
            String effectiveRentalStatus,
            Long effectiveLeaseId,
            BigDecimal activeOwnershipPercent,
            boolean ownershipComplete,
            boolean leaseContractMissing,
            List<String> warnings) {
    }
}
