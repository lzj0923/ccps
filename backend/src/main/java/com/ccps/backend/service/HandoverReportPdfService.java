package com.ccps.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.lowagie.text.Document;
import com.lowagie.text.Chunk;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/** Generates the CCPS handover-report layout supplied by the customer. */
public class HandoverReportPdfService {
    private static final Rectangle REFERENCE_PAGE = new Rectangle(960, 540);
    private static final String KEY_PHOTOS = "鑰匙、通行卡和遙控器照片 / Keys, Access Card & Remote Control";
    private static final List<String> UNIT_PHOTO_SECTIONS = List.of(
            "客廳照片 / Living Room",
            "飯廳照片 / Dining Room",
            "廚房照片 / Kitchen",
            "主臥室照片 / Master Bedroom",
            "主浴室照片 / Master Bathroom");
    private static final String TENANT_PHOTOS = "瑕疵 (扣租客押金) / Defects (Deduct From Deposit)";
    private static final String OWNER_PHOTOS = "瑕疵 (询问屋主是否要维修) / Defects (Ask Owner If Repairs Are Needed)";
    private static final Set<String> INTERNAL_PHOTO_SOURCE_NOTES = Set.of(
            "从租赁合同补充资料新增",
            "從租賃合約補充資料新增",
            "Added from tenancy agreement details",
            "由附件签约生成交接报告时新增",
            "由附件簽約產生交接報告時新增",
            "Added while generating a handover report from Files & Signing");

    public byte[] create(Report report) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(REFERENCE_PAGE, 38, 38, 24, 42);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new CcpsLogoPageEvent());
            document.open();
            BaseFont base = PdfFontResources.regular();
            com.lowagie.text.Font cover = new com.lowagie.text.Font(base, 40, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font title = new com.lowagie.text.Font(base, 28, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font heading = new com.lowagie.text.Font(base, 22, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font body = new com.lowagie.text.Font(base, 14);
            com.lowagie.text.Font caption = new com.lowagie.text.Font(base, 15, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font recommendation = new com.lowagie.text.Font(base, 14,
                    com.lowagie.text.Font.BOLD, java.awt.Color.RED);

            Paragraph coverTitle = paragraph("交接报告\nHandover Report", cover);
            coverTitle.setAlignment(Element.ALIGN_CENTER);
            coverTitle.setLeading(48);
            coverTitle.setSpacingBefore(115);
            coverTitle.setSpacingAfter(56);
            document.add(coverTitle);
            Paragraph company = paragraph("BY CCPS PROPERTIES MANAGEMENT SDN BHD",
                    new com.lowagie.text.Font(base, 21));
            company.setAlignment(Element.ALIGN_CENTER);
            document.add(company);
            document.newPage();

            PdfPTable details = new PdfPTable(new float[] { .5f, 1.45f, 2.8f });
            details.setWidthPercentage(82);
            details.setHorizontalAlignment(Element.ALIGN_LEFT);
            addSpanningTitle(details, "Details 详情：", title, 3);
            addDetail(details, "1", "业主名\nOwner Name", report.ownerName(), body);
            addDetail(details, "2", "项目名称\nProject Name", report.projectName(), body);
            addDetail(details, "3", "单元号\nUnit Nos", report.unitNo(), body);
            addDetail(details, "4", "款式\nUnit Type", report.unitType(), body);
            addDetail(details, "5", "交接对象\nHandover From", report.handoverFrom(), body);
            addDetail(details, "6", "交接业务\nHandover Person", report.handoverTo(), body);
            addDetail(details, "7", "交接日期\nHandover Date", date(report.handoverDate()), body);
            document.add(details);

            addPartCover(document, "（一）. 钥匙,通行卡和遥控器", "Keys, Access Card & Remote Control", title);
            for (Section section : report.sections()) {
                if (isAccessSection(section.title())) addInventoryPages(document, section, body, heading, 11);
            }
            int keyPhotoPages = photoPageCount(KEY_PHOTOS, report.photos());
            if (keyPhotoPages > 0) addPhotoPages(document, KEY_PHOTOS, report.photos(), keyPhotoPages, caption, heading);

            addPartCover(document, "（二）. 单位和物品", "Unit & Item", title);
            for (Section section : report.sections()) {
                if (!isAccessSection(section.title())) addInventoryPages(document, section, body, heading, 11);
            }
            LinkedHashSet<String> unitPhotoSections = new LinkedHashSet<>(UNIT_PHOTO_SECTIONS);
            if (report.photos() != null) {
                report.photos().stream().map(Photo::section).filter(this::isUnitPhotoSection).forEach(unitPhotoSections::add);
            }
            for (String photoSection : unitPhotoSections) {
                int pages = photoPageCount(photoSection, report.photos());
                if (pages > 0) addPhotoPages(document, photoSection, report.photos(), pages, caption, heading);
            }

            addPartCover(document, "（三）. 瑕疵（扣租客押金）", "Defects (Deduct From Deposit)", title);
            addTenantIssues(document, report.tenantIssues(), body, heading);
            int tenantPhotoPages = photoPageCount(TENANT_PHOTOS, report.photos());
            if (tenantPhotoPages > 0) addPhotoPages(document, TENANT_PHOTOS, report.photos(), tenantPhotoPages, caption, heading);
            addPartCover(document, "（四）. 瑕疵（询问屋主是否要维修）", "Defects (Ask Owner If Repairs Are Needed)", title);
            addOwnerIssues(document, report.ownerIssues(), body, heading, recommendation);
            int ownerPhotoPages = photoPageCount(OWNER_PHOTOS, report.photos());
            if (ownerPhotoPages > 0) addPhotoPages(document, OWNER_PHOTOS, report.photos(), ownerPhotoPages, caption, heading);
            if (report.remarks() != null && !report.remarks().isBlank()) {
                document.add(spacingHeading("备注 / Remarks", heading));
                document.add(paragraph(report.remarks(), body));
            }
            document.newPage();
            Paragraph thanks = paragraph("谢谢\nThank You", title);
            thanks.setAlignment(Element.ALIGN_CENTER);
            thanks.setLeading(36);
            thanks.setSpacingBefore(175);
            document.add(thanks);
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate handover report PDF", exception);
        }
    }

    private void addPartCover(Document document, String chinese, String english, com.lowagie.text.Font title) throws Exception {
        document.newPage();
        Paragraph heading = paragraph(chinese + "\n" + english, title);
        heading.setLeading(36);
        heading.setSpacingBefore(178);
        heading.setAlignment(Element.ALIGN_CENTER);
        document.add(heading);
    }

    private void addInventoryPages(Document document, Section section, com.lowagie.text.Font body,
            com.lowagie.text.Font heading, int rowsPerPage) throws Exception {
        List<Item> items = section.items() == null ? List.of() : section.items();
        if (items.isEmpty()) {
            document.newPage();
            addInventory(document, section, List.of(), 0, body, heading);
            return;
        }
        for (int start = 0; start < items.size(); start += rowsPerPage) {
            document.newPage();
            addInventory(document, section, items.subList(start, Math.min(start + rowsPerPage, items.size())), start, body, heading);
        }
    }

    private void addInventory(Document document, Section section, List<Item> items, int startNumber, com.lowagie.text.Font body,
            com.lowagie.text.Font heading) throws Exception {
        document.add(spacingHeading(singleLineSection(section.title()) + "：", heading));
        PdfPTable table = new PdfPTable(new float[] { .45f, 2.5f, .75f, 1.55f });
        table.setWidthPercentage(100);
        addHeader(table, "No.", body, 43);
        addHeader(table, "交接清单\nHandover List", body, 43);
        addHeader(table, "数量\nQuantity", body, 43);
        addHeader(table, "备注\nRemark", body, 43);
        int number = startNumber + 1;
        for (Item item : items) {
            addCell(table, String.valueOf(number++), body, Element.ALIGN_CENTER, 31);
            addCell(table, text(item.name()), body, Element.ALIGN_LEFT, 31);
            addCell(table, text(item.quantity()), body, Element.ALIGN_CENTER, 31);
            String remarks = item.remarks();
            if ((remarks == null || remarks.isBlank()) && item.condition() != null && !item.condition().isBlank()) remarks = item.condition();
            addCell(table, text(remarks), body, Element.ALIGN_LEFT, 31);
        }
        document.add(table);
    }

    private void addTenantIssues(Document document, List<Issue> issues, com.lowagie.text.Font body,
            com.lowagie.text.Font heading) throws Exception {
        List<Issue> rows = issues == null ? List.of() : issues;
        int pages = Math.max(1, (int) Math.ceil(rows.size() / 8.0));
        for (int page = 0; page < pages; page++) {
            document.newPage();
            PdfPTable table = new PdfPTable(new float[] { .45f, 4.55f });
            table.setWidthPercentage(68);
            table.setHorizontalAlignment(Element.ALIGN_LEFT);
            addSpanningTitle(table, "瑕疵（扣租客押金）\nDefects (Deduct From Deposit)：", heading, 2);
            addHeader(table, "No.", body, 45);
            addHeader(table, "维修与维护：\nRepair & Maintenance：", body, 45);
            int from = page * 8;
            int to = Math.min(from + 8, rows.size());
            for (int index = from; index < to; index++) {
                Issue issue = rows.get(index);
                addCell(table, String.valueOf(index + 1), body, Element.ALIGN_CENTER, 42);
                addCell(table, "问题 ： " + issueText(issue, false), body, Element.ALIGN_LEFT, 42);
            }
            document.add(table);
        }
    }

    private void addOwnerIssues(Document document, List<Issue> issues, com.lowagie.text.Font body,
            com.lowagie.text.Font heading, com.lowagie.text.Font recommendation) throws Exception {
        List<Issue> rows = issues == null ? List.of() : issues;
        int pages = Math.max(1, (int) Math.ceil(rows.size() / 6.0));
        for (int page = 0; page < pages; page++) {
            document.newPage();
            PdfPTable table = new PdfPTable(new float[] { .45f, 4.55f });
            table.setWidthPercentage(68);
            table.setHorizontalAlignment(Element.ALIGN_LEFT);
            addSpanningTitle(table, "瑕疵（询问屋主是否要维修）\nDefects (Ask Owner If Repairs Are Needed)：", heading, 2);
            addHeader(table, "No.", body, 45);
            addHeader(table, "维修与维护：\nRepair & Maintenance：", body, 45);
            int from = page * 6;
            int to = Math.min(from + 6, rows.size());
            for (int index = from; index < to; index++) {
                Issue issue = rows.get(index);
                addCell(table, String.valueOf(index + 1), body, Element.ALIGN_CENTER, 56);
                PdfPCell issueCell = new PdfPCell();
                issueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                issueCell.setMinimumHeight(56);
                issueCell.setPadding(7);
                Paragraph problem = paragraph("问题 ： " + issueText(issue, false), body);
                problem.setLeading(18);
                issueCell.addElement(problem);
                Paragraph advice = new Paragraph();
                advice.setLeading(18);
                advice.add(mixedPhrase("建议 ： " + text(issue.recommendation()), recommendation));
                issueCell.addElement(advice);
                table.addCell(issueCell);
            }
            document.add(table);
        }
    }

    private void addPhotoPages(Document document, String section, List<Photo> photos, int pageCount,
            com.lowagie.text.Font caption, com.lowagie.text.Font heading) throws Exception {
        List<Photo> matching = photos == null ? List.of() : photos.stream().filter(photo -> section.equals(photo.section())
                && photo.path() != null && Files.isRegularFile(photo.path())).toList();
        for (int page = 0; page < pageCount; page++) {
            document.newPage();
            Paragraph pageHeading = paragraph(photoSectionHeading(section), heading);
            pageHeading.setAlignment(Element.ALIGN_CENTER);
            pageHeading.setLeading(28);
            pageHeading.setSpacingAfter(14);
            document.add(pageHeading);
            PdfPTable photoGrid = new PdfPTable(2);
            photoGrid.setWidthPercentage(88);
            photoGrid.setHorizontalAlignment(Element.ALIGN_CENTER);
            int from = page * 2;
            for (int slot = 0; slot < 2; slot++) {
                int index = from + slot;
                if (index >= matching.size()) {
                    PdfPCell empty = new PdfPCell();
                    empty.setBorder(Rectangle.NO_BORDER);
                    photoGrid.addCell(empty);
                    continue;
                }
                Photo photo = matching.get(index);
                PdfPTable card = new PdfPTable(1);
                Image image = Image.getInstance(photo.path().toAbsolutePath().toString());
                image.scaleToFit(368, 258);
                image.setAlignment(Element.ALIGN_CENTER);
                PdfPCell imageCell = new PdfPCell();
                imageCell.setFixedHeight(270);
                imageCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                imageCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                imageCell.setPadding(0);
                imageCell.addElement(image);
                card.addCell(imageCell);

                String photoCaption = visiblePhotoCaption(photo.caption());
                PdfPCell captionCell = new PdfPCell(mixedPhrase(
                        photoCaption.isBlank() ? "" : (index + 1) + ". " + photoCaption, caption));
                captionCell.setFixedHeight(48);
                captionCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                captionCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                captionCell.setPadding(6);
                if (photoCaption.isBlank()) captionCell.setBorder(Rectangle.NO_BORDER);
                card.addCell(captionCell);

                PdfPCell cardCell = new PdfPCell(card);
                cardCell.setBorder(Rectangle.NO_BORDER);
                cardCell.setPadding(10);
                photoGrid.addCell(cardCell);
            }
            document.add(photoGrid);
        }
    }

    private int photoPageCount(String section, List<Photo> photos) {
        long count = photos == null ? 0 : photos.stream().filter(photo -> section.equals(photo.section())
                && photo.path() != null && Files.isRegularFile(photo.path())).count();
        return (int) Math.ceil(count / 2.0);
    }

    private String visiblePhotoCaption(String caption) {
        String value = caption == null ? "" : caption.trim();
        return INTERNAL_PHOTO_SOURCE_NOTES.contains(value) ? "" : value;
    }

    private String photoSectionHeading(String section) {
        if (section == null) return "";
        int divider = section.indexOf(" / ");
        return divider < 0 ? section : section.substring(0, divider) + "\n" + section.substring(divider + 3);
    }

    private String singleLineSection(String section) {
        return section == null ? "" : section.replace(" / ", " ");
    }

    private boolean isAccessSection(String section) {
        if (section == null) return false;
        String value = section.trim().toLowerCase();
        return value.startsWith("鑰匙") || value.startsWith("钥匙") || value.startsWith("門禁卡")
                || value.startsWith("门禁卡") || value.startsWith("通行卡") || value.startsWith("遙控器")
                || value.startsWith("遥控器") || value.startsWith("keys") || value.startsWith("access card")
                || value.startsWith("remote control");
    }

    private boolean isUnitPhotoSection(String section) {
        return section != null && !section.isBlank() && !KEY_PHOTOS.equals(section)
                && !TENANT_PHOTOS.equals(section) && !OWNER_PHOTOS.equals(section);
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
        addCell(table, number, body, Element.ALIGN_CENTER, 45);
        addCell(table, label, body, Element.ALIGN_LEFT, 45);
        addCell(table, text(value), body, Element.ALIGN_CENTER, 45);
    }

    private Paragraph spacingHeading(String value, com.lowagie.text.Font font) {
        Paragraph paragraph = paragraph(value, font);
        paragraph.setSpacingBefore(3);
        paragraph.setSpacingAfter(7);
        return paragraph;
    }

    private void addSpanningTitle(PdfPTable table, String value, com.lowagie.text.Font font, int columns) {
        PdfPCell cell = new PdfPCell(mixedPhrase(value, font));
        cell.setColspan(columns);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(7);
        cell.setMinimumHeight(45);
        table.addCell(cell);
    }

    private void addHeader(PdfPTable table, String value, com.lowagie.text.Font font, float minimumHeight) {
        PdfPCell cell = new PdfPCell(mixedPhrase(value, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setMinimumHeight(minimumHeight);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String value, com.lowagie.text.Font font, int alignment, float minimumHeight) {
        PdfPCell cell = new PdfPCell(mixedPhrase(value, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setMinimumHeight(minimumHeight);
        table.addCell(cell);
    }

    private String text(String value) { return value == null || value.isBlank() ? "—" : value; }
    private String date(LocalDate value) { return value == null ? "—" : String.format("%02d/%02d/%04d", value.getDayOfMonth(), value.getMonthValue(), value.getYear()); }

    private Paragraph paragraph(String value, com.lowagie.text.Font font) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(mixedPhrase(value, font));
        return paragraph;
    }

    private Phrase mixedPhrase(String value, com.lowagie.text.Font cjkFont) {
        Phrase phrase = new Phrase();
        com.lowagie.text.Font latinFont = latinFont(cjkFont);
        String text = value == null ? "" : value;
        StringBuilder run = new StringBuilder();
        boolean latin = false;
        boolean initialized = false;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            boolean nextLatin = character <= 0x7f;
            if (initialized && nextLatin != latin) {
                phrase.add(new Chunk(run.toString(), latin ? latinFont : cjkFont));
                run.setLength(0);
            }
            run.append(character);
            latin = nextLatin;
            initialized = true;
        }
        if (!run.isEmpty()) phrase.add(new Chunk(run.toString(), latin ? latinFont : cjkFont));
        return phrase;
    }

    private com.lowagie.text.Font latinFont(com.lowagie.text.Font source) {
        try {
            BaseFont latin = (source.getStyle() & com.lowagie.text.Font.BOLD) != 0
                    ? PdfFontResources.bold() : PdfFontResources.regular();
            return new com.lowagie.text.Font(latin, source.getSize(), source.getStyle(), source.getColor());
        } catch (Exception ignored) {
            return source;
        }
    }

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
