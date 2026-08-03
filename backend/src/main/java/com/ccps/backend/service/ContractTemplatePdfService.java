package com.ccps.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

import org.springframework.stereotype.Service;

/**
 * Fills the two customer supplied PDF forms. The source pages are never
 * rebuilt: values are painted into the blank lines on top of the original
 * page so the wording, logo, spacing and page size remain unchanged.
 */
@Service
public class ContractTemplatePdfService {
    private static final String OTR_TEMPLATE = "/contract-templates/letter-offer-to-rent.pdf";
    private static final String AUTHORIZATION_TEMPLATE = "/contract-templates/letter-of-appointment-to-rent.pdf";

    public enum TemplateType { OTR, AUTHORIZATION }

    public record TemplateData(
            String caseNo,
            String propertyAddress,
            String landlord,
            String tenant,
            String tenancyYears,
            String commencementDate,
            String securityDepositMonths,
            String advanceRental,
            String utilityDepositMonths,
            String securityDeposit,
            String utilityDeposit,
            String stampingFee,
            String totalBeforeKeys,
            String periodYears,
            String startDate,
            String earnestDeposit,
            String landlordName,
            String landlordIdentity,
            String landlordDate,
            String tenantName,
            String tenantIdentity,
            String tenantDate,
            String commission,
            String otherConditions,
            List<String> unused) {
        public TemplateData {
            unused = unused == null ? List.of() : List.copyOf(unused);
        }
    }

    public byte[] generate(TemplateType type, TemplateData data) {
        Objects.requireNonNull(type, "template type");
        Objects.requireNonNull(data, "template data");
        String resource = type == TemplateType.OTR ? OTR_TEMPLATE : AUTHORIZATION_TEMPLATE;
        try (InputStream source = ContractTemplatePdfService.class.getResourceAsStream(resource);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (source == null) throw new IllegalStateException("Contract template is missing: " + resource);
            PdfReader reader = new PdfReader(source);
            PdfStamper stamper = new PdfStamper(reader, output);
            PdfContentByte canvas = stamper.getOverContent(1);
            BaseFont latin = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            BaseFont cjk = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            if (type == TemplateType.OTR) fillOtr(canvas, latin, cjk, data);
            else fillAuthorization(canvas, latin, cjk, data);
            stamper.close();
            reader.close();
            return output.toByteArray();
        } catch (IOException | DocumentException exception) {
            throw new IllegalStateException("Unable to generate contract template", exception);
        }
    }

    /** Converts the JSON field map used by the admin UI into the fixed template model. */
    public TemplateData data(Map<String, String> fields) {
        Map<String, String> values = fields == null ? Map.of() : fields;
        return new TemplateData(
                value(values, "caseNo"), value(values, "propertyAddress"), valueOrFallback(values, "landlord", "landlordName"),
                valueOrFallback(values, "tenant", "tenantName"), value(values, "tenancyYears"), value(values, "commencementDate"),
                value(values, "securityDepositMonths"), value(values, "advanceRental"),
                value(values, "utilityDepositMonths"), value(values, "securityDeposit"),
                value(values, "utilityDeposit"), value(values, "stampingFee"), value(values, "totalBeforeKeys"),
                value(values, "periodYears"), value(values, "startDate"), value(values, "earnestDeposit"),
                value(values, "landlordName"), value(values, "landlordIdentity"), value(values, "landlordDate"),
                value(values, "tenantName"), value(values, "tenantIdentity"), value(values, "tenantDate"),
                value(values, "commission"), value(values, "otherConditions"), List.of());
    }

    private String value(Map<String, String> values, String key) {
        String value = values.get(key);
        return value == null ? "" : value.trim();
    }

    private String valueOrFallback(Map<String, String> values, String primaryKey, String fallbackKey) {
        String primary = value(values, primaryKey);
        return primary.isBlank() ? value(values, fallbackKey) : primary;
    }

    private void fillOtr(PdfContentByte canvas, BaseFont latin, BaseFont cjk, TemplateData d) throws IOException {
        text(canvas, latin, cjk, d.caseNo(), 460, 730, 9, 80);
        text(canvas, latin, cjk, d.propertyAddress(), 145, 704, 9, 390);
        text(canvas, latin, cjk, d.landlord(), 120, 690, 9, 435);
        text(canvas, latin, cjk, d.tenant(), 120, 678, 9, 435);
        text(canvas, latin, cjk, amount(d.advanceRental()), 470, 640, 9, 90);
        text(canvas, latin, cjk, d.securityDepositMonths(), 220, 630, 9, 42);
        text(canvas, latin, cjk, amount(d.securityDeposit()), 470, 630, 9, 90);
        text(canvas, latin, cjk, d.utilityDepositMonths(), 350, 620, 9, 42);
        text(canvas, latin, cjk, amount(d.utilityDeposit()), 470, 620, 9, 90);
        text(canvas, latin, cjk, amount(d.stampingFee()), 470, 610, 9, 90);
        text(canvas, latin, cjk, amount(d.totalBeforeKeys()), 470, 600, 9, 90);
        text(canvas, latin, cjk, d.periodYears(), 180, 585, 9, 50);
        text(canvas, latin, cjk, d.commencementDate(), 185, 565, 9, 220);
        text(canvas, latin, cjk, amount(d.earnestDeposit()), 250, 442, 9, 125);
        text(canvas, latin, cjk, d.tenantName(), 115, 354, 9, 175);
        text(canvas, latin, cjk, d.tenantIdentity(), 115, 336, 9, 175);
        text(canvas, latin, cjk, d.tenantDate(), 115, 326, 9, 175);
        text(canvas, latin, cjk, d.landlordName(), 340, 354, 9, 175);
        text(canvas, latin, cjk, d.landlordIdentity(), 340, 336, 9, 175);
        text(canvas, latin, cjk, d.landlordDate(), 340, 326, 9, 175);
        text(canvas, latin, cjk, d.otherConditions(), 90, 105, 8, 430);
    }

    private void fillAuthorization(PdfContentByte canvas, BaseFont latin, BaseFont cjk, TemplateData d) throws IOException {
        text(canvas, latin, cjk, d.caseNo(), 455, 716, 9, 100);
        text(canvas, latin, cjk, d.propertyAddress(), 75, 651, 9, 470);
        text(canvas, latin, cjk, amount(d.earnestDeposit()), 470, 594, 9, 95);
        text(canvas, latin, cjk, d.commission(), 418, 557, 9, 45);
        text(canvas, latin, cjk, d.securityDepositMonths(), 200, 547, 9, 28);
        text(canvas, latin, cjk, amount(d.utilityDepositMonths()), 232, 547, 9, 55);
        text(canvas, latin, cjk, amount(d.totalBeforeKeys()), 470, 547, 9, 85);
        text(canvas, latin, cjk, d.startDate(), 316, 364, 9, 75);
        text(canvas, latin, cjk, d.commencementDate(), 404, 364, 9, 75);
        text(canvas, latin, cjk, d.landlordName(), 110, 243, 9, 145);
        text(canvas, latin, cjk, d.landlordIdentity(), 110, 234, 9, 145);
        text(canvas, latin, cjk, d.propertyAddress(), 110, 225, 8, 145);
        text(canvas, latin, cjk, d.landlordDate(), 110, 216, 9, 145);
        text(canvas, latin, cjk, d.tenantName(), 260, 243, 9, 145);
        text(canvas, latin, cjk, d.tenantIdentity(), 260, 234, 9, 145);
        text(canvas, latin, cjk, d.tenantDate(), 260, 216, 9, 145);
    }

    private void text(PdfContentByte canvas, BaseFont latin, BaseFont cjk, String value, float x, float y,
            float size, float maxWidth) throws IOException {
        if (value == null || value.isBlank()) return;
        String safe = value.replace('\n', ' ').replace('\r', ' ').trim();
        BaseFont font = safe.chars().allMatch(ch -> ch < 128) ? latin : cjk;
        float actualSize = size;
        while (actualSize > 6.2f && font.getWidthPoint(safe, actualSize) > maxWidth) actualSize -= .4f;
        canvas.beginText();
        canvas.setFontAndSize(font, actualSize);
        canvas.setTextMatrix(x, y);
        canvas.showText(safe);
        canvas.endText();
    }

    private String amount(String value) {
        if (value == null) return "";
        return value.trim().replaceFirst("^(?i)(RM|MYR)\\s*", "");
    }
}
