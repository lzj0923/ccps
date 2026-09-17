package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

class FinanceDocumentPdfRendererTest {
    private static final Path PREVIEW_DIR = Path.of("target", "finance-document-previews");

    @Test
    void rendersInvoiceInEnglishReferenceFormatWithoutPageNumber() throws Exception {
        byte[] pdf = render(new FinanceDocumentPdfRenderer.Data(
                true,
                "CCPS PROPERTY MANAGEMENT SDN. BHD.",
                "",
                List.of(),
                List.of("TENANT NAME", "PROJECT / A-01"),
                "IV-TXN-20260825-001",
                LocalDate.of(2026, 8, 25),
                "RENTAL FOR 01/08/2026 TO 31/08/2026",
                List.of(
                        new FinanceDocumentPdfRenderer.Detail("Transaction No.", "TXN-20260825-001"),
                        new FinanceDocumentPdfRenderer.Detail("Item Code", "rent_payment"),
                        new FinanceDocumentPdfRenderer.Detail("Property", "PROJECT / A-01"),
                        new FinanceDocumentPdfRenderer.Detail("Payment Method", "Bank Transfer"),
                        new FinanceDocumentPdfRenderer.Detail("Payment Reference", "BANK-REF-001"),
                        new FinanceDocumentPdfRenderer.Detail("Status", "confirmed")),
                new BigDecimal("8000.00"),
                "MYR",
                List.of("Notes:",
                        "1. All cheques should be crossed and made payable to: CCPS PROPERTY MANAGEMENT SDN. BHD.",
                        "2. All payments shall be remitted to the following bank account:",
                        "Account Holder: CCPS PROPERTY MANAGEMENT SDN. BHD.",
                        "Bank: MAYBANK    Account No.: 1234567890")),
                "invoice-reference.pdf");

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf))) {
            assertThat(reader.getNumberOfPages()).isOne();
            String text = new PdfTextExtractor(reader).getTextFromPage(1, true);
            assertThat(text).contains("INVOICE NO.", "DESCRIPTION", "TOTAL AMOUNT PAYABLE (MYR)",
                    "EIGHT THOUSAND ONLY", "This is a computer generated document");
            assertThat(text).doesNotContain("Page 1", "1 of 1", "發票", "收據");
        }
    }

    @Test
    void rendersReceiptInEnglishReferenceFormatWithoutPageNumber() throws Exception {
        byte[] pdf = render(new FinanceDocumentPdfRenderer.Data(
                false,
                "CCPS PROPERTY MANAGEMENT SDN. BHD.",
                "",
                List.of(),
                List.of("TENANT NAME", "PROJECT / A-01", "LEASE NO. : LEASE-001"),
                "RENT-RCP-20260825-001",
                LocalDate.of(2026, 8, 25),
                "RENTAL FOR 01/08/2026 TO 31/08/2026",
                List.of(
                        new FinanceDocumentPdfRenderer.Detail("Transaction No.", "RENT-20260825-001"),
                        new FinanceDocumentPdfRenderer.Detail("Lease No.", "LEASE-001"),
                        new FinanceDocumentPdfRenderer.Detail("Tenancy Period", "01/08/2026 TO 31/08/2026"),
                        new FinanceDocumentPdfRenderer.Detail("Billing Month", "2026-08"),
                        new FinanceDocumentPdfRenderer.Detail("Payment Method", "Cash"),
                        new FinanceDocumentPdfRenderer.Detail("Payer", "TENANT NAME"),
                        new FinanceDocumentPdfRenderer.Detail("Payment Reference", "CASH-001"),
                        new FinanceDocumentPdfRenderer.Detail("Note", "Payment confirmed")),
                new BigDecimal("8000.50"),
                "MYR",
                List.of("This receipt was generated automatically after the payment was confirmed.")),
                "receipt-reference.pdf");

        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf))) {
            assertThat(reader.getNumberOfPages()).isOne();
            String text = new PdfTextExtractor(reader).getTextFromPage(1, true);
            assertThat(text).contains("RECEIPT NO.", "TENANCY PERIOD", "TOTAL AMOUNT RECEIVED (MYR)",
                    "EIGHT THOUSAND AND FIFTY SEN ONLY");
            assertThat(text).doesNotContain("Page 1", "1 of 1", "發票", "收據");
        }
    }

    @Test
    void spellsWholeAndFractionalRinggitAmountsInEnglish() {
        assertThat(FinanceDocumentPdfRenderer.amountInWords(new BigDecimal("8064.52")))
                .isEqualTo("EIGHT THOUSAND SIXTY FOUR AND FIFTY TWO SEN ONLY");
        assertThat(FinanceDocumentPdfRenderer.amountInWords(BigDecimal.ZERO)).isEqualTo("ZERO ONLY");
    }

    private byte[] render(FinanceDocumentPdfRenderer.Data data, String previewName) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        FinanceDocumentPdfRenderer.write(output, data);
        byte[] pdf = output.toByteArray();
        Files.createDirectories(PREVIEW_DIR);
        Files.write(PREVIEW_DIR.resolve(previewName), pdf);
        return pdf;
    }
}
