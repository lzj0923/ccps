package com.ccps.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ccps.backend.dto.AdminPropertyHandoverReportResponse;
import com.ccps.backend.dto.OwnerPropertyHandoverReportResponse;

@Service
public class OwnerPropertyReportService {
    private final AdminPropertyHandoverReportService handoverService;

    public OwnerPropertyReportService(AdminPropertyHandoverReportService handoverService) {
        this.handoverService = handoverService;
    }

    @Transactional(readOnly = true)
    public List<OwnerPropertyHandoverReportResponse> handoverReports(Long userId, Long ownerUnitId) {
        return handoverService.listForOwnerUser(userId, ownerUnitId).stream().map(this::toOwnerReport).toList();
    }

    @Transactional(readOnly = true)
    public AdminPropertyHandoverReportService.Download downloadHandoverReport(
            Long userId, Long ownerUnitId, Long reportId) {
        return handoverService.downloadForOwnerUser(userId, ownerUnitId, reportId);
    }

    private OwnerPropertyHandoverReportResponse toOwnerReport(AdminPropertyHandoverReportResponse report) {
        boolean generated = report.contentJson() != null && !report.contentJson().isBlank();
        String fileName = report.originalName();
        if (fileName == null || fileName.isBlank()) {
            fileName = "交屋報告_" + report.title() + "_" + report.reportDate() + ".pdf";
        }
        return new OwnerPropertyHandoverReportResponse(
                report.id(), report.title(), report.reportDate(), fileName,
                generated ? "application/pdf" : report.mimeType(), report.fileSize(), report.completed(),
                generated || report.documentId() != null, report.updatedAt());
    }
}
