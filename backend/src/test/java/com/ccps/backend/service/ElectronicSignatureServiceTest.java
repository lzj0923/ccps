package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.ElectronicSignatureStartRequest;
import com.ccps.backend.dto.ElectronicSignatureSignRequest;
import com.ccps.backend.mapper.ElectronicSignatureMapper;
import com.ccps.backend.mapper.ElectronicSignatureMapper.NewRequest;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import javax.imageio.ImageIO;

@ExtendWith(MockitoExtension.class)
class ElectronicSignatureServiceTest {
    @Mock private ElectronicSignatureMapper mapper;
    @Mock private JavaMailSender mailSender;
    @TempDir Path tempDir;

    @Test void startsLeaseSigningWithHashedAccessAndSendsOnlyThePublicLink() throws Exception {
        Path leaseRoot = tempDir.resolve("lease-contracts");
        Path original = leaseRoot.resolve("12/original.pdf");
        Files.createDirectories(original.getParent());
        try (var output = Files.newOutputStream(original)) {
            Document pdf = new Document(); PdfWriter.getInstance(pdf, output); pdf.open();
            pdf.add(new Paragraph("Original lease")); pdf.close();
        }
        ElectronicSignatureMapper.DocumentRow document = new ElectronicSignatureMapper.DocumentRow();
        document.setId(81L); document.setOriginalName("lease.pdf"); document.setStorageKey("12/original.pdf");
        document.setMimeType("application/pdf"); document.setChecksumSha256("source-hash");
        when(mapper.findLeaseDocument(12L)).thenReturn(document);
        AtomicReference<NewRequest> stored = new AtomicReference<>();
        when(mapper.insertRequest(any())).thenAnswer(call -> { NewRequest row = call.getArgument(0); row.setId(91L); stored.set(row); return 1; });

        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", leaseRoot.toString(), tempDir.resolve("rental-mandates").toString(),
                tempDir.resolve("signatures").toString(), Clock.fixed(Instant.parse("2026-07-23T08:00:00Z"), ZoneOffset.UTC));

        var started = service.startLease(7L, 12L,
                new ElectronicSignatureStartRequest("李建偉", "tenant@example.test", 7));

        assertThat(started.signingUrl()).startsWith("http://localhost:5173/sign/");
        assertThat(stored.get().getAccessTokenHash()).hasSize(64).doesNotContain("http");
        assertThat(stored.get().getVerificationCodeHash()).isNotBlank();
        assertThat(Files.size(original)).isGreaterThan(0);
        verify(mapper).insertAudit(7L, "start_electronic_signature", "lease", 12L, 91L, "李建偉");
        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test void refusesToStartAnotherRequestForAnAlreadySignedLeaseDocument() {
        ElectronicSignatureMapper.DocumentRow document = new ElectronicSignatureMapper.DocumentRow();
        document.setId(81L); document.setOriginalName("lease.pdf"); document.setStorageKey("12/original.pdf");
        document.setMimeType("application/pdf"); document.setChecksumSha256("source-hash");
        when(mapper.findLeaseDocument(12L)).thenReturn(document);
        when(mapper.countSignedRequests(81L, "lease", 12L)).thenReturn(1);
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", tempDir.resolve("lease-contracts").toString(), tempDir.resolve("rental-mandates").toString(),
                tempDir.resolve("signatures").toString(), Clock.fixed(Instant.parse("2026-07-23T08:00:00Z"), ZoneOffset.UTC));

        assertThatThrownBy(() -> service.startLease(7L, 12L,
                new ElectronicSignatureStartRequest("李建偉", "tenant@example.test", 7)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already signed");
    }

    @Test void signingCreatesSeparatePdfWithoutChangingTheOriginalContract() throws Exception {
        Path leaseRoot = tempDir.resolve("lease-contracts");
        Path original = leaseRoot.resolve("12/original.pdf"); Files.createDirectories(original.getParent());
        try (var output = Files.newOutputStream(original)) { Document pdf = new Document(); PdfWriter.getInstance(pdf, output); pdf.open(); pdf.add(new Paragraph("Original lease")); pdf.close(); }
        byte[] originalBytes = Files.readAllBytes(original);
        ElectronicSignatureMapper.RequestRow pending = requestRow("pending", null, null);
        ElectronicSignatureMapper.RequestRow signed = requestRow("signed", "91/signed-contract.pdf", LocalDateTime.of(2026, 7, 23, 16, 0));
        when(mapper.findByTokenHash(any())).thenReturn(pending, signed);
        when(mapper.insertSignedDocument(any())).thenAnswer(call -> { call.<ElectronicSignatureMapper.NewDocument>getArgument(0).setId(92L); return 1; });
        when(mapper.insertSignedDocumentLink(92L, "lease", 12L)).thenReturn(1);
        when(mapper.completeRequest(any(), any(), any(), any(), any(), any())).thenReturn(1);
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", leaseRoot.toString(), tempDir.resolve("rental-mandates").toString(),
                tempDir.resolve("signatures").toString(), Clock.fixed(Instant.parse("2026-07-23T08:00:00Z"), ZoneOffset.UTC));

        var result = service.sign("a-valid-one-time-token-with-more-than-thirty-two-chars",
                new ElectronicSignatureSignRequest("李建偉", "123456", signatureDataUrl(), true), "127.0.0.1", "JUnit");

        assertThat(result.status()).isEqualTo("signed");
        assertThat(Files.readAllBytes(original)).isEqualTo(originalBytes);
        assertThat(Files.size(tempDir.resolve("signatures/91/signed-contract.pdf"))).isGreaterThan(0);
        verify(mapper).insertEvent(91L, "signed", "Contract signed", "127.0.0.1", "JUnit");
    }

    @Test void signaturePlacementUsesThePageSignatureAreaInsteadOfFixedRightEdgeCoordinates() {
        var placement = ElectronicSignatureService.signaturePlacement("lease.pdf", 595f, 842f);

        assertThat(placement.signatureX()).isBetween(60f, 120f);
        assertThat(placement.signatureY()).isBetween(120f, 190f);
        assertThat(placement.dateX()).isGreaterThan(placement.signatureX());
    }

    private ElectronicSignatureMapper.RequestRow requestRow(String status, String signedStorageKey, LocalDateTime signedAt) {
        ElectronicSignatureMapper.RequestRow row = new ElectronicSignatureMapper.RequestRow(); row.setId(91L); row.setEntityType("lease"); row.setEntityId(12L);
        row.setSignerName("李建偉"); row.setSignerEmail("tenant@example.test"); row.setStatus(status); row.setOriginalName("lease.pdf"); row.setStorageKey("12/original.pdf"); row.setMimeType("application/pdf");
        row.setVerificationCodeHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456")); row.setVerificationExpiresAt(LocalDateTime.of(2026, 7, 23, 8, 10)); row.setExpiresAt(LocalDateTime.of(2026, 7, 30, 8, 0));
        row.setSignedStorageKey(signedStorageKey); row.setSignedOriginalName("lease-已簽署.pdf"); row.setSignedAt(signedAt); return row;
    }

    private String signatureDataUrl() throws Exception {
        BufferedImage image = new BufferedImage(120, 40, BufferedImage.TYPE_INT_ARGB); image.createGraphics().drawLine(5, 20, 110, 20);
        ByteArrayOutputStream output = new ByteArrayOutputStream(); ImageIO.write(image, "png", output);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    }
}
