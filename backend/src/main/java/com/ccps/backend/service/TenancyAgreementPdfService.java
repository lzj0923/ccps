package com.ccps.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

import org.springframework.stereotype.Service;

/**
 * Fills the customer supplied 22-page tenancy agreement PDF. The source PDF is
 * copied and overlaid; its wording, photos, page order and page size are kept.
 */
@Service
public class TenancyAgreementPdfService {
    public static final String TEMPLATE_RESOURCE = "/contract-templates/conlay-tenancy-agreement-template.pdf";
    public static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final float PAGE_WIDTH = 595.32f;
    private static final float PAGE_HEIGHT = 841.92f;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH);
    private static final List<String> INVENTORY_CATEGORY_ORDER = List.of(
            "客廳 Living Room", "飯廳 Dining Room", "廚房 Kitchen", "主臥室 Master Bedroom",
            "主浴室 Master Bathroom", "遙控器 Remote Control", "鑰匙 Keys", "門禁卡 Access Card");

    private static final List<PhotoSlot> PHOTO_SLOTS = List.of(
            new PhotoSlot(20, 18.4f, 545.5f, 260f, 195f),
            new PhotoSlot(20, 304.2f, 546.1f, 261.6f, 196.2f),
            new PhotoSlot(20, 11.4f, 315.56f, 263.4f, 197.55f),
            new PhotoSlot(20, 308.4f, 316.36f, 259.2f, 194.4f),
            new PhotoSlot(20, 15.6f, 90.095f, 261.6f, 196.2f),
            new PhotoSlot(20, 308.4f, 90.183f, 259.8f, 194.85f),
            new PhotoSlot(21, 12f, 535.9f, 279f, 209.2f),
            new PhotoSlot(21, 307.8f, 537.6f, 276.6f, 207.4f),
            new PhotoSlot(21, 11.4f, 306.2f, 279f, 209.2f),
            new PhotoSlot(21, 306f, 307f, 278.4f, 208.8f),
            new PhotoSlot(21, 10.8f, 79f, 279.6f, 209.7f),
            new PhotoSlot(21, 306.8f, 77.5f, 277.6f, 208.2f),
            new PhotoSlot(22, 12.6f, 533.6f, 282f, 211.5f));

    public byte[] generate(Map<String, String> fields) {
        return generate(fields, List.of(), null);
    }

    public byte[] generate(Map<String, String> fields, List<PropertyPhotoAsset> propertyPhotos) {
        return generate(fields, propertyPhotos, null);
    }

    /**
     * Generates the customer supplied agreement while replacing its Inventory
     * List with the enabled checklist items belonging to the lease's property.
     * A null inventory list keeps the original template inventory for backwards
     * compatibility; an empty list intentionally produces an empty inventory.
     */
    public byte[] generate(Map<String, String> fields, List<PropertyPhotoAsset> propertyPhotos,
            List<InventoryItem> inventoryItems) {
        Map<String, String> values = fields == null ? Map.of() : fields;
        try (InputStream source = TenancyAgreementPdfService.class.getResourceAsStream(TEMPLATE_RESOURCE);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (source == null) throw new IllegalStateException("Tenancy agreement PDF template is missing");
            PdfReader reader = new PdfReader(source);
            PdfStamper stamper = new PdfStamper(reader, output);
            BaseFont latin = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            BaseFont cjk = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            fillCover(stamper.getOverContent(1), latin, cjk, values);
            for (int page = 2; page <= reader.getNumberOfPages(); page++) {
                fillHeader(stamper.getOverContent(page), latin, cjk, values);
            }
            fillSignaturePage(stamper.getOverContent(12), latin, cjk, values);
            fillFirstSchedule(stamper.getOverContent(13), latin, cjk, values);
            fillSecondSchedule(stamper.getOverContent(14), latin, cjk, values);
            if (inventoryItems != null) {
                replaceInventory(stamper, latin, cjk, inventoryItems);
                // Inventory clearing reaches up to the original page header
                // on continuation pages; paint that header back afterward.
                for (int page = 15; page <= 17; page++) {
                    fillHeader(stamper.getOverContent(page), latin, cjk, values);
                }
            }
            replacePropertyPhotos(stamper, propertyPhotos == null ? List.of() : propertyPhotos);
            stamper.close();
            reader.close();
            return output.toByteArray();
        } catch (IOException | DocumentException exception) {
            throw new IllegalStateException("Unable to generate tenancy agreement PDF", exception);
        }
    }

    private void replaceInventory(PdfStamper stamper, BaseFont latin, BaseFont cjk,
            List<InventoryItem> inventoryItems) throws IOException {
        // Keep the agreement's original heading, footer, page numbers and
        // margins. Only the table area is replaced; the table itself follows
        // the supplied PDF: section title, five columns, two-line bilingual
        // header and boxed rows.
        // Page 15 has the original "Inventory List" heading above the first
        // table; start the white-out below that heading so it remains exact.
        cover(stamper.getOverContent(15), 60, 42, 485, 618);
        cover(stamper.getOverContent(16), 60, 42, 485, 706);
        cover(stamper.getOverContent(17), 60, 42, 485, 706);

        Map<String, List<InventoryItem>> grouped = new LinkedHashMap<>();
        for (String category : INVENTORY_CATEGORY_ORDER) grouped.put(category, new ArrayList<>());
        for (InventoryItem item : inventoryItems) {
            if (item == null || item.category() == null || item.itemName() == null || item.itemName().isBlank()) continue;
            grouped.computeIfAbsent(item.category().trim(), ignored -> new ArrayList<>()).add(item);
        }
        grouped.values().removeIf(List::isEmpty);
        if (grouped.isEmpty()) return;

        int totalRows = grouped.values().stream().mapToInt(List::size).sum();
        float rowHeight = totalRows > 60 ? 12.5f : 18.84f;
        float fontSize = totalRows > 60 ? 6.4f : 8.1f;
        int page = 15;
        float y = 585;
        for (Map.Entry<String, List<InventoryItem>> group : grouped.entrySet()) {
            int start = 0;
            while (start < group.getValue().size()) {
                boolean showTitle = start == 0;
                float titleHeight = showTitle ? 18 : 0;
                int capacity = (int) Math.floor((y - 58 - titleHeight - 32) / rowHeight);
                if (capacity < 1) {
                    page++;
                    y = 650;
                    continue;
                }
                int end = Math.min(group.getValue().size(), start + capacity);
                PdfContentByte canvas = stamper.getOverContent(page);
                drawInventoryTable(canvas, latin, cjk, group.getKey(), group.getValue().subList(start, end),
                        y, rowHeight, fontSize, showTitle);
                y -= titleHeight + 32 + rowHeight * (end - start) + 18;
                start = end;
                if (start < group.getValue().size() && y < 90) {
                    page++;
                    y = 650;
                }
                if (page > 17) throw new IllegalStateException("Property handover checklist exceeds tenancy agreement inventory pages");
            }
        }
    }

    private void drawInventoryTable(PdfContentByte canvas, BaseFont latin, BaseFont cjk, String category,
            List<InventoryItem> items, float titleY, float rowHeight, float fontSize, boolean showTitle) throws IOException {
        final float left = 72;
        final float[] columns = { 72, 103, 359, 410, 471, 532 };
        final float tableTop = showTitle ? titleY - 14 : titleY;
        final float headerRowHeight = 16;
        final float tableBottom = tableTop - headerRowHeight * 2 - rowHeight * items.size();

        if (showTitle) text(canvas, latin, cjk, category + " :", left + 5, titleY, 9.2f, 450);
        canvas.saveState();
        canvas.setColorStroke(Color.BLACK);
        canvas.setLineWidth(.55f);
        canvas.rectangle(left, tableBottom, columns[5] - left, tableTop - tableBottom);
        for (int i = 1; i < columns.length - 1; i++) {
            canvas.moveTo(columns[i], tableBottom);
            canvas.lineTo(columns[i], tableTop);
        }
        canvas.moveTo(left, tableTop - headerRowHeight);
        canvas.lineTo(columns[5], tableTop - headerRowHeight);
        canvas.moveTo(left, tableTop - headerRowHeight * 2);
        canvas.lineTo(columns[5], tableTop - headerRowHeight * 2);
        for (int row = 1; row <= items.size(); row++) {
            float lineY = tableTop - headerRowHeight * 2 - rowHeight * row;
            canvas.moveTo(left, lineY);
            canvas.lineTo(columns[5], lineY);
        }
        canvas.stroke();
        canvas.restoreState();

        text(canvas, latin, cjk, "No.", 79, tableTop - 11, 8.1f, 22);
        text(canvas, latin, cjk, "交接清單", 108, tableTop - 11, 8.1f, 245);
        text(canvas, latin, cjk, "數量", 373, tableTop - 11, 8.1f, 44);
        text(canvas, latin, cjk, "入住", 429, tableTop - 11, 8.1f, 44);
        text(canvas, latin, cjk, "退租", 490, tableTop - 11, 8.1f, 44);
        text(canvas, latin, cjk, "Handover List", 108, tableTop - 27, 8.1f, 245);
        text(canvas, latin, cjk, "Quantity", 364, tableTop - 27, 8.1f, 55);
        text(canvas, latin, cjk, "Check In", 420, tableTop - 27, 8.1f, 50);
        text(canvas, latin, cjk, "Check Out", 478, tableTop - 27, 8.1f, 52);

        float baseline = tableTop - headerRowHeight * 2 - rowHeight + Math.min(12, rowHeight - 3);
        int number = 1;
        for (InventoryItem item : items) {
            text(canvas, latin, cjk, Integer.toString(number++), 82, baseline, fontSize, 17);
            text(canvas, latin, cjk, item.itemName(), 108, baseline, fontSize, 245);
            text(canvas, latin, cjk, item.quantity() == null ? "" : item.quantity(), 375, baseline, fontSize, 42);
            baseline -= rowHeight;
        }
    }

    private void replacePropertyPhotos(PdfStamper stamper, List<PropertyPhotoAsset> propertyPhotos)
            throws IOException, DocumentException {
        List<PropertyPhotoAsset> ordered = propertyPhotos.stream()
                .filter(photo -> photo != null && photo.path() != null && Files.isRegularFile(photo.path()))
                .sorted((left, right) -> {
                    int cover = Boolean.compare(right.coverFlag(), left.coverFlag());
                    return cover != 0 ? cover : Integer.compare(left.sortOrder(), right.sortOrder());
                })
                .limit(PHOTO_SLOTS.size())
                .toList();
        for (int index = 0; index < PHOTO_SLOTS.size(); index++) {
            PhotoSlot slot = PHOTO_SLOTS.get(index);
            PdfContentByte canvas = stamper.getOverContent(slot.page());
            cover(canvas, slot.x(), slot.y(), slot.width(), slot.height());
            if (index >= ordered.size()) continue;
            Image image = Image.getInstance(cropToSlot(ordered.get(index).path(), slot.width(), slot.height()));
            image.setAbsolutePosition(slot.x(), slot.y());
            image.scaleAbsolute(slot.width(), slot.height());
            canvas.addImage(image);
        }
    }

    private byte[] cropToSlot(Path path, float targetWidth, float targetHeight) throws IOException {
        java.awt.image.BufferedImage source = javax.imageio.ImageIO.read(path.toFile());
        if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) throw new IOException("Invalid property photo");
        double sourceRatio = (double) source.getWidth() / source.getHeight();
        double targetRatio = targetWidth / targetHeight;
        int cropWidth = source.getWidth();
        int cropHeight = source.getHeight();
        if (sourceRatio > targetRatio) cropWidth = (int) Math.round(source.getHeight() * targetRatio);
        else if (sourceRatio < targetRatio) cropHeight = (int) Math.round(source.getWidth() / targetRatio);
        int cropX = (source.getWidth() - cropWidth) / 2;
        int cropY = (source.getHeight() - cropHeight) / 2;
        java.awt.image.BufferedImage cropped = new java.awt.image.BufferedImage(800, 600,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = cropped.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(source, 0, 0, cropped.getWidth(), cropped.getHeight(), cropX, cropY,
                cropX + cropWidth, cropY + cropHeight, Color.WHITE, null);
        graphics.dispose();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(cropped, "jpg", output);
            return output.toByteArray();
        }
    }

    public String fileName(Map<String, String> fields) {
        String base = java.util.stream.Stream.of(firstNotBlank(value(fields, "tenantName"), value(fields, "landlordName")), value(fields, "projectName"),
                        value(fields, "unitNo"), "租赁合同", value(fields, "caseNo"))
                .map(this::safeFilePart).filter(part -> !part.isBlank())
                .reduce((left, right) -> left + "-" + right).orElse("租赁合同");
        return base + ".pdf";
    }

    private String firstNotBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private String safeFilePart(String value) {
        if (value == null) return "";
        String safe = value.trim().replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "-").replaceAll("\\s+", " ");
        return safe.length() > 60 ? safe.substring(0, 60) : safe;
    }

    private void fillCover(PdfContentByte canvas, BaseFont latin, BaseFont cjk, Map<String, String> fields)
            throws IOException {
        cover(canvas, 220, 665, 280, 30);
        text(canvas, latin, cjk, "Dated this " + date(fields, "agreementDate"), 235, 680, 9, 250);
        cover(canvas, 220, 590, 180, 20);
        text(canvas, latin, cjk, value(fields, "landlordName"), 248, 600, 9, 170);
        cover(canvas, 220, 577, 180, 20);
        text(canvas, latin, cjk, value(fields, "landlordIdentity"), 306, 587, 9, 120);
        cover(canvas, 220, 494, 190, 20);
        text(canvas, latin, cjk, value(fields, "tenantName"), 235, 504, 9, 180);
        cover(canvas, 220, 481, 190, 20);
        text(canvas, latin, cjk, value(fields, "tenantIdentity"), 306, 491, 9, 120);
        cover(canvas, 105, 121, 420, 22);
        text(canvas, latin, cjk, value(fields, "propertyAddress"), 117, 131, 9, 400);
    }

    private void fillHeader(PdfContentByte canvas, BaseFont latin, BaseFont cjk, Map<String, String> fields)
            throws IOException {
        cover(canvas, 125, 774, 455, 39);
        text(canvas, latin, cjk, "Tenancy Agreement between " + value(fields, "landlordName"), 136, 797, 9, 385);
        text(canvas, latin, cjk, "AND " + value(fields, "tenantName") + " (" + date(fields, "leaseStart") + " - "
                + date(fields, "leaseEnd") + ")", 136, 784, 9, 390);
        text(canvas, latin, cjk, year(fields, "leaseStart"), 531, 797, 9, 42);
    }

    private void fillFirstSchedule(PdfContentByte canvas, BaseFont latin, BaseFont cjk, Map<String, String> fields)
            throws IOException {
        cover(canvas, 245, 621, 284, 38);
        text(canvas, latin, cjk, date(fields, "agreementDate"), 248, 640, 9, 270);

        cover(canvas, 245, 525, 284, 92);
        drawLines(canvas, latin, cjk, List.of(
                "Name: " + value(fields, "landlordName"),
                "Passport No.: " + value(fields, "landlordIdentity"),
                "Address: " + value(fields, "landlordAddress")), 248, 604, 8.6f, 265, 12);

        cover(canvas, 245, 386, 284, 136);
        drawLines(canvas, latin, cjk, List.of(
                "Name: " + value(fields, "tenantName"),
                "Passport No.: " + value(fields, "tenantIdentity"),
                "Contact No.: " + value(fields, "tenantPhone"),
                "Address: " + value(fields, "tenantAddress"),
                "Email Address: " + value(fields, "tenantEmail")), 248, 509, 8.6f, 265, 12);

        cover(canvas, 245, 328, 284, 52);
        drawLines(canvas, latin, cjk, wrap(value(fields, "propertyAddress"), 56), 248, 365, 8.6f, 265, 12);

        cover(canvas, 245, 179, 284, 146);
        drawLines(canvas, latin, cjk, List.of(
                value(fields, "termYears"),
                date(fields, "leaseStart"),
                date(fields, "leaseEnd")), 248, 300, 8.6f, 265, 48);
    }

    private void fillSignaturePage(PdfContentByte canvas, BaseFont latin, BaseFont cjk, Map<String, String> fields)
            throws IOException {
        cover(canvas, 68, 650, 175, 28);
        text(canvas, latin, cjk, value(fields, "landlordName"), 72, 670, 9, 170);
        text(canvas, latin, cjk, "Passport No. " + value(fields, "landlordIdentity"), 72, 656, 9, 170);
        cover(canvas, 68, 422, 175, 28);
        text(canvas, latin, cjk, value(fields, "tenantName"), 72, 442, 9, 170);
        text(canvas, latin, cjk, "Passport No. " + value(fields, "tenantIdentity"), 72, 428, 9, 170);
    }

    private void fillSecondSchedule(PdfContentByte canvas, BaseFont latin, BaseFont cjk, Map<String, String> fields)
            throws IOException {
        cover(canvas, 245, 568, 267, 174);
        List<String> payment = new ArrayList<>();
        payment.add(amountPhrase(value(fields, "monthlyRent")));
        payment.add(value(fields, "paymentDay").isBlank() ? "" : ordinal(value(fields, "paymentDay")) + " day of every month");
        payment.add("Bank Transfer:");
        if (!value(fields, "bankName").isBlank()) payment.add(value(fields, "bankName"));
        if (!value(fields, "bankAccount").isBlank()) payment.add("Account No: " + value(fields, "bankAccount"));
        if (!value(fields, "bankBranch").isBlank()) payment.add("Bank: " + value(fields, "bankBranch"));
        drawLines(canvas, latin, cjk, payment, 248, 723, 8.6f, 255, 12);

        cover(canvas, 245, 520, 267, 44);
        text(canvas, latin, cjk, amountPhrase(value(fields, "advanceRental")), 248, 548, 8.6f, 255);
        cover(canvas, 245, 439, 267, 76);
        drawLines(canvas, latin, cjk, wrap(amountPhrase(value(fields, "securityDeposit")), 54), 248, 495, 8.6f, 255, 12);
        cover(canvas, 245, 370, 267, 64);
        drawLines(canvas, latin, cjk, wrap(amountPhrase(value(fields, "utilityDeposit")), 54), 248, 430, 8.6f, 255, 12);
        cover(canvas, 245, 315, 267, 51);
        text(canvas, latin, cjk, value(fields, "use"), 248, 350, 8.6f, 255);
        cover(canvas, 245, 232, 267, 77);
        drawLines(canvas, latin, cjk, wrap(value(fields, "renewalOption"), 54), 248, 295, 8.6f, 255, 12);
        cover(canvas, 245, 163, 267, 64);
        drawLines(canvas, latin, cjk, wrap(value(fields, "specialConditions"), 54), 248, 215, 8.2f, 255, 12);
    }

    private void drawLines(PdfContentByte canvas, BaseFont latin, BaseFont cjk, List<String> lines, float x,
            float y, float size, float maxWidth, float lineGap) throws IOException {
        float current = y;
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                current -= lineGap;
                continue;
            }
            for (String wrapped : wrap(line, 58)) {
                text(canvas, latin, cjk, wrapped, x, current, size, maxWidth);
                current -= lineGap;
            }
        }
    }

    private void cover(PdfContentByte canvas, float x, float y, float width, float height) {
        canvas.saveState();
        canvas.setColorFill(Color.WHITE);
        canvas.rectangle(x, y, width, height);
        canvas.fill();
        canvas.restoreState();
    }

    private void text(PdfContentByte canvas, BaseFont latin, BaseFont cjk, String value, float x, float y,
            float size, float maxWidth) throws IOException {
        if (value == null || value.isBlank()) return;
        String safe = value.replace('\n', ' ').replace('\r', ' ').trim();
        BaseFont font = safe.chars().allMatch(ch -> ch < 128) ? latin : cjk;
        float actual = size;
        while (actual > 6.2f && font.getWidthPoint(safe, actual) > maxWidth) actual -= .3f;
        canvas.beginText();
        canvas.setFontAndSize(font, actual);
        canvas.setTextMatrix(x, y);
        canvas.showText(safe);
        canvas.endText();
    }

    private List<String> wrap(String value, int maxChars) {
        if (value == null || value.isBlank()) return List.of("");
        List<String> result = new ArrayList<>();
        String remaining = value.trim();
        while (remaining.length() > maxChars) {
            int cut = remaining.lastIndexOf(' ', maxChars);
            if (cut < 1) cut = maxChars;
            result.add(remaining.substring(0, cut).trim());
            remaining = remaining.substring(cut).trim();
        }
        result.add(remaining);
        return result;
    }

    private String value(Map<String, String> fields, String key) {
        if (fields == null) return "";
        String value = fields.get(key);
        return value == null ? "" : value.trim();
    }

    private String date(Map<String, String> fields, String key) {
        String value = value(fields, key);
        if (value.isBlank()) return "";
        try {
            return LocalDate.parse(value).format(DATE);
        } catch (RuntimeException ignored) {
            return value;
        }
    }

    private String year(Map<String, String> fields, String key) {
        String value = value(fields, key);
        return value.length() >= 4 ? value.substring(0, 4) : value;
    }

    private String ordinal(String value) {
        try {
            int day = Integer.parseInt(value);
            int mod100 = day % 100;
            String suffix = mod100 >= 11 && mod100 <= 13 ? "th" : switch (day % 10) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
            return day + suffix;
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private String amountPhrase(String value) {
        if (value == null || value.isBlank()) return "";
        String numeric = value.replaceAll("[^0-9.]", "");
        try {
            BigDecimal amount = new BigDecimal(numeric).setScale(2, RoundingMode.HALF_UP);
            long ringgit = amount.longValue();
            int cents = amount.remainder(BigDecimal.ONE).movePointRight(2).abs().intValue();
            String words = titleCase(numberToWords(ringgit));
            String centsText = cents == 0 ? "" : " and Sen " + String.format("%02d", cents);
            DecimalFormat format = new DecimalFormat("#,##0.00");
            return "Ringgit Malaysia " + words + centsText + " (RM " + format.format(amount) + ") Only";
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private String titleCase(String value) {
        if (value.isBlank()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String numberToWords(long number) {
        if (number == 0) return "zero";
        if (number < 1000) return belowThousand(number);
        if (number < 1_000_000) return belowThousand(number / 1000) + " thousand" + remainder(number, 1000);
        return belowThousand(number / 1_000_000) + " million" + remainder(number, 1_000_000);
    }

    private String remainder(long number, long divisor) {
        long rest = number % divisor;
        return rest == 0 ? "" : " " + numberToWords(rest);
    }

    private String belowThousand(long number) {
        String[] ones = { "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
                "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen" };
        String[] tens = { "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety" };
        if (number < 20) return ones[(int) number];
        if (number < 100) return tens[(int) (number / 10)] + (number % 10 == 0 ? "" : "-" + ones[(int) (number % 10)]);
        return ones[(int) (number / 100)] + " hundred" + (number % 100 == 0 ? "" : " " + belowThousand(number % 100));
    }

    public record PropertyPhotoAsset(Path path, String mimeType, int sortOrder, boolean coverFlag) { }
    public record InventoryItem(String category, String itemName, String quantity) { }
    private record PhotoSlot(int page, float x, float y, float width, float height) { }
}
