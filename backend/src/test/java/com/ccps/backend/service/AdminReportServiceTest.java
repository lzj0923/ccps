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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
        ReportDataRow newerRow = new ReportDataRow(); newerRow.setRecordDate(LocalDate.of(2026, 7, 31)); newerRow.setReferenceNo("RENT-02");
        newerRow.setCategory("rent_payment"); newerRow.setProjectName("Demo"); newerRow.setUnitNo("A-02"); newerRow.setPartyName("Owner");
        newerRow.setDescription("bank_transfer / MYR"); newerRow.setAmount(new BigDecimal("2200.00")); newerRow.setStatus("paid"); newerRow.setExtraStatus("not_synced");
        when(mapper.findRentCollectionRows(any(), any(), eq(null), eq(null), eq(null))).thenReturn(List.of(row, newerRow));
        AtomicReference<String> storage = new AtomicReference<>();
        when(mapper.completeRun(eq(55L), eq(2), any())).thenAnswer(invocation -> { storage.set(invocation.getArgument(2)); return 1; });
        when(mapper.findRun(55L)).thenAnswer(invocation -> completed(storage.get()));

        var result = service.generate(5L, new AdminReportGenerateRequest("rent_collection",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, null, null, null, "XLSX"));

        assertThat(result.status()).isEqualTo("completed");
        assertThat(storage.get()).endsWith("55-RENT_COLLECTION.xlsx");
        assertThat(Files.size(tempDir.resolve(storage.get()))).isGreaterThan(0);
        try (var input = Files.newInputStream(tempDir.resolve(storage.get())); var workbook = new XSSFWorkbook(input)) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(4).getCell(0).getStringCellValue()).isEqualTo("2026-07-31");
            assertThat(sheet.getRow(5).getCell(0).getStringCellValue()).isEqualTo("2026-07-20");
        }
        verify(mapper).insertAudit(5L, 55L, "租金收款進度");
        verify(mapper).findRentCollectionRows(any(), any(), eq(null), eq(null), eq(null));
    }

    @Test void propertyPaymentReportUsesInstallmentScheduleIncludingUnpaidItems() throws Exception {
        AdminReportService service = new AdminReportService(mapper, new ObjectMapper(), tempDir.toString());
        DefinitionRow definition = new DefinitionRow(); definition.setId(2L); definition.setReportCode("PROPERTY_PAYMENT");
        definition.setName("購房款收款與未收款"); definition.setReportType("property_payment"); definition.setEnabled(true);
        when(mapper.findDefinitionByType("property_payment")).thenReturn(definition);
        when(mapper.insertRun(any())).thenAnswer(invocation -> { NewRun run = invocation.getArgument(0); run.setId(54L); return 1; });
        ReportDataRow row = new ReportDataRow(); row.setRecordDate(LocalDate.of(2026, 9, 4));
        row.setReferenceNo("CONTRACT-01-01"); row.setCategory("房款分期"); row.setProjectName("Demo");
        row.setUnitNo("A-01"); row.setPartyName("Owner"); row.setDescription("第 1 期");
        row.setAmount(new BigDecimal("3333.34")); row.setStatus("unpaid"); row.setExtraStatus("已收 RM 0.00 · 未收 RM 3,333.34");
        when(mapper.findPropertyPaymentRows(any(), any(), eq(null), eq(null), eq(null))).thenReturn(List.of(row));
        AtomicReference<String> storage = new AtomicReference<>();
        when(mapper.completeRun(eq(54L), eq(1), any())).thenAnswer(invocation -> { storage.set(invocation.getArgument(2)); return 1; });
        when(mapper.findRun(54L)).thenAnswer(invocation -> { RunRow run = completed(storage.get()); run.setId(54L);
            run.setDefinitionId(2L); run.setReportName("購房款收款與未收款"); return run; });

        var result = service.generate(5L, new AdminReportGenerateRequest("property_payment",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null, null, null, null, "XLSX"));

        assertThat(result.status()).isEqualTo("completed");
        assertThat(Files.size(tempDir.resolve(storage.get()))).isGreaterThan(0);
        try (var input = Files.newInputStream(tempDir.resolve(storage.get())); var workbook = new XSSFWorkbook(input)) {
            assertThat(workbook.getSheetAt(0).getRow(4).getCell(8).getStringCellValue()).isEqualTo("unpaid");
        }
        verify(mapper).findPropertyPaymentRows(any(), any(), eq(null), eq(null), eq(null));
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
        when(mapper.findMaintenanceRows(any(), any(), eq(null), eq(null), eq(null))).thenReturn(List.of(row));
        AtomicReference<String> storage = new AtomicReference<>();
        when(mapper.completeRun(eq(56L), eq(1), any())).thenAnswer(invocation -> { storage.set(invocation.getArgument(2)); return 1; });
        when(mapper.findRun(56L)).thenAnswer(invocation -> { RunRow run = completed(storage.get()); run.setId(56L); run.setDefinitionId(4L); run.setReportName("維修費用統計"); run.setOutputFormat("PDF"); return run; });

        var result = service.generate(5L, new AdminReportGenerateRequest("maintenance",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, null, null, null, "PDF"));

        assertThat(result.errorMessage()).isNull();
        assertThat(result.status()).isEqualTo("completed");
        assertThat(storage.get()).endsWith("56-MAINTENANCE.pdf");
        assertThat(Files.size(tempDir.resolve(storage.get()))).isGreaterThan(0);
    }

    @Test void generatesIncomeExpenseXlsxInRequestedBilingualFormatForOwner() throws Exception {
        AdminReportService service = new AdminReportService(mapper, new ObjectMapper(), tempDir.toString());
        DefinitionRow definition = new DefinitionRow(); definition.setId(3L); definition.setReportCode("INCOME_EXPENSE");
        definition.setName("收入與支出明細"); definition.setReportType("income_expense"); definition.setEnabled(true);
        when(mapper.findDefinitionByType("income_expense")).thenReturn(definition);
        when(mapper.insertRun(any())).thenAnswer(invocation -> { NewRun run = invocation.getArgument(0); run.setId(57L); return 1; });
        ReportDataRow row = new ReportDataRow(); row.setItem("支出 餘額退款"); row.setObjectName("示範建案");
        row.setItemName("A-01"); row.setPaymentName("將餘款匯給業主"); row.setStatus("paid");
        row.setPaymentMethod("bank_transfer"); row.setRealDate(LocalDate.of(2026, 7, 5));
        row.setPayDate(LocalDate.of(2026, 7, 6)); row.setIncome(BigDecimal.ZERO);
        row.setExpense(new BigDecimal("2100.00")); row.setBalance(BigDecimal.ZERO); row.setCurrency("MYR");
        row.setLeaseStart(null); row.setLeaseEnd(null);
        row.setNote("測試備註"); row.setBankInfo("銀行名稱: TEST BANK\n銀行帳戶: TEST ACCOUNT\n銀行帳號: TEST-123");
        when(mapper.findIncomeExpenseRows(any(), any(), eq(null), eq(9L), eq(null))).thenReturn(List.of(row));
        AtomicReference<String> storage = new AtomicReference<>();
        when(mapper.completeRun(eq(57L), eq(1), any())).thenAnswer(invocation -> { storage.set(invocation.getArgument(2)); return 1; });
        when(mapper.findRun(57L)).thenAnswer(invocation -> { RunRow run = completed(storage.get()); run.setId(57L); run.setReportName("收入與支出明細"); return run; });

        var result = service.generate(5L, new AdminReportGenerateRequest("income_expense",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, 9L, null, null, "XLSX"));

        assertThat(result.status()).isEqualTo("completed");
        try (var input = Files.newInputStream(tempDir.resolve(storage.get())); var workbook = new XSSFWorkbook(input)) {
            var sheet = workbook.getSheetAt(0);
            List<String> actualHeaders = new ArrayList<>();
            sheet.getRow(0).forEach(cell -> actualHeaders.add(cell.getStringCellValue()));
            assertThat(actualHeaders).containsExactly(
                    "收支項目/Item", "物件名稱/ObjName", "物件項目/ItemName", "付款名稱/Name",
                    "狀態/Status", "付款方式/Payment", "實際收付款日期/RealDate", "確認收付款日期/PayDate",
                    "收入/Income", "支出/Expense", "餘額/Balance", "幣別/Currency", "租期(起)", "租期(迄)",
                    "備註/Note", "匯款銀行/Bank Info");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("支出 餘額退款");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("將餘款匯給業主");
            assertThat(sheet.getRow(1).getCell(4).getStringCellValue()).isEqualTo("已付款");
            assertThat(sheet.getRow(1).getCell(5).getStringCellValue()).isEqualTo("匯款");
            assertThat(sheet.getRow(1).getCell(6).getStringCellValue()).isEqualTo("2026/7/5");
            assertThat(sheet.getRow(1).getCell(7).getStringCellValue()).isEqualTo("2026/7/6");
            assertThat(sheet.getRow(1).getCell(8).getNumericCellValue()).isZero();
            assertThat(sheet.getRow(1).getCell(9).getNumericCellValue()).isEqualTo(2100.0);
            assertThat(sheet.getRow(1).getCell(10).getNumericCellValue()).isEqualTo(-2100.0);
            assertThat(sheet.getRow(1).getCell(15).getStringCellValue()).contains("銀行名稱: TEST BANK");
        }
    }

    @Test void downloadUsesReportScopeAndPeriodInFilename() throws Exception {
        AdminReportService service = new AdminReportService(mapper, new ObjectMapper(), tempDir.toString());
        Path stored = tempDir.resolve("2026-07/report.xlsx");
        Files.createDirectories(stored.getParent());
        Files.writeString(stored, "report");
        RunRow run = completed("2026-07/report.xlsx");
        run.setReportName("業主預備金返還清單");
        run.setScopeName("呂小布");
        when(mapper.findRun(55L)).thenReturn(run);

        var download = service.download(55L);

        assertThat(download.filename()).isEqualTo("業主預備金返還清單_呂小布_2026-07-01至2026-07-31.xlsx");
    }

    @Test void generatesOwnerStatementForExactlyOneOwner() {
        AdminReportService service = new AdminReportService(mapper, new ObjectMapper(), tempDir.toString());
        DefinitionRow definition = new DefinitionRow(); definition.setId(9L); definition.setReportCode("OWNER_STATEMENT");
        definition.setName("業主帳單"); definition.setReportType("owner_statement"); definition.setEnabled(true);
        when(mapper.findDefinitionByType("owner_statement")).thenReturn(definition);
        when(mapper.insertRun(any())).thenAnswer(invocation -> { NewRun run = invocation.getArgument(0); run.setId(58L); return 1; });
        when(mapper.findIncomeExpenseRows(any(), any(), eq(null), eq(21L), eq(null))).thenReturn(List.of());
        when(mapper.findRun(58L)).thenAnswer(invocation -> { RunRow run = completed(null); run.setId(58L); run.setDefinitionId(9L); run.setReportName("業主帳單"); return run; });

        var result = service.generate(5L, new AdminReportGenerateRequest("owner_statement",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, 21L, null, null, "XLSX"));

        assertThat(result.status()).isEqualTo("completed");
        verify(mapper).findIncomeExpenseRows(any(), any(), eq(null), eq(21L), eq(null));
    }

    @Test void generatesTenantStatementForExactlyOneTenant() {
        AdminReportService service = new AdminReportService(mapper, new ObjectMapper(), tempDir.toString());
        DefinitionRow definition = new DefinitionRow(); definition.setId(10L); definition.setReportCode("TENANT_STATEMENT");
        definition.setName("租客帳單"); definition.setReportType("tenant_statement"); definition.setEnabled(true);
        when(mapper.findDefinitionByType("tenant_statement")).thenReturn(definition);
        when(mapper.insertRun(any())).thenAnswer(invocation -> { NewRun run = invocation.getArgument(0); run.setId(59L); return 1; });
        when(mapper.findTenantStatementRows(any(), any(), eq(31L))).thenReturn(List.of());
        when(mapper.findRun(59L)).thenAnswer(invocation -> { RunRow run = completed(null); run.setId(59L); run.setDefinitionId(10L); run.setReportName("租客帳單"); return run; });

        var result = service.generate(5L, new AdminReportGenerateRequest("tenant_statement",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, null, 31L, null, "PDF"));

        assertThat(result.status()).isEqualTo("completed");
        verify(mapper).findTenantStatementRows(any(), any(), eq(31L));
    }

    private RunRow completed(String storage) {
        RunRow row = new RunRow(); row.setId(55L); row.setDefinitionId(3L); row.setReportName("租金收款進度");
        row.setDateStart(LocalDate.of(2026, 7, 1)); row.setDateEnd(LocalDate.of(2026, 7, 31)); row.setOutputFormat("XLSX");
        row.setStatus("completed"); row.setRecordCount(1); row.setStorageKey(storage); return row;
    }
}
