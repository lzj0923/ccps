package com.ccps.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Fills the customer supplied tenancy agreement DOCX without rebuilding its
 * wording, inventory tables, or formatting. The source template is read-only;
 * every request produces a new DOCX package.
 */
@Deprecated(forRemoval = true)
public class TenancyAgreementWordService {
    public static final String TEMPLATE_RESOURCE = "contract-templates/owner-tenancy-agreement-template.docx";
    public static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String WORD_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final String XML_NS = XMLConstants.XML_NS_URI;
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public boolean isTemplateAvailable() {
        return new ClassPathResource(TEMPLATE_RESOURCE).exists();
    }

    public byte[] generate(Map<String, String> fields) {
        if (!isTemplateAvailable()) {
            throw new IllegalStateException("Legacy tenancy agreement Word generation is unavailable; use the current PDF tenancy agreement service");
        }
        Map<String, String> values = fields == null ? Map.of() : fields;
        try (InputStream source = new ClassPathResource(TEMPLATE_RESOURCE).getInputStream();
                ZipInputStream input = new ZipInputStream(source);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(output)) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                byte[] content = input.readAllBytes();
                if ("word/document.xml".equals(entry.getName())) {
                    content = patchDocument(new String(content, StandardCharsets.UTF_8), values)
                            .getBytes(StandardCharsets.UTF_8);
                }
                ZipEntry copy = new ZipEntry(entry.getName());
                zip.putNextEntry(copy);
                zip.write(content);
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate tenancy agreement Word document", exception);
        }
    }

    public String fileName(Map<String, String> fields) {
        String base = java.util.stream.Stream.of(firstNotBlank(value(fields, "tenantName"), value(fields, "landlordName")), value(fields, "projectName"),
                        value(fields, "unitNo"), "租赁合同", value(fields, "caseNo"))
                .map(this::safeFilePart).filter(part -> !part.isBlank())
                .reduce((left, right) -> left + "-" + right).orElse("租赁合同");
        return base + ".docx";
    }

    private String firstNotBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private String safeFilePart(String value) {
        if (value == null) return "";
        String safe = value.trim().replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "-").replaceAll("\\s+", " ");
        return safe.length() > 60 ? safe.substring(0, 60) : safe;
    }

    private String patchDocument(String xml, Map<String, String> fields) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

        String owner = value(fields, "landlordName");
        String tenant = value(fields, "tenantName");
        String ownerIdentity = value(fields, "landlordIdentity");
        String tenantIdentity = value(fields, "tenantIdentity");
        replaceAllText(document, "(Date)", displayDate(value(fields, "agreementDate")));
        replaceAllText(document, "(Landlord Name)", owner);
        replaceAllText(document, "(Tenant Name)", tenant);
        replaceAllText(document, "(Landlord NRIC/Passport No.)", ownerIdentity);
        replaceAllText(document, "(Tenant NRIC/ Passport No.)", tenantIdentity);
        replaceOccurrences(document, "(NRIC/Passport No.)", List.of(ownerIdentity, tenantIdentity));

        Element firstSchedule = tableAt(document, 0);
        if (firstSchedule != null) {
            setCellLines(cellAt(firstSchedule, 1, 2), List.of(displayDate(value(fields, "agreementDate"))));
            setCellLines(cellAt(firstSchedule, 2, 2), List.of(
                    "Name: " + owner,
                    "NRIC/Passport No: " + ownerIdentity,
                    "Address: " + value(fields, "landlordAddress")));
            setCellLines(cellAt(firstSchedule, 3, 2), List.of(
                    "Name: " + tenant,
                    "NRIC/Passport No: " + tenantIdentity,
                    "Contact No: " + value(fields, "tenantPhone"),
                    "Address: " + value(fields, "tenantAddress"),
                    "Email Address: " + value(fields, "tenantEmail")));
            setCellLines(cellAt(firstSchedule, 4, 2), List.of(value(fields, "propertyAddress")));
            setCellLines(cellAt(firstSchedule, 5, 2), List.of(
                    valueOr(fields, "termYears", "One year"),
                    displayDate(value(fields, "leaseStart")),
                    displayDate(value(fields, "leaseEnd"))));
        }

        Element rentalSchedule = tableAt(document, 1);
        if (rentalSchedule != null) {
            Element monthly = cellAt(rentalSchedule, 0, 2);
            replaceInCell(monthly, "Ringgit Malaysia (RM) Only", value(fields, "monthlyRent"));
            replaceInCell(monthly, "1st day of every month", paymentDay(value(fields, "paymentDay")));
            setCellLines(cellAt(rentalSchedule, 1, 2), List.of(value(fields, "advanceRental")));
            setCellLines(cellAt(rentalSchedule, 2, 2), List.of(value(fields, "securityDeposit")));
            setCellLines(cellAt(rentalSchedule, 3, 2), List.of(value(fields, "utilityDeposit")));
            setCellLines(cellAt(rentalSchedule, 4, 2), List.of(valueOr(fields, "use", "For Residential use only")));
            setCellLines(cellAt(rentalSchedule, 6, 2), List.of(value(fields, "specialConditions")));
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        StringWriter result = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(result));
        return result.toString();
    }

    private Element tableAt(Document document, int index) {
        NodeList tables = document.getElementsByTagNameNS(WORD_NS, "tbl");
        return index >= 0 && index < tables.getLength() ? (Element) tables.item(index) : null;
    }

    private Element cellAt(Element table, int rowIndex, int cellIndex) {
        if (table == null) return null;
        List<Element> rows = directChildren(table, "tr");
        if (rowIndex < 0 || rowIndex >= rows.size()) return null;
        List<Element> cells = directChildren(rows.get(rowIndex), "tc");
        return cellIndex >= 0 && cellIndex < cells.size() ? cells.get(cellIndex) : null;
    }

    private List<Element> directChildren(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        if (parent == null) return result;
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && WORD_NS.equals(element.getNamespaceURI())
                    && localName.equals(element.getLocalName())) result.add(element);
        }
        return result;
    }

    private void setCellLines(Element cell, List<String> lines) {
        if (cell == null) return;
        List<Element> paragraphs = directChildren(cell, "p");
        if (paragraphs.isEmpty()) return;
        Element paragraph = paragraphs.get(0);
        Element paragraphProperties = null;
        Element runProperties = null;
        for (Node child = paragraph.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && "pPr".equals(element.getLocalName())) paragraphProperties = element;
            if (child instanceof Element element && "r".equals(element.getLocalName())) {
                for (Node runChild = element.getFirstChild(); runChild != null; runChild = runChild.getNextSibling()) {
                    if (runChild instanceof Element runElement && "rPr".equals(runElement.getLocalName())) {
                        runProperties = (Element) runElement.cloneNode(true);
                        break;
                    }
                }
            }
        }
        while (paragraph.hasChildNodes()) paragraph.removeChild(paragraph.getFirstChild());
        if (paragraphProperties != null) paragraph.appendChild(paragraphProperties);
        Element run = paragraph.getOwnerDocument().createElementNS(WORD_NS, "w:r");
        if (runProperties != null) run.appendChild(runProperties);
        List<String> safeLines = lines == null ? List.of("") : lines;
        for (int i = 0; i < safeLines.size(); i++) {
            if (i > 0) run.appendChild(paragraph.getOwnerDocument().createElementNS(WORD_NS, "w:br"));
            Element text = paragraph.getOwnerDocument().createElementNS(WORD_NS, "w:t");
            text.setAttributeNS(XML_NS, "xml:space", "preserve");
            text.setTextContent(safeLines.get(i) == null ? "" : safeLines.get(i));
            run.appendChild(text);
        }
        paragraph.appendChild(run);
        for (int i = 1; i < paragraphs.size(); i++) cell.removeChild(paragraphs.get(i));
    }

    private void replaceInCell(Element cell, String marker, String replacement) {
        if (cell == null || marker == null) return;
        NodeList texts = cell.getElementsByTagNameNS(WORD_NS, "t");
        for (int i = 0; i < texts.getLength(); i++) {
            Node text = texts.item(i);
            if (text.getTextContent().contains(marker)) {
                text.setTextContent(text.getTextContent().replace(marker, replacement == null ? "" : replacement));
            }
        }
    }

    private void replaceAllText(Document document, String marker, String replacement) {
        replaceOccurrences(document, marker, List.of(replacement == null ? "" : replacement));
    }

    private void replaceOccurrences(Document document, String marker, List<String> replacements) {
        NodeList texts = document.getElementsByTagNameNS(WORD_NS, "t");
        int occurrence = 0;
        for (int i = 0; i < texts.getLength(); i++) {
            Node text = texts.item(i);
            String current = text.getTextContent();
            if (!current.contains(marker)) continue;
            String replacement = replacements.isEmpty() ? "" : replacements.get(Math.min(occurrence, replacements.size() - 1));
            text.setTextContent(current.replace(marker, replacement == null ? "" : replacement));
            occurrence++;
        }
    }

    private String value(Map<String, String> fields, String key) {
        if (fields == null) return "";
        String value = fields.get(key);
        return value == null ? "" : value.trim();
    }

    private String valueOr(Map<String, String> fields, String key, String fallback) {
        String value = value(fields, key);
        return value.isBlank() ? fallback : value;
    }

    private String displayDate(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            LocalDate date = LocalDate.parse(value, ISO_DATE);
            int day = date.getDayOfMonth();
            String suffix = day % 100 >= 11 && day % 100 <= 13 ? "th" : switch (day % 10) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
            return day + suffix + " " + date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + date.getYear();
        } catch (DateTimeParseException ignored) {
            return value;
        }
    }

    private String paymentDay(String value) {
        if (value.isBlank()) return "";
        try {
            int day = Integer.parseInt(value.replaceAll("[^0-9]", ""));
            String suffix = day % 100 >= 11 && day % 100 <= 13 ? "th" : switch (day % 10) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
            return day + suffix + " day of every month";
        } catch (NumberFormatException ignored) {
            return value;
        }
    }
}
