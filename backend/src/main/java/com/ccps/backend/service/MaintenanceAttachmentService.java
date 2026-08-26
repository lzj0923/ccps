package com.ccps.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.MaintenanceDetailResponse.Attachment;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper.AttachmentFile;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper.NewAttachment;

@Service
public class MaintenanceAttachmentService {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final int MAX_FILES = 6;
    private static final Set<String> RELATION_TYPES = Set.of("before_photo", "after_photo", "invoice");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "application/pdf", ".pdf");

    private final OwnerExpenseMaintenanceMapper mapper;
    private final Path storageRoot;

    public MaintenanceAttachmentService(OwnerExpenseMaintenanceMapper mapper,
            @Value("${ccps.storage.maintenance-attachments:uploads/maintenance-attachments}") String storageRoot) {
        this.mapper = mapper;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Transactional
    public List<Attachment> upload(Long userId, Long workOrderId, String relationType, List<MultipartFile> files) {
        OwnerExpenseMaintenanceMapper.WorkOrderAccess access = mapper.findWorkOrderAccess(userId, workOrderId);
        return upload(userId, workOrderId, relationType, files, access);
    }

    @Transactional
    public List<Attachment> uploadAdmin(Long actorId, Long workOrderId, String relationType, List<MultipartFile> files) {
        OwnerExpenseMaintenanceMapper.WorkOrderAccess access = mapper.findAdminWorkOrderAccess(workOrderId);
        return upload(actorId, workOrderId, relationType, files, access);
    }

    private List<Attachment> upload(Long actorId, Long workOrderId, String relationType,
            List<MultipartFile> files, OwnerExpenseMaintenanceMapper.WorkOrderAccess access) {
        if (access == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance work order not found");
        }
        if ("completed".equals(access.getStatus()) || "cancelled".equals(access.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Completed or cancelled work orders cannot be changed");
        }
        if ("paid".equals(access.getPaymentStatus()) && "confirmed".equals(access.getConfirmationStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Payment is already confirmed; receipt upload is no longer required");
        }
        validate(relationType, files);
        List<Path> storedPaths = new ArrayList<>();
        try {
            int index = 0;
            for (MultipartFile file : files) {
                NewAttachment document = store(actorId, workOrderId, relationType, index++, file, storedPaths);
                mapper.insertAttachment(document);
                mapper.insertAttachmentLink(document.getId(), workOrderId, relationType);
            }
            return mapper.findAttachments(workOrderId);
        } catch (RuntimeException exception) {
            storedPaths.forEach(this::deleteQuietly);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Download download(Long userId, Long documentId) {
        AttachmentFile file = mapper.findAttachmentFile(userId, documentId);
        return download(file);
    }

    @Transactional(readOnly = true)
    public Download downloadAdmin(Long documentId) {
        return download(mapper.findAdminAttachmentFile(documentId));
    }

    private Download download(AttachmentFile file) {
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance attachment not found");
        }
        Path target = storageRoot.resolve(file.getStorageKey()).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attachment path");
        }
        if (!Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance attachment file is unavailable");
        }
        return new Download(target, safeOriginalName(file.getOriginalName()),
                file.getMimeType() == null ? "application/octet-stream" : file.getMimeType(),
                file.getFileSize() == null ? 0 : file.getFileSize());
    }

    private NewAttachment store(Long userId, Long workOrderId, String relationType, int index,
            MultipartFile file, List<Path> storedPaths) {
        String contentType = file.getContentType();
        String extension = EXTENSIONS.get(contentType);
        String token = UUID.randomUUID().toString().replace("-", "");
        Path directory = storageRoot.resolve(String.valueOf(workOrderId)).normalize();
        if (!directory.startsWith(storageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage path");
        }
        Path target = directory.resolve(token + "-" + index + extension).normalize();
        if (!target.startsWith(directory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
        }
        try {
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            storedPaths.add(target);
            NewAttachment document = new NewAttachment();
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            document.setDocumentNo("MNT-" + date + "-" + token.substring(0, 8).toUpperCase());
            document.setOriginalName(safeOriginalName(file.getOriginalFilename()));
            document.setStorageKey(storageRoot.relativize(target).toString().replace('\\', '/'));
            document.setMimeType(contentType);
            document.setFileSize(file.getSize());
            document.setChecksumSha256(sha256(target));
            document.setUploadedBy(userId);
            return document;
        } catch (IOException exception) {
            deleteQuietly(target);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to store maintenance attachment", exception);
        }
    }

    private void validate(String relationType, List<MultipartFile> files) {
        if (!RELATION_TYPES.contains(relationType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attachment type");
        }
        if (files == null || files.isEmpty() || files.size() > MAX_FILES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload between 1 and 6 files");
        }
        for (MultipartFile file : files) {
            String contentType = file == null ? null : file.getContentType();
            boolean supported = file != null && !file.isEmpty() && file.getSize() <= MAX_FILE_SIZE
                    && EXTENSIONS.containsKey(contentType);
            if (!supported) {
                String message = "Only JPG, PNG or PDF files up to 10MB are supported";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
            }
        }
    }

    private String safeOriginalName(String value) {
        String name = value == null ? "maintenance-attachment" : value.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\t]", "_");
        if (name.isBlank()) name = "maintenance-attachment";
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path);
                    DigestInputStream stream = new DigestInputStream(input, digest)) {
                stream.transferTo(java.io.OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void deleteQuietly(Path path) {
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
    }

    public record Download(Path path, String originalName, String mimeType, long size) { }
}
