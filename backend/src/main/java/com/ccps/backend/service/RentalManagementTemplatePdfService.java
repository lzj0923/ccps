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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final float MANAGEMENT_AUTHORIZATION_EMAIL_MIN_WIDTH = 87f;
    private static final Set<String> ALLOWED_FIELD_KEYS = Set.of("agreementDate", "agreementDate.day",
            "agreementDate.month", "agreementDate.year", "startDate.day", "startDate.month", "startDate.year",
            "endDate.day", "endDate.month", "endDate.year", "landlordName", "landlordIdentity",
            "projectName", "unitNo", "propertyAddress", "unitNoOrAddress", "managementOffice", "bankPayeeName", "bankName",
            "bankAddress", "bankBranchCode", "bankAccountNo", "bankSwiftCode", "ownerAddress", "ownerEmail",
            "ownerPhone", "authorizedAgentName", "authorizedAgentIdentity", "authorizedAgentPhone",
            "authorizedAgentEmail");
    private static final Set<String> SIGNATURE_FIELD_KEYS = Set.of("signature.owner", "signature.company",
            "signature.customer_service");
    public enum TemplateType {
        PROPERTY_MANAGEMENT_AGREEMENT("property-management-agreement", "/contract-templates/flattened/ccps-pma-v1.pdf", "CCPS-PMA"),
        MANAGEMENT_AUTHORIZATION("management-authorization", "/contract-templates/flattened/ccps-management-authorization-v1.pdf", "CCPS-Management-Authorization"),
        TERMINATION_LETTER("termination-letter", "/contract-templates/flattened/ccps-termination-letter-v1.pdf", "CCPS-Termination-Letter"),
        RENTAL_REMITTANCE("rental-remittance", "/contract-templates/flattened/ccps-rental-remittance-v1.pdf", "CCPS-Rental-Remittance");

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
        if (type == TemplateType.MANAGEMENT_AUTHORIZATION) validateManagementAuthorization(values);
        if (type == TemplateType.TERMINATION_LETTER) validateTerminationLetter(values);
        if (type == TemplateType.RENTAL_REMITTANCE) validateRentalRemittance(values);
        try (InputStream source = openTemplate(type); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(source);
            PdfStamper stamper = new PdfStamper(reader, output);
            BaseFont latin = PdfFontResources.regular();
            BaseFont cjk = latin;
            if (Files.isRegularFile(layoutPath(type))) fillConfigured(stamper, latin, cjk, values, currentLayout(type));
            else if (type == TemplateType.PROPERTY_MANAGEMENT_AGREEMENT) fillPma(stamper, latin, cjk, values);
            else if (type == TemplateType.TERMINATION_LETTER) fillTerminationLetter(stamper, latin, cjk, values);
            else if (type == TemplateType.RENTAL_REMITTANCE) fillConfigured(stamper, latin, cjk, values, currentLayout(type));
            else fillAuthorization(stamper, latin, cjk, values);
            stamper.close();
            reader.close();
            return output.toByteArray();
        } catch (IOException | DocumentException exception) {
            throw new IllegalStateException("Unable to generate rental management template", exception);
        }
    }

    public String fileName(TemplateType type, Map<String, String> fields) {
        String documentName = switch (type) {
            case PROPERTY_MANAGEMENT_AGREEMENT -> "代租管合约";
            case MANAGEMENT_AUTHORIZATION -> "授权委托书";
            case TERMINATION_LETTER -> "终止通知书";
            case RENTAL_REMITTANCE -> "租金汇款授权书";
        };
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
            if (pages < expectedPages(type) || pages > 50) {
                throw new IllegalArgumentException("Template must contain at least " + expectedPages(type) + " pages");
            }
            return new TemplateVersion(version, pages, file.getOriginalFilename(), Files.size(target));
        } catch (IllegalArgumentException exception) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            throw exception;
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

    public byte[] currentTemplate(TemplateType type) {
        try (InputStream source = openTemplate(type)) {
            return source.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read contract template", exception);
        }
    }

    public TemplateLayout currentLayout(TemplateType type) {
        TemplateVersion version = currentVersion(type);
        Path layout = layoutPath(type);
        if (Files.isRegularFile(layout)) {
            try {
                TemplateLayout saved = JSON.readValue(Files.readAllBytes(layout), TemplateLayout.class);
                if (version.version().equals(saved.version())) return migrateSavedLayout(type, saved);
            } catch (IOException exception) {
                throw new IllegalStateException("Unable to read contract template layout", exception);
            }
        }
        List<PageSize> pageSizes = currentPageSizes(type);
        return new TemplateLayout(version.version(), version.pages(), pageSizes, normalizedDefaultFields(type, pageSizes));
    }

    private TemplateLayout migrateSavedLayout(TemplateType type, TemplateLayout saved) {
        if (saved.fields() == null) return saved;
        List<TemplateFieldPosition> fields = saved.fields();
        if (type == TemplateType.MANAGEMENT_AUTHORIZATION
                && fields.stream().anyMatch(field -> "managementOffice".equals(field.fieldKey())
                        || "unitNoOrAddress".equals(field.fieldKey()))) {
            Map<String, TemplateFieldPosition> savedById = fields.stream()
                    .collect(java.util.stream.Collectors.toMap(TemplateFieldPosition::id, field -> field,
                            (first, ignored) -> first));
            fields = normalizedDefaultFields(type, saved.pageSizes()).stream()
                    .map(defaultField -> {
                        TemplateFieldPosition existing = savedById.get(defaultField.id());
                        return existing != null && defaultField.fieldKey().equals(existing.fieldKey())
                                ? existing : defaultField;
                    })
                    .toList();
        }
        if (type == TemplateType.MANAGEMENT_AUTHORIZATION) {
            fields = fields.stream().map(this::normalizeManagementAuthorizationField).toList();
        }
        return withMissingDefaultFields(type, new TemplateLayout(saved.version(), saved.pages(), saved.pageSizes(), fields));
    }

    private TemplateFieldPosition normalizeManagementAuthorizationField(TemplateFieldPosition field) {
        if (!"email-page-1".equals(field.id())) return field;
        return new TemplateFieldPosition(field.id(), field.fieldKey(), field.label(), field.page(), field.x(), field.y(),
                Math.max(8f, field.fontSize()), Math.max(MANAGEMENT_AUTHORIZATION_EMAIL_MIN_WIDTH, field.maxWidth()),
                field.maxLines(), field.lineHeight());
    }

    private TemplateLayout withMissingDefaultFields(TemplateType type, TemplateLayout layout) {
        List<TemplateFieldPosition> existing = layout.fields() == null ? List.of() : layout.fields();
        Set<String> ids = existing.stream().map(TemplateFieldPosition::id).collect(java.util.stream.Collectors.toSet());
        List<TemplateFieldPosition> merged = new ArrayList<>(existing);
        normalizedDefaultFields(type, layout.pageSizes()).stream()
                .filter(field -> !ids.contains(field.id()))
                .forEach(merged::add);
        return new TemplateLayout(layout.version(), layout.pages(), layout.pageSizes(), List.copyOf(merged));
    }

    public TemplateLayout saveLayout(TemplateType type, TemplateLayout request) {
        TemplateVersion active = currentVersion(type);
        if (request == null || !active.version().equals(request.version())) {
            throw new IllegalArgumentException("Template version changed; reload the latest template before saving positions");
        }
        if (request.pages() != active.pages() || request.fields() == null || request.fields().isEmpty()) {
            throw new IllegalArgumentException("Template fields are required");
        }
        List<PageSize> pageSizes = currentPageSizes(type);
        Set<String> ids = new HashSet<>();
        List<TemplateFieldPosition> normalizedFields = new ArrayList<>();
        for (TemplateFieldPosition field : request.fields()) {
            if (field == null || field.id() == null || field.id().isBlank() || !ids.add(field.id())
                    || !isAllowedFieldKey(field.fieldKey())) {
                throw new IllegalArgumentException("Invalid template field position");
            }
            int page = Math.max(1, Math.min(active.pages(), field.page()));
            PageSize pageSize = pageSizes.get(page - 1);
            float x = clamp(finiteOr(field.x(), 0f), 0f, pageSize.width());
            float y = clamp(finiteOr(field.y(), 0f), 0f, pageSize.height());
            float fontSize = clamp(finiteOr(field.fontSize(), 9f), 6f, 30f);
            float maxWidth = clamp(finiteOr(field.maxWidth(), 120f), 20f, pageSize.width());
            int maxLines = Math.max(1, Math.min(8, field.maxLines()));
            float lineHeight = clamp(finiteOr(field.lineHeight(), fontSize + 2f), 6f, 40f);
            normalizedFields.add(new TemplateFieldPosition(field.id(), field.fieldKey(), field.label(), page, x, y,
                    fontSize, maxWidth, maxLines, lineHeight));
        }
        Set<String> savedIds = normalizedFields.stream().map(TemplateFieldPosition::id)
                .collect(java.util.stream.Collectors.toSet());
        normalizedDefaultFields(type, pageSizes).stream()
                .filter(field -> !savedIds.contains(field.id()))
                .forEach(normalizedFields::add);
        TemplateLayout saved = new TemplateLayout(active.version(), active.pages(), pageSizes,
                List.copyOf(normalizedFields));
        Path target = layoutPath(type);
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            Files.createDirectories(target.getParent());
            Files.write(temporary, JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(saved));
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            return saved;
        } catch (IOException exception) {
            try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            throw new IllegalStateException("Unable to store contract template layout", exception);
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

    private Path layoutPath(TemplateType type) {
        Path active = latest(type);
        Path directory = templateRoot.resolve(type.key()).normalize();
        String fileName = active == null ? "builtin.layout.json"
                : active.getFileName().toString().replaceFirst("(?i)\\.pdf$", ".layout.json");
        Path result = directory.resolve(fileName).normalize();
        if (!result.startsWith(directory)) throw new IllegalArgumentException("Invalid template layout path");
        return result;
    }

    private List<PageSize> currentPageSizes(TemplateType type) {
        try (InputStream source = openTemplate(type)) {
            PdfReader reader = new PdfReader(source);
            List<PageSize> sizes = new ArrayList<>();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                var size = reader.getPageSize(page);
                sizes.add(new PageSize(size.getWidth(), size.getHeight()));
            }
            reader.close();
            return List.copyOf(sizes);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read contract template page sizes", exception);
        }
    }

    private List<TemplateFieldPosition> defaultFields(TemplateType type) {
        if (type == TemplateType.TERMINATION_LETTER) {
            return List.of(
                    field("agreement-date-page-1", "agreementDate", "日期", 1, 117, 624, 8, 130),
                    field("owner-page-1", "landlordName", "业主姓名", 1, 64, 500, 8, 115),
                    field("project-page-1", "projectName", "建案名称", 1, 348, 500, 8, 100),
                    field("unit-page-1", "unitNo", "单位号码", 1, 54, 485, 8, 98),
                    field("agent-name-page-1", "authorizedAgentName", "授权代理人姓名", 1, 106, 454, 8, 74),
                    field("owner-cn-page-1", "landlordName", "业主姓名（中文段）", 1, 86, 376, 8, 115),
                    field("project-cn-page-1", "projectName", "建案名称（中文段）", 1, 260, 376, 8, 94),
                    field("unit-cn-page-1", "unitNo", "单位号码（中文段）", 1, 420, 376, 8, 77),
                    field("agent-name-cn-page-1", "authorizedAgentName", "代理人姓名（中文段）", 1, 138, 343, 8, 92),
                    field("agent-name-details-page-1", "authorizedAgentName", "代理人姓名（明细）", 1, 240, 256, 8, 300),
                    field("agent-identity-page-1", "authorizedAgentIdentity", "授权代理人证件号码", 1, 240, 228, 8, 300),
                    field("agent-phone-page-1", "authorizedAgentPhone", "授权代理人联系电话", 1, 240, 200, 8, 300),
                    field("agent-email-page-1", "authorizedAgentEmail", "授权代理人邮箱", 1, 240, 173, 8, 300),
                    field("bank-name-page-2", "bankName", "银行名称", 2, 220, 637, 8, 320),
                    field("bank-payee-page-2", "bankPayeeName", "账户持有人", 2, 220, 609, 8, 320),
                    field("bank-account-page-2", "bankAccountNo", "银行账号", 2, 220, 581, 8, 320),
                    field("bank-swift-page-2", "bankSwiftCode", "SWIFT 代码", 2, 220, 553, 8, 320),
                    multilineField("bank-address-page-2", "bankAddress", "银行地址", 2, 220, 526, 8, 320, 2, 11),
                    field("owner-signature-name-page-2", "landlordName", "业主姓名（签署页）", 2, 242, 239, 8, 295),
                    field("owner-signature-identity-page-2", "landlordIdentity", "业主证件号码（签署页）", 2, 242, 224, 8, 295),
                    signatureField("signature-owner-page-2", "signature.owner", "业主签字", 2, 54, 250, 165, 32),
                    signatureField("signature-company-page-2", "signature.company", "物业管理公司授权代表签字", 2, 54, 139, 165, 32));
        }
        if (type == TemplateType.RENTAL_REMITTANCE) {
            return List.of(
                    field("date-page-1", "agreementDate", "日期", 1, 128, 758, 9, 110),
                    field("owner-page-1", "landlordName", "屋主姓名", 1, 250, 537, 8.5f, 245),
                    field("passport-page-1", "landlordIdentity", "护照号码", 1, 248, 522, 8.5f, 245),
                    field("unit-page-1", "unitNo", "单位号码", 1, 216, 507, 8.5f, 245),
                    field("payee-page-1", "bankPayeeName", "授权收款人姓名", 1, 310, 395, 8.5f, 195),
                    field("bank-page-1", "bankName", "银行", 1, 132, 370, 8.5f, 390),
                    field("account-page-1", "bankAccountNo", "账号", 1, 168, 341, 8.5f, 350),
                    field("swift-page-1", "bankSwiftCode", "SWIFT 代码", 1, 156, 313, 8.5f, 365),
                    field("branch-page-1", "bankBranchCode", "分行代码", 1, 212, 285, 8.5f, 305),
                    field("address-page-1", "bankAddress", "银行地址", 1, 194, 256, 8.5f, 318),
                    signatureField("signature-owner-page-1", "signature.owner", "屋主签字", 1, 72, 116, 112, 34));
        }
        if (type == TemplateType.MANAGEMENT_AUTHORIZATION) {
            return List.of(
                    field("agreement-date-page-1", "agreementDate", "日期", 1, 100, 760, 9, 105),
                    field("project-page-1", "projectName", "建案名称", 1, 348, 601, 8, 58),
                    field("unit-page-1", "unitNo", "单位号码", 1, 460, 601, 8, 73),
                    field("owner-page-1", "landlordName", "业主姓名", 1, 130, 531, 8, 76),
                    field("identity-page-1", "landlordIdentity", "护照号码", 1, 297, 531, 8, 75),
                    field("email-page-1", "ownerEmail", "业主邮箱", 1, 448, 531, 8,
                            MANAGEMENT_AUTHORIZATION_EMAIL_MIN_WIDTH),
                    field("owner-page-3", "landlordName", "业主姓名（签署页）", 3, 190, 332, 9, 230),
                    field("identity-page-3", "landlordIdentity", "护照号码（签署页）", 3, 190, 298, 9, 230),
                    field("agreement-date-page-3", "agreementDate", "日期（签署页）", 3, 190, 264, 9, 230),
                    signatureField("signature-owner-page-3", "signature.owner", "业主签字", 3, 72, 382, 112, 24));
        }
        return List.of(
                field("agreement-day-page-1", "agreementDate.day", "协议日", 1, 194, 634, 7.5f, 18),
                field("agreement-month-page-1", "agreementDate.month", "协议月", 1, 245, 634, 7.5f, 24),
                field("agreement-year-page-1", "agreementDate.year", "协议年", 1, 309, 634, 7.5f, 22),
                field("owner-page-1-a", "landlordName", "业主姓名", 1, 404, 634, 7.5f, 100),
                field("identity-page-1", "landlordIdentity", "证件号码", 1, 45, 619, 7.5f, 84),
                field("owner-page-1-b", "landlordName", "业主姓名（中文）", 1, 68, 593, 8, 100),
                field("identity-page-1-b", "landlordIdentity", "证件号码（中文）", 1, 268, 593, 7.5f, 66),
                field("agreement-day-page-1-b", "agreementDate.day", "协议日（中文）", 1, 518, 574, 7.5f, 40),
                field("agreement-month-page-1-b", "agreementDate.month", "协议月（中文）", 1, 20, 557, 7.5f, 50),
                field("agreement-year-page-1-b", "agreementDate.year", "协议年（中文）", 1, 81, 557, 7.5f, 60),
                multilineField("property-address-page-1", "propertyAddress", "房产地址", 1, 71, 401, 8, 485, 3, 14.6f),
                field("start-day-page-4-a", "startDate.day", "生效日", 4, 220, 314, 7.5f, 27),
                field("start-month-page-4-a", "startDate.month", "生效月", 4, 285, 314, 7.5f, 27),
                field("start-year-page-4-a", "startDate.year", "生效年", 4, 357, 314, 7.5f, 18),
                field("end-day-page-4-a", "endDate.day", "到期日", 4, 498, 314, 7.5f, 27),
                field("end-month-page-4-a", "endDate.month", "到期月", 4, 56, 298, 7.5f, 35),
                field("end-year-page-4-a", "endDate.year", "到期年", 4, 136, 298, 7.5f, 31),
                field("start-year-page-4-b", "startDate.year", "生效年（中文）", 4, 101, 282, 7.5f, 23),
                field("start-month-page-4-b", "startDate.month", "生效月（中文）", 4, 145, 282, 7.5f, 18),
                field("start-day-page-4-b", "startDate.day", "生效日（中文）", 4, 183, 282, 7.5f, 12),
                field("end-year-page-4-b", "endDate.year", "到期年（中文）", 4, 281, 282, 7.5f, 23),
                field("end-month-page-4-b", "endDate.month", "到期月（中文）", 4, 325, 282, 7.5f, 18),
                field("end-day-page-4-b", "endDate.day", "到期日（中文）", 4, 363, 282, 7.5f, 12),
                field("payee-page-6", "bankPayeeName", "收款人姓名", 6, 345, 642, 7.8f, 219),
                field("bank-page-6", "bankName", "银行名称", 6, 333, 625, 7.8f, 231),
                multilineField("bank-address-page-6", "bankAddress", "银行地址", 6, 356, 608, 7.2f, 206, 2, 9),
                field("branch-page-6", "bankBranchCode", "银行代码", 6, 350, 575, 7.8f, 214),
                field("account-page-6", "bankAccountNo", "银行账号", 6, 365, 558, 7.8f, 199),
                field("swift-page-6", "bankSwiftCode", "SWIFT 代码", 6, 385, 541, 7.8f, 179),
                multilineField("owner-address-page-6", "ownerAddress", "业主地址", 6, 226, 486, 7.8f, 329, 2, 10),
                field("owner-email-page-6", "ownerEmail", "业主邮箱", 6, 226, 443, 7.8f, 329),
                field("owner-phone-page-6", "ownerPhone", "业主电话", 6, 226, 396, 7.8f, 329),
                 field("owner-page-7", "landlordName", "业主姓名（签署页）", 7, 51, 542, 8, 150),
                 field("identity-page-7", "landlordIdentity", "证件号码（签署页）", 7, 111, 508, 8, 100),
                 signatureField("signature-owner-page-7", "signature.owner", "业主签字", 7, 246, 575, 118, 34),
                 signatureField("signature-company-page-7", "signature.company", "物业管理公司签字", 7, 246, 251, 118, 34),
                 signatureField("signature-customer-service-1-page-7", "signature.customer_service", "客服见证人签字 1", 7, 33, 464, 92, 27),
                 signatureField("signature-customer-service-2-page-7", "signature.customer_service", "客服见证人签字 2", 7, 33, 190, 92, 27));
    }

    private List<TemplateFieldPosition> normalizedDefaultFields(TemplateType type, List<PageSize> pageSizes) {
        int pages = pageSizes.size();
        return defaultFields(type).stream().map(field -> {
            int page = Math.max(1, Math.min(pages, field.page()));
            PageSize size = pageSizes.get(page - 1);
            float x = Math.max(0, Math.min(size.width(), field.x()));
            float y = Math.max(0, Math.min(size.height(), field.y()));
            return new TemplateFieldPosition(field.id(), field.fieldKey(), field.label(), page, x, y,
                    field.fontSize(), field.maxWidth(), field.maxLines(), field.lineHeight());
        }).toList();
    }

    private TemplateFieldPosition field(String id, String key, String label, int page, float x, float y,
            float size, float width) {
        return new TemplateFieldPosition(id, key, label, page, x, y, size, width, 1, Math.max(8, size + 2));
    }

    private TemplateFieldPosition signatureField(String id, String key, String label, int page, float x, float y,
            float width, float height) {
        return new TemplateFieldPosition(id, key, label, page, x, y, 9f, width, 1, height);
    }

    private TemplateFieldPosition multilineField(String id, String key, String label, int page, float x, float y,
            float size, float width, int lines, float lineHeight) {
        return new TemplateFieldPosition(id, key, label, page, x, y, size, width, lines, lineHeight);
    }

    private float finiteOr(float value, float fallback) {
        return Float.isFinite(value) && value > 0 ? value : fallback;
    }

    private float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private void fillConfigured(PdfStamper stamper, BaseFont latin, BaseFont cjk, Map<String, String> values,
            TemplateLayout layout) throws IOException {
        for (TemplateFieldPosition field : layout.fields()) {
            if (isSignatureField(field)) continue;
            String resolved = resolveFieldValue(values, field.fieldKey());
            if (field.maxLines() > 1) {
                multilineText(stamper.getOverContent(field.page()), latin, cjk, resolved, field.x(), field.y(),
                        field.maxWidth(), field.fontSize(), field.maxLines(), field.lineHeight());
            } else {
                text(stamper.getOverContent(field.page()), latin, cjk, resolved, field.x(), field.y(),
                        field.fontSize(), field.maxWidth(), minimumFontSize(field));
            }
        }
    }

    private boolean isAllowedFieldKey(String key) {
        return ALLOWED_FIELD_KEYS.contains(key) || SIGNATURE_FIELD_KEYS.contains(key);
    }

    public static boolean isSignatureField(TemplateFieldPosition field) {
        return field != null && SIGNATURE_FIELD_KEYS.contains(field.fieldKey());
    }

    private float minimumFontSize(TemplateFieldPosition field) {
        return switch (field.id()) {
            case "project-page-1", "unit-page-1", "owner-page-1", "identity-page-1" -> 4.5f;
            default -> 6.2f;
        };
    }

    private String resolveFieldValue(Map<String, String> values, String key) {
        if ("unitNoOrAddress".equals(key)) return valueOr(values, "unitNo", "propertyAddress");
        int separator = key.indexOf('.');
        if (separator < 0) {
            String resolved = value(values, key);
            return List.of("agreementDate", "startDate", "endDate").contains(key)
                    ? displayDate(resolved) : resolved;
        }
        DateParts parts = dateParts(values, key.substring(0, separator));
        return switch (key.substring(separator + 1)) {
            case "day" -> parts.day();
            case "month" -> parts.month();
            case "year" -> parts.year();
            default -> "";
        };
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
        text(page1, latin, cjk, agreementDate.day(), 194, 634, 7.5f, 18);
        text(page1, latin, cjk, agreementDate.month(), 245, 634, 7.5f, 24);
        text(page1, latin, cjk, agreementDate.year(), 309, 634, 7.5f, 22);
        text(page1, latin, cjk, owner, 404, 634, 7.5f, 100);
        text(page1, latin, cjk, identity, 45, 619, 7.5f, 84);
        text(page1, latin, cjk, owner, 68, 593, 8f, 100);
        text(page1, latin, cjk, identity, 268, 593, 7.5f, 66);
        text(page1, latin, cjk, agreementDate.day(), 518, 574, 7.5f, 40);
        text(page1, latin, cjk, agreementDate.month(), 20, 557, 7.5f, 50);
        text(page1, latin, cjk, agreementDate.year(), 81, 557, 7.5f, 60);
        underlinedLines(page1, latin, cjk, address, 69, 401, 487, 8f, 3, 14.6f);

        PdfContentByte page4 = stamper.getOverContent(4);
        text(page4, latin, cjk, startDate.day(), 220, 314, 7.5f, 27);
        text(page4, latin, cjk, startDate.month(), 285, 314, 7.5f, 27);
        text(page4, latin, cjk, startDate.year(), 357, 314, 7.5f, 18);
        text(page4, latin, cjk, endDate.day(), 498, 314, 7.5f, 27);
        text(page4, latin, cjk, endDate.month(), 56, 298, 7.5f, 35);
        text(page4, latin, cjk, endDate.year(), 136, 298, 7.5f, 31);
        text(page4, latin, cjk, startDate.year(), 101, 282, 7.5f, 23);
        text(page4, latin, cjk, startDate.month(), 145, 282, 7.5f, 18);
        text(page4, latin, cjk, startDate.day(), 183, 282, 7.5f, 12);
        text(page4, latin, cjk, endDate.year(), 281, 282, 7.5f, 23);
        text(page4, latin, cjk, endDate.month(), 325, 282, 7.5f, 18);
        text(page4, latin, cjk, endDate.day(), 363, 282, 7.5f, 12);

        PdfContentByte page6 = stamper.getOverContent(6);
        text(page6, latin, cjk, value(values, "bankPayeeName"), 345, 642, 7.8f, 219);
        text(page6, latin, cjk, value(values, "bankName"), 333, 625, 7.8f, 231);
        multilineText(page6, latin, cjk, value(values, "bankAddress"), 356, 608, 206, 7.2f, 2, 9f);
        text(page6, latin, cjk, value(values, "bankBranchCode"), 350, 575, 7.8f, 214);
        text(page6, latin, cjk, value(values, "bankAccountNo"), 365, 558, 7.8f, 199);
        text(page6, latin, cjk, value(values, "bankSwiftCode"), 385, 541, 7.8f, 179);
        multilineText(page6, latin, cjk, value(values, "ownerAddress"), 226, 486, 329, 7.8f, 2, 10f);
        text(page6, latin, cjk, value(values, "ownerEmail"), 226, 443, 7.8f, 329);
        text(page6, latin, cjk, value(values, "ownerPhone"), 226, 396, 7.8f, 329);

        PdfContentByte page7 = stamper.getOverContent(7);
        text(page7, latin, cjk, owner, 51, 542, 8f, 150);
        text(page7, latin, cjk, identity, 111, 508, 8f, 100);
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

    private void validateManagementAuthorization(Map<String, String> values) {
        List<String> missing = Stream.of("projectName", "unitNo", "landlordName", "landlordIdentity",
                        "ownerEmail", "agreementDate")
                .filter(key -> value(values, key).isBlank()).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required management authorization fields: "
                    + String.join(", ", missing));
        }
        dateParts(values, "agreementDate");
    }

    private void validateTerminationLetter(Map<String, String> values) {
        List<String> missing = Stream.of("agreementDate", "landlordName", "landlordIdentity", "projectName", "unitNo",
                        "authorizedAgentName", "authorizedAgentIdentity", "authorizedAgentPhone", "authorizedAgentEmail",
                        "bankName", "bankPayeeName", "bankAccountNo", "bankSwiftCode", "bankAddress")
                .filter(key -> value(values, key).isBlank()).toList();
        if (!missing.isEmpty()) throw new IllegalArgumentException("Missing required termination letter fields: " + String.join(", ", missing));
        dateParts(values, "agreementDate");
    }

    private void validateRentalRemittance(Map<String, String> values) {
        List<String> missing = Stream.of("agreementDate", "landlordName", "landlordIdentity", "unitNo",
                        "bankPayeeName", "bankName", "bankAccountNo", "bankSwiftCode", "bankBranchCode", "bankAddress")
                .filter(key -> value(values, key).isBlank()).toList();
        if (!missing.isEmpty()) throw new IllegalArgumentException("Missing required rental remittance fields: " + String.join(", ", missing));
        dateParts(values, "agreementDate");
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

    private String displayDate(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            return LocalDate.parse(value.trim()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException ignored) {
            return value.trim();
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
        String date = displayDate(valueOr(values, "agreementDate", "startDate"));
        PdfContentByte page1 = stamper.getOverContent(1);
        text(page1, latin, cjk, date, 100, 760, 9, 105);
        text(page1, latin, cjk, value(values, "projectName"), 348, 601, 8, 58, 4.5f);
        text(page1, latin, cjk, value(values, "unitNo"), 460, 601, 8, 73, 4.5f);
        text(page1, latin, cjk, owner, 130, 531, 8, 76, 4.5f);
        text(page1, latin, cjk, identity, 297, 531, 8, 75, 4.5f);
        text(page1, latin, cjk, value(values, "ownerEmail"), 448, 531, 8, 75, 4.5f);

        PdfContentByte page3 = stamper.getOverContent(3);
        text(page3, latin, cjk, owner, 190, 332, 9, 230);
        text(page3, latin, cjk, identity, 190, 298, 9, 230);
        text(page3, latin, cjk, date, 190, 264, 9, 230);
    }

    private void fillTerminationLetter(PdfStamper stamper, BaseFont latin, BaseFont cjk, Map<String, String> values) throws IOException {
        PdfContentByte page1 = stamper.getOverContent(1);
        text(page1, latin, cjk, displayDate(value(values, "agreementDate")), 117, 624, 8, 130);
        text(page1, latin, cjk, value(values, "landlordName"), 64, 500, 8, 115);
        text(page1, latin, cjk, value(values, "projectName"), 348, 500, 8, 100);
        text(page1, latin, cjk, value(values, "unitNo"), 54, 485, 8, 98);
        text(page1, latin, cjk, value(values, "authorizedAgentName"), 106, 454, 8, 74);
        text(page1, latin, cjk, value(values, "landlordName"), 86, 376, 8, 115);
        text(page1, latin, cjk, value(values, "projectName"), 260, 376, 8, 94);
        text(page1, latin, cjk, value(values, "unitNo"), 420, 376, 8, 77);
        text(page1, latin, cjk, value(values, "authorizedAgentName"), 138, 343, 8, 92);
        // The detailed agent section is lower on page 1; keep the paragraph's
        // agent name and repeat the complete details in their dedicated blanks.
        text(page1, latin, cjk, value(values, "authorizedAgentName"), 240, 256, 8, 300);
        text(page1, latin, cjk, value(values, "authorizedAgentIdentity"), 240, 228, 8, 300);
        text(page1, latin, cjk, value(values, "authorizedAgentPhone"), 240, 200, 8, 300);
        text(page1, latin, cjk, value(values, "authorizedAgentEmail"), 240, 173, 8, 300);

        PdfContentByte page2 = stamper.getOverContent(2);
        text(page2, latin, cjk, value(values, "bankName"), 220, 637, 8, 320);
        text(page2, latin, cjk, value(values, "bankPayeeName"), 220, 609, 8, 320);
        text(page2, latin, cjk, value(values, "bankAccountNo"), 220, 581, 8, 320);
        text(page2, latin, cjk, value(values, "bankSwiftCode"), 220, 553, 8, 320);
        multilineText(page2, latin, cjk, value(values, "bankAddress"), 220, 526, 320, 8, 2, 11);
        text(page2, latin, cjk, value(values, "landlordName"), 242, 239, 8, 295);
        text(page2, latin, cjk, value(values, "landlordIdentity"), 242, 224, 8, 295);
    }

    private void text(PdfContentByte canvas, BaseFont latin, BaseFont cjk, String value, float x, float y,
            float size, float maxWidth) throws IOException {
        text(canvas, latin, cjk, value, x, y, size, maxWidth, 6.2f);
    }

    private void text(PdfContentByte canvas, BaseFont latin, BaseFont cjk, String value, float x, float y,
            float size, float maxWidth, float minimumSize) throws IOException {
        if (value == null || value.isBlank()) return;
        String safe = value.replace('\n', ' ').replace('\r', ' ').trim();
        BaseFont font = fontFor(latin, cjk, safe);
        float actual = size;
        while (actual > minimumSize && font.getWidthPoint(safe, actual) > maxWidth) actual -= .25f;
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
        return switch (type) {
            case PROPERTY_MANAGEMENT_AGREEMENT -> 9;
            case MANAGEMENT_AUTHORIZATION -> 3;
            case TERMINATION_LETTER -> 2;
            case RENTAL_REMITTANCE -> 1;
        };
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
    public record PageSize(float width, float height) { }
    public record TemplateFieldPosition(String id, String fieldKey, String label, int page, float x, float y,
            float fontSize, float maxWidth, int maxLines, float lineHeight) { }
    public record TemplateLayout(String version, int pages, List<PageSize> pageSizes,
            List<TemplateFieldPosition> fields) { }
    private record DateParts(String day, String month, String year) { }
}
