package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.dto.AdminReportGenerateRequest;
import com.ccps.backend.mapper.AdminReportMapper;
import com.ccps.backend.mapper.AdminReportMapper.DefinitionRow;
import com.ccps.backend.mapper.AdminReportMapper.NewRun;
import com.ccps.backend.mapper.AdminReportMapper.ReportDataRow;
import com.ccps.backend.mapper.AdminReportMapper.RunRow;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {
    @Mock private AdminReportMapper mapper;
    @TempDir Path tempDir;

    @Test void generatesXlsxAndStoresCompletedRun() throws Exception {
        AdminReportService service = new AdminReportService(mapper, new ObjectMapper(), tempDir.toString());
        DefinitionRow definition = new DefinitionRow(); definition.setId(3L); definition.setReportCode("RENT_COLLECTION");
        definition.setName("租金收款進度"); definition.setReportType("rent_collection"); definition.setEnabled(true);
        when(mapper.findDefinitionByType("rent_collection")).thenReturn(definition);
        when(mapper.insertRun(any())).thenAnswer(invocation -> { NewRun run = invocation.getArgument(0); run.setId(55L); return 1; });
        ReportDataRow row = new ReportDataRow(); row.setRecordDate(LocalDate.of(2026, 7, 20)); row.setReferenceNo("RENT-01");
        row.setCategory("rent_payment"); row.setProjectName("Demo"); row.setUnitNo("A-01"); row.setPartyName("Owner");
        row.setDescription("bank_transfer / MYR"); row.setAmount(new BigDecimal("2100.00")); row.setStatus("paid"); row.setExtraStatus("not_synced");
        when(mapper.findFinanceReportRows(eq("rent_collection"), any(), any(), eq(null))).thenReturn(List.of(row));
        AtomicReference<String> storage = new AtomicReference<>();
        when(mapper.completeRun(eq(55L), eq(1), any())).thenAnswer(invocation -> { storage.set(invocation.getArgument(2)); return 1; });
        when(mapper.findRun(55L)).thenAnswer(invocation -> completed(storage.get()));

        var result = service.generate(5L, new AdminReportGenerateRequest("rent_collection",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, "XLSX"));

        assertThat(result.status()).isEqualTo("completed");
        assertThat(storage.get()).endsWith("55-RENT_COLLECTION.xlsx");
        assertThat(Files.size(tempDir.resolve(storage.get()))).isGreaterThan(0);
        verify(mapper).insertAudit(5L, 55L, "租金收款進度");
    }

    @Test void generatesPdfWithChineseReportTitle() throws Exception {
        AdminReportService service = new AdminReportService(mapper, new ObjectMapper(), tempDir.toString());
        DefinitionRow definition = new DefinitionRow(); definition.setId(4L); definition.setReportCode("MAINTENANCE");
        definition.setName("維修費用統計"); definition.setReportType("maintenance"); definition.setEnabled(true);
        when(mapper.findDefinitionByType("maintenance")).thenReturn(definition);
        when(mapper.insertRun(any())).thenAnswer(invocation -> { NewRun run = invocation.getArgument(0); run.setId(56L); return 1; });
        ReportDataRow row = new ReportDataRow(); row.setRecordDate(LocalDate.of(2026, 7, 20)); row.setReferenceNo("WO-01");
        row.setCategory("plumbing"); row.setProjectName("示範建案"); row.setUnitNo("A-01"); row.setPartyName("維修供應商");
        row.setDescription("水管維修"); row.setAmount(new BigDecimal("350.00")); row.setStatus("completed"); row.setExtraStatus("已完成");
        when(mapper.findMaintenanceRows(any(), any(), eq(null))).thenReturn(List.of(row));
        AtomicReference<String> storage = new AtomicReference<>();
        when(mapper.completeRun(eq(56L), eq(1), any())).thenAnswer(invocation -> { storage.set(invocation.getArgument(2)); return 1; });
        when(mapper.findRun(56L)).thenAnswer(invocation -> { RunRow run = completed(storage.get()); run.setId(56L); run.setDefinitionId(4L); run.setReportName("維修費用統計"); run.setOutputFormat("PDF"); return run; });

        var result = service.generate(5L, new AdminReportGenerateRequest("maintenance",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, "PDF"));

        assertThat(result.errorMessage()).isNull();
        assertThat(result.status()).isEqualTo("completed");
        assertThat(storage.get()).endsWith("56-MAINTENANCE.pdf");
        assertThat(Files.size(tempDir.resolve(storage.get()))).isGreaterThan(0);
    }

    private RunRow completed(String storage) {
        RunRow row = new RunRow(); row.setId(55L); row.setDefinitionId(3L); row.setReportName("租金收款進度");
        row.setDateStart(LocalDate.of(2026, 7, 1)); row.setDateEnd(LocalDate.of(2026, 7, 31)); row.setOutputFormat("XLSX");
        row.setStatus("completed"); row.setRecordCount(1); row.setStorageKey(storage); return row;
    }
}
