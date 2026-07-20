package com.ccps.backend.service;

import java.math.BigDecimal;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminLeaseCreateRequest;
import com.ccps.backend.dto.AdminLeaseUpdateRequest;
import com.ccps.backend.dto.AdminLeaseTransferRequest;
import com.ccps.backend.dto.AdminRentFinanceResponse;
import com.ccps.backend.dto.AdminRentCollectionResponse;
import com.ccps.backend.dto.AdminRentCollectionRequest;
import com.ccps.backend.dto.AdminRecordCreateResponse;
import com.ccps.backend.dto.AdminTenancyOptionsResponse;
import com.ccps.backend.dto.AdminTenancyResponse;
import com.ccps.backend.dto.AdminTenantCreateRequest;
import com.ccps.backend.mapper.AdminTenancyMapper;
import com.ccps.backend.mapper.AdminTenancyMapper.ContractFile;
import com.ccps.backend.mapper.AdminTenancyMapper.LeaseContractContext;
import com.ccps.backend.mapper.AdminTenancyMapper.LeaseChangeContext;
import com.ccps.backend.mapper.AdminTenancyMapper.NewLease;
import com.ccps.backend.mapper.AdminTenancyMapper.NewContractDocument;
import com.ccps.backend.mapper.AdminTenancyMapper.NewTenant;
import com.ccps.backend.mapper.AdminTenancyMapper.RentFinanceRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentFinanceSummaryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentProofContext;
import com.ccps.backend.mapper.AdminTenancyMapper.RentCollectionRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentCollectionSummaryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentCollectionContext;
import com.ccps.backend.mapper.AdminTenancyMapper.NewRentCollection;
import com.ccps.backend.mapper.AdminTenancyMapper.SummaryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.TenancyRow;

@Service
public class AdminTenancyService {
    private static final Set<String> STATUSES = Set.of("paid", "partial", "unpaid", "overdue", "pending_review");
    private static final Set<String> RENT_PAYMENT_METHODS = Set.of("bank_transfer", "online_payment", "cash", "cheque");
    private static final long MAX_CONTRACT_SIZE = 10L * 1024 * 1024;
    private static final Map<String, String> CONTRACT_EXTENSIONS = Map.of(
            "application/pdf", ".pdf", "image/jpeg", ".jpg", "image/png", ".png");
    private final AdminTenancyMapper mapper;
    private final Clock clock;
    private final Path contractStorageRoot;
    private final Path rentProofStorageRoot;

    @Autowired
    public AdminTenancyService(AdminTenancyMapper mapper,
            @Value("${ccps.storage.lease-contracts:uploads/lease-contracts}") String storageRoot,
            @Value("${ccps.storage.payment-proofs:uploads/payment-proofs}") String rentProofRoot) {
        this(mapper, Clock.systemDefaultZone(), Path.of(storageRoot).toAbsolutePath().normalize(),
                Path.of(rentProofRoot).toAbsolutePath().normalize());
    }
    AdminTenancyService(AdminTenancyMapper mapper, Clock clock) {
        this(mapper, clock, Path.of("uploads/lease-contracts").toAbsolutePath().normalize(),
                Path.of("uploads/payment-proofs").toAbsolutePath().normalize());
    }
    AdminTenancyService(AdminTenancyMapper mapper, Clock clock, Path contractStorageRoot) {
        this(mapper, clock, contractStorageRoot, contractStorageRoot);
    }
    AdminTenancyService(AdminTenancyMapper mapper, Clock clock, Path contractStorageRoot, Path rentProofStorageRoot) {
        this.mapper = mapper; this.clock = clock; this.contractStorageRoot = contractStorageRoot;
        this.rentProofStorageRoot = rentProofStorageRoot;
    }

    @Transactional(readOnly = true)
    public AdminTenancyResponse find(int requestedPage, int requestedPageSize, String keyword,
            String projectName, String status) {
        int pageSize = Math.max(1, Math.min(requestedPageSize, 100));
        String normalizedStatus = normalize(status);
        if (normalizedStatus != null && !STATUSES.contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tenancy status");
        }
        String normalizedKeyword = normalize(keyword);
        String normalizedProject = normalize(projectName);
        long totalRows = zero(mapper.countPage(normalizedKeyword, normalizedProject, normalizedStatus));
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        List<AdminTenancyResponse.Item> rows = mapper.findPage(normalizedKeyword, normalizedProject,
                normalizedStatus, pageSize, (page - 1) * pageSize).stream().map(this::toItem).toList();
        SummaryRow source = mapper.findSummary();
        AdminTenancyResponse.Summary summary = new AdminTenancyResponse.Summary(
                source == null ? 0 : zero(source.getTenantCount()), source == null ? 0 : zero(source.getActiveLeaseCount()),
                source == null ? BigDecimal.ZERO : zero(source.getCurrentDue()), source == null ? BigDecimal.ZERO : zero(source.getCurrentPaid()),
                source == null ? BigDecimal.ZERO : zero(source.getCurrentUnpaid()), source == null ? 0 : zero(source.getPartialCount()),
                source == null ? 0 : zero(source.getOverdueCount()), source == null ? 0 : zero(source.getPendingReviewCount()));
        return new AdminTenancyResponse(summary, rows, new AdminTenancyResponse.Page(totalRows, page, pageSize, totalPages));
    }

    @Transactional(readOnly = true)
    public AdminTenancyOptionsResponse options() {
        return new AdminTenancyOptionsResponse(mapper.findProjects(), mapper.findTenantOptions(), mapper.findAvailableUnits());
    }

    @Transactional(readOnly = true)
    public AdminRentFinanceResponse findRentFinance(int requestedPage, int requestedPageSize, String keyword,
            String projectName, String status, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must not be before start date");
        }
        int pageSize = Math.max(1, Math.min(requestedPageSize, 100));
        String normalizedStatus = normalize(status);
        String confirmationStatus = normalizedStatus != null
                && Set.of("pending", "confirmed", "rejected", "history").contains(normalizedStatus) ? normalizedStatus : null;
        String syncStatus = switch (normalizedStatus == null ? "" : normalizedStatus) {
            case "sync_pending" -> "pending"; case "sync_failed" -> "failed"; case "not_synced", "synced" -> normalizedStatus; default -> null;
        };
        String proofStatus = "proof_uploaded".equals(normalizedStatus) || "proof_missing".equals(normalizedStatus)
                ? normalizedStatus : null;
        String normalizedKeyword = normalize(keyword); String normalizedProject = normalize(projectName);
        long totalRows = zero(mapper.countRentFinancePage(normalizedKeyword, normalizedProject, confirmationStatus,
                syncStatus, proofStatus, startDate, endDate));
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        List<AdminRentFinanceResponse.Item> rows = mapper.findRentFinancePage(normalizedKeyword, normalizedProject,
                confirmationStatus, syncStatus, proofStatus, startDate, endDate, pageSize, (page - 1) * pageSize)
                .stream().map(this::toRentFinanceItem).toList();
        RentFinanceSummaryRow source = mapper.findRentFinanceSummary();
        AdminRentFinanceResponse.Summary summary = new AdminRentFinanceResponse.Summary(
                source == null ? 0 : zero(source.getTotalCount()), source == null ? 0 : zero(source.getWithProofCount()),
                source == null ? 0 : zero(source.getMissingProofCount()), source == null ? 0 : zero(source.getPendingSyncCount()),
                source == null ? BigDecimal.ZERO : zero(source.getTotalAmount()),
                source == null ? BigDecimal.ZERO : zero(source.getMonthAmount()));
        return new AdminRentFinanceResponse(summary, rows,
                new AdminRentFinanceResponse.Page(totalRows, page, pageSize, totalPages));
    }

    @Transactional(readOnly = true)
    public List<String> rentFinanceProjects() { return mapper.findRentFinanceProjects(); }

    @Transactional(readOnly = true)
    public AdminRentCollectionResponse findRentCollections(int requestedPage, int requestedPageSize, String keyword,
            String projectName, String status, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must not be before start date");
        }
        String normalizedStatus = normalize(status);
        if (normalizedStatus != null && !Set.of("unpaid", "partial", "overdue").contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rent collection status");
        }
        int pageSize = Math.max(1, Math.min(requestedPageSize, 100));
        String normalizedKeyword = normalize(keyword); String normalizedProject = normalize(projectName);
        long totalRows = zero(mapper.countRentCollections(normalizedKeyword, normalizedProject, normalizedStatus, startDate, endDate));
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        List<AdminRentCollectionResponse.Item> rows = mapper.findRentCollections(normalizedKeyword, normalizedProject,
                normalizedStatus, startDate, endDate, pageSize, (page - 1) * pageSize).stream()
                .map(this::toRentCollectionItem).toList();
        RentCollectionSummaryRow source = mapper.findRentCollectionSummary();
        AdminRentCollectionResponse.Summary summary = new AdminRentCollectionResponse.Summary(
                source == null ? 0 : zero(source.getOutstandingCount()), source == null ? 0 : zero(source.getUnpaidCount()),
                source == null ? 0 : zero(source.getPartialCount()), source == null ? 0 : zero(source.getOverdueCount()),
                source == null ? BigDecimal.ZERO : zero(source.getOutstandingAmount()),
                source == null ? BigDecimal.ZERO : zero(source.getMonthReceived()));
        return new AdminRentCollectionResponse(summary, rows,
                new AdminRentCollectionResponse.Page(totalRows, page, pageSize, totalPages));
    }

    @Transactional(readOnly = true)
    public List<String> rentCollectionProjects() { return mapper.findRentCollectionProjects(); }

    @Transactional
    public AdminRecordCreateResponse confirmRentCollection(Long actorId, Long invoiceId,
            AdminRentCollectionRequest request, MultipartFile proof) {
        if (!RENT_PAYMENT_METHODS.contains(request.paymentMethod())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rent payment method");
        }
        LocalDate today = LocalDate.now(clock);
        if (request.paymentDate().isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rent payment date cannot be in the future");
        }
        if (!"cash".equals(request.paymentMethod()) && normalize(request.paymentReference()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment reference is required for non-cash rent collection");
        }
        RentCollectionContext context = mapper.lockRentCollection(invoiceId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rent invoice not found");
        BigDecimal beforePaid = zero(context.getAmountPaid());
        BigDecimal outstanding = zero(context.getAmountDue()).subtract(beforePaid).max(BigDecimal.ZERO);
        if (outstanding.signum() <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Rent invoice is already paid");
        if (request.amount().compareTo(outstanding) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Received amount exceeds the outstanding rent");
        }
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String timestamp = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String transactionNo = "RENT-ADM-" + timestamp + "-" + token;
        String receiptNo = "RENT-RCP-" + timestamp + "-" + token;
        String payerName = normalize(request.payerName()) == null ? context.getTenantName() : request.payerName().trim();
        String reference = normalize(request.paymentReference());
        String note = normalize(request.note()) == null ? "管理員確認租金收款" : request.note().trim();
        BigDecimal afterPaid = beforePaid.add(request.amount());

        NewRentCollection record = new NewRentCollection();
        record.setTransactionNo(transactionNo); record.setUnitId(context.getUnitId()); record.setOwnerId(context.getOwnerId());
        record.setTenantId(context.getTenantId()); record.setAmount(request.amount()); record.setPaymentDate(request.paymentDate());
        record.setPaymentMethod(request.paymentMethod()); record.setActorId(actorId);
        if (mapper.insertConfirmedRentPayment(record) != 1 || record.getId() == null
                || mapper.linkRentPayment(invoiceId, record.getId()) != 1
                || mapper.insertRentCollectionReceipt(record.getId(), receiptNo, payerName, reference, note) != 1
                || mapper.applyRentCollection(invoiceId, request.amount()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rent collection state changed; reload and try again");
        }
        mapper.insertRentCollectionAudit(actorId, record.getId(), invoiceId, beforePaid, request.amount(), afterPaid, note);
        mapper.insertTenantNotification(context.getTenantId(), "租金收款已確認",
                "%s %s 已確認收到租金 RM %s，本期尚欠 RM %s。".formatted(context.getProjectName(), context.getUnitNo(),
                        money(request.amount()), money(outstanding.subtract(request.amount()))),
                "rent_invoice", invoiceId, "normal");
        if (proof != null && !proof.isEmpty()) uploadRentProof(actorId, record.getId(), proof);
        return new AdminRecordCreateResponse(record.getId(), transactionNo);
    }

    @Transactional
    public Long createTenant(AdminTenantCreateRequest request) {
        String identity = normalize(request.identityNo());
        String email = normalize(request.email());
        if (identity != null && mapper.countTenantIdentity(identity) > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant identity number already exists");
        if (email != null && mapper.countTenantEmail(email) > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant email already exists");
        NewTenant tenant = new NewTenant(); tenant.setFullName(request.fullName().trim()); tenant.setIdentityNo(identity);
        tenant.setPhone(normalize(request.phone())); tenant.setEmail(email); tenant.setStatus(request.status());
        if (mapper.insertTenant(tenant) != 1 || tenant.getId() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create tenant");
        return tenant.getId();
    }

    @Transactional
    public Long createLease(AdminLeaseCreateRequest request) {
        if (request.startDate() == null || request.endDate() == null || request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lease end date must not be before start date");
        }
        if (mapper.countActiveTenant(request.tenantId()) != 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active tenant not found");
        if (mapper.countOperatingUnit(request.unitId()) != 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operating unit not found");
        if (mapper.countOverlappingLease(request.unitId(), request.startDate(), request.endDate()) > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Unit already has an overlapping active lease");
        NewLease lease = new NewLease(); lease.setTenantId(request.tenantId()); lease.setUnitId(request.unitId());
        lease.setLeaseNo("LEASE-" + LocalDate.now(clock).toString().replace("-", "") + "-" + Long.toHexString(System.nanoTime()).toUpperCase());
        lease.setStartDate(request.startDate()); lease.setEndDate(request.endDate()); lease.setMonthlyRent(request.monthlyRent());
        lease.setDepositAmount(request.depositAmount()); lease.setPaymentDay(request.paymentDay());
        if (mapper.insertLease(lease) != 1 || lease.getId() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create lease");
        mapper.activateRentalService(request.unitId()); mapper.markUnitRented(request.unitId());
        LocalDate today = LocalDate.now(clock); LocalDate month = today.withDayOfMonth(1);
        if (!today.isBefore(request.startDate()) && !today.isAfter(request.endDate())) {
            LocalDate due = month.withDayOfMonth(Math.min(request.paymentDay(), month.lengthOfMonth()));
            mapper.insertInvoice(lease.getId(), month, due, request.monthlyRent());
        }
        return lease.getId();
    }

    @Transactional
    public void updateLease(Long actorId, Long leaseId, AdminLeaseUpdateRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lease end date must not be before start date");
        }
        LeaseChangeContext current = mapper.lockLeaseForChange(leaseId);
        if (current == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease not found");
        if (!"active".equals(current.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active leases can be edited");
        }
        if (request.endDate().isBefore(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active lease end date cannot be in the past");
        }
        if (mapper.countOtherOverlappingLease(leaseId, current.getUnitId(), request.startDate(), request.endDate()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unit has another overlapping active lease");
        }
        if (mapper.updateLeaseTerms(leaseId, request.startDate(), request.endDate(), request.monthlyRent(),
                request.depositAmount(), request.paymentDay()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lease state changed; reload and try again");
        }
        mapper.updateFutureUnpaidInvoiceTerms(leaseId, LocalDate.now(clock).withDayOfMonth(1),
                request.monthlyRent(), request.paymentDay());
        mapper.insertLeaseUpdateAudit(actorId, leaseId, current.getStartDate(), current.getEndDate(),
                current.getMonthlyRent(), current.getDepositAmount(), current.getPaymentDay(), request.startDate(),
                request.endDate(), request.monthlyRent(), request.depositAmount(), request.paymentDay());
    }

    @Transactional
    public Long transferLease(Long actorId, Long leaseId, AdminLeaseTransferRequest request) {
        LeaseChangeContext current = mapper.lockLeaseForChange(leaseId);
        if (current == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease not found");
        if (!"active".equals(current.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active leases can be transferred");
        }
        LocalDate today = LocalDate.now(clock);
        if (request.newTenantId().equals(current.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a different tenant for the transfer");
        }
        if (mapper.countActiveTenant(request.newTenantId()) != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active receiving tenant not found");
        }
        if (request.transferDate().isBefore(today) || !request.transferDate().isAfter(current.getStartDate())
                || request.transferDate().isAfter(current.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Transfer date must be after the original start date, not earlier than today, and within the original lease");
        }
        if (request.endDate().isBefore(request.transferDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New lease end date must not be before transfer date");
        }
        if (mapper.countOtherOverlappingLease(leaseId, current.getUnitId(), request.transferDate(), request.endDate()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unit has another overlapping active lease");
        }
        if (mapper.closeLeaseForTransfer(leaseId, request.transferDate().minusDays(1)) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lease state changed; reload and try again");
        }
        mapper.deleteOldFutureInvoices(leaseId, request.transferDate().withDayOfMonth(1));

        NewLease transferred = new NewLease();
        transferred.setTenantId(request.newTenantId()); transferred.setUnitId(current.getUnitId());
        transferred.setLeaseNo("LEASE-TR-" + today.toString().replace("-", "") + "-" + Long.toHexString(System.nanoTime()).toUpperCase());
        transferred.setStartDate(request.transferDate()); transferred.setEndDate(request.endDate());
        transferred.setMonthlyRent(request.monthlyRent()); transferred.setDepositAmount(request.depositAmount());
        transferred.setPaymentDay(request.paymentDay());
        if (mapper.insertLease(transferred) != 1 || transferred.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create transferred lease");
        }
        LocalDate firstBillingMonth = request.transferDate().getDayOfMonth() == 1
                ? request.transferDate().withDayOfMonth(1) : request.transferDate().plusMonths(1).withDayOfMonth(1);
        if (!today.isBefore(request.transferDate()) && !today.isAfter(request.endDate())
                && !firstBillingMonth.isAfter(today.withDayOfMonth(1))) {
            LocalDate due = firstBillingMonth.withDayOfMonth(Math.min(request.paymentDay(), firstBillingMonth.lengthOfMonth()));
            mapper.insertInvoice(transferred.getId(), firstBillingMonth, due, request.monthlyRent());
        }
        mapper.insertLeaseTransferAudit(actorId, leaseId, transferred.getId(), current.getTenantId(),
                request.newTenantId(), current.getEndDate(), request.transferDate());
        return transferred.getId();
    }

    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void generateCurrentMonthInvoices() {
        mapper.generateMonthlyInvoices(LocalDate.now(clock).withDayOfMonth(1));
    }

    @Transactional
    public Long uploadContract(Long actorId, Long leaseId, MultipartFile file) {
        validateContract(file);
        LeaseContractContext context = mapper.lockLeaseContract(leaseId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease not found");
        String token = UUID.randomUUID().toString().replace("-", "");
        String extension = CONTRACT_EXTENSIONS.get(file.getContentType());
        Path directory = contractStorageRoot.resolve(String.valueOf(leaseId)).normalize();
        Path target = directory.resolve(token + extension).normalize();
        if (!directory.startsWith(contractStorageRoot) || !target.startsWith(directory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid contract storage path");
        }
        try {
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
            NewContractDocument document = new NewContractDocument();
            String timestamp = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            document.setDocumentNo("LEASE-DOC-" + timestamp + "-" + token.substring(0, 8).toUpperCase());
            document.setOriginalName(safeFileName(file.getOriginalFilename()));
            document.setStorageKey(contractStorageRoot.relativize(target).toString().replace('\\', '/'));
            document.setMimeType(file.getContentType()); document.setFileSize(file.getSize());
            document.setChecksumSha256(sha256(target)); document.setUploadedBy(actorId);
            if (mapper.insertContractDocument(document) != 1 || document.getId() == null
                    || mapper.insertContractLink(document.getId(), leaseId) != 1
                    || mapper.updateLeaseContract(leaseId, document.getId()) != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save lease contract");
            }
            Long oldDocumentId = context.getContractDocumentId();
            if (oldDocumentId != null && mapper.supersedeContractDocument(oldDocumentId) != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to replace lease contract");
            }
            mapper.insertContractAudit(actorId, leaseId, oldDocumentId, document.getId(),
                    document.getOriginalName(), oldDocumentId == null ? "upload_lease_contract" : "replace_lease_contract");
            return document.getId();
        } catch (IOException exception) {
            deleteQuietly(target);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store lease contract", exception);
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Download downloadContract(Long leaseId) {
        ContractFile file = mapper.findContractFile(leaseId);
        if (file == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease contract not found");
        Path target = contractStorageRoot.resolve(file.getStorageKey()).normalize();
        if (!target.startsWith(contractStorageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid contract path");
        }
        if (!Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease contract file is unavailable");
        }
        return new Download(target, safeFileName(file.getOriginalName()),
                normalize(file.getMimeType()) == null ? "application/octet-stream" : file.getMimeType(),
                file.getFileSize() == null ? 0 : file.getFileSize());
    }

    @Transactional(readOnly = true)
    public Download downloadRentProof(Long documentId) {
        ContractFile file = mapper.findRentProofFile(documentId);
        if (file == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rent payment proof not found");
        Path target = rentProofStorageRoot.resolve(file.getStorageKey()).normalize();
        if (!target.startsWith(rentProofStorageRoot)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rent proof path");
        if (!Files.isRegularFile(target)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rent payment proof file is unavailable");
        return new Download(target, safeFileName(file.getOriginalName()),
                normalize(file.getMimeType()) == null ? "application/octet-stream" : file.getMimeType(),
                file.getFileSize() == null ? 0 : file.getFileSize());
    }

    @Transactional
    public Long uploadRentProof(Long actorId, Long financeRecordId, MultipartFile file) {
        validateProof(file);
        RentProofContext context = mapper.lockRentProof(financeRecordId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rent payment record not found");
        String token = UUID.randomUUID().toString().replace("-", "");
        String extension = CONTRACT_EXTENSIONS.get(file.getContentType());
        Path directory = rentProofStorageRoot.resolve("rent").resolve(String.valueOf(financeRecordId)).normalize();
        Path target = directory.resolve(token + extension).normalize();
        if (!directory.startsWith(rentProofStorageRoot) || !target.startsWith(directory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rent proof storage path");
        }
        try {
            Files.createDirectories(directory);
            try (InputStream input = file.getInputStream()) { Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING); }
            NewContractDocument document = new NewContractDocument();
            String timestamp = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            document.setDocumentNo("RENT-PROOF-" + timestamp + "-" + token.substring(0, 8).toUpperCase());
            document.setOriginalName(safeFileName(file.getOriginalFilename()));
            document.setStorageKey(rentProofStorageRoot.relativize(target).toString().replace('\\', '/'));
            document.setMimeType(file.getContentType()); document.setFileSize(file.getSize());
            document.setChecksumSha256(sha256(target)); document.setUploadedBy(actorId);
            if (mapper.insertRentProofDocument(document) != 1 || document.getId() == null
                    || mapper.insertRentProofLink(document.getId(), financeRecordId) != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save rent payment proof");
            }
            mapper.updateRentReceiptProof(financeRecordId, document.getId());
            Long oldDocumentId = context.getProofDocumentId();
            if (oldDocumentId != null && mapper.supersedeRentProof(oldDocumentId) != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to replace rent payment proof");
            }
            mapper.insertRentProofAudit(actorId, financeRecordId, oldDocumentId, document.getId(),
                    document.getOriginalName(), oldDocumentId == null ? "upload_rent_proof" : "replace_rent_proof");
            return document.getId();
        } catch (IOException exception) {
            deleteQuietly(target);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store rent payment proof", exception);
        } catch (RuntimeException exception) {
            deleteQuietly(target); throw exception;
        }
    }

    @Transactional
    public void sendReminder(Long invoiceId) {
        var context = mapper.findReminder(invoiceId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rent invoice not found");
        BigDecimal outstanding = zero(context.getAmountDue()).subtract(zero(context.getAmountPaid())).max(BigDecimal.ZERO);
        if (outstanding.signum() <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Rent invoice is already paid");
        mapper.insertTenantNotification(context.getTenantId(), "租金繳付提醒",
                "%s %s 尚有租金 RM %s 待繳，到期日 %s。".formatted(context.getProjectName(), context.getUnitNo(), money(outstanding), context.getDueDate()),
                "rent_invoice", invoiceId, "high");
    }

    private AdminTenancyResponse.Item toItem(TenancyRow r) { return new AdminTenancyResponse.Item(
            r.getTenantId(), r.getTenantName(), r.getIdentityNo(), r.getPhone(), r.getEmail(), r.getTenantStatus(),
            r.getLeaseId(), r.getLeaseNo(), r.getProjectId(), r.getProjectName(), r.getUnitId(), r.getUnitNo(),
            r.getLeaseStart(), r.getLeaseEnd(), zero(r.getMonthlyRent()), zero(r.getDepositAmount()), r.getPaymentDay(), r.getLeaseStatus(),
            r.getContractDocumentId(), r.getContractDocumentName(), r.getContractDocumentMimeType(), r.getContractDocumentSize(),
            r.getInvoiceId(), r.getBillingMonth(), r.getDueDate(), zero(r.getAmountDue()), zero(r.getAmountPaid()),
            zero(r.getAmountUnpaid()), r.getRentStatus(), r.getFinanceRecordId(), r.getTransactionNo(), r.getConfirmationStatus(),
            r.getPaymentMethod(), r.getPaymentDate(), r.getReviewNote(), r.getConfirmedByName(), r.getConfirmedAt()); }
    private AdminRentFinanceResponse.Item toRentFinanceItem(RentFinanceRow r) { return new AdminRentFinanceResponse.Item(
            r.getId(), r.getTransactionNo(), r.getTenantName(), r.getProjectName(), r.getUnitNo(), r.getLeaseId(), r.getLeaseNo(),
            r.getInvoiceId(), r.getBillingMonth(), r.getDueDate(), zero(r.getInvoiceAmount()), zero(r.getInvoicePaid()),
            zero(r.getAmount()), r.getCurrency(), r.getTransactionDate(), r.getPaymentMethod(), r.getConfirmationStatus(),
            r.getSyncStatus(), r.getProofDocumentId(), r.getProofName(), r.getProofMimeType(), r.getProofSize(), r.getReviewNote(),
            r.getConfirmedByName(), r.getConfirmedAt(), r.getSubmittedAt()); }
    private AdminRentCollectionResponse.Item toRentCollectionItem(RentCollectionRow r) {
        return new AdminRentCollectionResponse.Item(r.getInvoiceId(), r.getLeaseId(), r.getLeaseNo(), r.getTenantName(),
                r.getProjectName(), r.getUnitNo(), r.getBillingMonth(), r.getDueDate(), zero(r.getAmountDue()),
                zero(r.getAmountPaid()), zero(r.getOutstandingAmount()), r.getCollectionStatus(), zero(r.getOverdueDays()),
                r.getLatestFinanceRecordId(), r.getLatestTransactionNo(), zero(r.getLatestPaymentAmount()),
                r.getLatestPaymentDate(), r.getLatestPaymentMethod(), r.getLatestProofDocumentId(), r.getLatestProofName(),
                r.getLatestProofMimeType(), r.getLatestProofSize(), r.getLatestConfirmedAt());
    }
    private String normalize(String v) { return v == null || v.trim().isEmpty() ? null : v.trim(); }
    private BigDecimal zero(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private long zero(Long v) { return v == null ? 0 : v; }
    private String money(BigDecimal v) { return zero(v).setScale(2).toPlainString(); }
    private void validateContract(MultipartFile file) {
        boolean valid = file != null && !file.isEmpty() && file.getSize() <= MAX_CONTRACT_SIZE
                && CONTRACT_EXTENSIONS.containsKey(file.getContentType());
        if (!valid) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Only PDF, JPG or PNG lease contracts up to 10MB are supported");
    }
    private void validateProof(MultipartFile file) {
        boolean valid = file != null && !file.isEmpty() && file.getSize() <= MAX_CONTRACT_SIZE
                && CONTRACT_EXTENSIONS.containsKey(file.getContentType());
        if (!valid) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Only PDF, JPG or PNG rent payment proofs up to 10MB are supported");
    }
    private String safeFileName(String value) {
        String name = value == null ? "lease-contract" : value.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\t]", "_");
        if (name.isBlank()) name = "lease-contract";
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
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }
    private void deleteQuietly(Path path) { try { Files.deleteIfExists(path); } catch (IOException ignored) { } }
    public record Download(Path path, String originalName, String mimeType, long size) { }
}
