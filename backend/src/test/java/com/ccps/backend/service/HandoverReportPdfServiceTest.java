package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.mapper.AdminPropertyHandoverReportMapper;
import com.ccps.backend.mapper.AdminPropertyHandoverReportMapper.PropertyInfo;
import com.ccps.backend.mapper.AdminPropertyHandoverReportMapper.ReportRow;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfNumber;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

@ExtendWith(MockitoExtension.class)
class HandoverReportPdfServiceTest {
    @Mock private AdminPropertyHandoverReportMapper mapper;

    @Test
    void createsPdfForStandardHandoverReport() {
        HandoverReportPdfService service = new HandoverReportPdfService();
        HandoverReportPdfService.Report report = new HandoverReportPdfService.Report(
                "Meridin Medini", "A-25-04", "陳先生", "1房 1廳", "原租客", "交接人員",
                LocalDate.of(2026, 7, 24),
                List.of(new HandoverReportPdfService.Section("鑰匙、門卡及遙控器",
                        List.of(new HandoverReportPdfService.Item("大門鑰匙", "2", "正常", "")))),
                List.of(new HandoverReportPdfService.Issue("客廳牆面", "刮痕", "從押金扣除", "")),
                List.of(new HandoverReportPdfService.Issue("廚房水龍頭", "漏水", "請業主決定", "")),
                "現場已完成交接。", List.of());

        byte[] pdf = service.create(report);

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void placesTenantDefectPartTitleBeforeTenantDefectContent() throws Exception {
        HandoverReportPdfService service = new HandoverReportPdfService();
        HandoverReportPdfService.Report report = new HandoverReportPdfService.Report(
                "Meridin Medini", "A-25-04", "陈先生", "1房 1厅", "原租客", "交接人员",
                LocalDate.of(2026, 7, 24), List.of(),
                List.of(new HandoverReportPdfService.Issue("TENANT-DEFECT-CONTENT", "", "", "")),
                List.of(), "", List.of());

        byte[] pdf = service.create(report);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf))) {
            int titlePage = firstPageContaining(reader, "Deduct From Deposit");
            int contentPage = firstPageContaining(reader, "TENANT-DEFECT-CONTENT");
            assertThat(titlePage).isPositive();
            assertThat(contentPage).isGreaterThan(titlePage);
        }
    }

    @Test
    void includesRoomPhotoWhenItsChecklistSectionIsNotAfterThreeKeySections() throws Exception {
        Path photo = temporaryPhoto();
        HandoverReportPdfService service = new HandoverReportPdfService();
        HandoverReportPdfService.Report report = new HandoverReportPdfService.Report(
                "Meridin Medini", "A-25-04", "陳先生", "1房 1廳", "原租客", "交接人員",
                LocalDate.of(2026, 7, 24),
                List.of(new HandoverReportPdfService.Section("客廳 / Living Room",
                        List.of(new HandoverReportPdfService.Item("沙發", "1", "正常", "")))),
                List.of(), List.of(), "", List.of(new HandoverReportPdfService.Photo(
                        "客廳照片 / Living Room", "ROOM-PHOTO-MUST-BE-IN-PDF", photo)));

        byte[] pdf = service.create(report);

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf))) {
            assertThat(containsImage(reader, 80, 60)).isTrue();
        }
    }

    @Test
    void doesNotRenderInternalPropertyPhotoSourceNoteAsPhotoCaption() throws Exception {
        Path photo = temporaryPhoto();
        HandoverReportPdfService service = new HandoverReportPdfService();

        byte[] withInternalNote = service.create(reportWithPhoto(photo, "从租赁合同补充资料新增"));
        byte[] withoutCaption = service.create(reportWithPhoto(photo, ""));

        assertThat(photoPageContent(withInternalNote, 80, 60))
                .isEqualTo(photoPageContent(withoutCaption, 80, 60));
    }

    @Test
    void keepsUserEnteredPhotoCaption() throws Exception {
        Path photo = temporaryPhoto();
        HandoverReportPdfService service = new HandoverReportPdfService();

        byte[] withUserCaption = service.create(reportWithPhoto(photo, "User-entered room condition"));
        byte[] withoutCaption = service.create(reportWithPhoto(photo, ""));

        assertThat(photoPageContent(withUserCaption, 80, 60))
                .isNotEqualTo(photoPageContent(withoutCaption, 80, 60));
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(withUserCaption))) {
            assertThat(reader.getPageSize(1).getWidth()).isEqualTo(960f);
            assertThat(reader.getPageSize(1).getHeight()).isEqualTo(540f);
            String details = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(2, true);
            assertThat(details).contains("24/07/2026").doesNotContain("24.07.2026");
        }
        String verificationOutput = System.getProperty("handover.pdf.output");
        if (verificationOutput != null && !verificationOutput.isBlank()) {
            Files.write(Path.of(verificationOutput), withUserCaption);
        }
    }

    @Test
    void downloadsStandardReportWithTenantAndOwnerDefectsFromContentJson() throws Exception {
        ReportRow row = new ReportRow(); row.setId(11L); row.setOwnerUnitId(12L); row.setTitle("A-25-04 交屋");
        row.setReportDate(LocalDate.of(2026, 7, 24));
        row.setContentJson("{\"handoverFrom\":\"原租客\",\"handoverTo\":\"業務\",\"sections\":[{\"title\":\"客廳\",\"items\":[{\"name\":\"沙發\",\"quantity\":\"1\",\"condition\":\"正常\",\"remarks\":\"\"}]}],\"tenantIssues\":[{\"name\":\"TENANT-WALL-DAMAGE\"}],\"ownerIssues\":[{\"name\":\"OWNER-AIRCON-FAULT\",\"recommendation\":\"INSPECT-AND-QUOTE\"}],\"photos\":[]}");
        PropertyInfo property = new PropertyInfo(); property.setProjectName("Meridin Medini"); property.setUnitNo("A-25-04"); property.setOwnerName("陳先生");
        when(mapper.ownsProperty(7L, 12L)).thenReturn(1);
        when(mapper.find(12L, 11L)).thenReturn(row);
        when(mapper.propertyInfo(12L)).thenReturn(property);

        var service = new AdminPropertyHandoverReportService(mapper, new ObjectMapper(), "target/test-handover-reports");
        var download = service.download(7L, 12L, 11L);

        assertThat(download.mimeType()).isEqualTo("application/pdf");
        assertThat(download.originalName()).contains("交屋報告");
        assertThat(download.bytes()).startsWith("%PDF".getBytes());
        row.setContentJson("{\"handoverFrom\":\"原租客\",\"handoverTo\":\"業務\",\"sections\":[{\"title\":\"客廳\",\"items\":[{\"name\":\"沙發\",\"quantity\":\"1\",\"condition\":\"正常\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"photos\":[]}");
        var emptyDownload = service.download(7L, 12L, 11L);
        try (PdfReader populated = new PdfReader(new ByteArrayInputStream(download.bytes()));
                PdfReader empty = new PdfReader(new ByteArrayInputStream(emptyDownload.bytes()))) {
            assertThat(allText(populated)).contains("TENANT-WALL-DAMAGE", "OWNER-AIRCON-FAULT", "INSPECT-AND-QUOTE");
            assertThat(allText(empty)).doesNotContain("TENANT-WALL-DAMAGE", "OWNER-AIRCON-FAULT", "INSPECT-AND-QUOTE");
        }
    }

    private Path temporaryPhoto() throws Exception {
        BufferedImage image = new BufferedImage(80, 60, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) image.setRGB(x, y, Color.RED.getRGB());
        }
        Path path = Files.createTempFile("handover-photo-", ".jpg");
        ImageIO.write(image, "jpg", path.toFile());
        return path;
    }

    private HandoverReportPdfService.Report reportWithPhoto(Path photo, String caption) {
        return new HandoverReportPdfService.Report(
                "Meridin Medini", "A-25-04", "陳先生", "1房 1廳", "原租客", "交接人員",
                LocalDate.of(2026, 7, 24),
                List.of(new HandoverReportPdfService.Section("客廳 / Living Room",
                        List.of(new HandoverReportPdfService.Item("沙發", "1", "正常", "")))),
                List.of(new HandoverReportPdfService.Issue("客厅墙面刮伤", "", "", "")),
                List.of(new HandoverReportPdfService.Issue("主卧空调无法制冷", "", "安排技师检查并报价", "")),
                "", List.of(
                        new HandoverReportPdfService.Photo("客廳照片 / Living Room", caption, photo),
                        new HandoverReportPdfService.Photo("客廳照片 / Living Room", caption, photo)));
    }

    private byte[] photoPageContent(byte[] pdf, int width, int height) throws Exception {
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf))) {
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                if (pageContainsImage(reader, page, width, height)) return reader.getPageContent(page);
            }
        }
        throw new AssertionError("Photo page was not found");
    }

    private int firstPageContaining(PdfReader reader, String expected) throws Exception {
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            if (extractor.getTextFromPage(page, true).contains(expected)) return page;
        }
        return -1;
    }

    private String allText(PdfReader reader) throws Exception {
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        StringBuilder text = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            text.append(extractor.getTextFromPage(page, true)).append('\n');
        }
        return text.toString();
    }

    private boolean containsImage(PdfReader reader, int width, int height) {
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            if (pageContainsImage(reader, page, width, height)) return true;
        }
        return false;
    }

    private boolean pageContainsImage(PdfReader reader, int page, int width, int height) {
        PdfDictionary resources = reader.getPageN(page).getAsDict(PdfName.RESOURCES);
        PdfDictionary objects = resources == null ? null : resources.getAsDict(PdfName.XOBJECT);
        if (objects == null) return false;
        for (PdfName name : objects.getKeys()) {
            PdfObject object = PdfReader.getPdfObject(objects.get(name));
            if (!(object instanceof PdfDictionary image) || !PdfName.IMAGE.equals(image.getAsName(PdfName.SUBTYPE))) continue;
            PdfNumber imageWidth = image.getAsNumber(PdfName.WIDTH);
            PdfNumber imageHeight = image.getAsNumber(PdfName.HEIGHT);
            if (imageWidth != null && imageHeight != null
                    && imageWidth.intValue() == width && imageHeight.intValue() == height) return true;
        }
        return false;
    }
}
