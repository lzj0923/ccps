package com.ccps.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;

class ContractTemplatePdfServiceTest {
    @Test
    void mapsTheAdminOwnerAndTenantNamesIntoTheOfferBodyFields() {
        ContractTemplatePdfService service = new ContractTemplatePdfService();

        ContractTemplatePdfService.TemplateData data = service.data(Map.of(
                "landlordName", "Jane Owner",
                "tenantName", "John Tenant"));

        assertEquals("Jane Owner", data.landlord());
        assertEquals("John Tenant", data.tenant());
    }

    @Test
    void fillsTheOriginalOfferToRentTemplateWithoutChangingItsPageCount() throws Exception {
        ContractTemplatePdfService service = new ContractTemplatePdfService();
        byte[] pdf = service.generate(ContractTemplatePdfService.TemplateType.OTR,
                new ContractTemplatePdfService.TemplateData(
                        "CASE-001", "Unit 12-08, Example Residence", "Jane Owner", "John Tenant",
                        "3", "2026-08-01", "1", "RM 3,500.00", "2", "RM 7,000.00",
                        "1", "RM 3,500.00", "RM 14,000.00", "1", "2026-08-01", "RM 3,500.00",
                        "Jane Owner", "900101-01-1234", "2026-07-27", "John Tenant", "P1234567", "2026-07-27",
                        "RM 3,500.00", "Additional condition", List.of()));

        Path file = Files.createTempFile("otr-generated-", ".pdf");
        Files.write(file, pdf);
        PdfReader reader = new PdfReader(pdf);
        String text = String.join("\n", reader.getPageN(1).toString());
        assertEquals(1, reader.getNumberOfPages());
        assertTrue(pdf.length > 100_000);
        assertTrue(Files.size(file) > 100_000);
        reader.close();
        Files.deleteIfExists(file);
        if (System.getProperty("renderSamples") != null) {
            Files.write(Path.of("../tmp/pdfs/generated-otr.pdf"), pdf);
            Files.write(Path.of("../tmp/pdfs/generated-authorization.pdf"),
                    service.generate(ContractTemplatePdfService.TemplateType.AUTHORIZATION,
                            new ContractTemplatePdfService.TemplateData(
                                    "CASE-001", "Unit 12-08, Example Residence", "Jane Owner", "John Tenant",
                                    "3", "2026-08-01", "2", "RM 3,500.00", "1", "RM 7,000.00",
                                    "RM 3,500.00", "RM 100.00", "RM 14,100.00", "1", "2026-08-01", "RM 3,500.00",
                                    "Jane Owner", "900101-01-1234", "2026-07-27", "John Tenant", "P1234567", "2026-07-27",
                                    "RM 3,500.00", "Additional condition", List.of())));
        }
    }
}
