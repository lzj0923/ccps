package com.ccps.backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminProjectCreateRequest;
import com.ccps.backend.dto.AdminProjectManagementResponse;
import com.ccps.backend.mapper.AdminProjectMapper;
import com.ccps.backend.mapper.AdminProjectMapper.ProjectRow;
import com.ccps.backend.mapper.AdminProjectMapper.ProjectWrite;
import com.ccps.backend.mapper.AdminProjectMapper.SummaryRow;

@Service
public class AdminProjectService {
    private final AdminProjectMapper mapper;

    public AdminProjectService(AdminProjectMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public AdminProjectManagementResponse findAll(int requestedPage, int requestedPageSize,
                                                   String keyword, String status) {
        int pageSize = Math.max(1, Math.min(requestedPageSize, 100));
        String normalizedKeyword = normalize(keyword);
        String normalizedStatus = normalizeStatus(status);
        long totalRows = mapper.countRows(normalizedKeyword, normalizedStatus);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        List<AdminProjectManagementResponse.Project> rows = mapper
                .findRows(normalizedKeyword, normalizedStatus, pageSize, (page - 1) * pageSize)
                .stream().map(this::toProject).toList();
        SummaryRow source = mapper.findSummary();
        AdminProjectManagementResponse.Summary summary = new AdminProjectManagementResponse.Summary(
                zero(source == null ? null : source.getTotalCount()),
                zero(source == null ? null : source.getActiveCount()),
                zero(source == null ? null : source.getInactiveCount()),
                zero(source == null ? null : source.getUnitCount()));
        return new AdminProjectManagementResponse(summary, rows, mapper.findCities(),
                new AdminProjectManagementResponse.Page(totalRows, page, pageSize, totalPages));
    }

    @Transactional(readOnly = true)
    public boolean isProjectCodeAvailable(String projectCode) {
        String normalized = normalize(projectCode);
        return normalized != null && mapper.countProjectCode(normalized.toUpperCase(), null) == 0;
    }

    @Transactional
    public AdminProjectManagementResponse.Project create(AdminProjectCreateRequest request) {
        ProjectWrite write = toWrite(null, request);
        ensureUniqueCode(write.getProjectCode(), null);
        if (mapper.insert(write) != 1 || write.getId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Project could not be created");
        }
        return requireProject(write.getId());
    }

    @Transactional
    public AdminProjectManagementResponse.Project update(Long id, AdminProjectCreateRequest request) {
        if (mapper.findById(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        ProjectWrite write = toWrite(id, request);
        ensureUniqueCode(write.getProjectCode(), id);
        if (mapper.update(write) != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Project could not be updated");
        }
        return requireProject(id);
    }

    @Transactional
    public void delete(Long id) {
        if (mapper.findById(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        if (mapper.countReferences(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project with related records cannot be deleted");
        }
        if (mapper.delete(id) != 1) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Project could not be deleted");
        }
    }

    private ProjectWrite toWrite(Long id, AdminProjectCreateRequest request) {
        ProjectWrite write = new ProjectWrite();
        write.setId(id);
        write.setProjectCode(request.projectCode().trim().toUpperCase());
        write.setName(request.name().trim());
        write.setAddress(normalize(request.address()));
        write.setState(normalize(request.state()));
        write.setCity(normalize(request.city()));
        write.setCountryCode(request.countryCode().trim().toUpperCase());
        write.setStatus(request.status());
        return write;
    }

    private void ensureUniqueCode(String projectCode, Long excludeId) {
        if (mapper.countProjectCode(projectCode, excludeId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project code already exists");
        }
    }

    private AdminProjectManagementResponse.Project requireProject(Long id) {
        ProjectRow row = mapper.findById(id);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        return toProject(row);
    }

    private AdminProjectManagementResponse.Project toProject(ProjectRow row) {
        return new AdminProjectManagementResponse.Project(row.getId(), row.getProjectCode(), row.getName(),
                row.getAddress(), row.getState(), row.getCity(), row.getCountryCode(), row.getStatus(),
                zero(row.getUnitCount()), zero(row.getOwnerCount()), row.getCreatedAt(), row.getUpdatedAt());
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeStatus(String value) {
        String normalized = normalize(value);
        return "active".equals(normalized) || "inactive".equals(normalized) ? normalized : null;
    }

    private long zero(Long value) { return value == null ? 0 : value; }
}
