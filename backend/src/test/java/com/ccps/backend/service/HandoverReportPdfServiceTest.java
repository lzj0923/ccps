package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.mapper.AdminPropertyHandoverReportMapper;
import com.ccps.backend.mapper.AdminPropertyHandoverReportMapper.PropertyInfo;
import com.ccps.backend.mapper.AdminPropertyHandoverReportMapper.ReportRow;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class HandoverReportPdfServiceTest {
    @Mock private AdminPropertyHandoverReportMapper mapper;

    @Test
    void createsPdfForStandardHandoverReport() {
        HandoverReportPdfService service = new HandoverReportPdfService();
        HandoverReportPdfService.Report report = new HandoverReportPdfService.Report(
                "Meridin Medini", "A-25-04", "陳先生", "1房 1廳", "原租客", "交接人員",
                LocalDate.of(2026, 7, 24),
                List.of(new HandoverReportPdfService.Section("鑰匙、門卡及遙控器",
                        List.of(new HandoverReportPdfService.Item("大門鑰匙", "2", "正常", "")))),
                List.of(new HandoverReportPdfService.Issue("客廳牆面", "刮痕", "從押金扣除", "")),
                List.of(new HandoverReportPdfService.Issue("廚房水龍頭", "漏水", "請業主決定", "")),
                "現場已完成交接。", List.of());

        byte[] pdf = service.create(report);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void downloadsStandardReportAsGeneratedPdf() {
        ReportRow row = new ReportRow(); row.setId(11L); row.setOwnerUnitId(12L); row.setTitle("A-25-04 交屋");
        row.setReportDate(LocalDate.of(2026, 7, 24));
        row.setContentJson("{\"handoverFrom\":\"原租客\",\"handoverTo\":\"業務\",\"sections\":[{\"title\":\"客廳\",\"items\":[{\"name\":\"沙發\",\"quantity\":\"1\",\"condition\":\"正常\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"photos\":[]}");
        PropertyInfo property = new PropertyInfo(); property.setProjectName("Meridin Medini"); property.setUnitNo("A-25-04"); property.setOwnerName("陳先生");
        when(mapper.ownsProperty(7L, 12L)).thenReturn(1);
        when(mapper.find(12L, 11L)).thenReturn(row);
        when(mapper.propertyInfo(12L)).thenReturn(property);

        var download = new AdminPropertyHandoverReportService(mapper, new ObjectMapper(), "target/test-handover-reports").download(7L, 12L, 11L);

        assertThat(download.mimeType()).isEqualTo("application/pdf");
        assertThat(download.originalName()).contains("交屋報告");
        assertThat(download.bytes()).startsWith("%PDF".getBytes());
    }
}
