package com.ccps.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/** Generates the CCPS handover-report layout supplied by the customer. */
public class HandoverReportPdfService {
    private static final String KEY_PHOTOS = "鑰匙、通行卡和遙控器照片 / Keys, Access Card & Remote Control";
    private static final String TENANT_PHOTOS = "瑕疵 (扣租客押金) / Defects (Deduct From Deposit)";
    private static final String OWNER_PHOTOS = "瑕疵 (询问屋主是否要维修) / Defects (Ask Owner If Repairs Are Needed)";

    public byte[] create(Report report) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new CcpsLogoPageEvent());
            document.open();
            BaseFont base = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            com.lowagie.text.Font cover = new com.lowagie.text.Font(base, 24, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font title = new com.lowagie.text.Font(base, 17, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font heading = new com.lowagie.text.Font(base, 12, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font body = new com.lowagie.text.Font(base, 9);

            Paragraph coverTitle = new Paragraph("交接报告\nHandover Report", cover);
            coverTitle.setAlignment(Element.ALIGN_CENTER);
            coverTitle.setSpacingBefore(230);
            coverTitle.setSpacingAfter(18);
            document.add(coverTitle);
            Paragraph company = new Paragraph("BY CCPS PROPERTIES MANAGEMENT SDN BHD", heading);
            company.setAlignment(Element.ALIGN_CENTER);
            document.add(company);
            document.newPage();

            document.add(spacingHeading("Details 详情：", title));
            PdfPTable details = new PdfPTable(new float[] { .5f, 1.45f, 2.8f });
            details.setWidthPercentage(100);
            addDetail(details, "1", "业主名\nOwner Name", report.ownerName(), body);
            addDetail(details, "2", "项目名称\nProject Name", report.projectName(), body);
            addDetail(details, "3", "单元号\nUnit Nos", report.unitNo(), body);
            addDetail(details, "4", "款式\nUnit Type", report.unitType(), body);
            addDetail(details, "5", "交接对象\nHandover From", report.handoverFrom(), body);
            addDetail(details, "6", "交接业务\nHandover Person", report.handoverTo(), body);
            addDetail(details, "7", "交接日期\nHandover Date", date(report.handoverDate()), body);
            document.add(details);

            addPartCover(document, "（一）钥匙, 通行卡和遥控器", "Keys, Access Card & Remote Control", title);
            for (int i = 0; i < Math.min(3, report.sections().size()); i++) addInventoryPages(document, report.sections().get(i), body, heading, i == 0 ? 11 : 11);
            addPhotoPages(document, KEY_PHOTOS, report.photos(), 1, body, heading);

            addPartCover(document, "（二）单位和物品", "Unit & Item", title);
            for (int i = 3; i < report.sections().size(); i++) {
                Section section = report.sections().get(i);
                addInventoryPages(document, section, body, heading, 11);
                int pages = section.title().startsWith("客廳") || section.title().startsWith("廚房") || section.title().startsWith("主臥室") ? 2 : 1;
                addPhotoPages(document, photoSection(section.title()), report.photos(), pages, body, heading);
            }

            addTenantIssues(document, report.tenantIssues(), body, heading);
            int tenantPhotoPages = photoPageCount(TENANT_PHOTOS, report.photos());
            if (tenantPhotoPages > 0) addPhotoPages(document, TENANT_PHOTOS, report.photos(), tenantPhotoPages, body, heading);
            addPartCover(document, "（四）瑕疵 (询问屋主是否要维修)", "Defects (Ask Owner If Repairs Are Needed)", title);
            addOwnerIssues(document, report.ownerIssues(), body, heading);
            int ownerPhotoPages = photoPageCount(OWNER_PHOTOS, report.photos());
            if (ownerPhotoPages > 0) addPhotoPages(document, OWNER_PHOTOS, report.photos(), ownerPhotoPages, body, heading);
            if (report.remarks() != null && !report.remarks().isBlank()) {
                document.add(spacingHeading("备注 / Remarks", heading));
                document.add(new Paragraph(report.remarks(), body));
            }
            document.newPage();
            Paragraph thanks = new Paragraph("谢谢\nThank You", title);
            thanks.setAlignment(Element.ALIGN_CENTER);
            thanks.setSpacingBefore(28);
            document.add(thanks);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate handover report PDF", exception);
        }
    }

    private void addPartCover(Document document, String chinese, String english, com.lowagie.text.Font title) throws Exception {
        document.newPage();
        Paragraph heading = new Paragraph(chinese + "\n" + english, title);
        heading.setSpacingBefore(220);
        heading.setAlignment(Element.ALIGN_CENTER);
        document.add(heading);
    }

    private void addInventoryPages(Document document, Section section, com.lowagie.text.Font body,
            com.lowagie.text.Font heading, int rowsPerPage) throws Exception {
        List<Item> items = section.items() == null ? List.of() : section.items();
        if (items.isEmpty()) { addInventory(document, section, List.of(), 0, body, heading); return; }
        for (int start = 0; start < items.size(); start += rowsPerPage) {
            document.newPage();
            addInventory(document, section, items.subList(start, Math.min(start + rowsPerPage, items.size())), start, body, heading);
        }
    }

    private void addInventory(Document document, Section section, List<Item> items, int startNumber, com.lowagie.text.Font body,
            com.lowagie.text.Font heading) throws Exception {
        document.add(spacingHeading(section.title() + "：", heading));
        PdfPTable table = new PdfPTable(new float[] { .45f, 2.5f, .75f, 1.55f });
        table.setWidthPercentage(100);
        addHeader(table, "No.", body);
        addHeader(table, "交接清单\nHandover List", body);
        addHeader(table, "数量\nQuantity", body);
        addHeader(table, "备注\nRemark", body);
        int number = startNumber + 1;
        for (Item item : items) {
            addCell(table, String.valueOf(number++), body, Element.ALIGN_CENTER);
            addCell(table, text(item.name()), body, Element.ALIGN_LEFT);
            addCell(table, text(item.quantity()), body, Element.ALIGN_CENTER);
            String remarks = item.remarks();
            if ((remarks == null || remarks.isBlank()) && item.condition() != null && !item.condition().isBlank()) remarks = item.condition();
            addCell(table, text(remarks), body, Element.ALIGN_LEFT);
        }
        document.add(table);
    }

    private void addTenantIssues(Document document, List<Issue> issues, com.lowagie.text.Font body,
            com.lowagie.text.Font heading) throws Exception {
        document.newPage();
        document.add(spacingHeading("（三）瑕疵 (扣租客押金)\nDefects (Deduct From Deposit)：", heading));
        PdfPTable table = new PdfPTable(new float[] { .45f, 4.55f });
        table.setWidthPercentage(100);
        addHeader(table, "No.", body);
        addHeader(table, "维修与维护\nRepair & Maintenance", body);
        int number = 1;
        for (Issue issue : issues) {
            addCell(table, String.valueOf(number++), body, Element.ALIGN_CENTER);
            addCell(table, issueText(issue, false), body, Element.ALIGN_LEFT);
        }
        document.add(table);
    }

    private void addOwnerIssues(Document document, List<Issue> issues, com.lowagie.text.Font body,
            com.lowagie.text.Font heading) throws Exception {
        document.newPage();
        document.add(spacingHeading("瑕疵 (询问屋主是否要维修)\nDefects (Ask Owner If Repairs Are Needed)：", heading));
        PdfPTable table = new PdfPTable(new float[] { .45f, 2.7f, 2.3f });
        table.setWidthPercentage(100);
        addHeader(table, "No.", body);
        addHeader(table, "问题\nRepair & Maintenance", body);
        addHeader(table, "建议\nRecommendation", body);
        int number = 1;
        for (Issue issue : issues) {
            addCell(table, String.valueOf(number++), body, Element.ALIGN_CENTER);
            addCell(table, issueText(issue, false), body, Element.ALIGN_LEFT);
            addCell(table, text(issue.recommendation()), body, Element.ALIGN_LEFT);
        }
        document.add(table);
    }

    private void addPhotoPages(Document document, String section, List<Photo> photos, int pageCount,
            com.lowagie.text.Font body, com.lowagie.text.Font heading) throws Exception {
        List<Photo> matching = photos == null ? List.of() : photos.stream().filter(photo -> section.equals(photo.section())
                && photo.path() != null && Files.isRegularFile(photo.path())).toList();
        for (int page = 0; page < pageCount; page++) {
            document.newPage();
            document.add(spacingHeading(section, heading));
            int from = page * 2;
            for (Photo photo : matching.subList(Math.min(from, matching.size()), Math.min(from + 2, matching.size()))) {
                if (photo.caption() != null && !photo.caption().isBlank()) document.add(new Paragraph(photo.caption(), body));
                Image image = Image.getInstance(photo.path().toAbsolutePath().toString());
                image.scaleToFit(700, 390);
                image.setAlignment(Element.ALIGN_CENTER);
                document.add(image);
            }
        }
    }

    private int photoPageCount(String section, List<Photo> photos) {
        long count = photos == null ? 0 : photos.stream().filter(photo -> section.equals(photo.section())
                && photo.path() != null && Files.isRegularFile(photo.path())).count();
        return (int) Math.ceil(count / 2.0);
    }

    private String photoSection(String section) {
        if (section.startsWith("客廳")) return "客廳照片 / Living Room";
        if (section.startsWith("飯廳")) return "飯廳照片 / Dining Room";
        if (section.startsWith("廚房")) return "廚房照片 / Kitchen";
        if (section.startsWith("主臥室")) return "主臥室照片 / Master Bedroom";
        if (section.startsWith("主浴室")) return "主浴室照片 / Master Bathroom";
        return section;
    }

    private String issueText(Issue issue, boolean includeRecommendation) {
        String issueName = text(issue.name());
        String condition = issue.condition();
        String remarks = issue.remarks();
        StringBuilder result = new StringBuilder(issueName);
        if (condition != null && !condition.isBlank() && !condition.equals(issueName)) result.append("\n").append(condition);
        if (remarks != null && !remarks.isBlank()) result.append("\n").append(remarks);
        if (includeRecommendation && issue.recommendation() != null && !issue.recommendation().isBlank()) result.append("\n").append(issue.recommendation());
        return result.toString();
    }

    private void addDetail(PdfPTable table, String number, String label, String value, com.lowagie.text.Font body) {
        addCell(table, number, body, Element.ALIGN_CENTER);
        addCell(table, label, body, Element.ALIGN_LEFT);
        addCell(table, text(value), body, Element.ALIGN_CENTER);
    }

    private Paragraph spacingHeading(String value, com.lowagie.text.Font font) {
        Paragraph paragraph = new Paragraph(value, font);
        paragraph.setSpacingBefore(14);
        paragraph.setSpacingAfter(7);
        return paragraph;
    }

    private void addHeader(PdfPTable table, String value, com.lowagie.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(new java.awt.Color(245, 245, 245));
        cell.setPadding(6);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String value, com.lowagie.text.Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        table.addCell(cell);
    }

    private String text(String value) { return value == null || value.isBlank() ? "—" : value; }
    private String date(LocalDate value) { return value == null ? "—" : String.format("%02d.%02d.%04d", value.getDayOfMonth(), value.getMonthValue(), value.getYear()); }

    public record Report(String projectName, String unitNo, String ownerName, String unitType, String handoverFrom,
            String handoverTo, LocalDate handoverDate, List<Section> sections, List<Issue> tenantIssues,
            List<Issue> ownerIssues, String remarks, List<Photo> photos) { }
    public record Section(String title, List<Item> items) { }
    public record Item(String name, String quantity, String condition, String remarks) { }
    public record Issue(String name, String condition, String recommendation, String remarks) { }
    public record Photo(String section, String caption, Path path) { }

    private static class CcpsLogoPageEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try (InputStream input = HandoverReportPdfService.class.getResourceAsStream("/report-assets/ccps-handover-logo.png")) {
                if (input == null) return;
                Image logo = Image.getInstance(input.readAllBytes());
                logo.scaleToFit(155, 42);
                logo.setAbsolutePosition(document.right() - logo.getScaledWidth(), 18);
                writer.getDirectContent().addImage(logo);
            } catch (Exception ignored) {
                // The report remains downloadable even if a deployment omits the optional logo asset.
            }
        }
    }
}
