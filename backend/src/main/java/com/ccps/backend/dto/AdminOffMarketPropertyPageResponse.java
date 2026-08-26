package com.ccps.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminOffMarketPropertyPageResponse(List<Item> rows, Page page, Summary summary) {
    public record Item(Long unitId, Long ownerUnitId, Long ownerId, String ownerName, String ownerPhone,
            String projectName, String city, String building, String floorNo, String unitNo, String unitType,
            String listingStatus, String assetStage, String reasonCode, String note, LocalDateTime offMarketAt,
            String offMarketByName, long leaseCount, long documentCount, long maintenanceCount) {}
    public record Page(long totalRows, int page, int pageSize, int totalPages) {}
    public record Summary(long total, long thisMonth, long withDocuments, long canRelist) {}
}
