package com.ccps.backend.service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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
    private static final List<String> HEADERS = List.of("日期", "單號", "分類", "建案", "單位", "對象",
            "說明", "金額", "狀態", "附加狀態");
    private final AdminReportMapper mapper;
    private final ObjectMapper objectMapper;
    private final Path reportRoot;

    public AdminReportService(AdminReportMapper mapper, ObjectMapper objectMapper,
                              @Value("${ccps.storage.reports:uploads/reports}") String root) {
        this.mapper = mapper; this.objectMapper = objectMapper;
        this.reportRoot = Path.of(root).toAbsolutePath().normalize();
    }

    public AdminReportResponse overview() {
        mapper.ensureDefinitions();
        SummaryRow summary = mapper.findSummary();
        return new AdminReportResponse(new AdminReportResponse.Summary(
                count(summary == null ? null : summary.getDefinitionCount()),
                count(summary == null ? null : summary.getCompletedCount()),
                count(summary == null ? null : summary.getFailedCount()),
                count(summary == null ? null : summary.getGeneratedThisMonth())),
                mapper.findDefinitions().stream().map(this::toDefinition).toList(),
                mapper.findRuns().stream().map(this::toRun).toList(),
                mapper.findProjects().stream().map(row -> new AdminReportResponse.Project(row.getId(), row.getName())).toList());
    }

    public AdminReportResponse.Run generate(Long actorId, AdminReportGenerateRequest request) {
        if (request.dateEnd().isBefore(request.dateStart())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report end date cannot be before start date");
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
        try { return new Download(file, safe(run.getReportName()) + "." + run.getOutputFormat().toLowerCase(Locale.ROOT), Files.size(file)); }
        catch (IOException exception) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read report file", exception); }
    }

    private List<ReportDataRow> data(AdminReportGenerateRequest request) {
        return switch (request.reportType()) {
            case "maintenance" -> mapper.findMaintenanceRows(request.dateStart(), request.dateEnd(), request.projectId());
            case "reserve" -> mapper.findReserveRows(request.dateStart(), request.dateEnd(), request.projectId());
            case "sync" -> mapper.findSyncRows(request.dateStart(), request.dateEnd());
            default -> mapper.findFinanceReportRows(request.reportType(), request.dateStart(), request.dateEnd(), request.projectId());
        };
    }

    private void writeXlsx(Path target, String title, AdminReportGenerateRequest request,
                           List<ReportDataRow> rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); OutputStream output = Files.newOutputStream(target)) {
            Sheet sheet = workbook.createSheet("Report");
            CellStyle titleStyle = workbook.createCellStyle(); Font titleFont = workbook.createFont();
            titleFont.setBold(true); titleFont.setFontHeightInPoints((short) 16); titleStyle.setFont(titleFont);
            Row titleRow = sheet.createRow(0); Cell titleCell = titleRow.createCell(0); titleCell.setCellValue(title); titleCell.setCellStyle(titleStyle);
            sheet.createRow(1).createCell(0).setCellValue(request.dateStart() + " ~ " + request.dateEnd());
            CellStyle headerStyle = workbook.createCellStyle(); Font headerFont = workbook.createFont(); headerFont.setBold(true); headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont); headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Row header = sheet.createRow(3); for (int i = 0; i < HEADERS.size(); i++) { Cell cell = header.createCell(i); cell.setCellValue(HEADERS.get(i)); cell.setCellStyle(headerStyle); }
            int index = 4;
            for (ReportDataRow source : rows) {
                Row row = sheet.createRow(index++); List<String> values = values(source);
                for (int i = 0; i < values.size(); i++) {
                    Cell cell = row.createCell(i);
                    if (i == 7) cell.setCellValue(zero(source.getAmount()).doubleValue()); else cell.setCellValue(values.get(i));
                }
            }
            for (int i = 0; i < HEADERS.size(); i++) { sheet.autoSizeColumn(i); sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 700, 12000)); }
            sheet.createFreezePane(0, 4); workbook.write(output);
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
            PdfPTable table = new PdfPTable(HEADERS.size()); table.setWidthPercentage(100);
            for (String header : HEADERS) { PdfPCell cell = new PdfPCell(new Phrase(header, bodyFont)); cell.setHorizontalAlignment(Element.ALIGN_CENTER); cell.setBackgroundColor(new java.awt.Color(224, 232, 242)); table.addCell(cell); }
            for (ReportDataRow row : rows) for (String value : values(row)) table.addCell(new Phrase(value, bodyFont));
            document.add(table);
            // PdfWriter emits the cross-reference table and trailer during close.
            // Close the document before try-with-resources closes its stream.
            document.close();
        } finally { if (document.isOpen()) document.close(); }
    }

    private List<String> values(ReportDataRow row) {
        List<String> values = new ArrayList<>(); values.add(text(row.getRecordDate())); values.add(text(row.getReferenceNo()));
        values.add(text(row.getCategory())); values.add(text(row.getProjectName())); values.add(text(row.getUnitNo()));
        values.add(text(row.getPartyName())); values.add(text(row.getDescription())); values.add(zero(row.getAmount()).setScale(2).toPlainString());
        values.add(text(row.getStatus())); values.add(text(row.getExtraStatus())); return values;
    }
    private Path target(Long runId, String code, String format) { String month = LocalDateTime.now().toLocalDate().toString().substring(0, 7); return reportRoot.resolve(month).resolve(runId + "-" + safe(code) + "." + format.toLowerCase(Locale.ROOT)).normalize(); }
    private String filters(AdminReportGenerateRequest request) { try { return objectMapper.writeValueAsString(request); } catch (JsonProcessingException e) { return "{}"; } }
    private String safe(String value) { return value == null ? "report" : value.replaceAll("[^A-Za-z0-9_\\-\\p{IsHan}]", "_"); }
    private String message(Exception e) {
        Throwable current = e;
        while (current.getCause() != null && (current.getMessage() == null || current.getMessage().isBlank())) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }
    private String text(Object value) { return value == null || String.valueOf(value).isBlank() ? "—" : String.valueOf(value); }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private long count(Long value) { return value == null ? 0 : value; }
    private int value(Integer value) { return value == null ? 0 : value; }
    private AdminReportResponse.Definition toDefinition(DefinitionRow row) { return new AdminReportResponse.Definition(row.getId(), row.getReportCode(), row.getName(), row.getReportType(), row.getDefaultFormat(), row.getDefaultFilters(), row.getScheduleCron(), Boolean.TRUE.equals(row.getEnabled()), row.getUpdatedAt()); }
    private AdminReportResponse.Run toRun(RunRow row) { return new AdminReportResponse.Run(row.getId(), row.getDefinitionId(), row.getReportName(), row.getRequestedByName(), row.getDateStart(), row.getDateEnd(), row.getProjectId(), row.getProjectName(), row.getOutputFormat(), row.getStatus(), row.getRecordCount(), row.getErrorMessage(), row.getStartedAt(), row.getCompletedAt(), row.getCreatedAt(), "completed".equals(row.getStatus()) && row.getStorageKey() != null); }
    private RunRow requireRun(Long id) { RunRow row = mapper.findRun(id); if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report run not found"); return row; }
    public record Download(Path path, String filename, long size) { }
}
