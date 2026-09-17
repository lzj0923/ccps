package com.ccps.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import javax.imageio.ImageIO;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.ElectronicSignaturePublicResponse;
import com.ccps.backend.dto.ElectronicSignaturePackageRequest;
import com.ccps.backend.dto.ElectronicSignatureParticipantRequest;
import com.ccps.backend.dto.ElectronicSignatureParticipantResponse;
import com.ccps.backend.dto.ElectronicSignatureSignRequest;
import com.ccps.backend.dto.ElectronicSignatureStartRequest;
import com.ccps.backend.dto.ElectronicSignatureStartResponse;
import com.ccps.backend.dto.ElectronicSignatureLinkResponse;
import com.ccps.backend.mapper.ElectronicSignatureMapper;
import com.ccps.backend.mapper.ElectronicSignatureMapper.DocumentRow;
import com.ccps.backend.mapper.ElectronicSignatureMapper.NewDocument;
import com.ccps.backend.mapper.ElectronicSignatureMapper.NewNotification;
import com.ccps.backend.mapper.ElectronicSignatureMapper.NewRequest;
import com.ccps.backend.mapper.ElectronicSignatureMapper.ParticipantRow;
import com.ccps.backend.mapper.ElectronicSignatureMapper.RequesterRow;
import com.ccps.backend.mapper.ElectronicSignatureMapper.RequestRow;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

@Service
public class ElectronicSignatureService {
    private static final int DEFAULT_EXPIRY_DAYS = 7;
    private static final String VERIFICATION_NOT_REQUIRED = "not-required";
    private static final long MAX_SIGNATURE_BYTES = 1024L * 1024L;

    private final ElectronicSignatureMapper mapper;
    private final JavaMailSender mailSender;
    private final String mailHost;
    private final String fromAddress;
    private final String senderName;
    private final String frontendBase;
    private final Path leaseRoot;
    private final Path mandateRoot;
    private final Path signatureRoot;
    private final Clock clock;
    private final RentalManagementTemplatePdfService templateService;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public ElectronicSignatureService(ElectronicSignatureMapper mapper, ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.host:}") String mailHost, @Value("${ccps.mail.from:}") String fromAddress,
            @Value("${ccps.mail.sender-name:CCPS 家慶佳業}") String senderName,
            @Value("${ccps.signing.frontend-base:http://localhost:5173}") String frontendBase,
            @Value("${ccps.storage.lease-contracts:uploads/lease-contracts}") String leaseRoot,
             @Value("${ccps.storage.rental-mandates:uploads/rental-mandates}") String mandateRoot,
             @Value("${ccps.storage.electronic-signatures:uploads/electronic-signatures}") String signatureRoot,
             ObjectProvider<RentalManagementTemplatePdfService> templateServiceProvider) {
        this(mapper, mailSenderProvider.getIfAvailable(), mailHost, fromAddress, senderName, frontendBase,
                leaseRoot, mandateRoot, signatureRoot, Clock.systemDefaultZone(), templateServiceProvider.getIfAvailable());
    }

    ElectronicSignatureService(ElectronicSignatureMapper mapper, JavaMailSender mailSender, String mailHost,
            String fromAddress, String senderName, String frontendBase, String leaseRoot, String mandateRoot,
            String signatureRoot, Clock clock) {
        this(mapper, mailSender, mailHost, fromAddress, senderName, frontendBase, leaseRoot, mandateRoot,
                signatureRoot, clock, null);
    }

    ElectronicSignatureService(ElectronicSignatureMapper mapper, JavaMailSender mailSender, String mailHost,
            String fromAddress, String senderName, String frontendBase, String leaseRoot, String mandateRoot,
            String signatureRoot, Clock clock, RentalManagementTemplatePdfService templateService) {
        this.mapper = mapper; this.mailSender = mailSender; this.mailHost = mailHost; this.fromAddress = fromAddress;
        this.senderName = senderName; this.frontendBase = frontendBase.replaceAll("/+$", "");
        this.leaseRoot = Path.of(leaseRoot).toAbsolutePath().normalize();
        this.mandateRoot = Path.of(mandateRoot).toAbsolutePath().normalize();
        this.signatureRoot = Path.of(signatureRoot).toAbsolutePath().normalize(); this.clock = clock;
        this.templateService = templateService;
    }

    @Transactional(readOnly = true)
    public List<ElectronicSignatureParticipantResponse> leaseParticipants(Long leaseId) {
        DocumentRow document = mapper.findLeaseDocument(leaseId);
        if (document == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract document not found");
        return mapper.findParticipants(document.getId());
    }

    @Transactional
    public ElectronicSignatureStartResponse startLeasePackage(Long actorId, Long leaseId,
            ElectronicSignaturePackageRequest request) {
        DocumentRow document = mapper.findLeaseDocument(leaseId);
        if (document == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract document not found");
        return startPackage(actorId, "lease", leaseId, document, "lease_contract", request);
    }

    @Transactional
    public ElectronicSignatureStartResponse startMandateDocument(Long actorId, Long mandateId, Long documentId,
            ElectronicSignatureStartRequest request) {
        DocumentRow document = mapper.findMandateDocument(mandateId, documentId);
        if (document == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract document not found");
        return start(actorId, "rental_mandate", mandateId, document, request,
                mandateSigningPlan(document.getRelationType(), request.signerRole()));
    }

    @Transactional(readOnly = true)
    public List<ElectronicSignatureParticipantResponse> mandateParticipants(Long mandateId, Long documentId) {
        DocumentRow document = mapper.findMandateDocument(mandateId, documentId);
        if (document == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract document not found");
        return mapper.findParticipants(document.getId());
    }

    @Transactional
    public ElectronicSignatureStartResponse startMandatePackage(Long actorId, Long mandateId, Long documentId,
            ElectronicSignaturePackageRequest request) {
        DocumentRow document = mapper.findMandateDocument(mandateId, documentId);
        if (document == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract document not found");
        document = upgradeLegacyOtr(actorId, mandateId, document);
        String kind = Objects.toString(document.getRelationType(), "").toLowerCase(Locale.ROOT);
        return startPackage(actorId, "rental_mandate", mandateId, document, kind, request);
    }

    /**
     * Historical OTR records stored only the offer page. Reissuing a signing link must
     * upgrade that source file first; otherwise every new link still opens the same
     * one-page legacy PDF even though the current template is already two pages.
     */
    private DocumentRow upgradeLegacyOtr(Long actorId, Long mandateId, DocumentRow document) {
        if (!"otr".equalsIgnoreCase(document.getRelationType())
                || !"application/pdf".equalsIgnoreCase(document.getMimeType())) return document;
        Path otr = resolveSourceDocument("rental_mandate", document.getStorageKey(), false);
        if (!Files.isRegularFile(otr)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract file is unavailable");
        }
        try (PdfReader reader = new PdfReader(Files.readAllBytes(otr))) {
            if (reader.getNumberOfPages() >= 2) return document;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The historical OTR file is invalid and must be regenerated", exception);
        }

        DocumentRow appointment = mapper.findCurrentMandateDocumentByRelation(mandateId,
                "rental_appointment_draft");
        if (appointment == null || !"application/pdf".equalsIgnoreCase(appointment.getMimeType())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The appointment page is missing; regenerate the OTR before creating signing links");
        }
        Path appointmentFile = resolveSourceDocument("rental_mandate", appointment.getStorageKey(), false);
        if (!Files.isRegularFile(appointmentFile)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The appointment page is unavailable; regenerate the OTR before creating signing links");
        }

        String token = java.util.UUID.randomUUID().toString().replace("-", "");
        Path target = mandateRoot.resolve(String.valueOf(mandateId)).resolve(token + ".pdf").normalize();
        if (!target.startsWith(mandateRoot)) throw bad("Invalid contract path");
        try {
            byte[] merged = mergeLegacyOtr(otr, appointmentFile);
            Files.createDirectories(target.getParent());
            Files.write(target, merged);
            NewDocument replacement = new NewDocument();
            replacement.setDocumentNo("RM-DOC-" + token.substring(0, 10).toUpperCase(Locale.ROOT));
            replacement.setOriginalName(document.getOriginalName());
            replacement.setStorageKey(mandateRoot.relativize(target).toString().replace('\\', '/'));
            replacement.setFileSize((long) merged.length);
            replacement.setChecksumSha256(sha256(merged));
            replacement.setUploadedBy(actorId);
            if (mapper.insertMandateSourceDocument(replacement, "otr") != 1 || replacement.getId() == null
                    || mapper.insertMandateSourceDocumentLink(replacement.getId(), mandateId, "otr") != 1) {
                throw conflict("Unable to upgrade the historical OTR document");
            }
            mapper.voidSignedDocumentsForRoot(document.getId());
            mapper.supersedeRequestsForRoot(document.getId());
            if (mapper.supersedeMandateSourceDocument(document.getId()) != 1) {
                throw conflict("Unable to replace the historical OTR document");
            }
            DocumentRow upgraded = new DocumentRow();
            upgraded.setId(replacement.getId());
            upgraded.setOriginalName(replacement.getOriginalName());
            upgraded.setStorageKey(replacement.getStorageKey());
            upgraded.setMimeType("application/pdf");
            upgraded.setFileSize(replacement.getFileSize());
            upgraded.setChecksumSha256(replacement.getChecksumSha256());
            upgraded.setRelationType("otr");
            return upgraded;
        } catch (IOException | DocumentException exception) {
            try { Files.deleteIfExists(target); } catch (IOException ignored) { }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to upgrade the historical OTR document", exception);
        }
    }

    private byte[] mergeLegacyOtr(Path otr, Path appointment) throws IOException, DocumentException {
        try (PdfReader offerReader = new PdfReader(Files.readAllBytes(otr));
                PdfReader appointmentReader = new PdfReader(Files.readAllBytes(appointment));
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (offerReader.getNumberOfPages() != 1 || appointmentReader.getNumberOfPages() < 1) {
                throw new DocumentException("Unexpected OTR packet page count");
            }
            Document packet = new Document();
            PdfCopy copy = new PdfCopy(packet, output);
            packet.open();
            copy.addPage(copy.getImportedPage(offerReader, 1));
            copy.addPage(copy.getImportedPage(appointmentReader, 1));
            packet.close();
            return output.toByteArray();
        }
    }

    private ElectronicSignatureStartResponse startPackage(Long actorId, String entityType, Long entityId,
            DocumentRow document, String kind, ElectronicSignaturePackageRequest request) {
        List<String> roles = signingPackageRoles(kind, request);
        if (roles.isEmpty()) throw bad("This document does not use a multi-signer package");
        Map<String, ElectronicSignatureParticipantRequest> byRole = new LinkedHashMap<>();
        for (ElectronicSignatureParticipantRequest signer : request.signers()) {
            String role = Objects.toString(signer.signerRole(), "").trim().toLowerCase(Locale.ROOT);
            if (!roles.contains(role) || byRole.putIfAbsent(role, signer) != null) {
                throw bad("The signer package contains an invalid or duplicate role");
            }
        }
        if (byRole.size() != roles.size()) throw bad("All signer details are required before starting the package");
        int expiryDays = request.expiresInDays() == null ? DEFAULT_EXPIRY_DAYS : request.expiresInDays();

        mapper.voidSignedDocumentsForRoot(document.getId());
        mapper.supersedeRequestsForRoot(document.getId());
        if (!"application/pdf".equalsIgnoreCase(document.getMimeType())) throw bad("Only PDF contracts can be signed online");
        Path source = resolveSourceDocument(entityType, document.getStorageKey(), false);
        if (!Files.isRegularFile(source)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract file is unavailable");
        LocalDateTime now = LocalDateTime.now(clock);
        List<Invitation> invitations = new ArrayList<>();
        for (int index = 0; index < roles.size(); index++) {
            String role = roles.get(index); ElectronicSignatureParticipantRequest signer = byRole.get(role);
            ParticipantRow participant = new ParticipantRow(); participant.setRootDocumentId(document.getId());
            participant.setDocumentKind(kind); participant.setSignerRole(role); participant.setSigningOrder(index + 1);
            participant.setSignerName(trim(signer.signerName(), "Signer name"));
            participant.setSignerEmail(optionalEmail(signer.signerEmail()));
            participant.setExpiresInDays(expiryDays); participant.setUpdatedBy(actorId);
            if (mapper.upsertParticipant(participant) <= 0) throw conflict("Unable to save signer details");
            invitations.add(createPackageInvitation(actorId, entityType, entityId, document, participant, now));
        }
        Invitation first = invitations.get(0);
        List<ElectronicSignatureLinkResponse> links = invitations.stream()
                .map(invitation -> signatureLink(invitation.row(), invitation.url()))
                .toList();
        return new ElectronicSignatureStartResponse(first.row().getId(), first.url(),
                first.row().getExpiresAt(), links);
    }

    private Invitation createPackageInvitation(Long actorId, String entityType, Long entityId, DocumentRow document,
            ParticipantRow participant, LocalDateTime now) {
        String token = randomToken();
        NewRequest row = new NewRequest(); row.setSourceDocumentId(document.getId()); row.setRootDocumentId(document.getId());
        row.setEntityType(entityType); row.setEntityId(entityId); row.setDocumentKind(participant.getDocumentKind());
        row.setSignerRole(participant.getSignerRole()); row.setSigningOrder(participant.getSigningOrder());
        row.setSignerName(participant.getSignerName()); row.setSignerEmail(participant.getSignerEmail());
        row.setAccessTokenHash(sha256(token)); row.setVerificationCodeHash(VERIFICATION_NOT_REQUIRED);
        row.setExpiresAt(now.plusDays(participant.getExpiresInDays()));
        row.setVerificationExpiresAt(row.getExpiresAt()); row.setRequestedBy(actorId);
        row.setSourceChecksumSha256(document.getChecksumSha256()); row.setStatus("pending");
        if (mapper.insertRequest(row) != 1 || row.getId() == null) throw conflict("Unable to create signing request");
        String url = frontendBase + "/sign/" + token;
        mapper.insertEvent(row.getId(), "requested", "Parallel signing link created", null, null);
        mapper.insertAudit(actorId, "start_electronic_signature", entityType, entityId, row.getId(), row.getSignerName());
        return new Invitation(row, url);
    }

    private ElectronicSignatureStartResponse start(Long actorId, String entityType, Long entityId, DocumentRow document,
            ElectronicSignatureStartRequest request, SigningPlan plan) {
        if (document == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract document not found");
        Long rootDocumentId = document.getId();
        if (mapper.countSignedRole(rootDocumentId, plan.signerRole()) > 0) {
            throw conflict("This signer role has already completed this document");
        }
        if (mapper.countPendingRequestsForRoot(rootDocumentId) > 0) {
            throw conflict("A signing request is already in progress for this contract");
        }
        DocumentRow sourceDocument = document;
        if (plan.signingOrder() > 1) {
            sourceDocument = mapper.findSignedStepDocument(rootDocumentId, plan.signingOrder() - 1);
            if (sourceDocument == null) throw conflict("The previous signer must complete signing first");
        }
        if (!"application/pdf".equalsIgnoreCase(sourceDocument.getMimeType())) throw bad("Only PDF contracts can be signed online");
        Path source = resolveSourceDocument(entityType, sourceDocument.getStorageKey(), plan.signingOrder() > 1);
        if (!Files.isRegularFile(source)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract file is unavailable");
        LocalDateTime now = LocalDateTime.now(clock);
        int expiryDays = request.expiresInDays() == null ? DEFAULT_EXPIRY_DAYS : request.expiresInDays();
        String token = randomToken();
        NewRequest row = new NewRequest(); row.setSourceDocumentId(sourceDocument.getId()); row.setRootDocumentId(rootDocumentId);
        row.setEntityType(entityType); row.setEntityId(entityId); row.setDocumentKind(plan.documentKind());
        row.setSignerRole(plan.signerRole()); row.setSigningOrder(plan.signingOrder());
        row.setSignerName(trim(request.signerName(), "Signer name")); row.setSignerEmail(optionalEmail(request.signerEmail()));
        row.setAccessTokenHash(sha256(token)); row.setVerificationCodeHash(VERIFICATION_NOT_REQUIRED);
        row.setExpiresAt(now.plusDays(expiryDays)); row.setVerificationExpiresAt(row.getExpiresAt());
        row.setRequestedBy(actorId); row.setSourceChecksumSha256(sourceDocument.getChecksumSha256()); row.setStatus("pending");
        if (mapper.insertRequest(row) != 1 || row.getId() == null) throw conflict("Unable to create signing request");
        String url = frontendBase + "/sign/" + token;
        mapper.insertEvent(row.getId(), "requested", "Signing link created", null, null);
        mapper.insertAudit(actorId, "start_electronic_signature", entityType, entityId, row.getId(), row.getSignerName());
        return new ElectronicSignatureStartResponse(row.getId(), url, row.getExpiresAt(),
                List.of(signatureLink(row, url)));
    }

    private ElectronicSignatureLinkResponse signatureLink(NewRequest row, String url) {
        return new ElectronicSignatureLinkResponse(row.getId(), row.getSignerRole(), row.getSignerName(),
                row.getSignerEmail(), url, row.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public ElectronicSignaturePublicResponse publicView(String token) { return response(requireRequest(token)); }

    // Internal authenticated entry points: OwnerSignatureService verifies assignment first.
    public ElectronicSignaturePublicResponse viewById(Long id) { return response(requireRequestById(id)); }

    @Transactional
    public ElectronicSignaturePublicResponse signById(Long id, ElectronicSignatureSignRequest request,
            String remoteIp, String userAgent) {
        RequestRow row = requireRequestById(id);
        requirePending(row);
        return signRequest(row, request, remoteIp, userAgent, () -> requireRequestById(id));
    }

    public Download downloadById(Long id, boolean signed) {
        RequestRow row = requireRequestById(id);
        if ("superseded".equals(row.getStatus()) || "cancelled".equals(row.getStatus()))
            throw new ResponseStatusException(HttpStatus.GONE, "签署任务已失效");
        return signed ? downloadSigned(row) : downloadOriginal(row);
    }

    private RequestRow requireRequestById(Long id) {
        RequestRow row = mapper.findById(id);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "签署任务不存在");
        return row;
    }

    @Transactional
    public void sendInvitationEmail(Long actorId, Long requestId, String token, String recipientEmail) {
        requireMail();
        RequestRow row = requirePendingRequest(token);
        if (!Objects.equals(row.getId(), requestId)) throw bad("Signing link does not match the request");
        String email = trim(recipientEmail, "Recipient email").toLowerCase(Locale.ROOT);
        if (mapper.updateRequestSignerEmail(requestId, email) != 1) {
            throw conflict("Unable to save the signing email address");
        }
        if (row.getRootDocumentId() != null && row.getSignerRole() != null && !row.getSignerRole().isBlank()) {
            mapper.updateParticipantSignerEmail(row.getRootDocumentId(), row.getSignerRole(), email);
        }
        String url = frontendBase + "/sign/" + token;
        sendInvitationEmail(email, row.getSignerName(), url, row.getExpiresAt());
        mapper.insertEvent(row.getId(), "invitation_email_sent", "Signing invitation sent by email", null, null);
        mapper.insertAudit(actorId, "send_electronic_signature_invitation", row.getEntityType(), row.getEntityId(),
                row.getId(), email);
    }

    @Transactional
    public ElectronicSignaturePublicResponse sign(String token, ElectronicSignatureSignRequest request, String remoteIp,
            String userAgent) {
        RequestRow row = requirePendingRequest(token);
        return signRequest(row, request, remoteIp, userAgent, () -> requireRequest(token));
    }

    private ElectronicSignaturePublicResponse signRequest(RequestRow row, ElectronicSignatureSignRequest request,
            String remoteIp, String userAgent, java.util.function.Supplier<RequestRow> reload) {
        // Serialize App and public-link submissions before touching the shared output file.
        // A locking read also sees a concurrent commit under MySQL REPEATABLE READ.
        if (!"pending".equals(mapper.lockRequestStatus(row.getId())))
            throw conflict("This signing request is already closed");
        if (!Boolean.TRUE.equals(request.consent())) throw bad("Consent is required before signing");
        if (!normalizeName(request.signerName()).equals(normalizeName(row.getSignerName()))) throw bad("Signer name does not match the request");
        String identityNo = Objects.toString(request.identityNo(), "").trim();
        boolean pmaWitness = "property_management_agreement_draft".equalsIgnoreCase(row.getDocumentKind())
                && "customer_service".equalsIgnoreCase(row.getSignerRole());
        boolean leaseWitness = "lease_contract".equalsIgnoreCase(row.getDocumentKind())
                && List.of("owner_witness", "tenant_witness").contains(
                        Objects.toString(row.getSignerRole(), "").toLowerCase(Locale.ROOT));
        boolean appointmentWitness = "rental_appointment_draft".equalsIgnoreCase(row.getDocumentKind())
                && "witness".equalsIgnoreCase(row.getSignerRole());
        if ((pmaWitness || leaseWitness || appointmentWitness) && identityNo.isBlank()) {
            throw bad("Witness passport or identity number is required");
        }
        byte[] signature = decodeSignature(request.signatureDataUrl());
        Long rootId = rootDocumentId(row); List<String> packageRoles = multiSignerRoles(row.getDocumentKind());
        DocumentRow aggregate = null;
        if (!packageRoles.isEmpty()) {
            if (mapper.lockDocument(rootId) == null) throw conflict("The original contract document is unavailable");
            aggregate = mapper.findLatestActiveSignedDocument(rootId);
        }
        String sourceStorageKey = aggregate == null ? row.getStorageKey() : aggregate.getStorageKey();
        boolean signedSource = aggregate != null || (packageRoles.isEmpty() && row.getSigningOrder() != null && row.getSigningOrder() > 1);
        Path source = resolveSourceDocument(row.getEntityType(), sourceStorageKey, signedSource);
        if (!Files.isRegularFile(source)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract file is unavailable");
        LocalDateTime signedAt = LocalDateTime.now(clock); Path generated = signatureTarget(row.getId());
        try {
            Files.createDirectories(generated.getParent());
            stampSignedPdf(source, generated, signature, row.getSignerName(), identityNo, signedAt,
                    row.getDocumentKind(), row.getSignerRole());
            NewDocument signed = new NewDocument(); signed.setDocumentNo("SIGNED-" + row.getId() + "-" + signedAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            signed.setOriginalName(signedName(row.getOriginalName())); signed.setStorageKey(signatureRoot.relativize(generated).toString().replace('\\', '/'));
            signed.setFileSize(Files.size(generated)); signed.setChecksumSha256(sha256(generated)); signed.setUploadedBy(null);
            if (mapper.insertSignedDocument(signed) != 1 || signed.getId() == null)
                throw conflict("Unable to save signed contract");
            int participantCount = packageRoles.isEmpty() ? 0 : mapper.countParticipantsForRoot(rootId);
            int expectedSigners = participantCount > 0 ? participantCount : packageRoles.size();
            boolean packageComplete = packageRoles.isEmpty()
                    || mapper.countSignedRolesForRoot(rootId) + 1 >= expectedSigners;
            int linkResult = "rental_mandate".equals(row.getEntityType())
                    ? mapper.insertSignedMandateDocumentLink(signed.getId(), row.getEntityId(), signedRelation(row, packageComplete))
                    : mapper.insertSignedDocumentLink(signed.getId(), row.getEntityType(), row.getEntityId());
            if (linkResult != 1
                    || mapper.completeRequest(row.getId(), signedAt, signed.getId(), sha256(signature), shorten(remoteIp, 64), shorten(userAgent, 500)) != 1) throw conflict("Unable to save signed contract");
            // A re-sign replaces the previous signed file only after the new file is safely saved.
            mapper.supersedePreviousSignedDocuments(rootId, signed.getId());
            mapper.insertEvent(row.getId(), "signed", "Contract signed", remoteIp, userAgent);
            notifySignatureRequester(row, signedAt);
            mapper.insertAudit(null, "complete_electronic_signature", row.getEntityType(), row.getEntityId(), row.getId(), row.getSignerName());
            return response(reload.get());
        } catch (IOException exception) {
            deleteQuietly(generated); throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate signed contract", exception);
        } catch (RuntimeException exception) { deleteQuietly(generated); throw exception; }
    }

    @Transactional(readOnly = true)
    public Download downloadOriginal(String token) {
        RequestRow row = requireRequest(token);
        return downloadOriginal(row);
    }
    private Download downloadOriginal(RequestRow row) {
        boolean signedSource = row.getRootDocumentId() != null
                ? !Objects.equals(row.getSourceDocumentId(), row.getRootDocumentId())
                : row.getSigningOrder() != null && row.getSigningOrder() > 1;
        Path path = resolveSourceDocument(row.getEntityType(), row.getStorageKey(), signedSource);
        if (!Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract file is unavailable");
        return new Download(path, safeName(row.getOriginalName()));
    }

    @Transactional(readOnly = true)
    public Download downloadSigned(String token) {
        RequestRow row = requireRequest(token);
        return downloadSigned(row);
    }
    private Download downloadSigned(RequestRow row) {
        if (!"signed".equals(row.getStatus())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Signed contract is not available");
        }
        String storageKey = row.getSignedStorageKey();
        String originalName = row.getSignedOriginalName();
        if (storageKey == null || "voided".equalsIgnoreCase(row.getSignedDocumentStatus())
                || "superseded".equalsIgnoreCase(row.getSignedDocumentStatus())) {
            DocumentRow latest = mapper.findLatestActiveSignedDocument(rootDocumentId(row));
            if (latest == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Signed contract is not available");
            storageKey = latest.getStorageKey(); originalName = latest.getOriginalName();
        }
        Path path = signatureRoot.resolve(storageKey).normalize();
        if (!path.startsWith(signatureRoot) || !Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Signed contract file is unavailable");
        return new Download(path, safeName(originalName));
    }

    private RequestRow requireRequest(String token) {
        if (token == null || token.length() < 32) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Signing link not found");
        RequestRow row = mapper.findByTokenHash(sha256(token));
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Signing link not found");
        return row;
    }
    private RequestRow requirePendingRequest(String token) {
        RequestRow row = requireRequest(token);
        requirePending(row);
        return row;
    }
    private void requirePending(RequestRow row) {
        if (!"pending".equals(row.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This signing request is already closed");
        if (row.getExpiresAt() == null || row.getExpiresAt().isBefore(LocalDateTime.now(clock))) throw new ResponseStatusException(HttpStatus.GONE, "This signing link has expired");
    }
    private ElectronicSignaturePublicResponse response(RequestRow row) {
        boolean canSign = "pending".equals(row.getStatus()) && row.getExpiresAt() != null && !row.getExpiresAt().isBefore(LocalDateTime.now(clock));
        boolean signed = "signed".equals(row.getStatus());
        return new ElectronicSignaturePublicResponse(row.getId(), row.getOriginalName(), row.getSignerName(),
                mask(row.getSignerEmail()), row.getStatus(), row.getExpiresAt(), row.getSignedAt(), canSign, signed,
                row.getDocumentKind(), row.getSignerRole(), primarySignaturePage(row));
    }

    private int primarySignaturePage(RequestRow row) {
        int fallback = defaultSignaturePage(row.getDocumentKind());
        try {
            boolean signedSource = row.getRootDocumentId() != null
                    ? !Objects.equals(row.getSourceDocumentId(), row.getRootDocumentId())
                    : row.getSigningOrder() != null && row.getSigningOrder() > 1;
            Path source = resolveSourceDocument(row.getEntityType(), row.getStorageKey(), signedSource);
            if (!Files.isRegularFile(source)) return fallback;
            try (PdfReader reader = new PdfReader(Files.readAllBytes(source))) {
                return signaturePlacements(row.getDocumentKind(), row.getSignerRole(), reader).stream()
                        .filter(placement -> placement.page() >= 1 && placement.page() <= reader.getNumberOfPages())
                        .sorted(Comparator
                                .comparingDouble((SignaturePlacement placement) ->
                                        placement.signatureWidth() * placement.signatureHeight())
                                .reversed()
                                .thenComparingInt(SignaturePlacement::page))
                        .map(SignaturePlacement::page)
                        .findFirst()
                        .orElse(fallback);
            }
        } catch (Exception ignored) {
            return fallback;
        }
    }

    static int defaultSignaturePage(String documentKind) {
        return switch (Objects.toString(documentKind, "").toLowerCase(Locale.ROOT)) {
            case "property_management_agreement_draft" -> 7;
            case "management_authorization_draft", "authorization_draft", "authorization" -> 3;
            case "termination_letter_draft" -> 2;
            case "lease_contract" -> 12;
            default -> 1;
        };
    }
    private void sendInvitationEmail(String email, String name, String url, LocalDateTime expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage(); message.setFrom(senderName + " <" + fromAddress + ">"); message.setTo(email);
        message.setSubject("CCPS 合約簽署邀請"); message.setText("您好 " + name + "：\n\n請透過以下連結查看及簽署合約：\n" + url + "\n\n開啟連結後即可直接查看並簽署。\n簽署連結有效至：" + expiresAt + "\n\n若非本人操作，請忽略此郵件。");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "签署邮件发送失败，请稍后重试", exception);
        }
    }
    private void requireMail() { if (mailSender == null || mailHost == null || mailHost.isBlank() || fromAddress == null || fromAddress.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email service is not configured"); }

    private void notifySignatureRequester(RequestRow row, LocalDateTime signedAt) {
        if (row.getRequestedBy() == null || !List.of("owner", "second_owner").contains(Objects.toString(row.getSignerRole(), "").toLowerCase(Locale.ROOT))) {
            return;
        }
        RequesterRow requester = mapper.findRequester(row.getRequestedBy());
        if (requester == null) return;
        String title = "屋主已完成电子签署";
        String body = "您发起的文件「%s」已由屋主「%s」完成签署。签署时间：%s。请登录 CCPS 查看最新签署文件。"
                .formatted(text(row.getOriginalName(), "未命名文件"), text(row.getSignerName(), "—"),
                        signedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        NewNotification notification = new NewNotification();
        notification.setRecipientUserId(requester.getId());
        notification.setTitle(title);
        notification.setBody(body);
        notification.setRelatedType("electronic_signature");
        notification.setRelatedId(row.getId());
        notification.setPriority("normal");
        if (mapper.insertSignatureNotification(notification) != 1 || notification.getId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "签署完成通知建立失败");
        }
        mapper.insertSignatureInAppDelivery(notification.getId());
        if (requester.getEmail() != null && !requester.getEmail().isBlank()) {
            mapper.insertSignatureEmailDelivery(notification.getId(), requester.getEmail().trim().toLowerCase(Locale.ROOT));
        }
    }

    private String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Path resolveSource(String entityType, String storageKey) { return resolveSourceDocument(entityType, storageKey, false); }
    private Path resolveSourceDocument(String entityType, String storageKey, boolean signedStep) { Path root = signedStep ? signatureRoot : "lease".equals(entityType) ? leaseRoot : "rental_mandate".equals(entityType) ? mandateRoot : null; if (root == null) throw bad("Unsupported contract type"); Path path = root.resolve(storageKey).normalize(); if (!path.startsWith(root)) throw bad("Invalid contract path"); return path; }
    private Path signatureTarget(Long requestId) { return signatureRoot.resolve(String.valueOf(requestId)).resolve("signed-contract.pdf").normalize(); }
    private void stampSignedPdf(Path source, Path target, byte[] signature, String signerName, String identityNo, LocalDateTime signedAt,
            String documentKind, String signerRole) throws IOException {
        try (PdfReader reader = new PdfReader(Files.readAllBytes(source)); var output = Files.newOutputStream(target)) {
            PdfStamper stamper = new PdfStamper(reader, output);
            boolean manualLeaseSignature = "lease_contract".equalsIgnoreCase(Objects.toString(documentKind, ""))
                    && List.of("owner", "owner_witness", "tenant", "tenant_witness")
                            .contains(Objects.toString(signerRole, "").toLowerCase(Locale.ROOT));
            boolean pmaSignature = "property_management_agreement_draft".equalsIgnoreCase(
                    Objects.toString(documentKind, ""));
            boolean rentalAppointmentSignature = "rental_appointment_draft".equalsIgnoreCase(
                    Objects.toString(documentKind, ""));
            boolean otrSignature = List.of("otr", "otr_document")
                    .contains(Objects.toString(documentKind, "").toLowerCase(Locale.ROOT));
            boolean managementAuthorizationSignature = List.of(
                     "management_authorization_draft", "authorization_draft", "authorization")
                     .contains(Objects.toString(documentKind, "").toLowerCase(Locale.ROOT));
            boolean terminationLetterSignature = "termination_letter_draft".equalsIgnoreCase(
                    Objects.toString(documentKind, ""));
            boolean rentalRemittanceSignature = "rental_remittance_draft".equalsIgnoreCase(
                    Objects.toString(documentKind, ""));
            for (SignaturePlacement placement : signaturePlacements(documentKind, signerRole, reader)) {
                PdfContentByte canvas = stamper.getOverContent(placement.page());
                boolean signatureOnly = manualLeaseSignature || pmaSignature || rentalAppointmentSignature || otrSignature
                        || managementAuthorizationSignature || terminationLetterSignature || rentalRemittanceSignature;
                Image image = Image.getInstance(signatureOnly ? trimSignatureImage(signature) : signature);
                image.scaleToFit(placement.signatureWidth(), placement.signatureHeight());
                float signatureX = placement.signatureX();
                float signatureY = placement.signatureY();
                if (signatureOnly) {
                    signatureX += (placement.signatureWidth() - image.getScaledWidth()) / 2f;
                }
                if (manualLeaseSignature || managementAuthorizationSignature) {
                    signatureY += (placement.signatureHeight() - image.getScaledHeight()) / 2f;
                }
                image.setAbsolutePosition(signatureX, signatureY); canvas.addImage(image);
                if (placement.dateX() > 0 && placement.dateY() > 0) {
                    BaseFont font = PdfFontResources.regular();
                    canvas.beginText(); canvas.setFontAndSize(font, placement.dateOnly() ? 9 : 7.5f);
                    if (placement.dateOnly()) {
                        canvas.setTextMatrix(placement.dateX(), placement.dateY()); canvas.showText(signedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    } else {
                        canvas.setTextMatrix(placement.signatureX(), placement.signatureY() - 10); canvas.showText("電子簽署：" + signerName);
                        canvas.setTextMatrix(placement.dateX(), placement.dateY()); canvas.showText(signedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    }
                    canvas.endText();
                }
            }
            if (pmaSignature && "customer_service".equalsIgnoreCase(Objects.toString(signerRole, ""))) {
                stampPmaWitnessDetails(stamper.getOverContent(7), signerName, identityNo);
            }
            if ("lease_contract".equalsIgnoreCase(Objects.toString(documentKind, ""))
                    && List.of("owner_witness", "tenant_witness").contains(
                            Objects.toString(signerRole, "").toLowerCase(Locale.ROOT))) {
                stampLeaseWitnessDetails(stamper, signerRole, signerName, identityNo, signedAt);
            }
            stamper.close();
        } catch (Exception exception) { if (exception instanceof IOException io) throw io; throw new IOException("Unable to stamp PDF", exception); }
    }
    private byte[] decodeSignature(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:image/png;base64,")) {
            throw bad("Invalid handwritten signature");
        }
        try {
            byte[] data = Base64.getDecoder().decode(dataUrl.substring("data:image/png;base64,".length()));
            if (data.length == 0 || data.length > MAX_SIGNATURE_BYTES || !hasMeaningfulSignatureInk(data)) {
                throw bad("Please provide a complete handwritten signature");
            }
            return data;
        } catch (IllegalArgumentException | IOException exception) {
            throw bad("Handwritten signature is invalid");
        }
    }

    static boolean hasMeaningfulSignatureInk(byte[] data) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        if (image == null || image.getWidth() < 20 || image.getHeight() < 20) return false;
        int minX = image.getWidth(), minY = image.getHeight(), maxX = -1, maxY = -1, inkPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xff;
                int red = (argb >>> 16) & 0xff;
                int green = (argb >>> 8) & 0xff;
                int blue = argb & 0xff;
                if (alpha > 12 && Math.min(red, Math.min(green, blue)) < 245) {
                    inkPixels++;
                    minX = Math.min(minX, x); minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                }
            }
        }
        return inkPixels >= 40 && maxX - minX >= 12 && maxY - minY >= 8;
    }
    private String randomToken() { byte[] bytes = new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    static SignaturePlacement signaturePlacement(String originalName, float pageWidth, float pageHeight) {
        float signatureX = Math.max(36f, pageWidth * 0.14f);
        float signatureY = Math.max(90f, pageHeight * 0.18f);
        float signatureWidth = Math.min(150f, pageWidth * 0.24f);
        float signatureHeight = Math.min(52f, pageHeight * 0.07f);
        float dateX = Math.min(pageWidth - 150f, pageWidth * 0.50f);
        return new SignaturePlacement(1, signatureX, signatureY, signatureWidth, signatureHeight, dateX, signatureY - 22f, false);
    }

    private List<SignaturePlacement> signaturePlacements(String documentKind, String signerRole, PdfReader reader) {
        String kind = Objects.toString(documentKind, "").toLowerCase(Locale.ROOT);
        String role = Objects.toString(signerRole, "signer").toLowerCase(Locale.ROOT);
        List<SignaturePlacement> configured = configuredSignaturePlacements(kind, role, reader);
        if (configured != null && !configured.isEmpty()) return configured;
        if ("lease_contract".equals(kind)
                && List.of("owner", "owner_witness", "tenant", "tenant_witness").contains(role)) {
            List<SignaturePlacement> placements = new ArrayList<>();
            // Only the tenant initials the numbered pages. Keep the initials in the
            // otherwise empty middle of the footer so they never cover the page
            // number or template version. Pages 12 and 19 already contain the
            // tenant's full execution/check-in signature and do not need initials.
            if ("tenant".equals(role)) {
                for (int page = 2; page <= reader.getNumberOfPages(); page++) {
                    if (page == 12 || page == 19) continue;
                    placements.add(new SignaturePlacement(page, 265, 20, 42, 18, 0, 0, true));
                }
            }
            if ("owner".equals(role) && reader.getNumberOfPages() >= 12) {
                placements.add(new SignaturePlacement(12, 375, 640, 65, 50, 0, 0, true));
            }
            if ("tenant".equals(role) && reader.getNumberOfPages() >= 12) {
                placements.add(new SignaturePlacement(12, 375, 385, 65, 50, 0, 0, true));
            }
            if ("tenant".equals(role) && reader.getNumberOfPages() >= 19) {
                placements.add(new SignaturePlacement(19, 95, 585, 65, 50, 0, 0, true));
            }
            if ("owner_witness".equals(role) && reader.getNumberOfPages() >= 12) {
                placements.add(new SignaturePlacement(12, 72, 585, 150, 32, 0, 0, true));
            }
            if ("tenant_witness".equals(role) && reader.getNumberOfPages() >= 12) {
                placements.add(new SignaturePlacement(12, 72, 350, 150, 32, 0, 0, true));
            }
            if ("tenant_witness".equals(role) && reader.getNumberOfPages() >= 19) {
                placements.add(new SignaturePlacement(19, 360, 585, 150, 32, 0, 0, true));
            }
            return placements;
        }
        if ("property_management_agreement_draft".equals(kind)) {
            if ("owner".equals(role)) return List.of(new SignaturePlacement(7, 246, 575, 118, 34, 0, 0, false));
            if ("company".equals(role)) return List.of(new SignaturePlacement(7, 246, 251, 118, 34, 0, 0, false));
            if ("customer_service".equals(role)) return List.of(
                    new SignaturePlacement(7, 33, 464, 92, 27, 0, 0, false),
                    new SignaturePlacement(7, 33, 190, 92, 27, 0, 0, false));
        }
        if ("rental_appointment_draft".equals(kind)) {
            return switch (role) {
                case "owner" -> List.of(new SignaturePlacement(1, 75, 260, 130, 38, 0, 0, true));
                case "second_owner" -> List.of(new SignaturePlacement(1, 245, 260, 130, 38, 0, 0, true));
                case "witness" -> List.of(new SignaturePlacement(1, 415, 260, 130, 38, 0, 0, true));
                default -> throw bad("Unsupported signer role for rental appointment");
            };
        }
        if ("otr".equals(kind) || "otr_document".equals(kind)) {
            boolean includesAppointmentPage = reader.getNumberOfPages() >= 2;
            return switch (role) {
                case "tenant" -> List.of(new SignaturePlacement(1, 115, 365, 150, 32, 0, 0, true));
                case "tenant_witness" -> List.of(new SignaturePlacement(1, 115, 160, 150, 28, 0, 0, true));
                case "owner" -> includesAppointmentPage
                        ? List.of(new SignaturePlacement(1, 340, 365, 150, 32, 0, 0, true),
                                new SignaturePlacement(2, 75, 260, 130, 38, 0, 0, true))
                        : List.of(new SignaturePlacement(1, 340, 365, 150, 32, 0, 0, true));
                case "owner_witness" -> includesAppointmentPage
                        ? List.of(new SignaturePlacement(1, 340, 160, 150, 28, 0, 0, true),
                                new SignaturePlacement(2, 415, 260, 130, 38, 0, 0, true))
                        : List.of(new SignaturePlacement(1, 340, 160, 150, 28, 0, 0, true));
                case "second_owner" -> {
                    if (!includesAppointmentPage) {
                        throw bad("The OTR has no rental appointment page for the second owner");
                    }
                    yield List.of(new SignaturePlacement(2, 245, 260, 130, 38, 0, 0, true));
                }
                default -> throw bad("Unsupported signer role for OTR");
            };
        }
        if ("management_authorization_draft".equals(kind) || "authorization_draft".equals(kind)
                 || "authorization".equals(kind)) {
            return List.of(new SignaturePlacement(3, 72, 382, 112, 24, 0, 0, true));
        }
        if ("termination_letter_draft".equals(kind)) {
            return switch (role) {
                case "owner" -> List.of(new SignaturePlacement(2, 54, 250, 165, 32, 0, 0, true));
                case "company" -> List.of(new SignaturePlacement(2, 54, 139, 165, 32, 0, 0, true));
                default -> throw bad("The termination letter must be signed by the owner and company representative");
            };
        }
        if ("rental_remittance_draft".equals(kind)) {
            if (!"owner".equals(role)) throw bad("The rental remittance letter must be signed by the owner");
            // The sample has one owner signature line at the lower-left of page 1.
            return List.of(new SignaturePlacement(1, 72, 116, 112, 34, 0, 0, true));
        }
        int page = reader.getNumberOfPages();
        com.lowagie.text.Rectangle pageSize = reader.getPageSize(page);
        SignaturePlacement generic = signaturePlacement(null, pageSize.getWidth(), pageSize.getHeight());
        return List.of(new SignaturePlacement(page, generic.signatureX(), generic.signatureY(), generic.signatureWidth(),
                generic.signatureHeight(), generic.dateX(), generic.dateY(), generic.dateOnly()));
    }

    private List<SignaturePlacement> configuredSignaturePlacements(String documentKind, String signerRole,
            PdfReader reader) {
        if (templateService == null) return List.of();
        RentalManagementTemplatePdfService.TemplateType type = templateTypeFor(documentKind);
        if (type == null) return List.of();
        String role = Objects.toString(signerRole, "").toLowerCase(Locale.ROOT);
        List<String> keys = switch (type) {
            case PROPERTY_MANAGEMENT_AGREEMENT -> switch (role) {
                case "owner" -> List.of("signature.owner");
                case "company" -> List.of("signature.company");
                case "customer_service" -> List.of("signature.customer_service");
                default -> List.of();
            };
            case TERMINATION_LETTER -> switch (role) {
                case "owner" -> List.of("signature.owner");
                case "company" -> List.of("signature.company");
                default -> List.of();
            };
            case MANAGEMENT_AUTHORIZATION, RENTAL_REMITTANCE ->
                    "owner".equals(role) ? List.of("signature.owner") : List.of();
        };
        if (keys.isEmpty()) return List.of();
        try {
            return templateService.currentLayout(type).fields().stream()
                    .filter(RentalManagementTemplatePdfService::isSignatureField)
                    .filter(field -> keys.contains(field.fieldKey()))
                    .filter(field -> field.page() >= 1 && field.page() <= reader.getNumberOfPages())
                    .map(field -> new SignaturePlacement(field.page(), field.x(), field.y(), field.maxWidth(),
                            Math.max(8f, field.lineHeight()), 0, 0, true))
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private RentalManagementTemplatePdfService.TemplateType templateTypeFor(String documentKind) {
        return switch (Objects.toString(documentKind, "").toLowerCase(Locale.ROOT)) {
            case "property_management_agreement_draft" ->
                    RentalManagementTemplatePdfService.TemplateType.PROPERTY_MANAGEMENT_AGREEMENT;
            case "management_authorization_draft", "authorization_draft", "authorization" ->
                    RentalManagementTemplatePdfService.TemplateType.MANAGEMENT_AUTHORIZATION;
            case "termination_letter_draft" -> RentalManagementTemplatePdfService.TemplateType.TERMINATION_LETTER;
            case "rental_remittance_draft" -> RentalManagementTemplatePdfService.TemplateType.RENTAL_REMITTANCE;
            default -> null;
        };
    }

    private void stampPmaWitnessDetails(PdfContentByte canvas, String signerName, String identityNo) throws Exception {
        BaseFont latin = PdfFontResources.regular();
        BaseFont cjk = latin;
        String safeName = shorten(Objects.toString(signerName, "").trim(), 80);
        BaseFont nameFont = safeName.chars().allMatch(ch -> ch < 128) ? latin : cjk;
        canvas.beginText();
        canvas.setFontAndSize(nameFont, 8.5f);
        for (float presenceY : List.of(572f, 249f)) {
            canvas.setTextMatrix(104f, presenceY);
            canvas.showText(safeName);
        }
        for (float nameY : List.of(416f, 142f)) {
            canvas.setTextMatrix(61f, nameY);
            canvas.showText(safeName);
        }
        canvas.endText();
        BaseFont identityFont = latin;
        canvas.beginText();
        canvas.setFontAndSize(identityFont, 8.5f);
        for (float identityY : List.of(384f, 110f)) {
            canvas.setTextMatrix(111f, identityY);
            canvas.showText(shorten(Objects.toString(identityNo, "").trim(), 120));
        }
        canvas.endText();
    }

    private void stampLeaseWitnessDetails(PdfStamper stamper, String signerRole, String signerName,
            String identityNo, LocalDateTime signedAt) throws Exception {
        boolean ownerWitness = "owner_witness".equalsIgnoreCase(Objects.toString(signerRole, ""));
        BaseFont cjk = PdfFontResources.regular();
        BaseFont latin = cjk;
        PdfContentByte signaturePage = stamper.getOverContent(12);
        signaturePage.beginText();
        signaturePage.setFontAndSize(cjk, 8.5f);
        signaturePage.setTextMatrix(108f, ownerWitness ? 541f : 307f);
        signaturePage.showText(shorten(Objects.toString(signerName, "").trim(), 80));
        signaturePage.endText();
        signaturePage.beginText();
        signaturePage.setFontAndSize(latin, 8.5f);
        signaturePage.setTextMatrix(121f, ownerWitness ? 527f : 293f);
        signaturePage.showText(shorten(Objects.toString(identityNo, "").trim(), 100));
        signaturePage.endText();
        if (!ownerWitness && stamper.getReader().getNumberOfPages() >= 19) {
            PdfContentByte checkInPage = stamper.getOverContent(19);
            checkInPage.beginText();
            checkInPage.setFontAndSize(cjk, 8.5f);
            checkInPage.setTextMatrix(405f, 538f);
            checkInPage.showText(shorten(Objects.toString(signerName, "").trim(), 75));
            checkInPage.endText();
            checkInPage.beginText();
            checkInPage.setFontAndSize(latin, 8.5f);
            checkInPage.setTextMatrix(444f, 515f);
            checkInPage.showText("Witness");
            checkInPage.setTextMatrix(399f, 492f);
            checkInPage.showText(signedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            checkInPage.endText();
        }
    }

    static byte[] trimSignatureImage(byte[] signature) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(signature));
        if (source == null) return signature;
        int minX = source.getWidth(), minY = source.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int argb = source.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xff;
                int red = (argb >>> 16) & 0xff;
                int green = (argb >>> 8) & 0xff;
                int blue = argb & 0xff;
                if (alpha > 12 && Math.min(red, Math.min(green, blue)) < 245) {
                    minX = Math.min(minX, x); minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x); maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) return signature;
        int padding = 4;
        minX = Math.max(0, minX - padding); minY = Math.max(0, minY - padding);
        maxX = Math.min(source.getWidth() - 1, maxX + padding);
        maxY = Math.min(source.getHeight() - 1, maxY + padding);
        BufferedImage trimmed = source.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(trimmed, "png", output);
        return output.toByteArray();
    }

    private SigningPlan mandateSigningPlan(String relationType, String requestedRole) {
        String kind = Objects.toString(relationType, "authorization_draft").toLowerCase(Locale.ROOT);
        String role = Objects.toString(requestedRole, "").trim().toLowerCase(Locale.ROOT);
        if ("property_management_agreement_draft".equals(kind)) {
            if (role.isBlank()) role = "owner";
            return switch (role) {
                case "owner" -> new SigningPlan(kind, role, 1, 3);
                case "company" -> new SigningPlan(kind, role, 2, 3);
                case "customer_service" -> new SigningPlan(kind, role, 3, 3);
                default -> throw bad("Unsupported signer role for property management agreement");
            };
        }
        if ("otr".equals(kind) || "otr_document".equals(kind)) {
            if (role.isBlank()) role = "tenant";
            return switch (role) {
                case "tenant" -> new SigningPlan(kind, role, 1, 4);
                case "tenant_witness" -> new SigningPlan(kind, role, 2, 4);
                case "owner" -> new SigningPlan(kind, role, 3, 4);
                case "owner_witness" -> new SigningPlan(kind, role, 4, 4);
                default -> throw bad("Unsupported signer role for OTR");
            };
        }
        if ("termination_letter_draft".equals(kind)) {
            if (role.isBlank()) role = "owner";
            return switch (role) {
                case "owner" -> new SigningPlan(kind, role, 1, 2);
                case "company" -> new SigningPlan(kind, role, 2, 2);
                default -> throw bad("Unsupported signer role for termination letter");
            };
        }
        if ("rental_remittance_draft".equals(kind)) {
            if (!role.isBlank() && !"owner".equals(role)) throw bad("The rental remittance letter must be signed by the owner");
            return new SigningPlan(kind, "owner", 1, 1);
        }
        if (!role.isBlank() && !"owner".equals(role)) throw bad("The management authorization must be signed by the owner");
        return new SigningPlan(kind, "owner", 1, 1);
    }

    private List<String> multiSignerRoles(String documentKind) {
        String kind = Objects.toString(documentKind, "").toLowerCase(Locale.ROOT);
        if ("property_management_agreement_draft".equals(kind)) {
            return List.of("owner", "company", "customer_service");
        }
        if ("otr".equals(kind) || "otr_document".equals(kind)) {
            return List.of("tenant", "tenant_witness", "owner", "owner_witness");
        }
        if ("lease_contract".equals(kind)) {
            return List.of("owner", "owner_witness", "tenant", "tenant_witness");
        }
        if ("rental_appointment_draft".equals(kind)) {
            return List.of("owner", "second_owner", "witness");
        }
        if ("termination_letter_draft".equals(kind)) {
            return List.of("owner", "company");
        }
        return List.of();
    }

    private List<String> signingPackageRoles(String documentKind, ElectronicSignaturePackageRequest request) {
        String kind = Objects.toString(documentKind, "").toLowerCase(Locale.ROOT);
        if ("otr".equals(kind) || "otr_document".equals(kind)) {
            List<String> submitted = request.signers().stream()
                    .map(signer -> Objects.toString(signer.signerRole(), "").trim().toLowerCase(Locale.ROOT)).toList();
            List<String> required = List.of("tenant", "tenant_witness", "owner", "owner_witness");
            if (!submitted.containsAll(required)) {
                throw bad("The tenant, tenant witness, owner and shared owner witness are required for OTR");
            }
            return submitted.contains("second_owner")
                    ? List.of("tenant", "tenant_witness", "owner", "owner_witness", "second_owner")
                    : required;
        }
        if (!"rental_appointment_draft".equals(kind)) return multiSignerRoles(kind);
        List<String> submitted = request.signers().stream()
                .map(signer -> Objects.toString(signer.signerRole(), "").trim().toLowerCase(Locale.ROOT)).toList();
        if (!submitted.contains("owner") || !submitted.contains("witness")) {
            throw bad("The owner and witness are required for the rental appointment");
        }
        return submitted.contains("second_owner")
                ? List.of("owner", "second_owner", "witness") : List.of("owner", "witness");
    }

    private String signedRelation(RequestRow row, boolean packageComplete) {
        String kind = Objects.toString(row.getDocumentKind(), "authorization_draft");
        if ("property_management_agreement_draft".equals(kind)) {
            return packageComplete
                    ? "property_management_agreement_signed" : "property_management_agreement_partial";
        }
        if ("management_authorization_draft".equals(kind)) return "management_authorization_signed";
        if ("termination_letter_draft".equals(kind)) {
            return packageComplete ? "termination_letter_signed" : "termination_letter_partial";
        }
        if ("rental_remittance_draft".equals(kind)) return "rental_remittance_signed";
        if ("rental_appointment_draft".equals(kind)) {
            return packageComplete ? "rental_appointment_signed" : "rental_appointment_partial";
        }
        if ("otr".equals(kind) || "otr_document".equals(kind)) {
            return packageComplete ? "otr_signed" : "otr_partial";
        }
        return "signed_contract";
    }

    private Long rootDocumentId(RequestRow row) {
        return row.getRootDocumentId() == null ? row.getSourceDocumentId() : row.getRootDocumentId();
    }
    private String sha256(String value) { return sha256(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private String sha256(Path path) throws IOException { return sha256(Files.readAllBytes(path)); }
    private String sha256(byte[] value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); } }
    private String signedName(String name) { String safe = safeName(name); int dot = safe.lastIndexOf('.'); return (dot > 0 ? safe.substring(0, dot) : safe) + "-已签署.pdf"; }
    private String safeName(String name) { String value = name == null ? "contract.pdf" : name.replace('\\', '/'); value = value.substring(value.lastIndexOf('/') + 1).replaceAll("[\\r\\n]", "_"); return value.isBlank() ? "contract.pdf" : value; }
    private String trim(String value, String label) { if (value == null || value.isBlank()) throw bad(label + " is required"); return shorten(value.trim(), 190); }
    private String optionalEmail(String value) { return value == null || value.isBlank() ? "" : shorten(value.trim().toLowerCase(Locale.ROOT), 190); }
    private String normalizeName(String value) { return Objects.toString(value, "").trim().replaceAll("\\s+", " "); }
    private String shorten(String value, int max) { if (value == null) return null; return value.length() <= max ? value : value.substring(0, max); }
    private String mask(String email) { if (email == null || email.isBlank()) return "—"; int at = email.indexOf('@'); if (at <= 1) return "*" + email.substring(Math.max(at, 0)); return email.substring(0, 1) + "***" + email.substring(at); }
    private void deleteQuietly(Path path) { try { if (path != null) Files.deleteIfExists(path); } catch (IOException ignored) { } }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }

    public record Download(Path path, String originalName) { }
    record Invitation(NewRequest row, String url) { }
    record SignaturePlacement(int page, float signatureX, float signatureY, float signatureWidth, float signatureHeight, float dateX,
            float dateY, boolean dateOnly) { }
    record SigningPlan(String documentKind, String signerRole, int signingOrder, int totalSteps) { }
}
