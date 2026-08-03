package com.ccps.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminPropertyAttachmentResponse;
import com.ccps.backend.mapper.AdminPropertyAttachmentMapper;
import com.ccps.backend.mapper.AdminPropertyAttachmentMapper.AttachmentRow;
import com.ccps.backend.mapper.AdminPropertyAttachmentMapper.DocumentRow;

@Service
public class AdminPropertyAttachmentService {
    private static final long MAX_SIZE = 15L * 1024L * 1024L;
    private static final Set<String> EXTENSIONS = Set.of("pdf","doc","docx","xls","xlsx","txt","csv","jpg","jpeg","png");

    private final AdminPropertyAttachmentMapper mapper;
    private final Path root;

    public AdminPropertyAttachmentService(AdminPropertyAttachmentMapper mapper,
            @Value("${ccps.storage.property-attachments:uploads/property-attachments}") String root) {
        this.mapper = mapper;
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<AdminPropertyAttachmentResponse> list(Long ownerId, Long ownerUnitId) {
        requireProperty(ownerId, ownerUnitId);
        return mapper.list(ownerUnitId).stream().map(this::response).toList();
    }

    @Transactional
    public AdminPropertyAttachmentResponse create(Long actorId, Long ownerId, Long ownerUnitId, String title,
            String remarks, boolean enabled, MultipartFile file) {
        requireProperty(ownerId, ownerUnitId);
        validateMetadata(title, remarks);
        StoredFile stored = store(ownerUnitId, file);
        try {
            DocumentRow document = document(actorId, stored);
            if (mapper.insertDocument(document) != 1 || document.getId() == null) throw conflict("Unable to create attachment document");
            AttachmentRow row = new AttachmentRow(); row.setOwnerUnitId(ownerUnitId); row.setDocumentId(document.getId());
            row.setTitle(title.trim()); row.setRemarks(blankToNull(remarks)); row.setEnabled(enabled); row.setCreatedBy(actorId);
            if (mapper.insertAttachment(row) != 1 || row.getId() == null) throw conflict("Unable to create property attachment");
            return response(requireAttachment(ownerUnitId, row.getId()));
        } catch (RuntimeException error) {
            deleteQuietly(stored.path());
            throw error;
        }
    }

    @Transactional
    public AdminPropertyAttachmentResponse update(Long ownerId, Long ownerUnitId, Long attachmentId, String title,
            String remarks, boolean enabled, MultipartFile file) {
        requireProperty(ownerId, ownerUnitId);
        validateMetadata(title, remarks);
        AttachmentRow current = requireAttachment(ownerUnitId, attachmentId);
        StoredFile replacement = file == null || file.isEmpty() ? null : store(ownerUnitId, file);
        Path previous = resolve(current.getStorageKey());
        try {
            current.setTitle(title.trim()); current.setRemarks(blankToNull(remarks)); current.setEnabled(enabled);
            if (mapper.updateAttachment(current) != 1) throw conflict("Property attachment was changed by another request");
            if (replacement != null) {
                DocumentRow document = document(null, replacement); document.setId(current.getDocumentId());
                if (mapper.updateDocument(document) != 1) throw conflict("Unable to replace attachment file");
                deleteQuietly(previous);
            }
            return response(requireAttachment(ownerUnitId, attachmentId));
        } catch (RuntimeException error) {
            if (replacement != null) deleteQuietly(replacement.path());
            throw error;
        }
    }

    @Transactional
    public void delete(Long ownerId, Long ownerUnitId, Long attachmentId) {
        requireProperty(ownerId, ownerUnitId);
        AttachmentRow current = requireAttachment(ownerUnitId, attachmentId);
        if (mapper.deleteAttachment(ownerUnitId, attachmentId) != 1) throw conflict("Property attachment was changed by another request");
        mapper.deleteDocument(current.getDocumentId());
        deleteQuietly(resolve(current.getStorageKey()));
    }

    @Transactional(readOnly = true)
    public Download download(Long ownerId, Long ownerUnitId, Long attachmentId) {
        requireProperty(ownerId, ownerUnitId);
        AttachmentRow row = requireAttachment(ownerUnitId, attachmentId);
        Path path = resolve(row.getStorageKey());
        if (!Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment file not found");
        return new Download(path, row.getOriginalName(), row.getMimeType() == null ? "application/octet-stream" : row.getMimeType(),
                row.getFileSize() == null ? 0 : row.getFileSize());
    }

    private StoredFile store(Long ownerUnitId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw bad("Attachment file is required");
        if (file.getSize() > MAX_SIZE) throw bad("Attachment exceeds 15 MB");
        String originalName = safeName(file.getOriginalFilename());
        String extension = extension(originalName);
        if (!EXTENSIONS.contains(extension)) throw bad("Unsupported attachment type");
        String token = UUID.randomUUID().toString().replace("-", "");
        Path directory = root.resolve(String.valueOf(ownerUnitId)).normalize();
        Path path = directory.resolve(token + "." + extension).normalize();
        if (!directory.startsWith(root) || !path.startsWith(directory)) throw bad("Invalid attachment storage path");
        try {
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream()) { Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store property attachment");
        }
        String mimeType = file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream" : file.getContentType().toLowerCase(Locale.ROOT);
        return new StoredFile(path, root.relativize(path).toString().replace('\\','/'), originalName, mimeType, file.getSize());
    }

    private DocumentRow document(Long actorId, StoredFile stored) {
        DocumentRow row = new DocumentRow();
        row.setDocumentNo("PROPERTY-FILE-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0,8).toUpperCase());
        row.setOriginalName(stored.originalName()); row.setStorageKey(stored.storageKey()); row.setMimeType(stored.mimeType());
        row.setFileSize(stored.size()); row.setUploadedBy(actorId); return row;
    }

    private AdminPropertyAttachmentResponse response(AttachmentRow row) {
        String filePath = "/相關附件/" + row.getOriginalName();
        return new AdminPropertyAttachmentResponse(row.getId(),row.getOwnerUnitId(),row.getDocumentId(),row.getTitle(),
                filePath,row.getOriginalName(),row.getMimeType(),row.getFileSize(),row.getRemarks(),row.isEnabled(),
                row.getCreatedBy(),row.getCreatedByName(),row.getCreatedAt(),row.getUpdatedAt());
    }

    private void validateMetadata(String title, String remarks) {
        if (title == null || title.isBlank() || title.trim().length() > 160) throw bad("Attachment title is required and must not exceed 160 characters");
        if (remarks != null && remarks.length() > 1000) throw bad("Attachment remarks must not exceed 1000 characters");
    }
    private void requireProperty(Long ownerId, Long ownerUnitId) { if (mapper.ownsProperty(ownerId, ownerUnitId) != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Property not found"); }
    private AttachmentRow requireAttachment(Long ownerUnitId, Long attachmentId) { AttachmentRow row=mapper.find(ownerUnitId,attachmentId); if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Property attachment not found"); return row; }
    private Path resolve(String storageKey) { Path path=root.resolve(storageKey).normalize(); if(!path.startsWith(root))throw bad("Invalid attachment storage path"); return path; }
    private String safeName(String value) { String normalized=value==null?"attachment":value.replace('\\','/'); int index=normalized.lastIndexOf('/'); String name=index>=0?normalized.substring(index+1):normalized; return name.replaceAll("[\\r\\n]","_"); }
    private String extension(String name) { int index=name.lastIndexOf('.'); return index<0?"":name.substring(index+1).toLowerCase(Locale.ROOT); }
    private String blankToNull(String value) { return value==null||value.isBlank()?null:value.trim(); }
    private void deleteQuietly(Path path) { try { Files.deleteIfExists(path); } catch (IOException ignored) { } }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }

    private record StoredFile(Path path,String storageKey,String originalName,String mimeType,long size) { }
    public record Download(Path path,String originalName,String mimeType,long size) { }
}
