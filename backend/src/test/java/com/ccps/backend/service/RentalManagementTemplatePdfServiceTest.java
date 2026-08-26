package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

class RentalManagementTemplatePdfServiceTest {
    @TempDir Path tempDir;

    @Test
    void rejectsPmaGenerationWhenRequiredOwnerAgreementOrBankDetailsAreMissing() {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());

        assertThatThrownBy(() -> service.generate(
                RentalManagementTemplatePdfService.TemplateType.PROPERTY_MANAGEMENT_AGREEMENT,
                Map.of("landlordName", "张业主", "propertyAddress", "翻斗花园 102")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("landlordIdentity")
                .hasMessageContaining("bankAccountNo");
    }

    @Test
    void generatesTheNinePagePmaWithOwnerAndPropertyDetails() throws Exception {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());

        byte[] pdf = service.generate(
                RentalManagementTemplatePdfService.TemplateType.PROPERTY_MANAGEMENT_AGREEMENT,
                Map.ofEntries(Map.entry("caseNo", "RM-100"), Map.entry("landlordName", "张业主"),
                        Map.entry("landlordIdentity", "A123456"), Map.entry("propertyAddress", "翻斗花园 102"),
                        Map.entry("agreementDate", "2026-08-08"), Map.entry("startDate", "2026-08-08"),
                        Map.entry("endDate", "2027-08-07"), Map.entry("bankPayeeName", "张业主"),
                        Map.entry("bankName", "RHB Bank"), Map.entry("bankAddress", "Kuala Lumpur"),
                        Map.entry("bankBranchCode", "068"), Map.entry("bankAccountNo", "1234567890"),
                        Map.entry("bankSwiftCode", "RHBBMYKL"), Map.entry("ownerAddress", "Kuala Lumpur"),
                        Map.entry("ownerEmail", "owner@example.com"), Map.entry("ownerPhone", "+60123456789")));

        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(9);
        assertThat(pdf.length).isGreaterThan(500_000);
        String page1Content = new String(reader.getPageContent(1), StandardCharsets.ISO_8859_1);
        String page4Content = new String(reader.getPageContent(4), StandardCharsets.ISO_8859_1);
        assertThat(page1Content).contains("1 0 0 1 194 634 Tm").contains("1 0 0 1 404 634 Tm")
                .contains("1 0 0 1 268 593 Tm")
                .doesNotContain("1 0 0 1 270 629 Tm").doesNotContain("1 0 0 1 350 614 Tm");
        assertThat(page4Content).contains("1 0 0 1 220 314 Tm").contains("1 0 0 1 145 282 Tm")
                .doesNotContain("1 0 0 1 285 268 Tm");
        String qaOutput = System.getProperty("pma.layout.qa.output", "").trim();
        if (!qaOutput.isEmpty()) Files.write(Path.of(qaOutput), pdf);
        reader.close();
    }

    @Test
    void generatesTheThreePageOwnerAuthorization() throws Exception {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());

        byte[] pdf = service.generate(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION,
                Map.ofEntries(Map.entry("caseNo", "RM-100"), Map.entry("landlordName", "张业主"),
                        Map.entry("landlordIdentity", "A123456"), Map.entry("ownerEmail", "owner@example.com"),
                        Map.entry("projectName", "翻斗花园"), Map.entry("unitNo", "102"),
                        Map.entry("agreementDate", "2026-08-08")));

        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(3);
        assertThat(pdf.length).isGreaterThan(100_000);
        String page1Content = new String(reader.getPageContent(1), StandardCharsets.ISO_8859_1);
        assertThat(page1Content)
                .contains("1 0 0 1 100 760 Tm")
                .contains("1 0 0 1 348 601 Tm")
                .contains("1 0 0 1 460 601 Tm")
                .contains("1 0 0 1 130 531 Tm")
                .contains("1 0 0 1 297 531 Tm")
                .contains("1 0 0 1 448 531 Tm")
                .doesNotContain("1 0 0 1 72 690 Tm");
        String page3Content = new String(reader.getPageContent(3), StandardCharsets.ISO_8859_1);
        assertThat(page3Content)
                .contains("1 0 0 1 190 332 Tm")
                .contains("1 0 0 1 190 298 Tm")
                .contains("1 0 0 1 190 264 Tm");
        String qaOutput = System.getProperty("authorization.layout.qa.output", "").trim();
        if (!qaOutput.isEmpty()) Files.write(Path.of(qaOutput), pdf);
        reader.close();
    }

    @Test
    void generatesTheTwoPageOwnerTerminationLetterWithAllOwnerAndBankDetails() throws Exception {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());

        byte[] pdf = service.generate(
                RentalManagementTemplatePdfService.TemplateType.TERMINATION_LETTER,
                Map.ofEntries(Map.entry("agreementDate", "2026-08-19"), Map.entry("landlordName", "张业主"),
                Map.entry("landlordIdentity", "P1234567"), Map.entry("ownerEmail", "owner@example.com"),
                        Map.entry("projectName", "海天公寓"), Map.entry("unitNo", "12楼1号"),
                        Map.entry("authorizedAgentName", "代理人甲"), Map.entry("authorizedAgentIdentity", "A123456"),
                        Map.entry("authorizedAgentPhone", "+60123456789"), Map.entry("authorizedAgentEmail", "agent@example.com"),
                        Map.entry("bankName", "Maybank"), Map.entry("bankPayeeName", "张业主"),
                        Map.entry("bankAccountNo", "123456789"), Map.entry("bankSwiftCode", "MBBEMYKL"),
                        Map.entry("bankAddress", "Kuala Lumpur")));

        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(2);
        String page1Content = new String(reader.getPageContent(1), StandardCharsets.ISO_8859_1);
        String page2Content = new String(reader.getPageContent(2), StandardCharsets.ISO_8859_1);
        assertThat(page1Content)
                .contains("1 0 0 1 117 624 Tm")
                .contains("1 0 0 1 64 500 Tm")
                .contains("1 0 0 1 348 500 Tm")
                .contains("1 0 0 1 106 454 Tm")
                .contains("1 0 0 1 86 376 Tm")
                .contains("1 0 0 1 260 376 Tm")
                .contains("1 0 0 1 420 376 Tm")
                .contains("1 0 0 1 138 343 Tm")
                .contains("1 0 0 1 240 256 Tm")
                .contains("1 0 0 1 240 228 Tm")
                .contains("1 0 0 1 240 200 Tm")
                .contains("1 0 0 1 240 173 Tm");
        assertThat(page2Content)
                .contains("1 0 0 1 220 637 Tm")
                .contains("1 0 0 1 220 581 Tm")
                .contains("1 0 0 1 242 239 Tm")
                .contains("1 0 0 1 242 224 Tm");
        String qaOutput = System.getProperty("termination.layout.qa.output", "").trim();
        if (!qaOutput.isEmpty()) Files.write(Path.of(qaOutput), pdf);
        reader.close();
    }

    @Test
    void generatesTheOnePageOwnerRentalRemittanceWithCompleteBankDetails() throws Exception {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());

        byte[] pdf = service.generate(
                RentalManagementTemplatePdfService.TemplateType.RENTAL_REMITTANCE,
                Map.ofEntries(Map.entry("agreementDate", "2026-08-20"), Map.entry("landlordName", "张业主"),
                        Map.entry("landlordIdentity", "P1234567"), Map.entry("unitNo", "12楼1号"),
                        Map.entry("bankPayeeName", "张业主"), Map.entry("bankName", "Maybank"),
                        Map.entry("bankAccountNo", "123456789"), Map.entry("bankSwiftCode", "MBBEMYKL"),
                        Map.entry("bankBranchCode", "001"), Map.entry("bankAddress", "Kuala Lumpur")));

        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(1);
        String content = new String(reader.getPageContent(1), StandardCharsets.ISO_8859_1);
        assertThat(content).contains("1 0 0 1 128 758 Tm")
                .contains("1 0 0 1 250 537 Tm")
                .contains("1 0 0 1 216 507 Tm")
                .contains("1 0 0 1 194 256 Tm");
        String qaOutput = System.getProperty("rental.remittance.layout.qa.output", "").trim();
        if (!qaOutput.isEmpty()) Files.write(Path.of(qaOutput), pdf);
        reader.close();
    }

    @Test
    void rejectsRentalRemittanceWithoutAnyRequiredBankField() {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());

        assertThatThrownBy(() -> service.generate(RentalManagementTemplatePdfService.TemplateType.RENTAL_REMITTANCE,
                Map.of("agreementDate", "2026-08-20", "landlordName", "张业主", "landlordIdentity", "P1234567", "unitNo", "102")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bankAccountNo")
                .hasMessageContaining("bankBranchCode");
    }

    @Test
    void exposesOnlyTheSixLatestAuthorizationInputs() {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());

        var layout = service.currentLayout(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION);

        assertThat(layout.fields().stream().filter(field -> !RentalManagementTemplatePdfService.isSignatureField(field))
                .map(RentalManagementTemplatePdfService.TemplateFieldPosition::fieldKey)
                .collect(java.util.stream.Collectors.toSet()))
                .containsExactlyInAnyOrder("projectName", "unitNo", "landlordName", "landlordIdentity",
                        "ownerEmail", "agreementDate");
        assertThat(layout.fields()).noneMatch(field -> "managementOffice".equals(field.fieldKey())
                || "unitNoOrAddress".equals(field.fieldKey()));
    }

    @Test
    void exposesSignaturePositionsForEveryGeneratedAttachmentTemplate() {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());

        assertThat(service.currentLayout(RentalManagementTemplatePdfService.TemplateType.PROPERTY_MANAGEMENT_AGREEMENT)
                .fields()).anyMatch(field -> "signature.owner".equals(field.fieldKey()));
        assertThat(service.currentLayout(RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION)
                .fields()).anyMatch(field -> "signature.owner".equals(field.fieldKey()));
        assertThat(service.currentLayout(RentalManagementTemplatePdfService.TemplateType.TERMINATION_LETTER)
                .fields()).anyMatch(field -> "signature.owner".equals(field.fieldKey()));
        assertThat(service.currentLayout(RentalManagementTemplatePdfService.TemplateType.RENTAL_REMITTANCE)
                .fields()).anyMatch(field -> "signature.owner".equals(field.fieldKey()));
    }

    @Test
    void savesAndReloadsSignaturePositionWithoutTreatingItAsTextInput() {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());
        var initial = service.currentLayout(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION);
        var signature = initial.fields().stream().filter(field -> "signature.owner".equals(field.fieldKey()))
                .findFirst().orElseThrow();
        var moved = initial.fields().stream()
                .map(field -> field.id().equals(signature.id())
                        ? new RentalManagementTemplatePdfService.TemplateFieldPosition(field.id(), field.fieldKey(),
                                field.label(), field.page(), 222f, 444f, field.fontSize(), field.maxWidth(),
                                field.maxLines(), field.lineHeight())
                        : field)
                .toList();

        service.saveLayout(RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION,
                new RentalManagementTemplatePdfService.TemplateLayout(initial.version(), initial.pages(),
                        initial.pageSizes(), moved));

        var saved = service.currentLayout(RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION);
        var reloaded = saved.fields().stream().filter(field -> field.id().equals(signature.id())).findFirst().orElseThrow();
        assertThat(reloaded.x()).isEqualTo(222f);
        assertThat(reloaded.y()).isEqualTo(444f);
    }

    @Test
    void placesProjectAndUnitAtTheAnnotatedLatestTemplateCoordinates() {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());

        var fields = service.currentLayout(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION).fields();
        var project = fields.stream().filter(field -> "project-page-1".equals(field.id())).findFirst().orElseThrow();
        var unit = fields.stream().filter(field -> "unit-page-1".equals(field.id())).findFirst().orElseThrow();

        assertThat(project.x()).isEqualTo(348f);
        assertThat(project.y()).isEqualTo(601f);
        assertThat(project.maxWidth()).isEqualTo(58f);
        assertThat(unit.x()).isEqualTo(460f);
        assertThat(unit.y()).isEqualTo(601f);
        assertThat(unit.maxWidth()).isEqualTo(73f);
    }

    @Test
    void migratesSavedLegacyAuthorizationLayoutToProjectAndUnitFields() {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());
        var initial = service.currentLayout(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION);
        var legacy = List.of(
                new RentalManagementTemplatePdfService.TemplateFieldPosition("management-office-page-1",
                        "managementOffice", "收件单位（To）", 1, 88.5f, 699.5f, 8f, 208f, 4, 11f),
                new RentalManagementTemplatePdfService.TemplateFieldPosition("unit-page-1", "unitNoOrAddress",
                        "房产单位", 1, 337f, 599.5f, 9f, 80f, 1, 11f),
                new RentalManagementTemplatePdfService.TemplateFieldPosition("owner-page-1", "landlordName",
                        "业主姓名", 1, 150.5f, 534f, 8f, 33f, 1, 10f));
        service.saveLayout(RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION,
                new RentalManagementTemplatePdfService.TemplateLayout(initial.version(), initial.pages(),
                        initial.pageSizes(), legacy));

        var migrated = service.currentLayout(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION);

        assertThat(migrated.fields()).anyMatch(field -> "project-page-1".equals(field.id())
                && "projectName".equals(field.fieldKey()) && field.x() == 348f);
        assertThat(migrated.fields()).anyMatch(field -> "unit-page-1".equals(field.id())
                && "unitNo".equals(field.fieldKey()) && field.x() == 460f);
        assertThat(migrated.fields()).noneMatch(field -> "managementOffice".equals(field.fieldKey())
                || "unitNoOrAddress".equals(field.fieldKey()));
        assertThat(migrated.fields()).anyMatch(field -> "owner-page-1".equals(field.id())
                && field.x() == 150.5f && field.y() == 534f);
        assertThat(migrated.fields()).anyMatch(field -> "signature.owner".equals(field.fieldKey()));
    }

    @Test
    void namesGeneratedFilesByOwnerPropertyDocumentTypeAndReference() {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());

        String fileName = service.fileName(
                RentalManagementTemplatePdfService.TemplateType.PROPERTY_MANAGEMENT_AGREEMENT,
                Map.of("landlordName", "张三", "projectName", "翻斗花园", "unitNo", "102", "caseNo", "RM-001"));

        assertThat(fileName).isEqualTo("张三-翻斗花园-102-代租管合约-RM-001.pdf");
    }

    @Test
    void savesFieldPositionsForTheActiveTemplateVersion() {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());
        var initial = service.currentLayout(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION);
        var moved = initial.fields().stream()
                .map(field -> "owner-page-1".equals(field.id())
                        ? new RentalManagementTemplatePdfService.TemplateFieldPosition(field.id(), field.fieldKey(),
                                field.label(), field.page(), 222f, 444f, 10f, field.maxWidth(), field.maxLines(),
                                field.lineHeight())
                        : field)
                .toList();

        service.saveLayout(RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION,
                new RentalManagementTemplatePdfService.TemplateLayout(initial.version(), initial.pages(),
                        initial.pageSizes(), moved));

        var saved = service.currentLayout(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION);
        var owner = saved.fields().stream().filter(field -> "owner-page-1".equals(field.id())).findFirst().orElseThrow();
        assertThat(owner.x()).isEqualTo(222f);
        assertThat(owner.y()).isEqualTo(444f);
    }

    @Test
    void overwritesPreviouslySavedFieldPositionsForTheSameTemplateVersion() {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());
        var initial = service.currentLayout(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION);
        service.saveLayout(RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION, initial);

        var moved = initial.fields().stream()
                .map(field -> "owner-page-1".equals(field.id())
                        ? new RentalManagementTemplatePdfService.TemplateFieldPosition(field.id(), field.fieldKey(),
                                field.label(), field.page(), 230f, 450f, field.fontSize(), field.maxWidth(),
                                field.maxLines(), field.lineHeight())
                        : field)
                .toList();

        service.saveLayout(RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION,
                new RentalManagementTemplatePdfService.TemplateLayout(initial.version(), initial.pages(),
                        initial.pageSizes(), moved));

        var saved = service.currentLayout(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION);
        var owner = saved.fields().stream().filter(field -> "owner-page-1".equals(field.id())).findFirst().orElseThrow();
        assertThat(owner.x()).isEqualTo(230f);
        assertThat(owner.y()).isEqualTo(450f);
    }

    @Test
    void normalizesIncompleteNumericEditorValuesBeforeSaving() {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());
        var initial = service.currentLayout(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION);
        var edited = new RentalManagementTemplatePdfService.TemplateFieldPosition("owner-page-1", "landlordName",
                "业主姓名", 0, -12f, -8f, 0f, 0f, 0, 0f);

        var saved = service.saveLayout(RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION,
                new RentalManagementTemplatePdfService.TemplateLayout(initial.version(), initial.pages(),
                        initial.pageSizes(), List.of(edited)));

        var field = saved.fields().get(0);
        assertThat(field.page()).isEqualTo(1);
        assertThat(field.x()).isZero();
        assertThat(field.y()).isZero();
        assertThat(field.fontSize()).isEqualTo(9f);
        assertThat(field.maxWidth()).isEqualTo(120f);
        assertThat(field.maxLines()).isEqualTo(1);
        assertThat(field.lineHeight()).isEqualTo(11f);
    }

    @Test
    void usesSavedFieldPositionsWhenGeneratingANewFile() throws Exception {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());
        var initial = service.currentLayout(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION);
        var onlyOwner = new RentalManagementTemplatePdfService.TemplateFieldPosition("owner-page-1", "landlordName",
                "业主姓名", 2, 222f, 444f, 10f, 160f, 1, 11f);
        service.saveLayout(RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION,
                new RentalManagementTemplatePdfService.TemplateLayout(initial.version(), initial.pages(),
                        initial.pageSizes(), List.of(onlyOwner)));

        byte[] pdf = service.generate(RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION,
                Map.of("landlordName", "Owner", "agreementDate", "2026-08-08"));

        PdfReader reader = new PdfReader(pdf);
        String page1Content = new String(reader.getPageContent(1), StandardCharsets.ISO_8859_1);
        String page2Content = new String(reader.getPageContent(2), StandardCharsets.ISO_8859_1);
        assertThat(page1Content).doesNotContain("1 0 0 1 222 444 Tm");
        assertThat(page2Content).contains("1 0 0 1 222 444 Tm");
        reader.close();
    }

    @Test
    void acceptsANewTemplatePageCountAndKeepsDefaultFieldsOnValidPages() throws Exception {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, output);
        document.open();
        document.add(new Paragraph("Page 1"));
        document.newPage();
        document.add(new Paragraph("Page 2"));
        document.close();

        var version = service.replace(RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION,
                new MockMultipartFile("file", "updated.pdf", "application/pdf", output.toByteArray()));
        var layout = service.currentLayout(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION);

        assertThat(version.pages()).isEqualTo(2);
        assertThat(layout.pages()).isEqualTo(2);
        assertThat(layout.fields()).allMatch(field -> field.page() >= 1 && field.page() <= 2);
        assertThat(layout.fields()).anyMatch(field -> "owner-page-3".equals(field.id()) && field.page() == 2);
    }
}
