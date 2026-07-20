package com.ccps.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.PaymentProofSubmissionResponse;
import com.ccps.backend.dto.PaymentProofSubmissionResponse.FileItem;
import com.ccps.backend.mapper.PaymentProofMapper;
import com.ccps.backend.mapper.PaymentProofMapper.NewDocument;
import com.ccps.backend.mapper.PaymentProofMapper.NewFinanceRecord;
import com.ccps.backend.mapper.PaymentProofMapper.NewReceipt;
import com.ccps.backend.mapper.PaymentProofMapper.SubmissionContext;

@Service
public class PaymentProofService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_FILES = 4;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "application/pdf", ".pdf");

    private final PaymentProofMapper mapper;
    private final Path storageRoot;

    public PaymentProofService(PaymentProofMapper mapper,
            @Value("${ccps.storage.payment-proofs:uploads/payment-proofs}") String storageRoot) {
        this.mapper = mapper;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Transactional
    public PaymentProofSubmissionResponse submit(Long userId, Long ownerUnitId, Submission submission,
            List<MultipartFile> files) {
        SubmissionContext context = mapper.findSubmissionContext(userId, ownerUnitId, submission.installmentId());
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment installment not found");
        validate(submission, files, context);

        List<Path> storedPaths = new ArrayList<>();
        try {
            String token = UUID.randomUUID().toString().replace("-", "");
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            NewFinanceRecord finance = new NewFinanceRecord();
            finance.setTransactionNo("PP-" + date + "-" + token.substring(0, 8).toUpperCase());
            finance.setUnitId(context.getUnitId());
            finance.setOwnerId(context.getOwnerId());
            finance.setAmount(submission.amount());
            finance.setCurrency(context.getCurrency() == null ? "MYR" : context.getCurrency());
            finance.setPaymentDate(submission.paymentDate());
            finance.setPaymentMethod(submission.paymentMethod());
            finance.setCreatedBy(userId);
            mapper.insertFinanceRecord(finance);

            List<NewDocument> documents = new ArrayList<>();
            List<FileItem> fileItems = new ArrayList<>();
            int index = 0;
            for (MultipartFile file : files) {
                NewDocument document = storeDocument(userId, ownerUnitId, token, index++, file, storedPaths);
                mapper.insertDocument(document);
                mapper.insertDocumentLink(document.getId(), finance.getId());
                documents.add(document);
                fileItems.add(new FileItem(document.getId(), document.getOriginalName(), document.getMimeType(), document.getFileSize()));
            }

            NewReceipt receipt = new NewReceipt();
            receipt.setFinanceRecordId(finance.getId());
            receipt.setReceiptNo("RCP-" + date + "-" + token.substring(8, 16).toUpperCase());
            receipt.setPayerName(submission.payerName().trim());
            receipt.setBankReference(submission.bankName().trim() + " | " + submission.reference().trim());
            receipt.setProofDocumentId(documents.get(0).getId());
            receipt.setSubmissionNote(blankToNull(submission.note()));
            mapper.insertReceipt(receipt);
            mapper.insertAllocation(receipt.getId(), submission.installmentId(), submission.amount());

            return new PaymentProofSubmissionResponse(receipt.getId(), receipt.getReceiptNo(),
                    submission.installmentId(), submission.amount(), submission.paymentDate(),
                    submission.paymentMethod(), receipt.getPayerName(), "pending", LocalDateTime.now(), fileItems);
        } catch (RuntimeException exception) {
            storedPaths.forEach(this::deleteQuietly);
            throw exception;
        }
    }

    private NewDocument storeDocument(Long userId, Long ownerUnitId, String token, int index,
            MultipartFile file, List<Path> storedPaths) {
        String contentType = file.getContentType();
        String extension = EXTENSIONS.get(contentType);
        String originalName = safeOriginalName(file.getOriginalFilename());
        Path directory = storageRoot.resolve(String.valueOf(ownerUnitId)).normalize();
        if (!directory.startsWith(storageRoot)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage path");
        String storedName = token + "-" + index + extension;
        Path target = directory.resolve(storedName).normalize();
        if (!target.startsWith(directory)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        try {
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            storedPaths.add(target);
            NewDocument document = new NewDocument();
            document.setDocumentNo("DOC-" + token.substring(0, 12).toUpperCase() + "-" + index);
            document.setOriginalName(originalName);
            document.setStorageKey(storageRoot.relativize(target).toString().replace('\\', '/'));
            document.setMimeType(contentType);
            document.setFileSize(file.getSize());
            document.setChecksumSha256(sha256(target));
            document.setUploadedBy(userId);
            return document;
        } catch (IOException exception) {
            deleteQuietly(target);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store payment proof", exception);
        }
    }

    private void validate(Submission submission, List<MultipartFile> files, SubmissionContext context) {
        if (submission.installmentId() == null || submission.amount() == null || submission.amount().signum() <= 0
                || submission.paymentDate() == null || isBlank(submission.paymentMethod())
                || isBlank(submission.bankName()) || isBlank(submission.reference()) || isBlank(submission.payerName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please complete all required payment fields");
        }
        if (submission.paymentDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment date cannot be in the future");
        }
        if (submission.bankName().trim().length() + submission.reference().trim().length() + 3 > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bank name and reference are too long");
        }
        if (submission.payerName().trim().length() > 160 || (submission.note() != null && submission.note().length() > 200)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment information is too long");
        }
        BigDecimal available = zero(context.getAmountDue()).subtract(zero(context.getAmountPaid()))
                .subtract(zero(context.getPendingAmount())).max(BigDecimal.ZERO);
        if (submission.amount().compareTo(available) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment amount exceeds the remaining amount awaiting proof");
        }
        if (files == null || files.isEmpty() || files.size() > MAX_FILES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload between 1 and 4 proof files");
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE || !EXTENSIONS.containsKey(file.getContentType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG, PNG or PDF files up to 10MB are supported");
            }
        }
    }

    private String safeOriginalName(String value) {
        String name = value == null ? "payment-proof" : value.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\t]", "_");
        if (name.isBlank()) name = "payment-proof";
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path); DigestInputStream stream = new DigestInputStream(input, digest)) {
                stream.transferTo(java.io.OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void deleteQuietly(Path path) { try { Files.deleteIfExists(path); } catch (IOException ignored) {} }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
    private String blankToNull(String value) { return isBlank(value) ? null : value.trim(); }

    public record Submission(Long installmentId, BigDecimal amount, LocalDate paymentDate,
            String paymentMethod, String bankName, String reference, String payerName, String note) {}
}
