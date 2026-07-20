package com.ccps.backend.dto;

/** Database-backed headline counts for the admin owner workspace. */
public record AdminOwnerSummaryResponse(
        int ownerCount,
        int propertyCount,
        int operatingPropertyCount,
        int unpaidOwnerCount,
        int lowReserveOwnerCount) {
}
