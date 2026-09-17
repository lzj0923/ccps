package com.ccps.backend.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminPropertyPhotoResponse;
import com.ccps.backend.dto.AdminPropertyPhotoVersionResponse;
import com.ccps.backend.mapper.AdminPropertyPhotoMapper;
import com.ccps.backend.mapper.AdminPropertyPhotoMapper.DocumentRow;
import com.ccps.backend.mapper.AdminPropertyPhotoMapper.PhotoRow;

@Service
public class AdminPropertyPhotoService {
    private static final long MAX_SIZE = 10L * 1024L * 1024L;
    private static final Set<String> CATEGORIES = Set.of("exterior", "interior", "facility", "defect", "floor_plan", "other", "handover_keys", "handover_living_room", "handover_dining_room", "handover_kitchen", "handover_master_bedroom", "handover_master_bathroom", "handover_defect_deposit", "handover_defect_owner_repair");
    private static final Map<String, String> EXTENSIONS = Map.of("image/jpeg", ".jpg", "image/png", ".png");

    private final AdminPropertyPhotoMapper mapper;
    private final Path root;

    public AdminPropertyPhotoService(AdminPropertyPhotoMapper mapper,
            @Value("${ccps.storage.property-photos:uploads/property-photos}") String root) {
        this.mapper = mapper;
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public List<AdminPropertyPhotoResponse> list(Long ownerId, Long ownerUnitId, Long leaseId, String versionMonth) {
        requireProperty(ownerId, ownerUnitId);
        if (leaseId == null) return mapper.listRegular(ownerUnitId, blankToNull(versionMonth)).stream().map(this::response).toList();
        requireLease(ownerUnitId, leaseId);
        return mapper.listRental(ownerUnitId, leaseId).stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminPropertyPhotoVersionResponse> versions(Long ownerId, Long ownerUnitId) {
        requireProperty(ownerId, ownerUnitId);
        return mapper.listVersions(ownerUnitId).stream()
                .map(row -> new AdminPropertyPhotoVersionResponse(row.getVersionMonth(),
                        row.getPhotoCount() == null ? 0 : row.getPhotoCount(), row.getCoverPhotoId(), row.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PhotoAsset> regularAssetsForLease(Long leaseId, String selectedPhotoIds) {
        if (leaseId == null) return List.of();
        List<PhotoRow> rows = mapper.listRegularByLease(leaseId);
        if (selectedPhotoIds != null) {
            if (selectedPhotoIds.isBlank()) return List.of();
            Map<Long, PhotoRow> rowsById = new LinkedHashMap<>();
            rows.forEach(row -> rowsById.put(row.getId(), row));
            List<PhotoRow> selected = new ArrayList<>();
            for (String value : selectedPhotoIds.split(",")) {
                try {
                    PhotoRow row = rowsById.get(Long.valueOf(value.trim()));
                    if (row != null && !selected.contains(row)) selected.add(row);
                } catch (NumberFormatException ignored) { }
            }
            rows = selected;
        }
        List<PhotoAsset> assets = new ArrayList<>();
        for (PhotoRow row : rows) {
            Path path = resolve(row.getStorageKey());
            if (Files.isRegularFile(path)) assets.add(new PhotoAsset(path, row.getMimeType(), assets.size(), false));
        }
        return List.copyOf(assets);
    }

    @Transactional
    public AdminPropertyPhotoResponse create(Long actorId, Long ownerId, Long ownerUnitId, String title,
            String category, String description, Integer sortOrder, boolean cover, Long leaseId,String rentalStage,
            String versionMonth, MultipartFile file) {
        requireProperty(ownerId, ownerUnitId);
        validateMetadata(ownerUnitId,title, category, description, sortOrder,cover,leaseId,rentalStage);
        boolean rental = leaseId != null;
        String photoVersion = rental ? null : normalizeVersionMonth(versionMonth);
        StoredFile stored = store(ownerUnitId, file);
        String resolvedTitle = resolveTitle(title, stored.originalName());
        try {
            DocumentRow document = document(actorId, stored);
            if (mapper.insertDocument(document) != 1 || document.getId() == null) throw conflict("Unable to create photo document");
            PhotoRow photo = photo(ownerUnitId,leaseId,rentalStage,photoVersion, document.getId(), resolvedTitle, category, description, sortOrder,
                    !rental && (cover || mapper.countRegular(ownerUnitId, photoVersion) == 0));
            if (photo.isCoverFlag()) mapper.clearRegularCover(ownerUnitId, photoVersion);
            if (mapper.insertPhoto(photo) != 1 || photo.getId() == null) throw conflict("Unable to create property photo");
            if (!rental) ensureCover(ownerUnitId, photoVersion);
            return response(requirePhoto(ownerUnitId, photo.getId()));
        } catch (RuntimeException error) {
            deleteQuietly(stored.path());
            throw error;
        }
    }

    @Transactional
    public AdminPropertyPhotoResponse update(Long ownerId, Long ownerUnitId, Long photoId, String title,
            String category, String description, Integer sortOrder, boolean cover,Long leaseId,String rentalStage,
            String versionMonth, MultipartFile file) {
        requireProperty(ownerId, ownerUnitId);
        validateMetadata(ownerUnitId,title, category, description, sortOrder,cover,leaseId,rentalStage);
        PhotoRow current = requirePhoto(ownerUnitId, photoId);
        boolean currentRental = current.getLeaseId() != null;
        boolean requestedRental = leaseId != null;
        if (currentRental != requestedRental) throw bad("Property and rental photos cannot be converted into each other");
        if (currentRental && !current.getLeaseId().equals(leaseId)) throw bad("Rental photo cannot be moved to another lease");
        String previousVersion = current.getVersionMonth();
        String photoVersion = requestedRental ? null : normalizeVersionMonth(
                blankToNull(versionMonth) == null ? previousVersion : versionMonth);
        StoredFile replacement = file == null || file.isEmpty() ? null : store(ownerUnitId, file);
        Path previous = resolve(current.getStorageKey());
        String resolvedTitle = resolveTitle(title, replacement == null ? current.getOriginalName() : replacement.originalName());
        try {
            current.setLeaseId(leaseId);current.setRentalStage(blankToNull(rentalStage));current.setVersionMonth(photoVersion);current.setTitle(resolvedTitle); current.setCategory(category); current.setDescription(blankToNull(description));
            current.setSortOrder(sortOrder == null ? 0 : sortOrder); current.setCoverFlag(cover);
            if (cover && !requestedRental) mapper.clearRegularCover(ownerUnitId, photoVersion);
            if (mapper.updatePhoto(current) != 1) throw conflict("Property photo was changed by another request");
            if (replacement != null) {
                DocumentRow document = document(null, replacement); document.setId(current.getDocumentId());
                if (mapper.updateDocument(document) != 1) throw conflict("Unable to replace photo file");
            }
            if (!requestedRental) {
                ensureCover(ownerUnitId, previousVersion);
                if (!photoVersion.equals(previousVersion)) ensureCover(ownerUnitId, photoVersion);
            }
            if (replacement != null) deleteQuietly(previous);
            return response(requirePhoto(ownerUnitId, photoId));
        } catch (RuntimeException error) {
            if (replacement != null) deleteQuietly(replacement.path());
            throw error;
        }
    }

    @Transactional
    public void delete(Long ownerId, Long ownerUnitId, Long photoId) {
        requireProperty(ownerId, ownerUnitId);
        PhotoRow current = requirePhoto(ownerUnitId, photoId);
        if (mapper.deletePhoto(ownerUnitId, photoId) != 1) throw conflict("Property photo was changed by another request");
        mapper.deleteDocument(current.getDocumentId());
        if (current.getLeaseId() == null) ensureCover(ownerUnitId, current.getVersionMonth());
        deleteQuietly(resolve(current.getStorageKey()));
    }

    @Transactional(readOnly = true)
    public Download content(Long ownerId, Long ownerUnitId, Long photoId) {
        requireProperty(ownerId, ownerUnitId);
        PhotoRow row = requirePhoto(ownerUnitId, photoId);
        Path path = resolve(row.getStorageKey());
        if (!Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo file not found");
        return new Download(path, row.getOriginalName(), row.getMimeType(), row.getFileSize() == null ? 0 : row.getFileSize());
    }

    private void validateMetadata(Long ownerUnitId,String title, String category, String description, Integer sortOrder,
            boolean cover,Long leaseId,String rentalStage) {
        if (title != null && !title.isBlank() && title.trim().length() > 120) throw bad("Photo title must not exceed 120 characters");
        if (!CATEGORIES.contains(category)) throw bad("Unsupported photo category");
        if (description != null && description.length() > 500) throw bad("Photo description must not exceed 500 characters");
        if (sortOrder != null && (sortOrder < 0 || sortOrder > 9999)) throw bad("Photo order must be between 0 and 9999");
        String stage=blankToNull(rentalStage);
        if(stage!=null&&!Set.of("before","after").contains(stage))throw bad("Unsupported rental photo stage");
        if((leaseId==null)!=(stage==null))throw bad("Rental photos require both a lease and a rental stage");
        if(leaseId!=null){requireLease(ownerUnitId,leaseId);if(cover)throw bad("Rental photos cannot be used as property cover");}
    }

    private StoredFile store(Long ownerUnitId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw bad("Photo file is required");
        String mimeType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String extension = EXTENSIONS.get(mimeType);
        if (extension == null) throw bad("Only JPG and PNG photos are supported");
        if (file.getSize() > MAX_SIZE) throw bad("Photo exceeds 10 MB");
        try (InputStream input = file.getInputStream()) {
            BufferedImage image = ImageIO.read(input);
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) throw bad("Uploaded file is not a valid image");
        } catch (IOException error) {
            throw bad("Unable to read uploaded photo");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        Path directory = root.resolve(String.valueOf(ownerUnitId)).normalize();
        Path path = directory.resolve(token + extension).normalize();
        if (!directory.startsWith(root) || !path.startsWith(directory)) throw bad("Invalid photo storage path");
        try {
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException error) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store property photo");
        }
        return new StoredFile(path, root.relativize(path).toString().replace('\\', '/'),
                safeName(file.getOriginalFilename()), mimeType, file.getSize());
    }

    private DocumentRow document(Long actorId, StoredFile stored) {
        DocumentRow row = new DocumentRow();
        row.setDocumentNo("PHOTO-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        row.setOriginalName(stored.originalName()); row.setStorageKey(stored.storageKey());
        row.setMimeType(stored.mimeType()); row.setFileSize(stored.size()); row.setUploadedBy(actorId);
        return row;
    }

    private PhotoRow photo(Long ownerUnitId,Long leaseId,String rentalStage,String versionMonth, Long documentId, String title, String category,
            String description, Integer sortOrder, boolean cover) {
        PhotoRow row = new PhotoRow(); row.setOwnerUnitId(ownerUnitId);row.setLeaseId(leaseId);row.setRentalStage(blankToNull(rentalStage));row.setVersionMonth(versionMonth); row.setDocumentId(documentId);
        row.setTitle(title.trim()); row.setCategory(category); row.setDescription(blankToNull(description));
        row.setSortOrder(sortOrder == null ? 0 : sortOrder); row.setCoverFlag(cover); return row;
    }

    private void ensureCover(Long ownerUnitId, String versionMonth) {
        if (versionMonth != null && mapper.countRegular(ownerUnitId, versionMonth) > 0
                && mapper.regularCoverCount(ownerUnitId, versionMonth) == 0) {
            mapper.assignFirstRegularCover(ownerUnitId, versionMonth);
        }
    }
    private void requireProperty(Long ownerId, Long ownerUnitId) { if (mapper.ownsProperty(ownerId, ownerUnitId) != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"); }
    private void requireLease(Long ownerUnitId,Long leaseId){if(mapper.leaseBelongs(ownerUnitId,leaseId)!=1)throw bad("Lease does not belong to this property");}
    private PhotoRow requirePhoto(Long ownerUnitId, Long photoId) { PhotoRow row=mapper.find(ownerUnitId, photoId); if(row==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Property photo not found"); return row; }
    private AdminPropertyPhotoResponse response(PhotoRow row) { return new AdminPropertyPhotoResponse(row.getId(),row.getOwnerUnitId(),row.getLeaseId(),row.getRentalStage(),row.getVersionMonth(),row.getDocumentId(),row.getTitle(),row.getCategory(),row.getDescription(),row.getSortOrder(),row.isCoverFlag(),row.getOriginalName(),row.getMimeType(),row.getFileSize(),row.getCreatedAt(),row.getUpdatedAt()); }
    private Path resolve(String storageKey) { Path path=root.resolve(storageKey).normalize(); if(!path.startsWith(root))throw bad("Invalid photo storage path"); return path; }
    private String safeName(String value) { String normalized=value==null?"photo":value.replace('\\','/'); int index=normalized.lastIndexOf('/'); String name=index>=0?normalized.substring(index+1):normalized; return name.replaceAll("[\\r\\n]", "_"); }
    private String resolveTitle(String title, String originalName) {
        String resolved = blankToNull(title);
        if (resolved == null) resolved = blankToNull(originalName);
        if (resolved == null) resolved = "photo";
        return resolved.length() <= 120 ? resolved : resolved.substring(0, 120);
    }
    private String blankToNull(String value) { return value==null||value.isBlank()?null:value.trim(); }
    private String normalizeVersionMonth(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) return YearMonth.now().toString();
        requireVersionMonth(normalized);
        return normalized;
    }
    private void requireVersionMonth(String value) {
        try { YearMonth.parse(value); }
        catch (RuntimeException exception) { throw bad("Photo version must use YYYY-MM format"); }
    }
    private void deleteQuietly(Path path) { try { Files.deleteIfExists(path); } catch (IOException ignored) { } }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }

    private record StoredFile(Path path, String storageKey, String originalName, String mimeType, long size) { }
    public record Download(Path path, String originalName, String mimeType, long size) { }
    public record PhotoAsset(Path path, String mimeType, int sortOrder, boolean coverFlag) { }
}
