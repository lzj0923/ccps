package com.ccps.backend.service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminReportGenerateRequest;
import com.ccps.backend.dto.AdminReportResponse;
import com.ccps.backend.mapper.AdminReportMapper;
import com.ccps.backend.mapper.AdminReportMapper.DefinitionRow;
import com.ccps.backend.mapper.AdminReportMapper.NewRun;
import com.ccps.backend.mapper.AdminReportMapper.ReportDataRow;
import com.ccps.backend.mapper.AdminReportMapper.RunRow;
import com.ccps.backend.mapper.AdminReportMapper.SummaryRow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class AdminReportService {
    private static final List<String> GENERIC_HEADERS = List.of("日期", "單號", "分類", "建案", "單位", "對象",
            "說明", "金額", "狀態", "附加狀態");
    private static final List<String> INCOME_EXPENSE_HEADERS = List.of(
            "收支項目/Item", "物件名稱/ObjName", "物件項目/ItemName", "付款名稱/Name",
            "狀態/Status", "付款方式/Payment", "實際收付款日期/RealDate", "確認收付款日期/PayDate",
            "收入/Income", "支出/Expense", "餘額/Balance", "幣別/Currency", "租期(起)", "租期(迄)",
            "備註/Note", "匯款銀行/Bank Info");
    private final AdminReportMapper mapper;
    private final ObjectMapper objectMapper;
    private final Path reportRoot;

    public AdminReportService(AdminReportMapper mapper, ObjectMapper objectMapper,
                              @Value("${ccps.storage.reports:uploads/reports}") String root) {
        this.mapper = mapper; this.objectMapper = objectMapper;
        this.reportRoot = Path.of(root).toAbsolutePath().normalize();
    }

    public AdminReportResponse overview(int requestedPage, int requestedPageSize, String keyword,
                                        String project, String status) {
        mapper.ensureDefinitions();
        SummaryRow summary = mapper.findSummary();
        int pageSize = Math.max(1, Math.min(requestedPageSize, 100));
        String normalizedKeyword = normalize(keyword);
        String normalizedProject = normalizeChoice(project);
        String normalizedStatus = normalizeStatus(status);
        long totalRows = mapper.countRuns(normalizedKeyword, normalizedProject, normalizedStatus);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        return new AdminReportResponse(new AdminReportResponse.Summary(
                count(summary == null ? null : summary.getDefinitionCount()),
                count(summary == null ? null : summary.getCompletedCount()),
                count(summary == null ? null : summary.getFailedCount()),
                count(summary == null ? null : summary.getGeneratedThisMonth())),
                mapper.findDefinitions().stream().map(this::toDefinition).toList(),
                mapper.findRuns(normalizedKeyword, normalizedProject, normalizedStatus,
                        pageSize, (page - 1) * pageSize).stream().map(this::toRun).toList(),
                mapper.findProjects().stream().map(row -> new AdminReportResponse.Project(row.getId(), row.getName())).toList(),
                mapper.findOwners().stream().map(row -> new AdminReportResponse.Owner(row.getId(), row.getName())).toList(),
                mapper.findTenants().stream().map(row -> new AdminReportResponse.Tenant(row.getId(), row.getName())).toList(),
                mapper.findUnits().stream().map(row -> new AdminReportResponse.Unit(
                        row.getId(), row.getProjectId(), row.getProjectName(), row.getUnitNo())).toList(),
                new AdminReportResponse.Page(totalRows, page, pageSize, totalPages));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String normalizeChoice(String value) {
        String normalized = normalize(value);
        return normalized == null || normalized.contains("全部") ? null : normalized;
    }

    private String normalizeStatus(String value) {
        String normalized = normalizeChoice(value);
        if (normalized == null) return null;
        return switch (normalized) {
            case "已完成" -> "completed";
            case "產生中" -> "active";
            case "產生失敗" -> "failed";
            default -> normalized;
        };
    }

    public AdminReportResponse.Run generate(Long actorId, AdminReportGenerateRequest request) {
        if (request.dateEnd().isBefore(request.dateStart())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report end date cannot be before start date");
        }
        int scopeCount = (request.projectId() == null ? 0 : 1) + (request.ownerId() == null ? 0 : 1)
                + (request.tenantId() == null ? 0 : 1) + (request.unitId() == null ? 0 : 1);
        if (scopeCount > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose only one report scope");
        }
        if ("owner_statement".equals(request.reportType()) && request.ownerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner statement requires one owner");
        }
        if ("tenant_statement".equals(request.reportType()) && request.tenantId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant statement requires one tenant");
        }
        if (request.tenantId() != null && !"tenant_statement".equals(request.reportType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant scope is only available for tenant statements");
        }
        mapper.ensureDefinitions();
        DefinitionRow definition = mapper.findDefinitionByType(request.reportType());
        if (definition == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report definition not found");
        NewRun run = new NewRun(); run.setDefinitionId(definition.getId()); run.setReportName(definition.getName());
        run.setRequestedBy(actorId); run.setDateStart(request.dateStart()); run.setDateEnd(request.dateEnd());
        run.setProjectId(request.projectId()); run.setOutputFormat(request.outputFormat());
        run.setFiltersJson(filters(request));
        if (mapper.insertRun(run) != 1 || run.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report run could not be created");
        }
        try {
            List<ReportDataRow> rows = data(request);
            Path target = target(run.getId(), definition.getReportCode(), request.outputFormat());
            Files.createDirectories(target.getParent());
            if ("PDF".equals(request.outputFormat())) writePdf(target, definition.getName(), request, rows);
            else writeXlsx(target, definition.getName(), request, rows);
            String storageKey = reportRoot.relativize(target).toString().replace('\\', '/');
            mapper.completeRun(run.getId(), rows.size(), storageKey);
            mapper.insertAudit(actorId, run.getId(), definition.getName());
        } catch (Exception exception) {
            mapper.failRun(run.getId(), message(exception));
        }
        return toRun(mapper.findRun(run.getId()));
    }

    public Download download(Long runId) {
        RunRow run = requireRun(runId);
        if (!"completed".equals(run.getStatus()) || run.getStorageKey() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report file is not available");
        }
        Path file = reportRoot.resolve(run.getStorageKey()).normalize();
        if (!file.startsWith(reportRoot)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid report path");
        if (!Files.isRegularFile(file)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report file not found");
        try { return new Download(file, downloadName(run), Files.size(file)); }
        catch (IOException exception) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read report file", exception); }
    }

    private List<ReportDataRow> data(AdminReportGenerateRequest request) {
        return switch (request.reportType()) {
            case "income_expense", "owner_statement" -> withRunningBalance(mapper.findIncomeExpenseRows(
                    request.dateStart(), request.dateEnd(), request.projectId(), request.ownerId(), request.unitId()));
            case "tenant_statement" -> mapper.findTenantStatementRows(
                    request.dateStart(), request.dateEnd(), request.tenantId());
            case "maintenance" -> mapper.findMaintenanceRows(request.dateStart(), request.dateEnd(),
                    request.projectId(), request.ownerId(), request.unitId());
            case "reserve" -> mapper.findReserveRows(request.dateStart(), request.dateEnd(),
                    request.projectId(), request.ownerId(), request.unitId());
            case "sync" -> mapper.findSyncRows(request.dateStart(), request.dateEnd());
            default -> mapper.findFinanceReportRows(request.reportType(), request.dateStart(), request.dateEnd(),
                    request.projectId(), request.ownerId(), request.unitId());
        };
    }

    private List<ReportDataRow> withRunningBalance(List<ReportDataRow> rows) {
        BigDecimal runningBalance = BigDecimal.ZERO;
        for (ReportDataRow row : rows) {
            row.setBalance(runningBalance);
            runningBalance = runningBalance.add(zero(row.getExpense())).subtract(zero(row.getIncome()));
        }
        return rows;
    }

    private void writeXlsx(Path target, String title, AdminReportGenerateRequest request,
                           List<ReportDataRow> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); OutputStream output = Files.newOutputStream(target)) {
            Sheet sheet = workbook.createSheet("Report");
            boolean incomeExpense = isIncomeExpense(request.reportType());
            if (!incomeExpense) {
                CellStyle titleStyle = workbook.createCellStyle(); Font titleFont = workbook.createFont();
                titleFont.setBold(true); titleFont.setFontHeightInPoints((short) 16); titleStyle.setFont(titleFont);
                Row titleRow = sheet.createRow(0); Cell titleCell = titleRow.createCell(0); titleCell.setCellValue(title); titleCell.setCellStyle(titleStyle);
                sheet.createRow(1).createCell(0).setCellValue(request.dateStart() + " ~ " + request.dateEnd());
            }
            CellStyle headerStyle = workbook.createCellStyle(); Font headerFont = workbook.createFont(); headerFont.setBold(true);
            headerFont.setColor(incomeExpense ? IndexedColors.BLACK.getIndex() : IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont); headerStyle.setFillForegroundColor(incomeExpense ? IndexedColors.GREY_25_PERCENT.getIndex() : IndexedColors.DARK_BLUE.getIndex()); headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            if (incomeExpense) {
                headerStyle.setAlignment(HorizontalAlignment.CENTER); headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                headerStyle.setWrapText(true); headerStyle.setBorderTop(BorderStyle.THIN); headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN); headerStyle.setBorderRight(BorderStyle.THIN);
            }
            CellStyle incomeExpenseCellStyle = workbook.createCellStyle();
            incomeExpenseCellStyle.setVerticalAlignment(VerticalAlignment.TOP); incomeExpenseCellStyle.setWrapText(true);
            incomeExpenseCellStyle.setBorderTop(BorderStyle.THIN); incomeExpenseCellStyle.setBorderBottom(BorderStyle.THIN);
            incomeExpenseCellStyle.setBorderLeft(BorderStyle.THIN); incomeExpenseCellStyle.setBorderRight(BorderStyle.THIN);
            List<String> headers = headers(request.reportType());
            int headerIndex = incomeExpense ? 0 : 3;
            Row header = sheet.createRow(headerIndex); for (int i = 0; i < headers.size(); i++) { Cell cell = header.createCell(i); cell.setCellValue(headers.get(i)); cell.setCellStyle(headerStyle); }
            if (incomeExpense) header.setHeightInPoints(30);
            int index = headerIndex + 1;
            for (ReportDataRow source : rows) {
                Row row = sheet.createRow(index++); List<String> values = values(source, request.reportType());
                for (int i = 0; i < values.size(); i++) {
                    Cell cell = row.createCell(i);
                    if (numericColumn(request.reportType(), i)) cell.setCellValue(new BigDecimal(values.get(i)).doubleValue());
                    else cell.setCellValue(values.get(i));
                    if (incomeExpense) cell.setCellStyle(incomeExpenseCellStyle);
                }
                if (incomeExpense && source.getBankInfo() != null && source.getBankInfo().contains("\n")) row.setHeightInPoints(58);
            }
            for (int i = 0; i < headers.size(); i++) { sheet.autoSizeColumn(i); sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 700, 12000)); }
            sheet.createFreezePane(0, incomeExpense ? 1 : 4); workbook.write(output);
        }
    }

    private void writePdf(Path target, String title, AdminReportGenerateRequest request,
                          List<ReportDataRow> rows) throws Exception {
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        try (OutputStream output = Files.newOutputStream(target)) {
            PdfWriter.getInstance(document, output); document.open();
            BaseFont base = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(base, 16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font bodyFont = new com.lowagie.text.Font(base, 8);
            Paragraph heading = new Paragraph(title, titleFont); heading.setAlignment(Element.ALIGN_CENTER); document.add(heading);
            Paragraph period = new Paragraph(request.dateStart() + " ~ " + request.dateEnd() + "　記錄數：" + rows.size(), bodyFont);
            period.setAlignment(Element.ALIGN_CENTER); period.setSpacingAfter(12); document.add(period);
            List<String> headers = headers(request.reportType());
            PdfPTable table = new PdfPTable(headers.size()); table.setWidthPercentage(100);
            for (String header : headers) { PdfPCell cell = new PdfPCell(new Phrase(header, bodyFont)); cell.setHorizontalAlignment(Element.ALIGN_CENTER); cell.setBackgroundColor(new java.awt.Color(224, 232, 242)); table.addCell(cell); }
            for (ReportDataRow row : rows) for (String value : values(row, request.reportType())) table.addCell(new Phrase(value, bodyFont));
            document.add(table);
            // PdfWriter emits the cross-reference table and trailer during close.
            // Close the document before try-with-resources closes its stream.
            document.close();
        } finally { if (document.isOpen()) document.close(); }
    }

    private List<String> values(ReportDataRow row, String reportType) {
        if (isIncomeExpense(reportType)) {
            return List.of(text(row.getItem()), text(row.getObjectName()), text(row.getItemName()),
                    text(row.getPaymentName()), displayStatus(row.getStatus()), displayPaymentMethod(row.getPaymentMethod()),
                    dateText(row.getRealDate()), dateText(row.getPayDate()), zero(row.getIncome()).setScale(2).toPlainString(),
                    zero(row.getExpense()).setScale(2).toPlainString(), zero(row.getBalance()).setScale(2).toPlainString(),
                    text(row.getCurrency()), dateText(row.getLeaseStart()), dateText(row.getLeaseEnd()),
                    text(row.getNote()), text(row.getBankInfo()));
        }
        List<String> values = new ArrayList<>(); values.add(text(row.getRecordDate())); values.add(text(row.getReferenceNo()));
        values.add(text(row.getCategory())); values.add(text(row.getProjectName())); values.add(text(row.getUnitNo()));
        values.add(text(row.getPartyName())); values.add(text(row.getDescription())); values.add(zero(row.getAmount()).setScale(2).toPlainString());
        values.add(text(row.getStatus())); values.add(text(row.getExtraStatus())); return values;
    }
    private List<String> headers(String reportType) { return isIncomeExpense(reportType) ? INCOME_EXPENSE_HEADERS : GENERIC_HEADERS; }
    private boolean numericColumn(String reportType, int index) { return isIncomeExpense(reportType) ? index >= 8 && index <= 10 : index == 7; }
    private boolean isIncomeExpense(String reportType) { return "income_expense".equals(reportType) || "owner_statement".equals(reportType); }
    private Path target(Long runId, String code, String format) { String month = LocalDateTime.now().toLocalDate().toString().substring(0, 7); return reportRoot.resolve(month).resolve(runId + "-" + safe(code) + "." + format.toLowerCase(Locale.ROOT)).normalize(); }
    private String downloadName(RunRow run) {
        String scope = run.getScopeName() == null || run.getScopeName().isBlank() ? "全部範圍" : run.getScopeName();
        String period = run.getDateStart() != null && run.getDateEnd() != null ? run.getDateStart() + "至" + run.getDateEnd()
                : run.getDateStart() != null ? run.getDateStart().toString() : run.getDateEnd() != null ? run.getDateEnd().toString() : "未設定期間";
        String format = run.getOutputFormat() == null ? "xlsx" : run.getOutputFormat().toLowerCase(Locale.ROOT);
        return filenamePart(run.getReportName()) + "_" + filenamePart(scope) + "_" + period + "." + format;
    }
    private String filenamePart(String value) { String name = value == null || value.isBlank() ? "報表" : value.trim(); return name.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_"); }
    private String filters(AdminReportGenerateRequest request) { try { return objectMapper.writeValueAsString(request); } catch (JsonProcessingException e) { return "{}"; } }
    private String safe(String value) { return value == null ? "report" : value.replaceAll("[^A-Za-z0-9_\\-\\p{IsHan}]", "_"); }
    private String message(Exception e) {
        Throwable current = e;
        while (current.getCause() != null && (current.getMessage() == null || current.getMessage().isBlank())) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
    private String text(Object value) { return value == null || String.valueOf(value).isBlank() ? "—" : String.valueOf(value); }
    private String dateText(LocalDate value) { return value == null ? "" : value.getYear() + "/" + value.getMonthValue() + "/" + value.getDayOfMonth(); }
    private String displayStatus(String value) {
        if (value == null || value.isBlank()) return "—";
        return switch (value) {
            case "paid" -> "已付款";
            case "unpaid" -> "未付款";
            case "partial" -> "部分付款";
            case "overdue" -> "逾期";
            case "pending" -> "待確認";
            case "pending_review" -> "待確認";
            case "rejected" -> "已拒絕";
            case "failed" -> "付款失敗";
            case "voided" -> "已作廢";
            default -> value;
        };
    }
    private String displayPaymentMethod(String value) {
        if (value == null || value.isBlank()) return "—";
        return switch (value) {
            case "bank_transfer" -> "匯款";
            case "online_payment" -> "線上付款";
            case "cash" -> "現金";
            case "cheque" -> "支票";
            case "reserve_account" -> "預備金";
            case "other" -> "其他";
            default -> value;
        };
    }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private long count(Long value) { return value == null ? 0 : value; }
    private int value(Integer value) { return value == null ? 0 : value; }
    private AdminReportResponse.Definition toDefinition(DefinitionRow row) { return new AdminReportResponse.Definition(row.getId(), row.getReportCode(), row.getName(), row.getReportType(), row.getDefaultFormat(), row.getDefaultFilters(), row.getScheduleCron(), Boolean.TRUE.equals(row.getEnabled()), row.getUpdatedAt()); }
    private AdminReportResponse.Run toRun(RunRow row) { return new AdminReportResponse.Run(row.getId(), row.getDefinitionId(), row.getReportName(), row.getRequestedByName(), row.getDateStart(), row.getDateEnd(), row.getProjectId(), row.getProjectName(), row.getScopeType(), row.getScopeName(), row.getOutputFormat(), row.getStatus(), row.getRecordCount(), row.getErrorMessage(), row.getStartedAt(), row.getCompletedAt(), row.getCreatedAt(), "completed".equals(row.getStatus()) && row.getStorageKey() != null); }
    private RunRow requireRun(Long id) { RunRow row = mapper.findRun(id); if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report run not found"); return row; }
    public record Download(Path path, String filename, long size) { }
}
