package com.ccps.backend.dto;

import java.util.List;

import com.ccps.backend.dto.AdminOwnerResponse.Property;

public record AdminPropertyPageResponse(List<Item> rows, Page page) {
    public record Item(Long ownerId, String ownerName, String ownerPhone, String ownerEmail,
            String rentalStatus, Property property) {}
    public record Page(long totalRows, int page, int pageSize, int totalPages) {}
}
