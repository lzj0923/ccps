package com.ccps.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    void rejectsGenerationWhenContractFieldsWouldBeLeftBlank() {
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.validateRequiredFields(Map.of(
                        "caseNo", "LS-1", "landlordName", "Owner", "tenantName", "Tenant")));

        assertTrue(error.getMessage().contains("租客通讯地址"));
        assertTrue(error.getMessage().contains("水电押金"));
        assertTrue(error.getMessage().contains("租约关联"));
        assertTrue(error.getMessage().contains("交接清单"));
        assertTrue(error.getMessage().contains("房产照片"));
    }

    @Test
    void acceptsAnyNonBlankSupplementValuesBeforeGeneratingTheLease() {
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        Map<String, String> values = new java.util.HashMap<>();
        values.put("leaseId", "1"); values.put("caseNo", "LS-1"); values.put("agreementDate", "2026-08-01");
        values.put("landlordName", "Owner"); values.put("landlordIdentity", "OWNER-1");
        values.put("landlordAddress", "1"); values.put("tenantName", "Tenant");
        values.put("tenantIdentity", "TENANT-1"); values.put("tenantPhone", "+60123456789");
        values.put("tenantEmail", "tenant@example.com"); values.put("tenantAddress", "1");
        values.put("propertyAddress", "Unit 1, Example Residence"); values.put("leaseStart", "2026-08-01");
        values.put("leaseEnd", "2027-07-31"); values.put("monthlyRent", "1000");
        values.put("paymentDay", "5"); values.put("paymentMode", "Bank Transfer");
        values.put("advanceRental", "1000"); values.put("securityDeposit", "2000");
        values.put("utilityDeposit", "500"); values.put("renewalOption", "One year");
        values.put("specialConditions", "None"); values.put("electricityMeter", "E-100");
        values.put("waterMeter", "W-100"); values.put("bankName", "Maybank");
        values.put("bankAccount", "1"); values.put("bankBranch", "1");
        values.put("handoverChecklistIds", "11"); values.put("photoIds", "21");

        service.validateRequiredFields(values);
    }

    @Test
    void defaultsMissingInventoryQuantityToOneWithoutChangingEnteredQuantity() {
        assertEquals("1", TenancyAgreementPdfService.displayInventoryQuantity(null));
        assertEquals("1", TenancyAgreementPdfService.displayInventoryQuantity("   "));
        assertEquals("0", TenancyAgreementPdfService.displayInventoryQuantity("0"));
        assertEquals("2", TenancyAgreementPdfService.displayInventoryQuantity(" 2 "));
    }

    @Test
    void generatesTheNineteenPageAgreementWithoutEmptyPropertyPhotoPages() throws Exception {
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
        assertEquals(19, reader.getNumberOfPages());
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
    void writesLeaseSchedulePaymentRenewalConditionsAndMeterReadingsWhenProvided() throws Exception {
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        byte[] result = service.generate(Map.ofEntries(
                Map.entry("monthlyRent", "RM 4,500.00"),
                Map.entry("leaseStart", "2026-08-01"),
                Map.entry("leaseEnd", "2027-07-31"),
                Map.entry("paymentMode", "Cash"),
                Map.entry("renewalOption", "Two (2) years by mutual agreement"),
                Map.entry("specialConditions", "No pets without written approval."),
                Map.entry("electricityMeter", "ELEC-IN-001"),
                Map.entry("waterMeter", "WATER-IN-002"),
                Map.entry("gasMeter", "GAS-IN-003")));

        PdfReader reader = new PdfReader(new ByteArrayInputStream(result));
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String schedule = extractor.getTextFromPage(14, true);
        String meters = extractor.getTextFromPage(18, true);
        assertTrue(schedule.contains("Cash"));
        assertTrue(schedule.contains("Two (2) years by mutual agreement"));
        assertTrue(schedule.contains("No pets without written approval."));
        assertTrue(meters.contains("ELEC-IN-001"));
        assertTrue(meters.contains("WATER-IN-002"));
        assertTrue(meters.contains("GAS-IN-003"));
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
    void addsOnePhotoPageForUpToSixPropertyPhotos() throws Exception {
        Path first = temporaryPhoto(Color.RED);
        Path second = temporaryPhoto(Color.BLUE);
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();

        byte[] result = service.generate(Map.of("leaseId", "42"), List.of(
                new TenancyAgreementPdfService.PropertyPhotoAsset(first, "image/jpeg", 0, true),
                new TenancyAgreementPdfService.PropertyPhotoAsset(second, "image/jpeg", 1, false)));

        PdfReader reader = new PdfReader(new ByteArrayInputStream(result));
        assertEquals(20, reader.getNumberOfPages());
        int page20Images = imageObjectCount(reader, 20);
        assertTrue(page20Images >= 2, "both property photos should be present on the generated photo page");
        reader.close();
        Files.deleteIfExists(first);
        Files.deleteIfExists(second);
    }

    @Test
    void calculatesPhotoPagesFromTheActualPhotoCountAtSixPerPage() throws Exception {
        Path photo = temporaryPhoto(Color.GREEN);
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        int[][] cases = { { 1, 20 }, { 6, 20 }, { 7, 21 }, { 12, 21 }, { 13, 22 }, { 19, 23 } };

        for (int[] testCase : cases) {
            var asset = new TenancyAgreementPdfService.PropertyPhotoAsset(photo, "image/jpeg", 0, true);
            byte[] result = service.generate(Map.of("leaseId", "42"),
                    java.util.Collections.nCopies(testCase[0], asset));
            String qaOutput = System.getProperty("lease.photo.qa.output", "").trim();
            if (testCase[0] == 7 && !qaOutput.isBlank()) {
                Path output = Path.of(qaOutput);
                Files.createDirectories(output.getParent());
                Files.write(output, result);
            }
            try (PdfReader reader = new PdfReader(new ByteArrayInputStream(result))) {
                assertEquals(testCase[1], reader.getNumberOfPages(),
                        testCase[0] + " photos should produce " + (testCase[1] - 19) + " photo pages");
            }
        }
        Files.deleteIfExists(photo);
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
        assertTrue(inventoryText.split("No additional handover items", -1).length == 3);
        assertTrue(page15Content.contains("60 42 485 618 re"), "inventory body must be covered before redraw");
        String qaOutput = System.getProperty("lease.inventory.qa.output", "").trim();
        if (!qaOutput.isBlank()) {
            Path output = Path.of(qaOutput);
            Files.createDirectories(output.getParent());
            Files.write(output, result);
        }
        reader.close();
    }

    @Test
    void keepsStandardInventoryCategoriesTogetherAndKeepsClauseTenSeventeenOnItsReferencePage() throws Exception {
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        List<TenancyAgreementPdfService.InventoryItem> items = new java.util.ArrayList<>();
        addItems(items, "客廳 Living Room", "LIVING", 17);
        addItems(items, "飯廳 Dining Room", "DINING", 7);
        addItems(items, "廚房 Kitchen", "KITCHEN", 14);
        addItems(items, "主臥室 Master Bedroom", "BEDROOM", 17);
        addItems(items, "主浴室 Master Bathroom", "BATHROOM", 9);
        addItems(items, "遙控器 Remote Control", "REMOTE", 4);
        addItems(items, "鑰匙 Keys", "KEY", 13);
        addItems(items, "門禁卡 Access Card", "ACCESS", 5);

        byte[] result = service.generate(Map.of("landlordName", "Owner", "tenantName", "Tenant",
                "leaseStart", "2026-08-10", "leaseEnd", "2028-12-31"), List.of(), items);
        PdfReader reader = new PdfReader(new ByteArrayInputStream(result));
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String page15 = extractor.getTextFromPage(15, true);
        String page16 = extractor.getTextFromPage(16, true);
        String page17 = extractor.getTextFromPage(17, true);
        String page10Content = new String(reader.getPageContent(10), StandardCharsets.ISO_8859_1);
        String page11Content = new String(reader.getPageContent(11), StandardCharsets.ISO_8859_1);

        assertTrue(page10Content.contains("108 140 Tm"));
        assertTrue(!page11Content.contains("108 140 Tm"));
        assertTogether(page15, page16, page17, "KITCHEN-1", "KITCHEN-14");
        assertTogether(page15, page16, page17, "BATHROOM-1", "BATHROOM-9");
        reader.close();
    }

    @Test
    void manualSizedInventoryUsesTheSameThreePageDistributionAsTheReference() throws Exception {
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        List<TenancyAgreementPdfService.InventoryItem> items = new java.util.ArrayList<>();
        addItems(items, "客廳 Living Room", "LIVING", 7);
        addItems(items, "飯廳 Dining Room", "DINING", 3);
        addItems(items, "廚房 Kitchen", "KITCHEN", 6);
        addItems(items, "主臥室 Master Bedroom", "MASTER", 7);
        addItems(items, "主浴室 Master Bathroom", "MASTER-BATH", 7);
        addItems(items, "次臥 Bedroom 2", "BEDROOM-2", 4);
        addItems(items, "次浴室 Bathroom 2", "BATHROOM-2", 6);
        addItems(items, "遙控器 Remote Control", "REMOTE", 2);
        addItems(items, "鑰匙 Keys", "KEY", 2);
        addItems(items, "門禁卡 Access Card", "ACCESS", 2);

        byte[] result = service.generate(Map.of("landlordName", "Owner", "tenantName", "Tenant",
                "leaseStart", "2026-08-10", "leaseEnd", "2028-12-31"), List.of(), items);
        PdfReader reader = new PdfReader(new ByteArrayInputStream(result));
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        String page15 = extractor.getTextFromPage(15, true);
        String page16 = extractor.getTextFromPage(16, true);
        String page17 = extractor.getTextFromPage(17, true);

        assertTrue(page15.contains("LIVING-1"));
        assertTrue(page15.contains("KITCHEN-6"));
        assertTrue(!page15.contains("MASTER-1"));
        assertTrue(page16.contains("MASTER-1"));
        assertTrue(page16.contains("MASTER-BATH-1"));
        assertTrue(page16.contains("BEDROOM-2-1"));
        assertTrue(!page16.contains("BATHROOM-2-1"));
        assertTrue(page17.contains("BATHROOM-2-1"));
        assertTrue(page17.contains("REMOTE-1"));
        assertTrue(page17.contains("KEY-1"));
        assertTrue(page17.contains("ACCESS-1"));
        reader.close();
    }

    @Test
    void clearsTheFullSignatureIdentityRowsBeforeWritingNewParties() throws Exception {
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        byte[] result = service.generate(Map.of(
                "landlordName", "NEW OWNER",
                "landlordIdentity", "OWNER-ID",
                "tenantName", "NEW TENANT",
                "tenantIdentity", "TENANT-ID"));

        PdfReader reader = new PdfReader(new ByteArrayInputStream(result));
        String page12Content = new String(reader.getPageContent(12), StandardCharsets.ISO_8859_1);
        assertTrue(page12Content.contains("68 628 300 68 re"));
        assertTrue(page12Content.contains("68 400 300 68 re"));
        reader.close();
    }

    @Test
    void derivesTheStandardAdvanceDepositUtilityAndUseWhenTheLeaseOnlyProvidesMonthlyRent() throws Exception {
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        byte[] result = service.generate(Map.of(
                "monthlyRent", "RM 1,000.00",
                "leaseStart", "2026-08-07",
                "leaseEnd", "2028-10-07"));

        PdfReader reader = new PdfReader(new ByteArrayInputStream(result));
        String page13 = new PdfTextExtractor(reader).getTextFromPage(13, true);
        String page14 = new PdfTextExtractor(reader).getTextFromPage(14, true);
        assertTrue(page13.contains("Two (2) years and Two (2) months"));
        assertTrue(page13.contains("7 August 2026"));
        assertTrue(page14.contains("Ringgit Malaysia Two thousand (RM 2,000.00) Only"));
        assertTrue(page14.contains("Ringgit Malaysia Five hundred (RM 500.00) Only"));
        assertTrue(page14.contains("For Residential use only"));
        assertTrue(page14.contains("1st day of every month"));
        reader.close();
    }

    @Test
    void keepsThePropertyAndTermRowsVisuallySeparatedAfterWritingTheFirstSchedule() throws Exception {
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        byte[] result = service.generate(Map.of(
                "propertyAddress", "102, 团结街道202",
                "leaseStart", "2026-08-07",
                "leaseEnd", "2028-10-07"));

        PdfReader reader = new PdfReader(new ByteArrayInputStream(result));
        String page13Content = new String(reader.getPageContent(13), StandardCharsets.ISO_8859_1);
        int propertyCover = page13Content.lastIndexOf("245 328 284 52 re");
        assertTrue(propertyCover >= 0);
        String contentAfterCover = page13Content.substring(propertyCover);
        assertTrue(contentAfterCover.contains("245 328 m"));
        assertTrue(contentAfterCover.contains("529 328 l"));
        reader.close();
    }

    @Test
    void keepsTheReferenceInventoryWhenThePropertyHasNoMaintainedChecklist() throws Exception {
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        byte[] result = service.generate(Map.of(), List.of(), null);

        PdfReader reader = new PdfReader(new ByteArrayInputStream(result));
        String page15Content = new String(reader.getPageContent(15), StandardCharsets.ISO_8859_1);
        assertTrue(!page15Content.contains("60 42 485 618 re"));
        assertTrue(reader.getPageN(15).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.XOBJECT).size() > 0);
        reader.close();
    }

    @Test
    void neverClearsTheReferenceInventoryWhenTheCallerPassesAnEmptyChecklist() throws Exception {
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        byte[] result = service.generate(Map.of(), List.of(), List.of());

        PdfReader reader = new PdfReader(new ByteArrayInputStream(result));
        String page15Content = new String(reader.getPageContent(15), StandardCharsets.ISO_8859_1);
        assertTrue(!page15Content.contains("60 42 485 618 re"));
        assertTrue(reader.getPageN(15).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.XOBJECT).size() > 0);
        reader.close();
    }

    @Test
    void fillsOwnerTenantMoveInAndMeterReadingDataOnTheFinalSchedules() throws Exception {
        TenancyAgreementPdfService service = new TenancyAgreementPdfService();
        byte[] result = service.generate(Map.ofEntries(
                Map.entry("landlordName", "OWNER COMPLETE NAME"),
                Map.entry("landlordIdentity", "OWNER-ID-10086"),
                Map.entry("landlordAddress", "OWNER MAILING ADDRESS"),
                Map.entry("tenantName", "TENANT COMPLETE NAME"),
                Map.entry("tenantIdentity", "TENANT-ID-20086"),
                Map.entry("tenantPhone", "+60123456789"),
                Map.entry("leaseStart", "2026-08-07"),
                Map.entry("handoverDate", "2026-08-07"),
                Map.entry("electricityMeter", "TNB-8821"),
                Map.entry("waterMeter", "WATER-3366")));

        String qaOutput = System.getProperty("lease.data.qa.output", "").trim();
        if (!qaOutput.isBlank()) {
            Path output = Path.of(qaOutput);
            Files.createDirectories(output.getParent());
            Files.write(output, result);
        }

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(result))) {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            String firstSchedule = extractor.getTextFromPage(13, true);
            String meters = extractor.getTextFromPage(18, true);
            String checkIn = extractor.getTextFromPage(19, true);
            assertTrue(firstSchedule.contains("OWNER MAILING ADDRESS"));
            assertTrue(meters.contains("TNB-8821"));
            assertTrue(meters.contains("WATER-3366"));
            assertTrue(checkIn.contains("TENANT COMPLETE NAME"));
            assertTrue(checkIn.contains("TENANT-ID-20086"));
            assertTrue(checkIn.contains("+60123456789"));
            assertTrue(checkIn.contains("7 August 2026"));
        }
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

    private void addItems(List<TenancyAgreementPdfService.InventoryItem> items, String category,
            String prefix, int count) {
        for (int number = 1; number <= count; number++) {
            items.add(new TenancyAgreementPdfService.InventoryItem(category,
                    prefix + "-" + number + " / Bilingual inventory item", "1"));
        }
    }

    private void assertTogether(String page15, String page16, String page17, String first, String last) {
        assertTrue((page15.contains(first) && page15.contains(last))
                || (page16.contains(first) && page16.contains(last))
                || (page17.contains(first) && page17.contains(last)),
                () -> first + " and " + last + " should stay on one inventory page");
    }
}
