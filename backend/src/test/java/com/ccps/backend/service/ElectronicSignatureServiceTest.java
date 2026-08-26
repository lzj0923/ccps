package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.ElectronicSignatureStartRequest;
import com.ccps.backend.dto.ElectronicSignatureSignRequest;
import com.ccps.backend.dto.ElectronicSignaturePackageRequest;
import com.ccps.backend.dto.ElectronicSignatureParticipantRequest;
import com.ccps.backend.dto.ElectronicSignatureStartResponse;
import com.ccps.backend.mapper.ElectronicSignatureMapper;
import com.ccps.backend.mapper.ElectronicSignatureMapper.NewRequest;
import com.ccps.backend.mapper.ElectronicSignatureMapper.NewNotification;
import com.ccps.backend.mapper.ElectronicSignatureMapper.ParticipantRow;
import com.ccps.backend.mapper.ElectronicSignatureMapper.RequesterRow;
import com.ccps.backend.mapper.ElectronicSignatureMapper.RequestRow;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import javax.imageio.ImageIO;

@ExtendWith(MockitoExtension.class)
class ElectronicSignatureServiceTest {
    @Mock private ElectronicSignatureMapper mapper;
    @Mock private JavaMailSender mailSender;
    @TempDir Path tempDir;

    @Test void startsTheCompanyPmaStepFromTheOwnerSignedPdf() throws Exception {
        Path mandateRoot = tempDir.resolve("rental-mandates");
        Path signatureRoot = tempDir.resolve("signatures");
        Path ownerSigned = signatureRoot.resolve("101/signed-contract.pdf");
        Files.createDirectories(ownerSigned.getParent());
        try (var output = Files.newOutputStream(ownerSigned)) {
            Document pdf = new Document(); PdfWriter.getInstance(pdf, output); pdf.open();
            pdf.add(new Paragraph("Owner signed PMA")); pdf.close();
        }
        ElectronicSignatureMapper.DocumentRow root = new ElectronicSignatureMapper.DocumentRow();
        root.setId(81L); root.setOriginalName("pma.pdf"); root.setStorageKey("12/pma.pdf");
        root.setMimeType("application/pdf"); root.setChecksumSha256("root-hash");
        root.setRelationType("property_management_agreement_draft");
        ElectronicSignatureMapper.DocumentRow previous = new ElectronicSignatureMapper.DocumentRow();
        previous.setId(92L); previous.setOriginalName("pma-owner-signed.pdf"); previous.setStorageKey("101/signed-contract.pdf");
        previous.setMimeType("application/pdf"); previous.setChecksumSha256("owner-signed-hash");
        when(mapper.findMandateDocument(12L, 81L)).thenReturn(root);
        when(mapper.findSignedStepDocument(81L, 1)).thenReturn(previous);
        AtomicReference<NewRequest> stored = new AtomicReference<>();
        when(mapper.insertRequest(any())).thenAnswer(call -> { NewRequest row = call.getArgument(0); row.setId(102L); stored.set(row); return 1; });
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", tempDir.resolve("lease-contracts").toString(), mandateRoot.toString(),
                signatureRoot.toString(), Clock.fixed(Instant.parse("2026-07-23T08:00:00Z"), ZoneOffset.UTC));

        service.startMandateDocument(7L, 12L, 81L,
                new ElectronicSignatureStartRequest("CCPS 公司", "company@example.test", 7, "company"));

        assertThat(stored.get().getRootDocumentId()).isEqualTo(81L);
        assertThat(stored.get().getSourceDocumentId()).isEqualTo(92L);
        assertThat(stored.get().getSignerRole()).isEqualTo("company");
        assertThat(stored.get().getSigningOrder()).isEqualTo(2);
    }

    @Test
    void usesSavedSignatureFieldCoordinatesForGeneratedAuthorizationSignatures() throws Exception {
        RentalManagementTemplatePdfService templateService = new RentalManagementTemplatePdfService(
                tempDir.resolve("templates").toString());
        var initial = templateService.currentLayout(
                RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION);
        var moved = initial.fields().stream()
                .map(field -> "signature.owner".equals(field.fieldKey())
                        ? new RentalManagementTemplatePdfService.TemplateFieldPosition(field.id(), field.fieldKey(),
                                field.label(), field.page(), 222f, 444f, field.fontSize(), field.maxWidth(),
                                field.maxLines(), field.lineHeight())
                        : field)
                .toList();
        templateService.saveLayout(RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION,
                new RentalManagementTemplatePdfService.TemplateLayout(initial.version(), initial.pages(),
                        initial.pageSizes(), moved));
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example",
                "noreply@example.test", "CCPS", "http://localhost:5173", tempDir.resolve("lease").toString(),
                tempDir.resolve("mandates").toString(), tempDir.resolve("signatures").toString(),
                Clock.systemDefaultZone(), templateService);
        Method method = ElectronicSignatureService.class.getDeclaredMethod("signaturePlacements", String.class,
                String.class, PdfReader.class);
        method.setAccessible(true);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph("Page 1"));
            document.newPage();
            document.add(new Paragraph("Page 2"));
            document.newPage();
            document.add(new Paragraph("Page 3"));
            document.close();
            try (PdfReader reader = new PdfReader(output.toByteArray())) {
                @SuppressWarnings("unchecked")
                List<ElectronicSignatureService.SignaturePlacement> placements =
                        (List<ElectronicSignatureService.SignaturePlacement>) method.invoke(service,
                                "management_authorization_draft", "owner", reader);
                assertThat(placements).singleElement().satisfies(placement -> {
                    assertThat(placement.signatureX()).isEqualTo(222f);
                    assertThat(placement.signatureY()).isEqualTo(444f);
                });
            }
        }
    }

    @Test void startsPmaPackageAfterSavingAllSignersAndSupersedingThePreviousRun() throws Exception {
        Path mandateRoot = tempDir.resolve("rental-mandates");
        Path original = mandateRoot.resolve("21/pma.pdf");
        Files.createDirectories(original.getParent());
        try (var output = Files.newOutputStream(original)) {
            Document pdf = new Document(); PdfWriter.getInstance(pdf, output); pdf.open();
            pdf.add(new Paragraph("Property management agreement")); pdf.close();
        }
        ElectronicSignatureMapper.DocumentRow document = new ElectronicSignatureMapper.DocumentRow();
        document.setId(81L); document.setOriginalName("pma.pdf"); document.setStorageKey("21/pma.pdf");
        document.setMimeType("application/pdf"); document.setChecksumSha256("source-hash");
        document.setRelationType("property_management_agreement_draft");
        when(mapper.findMandateDocument(21L, 81L)).thenReturn(document);
        when(mapper.upsertParticipant(any(ParticipantRow.class))).thenReturn(1);
        List<NewRequest> stored = new ArrayList<>();
        when(mapper.insertRequest(any())).thenAnswer(call -> { NewRequest row = call.getArgument(0); row.setId(91L + stored.size()); stored.add(row); return 1; });

        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", tempDir.resolve("lease-contracts").toString(), mandateRoot.toString(),
                tempDir.resolve("signatures").toString(), Clock.fixed(Instant.parse("2026-08-19T01:00:00Z"), ZoneOffset.UTC));
        service.startMandatePackage(7L, 21L, 81L, new ElectronicSignaturePackageRequest(List.of(
                new ElectronicSignatureParticipantRequest("owner", "屋主", "owner@example.test"),
                new ElectronicSignatureParticipantRequest("company", "公司代表", "company@example.test"),
                new ElectronicSignatureParticipantRequest("customer_service", "客服见证人", "witness@example.test")
        ), 7));

        verify(mapper).voidSignedDocumentsForRoot(81L);
        verify(mapper).supersedeRequestsForRoot(81L);
        verify(mapper, times(3)).upsertParticipant(any(ParticipantRow.class));
        assertThat(stored).extracting(NewRequest::getSignerRole)
                .containsExactly("owner", "company", "customer_service");
        assertThat(stored).extracting(NewRequest::getStatus).containsOnly("pending");
        assertThat(stored).extracting(NewRequest::getSourceDocumentId).containsOnly(81L);
        verify(mailSender, times(3)).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test void startsLeasePackageForAllFourPartiesAtTheSameTime() throws Exception {
        Path leaseRoot = tempDir.resolve("lease-contracts");
        Path original = leaseRoot.resolve("18/lease.pdf");
        Files.createDirectories(original.getParent());
        try (var output = Files.newOutputStream(original)) {
            Document pdf = new Document(); PdfWriter.getInstance(pdf, output); pdf.open();
            pdf.add(new Paragraph("Tenancy agreement")); pdf.close();
        }
        ElectronicSignatureMapper.DocumentRow document = new ElectronicSignatureMapper.DocumentRow();
        document.setId(81L); document.setOriginalName("lease.pdf"); document.setStorageKey("18/lease.pdf");
        document.setMimeType("application/pdf"); document.setChecksumSha256("source-hash");
        when(mapper.findLeaseDocument(18L)).thenReturn(document);
        when(mapper.upsertParticipant(any(ParticipantRow.class))).thenReturn(1);
        List<NewRequest> stored = new ArrayList<>();
        when(mapper.insertRequest(any())).thenAnswer(call -> {
            NewRequest row = call.getArgument(0); row.setId(101L + stored.size()); stored.add(row); return 1;
        });

        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", leaseRoot.toString(), tempDir.resolve("rental-mandates").toString(),
                tempDir.resolve("signatures").toString(), Clock.fixed(Instant.parse("2026-08-19T01:00:00Z"), ZoneOffset.UTC));
        ElectronicSignatureStartResponse response = service.startLeasePackage(7L, 18L, new ElectronicSignaturePackageRequest(List.of(
                new ElectronicSignatureParticipantRequest("owner", "房主", "owner@example.test"),
                new ElectronicSignatureParticipantRequest("owner_witness", "房主见证人", "owner-witness@example.test"),
                new ElectronicSignatureParticipantRequest("tenant", "租客", "tenant@example.test"),
                new ElectronicSignatureParticipantRequest("tenant_witness", "租客见证人", "tenant-witness@example.test")
        ), 7));

        verify(mapper).voidSignedDocumentsForRoot(81L);
        verify(mapper).supersedeRequestsForRoot(81L);
        verify(mapper, times(4)).upsertParticipant(any(ParticipantRow.class));
        assertThat(stored).extracting(NewRequest::getSignerRole)
                .containsExactly("owner", "owner_witness", "tenant", "tenant_witness");
        assertThat(stored).extracting(NewRequest::getEntityType).containsOnly("lease");
        assertThat(stored).extracting(NewRequest::getEntityId).containsOnly(18L);
        assertThat(stored).extracting(NewRequest::getStatus).containsOnly("pending");
        assertThat(response.signingLinks()).extracting("signerRole")
                .containsExactly("owner", "owner_witness", "tenant", "tenant_witness");
        assertThat(response.signingLinks()).allSatisfy(link ->
                assertThat(link.signingUrl()).startsWith("http://localhost:5173/sign/"));
        verify(mailSender, times(4)).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test void reportsUnavailableInsteadOfHttp500WhenLeaseInvitationEmailCannotConnect() throws Exception {
        Path leaseRoot = tempDir.resolve("lease-contracts");
        Path original = leaseRoot.resolve("18/lease.pdf");
        Files.createDirectories(original.getParent());
        try (var output = Files.newOutputStream(original)) {
            Document pdf = new Document(); PdfWriter.getInstance(pdf, output); pdf.open();
            pdf.add(new Paragraph("Tenancy agreement")); pdf.close();
        }
        ElectronicSignatureMapper.DocumentRow document = new ElectronicSignatureMapper.DocumentRow();
        document.setId(81L); document.setOriginalName("lease.pdf"); document.setStorageKey("18/lease.pdf");
        document.setMimeType("application/pdf"); document.setChecksumSha256("source-hash");
        when(mapper.findLeaseDocument(18L)).thenReturn(document);
        when(mapper.upsertParticipant(any(ParticipantRow.class))).thenReturn(1);
        AtomicReference<Long> requestId = new AtomicReference<>(101L);
        when(mapper.insertRequest(any())).thenAnswer(call -> {
            call.<NewRequest>getArgument(0).setId(requestId.getAndSet(requestId.get() + 1)); return 1;
        });
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("SMTP unavailable"))
                .when(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.qq.com", "noreply@example.test",
                "CCPS", "http://localhost:5173", leaseRoot.toString(), tempDir.resolve("rental-mandates").toString(),
                tempDir.resolve("signatures").toString(), Clock.systemUTC());

        assertThatThrownBy(() -> service.startLeasePackage(7L, 18L,
                new ElectronicSignaturePackageRequest(List.of(
                        new ElectronicSignatureParticipantRequest("owner", "房主", "owner@example.test"),
                        new ElectronicSignatureParticipantRequest("owner_witness", "房主见证人", "owner-witness@example.test"),
                        new ElectronicSignatureParticipantRequest("tenant", "租客", "tenant@example.test"),
                        new ElectronicSignatureParticipantRequest("tenant_witness", "租客见证人", "tenant-witness@example.test")
                ), 7)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode())
                                .isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test void parallelOtrSignaturesMergeIntoTheLatestDocumentAndCompleteInAnyOrder() throws Exception {
        Path signatureRoot = tempDir.resolve("signatures");
        Path signedStep = signatureRoot.resolve("92/signed-contract.pdf");
        Files.createDirectories(signedStep.getParent());
        try (var output = Files.newOutputStream(signedStep)) {
            Document pdf = new Document(); PdfWriter.getInstance(pdf, output); pdf.open(); pdf.add(new Paragraph("Three OTR signatures")); pdf.close();
        }
        RequestRow pending = requestRow("pending", null, null); pending.setEntityType("rental_mandate"); pending.setEntityId(21L);
        pending.setRootDocumentId(81L); pending.setDocumentKind("otr"); pending.setSignerRole("owner_witness");
        pending.setSigningOrder(4); pending.setSignerName("屋主见证人"); pending.setStorageKey("21/otr.pdf");
        RequestRow signed = requestRow("signed", "91/signed-contract.pdf", LocalDateTime.of(2026, 7, 23, 8, 0));
        signed.setEntityType("rental_mandate"); signed.setEntityId(21L); signed.setRootDocumentId(81L);
        signed.setDocumentKind("otr"); signed.setSignerRole("owner_witness"); signed.setSigningOrder(4);
        ElectronicSignatureMapper.DocumentRow previous = new ElectronicSignatureMapper.DocumentRow(); previous.setId(92L);
        previous.setOriginalName("otr-three-signed.pdf"); previous.setStorageKey("92/signed-contract.pdf");
        previous.setMimeType("application/pdf"); previous.setChecksumSha256("tenant-signed-hash");
        when(mapper.findByTokenHash(any())).thenReturn(pending, signed);
        when(mapper.lockDocument(81L)).thenReturn(81L);
        when(mapper.findLatestActiveSignedDocument(81L)).thenReturn(previous);
        when(mapper.countSignedRolesForRoot(81L)).thenReturn(3);
        when(mapper.insertSignedDocument(any())).thenAnswer(call -> { call.<ElectronicSignatureMapper.NewDocument>getArgument(0).setId(94L); return 1; });
        when(mapper.insertSignedMandateDocumentLink(94L, 21L, "otr_signed")).thenReturn(1);
        when(mapper.completeRequest(any(), any(), any(), any(), any(), any())).thenReturn(1);

        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", tempDir.resolve("lease-contracts").toString(),
                tempDir.resolve("rental-mandates").toString(), signatureRoot.toString(),
                Clock.fixed(Instant.parse("2026-07-23T08:00:00Z"), ZoneOffset.UTC));
        service.sign("a-valid-one-time-token-with-more-than-thirty-two-chars",
                new ElectronicSignatureSignRequest("屋主见证人", "123456", signatureDataUrl(), null, true), "127.0.0.1", "JUnit");

        verify(mapper).insertSignedMandateDocumentLink(94L, 21L, "otr_signed");
        assertThat(signatureRoot.resolve("91/signed-contract.pdf")).isRegularFile();
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
                new ElectronicSignatureSignRequest("李建偉", "123456", signatureDataUrl(), null, true), "127.0.0.1", "JUnit");

        assertThat(result.status()).isEqualTo("signed");
        assertThat(Files.readAllBytes(original)).isEqualTo(originalBytes);
        assertThat(Files.size(tempDir.resolve("signatures/91/signed-contract.pdf"))).isGreaterThan(0);
        verify(mapper).insertEvent(91L, "signed", "Contract signed", "127.0.0.1", "JUnit");
    }

    @Test void ownerSigningNotifiesTheAdminWhoStartedTheRequest() throws Exception {
        Path leaseRoot = tempDir.resolve("lease-contracts");
        Path original = leaseRoot.resolve("12/original.pdf");
        Files.createDirectories(original.getParent());
        try (var output = Files.newOutputStream(original)) {
            Document pdf = new Document(); PdfWriter.getInstance(pdf, output); pdf.open();
            pdf.add(new Paragraph("Owner notification contract")); pdf.close();
        }
        RequestRow pending = requestRow("pending", null, null);
        pending.setSignerRole("owner"); pending.setSignerName("屋主甲"); pending.setRequestedBy(7L);
        RequestRow signed = requestRow("signed", "91/signed-contract.pdf", LocalDateTime.of(2026, 7, 23, 8, 0));
        signed.setSignerRole("owner"); signed.setSignerName("屋主甲"); signed.setRequestedBy(7L);
        when(mapper.findByTokenHash(any())).thenReturn(pending, signed);
        when(mapper.insertSignedDocument(any())).thenAnswer(call -> {
            call.<ElectronicSignatureMapper.NewDocument>getArgument(0).setId(92L); return 1;
        });
        when(mapper.insertSignedDocumentLink(92L, "lease", 12L)).thenReturn(1);
        when(mapper.completeRequest(any(), any(), any(), any(), any(), any())).thenReturn(1);
        RequesterRow requester = new RequesterRow(); requester.setId(7L); requester.setDisplayName("建档管理员");
        requester.setEmail("admin@example.com");
        when(mapper.findRequester(7L)).thenReturn(requester);
        when(mapper.insertSignatureNotification(any())).thenAnswer(call -> {
            call.<NewNotification>getArgument(0).setId(501L); return 1;
        });

        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example",
                "noreply@example.test", "CCPS", "http://localhost:5173", leaseRoot.toString(),
                tempDir.resolve("rental-mandates").toString(), tempDir.resolve("signatures").toString(),
                Clock.fixed(Instant.parse("2026-07-23T08:00:00Z"), ZoneOffset.UTC));

        service.sign("a-valid-one-time-token-with-more-than-thirty-two-chars",
                new ElectronicSignatureSignRequest("屋主甲", "123456", signatureDataUrl(), null, true),
                "127.0.0.1", "JUnit");

        var notification = org.mockito.ArgumentCaptor.forClass(NewNotification.class);
        verify(mapper).insertSignatureNotification(notification.capture());
        assertThat(notification.getValue().getRecipientUserId()).isEqualTo(7L);
        assertThat(notification.getValue().getTitle()).isEqualTo("屋主已完成电子签署");
        assertThat(notification.getValue().getBody()).contains("屋主甲");
        verify(mapper).insertSignatureInAppDelivery(501L);
        verify(mapper).insertSignatureEmailDelivery(501L, "admin@example.com");
    }

    @Test void parallelOtrSignerCanPreviewTheOriginalMandatePdfRegardlessOfSigningOrder() throws Exception {
        Path mandateRoot = tempDir.resolve("rental-mandates");
        Path original = mandateRoot.resolve("25/original-otr.pdf");
        Files.createDirectories(original.getParent()); Files.writeString(original, "otr");
        RequestRow pending = requestRow("pending", null, null); pending.setEntityType("rental_mandate");
        pending.setEntityId(21L); pending.setSourceDocumentId(81L); pending.setRootDocumentId(81L);
        pending.setDocumentKind("otr"); pending.setSignerRole("owner_witness"); pending.setSigningOrder(4);
        pending.setOriginalName("otr.pdf"); pending.setStorageKey("25/original-otr.pdf");
        when(mapper.findByTokenHash(any())).thenReturn(pending);
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", tempDir.resolve("lease-contracts").toString(), mandateRoot.toString(),
                tempDir.resolve("signatures").toString(), Clock.fixed(Instant.parse("2026-07-23T08:00:00Z"), ZoneOffset.UTC));

        ElectronicSignatureService.Download download = service.downloadOriginal(
                "a-valid-one-time-token-with-more-than-thirty-two-chars");

        assertThat(download.path()).isEqualTo(original.toAbsolutePath().normalize());
    }

    @Test void signaturePlacementUsesThePageSignatureAreaInsteadOfFixedRightEdgeCoordinates() {
        var placement = ElectronicSignatureService.signaturePlacement("lease.pdf", 595f, 842f);

        assertThat(placement.signatureX()).isBetween(60f, 120f);
        assertThat(placement.signatureY()).isBetween(120f, 190f);
        assertThat(placement.dateX()).isGreaterThan(placement.signatureX());
    }

    @Test void placesOnlyTenantDirectlyAboveEveryPageNumberAndAllFourPartiesInTheirExecutionBoxes() throws Exception {
        Path template = tempDir.resolve("lease-template.pdf");
        try (var output = Files.newOutputStream(template)) {
            Document pdf = new Document();
            PdfWriter.getInstance(pdf, output); pdf.open();
            for (int page = 1; page <= 22; page++) {
                pdf.add(new Paragraph("Lease template page " + page));
                if (page < 22) pdf.newPage();
            }
            pdf.close();
        }
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", tempDir.resolve("lease-contracts").toString(), tempDir.resolve("rental-mandates").toString(),
                tempDir.resolve("signatures").toString(), Clock.systemUTC());

        var method = ElectronicSignatureService.class.getDeclaredMethod("signaturePlacements", String.class, String.class, PdfReader.class);
        method.setAccessible(true);
        try (PdfReader reader = new PdfReader(Files.readAllBytes(template))) {
            @SuppressWarnings("unchecked")
            List<ElectronicSignatureService.SignaturePlacement> ownerPlacements =
                    (List<ElectronicSignatureService.SignaturePlacement>) method.invoke(service, "lease_contract", "owner", reader);
            @SuppressWarnings("unchecked")
            List<ElectronicSignatureService.SignaturePlacement> tenantPlacements =
                    (List<ElectronicSignatureService.SignaturePlacement>) method.invoke(service, "lease_contract", "tenant", reader);
            @SuppressWarnings("unchecked")
            List<ElectronicSignatureService.SignaturePlacement> ownerWitnessPlacements =
                    (List<ElectronicSignatureService.SignaturePlacement>) method.invoke(service, "lease_contract", "owner_witness", reader);
            @SuppressWarnings("unchecked")
            List<ElectronicSignatureService.SignaturePlacement> tenantWitnessPlacements =
                    (List<ElectronicSignatureService.SignaturePlacement>) method.invoke(service, "lease_contract", "tenant_witness", reader);

            assertThat(ownerPlacements).hasSize(1);
            assertThat(tenantPlacements.stream().filter(placement -> placement.signatureY() == 28f)).hasSize(21)
                    .extracting(ElectronicSignatureService.SignaturePlacement::page)
                    .containsExactlyElementsOf(IntStream.rangeClosed(2, 22).boxed().toList());
            assertThat(tenantPlacements.stream().filter(placement -> placement.signatureY() == 28f))
                    .allSatisfy(placement -> {
                        assertThat(placement.signatureX()).isEqualTo(420f);
                        assertThat(placement.signatureWidth()).isEqualTo(110f);
                        assertThat(placement.signatureHeight()).isEqualTo(42f);
                    });
            assertThat(ownerPlacements.stream().filter(placement -> placement.page() == 12))
                    .singleElement().satisfies(placement -> {
                        assertThat(placement.signatureX()).isEqualTo(375f);
                        assertThat(placement.signatureY()).isEqualTo(640f);
                    });
            assertThat(tenantPlacements.stream().filter(placement -> placement.page() == 12 && placement.signatureY() != 28f))
                    .singleElement().satisfies(placement -> {
                        assertThat(placement.signatureX()).isEqualTo(375f);
                        assertThat(placement.signatureY()).isEqualTo(385f);
                    });
            assertThat(tenantPlacements.stream().filter(placement -> placement.page() == 19 && placement.signatureY() != 28f))
                    .singleElement().satisfies(placement -> {
                        assertThat(placement.signatureX()).isEqualTo(95f);
                        assertThat(placement.signatureY()).isEqualTo(585f);
                    });
            assertThat(ownerWitnessPlacements).singleElement().satisfies(placement -> {
                assertThat(placement.page()).isEqualTo(12);
                assertThat(placement.signatureX()).isEqualTo(72f);
                assertThat(placement.signatureY()).isEqualTo(585f);
            });
            assertThat(tenantWitnessPlacements).hasSize(2);
            assertThat(tenantWitnessPlacements).extracting(ElectronicSignatureService.SignaturePlacement::page)
                    .containsExactly(12, 19);
            assertThat(tenantPlacements.stream().filter(placement -> placement.page() == 22))
                    .singleElement().satisfies(placement -> assertThat(placement.signatureY()).isEqualTo(28f));
        }
    }

    @Test void rendersAllFourLeaseSignaturesForVisualQa() throws Exception {
        String sourcePath = System.getProperty("lease.qa.source",
                Objects.toString(System.getenv("LEASE_QA_SOURCE"), "")).trim();
        String outputPath = System.getProperty("lease.qa.output",
                Objects.toString(System.getenv("LEASE_QA_OUTPUT"), "")).trim();
        if (sourcePath.isEmpty() || outputPath.isEmpty()) return;
        Path source = Path.of(sourcePath); Path output = Path.of(outputPath);
        assertThat(source).isRegularFile(); Files.createDirectories(output.getParent());
        Path ownerSigned = tempDir.resolve("lease-owner-signed.pdf");
        Path ownerWitnessSigned = tempDir.resolve("lease-owner-witness-signed.pdf");
        Path tenantSigned = tempDir.resolve("lease-tenant-signed.pdf");
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", tempDir.resolve("lease-contracts").toString(), tempDir.resolve("rental-mandates").toString(),
                tempDir.resolve("signatures").toString(), Clock.systemUTC());
        Method stampMethod = ElectronicSignatureService.class.getDeclaredMethod("stampSignedPdf",
                Path.class, Path.class, byte[].class, String.class, String.class, LocalDateTime.class, String.class, String.class);
        stampMethod.setAccessible(true);
        byte[] ink = Base64.getDecoder().decode(tallSignatureDataUrl().substring("data:image/png;base64,".length()));
        LocalDateTime signedAt = LocalDateTime.of(2026, 8, 19, 10, 30);
        stampMethod.invoke(service, source, ownerSigned, ink, "房主", "", signedAt, "lease_contract", "owner");
        stampMethod.invoke(service, ownerSigned, ownerWitnessSigned, ink, "业主见证人", "OW-1001", signedAt,
                "lease_contract", "owner_witness");
        stampMethod.invoke(service, ownerWitnessSigned, tenantSigned, ink, "租客", "", signedAt,
                "lease_contract", "tenant");
        stampMethod.invoke(service, tenantSigned, output, ink, "租客见证人", "TW-1002", signedAt,
                "lease_contract", "tenant_witness");
        try (PdfReader signed = new PdfReader(Files.readAllBytes(output))) {
            assertThat(signed.getNumberOfPages()).isEqualTo(22);
        }
    }

    @Test void trimsTheBlankBrowserCanvasAroundAHandwrittenSignature() throws Exception {
        BufferedImage source = new BufferedImage(420, 180, BufferedImage.TYPE_INT_ARGB);
        var graphics = source.createGraphics();
        graphics.setColor(java.awt.Color.BLACK);
        graphics.fillRect(180, 65, 62, 46);
        graphics.dispose();
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        ImageIO.write(source, "png", encoded);

        Method method = ElectronicSignatureService.class.getDeclaredMethod("trimSignatureImage", byte[].class);
        method.setAccessible(true);
        byte[] trimmed = (byte[]) method.invoke(null, encoded.toByteArray());
        BufferedImage result = ImageIO.read(new java.io.ByteArrayInputStream(trimmed));

        assertThat(result.getWidth()).isBetween(62, 82);
        assertThat(result.getHeight()).isBetween(46, 66);
    }

    @Test void rentalAppointmentPlacesOwnerSignatureInsideFirstLandlordBoxWithoutDate() throws Exception {
        Path source = tempDir.resolve("rental-appointment.pdf");
        Path target = tempDir.resolve("rental-appointment-signed.pdf");
        ContractTemplatePdfService templateService = new ContractTemplatePdfService();
        Files.write(source, templateService.generate(ContractTemplatePdfService.TemplateType.AUTHORIZATION,
                templateService.data(Map.ofEntries(
                        Map.entry("caseNo", "RM-20260818-QA"), Map.entry("propertyAddress", "翻斗花园 102"),
                        Map.entry("earnestDeposit", "1000.00"), Map.entry("earnestDepositWords", "One Thousand"),
                        Map.entry("commission", "10"), Map.entry("commissionMonths", "1"),
                        Map.entry("sstPercent", "8"), Map.entry("commissionAmount", "1000.00"),
                        Map.entry("agencyFeeTotal", "1080.00"), Map.entry("startDate", "2026-08-18"),
                        Map.entry("commencementDate", "2027-08-17"), Map.entry("landlordName", "吕志杰"),
                        Map.entry("landlordIdentity", "10086"), Map.entry("landlordAddress", "翻斗花园翻斗街202"),
                        Map.entry("landlordDate", "2026-08-18"), Map.entry("witnessName", "见证人甲"),
                        Map.entry("witnessIdentity", "WITNESS-001"), Map.entry("witnessAddress", "吉隆坡见证街1号"),
                        Map.entry("witnessDate", "2026-08-18")))));
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", tempDir.resolve("lease-contracts").toString(), tempDir.resolve("rental-mandates").toString(),
                tempDir.resolve("signatures").toString(), Clock.systemUTC());

        Method placementsMethod = ElectronicSignatureService.class.getDeclaredMethod(
                "signaturePlacements", String.class, String.class, PdfReader.class);
        placementsMethod.setAccessible(true);
        try (PdfReader reader = new PdfReader(Files.readAllBytes(source))) {
            @SuppressWarnings("unchecked")
            List<ElectronicSignatureService.SignaturePlacement> placements =
                    (List<ElectronicSignatureService.SignaturePlacement>) placementsMethod.invoke(
                            service, "rental_appointment_draft", "owner", reader);

            assertThat(placements).singleElement().satisfies(placement -> {
                assertThat(placement.page()).isEqualTo(1);
                assertThat(placement.signatureX()).isEqualTo(75f);
                assertThat(placement.signatureY()).isEqualTo(260f);
                assertThat(placement.signatureWidth()).isEqualTo(130f);
                assertThat(placement.signatureHeight()).isEqualTo(38f);
                assertThat(placement.dateX()).isZero();
                assertThat(placement.dateY()).isZero();
            });
        }

        Method stampMethod = ElectronicSignatureService.class.getDeclaredMethod("stampSignedPdf",
                Path.class, Path.class, byte[].class, String.class, String.class, LocalDateTime.class, String.class, String.class);
        stampMethod.setAccessible(true);
        stampMethod.invoke(service, source, target,
                Base64.getDecoder().decode(tallSignatureDataUrl().substring("data:image/png;base64,".length())),
                "吕志杰", "10086", LocalDateTime.of(2026, 8, 18, 12, 0), "rental_appointment_draft", "owner");
        try (PdfReader signed = new PdfReader(Files.readAllBytes(target))) {
            String text = new PdfTextExtractor(signed).getTextFromPage(1, true);
            assertThat(text).doesNotContain("電子簽署", "2026-08-18 12:00");
        }
        String qaOutput = System.getProperty("rental.appointment.qa.output", "").trim();
        if (!qaOutput.isEmpty()) {
            Path qaTarget = Path.of(qaOutput).toAbsolutePath().normalize();
            Files.createDirectories(qaTarget.getParent());
            Files.copy(target, qaTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test void terminationLetterPlacesOnlyTheOwnerSignatureOnPageTwo() throws Exception {
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", tempDir.resolve("lease-contracts").toString(), tempDir.resolve("rental-mandates").toString(),
                tempDir.resolve("signatures").toString(), Clock.systemUTC());
        Method placementsMethod = ElectronicSignatureService.class.getDeclaredMethod(
                "signaturePlacements", String.class, String.class, PdfReader.class);
        placementsMethod.setAccessible(true);
        try (var input = getClass().getResourceAsStream("/contract-templates/ccps-termination-letter-v1.pdf");
                PdfReader reader = new PdfReader(input)) {
            @SuppressWarnings("unchecked")
            List<ElectronicSignatureService.SignaturePlacement> placements =
                    (List<ElectronicSignatureService.SignaturePlacement>) placementsMethod.invoke(
                            service, "termination_letter_draft", "owner", reader);
            assertThat(placements).singleElement().satisfies(placement -> {
                assertThat(placement.page()).isEqualTo(2);
                assertThat(placement.signatureX()).isEqualTo(54f);
                assertThat(placement.signatureY()).isEqualTo(250f);
                assertThat(placement.signatureWidth()).isEqualTo(165f);
                assertThat(placement.signatureHeight()).isEqualTo(32f);
                assertThat(placement.dateX()).isZero();
                assertThat(placement.dateY()).isZero();
            });
        }
    }

    @Test void otrRequiresTenantSigningAndPlacesInkInsideTheTenantSignatureBox() throws Exception {
        ContractTemplatePdfService templateService = new ContractTemplatePdfService();
        Path source = tempDir.resolve("otr.pdf");
        Path tenantSigned = tempDir.resolve("otr-tenant-signed.pdf");
        Path tenantWitnessSigned = tempDir.resolve("otr-tenant-witness-signed.pdf");
        Path ownerSigned = tempDir.resolve("otr-owner-signed.pdf");
        Path target = tempDir.resolve("otr-all-signed.pdf");
        Files.write(source, templateService.generate(ContractTemplatePdfService.TemplateType.OTR,
                templateService.data(Map.ofEntries(
                        Map.entry("caseNo", "CASE-OTR-001"), Map.entry("propertyAddress", "Unit 102, Fair Residence"),
                        Map.entry("landlordName", "Jane Owner"), Map.entry("tenantName", "John Tenant"),
                        Map.entry("advanceRental", "3500"), Map.entry("securityDepositMonths", "2"),
                        Map.entry("securityDeposit", "7000"), Map.entry("utilityDepositMonths", "0.5"),
                        Map.entry("utilityDeposit", "1750"), Map.entry("stampingFee", "100"),
                        Map.entry("totalBeforeKeys", "12350"), Map.entry("periodYears", "1"),
                        Map.entry("renewalYears", "1"), Map.entry("commencementDate", "2026-09-01"),
                        Map.entry("earnestDeposit", "3500"), Map.entry("tenantIdentity", "P1234567"),
                        Map.entry("tenantDate", "2026-08-18"), Map.entry("landlordIdentity", "900101-01-1234"),
                        Map.entry("landlordDate", "2026-08-18"), Map.entry("tenantWitnessName", "Tenant Witness"),
                        Map.entry("tenantWitnessIdentity", "TW-1"), Map.entry("tenantWitnessDate", "2026-08-18"),
                        Map.entry("landlordWitnessName", "Owner Witness"), Map.entry("landlordWitnessIdentity", "OW-1"),
                        Map.entry("landlordWitnessDate", "2026-08-18"), Map.entry("otherConditions", "Nil")))));
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example",
                "noreply@example.test", "CCPS", "http://localhost:5173",
                tempDir.resolve("lease-contracts").toString(), tempDir.resolve("rental-mandates").toString(),
                tempDir.resolve("signatures").toString(), Clock.systemUTC());

        Method planMethod = ElectronicSignatureService.class.getDeclaredMethod(
                "mandateSigningPlan", String.class, String.class);
        planMethod.setAccessible(true);
        List<ElectronicSignatureService.SigningPlan> plans = List.of(
                (ElectronicSignatureService.SigningPlan) planMethod.invoke(service, "otr", "tenant"),
                (ElectronicSignatureService.SigningPlan) planMethod.invoke(service, "otr", "tenant_witness"),
                (ElectronicSignatureService.SigningPlan) planMethod.invoke(service, "otr", "owner"),
                (ElectronicSignatureService.SigningPlan) planMethod.invoke(service, "otr", "owner_witness"));
        assertThat(plans).extracting(ElectronicSignatureService.SigningPlan::documentKind)
                .containsOnly("otr");
        assertThat(plans).extracting(ElectronicSignatureService.SigningPlan::signerRole)
                .containsExactly("tenant", "tenant_witness", "owner", "owner_witness");
        assertThat(plans).extracting(ElectronicSignatureService.SigningPlan::signingOrder)
                .containsExactly(1, 2, 3, 4);
        assertThat(plans).extracting(ElectronicSignatureService.SigningPlan::totalSteps)
                .containsOnly(4);

        Method relationMethod = ElectronicSignatureService.class.getDeclaredMethod(
                "signedRelation", ElectronicSignatureMapper.RequestRow.class, boolean.class);
        relationMethod.setAccessible(true);
        ElectronicSignatureMapper.RequestRow row = new ElectronicSignatureMapper.RequestRow();
        row.setDocumentKind("otr"); row.setSigningOrder(4);
        assertThat(relationMethod.invoke(service, row, false)).isEqualTo("otr_partial");
        assertThat(relationMethod.invoke(service, row, true)).isEqualTo("otr_signed");

        Method placementsMethod = ElectronicSignatureService.class.getDeclaredMethod(
                "signaturePlacements", String.class, String.class, PdfReader.class);
        placementsMethod.setAccessible(true);
        try (PdfReader reader = new PdfReader(Files.readAllBytes(source))) {
            assertOtrPlacement(placementsMethod, service, reader, "tenant", 115f, 365f, 150f, 32f);
            assertOtrPlacement(placementsMethod, service, reader, "tenant_witness", 115f, 160f, 150f, 28f);
            assertOtrPlacement(placementsMethod, service, reader, "owner", 340f, 365f, 150f, 32f);
            assertOtrPlacement(placementsMethod, service, reader, "owner_witness", 340f, 160f, 150f, 28f);
        }

        Method stampMethod = ElectronicSignatureService.class.getDeclaredMethod("stampSignedPdf",
                Path.class, Path.class, byte[].class, String.class, String.class, LocalDateTime.class,
                String.class, String.class);
        stampMethod.setAccessible(true);
        byte[] ink = Base64.getDecoder().decode(tallSignatureDataUrl().substring("data:image/png;base64,".length()));
        stampMethod.invoke(service, source, tenantSigned, ink, "John Tenant", "P1234567",
                LocalDateTime.of(2026, 8, 18, 12, 0), "otr", "tenant");
        stampMethod.invoke(service, tenantSigned, tenantWitnessSigned, ink, "Tenant Witness", "TW-1",
                LocalDateTime.of(2026, 8, 18, 12, 5), "otr", "tenant_witness");
        stampMethod.invoke(service, tenantWitnessSigned, ownerSigned, ink, "Jane Owner", "900101-01-1234",
                LocalDateTime.of(2026, 8, 18, 12, 10), "otr", "owner");
        stampMethod.invoke(service, ownerSigned, target, ink, "Owner Witness", "OW-1",
                LocalDateTime.of(2026, 8, 18, 12, 15), "otr", "owner_witness");
        try (PdfReader signed = new PdfReader(Files.readAllBytes(target))) {
            String text = new PdfTextExtractor(signed).getTextFromPage(1, true);
            assertThat(text).doesNotContain("電子簽署");
        }
        String qaOutput = System.getProperty("otr.signature.qa.output", "").trim();
        if (!qaOutput.isEmpty()) Files.copy(target, Path.of(qaOutput), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @SuppressWarnings("unchecked")
    private void assertOtrPlacement(Method placementsMethod, ElectronicSignatureService service, PdfReader reader,
            String role, float x, float y, float width, float height) throws Exception {
        List<ElectronicSignatureService.SignaturePlacement> placements =
                (List<ElectronicSignatureService.SignaturePlacement>) placementsMethod.invoke(service, "otr", role, reader);
        assertThat(placements).singleElement().satisfies(placement -> {
            assertThat(placement.page()).isEqualTo(1);
            assertThat(placement.signatureX()).isEqualTo(x);
            assertThat(placement.signatureY()).isEqualTo(y);
            assertThat(placement.signatureWidth()).isEqualTo(width);
            assertThat(placement.signatureHeight()).isEqualTo(height);
            assertThat(placement.dateX()).isZero();
            assertThat(placement.dateY()).isZero();
        });
    }

    @Test void pmaUsesSignatureOnlyAndPlacesBothWitnessSignaturesAboveTheirLines() throws Exception {
        Path source = tempDir.resolve("pma.pdf");
        Path target = tempDir.resolve("pma-signed.pdf");
        Path witnessTarget = tempDir.resolve("pma-witness-signed.pdf");
        try (var output = Files.newOutputStream(source)) {
            Document pdf = new Document();
            PdfWriter.getInstance(pdf, output); pdf.open();
            for (int page = 1; page <= 9; page++) {
                pdf.add(new Paragraph("PMA page " + page));
                if (page < 9) pdf.newPage();
            }
            pdf.close();
        }
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", tempDir.resolve("lease-contracts").toString(), tempDir.resolve("rental-mandates").toString(),
                tempDir.resolve("signatures").toString(), Clock.systemUTC());

        Method placementsMethod = ElectronicSignatureService.class.getDeclaredMethod(
                "signaturePlacements", String.class, String.class, PdfReader.class);
        placementsMethod.setAccessible(true);
        try (PdfReader reader = new PdfReader(Files.readAllBytes(source))) {
            @SuppressWarnings("unchecked")
            List<ElectronicSignatureService.SignaturePlacement> witnesses =
                    (List<ElectronicSignatureService.SignaturePlacement>) placementsMethod.invoke(
                            service, "property_management_agreement_draft", "customer_service", reader);
            assertThat(witnesses).hasSize(2);
            assertThat(witnesses).extracting(ElectronicSignatureService.SignaturePlacement::signatureY)
                    .containsExactly(464f, 190f);
            assertThat(witnesses).allSatisfy(placement -> {
                assertThat(placement.signatureX()).isEqualTo(33f);
                assertThat(placement.signatureWidth()).isEqualTo(92f);
                assertThat(placement.signatureHeight()).isEqualTo(27f);
                assertThat(placement.dateX()).isZero();
                assertThat(placement.dateY()).isZero();
            });
            for (String role : List.of("owner", "company")) {
                @SuppressWarnings("unchecked")
                List<ElectronicSignatureService.SignaturePlacement> signatures =
                        (List<ElectronicSignatureService.SignaturePlacement>) placementsMethod.invoke(
                                service, "property_management_agreement_draft", role, reader);
                assertThat(signatures).allSatisfy(placement -> {
                    assertThat(placement.signatureX()).isEqualTo(246f);
                    assertThat(placement.signatureWidth()).isEqualTo(118f);
                    assertThat(placement.signatureHeight()).isEqualTo(34f);
                    assertThat(placement.dateX()).isZero();
                    assertThat(placement.dateY()).isZero();
                });
            }
        }

        Method stampMethod = ElectronicSignatureService.class.getDeclaredMethod("stampSignedPdf",
                Path.class, Path.class, byte[].class, String.class, String.class, LocalDateTime.class, String.class, String.class);
        stampMethod.setAccessible(true);
        stampMethod.invoke(service, source, target, Base64.getDecoder().decode(signatureDataUrl().substring("data:image/png;base64,".length())),
                "吕志杰", "10086", LocalDateTime.of(2026, 8, 18, 10, 15), "property_management_agreement_draft", "owner");
        try (PdfReader signed = new PdfReader(Files.readAllBytes(target))) {
            String text = new PdfTextExtractor(signed).getTextFromPage(7, true);
            assertThat(text).doesNotContain("電子簽署", "吕志杰", "2026-08-18");
        }

        stampMethod.invoke(service, source, witnessTarget,
                Base64.getDecoder().decode(signatureDataUrl().substring("data:image/png;base64,".length())),
                "Witness Name", "WITNESS-9001", LocalDateTime.of(2026, 8, 18, 10, 17),
                "property_management_agreement_draft", "customer_service");
        try (PdfReader signed = new PdfReader(Files.readAllBytes(witnessTarget))) {
            String text = new PdfTextExtractor(signed).getTextFromPage(7, true);
            assertThat(text).doesNotContain("電子簽署", "2026-08-18");
            assertThat(text.split("WITNESS-9001", -1)).hasSize(3);
            String pageContent = new String(signed.getPageContent(7), java.nio.charset.StandardCharsets.ISO_8859_1);
            assertThat(pageContent).contains("104 572 Tm", "104 249 Tm", "61 416 Tm", "61 142 Tm");
        }
    }

    @Test void managementAuthorizationKeepsTrimmedSignatureAboveTheLineAndOmitsSigningDate() throws Exception {
        Path source = tempDir.resolve("management-authorization.pdf");
        Path target = tempDir.resolve("management-authorization-signed.pdf");
        try (var output = Files.newOutputStream(source)) {
            Document pdf = new Document();
            PdfWriter.getInstance(pdf, output); pdf.open();
            for (int page = 1; page <= 3; page++) {
                pdf.add(new Paragraph("Management authorization page " + page));
                if (page < 3) pdf.newPage();
            }
            pdf.close();
        }
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", tempDir.resolve("lease-contracts").toString(), tempDir.resolve("rental-mandates").toString(),
                tempDir.resolve("signatures").toString(), Clock.systemUTC());

        Method placementsMethod = ElectronicSignatureService.class.getDeclaredMethod(
                "signaturePlacements", String.class, String.class, PdfReader.class);
        placementsMethod.setAccessible(true);
        try (PdfReader reader = new PdfReader(Files.readAllBytes(source))) {
            @SuppressWarnings("unchecked")
            List<ElectronicSignatureService.SignaturePlacement> placements =
                    (List<ElectronicSignatureService.SignaturePlacement>) placementsMethod.invoke(
                            service, "management_authorization_draft", "owner", reader);
            assertThat(placements).singleElement().satisfies(placement -> {
                assertThat(placement.page()).isEqualTo(3);
                assertThat(placement.signatureX()).isEqualTo(72f);
                assertThat(placement.signatureY()).isEqualTo(382f);
                assertThat(placement.signatureWidth()).isEqualTo(112f);
                assertThat(placement.signatureHeight()).isEqualTo(24f);
                assertThat(placement.dateX()).isZero();
                assertThat(placement.dateY()).isZero();
            });
        }

        Method stampMethod = ElectronicSignatureService.class.getDeclaredMethod("stampSignedPdf",
                Path.class, Path.class, byte[].class, String.class, String.class, LocalDateTime.class, String.class, String.class);
        stampMethod.setAccessible(true);
        stampMethod.invoke(service, source, target,
                Base64.getDecoder().decode(tallSignatureDataUrl().substring("data:image/png;base64,".length())),
                "吕志杰", "10086", LocalDateTime.of(2026, 8, 18, 12, 0), "management_authorization_draft", "owner");
        try (PdfReader signed = new PdfReader(Files.readAllBytes(target))) {
            String text = new PdfTextExtractor(signed).getTextFromPage(3, true);
            assertThat(text).doesNotContain("電子簽署", "吕志杰", "2026-08-18");
        }
        String qaOutput = System.getProperty("management.authorization.qa.output", "").trim();
        if (!qaOutput.isEmpty()) Files.copy(target, Path.of(qaOutput), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @Test void rentalRemittanceUsesTheOwnerSignatureLineOnPageOne() throws Exception {
        Path source = tempDir.resolve("rental-remittance.pdf");
        try (var output = Files.newOutputStream(source)) {
            Document pdf = new Document();
            PdfWriter.getInstance(pdf, output);
            pdf.open();
            pdf.add(new Paragraph("Rental remittance"));
            pdf.close();
        }
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", tempDir.resolve("lease-contracts").toString(), tempDir.resolve("rental-mandates").toString(),
                tempDir.resolve("signatures").toString(), Clock.systemUTC());
        Method placementsMethod = ElectronicSignatureService.class.getDeclaredMethod("signaturePlacements", String.class, String.class, PdfReader.class);
        placementsMethod.setAccessible(true);
        try (PdfReader reader = new PdfReader(Files.readAllBytes(source))) {
            @SuppressWarnings("unchecked")
            List<ElectronicSignatureService.SignaturePlacement> placements =
                    (List<ElectronicSignatureService.SignaturePlacement>) placementsMethod.invoke(service, "rental_remittance_draft", "owner", reader);
            assertThat(placements).singleElement().satisfies(placement -> {
                assertThat(placement.page()).isEqualTo(1);
                assertThat(placement.signatureX()).isEqualTo(72f);
                assertThat(placement.signatureY()).isEqualTo(116f);
                assertThat(placement.signatureWidth()).isEqualTo(112f);
                assertThat(placement.signatureHeight()).isEqualTo(34f);
                assertThat(placement.dateX()).isZero();
                assertThat(placement.dateY()).isZero();
            });
        }
    }

    @Test void pmaRealTemplateKeepsWitnessDetailsInBothWitnessBlocks() throws Exception {
        Path source = Path.of(System.getProperty("user.dir"),
                "src/main/resources/contract-templates/ccps-pma-v1.pdf");
        if (!Files.isRegularFile(source)) {
            source = Path.of(System.getProperty("user.dir"),
                    "backend/src/main/resources/contract-templates/ccps-pma-v1.pdf");
        }
        assertThat(source).isRegularFile();
        Path ownerTarget = tempDir.resolve("pma-owner.pdf");
        Path companyTarget = tempDir.resolve("pma-company.pdf");
        Path finalTarget = tempDir.resolve("pma-final.pdf");
        ElectronicSignatureService service = new ElectronicSignatureService(mapper, mailSender, "smtp.example", "noreply@example.test",
                "CCPS", "http://localhost:5173", tempDir.resolve("lease-contracts").toString(), tempDir.resolve("rental-mandates").toString(),
                tempDir.resolve("signatures").toString(), Clock.systemUTC());
        Method stampMethod = ElectronicSignatureService.class.getDeclaredMethod("stampSignedPdf",
                Path.class, Path.class, byte[].class, String.class, String.class, LocalDateTime.class, String.class, String.class);
        stampMethod.setAccessible(true);
        byte[] ink = Base64.getDecoder().decode(tallSignatureDataUrl().substring("data:image/png;base64,".length()));
        LocalDateTime signedAt = LocalDateTime.of(2026, 8, 18, 10, 15);
        stampMethod.invoke(service, source, ownerTarget, ink, "业主", "", signedAt,
                "property_management_agreement_draft", "owner");
        stampMethod.invoke(service, ownerTarget, companyTarget, ink, "代管公司", "", signedAt,
                "property_management_agreement_draft", "company");
        stampMethod.invoke(service, companyTarget, finalTarget, ink, "Witness Name", "WITNESS-9001", signedAt,
                "property_management_agreement_draft", "customer_service");

        try (PdfReader signed = new PdfReader(Files.readAllBytes(finalTarget))) {
            String text = new PdfTextExtractor(signed).getTextFromPage(7, true);
            assertThat(text).doesNotContain("電子簽署", "2026-08-18 10:15");
            assertThat(text.split("WITNESS-9001", -1)).hasSize(3);
            String pageContent = new String(signed.getPageContent(7), java.nio.charset.StandardCharsets.ISO_8859_1);
            assertThat(pageContent).contains("104 572 Tm", "104 249 Tm", "61 416 Tm", "61 142 Tm");
        }
        String qaOutput = System.getProperty("pma.qa.output", "").trim();
        if (!qaOutput.isEmpty()) {
            Path qaTarget = Path.of(qaOutput).toAbsolutePath().normalize();
            Files.createDirectories(qaTarget.getParent());
            Files.copy(finalTarget, qaTarget, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
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

    private String tallSignatureDataUrl() throws Exception {
        BufferedImage image = new BufferedImage(180, 90, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setColor(new Color(18, 65, 125));
        graphics.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(12, 70, 42, 26);
        graphics.drawLine(42, 26, 55, 76);
        graphics.drawLine(55, 76, 75, 42);
        graphics.drawLine(75, 42, 91, 72);
        graphics.drawLine(91, 72, 116, 35);
        graphics.drawLine(22, 67, 161, 61);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream(); ImageIO.write(image, "png", output);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
    }
}
