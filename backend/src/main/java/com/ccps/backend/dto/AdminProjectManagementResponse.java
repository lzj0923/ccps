package com.ccps.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminProjectManagementResponse(Summary summary, List<Project> rows, List<String> cities, Page page) {
    public record Summary(long totalCount, long activeCount, long inactiveCount, long unitCount) {}

    public record Project(Long id, String projectCode, String name, String address, String state, String city,
                          String countryCode, String status, long unitCount, long ownerCount,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {}

    public record Page(long totalRows, int page, int pageSize, int totalPages) {}
}
