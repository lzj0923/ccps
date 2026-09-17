package com.ccps.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

class PdfFontResourcesTest {
    @Test
    void bundlesAndEmbedsRegularAndBoldCjkFonts() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, output);
        document.open();
        document.add(new Paragraph("CCPS 租赁合同", new com.lowagie.text.Font(PdfFontResources.regular(), 12)));
        document.add(new Paragraph("正式收据", new com.lowagie.text.Font(PdfFontResources.bold(), 12)));
        document.close();

        PdfReader reader = new PdfReader(output.toByteArray());
        PdfDictionary fonts = reader.getPageN(1).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.FONT);
        assertEquals(2, fonts.getKeys().size());
        for (PdfName key : fonts.getKeys()) {
            PdfDictionary type0 = (PdfDictionary)PdfReader.getPdfObject(fonts.get(key));
            PdfArray descendants = type0.getAsArray(PdfName.DESCENDANTFONTS);
            PdfDictionary cidFont = (PdfDictionary)PdfReader.getPdfObject(descendants.getPdfObject(0));
            PdfDictionary descriptor = (PdfDictionary)PdfReader.getPdfObject(cidFont.get(PdfName.FONTDESCRIPTOR));
            PdfObject embedded = descriptor.get(PdfName.FONTFILE3);
            if (embedded == null) embedded = descriptor.get(PdfName.FONTFILE2);
            assertNotNull(embedded);
        }
        reader.close();
    }

    @Test
    void fixedContractBackgroundsContainNoExternalFontDependencies() throws Exception {
        List<String> resources = List.of(
                "/contract-templates/flattened/letter-offer-to-rent.pdf",
                "/contract-templates/flattened/letter-of-appointment-to-rent.pdf",
                "/contract-templates/flattened/ccps-pma-v1.pdf",
                "/contract-templates/flattened/ccps-management-authorization-v1.pdf",
                "/contract-templates/flattened/ccps-termination-letter-v1.pdf",
                "/contract-templates/flattened/ccps-rental-remittance-v1.pdf",
                "/contract-templates/flattened/conlay-tenancy-agreement-template.pdf");
        for (String resource : resources) {
            try (InputStream input = getClass().getResourceAsStream(resource)) {
                assertNotNull(input, resource);
                PdfReader reader = new PdfReader(input.readAllBytes());
                for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                    PdfDictionary pageResources = reader.getPageN(page).getAsDict(PdfName.RESOURCES);
                    PdfDictionary fonts = pageResources == null ? null : pageResources.getAsDict(PdfName.FONT);
                    assertEquals(0, fonts == null ? 0 : fonts.size(), resource + " page " + page);
                }
                reader.close();
            }
        }
    }
}
