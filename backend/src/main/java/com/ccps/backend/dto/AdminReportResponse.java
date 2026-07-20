package com.ccps.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminReportResponse(Summary summary, List<Definition> definitions,
                                  List<Run> runs, List<Project> projects) {
    public record Summary(long definitionCount, long completedCount, long failedCount,
                          long generatedThisMonth) { }
    public record Definition(Long id, String reportCode, String name, String reportType,
                             String defaultFormat, String defaultFilters, String scheduleCron,
                             boolean enabled, LocalDateTime updatedAt) { }
    public record Run(Long id, Long definitionId, String reportName, String requestedByName,
                      LocalDate dateStart, LocalDate dateEnd, Long projectId, String projectName,
                      String outputFormat, String status, Integer recordCount, String errorMessage,
                      LocalDateTime startedAt, LocalDateTime completedAt, LocalDateTime createdAt,
                      boolean downloadable) { }
    public record Project(Long id, String name) { }
}
