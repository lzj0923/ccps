package com.ccps.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
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
            String renewalYears,
            String startDate,
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
            String commission,
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
                value(values, "periodYears"), value(values, "renewalYears"), value(values, "startDate"), value(values, "earnestDeposit"),
                value(values, "landlordName"), value(values, "landlordIdentity"), value(values, "landlordDate"),
                value(values, "tenantName"), value(values, "tenantIdentity"), value(values, "tenantDate"),
                valueOrFallback(values, "tenantWitnessName", "witnessName"),
                valueOrFallback(values, "tenantWitnessIdentity", "witnessIdentity"),
                valueOrFallback(values, "tenantWitnessDate", "witnessDate"),
                valueOrFallback(values, "landlordWitnessName", "witnessName"),
                valueOrFallback(values, "landlordWitnessIdentity", "witnessIdentity"),
                valueOrFallback(values, "landlordWitnessDate", "witnessDate"),
                valueOrFallback(values, "commission", "commissionPercent"), value(values, "earnestDepositWords"),
                value(values, "commissionMonths"), value(values, "sstPercent"), value(values, "commissionAmount"),
                value(values, "agencyFeeTotal"), value(values, "landlordAddress"), value(values, "secondLandlordName"),
                value(values, "secondLandlordIdentity"), value(values, "secondLandlordAddress"),
                value(values, "secondLandlordDate"), value(values, "witnessName"), value(values, "witnessIdentity"),
                value(values, "witnessAddress"), value(values, "witnessDate"), value(values, "otherConditions"), List.of());
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
            List<String> missing = required.keySet().stream().filter(key -> value(values, key).isBlank()).toList();
            if (!missing.isEmpty()) throw new IllegalArgumentException("OTR 出价函资料不完整：" + String.join(", ", missing));
            return;
        }
        if (type != TemplateType.AUTHORIZATION) return;
        Map<String, String> required = new LinkedHashMap<>();
        required.put("caseNo", "委托编号");
        required.put("propertyAddress", "房产完整地址");
        required.put("earnestDeposit", "订金金额");
        required.put("earnestDepositWords", "订金英文大写");
        required.put("commission", "佣金比例");
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
            if ("commission".equals(key)) return value(values, "commission").isBlank() && value(values, "commissionPercent").isBlank();
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
        text(canvas, latin, cjk, d.renewalYears(), 370, 585, 9, 50);
        text(canvas, latin, cjk, d.commencementDate(), 185, 565, 9, 220);
        text(canvas, latin, cjk, amount(d.earnestDeposit()), 250, 442, 9, 125);
        // The upper signing block has a compact 9-point row height. Keep every
        // value on the baseline of its own template row so the dates do not
        // collide with the dashed acceptance separator below.
        text(canvas, latin, cjk, d.tenantName(), 115, 355, 8, 175);
        text(canvas, latin, cjk, d.tenantIdentity(), 115, 346, 8, 175);
        text(canvas, latin, cjk, d.tenantDate(), 115, 337, 8, 175);
        text(canvas, latin, cjk, d.landlordName(), 340, 354, 8, 175);
        text(canvas, latin, cjk, d.landlordIdentity(), 340, 345, 8, 175);
        text(canvas, latin, cjk, d.landlordDate(), 340, 336, 8, 175);
        text(canvas, latin, cjk, d.tenantWitnessName(), 115, 148, 8, 150);
        text(canvas, latin, cjk, d.tenantWitnessIdentity(), 115, 138, 8, 150);
        text(canvas, latin, cjk, d.tenantWitnessDate(), 115, 128, 8, 150);
        text(canvas, latin, cjk, d.landlordWitnessName(), 340, 149, 8, 150);
        text(canvas, latin, cjk, d.landlordWitnessIdentity(), 340, 140, 8, 150);
        text(canvas, latin, cjk, d.landlordWitnessDate(), 340, 130, 8, 150);
        text(canvas, latin, cjk, d.otherConditions(), 90, 105, 8, 430);
    }

    private void fillAuthorization(PdfContentByte canvas, BaseFont latin, BaseFont cjk, TemplateData d) throws IOException {
        text(canvas, latin, cjk, d.caseNo(), 455, 716, 9, 100);
        text(canvas, latin, cjk, d.propertyAddress(), 75, 651, 9, 470);
        text(canvas, latin, cjk, amount(d.earnestDeposit()), 116, 584, 9, 105);
        text(canvas, latin, cjk, d.earnestDepositWords(), 303, 584, 9, 165);
        text(canvas, latin, cjk, d.commission(), 418, 557, 9, 45);
        text(canvas, latin, cjk, d.commissionMonths(), 458, 557, 9, 34);
        text(canvas, latin, cjk, d.sstPercent(), 203, 547, 9, 28);
        text(canvas, latin, cjk, amount(d.commissionAmount()), 244, 547, 8, 45);
        text(canvas, latin, cjk, amount(d.agencyFeeTotal()), 472, 547, 9, 42);
        text(canvas, latin, cjk, d.startDate(), 316, 364, 9, 75);
        text(canvas, latin, cjk, d.commencementDate(), 404, 364, 9, 75);
        // Keep the values visibly separated from the template labels and from
        // neighbouring columns. A slightly smaller size prevents long names,
        // identity numbers and addresses from touching the next column.
        text(canvas, latin, cjk, d.landlordName(), 118, 243, 8, 92);
        text(canvas, latin, cjk, d.landlordIdentity(), 118, 234, 8, 92);
        text(canvas, latin, cjk, d.landlordAddress(), 118, 225, 7.5f, 92);
        text(canvas, latin, cjk, d.landlordDate(), 118, 216, 8, 92);
        text(canvas, latin, cjk, d.secondLandlordName(), 265, 243, 8, 92);
        text(canvas, latin, cjk, d.secondLandlordIdentity(), 265, 234, 8, 92);
        text(canvas, latin, cjk, d.secondLandlordAddress(), 265, 225, 7.5f, 92);
        text(canvas, latin, cjk, d.secondLandlordDate(), 265, 216, 8, 92);
        text(canvas, latin, cjk, d.witnessName(), 418, 243, 8, 92);
        text(canvas, latin, cjk, d.witnessIdentity(), 418, 234, 8, 92);
        text(canvas, latin, cjk, d.witnessAddress(), 418, 225, 7.5f, 92);
        text(canvas, latin, cjk, d.witnessDate(), 418, 216, 8, 92);
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
