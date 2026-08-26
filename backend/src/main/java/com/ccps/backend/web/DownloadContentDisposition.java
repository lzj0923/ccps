package com.ccps.backend.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Browser-safe download names with an ASCII fallback and RFC 5987 UTF-8 name. */
public final class DownloadContentDisposition {
    private DownloadContentDisposition() { }

    public static String attachment(String fileName) {
        return value("attachment", fileName);
    }

    public static String inline(String fileName) {
        return value("inline", fileName);
    }

    public static String value(String type, String fileName) {
        String safe = fileName == null ? "" : fileName.replace("\r", "").replace("\n", "").trim();
        String extension = extensionOf(safe);
        String fallback = switch (extension) {
            case ".pdf" -> "tenancy-agreement.pdf";
            case ".docx" -> "tenancy-agreement.docx";
            default -> "download" + extension;
        };
        String encoded = URLEncoder.encode(safe.isBlank() ? fallback : safe, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return type + "; filename=\"" + fallback + "\"; filename*=UTF-8''" + encoded;
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return ".pdf";
        String extension = fileName.substring(dot).toLowerCase();
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : ".pdf";
    }
}
