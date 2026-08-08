package com.ccps.backend.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

/**
 * Generates the owner-side rental management signing documents. Template
 * selection and field placement live behind this service so the attachment
 * workspace does not need to know how a particular PDF version is built.
 */
@Service
public class RentalManagementTemplatePdfService {
    public enum TemplateType {
        PROPERTY_MANAGEMENT_AGREEMENT("property-management-agreement", "/contract-templates/ccps-pma-v1.pdf", "CCPS-PMA"),
        MANAGEMENT_AUTHORIZATION("management-authorization", "/contract-templates/ccps-management-authorization-v1.pdf", "CCPS-Management-Authorization");

        private final String key;
        private final String resource;
        private final String filePrefix;

        TemplateType(String key, String resource, String filePrefix) {
            this.key = key;
            this.resource = resource;
            this.filePrefix = filePrefix;
        }

        public String key() { return key; }
        public String resource() { return resource; }
        public String filePrefix() { return filePrefix; }
    }

    private final Path templateRoot;

    public RentalManagementTemplatePdfService(
            @Value("${ccps.storage.contract-templates:uploads/contract-templates}") String templateRoot) {
        this.templateRoot = Path.of(templateRoot).toAbsolutePath().normalize();
    }

    public byte[] generate(TemplateType type, Map<String, String> fields) {
        Map<String, String> values = fields == null ? Map.of() : fields;
        if (type == TemplateType.PROPERTY_MANAGEMENT_AGREEMENT) validatePma(values);
        try (InputStream source = openTemplate(type); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(source);
            PdfStamper stamper = new PdfStamper(reader, output);
            BaseFont latin = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            BaseFont cjk = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            if (type == TemplateType.PROPERTY_MANAGEMENT_AGREEMENT) fillPma(stamper, latin, cjk, values);
            else fillAuthorization(stamper, latin, cjk, values);
            stamper.close();
            reader.close();
            return output.toByteArray();
        } catch (IOException | DocumentException exception) {
            throw new IllegalStateException("Unable to generate rental management template", exception);
        }
    }

    public String fileName(TemplateType type, Map<String, String> fields) {
        String documentName = type == TemplateType.PROPERTY_MANAGEMENT_AGREEMENT ? "代租管合约" : "授权委托书";
        String base = Stream.of(value(fields, "landlordName"), value(fields, "projectName"), value(fields, "unitNo"),
                        documentName, value(fields, "caseNo"))
                .map(this::safeFilePart).filter(part -> !part.isBlank()).reduce((left, right) -> left + "-" + right)
                .orElse(documentName);
        return base + ".pdf";
    }

    /** Stores a new immutable template version. Existing generated files remain unchanged. */
    public TemplateVersion replace(TemplateType type, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > 20L * 1024L * 1024L
                || !"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new IllegalArgumentException("Only PDF templates up to 20MB are supported");
        }
        String version = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Path directory = templateRoot.resolve(type.key()).normalize();
        Path target = directory.resolve("v" + version + "-" + UUID.randomUUID().toString().substring(0, 8) + ".pdf").normalize();
        if (!directory.startsWith(templateRoot) || !target.startsWith(directory)) throw new IllegalArgumentException("Invalid template path");
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            // Validate before exposing the new version as active.
            PdfReader reader = new PdfReader(Files.readAllBytes(target));
            int pages = reader.getNumberOfPages();
            reader.close();
            if (pages != expectedPages(type)) {
                throw new IllegalArgumentException("Template page count must remain " + expectedPages(type));
            }
            return new TemplateVersion(version, pages, file.getOriginalFilename(), Files.size(target));
        } catch (Exception exception) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            throw new IllegalStateException("Unable to store contract template", exception);
        }
    }

    public TemplateVersion currentVersion(TemplateType type) {
        Path active = latest(type);
        if (active == null) return new TemplateVersion("20260604", expectedPages(type), type.filePrefix() + "-v1.pdf", -1);
        try {
            PdfReader reader = new PdfReader(Files.readAllBytes(active));
            int pages = reader.getNumberOfPages();
            reader.close();
            String name = active.getFileName().toString();
            String version = name.startsWith("v") && name.length() >= 15 ? name.substring(1, 15) : name;
            return new TemplateVersion(version, pages, name, Files.size(active));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read contract template", exception);
        }
    }

    private InputStream openTemplate(TemplateType type) throws IOException {
        Path active = latest(type);
        if (active != null) return Files.newInputStream(active);
        InputStream source = RentalManagementTemplatePdfService.class.getResourceAsStream(type.resource());
        if (source == null) throw new IllegalStateException("Contract template is missing: " + type.resource());
        return source;
    }

    private Path latest(TemplateType type) {
        Path directory = templateRoot.resolve(type.key()).normalize();
        if (!directory.startsWith(templateRoot) || !Files.isDirectory(directory)) return null;
        try (var files = Files.list(directory)) {
            return files.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".pdf"))
                    .max(Comparator.comparing(path -> path.getFileName().toString())).orElse(null);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to list contract templates", exception);
        }
    }

    private void fillPma(PdfStamper stamper, BaseFont latin, BaseFont cjk, Map<String, String> values) throws IOException {
        String owner = value(values, "landlordName");
        String identity = value(values, "landlordIdentity");
        String address = value(values, "propertyAddress");
        DateParts agreementDate = dateParts(values, "agreementDate");
        DateParts startDate = dateParts(values, "startDate");
        DateParts endDate = dateParts(values, "endDate");
        PdfContentByte page1 = stamper.getOverContent(1);
        // Page 1 contains separate day/month/year blanks. Writing a complete ISO date
        // into one blank was the cause of the original text overlap.
        text(page1, latin, cjk, agreementDate.day(), 201, 634, 7.5f, 16);
        text(page1, latin, cjk, agreementDate.month(), 253, 634, 7.5f, 44);
        text(page1, latin, cjk, agreementDate.year(), 338, 634, 7.5f, 41);
        text(page1, latin, cjk, owner, 456, 634, 7.5f, 41);
        text(page1, latin, cjk, identity, 39, 619, 7.5f, 87);
        text(page1, latin, cjk, owner, 64, 593, 8f, 65);
        text(page1, latin, cjk, agreementDate.day(), 451, 574, 7.5f, 48);
        text(page1, latin, cjk, agreementDate.month(), 517, 574, 7.5f, 45);
        text(page1, latin, cjk, agreementDate.year(), 15, 557, 7.5f, 77);
        underlinedLines(page1, latin, cjk, address, 64, 400, 367, 8f, 3, 14.6f);

        PdfContentByte page4 = stamper.getOverContent(4);
        text(page4, latin, cjk, startDate.day(), 217, 274, 7.5f, 31);
        text(page4, latin, cjk, startDate.month(), 286, 274, 7.5f, 31);
        text(page4, latin, cjk, startDate.year(), 363, 274, 7.5f, 21);
        text(page4, latin, cjk, endDate.day(), 509, 274, 7.5f, 31);
        text(page4, latin, cjk, endDate.month(), 90, 258, 7.5f, 20);
        text(page4, latin, cjk, endDate.year(), 151, 258, 7.5f, 28);
        text(page4, latin, cjk, startDate.year(), 95, 242, 7.5f, 23);
        text(page4, latin, cjk, startDate.month(), 139, 242, 7.5f, 18);
        text(page4, latin, cjk, startDate.day(), 177, 242, 7.5f, 12);
        text(page4, latin, cjk, endDate.year(), 272, 242, 7.5f, 23);
        text(page4, latin, cjk, endDate.month(), 316, 242, 7.5f, 18);
        text(page4, latin, cjk, endDate.day(), 354, 242, 7.5f, 12);

        PdfContentByte page6 = stamper.getOverContent(6);
        text(page6, latin, cjk, value(values, "bankPayeeName"), 339, 676, 7.8f, 225);
        text(page6, latin, cjk, value(values, "bankName"), 327, 659, 7.8f, 237);
        multilineText(page6, latin, cjk, value(values, "bankAddress"), 350, 642, 212, 7.2f, 2, 9f);
        text(page6, latin, cjk, value(values, "bankBranchCode"), 344, 609, 7.8f, 220);
        text(page6, latin, cjk, value(values, "bankAccountNo"), 359, 592, 7.8f, 205);
        text(page6, latin, cjk, value(values, "bankSwiftCode"), 379, 575, 7.8f, 185);
        multilineText(page6, latin, cjk, value(values, "ownerAddress"), 220, 520, 335, 7.8f, 2, 10f);
        text(page6, latin, cjk, value(values, "ownerEmail"), 220, 477, 7.8f, 335);
        text(page6, latin, cjk, value(values, "ownerPhone"), 220, 430, 7.8f, 335);

        PdfContentByte page7 = stamper.getOverContent(7);
        text(page7, latin, cjk, owner, 45, 541, 8f, 150);
        text(page7, latin, cjk, identity, 105, 507, 8f, 100);
    }

    private void validatePma(Map<String, String> values) {
        List<String> missing = Stream.of("landlordName", "landlordIdentity", "propertyAddress", "agreementDate",
                        "startDate", "endDate", "bankPayeeName", "bankName", "bankAddress", "bankBranchCode",
                        "bankAccountNo", "bankSwiftCode", "ownerAddress", "ownerEmail", "ownerPhone")
                .filter(key -> value(values, key).isBlank()).toList();
        if (!missing.isEmpty()) throw new IllegalArgumentException("Missing required PMA fields: " + String.join(", ", missing));
        dateParts(values, "agreementDate");
        dateParts(values, "startDate");
        dateParts(values, "endDate");
    }

    private DateParts dateParts(Map<String, String> values, String key) {
        try {
            LocalDate date = LocalDate.parse(value(values, key));
            return new DateParts(String.format("%02d", date.getDayOfMonth()), String.format("%02d", date.getMonthValue()),
                    String.valueOf(date.getYear()));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(key + " must use YYYY-MM-DD", exception);
        }
    }

    private void underlinedLines(PdfContentByte canvas, BaseFont latin, BaseFont cjk, String value, float x, float y,
            float width, float size, int maxLines, float lineHeight) throws IOException {
        BaseFont font = fontFor(latin, cjk, value);
        List<String> lines = wrap(value, font, size, width, maxLines);
        for (int index = 0; index < maxLines; index++) {
            underlinedText(canvas, latin, cjk, index < lines.size() ? lines.get(index) : "", x,
                    y - index * lineHeight, width, size);
        }
    }

    private void underlinedText(PdfContentByte canvas, BaseFont latin, BaseFont cjk, String value, float x, float y,
            float width, float size) throws IOException {
        canvas.saveState();
        canvas.setColorFill(Color.WHITE);
        canvas.rectangle(x - 1, y - 3, width + 2, 12);
        canvas.fill();
        canvas.setColorStroke(Color.DARK_GRAY);
        canvas.setLineWidth(.45f);
        canvas.moveTo(x, y - 2);
        canvas.lineTo(x + width, y - 2);
        canvas.stroke();
        canvas.restoreState();
        text(canvas, latin, cjk, value, x + 2, y, size, width - 4);
    }

    private void multilineText(PdfContentByte canvas, BaseFont latin, BaseFont cjk, String value, float x, float y,
            float width, float size, int maxLines, float lineHeight) throws IOException {
        BaseFont font = fontFor(latin, cjk, value);
        List<String> lines = wrap(value, font, size, width, maxLines);
        for (int index = 0; index < lines.size(); index++)
            text(canvas, latin, cjk, lines.get(index), x, y - index * lineHeight, size, width);
    }

    private List<String> wrap(String value, BaseFont font, float size, float width, int maxLines) {
        String remaining = value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
        List<String> lines = new ArrayList<>();
        while (!remaining.isBlank() && lines.size() < maxLines) {
            int end = remaining.length();
            while (end > 1 && font.getWidthPoint(remaining.substring(0, end), size) > width) end--;
            if (end < remaining.length()) {
                int space = remaining.lastIndexOf(' ', end - 1);
                if (space > Math.max(0, end / 2)) end = space;
            }
            String line = remaining.substring(0, end).trim();
            if (lines.size() == maxLines - 1 && end < remaining.length()) {
                String suffix = "…";
                while (!line.isEmpty() && font.getWidthPoint(line + suffix, size) > width)
                    line = line.substring(0, line.length() - 1).trim();
                line += suffix;
                remaining = "";
            } else remaining = remaining.substring(end).trim();
            lines.add(line);
        }
        return lines;
    }

    private void fillAuthorization(PdfStamper stamper, BaseFont latin, BaseFont cjk, Map<String, String> values) throws IOException {
        String owner = value(values, "landlordName");
        String identity = value(values, "landlordIdentity");
        String date = valueOr(values, "agreementDate", "startDate");
        String unit = valueOr(values, "unitNo", "propertyAddress");
        PdfContentByte page1 = stamper.getOverContent(1);
        text(page1, latin, cjk, date, 112, 758, 9, 150);
        text(page1, latin, cjk, value(values, "managementOffice"), 112, 729, 8.5f, 390);
        text(page1, latin, cjk, unit, 340, 598, 9, 210);
        text(page1, latin, cjk, owner, 157, 528, 8.5f, 120);
        text(page1, latin, cjk, identity, 275, 528, 8.5f, 105);
        text(page1, latin, cjk, value(values, "ownerEmail"), 385, 528, 8.5f, 165);

        PdfContentByte page3 = stamper.getOverContent(3);
        text(page3, latin, cjk, owner, 170, 340, 9, 230);
        text(page3, latin, cjk, identity, 170, 306, 9, 230);
        text(page3, latin, cjk, date, 170, 272, 9, 230);
    }

    private void text(PdfContentByte canvas, BaseFont latin, BaseFont cjk, String value, float x, float y,
            float size, float maxWidth) throws IOException {
        if (value == null || value.isBlank()) return;
        String safe = value.replace('\n', ' ').replace('\r', ' ').trim();
        BaseFont font = fontFor(latin, cjk, safe);
        float actual = size;
        while (actual > 6.2f && font.getWidthPoint(safe, actual) > maxWidth) actual -= .35f;
        canvas.beginText();
        canvas.setFontAndSize(font, actual);
        canvas.setTextMatrix(x, y);
        canvas.showText(safe);
        canvas.endText();
    }

    private BaseFont fontFor(BaseFont latin, BaseFont cjk, String value) {
        return value != null && value.chars().allMatch(ch -> ch < 128) ? latin : cjk;
    }

    private int expectedPages(TemplateType type) {
        return type == TemplateType.PROPERTY_MANAGEMENT_AGREEMENT ? 9 : 3;
    }

    private String value(Map<String, String> values, String key) {
        String value = values == null ? null : values.get(key);
        return value == null ? "" : value.trim();
    }

    private String valueOr(Map<String, String> values, String primary, String fallback) {
        String value = value(values, primary);
        return value.isBlank() ? value(values, fallback) : value;
    }

    private String safeFilePart(String value) {
        String safe = value == null ? "" : value.trim().replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "-")
                .replaceAll("\\s+", " ").replaceAll("-+", "-");
        if (safe.startsWith("-")) safe = safe.substring(1);
        if (safe.endsWith("-")) safe = safe.substring(0, safe.length() - 1);
        return safe.length() > 60 ? safe.substring(0, 60) : safe;
    }

    public record TemplateVersion(String version, int pages, String originalName, long fileSize) { }
    private record DateParts(String day, String month, String year) { }
}
