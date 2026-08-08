package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.lowagie.text.pdf.PdfReader;

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
        assertThat(page1Content).contains("1 0 0 1 201 634 Tm").contains("1 0 0 1 456 634 Tm")
                .doesNotContain("1 0 0 1 270 629 Tm").doesNotContain("1 0 0 1 350 614 Tm");
        assertThat(page4Content).contains("1 0 0 1 217 274 Tm").contains("1 0 0 1 151 258 Tm")
                .doesNotContain("1 0 0 1 285 268 Tm");
        reader.close();
    }

    @Test
    void generatesTheThreePageOwnerAuthorization() throws Exception {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());

        byte[] pdf = service.generate(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION,
                Map.of("caseNo", "RM-100", "landlordName", "张业主", "landlordIdentity", "A123456",
                        "ownerEmail", "owner@example.com", "unitNo", "102", "agreementDate", "2026-08-08"));

        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isEqualTo(3);
        assertThat(pdf.length).isGreaterThan(100_000);
        reader.close();
    }

    @Test
    void namesGeneratedFilesByOwnerPropertyDocumentTypeAndReference() {
        RentalManagementTemplatePdfService service = new RentalManagementTemplatePdfService(tempDir.toString());

        String fileName = service.fileName(
                RentalManagementTemplatePdfService.TemplateType.PROPERTY_MANAGEMENT_AGREEMENT,
                Map.of("landlordName", "张三", "projectName", "翻斗花园", "unitNo", "102", "caseNo", "RM-001"));

        assertThat(fileName).isEqualTo("张三-翻斗花园-102-代租管合约-RM-001.pdf");
    }
}
