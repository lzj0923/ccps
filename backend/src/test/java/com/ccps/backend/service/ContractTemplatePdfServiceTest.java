package com.ccps.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;

class ContractTemplatePdfServiceTest {
    @Test
    void rejectsIncompleteRentalAppointmentBeforePdfGeneration() {
        ContractTemplatePdfService service = new ContractTemplatePdfService();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.validateRequiredFields(ContractTemplatePdfService.TemplateType.AUTHORIZATION,
                        Map.of("caseNo", "RM-1", "propertyAddress", "Unit 1")));

        assertTrue(error.getMessage().contains("earnestDeposit"));
        assertTrue(error.getMessage().contains("witnessName"));
    }

    @Test
    void acceptsCompleteRentalAppointmentDetailsWithoutASecondLandlord() {
        ContractTemplatePdfService service = new ContractTemplatePdfService();

        service.validateRequiredFields(ContractTemplatePdfService.TemplateType.AUTHORIZATION, Map.ofEntries(
                Map.entry("caseNo", "RM-1"), Map.entry("propertyAddress", "Unit 1"),
                Map.entry("earnestDeposit", "1000"), Map.entry("earnestDepositWords", "One Thousand"),
                Map.entry("commission", "10"), Map.entry("commissionMonths", "1"),
                Map.entry("sstPercent", "8"), Map.entry("commissionAmount", "100"),
                Map.entry("agencyFeeTotal", "108"), Map.entry("startDate", "2026-08-01"),
                Map.entry("commencementDate", "2027-07-31"), Map.entry("landlordName", "Owner"),
                Map.entry("landlordIdentity", "ID1"), Map.entry("landlordAddress", "Address"),
                Map.entry("landlordDate", "2026-08-01"), Map.entry("witnessName", "Witness"),
                Map.entry("witnessIdentity", "ID2"), Map.entry("witnessAddress", "Address 2"),
                Map.entry("witnessDate", "2026-08-01"), Map.entry("hasSecondLandlord", "false")));
    }

    @Test
    void rejectsIncompleteOfferToRentBeforePdfGeneration() {
        ContractTemplatePdfService service = new ContractTemplatePdfService();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.validateRequiredFields(ContractTemplatePdfService.TemplateType.OTR,
                        Map.of("caseNo", "LEASE-1", "propertyAddress", "Unit 1")));

        assertTrue(error.getMessage().contains("advanceRental"));
        assertTrue(error.getMessage().contains("tenantWitnessName"));
        assertTrue(error.getMessage().contains("landlordWitnessName"));
    }

    @Test
    void mapsOfferToRentRenewalAndBothWitnessDetails() {
        ContractTemplatePdfService service = new ContractTemplatePdfService();

        ContractTemplatePdfService.TemplateData data = service.data(Map.ofEntries(
                Map.entry("renewalYears", "1"),
                Map.entry("tenantWitnessName", "Tenant Witness"),
                Map.entry("tenantWitnessIdentity", "TW-1"),
                Map.entry("tenantWitnessDate", "2026-08-18"),
                Map.entry("landlordWitnessName", "Landlord Witness"),
                Map.entry("landlordWitnessIdentity", "LW-1"),
                Map.entry("landlordWitnessDate", "2026-08-18")));

        assertEquals("1", data.renewalYears());
        assertEquals("Tenant Witness", data.tenantWitnessName());
        assertEquals("TW-1", data.tenantWitnessIdentity());
        assertEquals("2026-08-18", data.tenantWitnessDate());
        assertEquals("Landlord Witness", data.landlordWitnessName());
        assertEquals("LW-1", data.landlordWitnessIdentity());
        assertEquals("2026-08-18", data.landlordWitnessDate());
    }
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
                service.data(Map.ofEntries(
                        Map.entry("caseNo", "CASE-001"), Map.entry("propertyAddress", "Unit 12-08, Example Residence"),
                        Map.entry("landlordName", "Jane Owner"), Map.entry("tenantName", "John Tenant"),
                        Map.entry("tenancyYears", "3"), Map.entry("commencementDate", "2026-08-01"),
                        Map.entry("securityDepositMonths", "1"), Map.entry("advanceRental", "RM 3,500.00"),
                        Map.entry("utilityDepositMonths", "2"), Map.entry("securityDeposit", "RM 7,000.00"),
                        Map.entry("utilityDeposit", "RM 3,500.00"), Map.entry("stampingFee", "RM 100.00"),
                        Map.entry("totalBeforeKeys", "RM 14,000.00"), Map.entry("periodYears", "1"),
                        Map.entry("startDate", "2026-08-01"), Map.entry("earnestDeposit", "RM 3,500.00"),
                        Map.entry("landlordIdentity", "900101-01-1234"), Map.entry("landlordDate", "2026-07-27"),
                        Map.entry("tenantIdentity", "P1234567"), Map.entry("tenantDate", "2026-07-27"),
                        Map.entry("commission", "10"), Map.entry("otherConditions", "Additional condition"))));

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
            Files.write(Path.of("../project-resources/generated/tmp/pdfs/generated-otr.pdf"), pdf);
            Files.write(Path.of("../project-resources/generated/tmp/pdfs/generated-authorization.pdf"),
                    service.generate(ContractTemplatePdfService.TemplateType.AUTHORIZATION,
                            service.data(Map.ofEntries(
                                    Map.entry("caseNo", "CASE-001"), Map.entry("propertyAddress", "Unit 12-08, Example Residence"),
                                    Map.entry("earnestDeposit", "3500.00"), Map.entry("earnestDepositWords", "Three Thousand Five Hundred"),
                                    Map.entry("commission", "10"), Map.entry("commissionMonths", "1"),
                                    Map.entry("sstPercent", "8"), Map.entry("commissionAmount", "3500.00"),
                                    Map.entry("agencyFeeTotal", "3780.00"), Map.entry("startDate", "2026-08-01"),
                                    Map.entry("commencementDate", "2027-07-31"), Map.entry("landlordName", "Jane Owner"),
                                    Map.entry("landlordIdentity", "900101-01-1234"), Map.entry("landlordAddress", "Owner address"),
                                    Map.entry("landlordDate", "2026-07-27"), Map.entry("secondLandlordName", "John Co-owner"),
                                    Map.entry("secondLandlordIdentity", "P7654321"), Map.entry("secondLandlordAddress", "Co-owner address"),
                                    Map.entry("secondLandlordDate", "2026-07-27"), Map.entry("witnessName", "Mary Witness"),
                                    Map.entry("witnessIdentity", "W123456"), Map.entry("witnessAddress", "Witness address"),
                                    Map.entry("witnessDate", "2026-07-27")))));
        }
    }

    @Test
    void generatesAuthorizationForChineseOwnerAndProperty() throws Exception {
        ContractTemplatePdfService service = new ContractTemplatePdfService();

        byte[] pdf = service.generate(ContractTemplatePdfService.TemplateType.AUTHORIZATION,
                service.data(Map.of(
                        "caseNo", "RM-20260807-46375C",
                        "propertyAddress", "翻斗花园 · 102",
                        "landlordName", "吕志杰",
                        "startDate", "2026-08-07",
                        "commencementDate", "2027-08-06",
                        "commission", "3%")));

        assertTrue(pdf.length > 100_000);
        PdfReader reader = new PdfReader(pdf);
        assertEquals(1, reader.getNumberOfPages());
        reader.close();
    }

    @Test
    void keepsRentalAppointmentPartyDetailsAlignedInsideTheirOwnColumns() throws Exception {
        ContractTemplatePdfService service = new ContractTemplatePdfService();

        byte[] pdf = service.generate(ContractTemplatePdfService.TemplateType.AUTHORIZATION,
                service.data(Map.ofEntries(
                        Map.entry("landlordName", "First Owner"), Map.entry("landlordIdentity", "OWNER-1"),
                        Map.entry("landlordAddress", "First owner address"), Map.entry("landlordDate", "2026-08-18"),
                        Map.entry("secondLandlordName", "Second Owner"), Map.entry("secondLandlordIdentity", "OWNER-2"),
                        Map.entry("secondLandlordAddress", "Second owner address"), Map.entry("secondLandlordDate", "2026-08-18"),
                        Map.entry("witnessName", "Witness Name"), Map.entry("witnessIdentity", "WITNESS-1"),
                        Map.entry("witnessAddress", "Witness address"), Map.entry("witnessDate", "2026-08-18"))));

        try (PdfReader reader = new PdfReader(pdf)) {
            String content = new String(reader.getPageContent(1), java.nio.charset.StandardCharsets.ISO_8859_1);
            assertTrue(content.contains("118 243 Tm"));
            assertTrue(content.contains("265 243 Tm"));
            assertTrue(content.contains("418 243 Tm"));
        }
    }
}
