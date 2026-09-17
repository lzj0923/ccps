package com.ccps.backend.service;

import java.io.IOException;
import java.io.InputStream;

import com.lowagie.text.pdf.BaseFont;

/** Classpath-backed, embedded fonts shared by all server-generated PDFs. */
final class PdfFontResources {
    private static final String REGULAR_RESOURCE = "/fonts/NotoSansCJKsc-Regular.otf";
    private static final String BOLD_RESOURCE = "/fonts/NotoSansCJKsc-Bold.otf";

    private PdfFontResources() { }

    static BaseFont regular() { return RegularHolder.FONT; }
    static BaseFont bold() { return BoldHolder.FONT; }

    private static BaseFont load(String resource) {
        try (InputStream input = PdfFontResources.class.getResourceAsStream(resource)) {
            if (input == null) throw new IllegalStateException("Bundled PDF font is missing: " + resource);
            byte[] bytes = input.readAllBytes();
            String name = resource.substring(resource.lastIndexOf('/') + 1);
            return BaseFont.createFont(name, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
        } catch (IOException | com.lowagie.text.DocumentException exception) {
            throw new IllegalStateException("Unable to load bundled PDF font: " + resource, exception);
        }
    }

    private static final class RegularHolder { private static final BaseFont FONT = load(REGULAR_RESOURCE); }
    private static final class BoldHolder { private static final BaseFont FONT = load(BOLD_RESOURCE); }
}
