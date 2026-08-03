package com.ccps.backend.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminAuditOptionsResponse;
import com.ccps.backend.dto.AdminAuditResponse;
import com.ccps.backend.mapper.AdminAuditMapper;

@Service
public class AdminAuditService {
    private final AdminAuditMapper mapper;

    public AdminAuditService(AdminAuditMapper mapper) { this.mapper = mapper; }

    @Transactional(readOnly = true)
    public AdminAuditResponse list(int requestedPage, int requestedPageSize, String keyword, String action,
            Long actorId, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must not be before start date");
        }
        int pageSize = Math.max(1, Math.min(requestedPageSize, 100));
        long total = mapper.count(normalize(keyword), normalize(action), actorId, startDate, endDate);
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        return new AdminAuditResponse(mapper.find(normalize(keyword), normalize(action), actorId, startDate, endDate,
                pageSize, (page - 1) * pageSize), new AdminAuditResponse.Page(total, page, pageSize, totalPages));
    }

    @Transactional(readOnly = true)
    public AdminAuditOptionsResponse options() {
        return new AdminAuditOptionsResponse(mapper.findActors(), mapper.findActions());
    }

    @Transactional
    public void record(Long actorId, String action, String entityType, Long entityId, String beforeData, String afterData) {
        mapper.insert(actorId, action, entityType, entityId, beforeData, afterData);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
