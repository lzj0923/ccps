package com.ccps.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminReportResponse(Summary summary, List<Definition> definitions,
                                  List<Run> runs, List<Project> projects,
                                  List<Owner> owners, List<Tenant> tenants,
                                  List<Unit> units, Page page) {
    public record Summary(long definitionCount, long completedCount, long failedCount,
                          long generatedThisMonth) { }
    public record Definition(Long id, String reportCode, String name, String reportType,
                             String defaultFormat, String defaultFilters, String scheduleCron,
                             boolean enabled, LocalDateTime updatedAt) { }
    public record Run(Long id, Long definitionId, String reportName, String requestedByName,
                      LocalDate dateStart, LocalDate dateEnd, Long projectId, String projectName,
                      String scopeType, String scopeName,
                      String outputFormat, String status, Integer recordCount, String errorMessage,
                      LocalDateTime startedAt, LocalDateTime completedAt, LocalDateTime createdAt,
                      boolean downloadable) { }
    public record Project(Long id, String name) { }
    public record Owner(Long id, String name) { }
    public record Tenant(Long id, String name) { }
    public record Unit(Long id, Long projectId, String projectName, String unitNo) { }
    public record Page(long totalRows, int page, int pageSize, int totalPages) { }
}
