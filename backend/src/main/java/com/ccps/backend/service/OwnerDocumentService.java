package com.ccps.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.OwnerDocumentResponse;
import com.ccps.backend.dto.OwnerDocumentResponse.Category;
import com.ccps.backend.dto.OwnerDocumentResponse.DocumentItem;
import com.ccps.backend.dto.OwnerDocumentResponse.Summary;
import com.ccps.backend.mapper.OwnerDocumentMapper;
import com.ccps.backend.mapper.OwnerDocumentMapper.DocumentFile;
import com.ccps.backend.mapper.OwnerDocumentMapper.DocumentRow;

@Service
public class OwnerDocumentService {
    private final OwnerDocumentMapper mapper;
    private final List<Path> storageRoots;

    @Autowired
    public OwnerDocumentService(OwnerDocumentMapper mapper,
            @Value("${ccps.storage.document-root:uploads}") String documentRoot,
            @Value("${ccps.storage.payment-proofs:uploads/payment-proofs}") String paymentProofRoot,
            @Value("${ccps.storage.maintenance-attachments:uploads/maintenance-attachments}") String maintenanceRoot,
            @Value("${ccps.storage.reserve-topups:uploads/reserve-topups}") String reserveTopupRoot,
            @Value("${ccps.storage.lease-contracts:uploads/lease-contracts}") String leaseContractRoot,
            @Value("${ccps.storage.rental-mandates:uploads/rental-mandates}") String rentalMandateRoot,
            @Value("${ccps.storage.electronic-signatures:uploads/electronic-signatures}") String signatureRoot,
            @Value("${ccps.storage.property-contracts:uploads/property-contracts}") String propertyContractRoot,
            @Value("${ccps.storage.property-attachments:uploads/property-attachments}") String propertyAttachmentRoot,
            @Value("${ccps.storage.property-handover-reports:uploads/property-handover-reports}") String handoverReportRoot,
            @Value("${ccps.storage.property-cashflow:uploads/property-cashflow}") String propertyCashflowRoot,
            @Value("${ccps.storage.reports:uploads/reports}") String reportRoot) {
        this(mapper, List.of(documentRoot, paymentProofRoot, maintenanceRoot, reserveTopupRoot,
                leaseContractRoot, rentalMandateRoot, signatureRoot, propertyContractRoot,
                propertyAttachmentRoot, handoverReportRoot, propertyCashflowRoot, reportRoot).stream()
                .map(Path::of).toList());
    }

    OwnerDocumentService(OwnerDocumentMapper mapper, Path storageRoot) {
        this(mapper, List.of(storageRoot, storageRoot.resolve("payment-proofs"),
                storageRoot.resolve("maintenance-attachments"), storageRoot.resolve("reserve-topups"),
                storageRoot.resolve("lease-contracts"), storageRoot.resolve("rental-mandates"),
                storageRoot.resolve("electronic-signatures"), storageRoot.resolve("property-contracts"),
                storageRoot.resolve("property-attachments"), storageRoot.resolve("property-handover-reports"),
                storageRoot.resolve("property-cashflow"), storageRoot.resolve("reports"),
                storageRoot.resolve("documents")));
    }

    private OwnerDocumentService(OwnerDocumentMapper mapper, List<Path> storageRoots) {
        this.mapper = mapper;
        this.storageRoots = storageRoots.stream().map(path -> path.toAbsolutePath().normalize()).distinct().toList();
    }

    @Transactional(readOnly = true)
    public OwnerDocumentResponse getDocuments(Long userId) {
        List<DocumentRow> rows = mapper.findDocuments(userId);
        List<DocumentItem> items = rows.stream().map(this::toItem).toList();
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("all", items.size());
        items.forEach(item -> counts.merge(item.category(), 1, Integer::sum));
        List<Category> categories = List.of(
                new Category("all", "全部文件", counts.getOrDefault("all", 0)),
                new Category("sale", "買賣合約", counts.getOrDefault("sale", 0)),
                new Category("lease", "租賃合約", counts.getOrDefault("lease", 0)),
                new Category("proof", "付款憑證", counts.getOrDefault("proof", 0)),
                new Category("receipt", "收據發票", counts.getOrDefault("receipt", 0)),
                new Category("maintenance", "維修單據", counts.getOrDefault("maintenance", 0)),
                new Category("finance", "財務確認", counts.getOrDefault("finance", 0)),
                new Category("other", "其他資料", counts.getOrDefault("other", 0)));
        LocalDate today = LocalDate.now();
        int pending = (int) items.stream().filter(item -> "待簽署".equals(item.status())).count();
        int monthNew = (int) items.stream().filter(item -> item.createdAt() != null
                && item.createdAt().getYear() == today.getYear()
                && item.createdAt().getMonth() == today.getMonth()).count();
        int expiring = (int) items.stream().filter(item -> item.expiresAt() != null
                && !item.expiresAt().isBefore(today)
                && !item.expiresAt().isAfter(today.plusDays(30))).count();
        return new OwnerDocumentResponse(new Summary(items.size(), pending, monthNew, expiring), categories, items);
    }

    @Transactional(readOnly = true)
    public Download download(Long userId, Long documentId) {
        DocumentFile file = mapper.findDocumentFile(userId, documentId);
        if (file == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        Path target = resolveStoragePath(file.getStorageKey());
        if (target == null || !Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file is unavailable");
        }
        long size = file.getFileSize() == null ? fileSize(target) : file.getFileSize();
        return new Download(target, safeName(file.getOriginalName()),
                file.getMimeType() == null ? "application/octet-stream" : file.getMimeType(), size);
    }

    private DocumentItem toItem(DocumentRow row) {
        String category = category(row.getDocumentType(), row.getLinkEntityType());
        LocalDate today = LocalDate.now();
        String status = status(row.getStatus(), row.getExpiresAt(), today);
        return new DocumentItem(row.getId(), row.getDocumentNo() == null ? "DOC-" + row.getId() : row.getDocumentNo(),
                row.getOriginalName(), category, typeLabel(row.getDocumentType(), row.getLinkEntityType()), status, row.getMimeType(),
                row.getFileSize(), row.getCreatedAt(), row.getUpdatedAt(), row.getExpiresAt(), row.getUploaderName(),
                row.getProjectName(), row.getCity(), row.getUnitNo(), resolveStoragePath(row.getStorageKey()) != null);
    }

    private String category(String type, String linkEntityType) {
        String value = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if (value.contains("purchase")) return "sale";
        if ("lease".equals(linkEntityType)
                || value.equals("lease") || value.equals("lease_period")
                || value.contains("tenancy") || value.contains("rental_contract")) return "lease";
        if (value.contains("payment") || value.contains("proof")) return "proof";
        if (value.contains("invoice") || value.contains("receipt")) return "receipt";
        if (value.contains("maintenance") || value.contains("work_order") || value.contains("repair")) return "maintenance";
        if (value.contains("finance")) return "finance";
        return "other";
    }

    private String typeLabel(String type, String linkEntityType) {
        String value = type == null ? "" : type.toLowerCase(Locale.ROOT);
        if ("lease".equals(linkEntityType) || value.equals("lease") || value.equals("lease_period")
                || value.contains("tenancy") || value.contains("rental_contract")) return "租賃合約";
        return Map.ofEntries(
                Map.entry("purchase_contract", "買賣合約"),
                Map.entry("payment_proof", "付款憑證"),
                Map.entry("reserve_topup_proof", "預備金憑證"),
                Map.entry("invoice", "收據發票"),
                Map.entry("receipt", "收據發票"),
                Map.entry("maintenance_attachment", "維修單據"),
                Map.entry("handover_repair_attachment", "維修單據"),
                Map.entry("finance_confirmation", "財務確認"),
                Map.entry("cashflow_attachment", "收支附件"),
                Map.entry("property_attachment", "其他附件"),
                Map.entry("authorization_draft", "授權委託書"),
                Map.entry("mandate_document", "代管文件"),
                Map.entry("handover_photo", "交屋文件"),
                Map.entry("inventory", "交屋文件"),
                Map.entry("signed_contract", "簽署文件")
        ).getOrDefault(value, "其他資料");
    }

    private String status(String value, LocalDate expiresAt, LocalDate today) {
        if (expiresAt != null && !expiresAt.isBefore(today) && !expiresAt.isAfter(today.plusDays(30))) return "即將到期";
        return Map.of("active", "已生效", "confirmed", "已確認", "pending_review", "處理中",
                "pending_signature", "待簽署", "expired", "已過期").getOrDefault(value, value == null ? "處理中" : value);
    }

    private Path resolveStoragePath(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) return null;
        for (Path root : storageRoots) {
            Path target = root.resolve(storageKey).normalize();
            if (target.startsWith(root) && Files.isRegularFile(target)) return target;
        }
        return null;
    }

    private long fileSize(Path path) {
        try { return Files.size(path); } catch (IOException ignored) { return 0; }
    }

    private String safeName(String name) {
        if (name == null || name.isBlank()) return "document";
        return name.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "_");
    }

    public record Download(Path path, String originalName, String mimeType, long size) {}
}
