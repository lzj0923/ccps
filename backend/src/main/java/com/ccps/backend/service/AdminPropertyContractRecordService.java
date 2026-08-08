package com.ccps.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminPropertyContractRecordResponse;
import com.ccps.backend.dto.AdminPropertyLeaseOptionResponse;
import com.ccps.backend.mapper.AdminPropertyContractRecordMapper;
import com.ccps.backend.mapper.AdminPropertyContractRecordMapper.Row;
import com.ccps.backend.mapper.AdminPropertyContractRecordMapper.LeaseOptionRow;

@Service
public class AdminPropertyContractRecordService {
    private static final long MAX_FILE_SIZE = 15L * 1024L * 1024L;
    private static final Set<String> TYPES = Set.of(
            "A_HANDOVER_ASSISTANCE", "B_AGENCY", "C_MANAGEMENT", "D_RESALE",
            "E_LEASE_AMENDMENT", "F_RESALE_AMENDMENT", "L_LEASE", "O_LEASE_RESERVATION");
    private static final Set<String> STATUSES = Set.of("draft", "active", "completed", "cancelled");
    /** Handover assistance is event-based: one lease may have several handovers. */
    private static final Set<String> MULTI_INSTANCE_TYPES = Set.of("A_HANDOVER_ASSISTANCE");
    private static final Set<String> MIME_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final AdminPropertyContractRecordMapper mapper;
    private final Path root;

    public AdminPropertyContractRecordService(AdminPropertyContractRecordMapper mapper,
            @Value("${ccps.storage.property-contracts:uploads/property-contracts}") String root) {
        this.mapper = mapper;
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<AdminPropertyContractRecordResponse> list(Long ownerId, Long ownerUnitId) {
        requireProperty(ownerId, ownerUnitId);
        List<Row> rows = new ArrayList<>(mapper.list(ownerUnitId));
        rows.addAll(mapper.listLeaseContracts(ownerUnitId));
        return rows.stream()
                .sorted(Comparator.comparing(Row::getSignedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminPropertyLeaseOptionResponse> leaseOptions(Long ownerId, Long ownerUnitId) {
        requireProperty(ownerId, ownerUnitId);
        return mapper.leaseOptions(ownerUnitId).stream().map(this::leaseOption).toList();
    }

    @Transactional
    public AdminPropertyContractRecordResponse create(Long actorId, Long ownerId, Long ownerUnitId,
            Long leaseId, String contractType, String contractNo, LocalDate signedDate, LocalDate validFrom,
            LocalDate validTo, String status, String notes, MultipartFile file) {
        requireProperty(ownerId, ownerUnitId);
        leaseId = validateLease(ownerUnitId, leaseId, contractType);
        validate(contractType, contractNo, validFrom, validTo, status);
        Row existing = MULTI_INSTANCE_TYPES.contains(contractType) ? null
                : leaseId == null
                    ? mapper.findByPropertyAndType(ownerUnitId, contractType)
                    : mapper.findByLeaseAndType(ownerUnitId, leaseId, contractType);
        if (existing == null) {
            Row legacy = mapper.findByContractNo(ownerUnitId, contractNo.trim());
            if (legacy != null && legacy.getLeaseId() == null) existing = legacy;
        }
        if (existing != null) {
            StoredFile replacement = store(ownerUnitId, file, true);
            Path previous = resolve(existing.getStorageKey());
            existing.setLeaseId(leaseId);
            apply(existing, contractType, contractNo, signedDate, validFrom, validTo, status, notes, replacement);
            try {
                if (mapper.update(existing) != 1) throw conflict("Contract was changed by another request");
            } catch (RuntimeException error) {
                deleteQuietly(replacement.path());
                throw error;
            }
            deleteQuietly(previous);
            return response(mapper.find(ownerUnitId, existing.getId()));
        }
        StoredFile stored = store(ownerUnitId, file, true);
        Row row = new Row();
        row.setOwnerUnitId(ownerUnitId);
        row.setLeaseId(leaseId);
        apply(row, contractType, contractNo, signedDate, validFrom, validTo, status, notes, stored);
        row.setCreatedBy(actorId);
        try {
            if (mapper.insert(row) != 1 || row.getId() == null) throw conflict("Unable to create contract");
        } catch (RuntimeException error) {
            deleteQuietly(stored.path());
            throw error;
        }
        return response(mapper.find(ownerUnitId, row.getId()));
    }

    @Transactional
    public AdminPropertyContractRecordResponse update(Long ownerId, Long ownerUnitId, Long id,
            Long leaseId, String contractType, String contractNo, LocalDate signedDate, LocalDate validFrom,
            LocalDate validTo, String status, String notes, MultipartFile file) {
        requireProperty(ownerId, ownerUnitId);
        Row current = requireContract(ownerUnitId, id);
        leaseId = validateLease(ownerUnitId, leaseId, contractType);
        validate(contractType, contractNo, validFrom, validTo, status);
        StoredFile replacement = file == null || file.isEmpty() ? null : store(ownerUnitId, file, false);
        Path previous = resolve(current.getStorageKey());
        current.setLeaseId(leaseId);
        apply(current, contractType, contractNo, signedDate, validFrom, validTo, status, notes, replacement);
        try {
            if (mapper.update(current) != 1) throw conflict("Contract was changed by another request");
        } catch (RuntimeException error) {
            if (replacement != null) deleteQuietly(replacement.path());
            throw error;
        }
        if (replacement != null) deleteQuietly(previous);
        return response(mapper.find(ownerUnitId, id));
    }

    @Transactional
    public void delete(Long ownerId, Long ownerUnitId, Long id) {
        requireProperty(ownerId, ownerUnitId);
        Row row = requireContract(ownerUnitId, id);
        if (mapper.delete(ownerUnitId, id) != 1) throw conflict("Contract was changed by another request");
        deleteQuietly(resolve(row.getStorageKey()));
    }

    @Transactional(readOnly = true)
    public Download download(Long ownerId, Long ownerUnitId, Long id) {
        requireProperty(ownerId, ownerUnitId);
        Row row = requireContract(ownerUnitId, id);
        Path path = resolve(row.getStorageKey());
        if (!Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract attachment not found");
        return new Download(path, row.getOriginalName(), row.getMimeType(), row.getFileSize());
    }

    private void apply(Row row, String type, String no, LocalDate signedDate, LocalDate validFrom,
            LocalDate validTo, String status, String notes, StoredFile file) {
        row.setContractType(type);
        row.setContractNo(no.trim());
        row.setSignedDate(signedDate);
        row.setValidFrom(validFrom);
        row.setValidTo(validTo);
        row.setStatus(status);
        row.setNotes(blankToNull(notes));
        if (file != null) {
            row.setOriginalName(file.originalName());
            row.setStorageKey(file.storageKey());
            row.setMimeType(file.mimeType());
            row.setFileSize(file.size());
        }
    }

    private void validate(String type, String no, LocalDate from, LocalDate to, String status) {
        if ("L_LEASE".equals(type)) throw bad("Lease contracts are managed from the linked lease");
        if (!TYPES.contains(type)) throw bad("Unsupported contract type");
        if (no == null || no.isBlank() || no.trim().length() > 80) throw bad("Contract number is required");
        if (!STATUSES.contains(status)) throw bad("Unsupported contract status");
        if (from != null && to != null && to.isBefore(from)) throw bad("Valid end date cannot be before start date");
    }

    private Long validateLease(Long ownerUnitId, Long leaseId, String contractType) {
        if (leaseId == null) {
            if ("L_LEASE".equals(contractType)) throw bad("L.租賃合約必須關聯既有租約");
            return null;
        }
        if (mapper.leaseBelongsToProperty(ownerUnitId, leaseId) != 1) throw bad("Selected lease does not belong to this property");
        return leaseId;
    }

    private StoredFile store(Long ownerUnitId, MultipartFile file, boolean required) {
        if (file == null || file.isEmpty()) {
            if (required) throw bad("Contract attachment is required");
            return null;
        }
        if (file.getSize() > MAX_FILE_SIZE) throw bad("Contract attachment exceeds 15 MB");
        String mime = file.getContentType();
        if (mime == null || !MIME_TYPES.contains(mime)) throw bad("Only PDF, Word, JPG and PNG attachments are supported");
        String original = safeName(file.getOriginalFilename());
        String ext = extension(original, mime);
        String token = UUID.randomUUID().toString().replace("-", "");
        Path directory = root.resolve(String.valueOf(ownerUnitId)).normalize();
        Path target = directory.resolve(token + ext).normalize();
        if (!target.startsWith(root)) throw bad("Invalid attachment path");
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store contract attachment");
        }
        return new StoredFile(original, root.relativize(target).toString().replace('\\', '/'), mime, file.getSize(), target);
    }

    private String extension(String name, String mime) {
        String lower = name.toLowerCase();
        for (String ext : List.of(".pdf", ".doc", ".docx", ".jpg", ".jpeg", ".png")) {
            if (lower.endsWith(ext)) return ext;
        }
        return switch (mime) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "application/msword" -> ".doc";
            default -> ".docx";
        };
    }

    private String safeName(String name) {
        String value = name == null ? "contract-file" : name.replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1);
        if (value.isBlank()) value = "contract-file";
        return value.length() > 255 ? value.substring(value.length() - 255) : value;
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) throw bad("Invalid attachment path");
        return path;
    }

    private void requireProperty(Long ownerId, Long ownerUnitId) {
        if (mapper.ownsProperty(ownerId, ownerUnitId) != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner property not found");
    }

    private Row requireContract(Long ownerUnitId, Long id) {
        Row row = mapper.find(ownerUnitId, id);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Contract not found");
        return row;
    }

    private AdminPropertyContractRecordResponse response(Row row) {
        return new AdminPropertyContractRecordResponse(row.getId(), row.getOwnerUnitId(), row.getLeaseId(),
                row.getLeaseNo(), row.getTenantName(), row.getLeaseStart(), row.getLeaseEnd(), row.getMonthlyRent(),
                row.getLeaseStatus(), row.getContractType(),
                row.getContractNo(), row.getSignedDate(), row.getValidFrom(), row.getValidTo(), row.getStatus(),
                row.getNotes(), row.getOriginalName(), row.getMimeType(), row.getFileSize(), row.getCreatedAt(), row.getUpdatedAt());
    }

    private AdminPropertyLeaseOptionResponse leaseOption(LeaseOptionRow row) {
        return new AdminPropertyLeaseOptionResponse(row.getLeaseId(), row.getRentalMandateId(), row.getLeaseNo(), row.getTenantId(),
                row.getTenantName(), row.getStartDate(), row.getEndDate(), row.getMonthlyRent(),
                row.getDepositAmount(), row.getPaymentDay(), row.getStatus(), row.isLinked(), row.getSignatureStatus());
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private void deleteQuietly(Path path) { if (path != null) try { Files.deleteIfExists(path); } catch (IOException ignored) { } }

    private record StoredFile(String originalName, String storageKey, String mimeType, Long size, Path path) { }
    public record Download(Path path, String originalName, String mimeType, Long size) { }
}
