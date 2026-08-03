package com.ccps.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

class TenancyAgreementWordServiceTest {
    @Test
    void fillsTheOriginalFirstScheduleAndKeepsAllTemplateTables() throws Exception {
        TenancyAgreementWordService service = new TenancyAgreementWordService();
        byte[] result = service.generate(Map.ofEntries(
                Map.entry("caseNo", "LS-2026-001"),
                Map.entry("agreementDate", "2026-07-27"),
                Map.entry("landlordName", "Jane Owner"),
                Map.entry("landlordIdentity", "900101-01-1234"),
                Map.entry("landlordAddress", "Owner address"),
                Map.entry("tenantName", "John Tenant"),
                Map.entry("tenantIdentity", "P1234567"),
                Map.entry("tenantPhone", "+60123456789"),
                Map.entry("tenantAddress", "Tenant address"),
                Map.entry("tenantEmail", "john@example.com"),
                Map.entry("propertyAddress", "Unit 12-08, Example Residence"),
                Map.entry("termYears", "One year"),
                Map.entry("leaseStart", "2026-08-01"),
                Map.entry("leaseEnd", "2027-07-31"),
                Map.entry("monthlyRent", "RM 3,500.00"),
                Map.entry("paymentDay", "1"),
                Map.entry("advanceRental", "RM 3,500.00"),
                Map.entry("securityDeposit", "RM 7,000.00"),
                Map.entry("utilityDeposit", "RM 1,750.00"),
                Map.entry("use", "For Residential use only"),
                Map.entry("specialConditions", "No pets without written approval.")));

        String documentXml = entry(result, "word/document.xml");
        assertTrue(documentXml.contains("Jane Owner"));
        assertTrue(documentXml.contains("John Tenant"));
        assertTrue(documentXml.contains("Unit 12-08, Example Residence"));
        assertTrue(documentXml.contains("1st August 2026"));
        assertTrue(documentXml.contains("31st July 2027"));
        assertTrue(documentXml.contains("RM 3,500.00"));
        assertTrue(documentXml.contains("No pets without written approval."));
        assertFalse(documentXml.contains("(Landlord Name)"));
        assertFalse(documentXml.contains("(Tenant Name)"));
        assertEquals(21, count(documentXml, "<w:tbl ") + count(documentXml, "<w:tbl>"));
    }

    @Test
    void doesNotModifyTheTemplateResourceAndUsesAnInformativeFileName() throws Exception {
        TenancyAgreementWordService service = new TenancyAgreementWordService();
        byte[] before;
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(TenancyAgreementWordService.TEMPLATE_RESOURCE)) {
            before = stream.readAllBytes();
        }
        service.generate(Map.of("caseNo", "LS/2026/001", "unitNo", "F-16-06"));
        byte[] after;
        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream(TenancyAgreementWordService.TEMPLATE_RESOURCE)) {
            after = stream.readAllBytes();
        }
        assertEquals(hash(before), hash(after));
        assertEquals("Tenancy-Agreement-F-16-06.docx", service.fileName(Map.of("caseNo", "LS/2026/001", "unitNo", "F-16-06")));
    }

    private String entry(byte[] docx, String name) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docx))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (name.equals(entry.getName())) return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("Missing DOCX entry: " + name);
    }

    private int count(String text, String token) {
        int result = 0;
        for (int index = 0; (index = text.indexOf(token, index)) >= 0; index += token.length()) result++;
        return result;
    }

    private String hash(byte[] value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
        StringBuilder result = new StringBuilder();
        for (byte item : digest) result.append(String.format("%02x", item));
        return result.toString();
    }
}
