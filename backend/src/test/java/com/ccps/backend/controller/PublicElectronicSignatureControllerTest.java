package com.ccps.backend.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;

import com.ccps.backend.service.ElectronicSignatureService;
import com.ccps.backend.service.ElectronicSignatureService.Download;

class PublicElectronicSignatureControllerTest {
    @TempDir Path tempDir;

    @Test
    void signedDocumentCanBePreviewedInlineOrSavedExplicitly() throws Exception {
        Path pdf = tempDir.resolve("signed.pdf");
        Files.write(pdf, "%PDF-1.4".getBytes());
        ElectronicSignatureService service = mock(ElectronicSignatureService.class);
        when(service.downloadSigned("token")).thenReturn(new Download(pdf, "已签合同.pdf"));
        PublicElectronicSignatureController controller = new PublicElectronicSignatureController(service);

        String previewHeader = controller.signedDocument("token", false).getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION);
        String downloadHeader = controller.signedDocument("token", true).getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION);

        assertTrue(previewHeader.startsWith("inline;"));
        assertTrue(downloadHeader.startsWith("attachment;"));
    }
}
