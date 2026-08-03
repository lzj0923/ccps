package com.ccps.backend.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.ElectronicSignaturePublicResponse;
import com.ccps.backend.dto.ElectronicSignatureSignRequest;
import com.ccps.backend.dto.ElectronicSignatureStartRequest;
import com.ccps.backend.dto.ElectronicSignatureStartResponse;
import com.ccps.backend.mapper.ElectronicSignatureMapper;
import com.ccps.backend.mapper.ElectronicSignatureMapper.DocumentRow;
import com.ccps.backend.mapper.ElectronicSignatureMapper.NewDocument;
import com.ccps.backend.mapper.ElectronicSignatureMapper.NewRequest;
import com.ccps.backend.mapper.ElectronicSignatureMapper.RequestRow;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

@Service
public class ElectronicSignatureService {
    private static final int DEFAULT_EXPIRY_DAYS = 7;
    private static final int VERIFICATION_MINUTES = 10;
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
    private final SecureRandom random = new SecureRandom();
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    public ElectronicSignatureService(ElectronicSignatureMapper mapper, ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.host:}") String mailHost, @Value("${ccps.mail.from:}") String fromAddress,
            @Value("${ccps.mail.sender-name:CCPS 家慶佳業}") String senderName,
            @Value("${ccps.signing.frontend-base:http://localhost:5173}") String frontendBase,
            @Value("${ccps.storage.lease-contracts:uploads/lease-contracts}") String leaseRoot,
            @Value("${ccps.storage.rental-mandates:uploads/rental-mandates}") String mandateRoot,
            @Value("${ccps.storage.electronic-signatures:uploads/electronic-signatures}") String signatureRoot) {
        this(mapper, mailSenderProvider.getIfAvailable(), mailHost, fromAddress, senderName, frontendBase,
                leaseRoot, mandateRoot, signatureRoot, Clock.systemDefaultZone());
    }

    ElectronicSignatureService(ElectronicSignatureMapper mapper, JavaMailSender mailSender, String mailHost,
            String fromAddress, String senderName, String frontendBase, String leaseRoot, String mandateRoot,
            String signatureRoot, Clock clock) {
        this.mapper = mapper; this.mailSender = mailSender; this.mailHost = mailHost; this.fromAddress = fromAddress;
        this.senderName = senderName; this.frontendBase = frontendBase.replaceAll("/+$", "");
        this.leaseRoot = Path.of(leaseRoot).toAbsolutePath().normalize();
        this.mandateRoot = Path.of(mandateRoot).toAbsolutePath().normalize();
        this.signatureRoot = Path.of(signatureRoot).toAbsolutePath().normalize(); this.clock = clock;
    }

    @Transactional
    public ElectronicSignatureStartResponse startLease(Long actorId, Long leaseId, ElectronicSignatureStartRequest request) {
        return start(actorId, "lease", leaseId, mapper.findLeaseDocument(leaseId), request);
    }

    @Transactional
    public ElectronicSignatureStartResponse startMandateDocument(Long actorId, Long mandateId, Long documentId,
            ElectronicSignatureStartRequest request) {
        if ("active".equals(mapper.findMandateStatus(mandateId))) {
            throw conflict("Enabled rental mandates cannot start a new signature");
        }
        return start(actorId, "rental_mandate", mandateId, mapper.findMandateDocument(mandateId, documentId), request);
    }

    private ElectronicSignatureStartResponse start(Long actorId, String entityType, Long entityId, DocumentRow document,
            ElectronicSignatureStartRequest request) {
        requireMail();
        if (document == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract document not found");
        if (mapper.countSignedRequests(document.getId(), entityType, entityId) > 0) {
            throw conflict("This contract version is already signed; upload a replacement version before signing again");
        }
        if (mapper.countPendingRequests(entityType, entityId) > 0) {
            throw conflict("A signing request is already in progress for this contract");
        }
        if (!"application/pdf".equalsIgnoreCase(document.getMimeType())) throw bad("Only PDF contracts can be signed online");
        Path source = resolveSource(entityType, document.getStorageKey());
        if (!Files.isRegularFile(source)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract file is unavailable");
        LocalDateTime now = LocalDateTime.now(clock);
        int expiryDays = request.expiresInDays() == null ? DEFAULT_EXPIRY_DAYS : request.expiresInDays();
        String token = randomToken(); String code = verificationCode();
        NewRequest row = new NewRequest(); row.setSourceDocumentId(document.getId()); row.setEntityType(entityType); row.setEntityId(entityId);
        row.setSignerName(trim(request.signerName(), "Signer name")); row.setSignerEmail(trim(request.signerEmail(), "Signer email").toLowerCase(Locale.ROOT));
        row.setAccessTokenHash(sha256(token)); row.setVerificationCodeHash(encoder.encode(code));
        row.setVerificationExpiresAt(now.plusMinutes(VERIFICATION_MINUTES)); row.setExpiresAt(now.plusDays(expiryDays));
        row.setRequestedBy(actorId); row.setSourceChecksumSha256(document.getChecksumSha256());
        if (mapper.insertRequest(row) != 1 || row.getId() == null) throw conflict("Unable to create signing request");
        String url = frontendBase + "/sign/" + token;
        sendCode(row.getSignerEmail(), row.getSignerName(), url, code, row.getExpiresAt());
        mapper.insertEvent(row.getId(), "requested", "Signing link created", null, null);
        mapper.insertAudit(actorId, "start_electronic_signature", entityType, entityId, row.getId(), row.getSignerName());
        return new ElectronicSignatureStartResponse(row.getId(), url, row.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public ElectronicSignaturePublicResponse publicView(String token) { return response(requireRequest(token)); }

    @Transactional
    public void resendCode(String token, String remoteIp, String userAgent) {
        requireMail(); RequestRow row = requirePendingRequest(token);
        LocalDateTime now = LocalDateTime.now(clock);
        String code = verificationCode();
        if (mapper.updateVerificationCode(row.getId(), encoder.encode(code), now.plusMinutes(VERIFICATION_MINUTES)) != 1) throw conflict("Signing request has changed");
        sendCode(row.getSignerEmail(), row.getSignerName(), frontendBase + "/sign/" + token, code, row.getExpiresAt());
        mapper.insertEvent(row.getId(), "verification_resent", "Verification code re-sent", remoteIp, userAgent);
    }

    @Transactional
    public ElectronicSignaturePublicResponse sign(String token, ElectronicSignatureSignRequest request, String remoteIp,
            String userAgent) {
        RequestRow row = requirePendingRequest(token);
        if (!Boolean.TRUE.equals(request.consent())) throw bad("Consent is required before signing");
        if (row.getVerificationExpiresAt() == null || row.getVerificationExpiresAt().isBefore(LocalDateTime.now(clock))) throw bad("Verification code has expired");
        if (!encoder.matches(request.verificationCode().trim(), row.getVerificationCodeHash())) {
            mapper.insertEvent(row.getId(), "verification_failed", "Incorrect verification code", remoteIp, userAgent);
            throw bad("Incorrect verification code");
        }
        if (!normalizeName(request.signerName()).equals(normalizeName(row.getSignerName()))) throw bad("Signer name does not match the request");
        byte[] signature = decodeSignature(request.signatureDataUrl());
        Path source = resolveSource(row.getEntityType(), row.getStorageKey());
        if (!Files.isRegularFile(source)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract file is unavailable");
        LocalDateTime signedAt = LocalDateTime.now(clock); Path generated = signatureTarget(row.getId());
        try {
            Files.createDirectories(generated.getParent());
            stampSignedPdf(source, generated, signature, row.getSignerName(), signedAt, row.getOriginalName());
            NewDocument signed = new NewDocument(); signed.setDocumentNo("SIGNED-" + row.getId() + "-" + signedAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            signed.setOriginalName(signedName(row.getOriginalName())); signed.setStorageKey(signatureRoot.relativize(generated).toString().replace('\\', '/'));
            signed.setFileSize(Files.size(generated)); signed.setChecksumSha256(sha256(generated)); signed.setUploadedBy(null);
            if (mapper.insertSignedDocument(signed) != 1 || signed.getId() == null)
                throw conflict("Unable to save signed contract");
            int linkResult = "rental_mandate".equals(row.getEntityType())
                    ? mapper.insertSignedMandateAuthorizationLink(signed.getId(), row.getEntityId())
                    : mapper.insertSignedDocumentLink(signed.getId(), row.getEntityType(), row.getEntityId());
            if (linkResult != 1
                    || mapper.completeRequest(row.getId(), signedAt, signed.getId(), sha256(signature), shorten(remoteIp, 64), shorten(userAgent, 500)) != 1) throw conflict("Unable to save signed contract");
            // A re-sign replaces the previous signed file only after the new file is safely saved.
            mapper.supersedePreviousSignedDocuments(row.getEntityType(), row.getEntityId(), signed.getId());
            mapper.insertEvent(row.getId(), "signed", "Contract signed", remoteIp, userAgent);
            mapper.insertAudit(null, "complete_electronic_signature", row.getEntityType(), row.getEntityId(), row.getId(), row.getSignerName());
            return response(requireRequest(token));
        } catch (IOException exception) {
            deleteQuietly(generated); throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate signed contract", exception);
        } catch (RuntimeException exception) { deleteQuietly(generated); throw exception; }
    }

    @Transactional(readOnly = true)
    public Download downloadOriginal(String token) {
        RequestRow row = requireRequest(token); Path path = resolveSource(row.getEntityType(), row.getStorageKey());
        if (!Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract file is unavailable");
        return new Download(path, safeName(row.getOriginalName()));
    }

    @Transactional(readOnly = true)
    public Download downloadSigned(String token) {
        RequestRow row = requireRequest(token);
        if (!"signed".equals(row.getStatus()) || row.getSignedStorageKey() == null
                || "voided".equalsIgnoreCase(row.getSignedDocumentStatus())
                || "superseded".equalsIgnoreCase(row.getSignedDocumentStatus())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Signed contract is not available");
        Path path = signatureRoot.resolve(row.getSignedStorageKey()).normalize();
        if (!path.startsWith(signatureRoot) || !Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Signed contract file is unavailable");
        return new Download(path, safeName(row.getSignedOriginalName()));
    }

    private RequestRow requireRequest(String token) {
        if (token == null || token.length() < 32) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Signing link not found");
        RequestRow row = mapper.findByTokenHash(sha256(token));
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Signing link not found");
        return row;
    }
    private RequestRow requirePendingRequest(String token) {
        RequestRow row = requireRequest(token);
        if (!"pending".equals(row.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This signing request is already closed");
        if (row.getExpiresAt() == null || row.getExpiresAt().isBefore(LocalDateTime.now(clock))) throw new ResponseStatusException(HttpStatus.GONE, "This signing link has expired");
        return row;
    }
    private ElectronicSignaturePublicResponse response(RequestRow row) {
        boolean canSign = "pending".equals(row.getStatus()) && row.getExpiresAt() != null && !row.getExpiresAt().isBefore(LocalDateTime.now(clock));
        boolean signed = "signed".equals(row.getStatus())
                && !"voided".equalsIgnoreCase(row.getSignedDocumentStatus())
                && !"superseded".equalsIgnoreCase(row.getSignedDocumentStatus());
        return new ElectronicSignaturePublicResponse(row.getId(), row.getOriginalName(), row.getSignerName(), mask(row.getSignerEmail()), row.getStatus(), row.getExpiresAt(), row.getSignedAt(), canSign, signed);
    }
    private void sendCode(String email, String name, String url, String code, LocalDateTime expiresAt) {
        SimpleMailMessage message = new SimpleMailMessage(); message.setFrom(senderName + " <" + fromAddress + ">"); message.setTo(email);
        message.setSubject("CCPS 合約簽署邀請"); message.setText("您好 " + name + "：\n\n請透過以下連結查看及簽署合約：\n" + url + "\n\n本次驗證碼：" + code + "（10 分鐘內有效）\n簽署連結有效至：" + expiresAt + "\n\n若非本人操作，請忽略此郵件。");
        mailSender.send(message);
    }
    private void requireMail() { if (mailSender == null || mailHost == null || mailHost.isBlank() || fromAddress == null || fromAddress.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email service is not configured"); }
    private Path resolveSource(String entityType, String storageKey) { Path root = "lease".equals(entityType) ? leaseRoot : "rental_mandate".equals(entityType) ? mandateRoot : null; if (root == null) throw bad("Unsupported contract type"); Path path = root.resolve(storageKey).normalize(); if (!path.startsWith(root)) throw bad("Invalid contract path"); return path; }
    private Path signatureTarget(Long requestId) { return signatureRoot.resolve(String.valueOf(requestId)).resolve("signed-contract.pdf").normalize(); }
    private void stampSignedPdf(Path source, Path target, byte[] signature, String signerName, LocalDateTime signedAt,
            String originalName) throws IOException {
        try (PdfReader reader = new PdfReader(Files.readAllBytes(source)); var output = Files.newOutputStream(target)) {
            PdfStamper stamper = new PdfStamper(reader, output); int page = reader.getNumberOfPages(); PdfContentByte canvas = stamper.getOverContent(page);
            com.lowagie.text.Rectangle pageSize = reader.getPageSize(page);
            SignaturePlacement placement = signaturePlacement(originalName, pageSize.getWidth(), pageSize.getHeight());
            Image image = Image.getInstance(signature); image.scaleToFit(placement.signatureWidth(), placement.signatureHeight()); image.setAbsolutePosition(placement.signatureX(), placement.signatureY()); canvas.addImage(image);
            BaseFont font = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED); canvas.beginText(); canvas.setFontAndSize(font, placement.dateOnly() ? 9 : 8);
            if (placement.dateOnly()) {
                canvas.setTextMatrix(placement.dateX(), placement.dateY()); canvas.showText(signedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            } else {
                canvas.setTextMatrix(placement.signatureX(), placement.signatureY() - 12); canvas.showText("電子簽署：" + signerName);
                canvas.setTextMatrix(placement.dateX(), placement.dateY()); canvas.showText("時間：" + signedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            canvas.endText(); stamper.close();
        } catch (Exception exception) { if (exception instanceof IOException io) throw io; throw new IOException("Unable to stamp PDF", exception); }
    }
    private byte[] decodeSignature(String dataUrl) { if (dataUrl == null || !dataUrl.startsWith("data:image/png;base64,")) throw bad("Invalid handwritten signature"); try { byte[] data = Base64.getDecoder().decode(dataUrl.substring("data:image/png;base64,".length())); if (data.length == 0 || data.length > MAX_SIGNATURE_BYTES) throw bad("Handwritten signature is invalid"); return data; } catch (IllegalArgumentException exception) { throw bad("Handwritten signature is invalid"); } }
    private String randomToken() { byte[] bytes = new byte[32]; random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    static SignaturePlacement signaturePlacement(String originalName, float pageWidth, float pageHeight) {
        float signatureX = Math.max(36f, pageWidth * 0.14f);
        float signatureY = Math.max(90f, pageHeight * 0.18f);
        float signatureWidth = Math.min(150f, pageWidth * 0.24f);
        float signatureHeight = Math.min(52f, pageHeight * 0.07f);
        float dateX = Math.min(pageWidth - 150f, pageWidth * 0.50f);
        return new SignaturePlacement(signatureX, signatureY, signatureWidth, signatureHeight, dateX, signatureY - 22f, false);
    }
    private String verificationCode() { return String.valueOf(100000 + random.nextInt(900000)); }
    private String sha256(String value) { return sha256(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    private String sha256(Path path) throws IOException { return sha256(Files.readAllBytes(path)); }
    private String sha256(byte[] value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value)); } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); } }
    private String signedName(String name) { String safe = safeName(name); int dot = safe.lastIndexOf('.'); return (dot > 0 ? safe.substring(0, dot) : safe) + "-已簽署.pdf"; }
    private String safeName(String name) { String value = name == null ? "contract.pdf" : name.replace('\\', '/'); value = value.substring(value.lastIndexOf('/') + 1).replaceAll("[\\r\\n]", "_"); return value.isBlank() ? "contract.pdf" : value; }
    private String trim(String value, String label) { if (value == null || value.isBlank()) throw bad(label + " is required"); return shorten(value.trim(), 190); }
    private String normalizeName(String value) { return Objects.toString(value, "").trim().replaceAll("\\s+", " "); }
    private String shorten(String value, int max) { if (value == null) return null; return value.length() <= max ? value : value.substring(0, max); }
    private String mask(String email) { if (email == null || email.isBlank()) return "—"; int at = email.indexOf('@'); if (at <= 1) return "*" + email.substring(Math.max(at, 0)); return email.substring(0, 1) + "***" + email.substring(at); }
    private void deleteQuietly(Path path) { try { if (path != null) Files.deleteIfExists(path); } catch (IOException ignored) { } }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }

    public record Download(Path path, String originalName) { }
    record SignaturePlacement(float signatureX, float signatureY, float signatureWidth, float signatureHeight, float dateX,
            float dateY, boolean dateOnly) { }
}
