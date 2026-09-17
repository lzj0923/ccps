package com.ccps.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.LinkedHashMap;
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
    private static final String OTR_TEMPLATE = "/contract-templates/flattened/letter-offer-to-rent.pdf";
    private static final String AUTHORIZATION_TEMPLATE = "/contract-templates/flattened/letter-of-appointment-to-rent.pdf";

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
            String renewalYears,
            String startDate,
            String endDate,
            String earnestDeposit,
            String landlordName,
            String landlordIdentity,
            String landlordDate,
            String tenantName,
            String tenantIdentity,
            String tenantDate,
            String tenantWitnessName,
            String tenantWitnessIdentity,
            String tenantWitnessDate,
            String landlordWitnessName,
            String landlordWitnessIdentity,
            String landlordWitnessDate,
            String commissionWords,
            String earnestDepositWords,
            String commissionMonths,
            String sstPercent,
            String commissionAmount,
            String agencyFeeTotal,
            String landlordAddress,
            String secondLandlordName,
            String secondLandlordIdentity,
            String secondLandlordAddress,
            String secondLandlordDate,
            String witnessName,
            String witnessIdentity,
            String witnessAddress,
            String witnessDate,
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
            BaseFont latin = PdfFontResources.regular();
            BaseFont cjk = latin;
            if (type == TemplateType.OTR) {
                fillOtr(canvas, latin, cjk, data);
                if (reader.getNumberOfPages() >= 2) {
                    fillAuthorization(stamper.getOverContent(2), latin, cjk, data);
                }
            } else fillAuthorization(canvas, latin, cjk, data);
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
                value(values, "periodYears"), value(values, "renewalYears"), value(values, "startDate"), value(values, "endDate"), value(values, "earnestDeposit"),
                value(values, "landlordName"), value(values, "landlordIdentity"), value(values, "landlordDate"),
                value(values, "tenantName"), value(values, "tenantIdentity"), value(values, "tenantDate"),
                valueOrFallback(values, "tenantWitnessName", "witnessName"),
                valueOrFallback(values, "tenantWitnessIdentity", "witnessIdentity"),
                valueOrFallback(values, "tenantWitnessDate", "witnessDate"),
                valueOrFallback(values, "landlordWitnessName", "witnessName"),
                valueOrFallback(values, "landlordWitnessIdentity", "witnessIdentity"),
                valueOrFallback(values, "landlordWitnessDate", "witnessDate"),
                valueOrFallback(values, "commissionWords", "commission"), value(values, "earnestDepositWords"),
                value(values, "commissionMonths"), value(values, "sstPercent"), value(values, "commissionAmount"),
                value(values, "agencyFeeTotal"), valueOrFallback(values, "landlordAddress", "ownerAddress"), value(values, "secondLandlordName"),
                value(values, "secondLandlordIdentity"), value(values, "secondLandlordAddress"),
                value(values, "secondLandlordDate"), valueOrFallback(values, "landlordWitnessName", "witnessName"),
                valueOrFallback(values, "landlordWitnessIdentity", "witnessIdentity"), value(values, "witnessAddress"),
                valueOrFallback(values, "landlordWitnessDate", "witnessDate"), value(values, "otherConditions"), List.of());
    }

    /** Enforces the same completeness rules even when callers bypass the admin form. */
    public void validateRequiredFields(TemplateType type, Map<String, String> fields) {
        if (type == TemplateType.OTR) {
            Map<String, String> required = new LinkedHashMap<>();
            required.put("caseNo", "编号");
            required.put("propertyAddress", "房产地址");
            required.put("advanceRental", "预付租金");
            required.put("securityDepositMonths", "保证金月数");
            required.put("securityDeposit", "保证金金额");
            required.put("utilityDepositMonths", "水电押金月数");
            required.put("utilityDeposit", "水电押金金额");
            required.put("stampingFee", "印花及合同费用");
            required.put("totalBeforeKeys", "交钥匙前应付总额");
            required.put("periodYears", "租期年数");
            required.put("renewalYears", "续租年数");
            required.put("commencementDate", "租约开始日期");
            required.put("earnestDeposit", "订金金额");
            required.put("tenantName", "租客姓名");
            required.put("tenantIdentity", "租客证件号");
            required.put("tenantDate", "租客签署日期");
            required.put("landlordName", "业主姓名");
            required.put("landlordIdentity", "业主证件号");
            required.put("landlordDate", "业主签署日期");
            required.put("tenantWitnessName", "租客方见证人姓名");
            required.put("tenantWitnessIdentity", "租客方见证人证件号");
            required.put("tenantWitnessDate", "租客方见证日期");
            required.put("landlordWitnessName", "业主方见证人姓名");
            required.put("landlordWitnessIdentity", "业主方见证人证件号");
            required.put("landlordWitnessDate", "业主方见证日期");
            required.put("otherConditions", "其他条件");
            Map<String, String> values = fields == null ? Map.of() : fields;
            required.put("earnestDepositWords", "订金英文大写");
            required.put("commissionWords", "佣金月数英文");
            required.put("commissionMonths", "折合租金月数");
            required.put("sstPercent", "SST");
            required.put("commissionAmount", "佣金金额");
            required.put("agencyFeeTotal", "含税代理费总额");
            required.put("startDate", "委任开始日期");
            required.put("endDate", "委任结束日期");
            required.put("landlordAddress", "第一业主地址");
            required.put("witnessAddress", "第二页见证人地址");
            if (Boolean.parseBoolean(value(values, "hasSecondLandlord"))) {
                required.put("secondLandlordName", "第二业主姓名");
                required.put("secondLandlordIdentity", "第二业主证件号");
                required.put("secondLandlordAddress", "第二业主地址");
                required.put("secondLandlordDate", "第二业主签署日期");
            }
            List<String> missing = required.keySet().stream().filter(key -> {
                if ("commissionWords".equals(key)) {
                    return value(values, "commissionWords").isBlank() && value(values, "commission").isBlank();
                }
                return value(values, key).isBlank();
            }).toList();
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException("OTR／租赁委任书资料不完整：" + String.join(", ", missing));
            }
            return;
        }
        if (type != TemplateType.AUTHORIZATION) return;
        Map<String, String> required = new LinkedHashMap<>();
        required.put("caseNo", "委托编号");
        required.put("propertyAddress", "房产完整地址");
        required.put("earnestDeposit", "订金金额");
        required.put("earnestDepositWords", "订金英文大写");
        required.put("commissionWords", "佣金月数英文");
        required.put("commissionMonths", "折合租金月数");
        required.put("sstPercent", "SST");
        required.put("commissionAmount", "佣金金额");
        required.put("agencyFeeTotal", "含税代理费总额");
        required.put("startDate", "委任开始日期");
        required.put("commencementDate", "委任结束日期");
        required.put("landlordName", "第一业主姓名");
        required.put("landlordIdentity", "第一业主证件号");
        required.put("landlordAddress", "第一业主地址");
        required.put("landlordDate", "第一业主签署日期");
        required.put("witnessName", "见证人姓名");
        required.put("witnessIdentity", "见证人证件号");
        required.put("witnessAddress", "见证人地址");
        required.put("witnessDate", "见证日期");
        if (Boolean.parseBoolean(value(fields == null ? Map.of() : fields, "hasSecondLandlord"))) {
            required.put("secondLandlordName", "第二业主姓名");
            required.put("secondLandlordIdentity", "第二业主证件号");
            required.put("secondLandlordAddress", "第二业主地址");
            required.put("secondLandlordDate", "第二业主签署日期");
        }
        Map<String, String> values = fields == null ? Map.of() : fields;
        List<String> missing = required.keySet().stream().filter(key -> {
            if ("commissionWords".equals(key)) {
                return value(values, "commissionWords").isBlank() && value(values, "commission").isBlank();
            }
            return value(values, key).isBlank();
        }).toList();
        if (!missing.isEmpty()) throw new IllegalArgumentException("租赁委任书资料不完整：" + String.join(", ", missing));
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
        text(canvas, latin, cjk, d.caseNo(), 467, 730, 9, 68);
        multilineText(canvas, latin, cjk, d.propertyAddress(), 9,
                List.of(new TextLine(152, 715, 362), new TextLine(152, 702, 362)));
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
        text(canvas, latin, cjk, d.renewalYears(), 370, 585, 9, 50);
        text(canvas, latin, cjk, displayDate(d.commencementDate()), 185, 565, 9, 220);
        text(canvas, latin, cjk, amount(d.earnestDeposit()), 250, 442, 9, 125);
        // The upper signing block has a compact 9-point row height. Keep every
        // value on the baseline of its own template row so the dates do not
        // collide with the dashed acceptance separator below.
        text(canvas, latin, cjk, d.tenantName(), 115, 355, 8, 175);
        text(canvas, latin, cjk, d.tenantIdentity(), 115, 346, 8, 175);
        text(canvas, latin, cjk, displayDate(d.tenantDate()), 115, 337, 8, 175);
        text(canvas, latin, cjk, d.landlordName(), 340, 354, 8, 175);
        text(canvas, latin, cjk, d.landlordIdentity(), 340, 345, 8, 175);
        text(canvas, latin, cjk, displayDate(d.landlordDate()), 340, 336, 8, 175);
        text(canvas, latin, cjk, d.tenantWitnessName(), 115, 148, 8, 150);
        text(canvas, latin, cjk, d.tenantWitnessIdentity(), 115, 138, 8, 150);
        text(canvas, latin, cjk, displayDate(d.tenantWitnessDate()), 115, 128, 8, 150);
        text(canvas, latin, cjk, d.landlordWitnessName(), 340, 149, 8, 150);
        text(canvas, latin, cjk, d.landlordWitnessIdentity(), 340, 140, 8, 150);
        text(canvas, latin, cjk, displayDate(d.landlordWitnessDate()), 340, 130, 8, 150);
        text(canvas, latin, cjk, d.otherConditions(), 140, 110, 8, 390);
    }

    private void fillAuthorization(PdfContentByte canvas, BaseFont latin, BaseFont cjk, TemplateData d) throws IOException {
        text(canvas, latin, cjk, d.caseNo(), 455, 716, 9, 100);
        multilineText(canvas, latin, cjk, d.propertyAddress(), 9,
                List.of(new TextLine(75, 651, 440), new TextLine(75, 632, 440)));
        text(canvas, latin, cjk, amount(d.earnestDeposit()), 116, 584, 9, 105);
        text(canvas, latin, cjk, amountWords(d.earnestDepositWords()), 303, 584, 9, 165);
        text(canvas, latin, cjk, d.commissionWords(), 419, 557, 9, 29);
        text(canvas, latin, cjk, d.commissionMonths(), 458, 557, 9, 34);
        text(canvas, latin, cjk, d.sstPercent(), 203, 547, 9, 28);
        text(canvas, latin, cjk, amount(d.commissionAmount()), 244, 547, 8, 45);
        text(canvas, latin, cjk, amount(d.agencyFeeTotal()), 472, 547, 9, 42);
        text(canvas, latin, cjk, displayDate(d.startDate()), 316, 364, 9, 75);
        text(canvas, latin, cjk, displayDate(d.endDate().isBlank() ? d.commencementDate() : d.endDate()), 404, 364, 9, 75);
        // Keep the values visibly separated from the template labels and from
        // neighbouring columns. A slightly smaller size prevents long names,
        // identity numbers and addresses from touching the next column.
        text(canvas, latin, cjk, d.landlordName(), 115, 243, 8, 100);
        text(canvas, latin, cjk, d.landlordIdentity(), 115, 234, 8, 100);
        text(canvas, latin, cjk, d.landlordAddress(), 115, 225, 7.5f, 100);
        text(canvas, latin, cjk, displayDate(d.landlordDate()), 115, 216, 8, 100);
        text(canvas, latin, cjk, d.secondLandlordName(), 262, 243, 8, 99);
        text(canvas, latin, cjk, d.secondLandlordIdentity(), 262, 234, 8, 99);
        text(canvas, latin, cjk, d.secondLandlordAddress(), 262, 225, 7.5f, 99);
        text(canvas, latin, cjk, displayDate(d.secondLandlordDate()), 262, 216, 8, 99);
        text(canvas, latin, cjk, d.witnessName(), 415, 243, 8, 100);
        text(canvas, latin, cjk, d.witnessIdentity(), 415, 234, 8, 100);
        text(canvas, latin, cjk, d.witnessAddress(), 415, 225, 7.5f, 100);
        text(canvas, latin, cjk, displayDate(d.witnessDate()), 415, 216, 8, 100);
    }

    private void multilineText(PdfContentByte canvas, BaseFont latin, BaseFont cjk, String value, float size,
            List<TextLine> placements) throws IOException {
        if (value == null || value.isBlank() || placements == null || placements.isEmpty()) return;
        String remaining = value.replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
        BaseFont font = remaining.chars().allMatch(ch -> ch < 128) ? latin : cjk;
        for (int index = 0; index < placements.size() && !remaining.isBlank(); index++) {
            TextLine placement = placements.get(index);
            if (index == placements.size() - 1) {
                text(canvas, latin, cjk, remaining, placement.x(), placement.y(), size, placement.maxWidth());
                return;
            }
            int split = fittingPrefix(remaining, font, size, placement.maxWidth());
            String line = remaining.substring(0, split).trim();
            text(canvas, latin, cjk, line, placement.x(), placement.y(), size, placement.maxWidth());
            remaining = remaining.substring(split).trim();
        }
    }

    private int fittingPrefix(String value, BaseFont font, float size, float maxWidth) {
        if (font.getWidthPoint(value, size) <= maxWidth) return value.length();
        int lastBreak = -1;
        for (int index = 1; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index - 1))) lastBreak = index - 1;
            if (font.getWidthPoint(value.substring(0, index), size) > maxWidth) {
                if (lastBreak > 0) return lastBreak;
                return Math.max(1, index - 1);
            }
        }
        return value.length();
    }

    private String displayDate(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            return LocalDate.parse(value.trim()).format(DateTimeFormatter.ofPattern("d/M/uuuu"));
        } catch (DateTimeParseException ignored) {
            return value.trim();
        }
    }

    private record TextLine(float x, float y, float maxWidth) { }

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

    private String amountWords(String value) {
        if (value == null) return "";
        return value.trim()
                .replaceFirst("(?i)^\\(?\\s*Ringgit\\s+Malaysia\\s+", "")
                .replaceFirst("(?i)\\s+only\\s*\\)?$", "")
                .trim();
    }
}
