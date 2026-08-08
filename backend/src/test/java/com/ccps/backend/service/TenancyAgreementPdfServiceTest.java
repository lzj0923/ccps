package com.ccps.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

class TenancyAgreementPdfServiceTest {
    @Test
    void generatesTheOriginalTwentyTwoPageAgreementWithNewLeaseValues() throws Exception {
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        byte[] result = service.generate(Map.ofEntries(
                Map.entry("caseNo", "LS-TEST-001"),
                Map.entry("agreementDate", "2026-07-28"),
                Map.entry("landlordName", "NEW LANDLORD"),
                Map.entry("landlordIdentity", "OWNER-123"),
                Map.entry("landlordAddress", "New owner address"),
                Map.entry("tenantName", "NEW TENANT"),
                Map.entry("tenantIdentity", "TENANT-456"),
                Map.entry("tenantPhone", "+60111111111"),
                Map.entry("tenantAddress", "New tenant address"),
                Map.entry("tenantEmail", "new@example.com"),
                Map.entry("propertyAddress", "New property address"),
                Map.entry("leaseStart", "2026-08-01"),
                Map.entry("leaseEnd", "2027-07-31"),
                Map.entry("termYears", "One (1) year"),
                Map.entry("monthlyRent", "RM 4,500.00"),
                Map.entry("paymentDay", "5"),
                Map.entry("advanceRental", "RM 4,500.00"),
                Map.entry("securityDeposit", "RM 9,000.00"),
                Map.entry("utilityDeposit", "RM 2,250.00"),
                Map.entry("use", "For Residential use only"),
                Map.entry("specialConditions", "No pets.")));

        PdfReader reader = new PdfReader(new ByteArrayInputStream(result));
        assertEquals(22, reader.getNumberOfPages());
        String text = "";
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text += extractor.getTextFromPage(page, true);
        }
        assertTrue(text.contains("NEW LANDLORD"));
        assertTrue(text.contains("NEW TENANT"));
        assertTrue(text.contains("New property address"));
        assertTrue(text.contains("NEW LANDLORD"));
        assertTrue(text.contains("NEW TENANT"));
        reader.close();
    }

    @Test
    void usesAnInformativePdfFileName() {
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        assertEquals("李四-翻斗花园-F-40-12-租赁合同-LS-2026-001.pdf",
                service.fileName(Map.of("landlordName", "张三", "tenantName", "李四", "projectName", "翻斗花园",
                        "unitNo", "F-40-12", "caseNo", "LS-2026-001")));
    }

    @Test
    void replacesTheThirteenTemplatePhotosWithPropertyPhotos() throws Exception {
        Path first = temporaryPhoto(Color.RED);
        Path second = temporaryPhoto(Color.BLUE);
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();

        byte[] result = service.generate(Map.of("leaseId", "42"), List.of(
                new TenancyAgreementPdfService.PropertyPhotoAsset(first, "image/jpeg", 0, true),
                new TenancyAgreementPdfService.PropertyPhotoAsset(second, "image/jpeg", 1, false)));

        PdfReader reader = new PdfReader(new ByteArrayInputStream(result));
        assertEquals(22, reader.getNumberOfPages());
        int page20Images = imageObjectCount(reader, 20);
        int page21Images = imageObjectCount(reader, 21);
        int page22Images = imageObjectCount(reader, 22);
        assertTrue(page20Images >= 8, "two replacement images should be added to page 20");
        assertTrue(page21Images >= 6, "page 21 should retain its page resources");
        assertTrue(page22Images >= 1, "page 22 should retain its page resources");
        reader.close();
        Files.deleteIfExists(first);
        Files.deleteIfExists(second);
    }

    @Test
    void rendersOnlyEnabledPropertyChecklistItemsInInventorySchedule() throws Exception {
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        byte[] result = service.generate(Map.of("leaseId", "42"), List.of(), List.of(
                new TenancyAgreementPdfService.InventoryItem("客廳 Living Room", "Custom Sofa", "2"),
                new TenancyAgreementPdfService.InventoryItem("鑰匙 Keys", "Custom Key", "1")));

        PdfReader reader = new PdfReader(new ByteArrayInputStream(result));
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String inventoryText = extractor.getTextFromPage(15, true)
                + extractor.getTextFromPage(16, true)
                + extractor.getTextFromPage(17, true);
        String page15Content = new String(reader.getPageContent(15), StandardCharsets.ISO_8859_1);
        assertTrue(inventoryText.contains("Custom Sofa"));
        assertTrue(inventoryText.contains("Custom Key"));
        assertTrue(page15Content.contains("60 42 485 618 re"), "inventory body must be covered before redraw");
        assertTrue(page15Content.contains("Custom Sofa"));
        assertTrue(page15Content.contains("Custom Key"));
        reader.close();
    }

    private Path temporaryPhoto(Color color) throws Exception {
        BufferedImage image = new BufferedImage(80, 60, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) image.setRGB(x, y, color.getRGB());
        }
        Path path = Files.createTempFile("tenancy-photo-", ".jpg");
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", output);
            Files.write(path, output.toByteArray());
        }
        return path;
    }

    private int imageObjectCount(PdfReader reader, int page) {
        PdfDictionary resources = reader.getPageN(page).getAsDict(PdfName.RESOURCES);
        PdfDictionary objects = resources == null ? null : resources.getAsDict(PdfName.XOBJECT);
        return objects == null ? 0 : objects.size();
    }
}
