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
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.LinkedHashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.ReserveTopupReviewResponse;
import com.ccps.backend.dto.ReserveTopupSubmissionResponse;
import com.ccps.backend.dto.ReserveTopupSubmissionResponse.FileItem;
import com.ccps.backend.mapper.OwnerReserveMapper;
import com.ccps.backend.mapper.OwnerReserveMapper.ApprovedTopup;
import com.ccps.backend.mapper.OwnerReserveMapper.DocumentFile;
import com.ccps.backend.mapper.OwnerReserveMapper.NewDocument;
import com.ccps.backend.mapper.OwnerReserveMapper.NewTopup;
import com.ccps.backend.mapper.OwnerReserveMapper.TopupContext;
import com.ccps.backend.mapper.OwnerReserveMapper.TopupReceipt;
import com.ccps.backend.mapper.OwnerReserveMapper.TopupReviewRow;

@Service
public class ReserveTopupService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_FILES = 4;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "application/pdf", ".pdf");
    private static final Set<String> METHODS = Set.of("bank_transfer", "online_payment");

    private final OwnerReserveMapper mapper;
    private final Path storageRoot;
    private final Clock clock;

    @Autowired
    public ReserveTopupService(OwnerReserveMapper mapper,
            @Value("${ccps.storage.reserve-topups:uploads/reserve-topups}") String storageRoot) {
        this(mapper, storageRoot, Clock.systemDefaultZone());
    }

    ReserveTopupService(OwnerReserveMapper mapper, String storageRoot, Clock clock) {
        this.mapper = mapper;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.clock = clock;
    }

    @Transactional
    public ReserveTopupSubmissionResponse submit(Long userId, Submission submission, List<MultipartFile> files) {
        TopupContext context = submission.reserveAccountId() == null ? null
                : mapper.findTopupContext(userId, submission.reserveAccountId());
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserve account not found");
        }
        validate(submission, files);

        List<Path> storedPaths = new ArrayList<>();
        try {
            String token = UUID.randomUUID().toString().replace("-", "");
            String date = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            NewTopup topup = new NewTopup();
            topup.setTransactionNo("RTU-" + date + "-" + token.substring(0, 8).toUpperCase());
            topup.setUnitId(context.getUnitId());
            topup.setOwnerId(context.getOwnerId());
            topup.setAmount(submission.amount());
            topup.setPaymentDate(submission.paymentDate());
            topup.setPaymentMethod(submission.paymentMethod());
            topup.setCreatedBy(userId);
            mapper.insertFinanceRecord(topup);

            List<FileItem> fileItems = new ArrayList<>();
            Long firstDocumentId = null;
            int index = 0;
            for (MultipartFile file : files) {
                NewDocument document = store(userId, context.getReserveAccountId(), token, index++, file, storedPaths);
                mapper.insertDocument(document);
                mapper.insertDocumentLink(document.getId(), topup.getId());
                if (firstDocumentId == null) firstDocumentId = document.getId();
                fileItems.add(new FileItem(document.getId(), document.getOriginalName(),
                        document.getMimeType(), document.getFileSize()));
            }

            TopupReceipt receipt = new TopupReceipt();
            receipt.setFinanceRecordId(topup.getId());
            receipt.setReceiptNo("RTU-RCP-" + date + "-" + token.substring(8, 16).toUpperCase());
            receipt.setPayerName(submission.payerName().trim());
            receipt.setBankReference(submission.bankName().trim() + " | " + submission.reference().trim());
            receipt.setProofDocumentId(firstDocumentId);
            receipt.setNote(blankToNull(submission.note()));
            mapper.insertReceipt(receipt);

            return new ReserveTopupSubmissionResponse(topup.getId(), topup.getTransactionNo(),
                    context.getReserveAccountId(), topup.getAmount(), topup.getPaymentDate(),
                    "pending", LocalDateTime.now(clock), fileItems);
        } catch (RuntimeException exception) {
            storedPaths.forEach(this::deleteQuietly);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Download download(Long userId, Long documentId) {
        DocumentFile file = mapper.findDocumentFile(userId, documentId);
        if (file == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserve document not found");
        Path target = storageRoot.resolve(file.getStorageKey()).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reserve document path");
        }
        if (!Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserve document file is unavailable");
        }
        return new Download(target, safeName(file.getOriginalName()),
                file.getMimeType() == null ? "application/octet-stream" : file.getMimeType(),
                file.getFileSize() == null ? 0 : file.getFileSize());
    }

    @Transactional(readOnly = true)
    public Download downloadAdmin(Long documentId) {
        DocumentFile file = mapper.findAdminDocumentFile(documentId);
        if (file == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserve top-up proof not found");
        Path target = storageRoot.resolve(file.getStorageKey()).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reserve proof path");
        }
        if (!Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserve top-up proof file is unavailable");
        }
        return new Download(target, safeName(file.getOriginalName()),
                file.getMimeType() == null ? "application/octet-stream" : file.getMimeType(),
                file.getFileSize() == null ? 0 : file.getFileSize());
    }

    @Transactional
    public ReserveTopupReviewResponse review(Long reviewerId, Long financeRecordId, boolean approved, String note) {
        return reviewOne(reviewerId, financeRecordId, approved, null, note);
    }

    /** Called only by the central finance-confirmation seam. */
    @Transactional
    public ReserveTopupReviewResponse reviewFromFinance(Long reviewerId, Long financeRecordId, boolean approved,
            LocalDate transactionDate, String note) {
        return reviewOne(reviewerId, financeRecordId, approved, transactionDate, note);
    }

    @Transactional
    public void confirmBatch(Long reviewerId, List<Long> financeRecordIds, String note) {
        List<Long> ids = new LinkedHashSet<>(financeRecordIds).stream().toList();
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one reserve top-up");
        for (Long id : ids) reviewOne(reviewerId, id, true, null, note);
    }

    private ReserveTopupReviewResponse reviewOne(Long reviewerId, Long financeRecordId, boolean approved,
            LocalDate transactionDate, String note) {
        if (mapper.isAdmin(reviewerId) == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator role required");
        }
        TopupReviewRow row = mapper.findTopupForUpdate(financeRecordId);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserve top-up not found");
        if (!"pending".equals(row.getConfirmationStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve top-up has already been reviewed");
        }
        String reviewNote = blankToNull(note);
        if (!approved && reviewNote == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A rejection reason is required");
        }
        if (reviewNote != null && reviewNote.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Review note is too long");
        }

        BigDecimal balanceAfter = row.getCurrentBalance();
        String status = approved ? "confirmed" : "rejected";
        if (approved) {
            balanceAfter = zero(row.getCurrentBalance()).add(row.getAmount());
            ApprovedTopup topup = new ApprovedTopup();
            topup.setReserveAccountId(row.getReserveAccountId());
            topup.setFinanceRecordId(row.getFinanceRecordId());
            topup.setAmount(row.getAmount());
            LocalDateTime occurredAt = transactionDate == null
                    ? LocalDateTime.now(clock)
                    : LocalDateTime.of(transactionDate, LocalTime.now(clock));
            topup.setOccurredAt(occurredAt);
            topup.setBalanceAfter(balanceAfter);
            topup.setNote(reviewNote == null ? "預備金充值審核通過" : reviewNote);
            topup.setCreatedBy(reviewerId);
            mapper.insertReserveTopup(topup);
            mapper.updateReserveBalance(row.getReserveAccountId(), balanceAfter);
        }
        if (mapper.updateTopupReview(financeRecordId, status, reviewerId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve top-up review state changed");
        }
        mapper.updateReviewNote(financeRecordId, reviewNote);
        mapper.updateTopupDocuments(financeRecordId, approved ? "active" : "rejected");
        String title = approved ? "預備金充值已確認" : "預備金充值申請已退回";
        String body = approved
                ? "%s %s 預備金已充值 RM %s。".formatted(row.getProjectName(), row.getUnitNo(), row.getAmount().setScale(2).toPlainString())
                : "%s %s 預備金充值申請已退回：%s".formatted(row.getProjectName(), row.getUnitNo(), reviewNote);
        mapper.insertTopupNotification(row.getUserId(), row.getOwnerId(), financeRecordId, title, body,
                approved ? "normal" : "high");
        mapper.insertTopupAudit(reviewerId, financeRecordId,
                approved ? "confirm_reserve_topup" : "reject_reserve_topup", status, reviewNote);
        return new ReserveTopupReviewResponse(financeRecordId, status, balanceAfter);
    }

    private NewDocument store(Long userId, Long reserveAccountId, String token, int index,
            MultipartFile file, List<Path> storedPaths) {
        String extension = EXTENSIONS.get(file.getContentType());
        Path directory = storageRoot.resolve(String.valueOf(reserveAccountId)).normalize();
        if (!directory.startsWith(storageRoot)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage path");
        Path target = directory.resolve(token + "-" + index + extension).normalize();
        if (!target.startsWith(directory)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        try {
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            storedPaths.add(target);
            NewDocument document = new NewDocument();
            document.setDocumentNo("RTU-DOC-" + token.substring(0, 12).toUpperCase() + "-" + index);
            document.setOriginalName(safeName(file.getOriginalFilename()));
            document.setStorageKey(storageRoot.relativize(target).toString().replace('\\', '/'));
            document.setMimeType(file.getContentType());
            document.setFileSize(file.getSize());
            document.setChecksumSha256(sha256(target));
            document.setUploadedBy(userId);
            return document;
        } catch (IOException exception) {
            deleteQuietly(target);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store reserve top-up proof", exception);
        }
    }

    private void validate(Submission value, List<MultipartFile> files) {
        if (value.amount() == null || value.amount().signum() <= 0 || value.amount().compareTo(MAX_AMOUNT) > 0
                || value.paymentDate() == null || value.paymentDate().isAfter(LocalDate.now(clock))
                || !METHODS.contains(value.paymentMethod()) || isBlank(value.bankName())
                || isBlank(value.reference()) || isBlank(value.payerName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please complete valid reserve top-up information");
        }
        if (value.bankName().trim().length() + value.reference().trim().length() + 3 > 120
                || value.payerName().trim().length() > 160
                || (value.note() != null && value.note().length() > 200)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reserve top-up information is too long");
        }
        if (files == null || files.isEmpty() || files.size() > MAX_FILES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload between 1 and 4 proof files");
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE
                    || !EXTENSIONS.containsKey(file.getContentType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPG, PNG or PDF files up to 10MB are supported");
            }
        }
    }

    private String safeName(String value) {
        String name = value == null ? "reserve-topup-proof" : value.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\t]", "_");
        if (name.isBlank()) name = "reserve-topup-proof";
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

    private void deleteQuietly(Path path) { try { Files.deleteIfExists(path); } catch (IOException ignored) { } }
    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private boolean isBlank(String value) { return value == null || value.isBlank(); }
    private String blankToNull(String value) { return isBlank(value) ? null : value.trim(); }

    public record Submission(Long reserveAccountId, BigDecimal amount, LocalDate paymentDate,
            String paymentMethod, String bankName, String reference, String payerName, String note) { }
    public record Download(Path path, String originalName, String mimeType, long size) { }
}
