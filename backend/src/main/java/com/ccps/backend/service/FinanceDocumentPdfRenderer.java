/* Hallmark · pre-emit critique: P5 H5 E5 S5 R5 V4 */
package com.ccps.backend.service;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Shared A4 renderer for every generated invoice and receipt.
 *
 * <p>The layout follows the supplied black-and-white accounting reference and
 * deliberately contains no printed page number or page watermark.</p>
 */
final class FinanceDocumentPdfRenderer {
    private static final float PAGE_WIDTH = PageSize.A4.getWidth();
    private static final float PAGE_HEIGHT = PageSize.A4.getHeight();
    private static final float MARGIN = 18f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2f);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String DISCLAIMER = "This is a computer generated document and no signature is required.";

    private FinanceDocumentPdfRenderer() { }

    static void write(OutputStream output, Data data) throws Exception {
        Document document = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);
        PdfWriter writer = PdfWriter.getInstance(document, output);
        document.open();

        BaseFont latin = PdfFontResources.regular();
        BaseFont latinBold = PdfFontResources.bold();
        BaseFont unicode = latin;
        Fonts fonts = new Fonts(
                new Font(latinBold, 9f), new Font(latin, 6f), new Font(latinBold, 6.4f),
                new Font(latin, 6.4f), new Font(unicode, 6.4f), new Font(latinBold, 5.8f),
                new Font(latin, 5.8f), new Font(unicode, 5.8f));

        PdfContentByte canvas = writer.getDirectContent();
        drawHeader(canvas, data, fonts);
        drawPartyAndMetadata(canvas, data, fonts);
        drawItems(canvas, data, fonts);
        drawFooter(canvas, data, fonts);

        document.close();
    }

    private static void drawHeader(PdfContentByte canvas, Data data, Fonts fonts) {
        float y = PAGE_HEIGHT - 21f;
        showText(canvas, data.companyName(), fonts.company(), PAGE_WIDTH / 2f, y, Element.ALIGN_CENTER);
        y -= 9f;
        if (hasText(data.companyRegistration())) {
            showText(canvas, data.companyRegistration(), fonts.small(), PAGE_WIDTH / 2f, y, Element.ALIGN_CENTER);
            y -= 8f;
        }
        for (String line : cleanLines(data.companyDetails())) {
            showText(canvas, line, fonts.tinyUnicode(), PAGE_WIDTH / 2f, y, Element.ALIGN_CENTER);
            y -= 7f;
        }
        line(canvas, MARGIN, Math.min(y - 2f, PAGE_HEIGHT - 58f), PAGE_WIDTH - MARGIN, Math.min(y - 2f, PAGE_HEIGHT - 58f), .55f);
    }

    private static void drawPartyAndMetadata(PdfContentByte canvas, Data data, Fonts fonts) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setTotalWidth(CONTENT_WIDTH);
        table.setLockedWidth(true);
        table.setWidths(new float[] { 66f, 34f });

        PdfPCell party = new PdfPCell();
        party.setBorder(Rectangle.NO_BORDER);
        party.setPadding(0f);
        party.setPaddingTop(6f);
        for (String line : cleanLines(data.partyLines())) {
            Paragraph paragraph = new Paragraph(line, fonts.data());
            paragraph.setLeading(7.2f);
            party.addElement(paragraph);
        }
        table.addCell(party);

        PdfPCell meta = new PdfPCell();
        meta.setBorder(Rectangle.NO_BORDER);
        meta.setPadding(0f);
        meta.setPaddingTop(6f);
        meta.addElement(metaLine(data.invoice() ? "INVOICE NO." : "RECEIPT NO.", data.documentNo(), fonts));
        meta.addElement(metaLine("DATE", formatDate(data.documentDate()), fonts));
        table.addCell(meta);

        table.writeSelectedRows(0, -1, MARGIN, PAGE_HEIGHT - 69f, canvas);
    }

    private static Paragraph metaLine(String label, String value, Fonts fonts) {
        Paragraph line = new Paragraph();
        line.setLeading(8.5f);
        line.add(new Phrase(label, fonts.boldSmall()));
        line.add(new Phrase("   :   ", fonts.small()));
        line.add(new Phrase(text(value), fonts.data()));
        return line;
    }

    private static void drawItems(PdfContentByte canvas, Data data, Fonts fonts) throws Exception {
        PdfPTable table = new PdfPTable(3);
        table.setTotalWidth(CONTENT_WIDTH);
        table.setLockedWidth(true);
        table.setWidths(new float[] { 9f, 69f, 22f });

        table.addCell(headerCell("NO.", fonts, Element.ALIGN_LEFT));
        table.addCell(headerCell("DESCRIPTION", fonts, Element.ALIGN_LEFT));
        table.addCell(headerCell("AMOUNT\n(" + currency(data.currency()) + ")", fonts, Element.ALIGN_RIGHT));

        PdfPCell number = bodyCell("1", fonts.data(), Element.ALIGN_CENTER);
        number.setMinimumHeight(81f);
        table.addCell(number);

        PdfPCell description = new PdfPCell();
        description.setBorder(Rectangle.NO_BORDER);
        description.setPadding(6f);
        description.setPaddingLeft(1f);
        description.setMinimumHeight(81f);
        Paragraph main = new Paragraph(text(data.description()), fonts.data());
        main.setLeading(8f);
        main.setSpacingAfter(5f);
        description.addElement(main);
        for (Detail detail : data.details() == null ? List.<Detail>of() : data.details()) {
            if (detail == null || !hasText(detail.value())) continue;
            Paragraph line = new Paragraph();
            line.setLeading(7.2f);
            line.add(new Phrase(detail.label().toUpperCase(Locale.ROOT) + "   :   ", fonts.boldTiny()));
            line.add(new Phrase(detail.value(), fonts.tinyUnicode()));
            description.addElement(line);
        }
        table.addCell(description);

        PdfPCell amount = bodyCell(money(data.amount()), fonts.data(), Element.ALIGN_RIGHT);
        amount.setMinimumHeight(81f);
        table.addCell(amount);

        table.writeSelectedRows(0, -1, MARGIN, PAGE_HEIGHT - 141f, canvas);
    }

    private static PdfPCell headerCell(String value, Fonts fonts, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value, fonts.boldSmall()));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(3.5f);
        cell.setPaddingBottom(3.5f);
        cell.setPaddingLeft(alignment == Element.ALIGN_LEFT ? 0f : 2f);
        cell.setPaddingRight(alignment == Element.ALIGN_RIGHT ? 0f : 2f);
        cell.setBorder(Rectangle.TOP | Rectangle.BOTTOM);
        cell.setBorderWidthTop(.55f);
        cell.setBorderWidthBottom(.55f);
        return cell;
    }

    private static PdfPCell bodyCell(String value, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text(value), font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPaddingTop(7f);
        cell.setPaddingLeft(1f);
        cell.setPaddingRight(1f);
        return cell;
    }

    private static void drawFooter(PdfContentByte canvas, Data data, Fonts fonts) throws Exception {
        line(canvas, MARGIN, 155f, PAGE_WIDTH - MARGIN, 155f, .4f);
        showText(canvas, "RINGGIT MALAYSIA", fonts.boldTiny(), MARGIN, 143f, Element.ALIGN_LEFT);
        showText(canvas, ":", fonts.tiny(), MARGIN + 164f, 143f, Element.ALIGN_LEFT);
        showText(canvas, amountInWords(data.amount()), fonts.boldTiny(), MARGIN + 180f, 143f, Element.ALIGN_LEFT);

        float noteY = 127f;
        for (String note : cleanLines(data.notes())) {
            showText(canvas, note, fonts.tinyUnicode(), MARGIN, noteY, Element.ALIGN_LEFT);
            noteY -= 6f;
            if (noteY < 96f) break;
        }

        PdfPTable totals = new PdfPTable(2);
        totals.setTotalWidth(CONTENT_WIDTH);
        totals.setLockedWidth(true);
        totals.setWidths(new float[] { 77f, 23f });
        if (data.invoice()) {
            totals.addCell(totalCell("TOTAL (" + currency(data.currency()) + ")", fonts.small(), Element.ALIGN_LEFT, false));
            totals.addCell(totalCell(money(data.amount()), fonts.boldSmall(), Element.ALIGN_RIGHT, true));
            totals.addCell(totalCell("SERVICE TAX", fonts.small(), Element.ALIGN_LEFT, false));
            totals.addCell(totalCell("0.00", fonts.small(), Element.ALIGN_RIGHT, false));
            totals.addCell(totalCell("TOTAL AMOUNT PAYABLE (" + currency(data.currency()) + ")", fonts.boldSmall(), Element.ALIGN_LEFT, false));
            totals.addCell(totalCell(money(data.amount()), fonts.boldSmall(), Element.ALIGN_RIGHT, true));
        } else {
            totals.addCell(totalCell("TOTAL AMOUNT RECEIVED (" + currency(data.currency()) + ")", fonts.boldSmall(), Element.ALIGN_LEFT, false));
            totals.addCell(totalCell(money(data.amount()), fonts.boldSmall(), Element.ALIGN_RIGHT, true));
        }
        totals.writeSelectedRows(0, -1, MARGIN, data.invoice() ? 91f : 68f, canvas);

        line(canvas, MARGIN, 29f, PAGE_WIDTH - MARGIN, 29f, .4f);
        showText(canvas, DISCLAIMER, fonts.tiny(), MARGIN, 18f, Element.ALIGN_LEFT);
    }

    private static PdfPCell totalCell(String value, Font font, int alignment, boolean ruled) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPaddingTop(2.5f);
        cell.setPaddingBottom(2.5f);
        cell.setPaddingLeft(0f);
        cell.setPaddingRight(0f);
        cell.setBorder(ruled ? Rectangle.TOP | Rectangle.BOTTOM : Rectangle.NO_BORDER);
        cell.setBorderWidthTop(.45f);
        cell.setBorderWidthBottom(.45f);
        return cell;
    }

    private static void showText(PdfContentByte canvas, String value, Font font, float x, float y, int alignment) {
        com.lowagie.text.pdf.ColumnText.showTextAligned(canvas, alignment,
                new Phrase(text(value), font), x, y, 0f);
    }

    private static void line(PdfContentByte canvas, float x1, float y1, float x2, float y2, float width) {
        canvas.saveState();
        canvas.setLineWidth(width);
        canvas.moveTo(x1, y1);
        canvas.lineTo(x2, y2);
        canvas.stroke();
        canvas.restoreState();
    }

    static String formatDate(LocalDate value) {
        return value == null ? "-" : DATE.format(value);
    }

    static String billingPeriod(LocalDate billingMonth) {
        if (billingMonth == null) return "-";
        LocalDate start = billingMonth.withDayOfMonth(1);
        return formatDate(start) + " TO " + formatDate(start.withDayOfMonth(start.lengthOfMonth()));
    }

    static String amountInWords(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        boolean negative = amount.signum() < 0;
        amount = amount.abs();
        BigInteger ringgit = amount.toBigInteger();
        int sen = amount.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        StringBuilder words = new StringBuilder();
        if (negative) words.append("MINUS ");
        words.append(integerWords(ringgit));
        if (sen > 0) words.append(" AND ").append(integerWords(BigInteger.valueOf(sen))).append(" SEN");
        return words.append(" ONLY").toString();
    }

    private static String integerWords(BigInteger value) {
        if (value.signum() == 0) return "ZERO";
        String[] scales = { "", "THOUSAND", "MILLION", "BILLION", "TRILLION", "QUADRILLION" };
        List<String> groups = new ArrayList<>();
        BigInteger thousand = BigInteger.valueOf(1000);
        int scale = 0;
        while (value.signum() > 0) {
            int group = value.mod(thousand).intValue();
            if (group > 0) {
                String words = underThousand(group);
                if (scale < scales.length && !scales[scale].isEmpty()) words += " " + scales[scale];
                groups.add(0, words);
            }
            value = value.divide(thousand);
            scale++;
        }
        return String.join(" ", groups);
    }

    private static String underThousand(int value) {
        String[] belowTwenty = { "", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX", "SEVEN", "EIGHT", "NINE",
                "TEN", "ELEVEN", "TWELVE", "THIRTEEN", "FOURTEEN", "FIFTEEN", "SIXTEEN", "SEVENTEEN", "EIGHTEEN", "NINETEEN" };
        String[] tens = { "", "", "TWENTY", "THIRTY", "FORTY", "FIFTY", "SIXTY", "SEVENTY", "EIGHTY", "NINETY" };
        List<String> parts = new ArrayList<>();
        if (value >= 100) {
            parts.add(belowTwenty[value / 100] + " HUNDRED");
            value %= 100;
        }
        if (value >= 20) {
            parts.add(tens[value / 10]);
            value %= 10;
        }
        if (value > 0) parts.add(belowTwenty[value]);
        return String.join(" ", parts);
    }

    private static List<String> cleanLines(List<String> values) {
        if (values == null) return List.of();
        return values.stream().filter(FinanceDocumentPdfRenderer::hasText).map(String::trim).toList();
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }
    private static String text(String value) { return hasText(value) ? value.trim() : "-"; }
    private static String currency(String value) { return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "MYR"; }
    private static String money(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString(); }

    record Detail(String label, String value) { }

    record Data(
            boolean invoice,
            String companyName,
            String companyRegistration,
            List<String> companyDetails,
            List<String> partyLines,
            String documentNo,
            LocalDate documentDate,
            String description,
            List<Detail> details,
            BigDecimal amount,
            String currency,
            List<String> notes) {
        Data {
            companyDetails = companyDetails == null ? List.of() : List.copyOf(companyDetails);
            partyLines = partyLines == null ? List.of() : List.copyOf(partyLines);
            details = details == null ? List.of() : List.copyOf(details);
            notes = notes == null ? List.of() : List.copyOf(notes);
        }
    }

    private record Fonts(Font company, Font small, Font boldSmall, Font data, Font dataUnicode,
            Font boldTiny, Font tiny, Font tinyUnicode) { }
}
