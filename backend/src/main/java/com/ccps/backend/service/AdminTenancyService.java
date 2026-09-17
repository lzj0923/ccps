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
import java.util.Objects;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.math.RoundingMode;
import java.util.Set;
import java.util.LinkedHashSet;
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
import com.ccps.backend.dto.AdminLeaseCloseRequest;
import com.ccps.backend.dto.AdminLeaseInvoiceCreateRequest;
import com.ccps.backend.dto.AdminLeaseRentInvoiceResponse;
import com.ccps.backend.dto.AdminLeaseRenewalRequest;
import com.ccps.backend.dto.AdminLeasePeriodResponse;
import com.ccps.backend.dto.AdminLeaseUpdateRequest;
import com.ccps.backend.dto.AdminLeaseTransferRequest;
import com.ccps.backend.dto.AdminDepositAccountDetailResponse;
import com.ccps.backend.dto.AdminDepositAccountResponse;
import com.ccps.backend.dto.AdminRentFinanceResponse;
import com.ccps.backend.dto.AdminRentCollectionResponse;
import com.ccps.backend.dto.AdminRentCollectionRequest;
import com.ccps.backend.dto.AdminRentCollectionBatchRequest;
import com.ccps.backend.dto.AdminRecordCreateResponse;
import com.ccps.backend.dto.AdminTenancyOptionsResponse;
import com.ccps.backend.dto.AdminTenancyResponse;
import com.ccps.backend.dto.AdminTenantCreateRequest;
import com.ccps.backend.dto.AdminTenantDirectoryResponse;
import com.ccps.backend.dto.AdminTenantDetailResponse;
import com.ccps.backend.dto.AdminTenantDepositTransactionRequest;
import com.ccps.backend.dto.AdminTenantDepositTransactionResponse;
import com.ccps.backend.mapper.AdminTenancyMapper;
import com.ccps.backend.mapper.AdminTenancyMapper.ContractFile;
import com.ccps.backend.mapper.AdminTenancyMapper.DepositAccountRow;
import com.ccps.backend.mapper.AdminTenancyMapper.LeaseContractContext;
import com.ccps.backend.mapper.AdminTenancyMapper.LeaseChangeContext;
import com.ccps.backend.mapper.AdminTenancyMapper.LeaseInvoiceRow;
import com.ccps.backend.mapper.AdminTenancyMapper.LeasePeriodRow;
import com.ccps.backend.mapper.AdminTenancyMapper.LeasePeriodContractContext;
import com.ccps.backend.mapper.AdminTenancyMapper.NewLease;
import com.ccps.backend.mapper.AdminTenancyMapper.NewLeasePeriod;
import com.ccps.backend.mapper.AdminTenancyMapper.NewRentCredit;
import com.ccps.backend.mapper.AdminTenancyMapper.NewSecurityDeposit;
import com.ccps.backend.mapper.AdminTenancyMapper.NewTenantDepositTransaction;
import com.ccps.backend.mapper.AdminTenancyMapper.NewContractDocument;
import com.ccps.backend.mapper.AdminTenancyMapper.NewTenant;
import com.ccps.backend.mapper.AdminTenancyMapper.RentFinanceRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentFinanceSummaryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentProofContext;
import com.ccps.backend.mapper.AdminTenancyMapper.RentCollectionRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentCollectionSummaryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentCollectionContext;
import com.ccps.backend.mapper.AdminTenancyMapper.RentInvoiceAdvanceRow;
import com.ccps.backend.mapper.AdminTenancyMapper.NewRentCollection;
import com.ccps.backend.mapper.AdminTenancyMapper.RentCreditRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentInvoiceCreditRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentalSpaceContext;
import com.ccps.backend.mapper.AdminTenancyMapper.SummaryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.TenancyRow;
import com.ccps.backend.mapper.AdminTenancyMapper.TenantDirectoryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.TenantDirectorySummaryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.TenantDirectoryOverviewRow;
import com.ccps.backend.mapper.AdminTenancyMapper.TenantLeaseHistoryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.ExpiredLeaseRow;
import com.lowagie.text.pdf.PdfContentByte;

@Service
public class AdminTenancyService {
    private static final int LEASE_AUTO_TERMINATION_GRACE_MONTHS = 2;
    private static final Set<String> STATUSES = Set.of("paid", "partial", "unpaid", "overdue", "pending_review");
    private static final Set<String> RENT_PAYMENT_METHODS = Set.of("bank_transfer", "online_payment", "cash", "cheque", "security_deposit");
    private static final Set<String> RENT_CALCULATION_METHODS = Set.of("daily_prorated");
    private static final Set<String> DEPOSIT_CREDIT_TYPES = Set.of("tenant_repayment", "adjustment_credit");
    private static final Set<String> DEPOSIT_DEBIT_TYPES = Set.of("tenant_advance", "refund", "forfeiture", "adjustment_debit");
    private static final Set<String> DELETABLE_DEPOSIT_TYPES = Set.of(
            "tenant_repayment", "adjustment_credit", "tenant_advance", "adjustment_debit");
    private static final long MAX_CONTRACT_SIZE = 10L * 1024 * 1024;
    private static final long MAX_GENERATED_CONTRACT_SIZE = 30L * 1024 * 1024;
    private static final Map<String, String> CONTRACT_EXTENSIONS = Map.of(
            "application/pdf", ".pdf", "image/jpeg", ".jpg", "image/png", ".png");
    private final AdminTenancyMapper mapper;
    private final Clock clock;
    private final Path contractStorageRoot;
    private final Path rentProofStorageRoot;
    private final Path electronicSignatureStorageRoot;
    private PropertyExpensePostingService propertyExpensePostingService;
    private TenantWhatsAppSubscriptionService tenantWhatsAppSubscriptionService;

    @Autowired
    void setTenantWhatsAppSubscriptionService(TenantWhatsAppSubscriptionService service) {
        this.tenantWhatsAppSubscriptionService = service;
    }

    @Autowired
    public AdminTenancyService(AdminTenancyMapper mapper,
            @Value("${ccps.storage.lease-contracts:uploads/lease-contracts}") String storageRoot,
            @Value("${ccps.storage.payment-proofs:uploads/payment-proofs}") String rentProofRoot,
            @Value("${ccps.storage.electronic-signatures:uploads/electronic-signatures}") String electronicSignatureRoot) {
        this(mapper, Clock.systemDefaultZone(), Path.of(storageRoot).toAbsolutePath().normalize(),
                Path.of(rentProofRoot).toAbsolutePath().normalize(),
                Path.of(electronicSignatureRoot).toAbsolutePath().normalize());
    }
    AdminTenancyService(AdminTenancyMapper mapper, Clock clock) {
        this(mapper, clock, Path.of("uploads/lease-contracts").toAbsolutePath().normalize(),
                Path.of("uploads/payment-proofs").toAbsolutePath().normalize());
    }
    AdminTenancyService(AdminTenancyMapper mapper, Clock clock, Path contractStorageRoot) {
        this(mapper, clock, contractStorageRoot, contractStorageRoot,
                contractStorageRoot.resolveSibling("electronic-signatures"));
    }
    AdminTenancyService(AdminTenancyMapper mapper, Clock clock, Path contractStorageRoot, Path rentProofStorageRoot) {
        this(mapper, clock, contractStorageRoot, rentProofStorageRoot,
                contractStorageRoot.resolveSibling("electronic-signatures"));
    }
    AdminTenancyService(AdminTenancyMapper mapper, Clock clock, Path contractStorageRoot, Path rentProofStorageRoot,
            Path electronicSignatureStorageRoot) {
        this.mapper = mapper; this.clock = clock; this.contractStorageRoot = contractStorageRoot;
        this.rentProofStorageRoot = rentProofStorageRoot; this.electronicSignatureStorageRoot = electronicSignatureStorageRoot;
    }

    @Autowired(required = false)
    void setPropertyExpensePostingService(PropertyExpensePostingService service) {
        this.propertyExpensePostingService = service;
    }

    @Transactional
    public AdminTenancyResponse find(int requestedPage, int requestedPageSize, String keyword,
            String projectName, String status, LocalDate startDate, LocalDate endDate) {
        expireEndedLeases();
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must not be before start date");
        }
        int pageSize = Math.max(1, Math.min(requestedPageSize, 100));
        String normalizedStatus = normalize(status);
        if (normalizedStatus != null && !STATUSES.contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tenancy status");
        }
        String normalizedKeyword = normalize(keyword);
        String normalizedProject = normalize(projectName);
        long totalRows = zero(mapper.countPage(normalizedKeyword, normalizedProject, normalizedStatus, startDate, endDate));
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        List<AdminTenancyResponse.Item> rows = mapper.findPage(normalizedKeyword, normalizedProject,
                normalizedStatus, startDate, endDate, pageSize, (page - 1) * pageSize).stream().map(this::toItem).toList();
        SummaryRow source = mapper.findSummary();
        AdminTenancyResponse.Summary summary = new AdminTenancyResponse.Summary(
                source == null ? 0 : zero(source.getTenantCount()), source == null ? 0 : zero(source.getActiveLeaseCount()),
                source == null ? BigDecimal.ZERO : zero(source.getCurrentDue()), source == null ? BigDecimal.ZERO : zero(source.getCurrentPaid()),
                source == null ? BigDecimal.ZERO : zero(source.getCurrentUnpaid()), source == null ? BigDecimal.ZERO : zero(source.getTotalUnpaid()), source == null ? 0 : zero(source.getPartialCount()),
                source == null ? 0 : zero(source.getOverdueCount()), source == null ? 0 : zero(source.getPendingReviewCount()));
        return new AdminTenancyResponse(summary, rows, new AdminTenancyResponse.Page(totalRows, page, pageSize, totalPages));
    }

    @Transactional(readOnly = true)
    public AdminTenantDirectoryResponse findTenantDirectory(int requestedPage, int requestedPageSize, String keyword, String status) {
        int pageSize = Math.max(1, Math.min(requestedPageSize, 100));
        String normalizedStatus = normalize(status);
        if (normalizedStatus != null && !Set.of("active", "inactive").contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tenant status");
        }
        String normalizedKeyword = normalize(keyword);
        long totalRows = mapper.countTenantDirectory(normalizedKeyword, normalizedStatus);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        List<AdminTenantDirectoryResponse.Item> rows = mapper.findTenantDirectoryPage(normalizedKeyword, normalizedStatus, pageSize, (page - 1) * pageSize)
                .stream().map(this::toTenantDirectoryItem).toList();
        TenantDirectorySummaryRow summary = mapper.findTenantDirectorySummary();
        return new AdminTenantDirectoryResponse(new AdminTenantDirectoryResponse.Summary(
                summary == null ? 0 : zero(summary.getTotalCount()), summary == null ? 0 : zero(summary.getActiveCount()),
                summary == null ? 0 : zero(summary.getInactiveCount()), summary == null ? 0 : zero(summary.getActiveLeaseTenantCount())),
                 rows, new AdminTenantDirectoryResponse.Page(totalRows, page, pageSize, totalPages));
    }

    @Transactional(readOnly = true)
    public AdminTenantDetailResponse findTenantDetail(Long tenantId) {
        if (mapper.countTenant(tenantId) != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
        TenantDirectoryOverviewRow overview = mapper.findTenantDirectoryOverview(tenantId);
        return new AdminTenantDetailResponse(new AdminTenantDetailResponse.Overview(
                overview == null ? BigDecimal.ZERO : zero(overview.getUnpaidRent()),
                overview == null ? 0 : zero(overview.getPendingMaintenanceCount()),
                overview == null ? 0 : zero(overview.getPendingSignatureCount()),
                mapper.findTenantDepositTransactions(tenantId).stream()
                        .filter(item -> !"cancelled".equals(item.status()))
                        .map(AdminTenantDepositTransactionResponse::balanceAfter)
                        .findFirst().orElse(BigDecimal.ZERO)),
                mapper.findTenantLeaseHistory(tenantId).stream().map(this::toTenantLeaseDetail).toList(),
                mapper.findTenantDepositTransactions(tenantId));
    }

    @Transactional(readOnly = true)
    public AdminDepositAccountResponse findDepositAccounts(int requestedPage, int requestedPageSize,
            String keyword, String status) {
        int pageSize = Math.max(1, Math.min(requestedPageSize, 100));
        String normalizedKeyword = normalize(keyword);
        String normalizedStatus = normalize(status);
        List<DepositAccountRow> keywordRows = mapper.findDepositAccounts().stream()
                .filter(row -> matchesDepositKeyword(row, normalizedKeyword)).toList();
        List<DepositAccountRow> filtered = keywordRows.stream()
                .filter(row -> normalizedStatus == null || normalizedStatus.equals(depositAccountStatus(row))).toList();
        long totalRows = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        int from = Math.min((page - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        List<AdminDepositAccountResponse.Item> rows = filtered.subList(from, to).stream()
                .map(this::toDepositAccountItem).toList();
        AdminDepositAccountResponse.Summary summary = new AdminDepositAccountResponse.Summary(
                keywordRows.size(), countDepositStatus(keywordRows, "pending_collection"),
                countDepositStatus(keywordRows, "active"), countDepositStatus(keywordRows, "awaiting_settlement"),
                countDepositStatus(keywordRows, "settling"), countDepositStatus(keywordRows, "settled"),
                keywordRows.stream().map(row -> zero(row.getPostedBalance())).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new AdminDepositAccountResponse(summary, rows,
                new AdminDepositAccountResponse.Page(totalRows, page, pageSize, totalPages));
    }

    @Transactional(readOnly = true)
    public AdminDepositAccountDetailResponse findDepositAccount(Long leaseId) {
        DepositAccountRow row = mapper.findDepositAccount(leaseId);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit account not found");
        List<AdminTenantDepositTransactionResponse> transactions = mapper.findLeaseDepositTransactions(leaseId);
        List<String> allowedActions = new ArrayList<>();
        if ("pending".equals(row.getConfirmationStatus())) allowedActions.add("confirm_collection");
        if ("active".equals(row.getLeaseStatus())
                && (zero(row.getExpectedDeposit()).signum() == 0 || "confirmed".equals(row.getConfirmationStatus()))) {
            allowedActions.add("increase_deposit");
            if (zero(row.getAvailableBalance()).signum() > 0) allowedActions.add("tenant_advance");
        }
        BigDecimal outstandingAdvance = transactions.stream().filter(item -> "posted".equals(item.status()))
                .filter(item -> "tenant_advance".equals(item.transactionType()) || "tenant_repayment".equals(item.transactionType()))
                .map(item -> "tenant_advance".equals(item.transactionType()) ? item.amount() : item.amount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add).max(BigDecimal.ZERO);
        if (outstandingAdvance.signum() > 0) allowedActions.add("tenant_repayment");
        if (!"active".equals(row.getLeaseStatus()) && zero(row.getAvailableBalance()).signum() > 0) {
            allowedActions.add("refund"); allowedActions.add("forfeiture");
        }
        AdminDepositAccountDetailResponse.Account account = new AdminDepositAccountDetailResponse.Account(
                row.getLeaseId(), row.getLeaseNo(), row.getTenantId(), row.getTenantName(), row.getTenantPhone(),
                row.getProjectName(), row.getUnitNo(), row.getLeaseStatus(), row.getStartDate(), row.getEndDate(),
                zero(row.getExpectedDeposit()), zero(row.getPostedBalance()), zero(row.getAvailableBalance()),
                row.getDepositEntryStatus(), depositAccountStatus(row));
        AdminDepositAccountDetailResponse.Bill bill = new AdminDepositAccountDetailResponse.Bill(
                row.getFinanceRecordId(), row.getTransactionNo(), zero(row.getBillAmount()), row.getBillDate(),
                row.getDepositEntryStatus(), row.getConfirmationStatus(), row.getPaymentStatus());
        AdminDepositAccountDetailResponse.Reserve reserve = new AdminDepositAccountDetailResponse.Reserve(
                row.getOwnerId(), row.getOwnerName(), row.getReserveAccountId(), zero(row.getReserveBalance()));
        return new AdminDepositAccountDetailResponse(account, bill, reserve, transactions, allowedActions,
                mapper.findDepositBills(leaseId));
    }

    @Transactional
    public AdminRecordCreateResponse createTenantDepositTransaction(Long actorId, Long leaseId,
            AdminTenantDepositTransactionRequest request) {
        LeaseChangeContext lease = mapper.lockLeaseForChange(leaseId);
        if (lease == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease not found");
        DepositAccountRow account = mapper.findDepositAccount(leaseId);
        if (account == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Deposit account not found");
        String type = request.transactionType();
        boolean credit = DEPOSIT_CREDIT_TYPES.contains(type);
        if (!credit && !DEPOSIT_DEBIT_TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tenant deposit transaction type");
        }
        if (zero(account.getExpectedDeposit()).signum() > 0 && !"confirmed".equals(account.getConfirmationStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Confirm the security deposit bill before recording account transactions");
        }
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentBalance = zero(mapper.findLeaseDepositBalance(leaseId));
        if (!credit && currentBalance.compareTo(amount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant deposit balance is insufficient");
        }
        if (("refund".equals(type) || "forfeiture".equals(type)) && "active".equals(lease.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Deposit can only be settled after the lease has ended");
        }
        if (("tenant_advance".equals(type) || "adjustment_credit".equals(type)) && !"active".equals(lease.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This deposit action is only available for an active lease");
        }
        if ("refund".equals(type)) {
            if (account.getReserveAccountId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Active owner reserve account is required for a deposit refund");
            }
        }

        String timestamp = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String prefix = switch (type) {
            case "refund" -> "DEP-REF";
            case "forfeiture" -> "DEP-FOR";
            case "tenant_advance" -> "DEP-ADV";
            case "tenant_repayment" -> "DEP-REP";
            default -> "DEP-ADJ";
        };
        NewTenantDepositTransaction transaction = new NewTenantDepositTransaction();
        transaction.setLeaseId(leaseId); transaction.setTenantId(lease.getTenantId()); transaction.setUnitId(lease.getUnitId());
        transaction.setTransactionNo(prefix + "-" + timestamp + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        transaction.setTransactionType(type); transaction.setDirection(credit ? "credit" : "debit");
        transaction.setAmount(amount); transaction.setOccurredOn(request.occurredOn()); transaction.setCreatedBy(actorId);
        transaction.setDescription(normalize(request.description()) == null ? depositDescription(type, lease.getLeaseNo()) : request.description().trim());
        transaction.setStatus("posted");

        if ("refund".equals(type)) {
            transaction.setStatus("pending");
            if (mapper.insertTenantDepositRefundFinance(transaction) != 1 || transaction.getFinanceRecordId() == null
                    || mapper.insertTenantDepositRefundCashflow(transaction) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant deposit refund could not be created");
            }
        } else if ("forfeiture".equals(type)) {
            transaction.setStatus("pending");
            if (mapper.insertTenantDepositForfeitureFinance(transaction) != 1 || transaction.getFinanceRecordId() == null
                    || mapper.insertTenantDepositForfeitureCashflow(transaction) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant deposit forfeiture could not be created");
            }
        }
        if (mapper.insertTenantDepositTransaction(transaction) != 1 || transaction.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant deposit transaction could not be created");
        }
        mapper.insertTenantDepositAudit(actorId, transaction.getId(), leaseId, type, transaction.getDirection(), amount,
                transaction.getFinanceRecordId(), transaction.getDescription());
        return new AdminRecordCreateResponse(transaction.getId(), transaction.getTransactionNo());
    }

    @Transactional
    public void deleteTenantDepositTransactions(Long actorId, Long leaseId, List<Long> transactionIds) {
        LinkedHashSet<Long> selectedIds = transactionIds == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(transactionIds);
        if (selectedIds.isEmpty() || selectedIds.contains(null) || selectedIds.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select deposit transactions to delete");
        }
        if (mapper.lockLeaseForChange(leaseId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease not found");
        }

        List<AdminTenantDepositTransactionResponse> transactions = mapper.findLeaseDepositTransactions(leaseId);
        List<AdminTenantDepositTransactionResponse> selected = transactions.stream()
                .filter(item -> selectedIds.contains(item.id()))
                .toList();
        if (selected.size() != selectedIds.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Deposit transaction selection changed; reload and try again");
        }
        boolean containsProtectedRecord = selected.stream().anyMatch(item ->
                !"posted".equals(item.status()) || item.financeRecordId() != null
                        || !DELETABLE_DEPOSIT_TYPES.contains(item.transactionType()));
        if (containsProtectedRecord) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only manual deposit account transactions can be deleted");
        }

        List<AdminTenantDepositTransactionResponse> remaining = transactions.stream()
                .filter(item -> !selectedIds.contains(item.id()))
                .sorted(Comparator.comparing(AdminTenantDepositTransactionResponse::occurredOn)
                        .thenComparing(AdminTenantDepositTransactionResponse::id))
                .toList();
        BigDecimal runningBalance = BigDecimal.ZERO;
        BigDecimal runningAdvance = BigDecimal.ZERO;
        for (AdminTenantDepositTransactionResponse item : remaining) {
            runningBalance = runningBalance.add(
                    "credit".equals(item.direction()) ? item.amount() : item.amount().negate());
            if (runningBalance.signum() < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Deleting these transactions would make the deposit balance negative");
            }
            if ("posted".equals(item.status())) {
                if ("tenant_advance".equals(item.transactionType())) runningAdvance = runningAdvance.add(item.amount());
                if ("tenant_repayment".equals(item.transactionType())) runningAdvance = runningAdvance.subtract(item.amount());
                if (runningAdvance.signum() < 0) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Delete the related tenant repayment before deleting its advance");
                }
            }
        }

        List<Long> ids = new ArrayList<>(selectedIds);
        if (mapper.cancelTenantDepositTransactions(leaseId, ids) != ids.size()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Deposit transaction selection changed; reload and try again");
        }
        selected.forEach(item -> mapper.insertTenantDepositDeleteAudit(actorId, item.id(), leaseId,
                item.transactionType(), item.direction(), item.amount()));
    }

    @Transactional
    public AdminTenancyOptionsResponse options() {
        expireEndedLeases();
        return new AdminTenancyOptionsResponse(mapper.findProjects(), mapper.findTenantOptions(),
                mapper.findAvailableUnits(), mapper.findRentalSpaceOptions());
    }

    @Transactional(readOnly = true)
    public List<AdminLeaseRentInvoiceResponse> findLeaseInvoiceDetails(Long leaseId) {
        return mapper.findLeaseInvoiceDetails(leaseId).stream()
                .map(this::toLeaseInvoiceDetail).toList();
    }

    @Transactional(readOnly = true)
    public AdminRentFinanceResponse findRentFinance(int requestedPage, int requestedPageSize, String keyword,
            String projectName, String status, LocalDate startDate, LocalDate endDate,
            LocalDate billingMonth, String tenantName, String unitNo) {
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
        String normalizedTenant = normalize(tenantName); String normalizedUnit = normalize(unitNo);
        long totalRows = zero(mapper.countRentFinancePage(normalizedKeyword, normalizedProject, confirmationStatus,
                syncStatus, proofStatus, billingMonth, normalizedTenant, normalizedUnit, startDate, endDate));
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        List<AdminRentFinanceResponse.Item> rows = mapper.findRentFinancePage(normalizedKeyword, normalizedProject,
                confirmationStatus, syncStatus, proofStatus, billingMonth, normalizedTenant, normalizedUnit,
                startDate, endDate, pageSize, (page - 1) * pageSize)
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
            String projectName, String status, LocalDate startDate, LocalDate endDate, Long invoiceId) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must not be before start date");
        }
        String normalizedStatus = normalize(status);
        if (normalizedStatus != null && !Set.of("unpaid", "partial", "overdue").contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rent collection status");
        }
        int pageSize = Math.max(1, Math.min(requestedPageSize, 100));
        String normalizedKeyword = normalize(keyword); String normalizedProject = normalize(projectName);
        long totalRows = zero(mapper.countRentCollections(normalizedKeyword, normalizedProject, normalizedStatus, startDate, endDate, invoiceId));
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        List<AdminRentCollectionResponse.Item> rows = mapper.findRentCollections(normalizedKeyword, normalizedProject,
                normalizedStatus, startDate, endDate, invoiceId, pageSize, (page - 1) * pageSize).stream()
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
        if (request.receivedDate().isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rent payment date cannot be in the future");
        }
        RentCollectionContext context = mapper.lockRentCollection(invoiceId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rent invoice not found");
        BigDecimal beforePaid = zero(context.getAmountPaid());
        BigDecimal outstanding = zero(context.getAmountDue()).subtract(beforePaid).max(BigDecimal.ZERO);
        if (outstanding.signum() <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Rent invoice is already paid");
        boolean fromDeposit = "security_deposit".equals(request.paymentMethod());
        if (fromDeposit) {
            mapper.lockLeaseForChange(context.getLeaseId());
            if (request.amount().compareTo(outstanding) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Security deposit can only offset the current rent balance");
            }
            if (zero(mapper.findLeaseDepositBalance(context.getLeaseId())).compareTo(request.amount()) < 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant deposit balance is insufficient");
            }
        }
        List<RentInvoiceAdvanceRow> lockedInvoices = mapper.lockRentInvoicesForAdvance(context.getLeaseId(),
                context.getBillingMonth(), context.getEndDate().withDayOfMonth(1));
        BigDecimal leaseOutstanding = outstandingThroughLease(context, lockedInvoices);
        if (request.amount().compareTo(leaseOutstanding) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Received amount exceeds the remaining rent for this lease");
        }
        String timestamp = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String payerName = normalize(request.payerName()) == null ? context.getTenantName() : request.payerName().trim();
        String reference = normalize(request.paymentReference());
        String note = normalize(request.note()) == null ? "管理員確認租金收款" : request.note().trim();
        String allocationNote = normalize(request.allocationNote());
        if (allocationNote == null) allocationNote = normalize(mapper.findRentAllocationNoteDefault(context.getUnitId()));
        BigDecimal appliedToCurrentInvoice = request.amount().min(outstanding);
        BigDecimal prepaymentAmount = request.amount().subtract(appliedToCurrentInvoice);
        if (prepaymentAmount.signum() > 0) {
            lockedInvoices = ensureAdvanceInvoices(context, prepaymentAmount, lockedInvoices);
        }
        List<RentPaymentAllocation> allocations = rentPaymentAllocations(lockedInvoices, request.amount());
        RentPaymentAllocation partialFuture = allocations.size() > 1
                && !allocations.get(allocations.size() - 1).fullyCovered()
                        ? allocations.get(allocations.size() - 1) : null;
        BigDecimal partialPrepayment = partialFuture == null ? BigDecimal.ZERO : partialFuture.amount();
        List<RentPaymentAllocation> recordAllocations = partialFuture == null
                ? allocations : allocations.subList(0, allocations.size() - 1);
        List<AdminRecordCreateResponse> createdRecords = new ArrayList<>(recordAllocations.size());
        for (int allocationIndex = 0; allocationIndex < recordAllocations.size(); allocationIndex++) {
            RentPaymentAllocation allocation = recordAllocations.get(allocationIndex);
            String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            String transactionNo = "RENT-ADM-" + timestamp + "-" + token;
            String receiptNo = "RENT-RCP-" + timestamp + "-" + token;
            String billingMonth = allocation.billingMonth().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            BigDecimal financeAmount = allocation.amount().add(allocationIndex == 0 ? partialPrepayment : BigDecimal.ZERO);
            LocalDate postingDate = allocationIndex == 0 ? request.postingDate() : allocation.billingMonth();
            String receiptNote = recordAllocations.size() > 1 ? note + "；租金月份 " + billingMonth : note;
            if (allocationIndex == 0 && partialPrepayment.signum() > 0) {
                receiptNote += "；未足月预收余额 RM " + money(partialPrepayment);
            }
            NewRentCollection record = new NewRentCollection();
            record.setTransactionNo(transactionNo); record.setUnitId(context.getUnitId()); record.setOwnerId(context.getOwnerId());
            record.setTenantId(context.getTenantId()); record.setAmount(financeAmount); record.setReceivedDate(request.receivedDate());
            record.setPostingDate(postingDate);
            record.setPaymentMethod(request.paymentMethod()); record.setAllocationNote(allocationNote); record.setActorId(actorId);
            if (mapper.insertConfirmedRentPayment(record) != 1 || record.getId() == null
                    || mapper.linkRentPayment(allocation.invoiceId(), record.getId(), allocation.amount()) != 1
                    || mapper.insertRentCollectionReceipt(record.getId(), receiptNo, payerName, reference, receiptNote) != 1
                    || mapper.insertRentCashflow(record.getId(),context.getUnitId(),context.getOwnerId(),context.getTenantId(),
                            "租金收款 · " + billingMonth + " · " + context.getLeaseNo(),postingDate) != 1
                    || mapper.applyRentCollection(allocation.invoiceId(), allocation.amount()) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Rent collection state changed; reload and try again");
            }
            if (allocationNote != null) mapper.updateRentCashflowAllocationNote(record.getId(), allocationNote);
            mapper.insertRentCollectionAudit(actorId, record.getId(), allocation.invoiceId(), allocation.beforePaid(),
                    allocation.amount(), allocation.beforePaid().add(allocation.amount()),
                    recordAllocations.size() > 1 ? receiptNote + "；跨月租金已分项入账" : receiptNote);
            createdRecords.add(new AdminRecordCreateResponse(record.getId(), transactionNo, receiptNo));
        }
        if (Boolean.TRUE.equals(request.reuseAllocationNote()) && allocationNote != null) {
            mapper.upsertRentAllocationNoteDefault(context.getUnitId(), allocationNote, actorId);
        }
        if (fromDeposit) {
            AdminRecordCreateResponse primaryRecord = createdRecords.get(0);
            NewTenantDepositTransaction deposit = new NewTenantDepositTransaction();
            deposit.setLeaseId(context.getLeaseId()); deposit.setTenantId(context.getTenantId()); deposit.setUnitId(context.getUnitId());
            deposit.setFinanceRecordId(primaryRecord.id()); deposit.setTransactionNo(primaryRecord.referenceNo());
            deposit.setTransactionType("rent_deduction"); deposit.setDirection("debit"); deposit.setAmount(request.amount());
            deposit.setOccurredOn(request.receivedDate()); deposit.setDescription("扣租客押金支付租金 · " + context.getLeaseNo());
            deposit.setStatus("posted"); deposit.setCreatedBy(actorId);
            if (mapper.insertTenantDepositTransaction(deposit) != 1 || deposit.getId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to deduct the tenant deposit balance");
            }
            mapper.insertTenantDepositAudit(actorId, deposit.getId(), context.getLeaseId(), "rent_deduction", "debit",
                    request.amount(), primaryRecord.id(), deposit.getDescription());
        }
        if (partialPrepayment.signum() > 0) {
            NewRentCredit credit = new NewRentCredit();
            credit.setLeaseId(context.getLeaseId()); credit.setFinanceRecordId(createdRecords.get(0).id());
            credit.setReceivedAmount(partialPrepayment); credit.setRemainingAmount(partialPrepayment); credit.setActorId(actorId);
            if (mapper.insertRentCredit(credit) != 1 || credit.getId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create rent prepayment balance");
            }
            applyAvailableRentCredits(actorId, context.getLeaseId());
        }
        String coveredMonths = recordAllocations.stream().map(item -> item.billingMonth().format(DateTimeFormatter.ofPattern("yyyy-MM")))
                .distinct().reduce((left, right) -> left + "、" + right).orElse("");
        String partialNotice = partialPrepayment.signum() > 0
                ? "；另有未足月预收余额 RM " + money(partialPrepayment) : "";
        mapper.insertTenantNotification(context.getTenantId(), "租金收款已確認",
                "%s %s 已確認收到 %s 月租金，共 RM %s%s。".formatted(context.getProjectName(), context.getUnitNo(),
                        coveredMonths, money(request.amount()), partialNotice),
                "rent_invoice", invoiceId, "normal");
        if (proof != null && !proof.isEmpty()) {
            Long documentId = uploadRentProof(actorId, createdRecords.get(0).id(), proof);
            for (int index = 1; index < createdRecords.size(); index++) {
                Long financeRecordId = createdRecords.get(index).id();
                if (mapper.insertRentProofLink(documentId, financeRecordId) != 1
                        || mapper.updateRentReceiptProof(financeRecordId, documentId) != 1) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Unable to link rent payment proof to monthly record");
                }
                mapper.insertRentProofAudit(actorId, financeRecordId, null, documentId,
                        proof.getOriginalFilename(), "link_rent_proof_to_monthly_record");
            }
        }
        return createdRecords.get(0);
    }

    @Transactional
    public List<AdminRecordCreateResponse> confirmRentCollections(Long actorId,
            AdminRentCollectionBatchRequest request) {
        LinkedHashSet<Long> invoiceIds = new LinkedHashSet<>(request.invoiceIds());
        if (invoiceIds.contains(null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rent invoice id is required");
        }
        List<AdminRecordCreateResponse> results = new ArrayList<>(invoiceIds.size());
        for (Long invoiceId : invoiceIds) {
            RentCollectionContext context = mapper.lockRentCollection(invoiceId);
            if (context == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rent invoice not found");
            }
            BigDecimal outstanding = zero(context.getAmountDue()).subtract(zero(context.getAmountPaid()))
                    .max(BigDecimal.ZERO);
            if (outstanding.signum() <= 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Rent invoice is already paid");
            }
            AdminRentCollectionRequest item = new AdminRentCollectionRequest(
                    outstanding, request.receivedDate(), request.postingDate(), request.paymentMethod(), context.getTenantName(),
                    request.paymentReference(), request.note(), request.allocationNote(), request.reuseAllocationNote(), false);
            results.add(confirmRentCollection(actorId, invoiceId, item, null));
        }
        return results;
    }

    @Transactional
    public Long createTenant(AdminTenantCreateRequest request) {
        String identity = normalize(request.identityNo());
        String email = normalize(request.email());
        if (identity != null && mapper.countTenantIdentity(identity) > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant identity number already exists");
        NewTenant tenant = new NewTenant(); tenant.setFullName(request.fullName().trim()); tenant.setIdentityNo(identity);
        tenant.setPhone(normalize(request.phone())); tenant.setEmail(email); tenant.setStatus(request.status());
        if (mapper.insertTenant(tenant) != 1 || tenant.getId() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create tenant");
        tenantWhatsAppSubscriptionService.synchronizeTenant(tenant.getId(), tenant.getPhone(), tenant.getStatus(), request.whatsappEnabled());
        return tenant.getId();
    }

    @Transactional
    public void updateTenant(Long tenantId,AdminTenantCreateRequest request) {
        if(mapper.countTenant(tenantId)!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Tenant not found");
        String identity=normalize(request.identityNo()); String email=normalize(request.email());
        if(identity!=null&&mapper.countTenantIdentityExcluding(identity,tenantId)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Tenant identity number already exists");
        NewTenant tenant=new NewTenant();tenant.setId(tenantId);tenant.setFullName(request.fullName().trim());tenant.setIdentityNo(identity);
        tenant.setPhone(normalize(request.phone()));tenant.setEmail(email);tenant.setStatus(request.status());
        if(mapper.updateTenant(tenant)!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Unable to update tenant");
        tenantWhatsAppSubscriptionService.synchronizeTenant(tenantId, tenant.getPhone(), tenant.getStatus(), request.whatsappEnabled());
    }

    @Transactional
    public void updateTenantStatus(Long tenantId, String status) {
        if (mapper.countTenant(tenantId) != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
        }
        if (mapper.updateTenantStatus(tenantId, status) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to update tenant status");
        }
        tenantWhatsAppSubscriptionService.synchronizeTenant(tenantId);
    }

    @Transactional
    public void deleteTenant(Long tenantId) {
        if (mapper.countTenant(tenantId) != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
        if (mapper.countTenantLeases(tenantId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant has lease history and cannot be deleted; deactivate the tenant instead");
        }
        tenantWhatsAppSubscriptionService.deleteForTenant(tenantId);
        if (mapper.deleteTenant(tenantId) != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to delete tenant");
    }

    @Transactional
    public Long createLease(AdminLeaseCreateRequest request) {
        if (request.startDate() == null || request.endDate() == null || request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lease end date must not be before start date");
        }
        if (mapper.countActiveTenant(request.tenantId()) != 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active tenant not found");
        if (request.rentalMandateId() == null) {
            requireAgencyContract(request.unitId(), request.startDate(), request.endDate());
        } else if (mapper.countCurrentRentalMandate(request.rentalMandateId(), request.unitId(), request.startDate(), request.endDate()) != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current rental mandate does not cover this lease");
        }
        if (mapper.countOperatingUnit(request.unitId()) != 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operating unit not found");
        Long rentalSpaceId = requireAvailableRentalSpace(request.unitId(), request.rentalSpaceId(),
                request.startDate(), request.endDate(), null);
        String rentCalculationMethod = rentCalculationMethod(request.rentCalculationMethod());
        BigDecimal depositAmount = request.monthlyRent().multiply(new BigDecimal("2.5"))
                .setScale(2, RoundingMode.HALF_UP);
        NewLease lease = new NewLease(); lease.setTenantId(request.tenantId()); lease.setUnitId(request.unitId());
        lease.setRentalSpaceId(rentalSpaceId);
        lease.setRentalMandateId(request.rentalMandateId());
        lease.setLeaseNo("LEASE-" + LocalDate.now(clock).toString().replace("-", "") + "-" + Long.toHexString(System.nanoTime()).toUpperCase());
        lease.setStartDate(request.startDate()); lease.setEndDate(request.endDate()); lease.setMonthlyRent(request.monthlyRent());
        lease.setDepositAmount(depositAmount); lease.setPaymentDay(request.paymentDay()); lease.setRentCalculationMethod(rentCalculationMethod);
        if (mapper.insertLease(lease) != 1 || lease.getId() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create lease");
        NewLeasePeriod initialPeriod = leasePeriod(lease.getId(), 1, request.startDate(), request.endDate(),
                request.monthlyRent(), depositAmount, request.paymentDay(), rentCalculationMethod, null);
        if (mapper.insertLeasePeriod(initialPeriod) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create initial lease period");
        }
        createSecurityDeposit(lease);
        mapper.activateRentalService(request.unitId()); mapper.markUnitRented(request.unitId());
        LocalDate currentMonth = LocalDate.now(clock).withDayOfMonth(1);
        LocalDate lastBillingMonth = request.endDate().withDayOfMonth(1).isBefore(currentMonth)
                ? request.endDate().withDayOfMonth(1) : currentMonth;
        for (LocalDate month = request.startDate().withDayOfMonth(1);
                !month.isAfter(lastBillingMonth); month = month.plusMonths(1)) {
            LocalDate due = month.withDayOfMonth(Math.min(request.paymentDay(), month.lengthOfMonth()));
            if (mapper.insertInvoice(lease.getId(), month, due,
                    rentAmount(month, request.startDate(), request.endDate(), request.monthlyRent(), rentCalculationMethod)) == 1) {
                syncManagementFee(request.rentalMandateId(), null, month);
            }
        }
        return lease.getId();
    }

    @Transactional
    public void createLeaseInvoice(Long leaseId, AdminLeaseInvoiceCreateRequest request) {
        LeaseChangeContext lease = mapper.lockLeaseForChange(leaseId);
        if (lease == null || !"active".equals(lease.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active lease not found");
        }
        LocalDate billingMonth = request.billingMonth().withDayOfMonth(1);
        if (billingMonth.isBefore(lease.getStartDate().withDayOfMonth(1))
                || billingMonth.isAfter(lease.getEndDate().withDayOfMonth(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Billing month is outside the lease period");
        }
        LeasePeriodRow period = mapper.findLeasePeriodForBillingMonth(leaseId, billingMonth);
        LocalDate termStart = period == null ? lease.getStartDate() : period.getStartDate();
        LocalDate termEnd = period == null ? lease.getEndDate() : period.getEndDate();
        BigDecimal monthlyRent = period == null ? lease.getMonthlyRent() : period.getMonthlyRent();
        int paymentDay = period == null ? lease.getPaymentDay() : period.getPaymentDay();
        String method = period == null ? lease.getRentCalculationMethod() : period.getRentCalculationMethod();
        LocalDate dueDate = billingMonth.withDayOfMonth(Math.min(paymentDay, billingMonth.lengthOfMonth()));
        BigDecimal amountDue = rentAmount(billingMonth, termStart, termEnd, monthlyRent,
                rentCalculationMethod(method));
        if (mapper.repairZeroAmountInvoice(leaseId, billingMonth, dueDate, amountDue) == 1) {
            applyAvailableRentCredits(null, leaseId);
            syncManagementFee(lease.getRentalMandateId(), null, billingMonth);
            return;
        }
        if (mapper.insertInvoice(leaseId, billingMonth, dueDate, amountDue) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice already exists or could not be created");
        }
        applyAvailableRentCredits(null, leaseId);
        syncManagementFee(lease.getRentalMandateId(), null, billingMonth);
    }

    @Transactional(readOnly = true)
    public List<AdminLeasePeriodResponse> leasePeriods(Long leaseId) {
        if (mapper.countLeaseById(leaseId) != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease not found");
        }
        return mapper.findLeasePeriods(leaseId).stream().map(this::leasePeriodResponse).toList();
    }

    @Transactional
    public AdminLeasePeriodResponse renewLease(Long actorId, Long leaseId, AdminLeaseRenewalRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Renewal end date must not be before start date");
        }
        LeaseChangeContext lease = mapper.lockLeaseForChange(leaseId);
        if (lease == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease not found");
        if (!"active".equals(lease.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active leases can be renewed");
        }
        LeasePeriodRow latest = mapper.lockLatestLeasePeriod(leaseId);
        int nextPeriodNo;
        LocalDate latestEnd;
        if (latest == null) {
            NewLeasePeriod initial = leasePeriod(leaseId, 1, lease.getStartDate(), lease.getEndDate(),
                    lease.getMonthlyRent(), lease.getDepositAmount(), lease.getPaymentDay(),
                    rentCalculationMethod(lease.getRentCalculationMethod()), actorId);
            if (mapper.insertLeasePeriod(initial) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to preserve the original lease period");
            }
            nextPeriodNo = 2;
            latestEnd = lease.getEndDate();
        } else {
            nextPeriodNo = latest.getPeriodNo() + 1;
            latestEnd = latest.getEndDate();
        }
        LocalDate expectedStart = latestEnd.plusDays(1);
        if (!expectedStart.equals(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Renewal must start on the day after the current lease period ends");
        }
        requireAgencyContract(lease.getUnitId(), request.startDate(), request.endDate());
        String method = rentCalculationMethod(request.rentCalculationMethod());
        NewLeasePeriod period = leasePeriod(leaseId, nextPeriodNo, request.startDate(), request.endDate(),
                request.monthlyRent(), request.depositAmount(), request.paymentDay(), method, actorId);
        if (mapper.insertLeasePeriod(period) != 1 || period.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create renewal period");
        }
        if (mapper.extendLeaseForRenewal(leaseId, lease.getEndDate(), request.endDate(), request.monthlyRent(),
                request.depositAmount(), request.paymentDay(), method) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lease state changed; reload and try again");
        }
        createRenewalSecurityDeposit(lease, nextPeriodNo, request.depositAmount(), request.startDate());
        mapper.insertLeaseRenewalAudit(actorId, leaseId, period.getId(), nextPeriodNo, lease.getEndDate(),
                request.startDate(), request.endDate(), request.monthlyRent(), request.depositAmount(),
                request.paymentDay());
        LocalDate currentMonth = LocalDate.now(clock).withDayOfMonth(1);
        LocalDate lastMonth = request.endDate().withDayOfMonth(1).isBefore(currentMonth)
                ? request.endDate().withDayOfMonth(1) : currentMonth;
        for (LocalDate month = request.startDate().withDayOfMonth(1);
                !month.isAfter(lastMonth); month = month.plusMonths(1)) {
            LocalDate due = month.withDayOfMonth(Math.min(request.paymentDay(), month.lengthOfMonth()));
            if (mapper.insertInvoice(leaseId, month, due,
                    rentAmount(month, request.startDate(), request.endDate(), request.monthlyRent(), method)) == 1) {
                syncManagementFee(lease.getRentalMandateId(), actorId, month);
            }
        }
        LeasePeriodRow created = new LeasePeriodRow();
        created.setId(period.getId()); created.setLeaseId(leaseId); created.setPeriodNo(nextPeriodNo);
        created.setStartDate(request.startDate()); created.setEndDate(request.endDate());
        created.setMonthlyRent(request.monthlyRent()); created.setDepositAmount(request.depositAmount());
        created.setPaymentDay(request.paymentDay()); created.setRentCalculationMethod(method);
        return leasePeriodResponse(created);
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
        Long tenantId = request.tenantId() != null ? request.tenantId() : current.getTenantId();
        Long unitId = request.unitId() != null ? request.unitId() : current.getUnitId();
        Long requestedSpaceId = request.rentalSpaceId() != null ? request.rentalSpaceId()
                : Objects.equals(unitId, current.getUnitId()) ? current.getRentalSpaceId() : null;
        Long rentalSpaceId = requireAvailableRentalSpace(unitId, requestedSpaceId,
                request.startDate(), request.endDate(), leaseId);
        boolean partiesChanged = !Objects.equals(current.getTenantId(), tenantId)
                || !Objects.equals(current.getUnitId(), unitId)
                || !Objects.equals(current.getRentalSpaceId(), rentalSpaceId);
        if (request.tenantId() != null && mapper.countActiveTenant(tenantId) != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active tenant not found");
        }
        if (request.unitId() != null && mapper.countOperatingUnit(unitId) != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operating unit not found");
        }
        requireAgencyContract(unitId, request.startDate(), request.endDate());
        String rentCalculationMethod = rentCalculationMethod(request.rentCalculationMethod());
        int updated;
        if (partiesChanged && rentalSpaceId == null) {
            updated = mapper.updateLeasePartiesAndTerms(leaseId, tenantId, unitId, request.startDate(), request.endDate(),
                    request.monthlyRent(), request.depositAmount(), request.paymentDay(), rentCalculationMethod);
        } else if (partiesChanged) {
            updated = mapper.updateLeasePartiesAndTerms(leaseId, tenantId, unitId, rentalSpaceId,
                    request.startDate(), request.endDate(), request.monthlyRent(), request.depositAmount(),
                    request.paymentDay(), rentCalculationMethod);
        } else {
            updated = mapper.updateLeaseTerms(leaseId, request.startDate(), request.endDate(), request.monthlyRent(),
                    request.depositAmount(), request.paymentDay(), rentCalculationMethod);
        }
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lease state changed; reload and try again");
        }
        if (current.getDepositAmount() == null || current.getDepositAmount().compareTo(request.depositAmount()) != 0
                || partiesChanged || !Objects.equals(current.getStartDate(), request.startDate())) {
            syncPendingSecurityDeposit(leaseId, tenantId, unitId, request.depositAmount(), request.startDate(), current.getLeaseNo());
        }
        if (!Objects.equals(current.getUnitId(), unitId)) {
            mapper.markUnitAvailableIfNoActiveLease(current.getUnitId());
            mapper.activateRentalService(unitId);
            mapper.markUnitRented(unitId);
        }
        LocalDate startMonth = request.startDate().withDayOfMonth(1);
        LocalDate endMonth = request.endDate().withDayOfMonth(1);
        // Remove only unpaid invoices that belong to the erroneous old lease
        // period. Any paid or partly paid invoice remains as an audit record.
        mapper.deleteUnpaidInvoicesOutsideLeasePeriod(leaseId, startMonth, endMonth);
        // Recalculate every unpaid invoice in the edited lease period. Paid or
        // partially paid invoices are intentionally left untouched for audit safety.
        mapper.updateFutureUnpaidInvoiceTerms(leaseId, startMonth,
                request.monthlyRent(), request.paymentDay(), request.startDate(), request.endDate(), rentCalculationMethod);
        // Editing a lease can extend its start date backwards (or repair a
        // previously skipped month). Generate missing invoices through the
        // current month using the same safe monthly insert as lease creation.
        LocalDate currentMonth = LocalDate.now(clock).withDayOfMonth(1);
        LocalDate lastBillingMonth = request.endDate().withDayOfMonth(1).isBefore(currentMonth)
                ? request.endDate().withDayOfMonth(1) : currentMonth;
        for (LocalDate month = request.startDate().withDayOfMonth(1);
                !month.isAfter(lastBillingMonth); month = month.plusMonths(1)) {
            LocalDate due = month.withDayOfMonth(Math.min(request.paymentDay(), month.lengthOfMonth()));
            if (mapper.insertInvoice(leaseId, month, due,
                    rentAmount(month, request.startDate(), request.endDate(), request.monthlyRent(), rentCalculationMethod)) == 1) {
                syncManagementFee(current.getRentalMandateId(), actorId, month);
            }
        }
        if (partiesChanged) {
            mapper.insertLeasePartyUpdateAudit(actorId, leaseId, current.getTenantId(), current.getUnitId(),
                    current.getStartDate(), current.getEndDate(), current.getMonthlyRent(), current.getDepositAmount(),
                    current.getPaymentDay(), tenantId, unitId, request.startDate(), request.endDate(), request.monthlyRent(),
                    request.depositAmount(), request.paymentDay());
        } else {
            mapper.insertLeaseUpdateAudit(actorId, leaseId, current.getStartDate(), current.getEndDate(),
                    current.getMonthlyRent(), current.getDepositAmount(), current.getPaymentDay(), request.startDate(),
                    request.endDate(), request.monthlyRent(), request.depositAmount(), request.paymentDay());
        }
    }

    @Transactional
    public void closeLease(Long actorId, Long leaseId, AdminLeaseCloseRequest request) {
        LeaseChangeContext current = mapper.lockLeaseForChange(leaseId);
        if (current == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease not found");
        if (!"active".equals(current.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only active leases can be closed");
        }
        LocalDate endDate = request.endDate();
        if (endDate.isBefore(current.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lease end date must not be before the lease start date");
        }
        if (endDate.isAfter(LocalDate.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lease end date cannot be in the future");
        }
        if (mapper.closeLease(leaseId, endDate, "terminated") != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lease state changed; reload and try again");
        }
        mapper.deleteOldFutureInvoices(leaseId, endDate.withDayOfMonth(1).plusMonths(1));
        mapper.markUnitAvailableIfNoActiveLease(current.getUnitId());
        mapper.insertLeaseClosureAudit(actorId, leaseId, current.getEndDate(), endDate,
                request.reason(), request.notes());
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
        requireAgencyContract(current.getUnitId(), request.transferDate(), request.endDate());
        requireAvailableRentalSpace(current.getUnitId(), current.getRentalSpaceId(),
                request.transferDate(), request.endDate(), leaseId);
        if (mapper.closeLeaseForTransfer(leaseId, request.transferDate().minusDays(1)) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lease state changed; reload and try again");
        }
        mapper.deleteOldFutureInvoices(leaseId, request.transferDate().withDayOfMonth(1));

        NewLease transferred = new NewLease();
        transferred.setTenantId(request.newTenantId()); transferred.setUnitId(current.getUnitId());
        transferred.setRentalSpaceId(current.getRentalSpaceId());
        transferred.setRentalMandateId(current.getRentalMandateId());
        transferred.setLeaseNo("LEASE-TR-" + today.toString().replace("-", "") + "-" + Long.toHexString(System.nanoTime()).toUpperCase());
        transferred.setStartDate(request.transferDate()); transferred.setEndDate(request.endDate());
        transferred.setMonthlyRent(request.monthlyRent()); transferred.setDepositAmount(request.depositAmount());
        String rentCalculationMethod = rentCalculationMethod(request.rentCalculationMethod());
        transferred.setPaymentDay(request.paymentDay()); transferred.setRentCalculationMethod(rentCalculationMethod);
        if (mapper.insertLease(transferred) != 1 || transferred.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create transferred lease");
        }
        NewLeasePeriod transferredPeriod = leasePeriod(transferred.getId(), 1, request.transferDate(),
                request.endDate(), request.monthlyRent(), request.depositAmount(), request.paymentDay(),
                rentCalculationMethod, actorId);
        if (mapper.insertLeasePeriod(transferredPeriod) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create transferred lease period");
        }
        createSecurityDeposit(transferred);
        LocalDate firstBillingMonth = request.transferDate().getDayOfMonth() == 1
                ? request.transferDate().withDayOfMonth(1) : request.transferDate().plusMonths(1).withDayOfMonth(1);
        if (!today.isBefore(request.transferDate()) && !today.isAfter(request.endDate())
                && !firstBillingMonth.isAfter(today.withDayOfMonth(1))) {
            LocalDate due = firstBillingMonth.withDayOfMonth(Math.min(request.paymentDay(), firstBillingMonth.lengthOfMonth()));
            if (mapper.insertInvoice(transferred.getId(), firstBillingMonth, due,
                    rentAmount(firstBillingMonth, request.transferDate(), request.endDate(), request.monthlyRent(), rentCalculationMethod)) == 1) {
                syncManagementFee(current.getRentalMandateId(), actorId, firstBillingMonth);
            }
        }
        mapper.insertLeaseTransferAudit(actorId, leaseId, transferred.getId(), current.getTenantId(),
                request.newTenantId(), current.getEndDate(), request.transferDate());
        return transferred.getId();
    }

    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void generateCurrentMonthInvoices() {
        expireEndedLeases();
        mapper.generateMonthlyInvoices(LocalDate.now(clock).withDayOfMonth(1));
        mapper.findLeaseIdsWithAvailableRentCredit().forEach(leaseId -> applyAvailableRentCredits(null, leaseId));
        if (propertyExpensePostingService != null) propertyExpensePostingService.postDueExpenses();
    }

    private void syncManagementFee(Long mandateId, Long actorId, LocalDate billingMonth) {
        if (propertyExpensePostingService != null && mandateId != null) {
            propertyExpensePostingService.syncMandateFee(mandateId, actorId, billingMonth);
        }
    }

    @Transactional
    public int expireEndedLeases() {
        LocalDate today = LocalDate.now(clock);
        LocalDate graceCutoff = today.minusMonths(LEASE_AUTO_TERMINATION_GRACE_MONTHS);
        int expired = 0;
        for (ExpiredLeaseRow lease : mapper.findExpiredLeases(graceCutoff)) {
            if (mapper.expireLease(lease.getLeaseId(), graceCutoff) != 1) continue;
            mapper.markUnitAvailableIfNoActiveLease(lease.getUnitId());
            mapper.insertLeaseClosureAudit(null, lease.getLeaseId(), lease.getEndDate(), lease.getEndDate(),
                    "租约自然到期", "系统于租约结束满2个月宽限期后自动解约");
            expired++;
        }
        return expired;
    }

    private BigDecimal outstandingThroughLease(RentCollectionContext context, List<RentInvoiceAdvanceRow> invoices) {
        Map<LocalDate, RentInvoiceAdvanceRow> byMonth = invoiceRowsByMonth(invoices);
        BigDecimal total = BigDecimal.ZERO;
        LocalDate firstMonth = context.getBillingMonth().withDayOfMonth(1);
        LocalDate lastMonth = context.getEndDate().withDayOfMonth(1);
        String calculationMethod = rentCalculationMethod(context.getRentCalculationMethod());
        for (LocalDate month = firstMonth; !month.isAfter(lastMonth); month = month.plusMonths(1)) {
            RentInvoiceAdvanceRow row = byMonth.get(month);
            BigDecimal amountDue = row == null
                    ? rentAmount(month, context.getStartDate(), context.getEndDate(), context.getMonthlyRent(), calculationMethod)
                    : zero(row.getAmountDue());
            BigDecimal amountPaid = row == null ? BigDecimal.ZERO : zero(row.getAmountPaid());
            total = total.add(amountDue.subtract(amountPaid).max(BigDecimal.ZERO));
        }
        return total;
    }

    private List<RentInvoiceAdvanceRow> ensureAdvanceInvoices(RentCollectionContext context, BigDecimal advanceAmount,
            List<RentInvoiceAdvanceRow> invoices) {
        Map<LocalDate, RentInvoiceAdvanceRow> byMonth = invoiceRowsByMonth(invoices);
        LocalDate firstFutureMonth = context.getBillingMonth().withDayOfMonth(1).plusMonths(1);
        LocalDate lastMonth = context.getEndDate().withDayOfMonth(1);
        String calculationMethod = rentCalculationMethod(context.getRentCalculationMethod());
        BigDecimal availableFuture = sumOutstanding(invoices, firstFutureMonth);
        for (LocalDate month = firstFutureMonth;
                !month.isAfter(lastMonth) && availableFuture.compareTo(advanceAmount) < 0;
                month = month.plusMonths(1)) {
            RentInvoiceAdvanceRow row = byMonth.get(month);
            if (row == null) {
                BigDecimal amountDue = rentAmount(month, context.getStartDate(), context.getEndDate(),
                        context.getMonthlyRent(), calculationMethod);
                LocalDate dueDate = month.withDayOfMonth(Math.min(context.getPaymentDay(), month.lengthOfMonth()));
                mapper.insertInvoice(context.getLeaseId(), month, dueDate, amountDue);
                availableFuture = availableFuture.add(amountDue);
            } else {
                availableFuture = availableFuture.add(zero(row.getAmountDue()).subtract(zero(row.getAmountPaid())).max(BigDecimal.ZERO));
            }
        }
        List<RentInvoiceAdvanceRow> refreshed = mapper.lockRentInvoicesForAdvance(context.getLeaseId(),
                context.getBillingMonth(), lastMonth);
        if (sumOutstanding(refreshed, firstFutureMonth).compareTo(advanceAmount) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Unable to create enough future rent invoices for this payment");
        }
        return refreshed;
    }

    private List<RentPaymentAllocation> rentPaymentAllocations(List<RentInvoiceAdvanceRow> invoices,
            BigDecimal receivedAmount) {
        List<RentPaymentAllocation> allocations = new ArrayList<>();
        BigDecimal remaining = receivedAmount;
        for (RentInvoiceAdvanceRow invoice : invoices) {
            if (remaining.signum() <= 0) break;
            BigDecimal beforePaid = zero(invoice.getAmountPaid());
            BigDecimal outstanding = zero(invoice.getAmountDue()).subtract(beforePaid).max(BigDecimal.ZERO);
            if (outstanding.signum() <= 0) continue;
            BigDecimal allocated = remaining.min(outstanding);
            allocations.add(new RentPaymentAllocation(invoice.getInvoiceId(), invoice.getBillingMonth(), beforePaid,
                    outstanding, allocated));
            remaining = remaining.subtract(allocated);
        }
        if (remaining.signum() > 0 || allocations.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Unable to allocate rent payment to monthly invoices");
        }
        return allocations;
    }

    private record RentPaymentAllocation(Long invoiceId, LocalDate billingMonth, BigDecimal beforePaid,
            BigDecimal outstanding, BigDecimal amount) {
        boolean fullyCovered() { return amount.compareTo(outstanding) == 0; }
    }

    private Map<LocalDate, RentInvoiceAdvanceRow> invoiceRowsByMonth(List<RentInvoiceAdvanceRow> invoices) {
        Map<LocalDate, RentInvoiceAdvanceRow> rows = new java.util.HashMap<>();
        if (invoices != null) {
            for (RentInvoiceAdvanceRow invoice : invoices) {
                if (invoice != null && invoice.getBillingMonth() != null) rows.put(invoice.getBillingMonth().withDayOfMonth(1), invoice);
            }
        }
        return rows;
    }

    private BigDecimal sumOutstanding(List<RentInvoiceAdvanceRow> invoices, LocalDate fromMonth) {
        BigDecimal total = BigDecimal.ZERO;
        if (invoices == null) return total;
        for (RentInvoiceAdvanceRow invoice : invoices) {
            if (invoice == null || invoice.getBillingMonth() == null || invoice.getBillingMonth().isBefore(fromMonth)) continue;
            total = total.add(zero(invoice.getAmountDue()).subtract(zero(invoice.getAmountPaid())).max(BigDecimal.ZERO));
        }
        return total;
    }

    /** Applies confirmed prepayments to the oldest outstanding bill for the same lease. */
    private void applyAvailableRentCredits(Long actorId, Long leaseId) {
        List<RentInvoiceCreditRow> invoices = mapper.lockOutstandingInvoicesForCredit(leaseId);
        if (invoices.isEmpty()) return;
        for (RentCreditRow credit : mapper.lockAvailableRentCredits(leaseId)) {
            BigDecimal remaining = zero(credit.getRemainingAmount());
            for (RentInvoiceCreditRow invoice : invoices) {
                if (remaining.signum() <= 0) break;
                BigDecimal outstanding = zero(invoice.getAmountDue()).subtract(zero(invoice.getAmountPaid())).max(BigDecimal.ZERO);
                if (outstanding.signum() <= 0) continue;
                BigDecimal allocated = remaining.min(outstanding);
                if (mapper.applyRentCollection(invoice.getInvoiceId(), allocated) != 1
                        || mapper.consumeRentCredit(credit.getId(), allocated) != 1
                        || mapper.insertRentCreditAllocation(credit.getId(), invoice.getInvoiceId(), allocated, actorId) != 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Rent prepayment allocation changed; reload and try again");
                }
                invoice.setAmountPaid(zero(invoice.getAmountPaid()).add(allocated));
                remaining = remaining.subtract(allocated);
            }
        }
    }

    @Transactional
    public Long uploadContract(Long actorId, Long leaseId, MultipartFile file) {
        return storeContract(actorId, leaseId, file, MAX_CONTRACT_SIZE,
                "Only PDF, JPG or PNG lease contracts up to 10MB are supported");
    }

    @Transactional
    public Long uploadGeneratedContract(Long actorId, Long leaseId, MultipartFile file) {
        return storeContract(actorId, leaseId, file, MAX_GENERATED_CONTRACT_SIZE,
                "Generated PDF lease contracts must not exceed 30MB");
    }

    private Long storeContract(Long actorId, Long leaseId, MultipartFile file, long maximumSize,
            String validationMessage) {
        validateContract(file, maximumSize, validationMessage);
        if (mapper.countLeaseRentalMandate(leaseId) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Lease contract requires an active rental mandate covering the full lease term");
        }
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
            Long oldDocumentId = context.getContractDocumentId();
            if (oldDocumentId != null) {
                mapper.cancelActiveLeaseSignatures(leaseId, oldDocumentId);
            }
            if (mapper.insertContractDocument(document) != 1 || document.getId() == null
                    || mapper.insertContractLink(document.getId(), leaseId) != 1
                    || mapper.updateLeaseContract(leaseId, document.getId()) != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save lease contract");
            }
            mapper.updateInitialLeasePeriodContract(leaseId, document.getId());
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

    @Transactional
    public Long uploadRenewalContract(Long actorId, Long leaseId, Long periodId, MultipartFile file) {
        validateContract(file);
        LeasePeriodContractContext context = mapper.lockLeasePeriodContract(leaseId, periodId);
        if (context == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Renewal period not found");
        String token = UUID.randomUUID().toString().replace("-", "");
        String extension = CONTRACT_EXTENSIONS.get(file.getContentType());
        Path directory = contractStorageRoot.resolve(String.valueOf(leaseId))
                .resolve("period-" + context.getPeriodNo()).normalize();
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
            document.setDocumentNo("RENEWAL-DOC-" + timestamp + "-" + token.substring(0, 8).toUpperCase());
            document.setOriginalName(safeFileName(file.getOriginalFilename()));
            document.setStorageKey(contractStorageRoot.relativize(target).toString().replace('\\', '/'));
            document.setMimeType(file.getContentType()); document.setFileSize(file.getSize());
            document.setChecksumSha256(sha256(target)); document.setUploadedBy(actorId);
            if (mapper.insertContractDocument(document) != 1 || document.getId() == null
                    || mapper.insertLeasePeriodContractLink(document.getId(), periodId) != 1
                    || mapper.updateLeasePeriodContract(leaseId, periodId, document.getId()) != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save renewal contract");
            }
            Long oldDocumentId = context.getContractDocumentId();
            if (oldDocumentId != null && mapper.supersedeContractDocument(oldDocumentId) != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to replace renewal contract");
            }
            mapper.insertContractAudit(actorId, leaseId, oldDocumentId, document.getId(),
                    document.getOriginalName(), oldDocumentId == null ? "upload_renewal_contract" : "replace_renewal_contract");
            return document.getId();
        } catch (IOException exception) {
            deleteQuietly(target);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store renewal contract", exception);
        } catch (RuntimeException exception) {
            deleteQuietly(target);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Download downloadRenewalContract(Long leaseId, Long periodId) {
        ContractFile file = mapper.findLeasePeriodContractFile(leaseId, periodId);
        if (file == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Renewal contract not found");
        Path target = contractStorageRoot.resolve(file.getStorageKey()).normalize();
        if (!target.startsWith(contractStorageRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid contract path");
        }
        if (!Files.isRegularFile(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Renewal contract file is unavailable");
        }
        return new Download(target, safeFileName(file.getOriginalName()),
                normalize(file.getMimeType()) == null ? "application/octet-stream" : file.getMimeType(),
                file.getFileSize() == null ? 0 : file.getFileSize());
    }

    @Transactional(readOnly = true)
    public Download downloadContract(Long leaseId) {
        ContractFile file = mapper.findContractFile(leaseId);
        if (file == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lease contract not found");
        Path storageRoot = "electronic_signature".equals(file.getStorageArea())
                ? electronicSignatureStorageRoot : contractStorageRoot;
        Path target = storageRoot.resolve(file.getStorageKey()).normalize();
        if (!target.startsWith(storageRoot)) {
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
            r.getRentalSpaceId(), r.getRentalSpaceName(), r.getRentalSpaceType(),
            r.getLeaseStart(), r.getLeaseEnd(), zero(r.getMonthlyRent()), zero(r.getDepositAmount()), r.getPaymentDay(),
            rentCalculationMethod(r.getRentCalculationMethod()), r.getLeaseStatus(),
            r.getContractDocumentId(), r.getContractDocumentName(), r.getContractDocumentMimeType(), r.getContractDocumentSize(),
            r.getInvoiceId(), r.getBillingMonth(), r.getDueDate(), zero(r.getAmountDue()), zero(r.getAmountPaid()),
            zero(r.getAmountUnpaid()), zero(r.getTotalUnpaid()), zero(r.getPrepaidRentBalance()), r.getRentStatus(), r.getFinanceRecordId(), r.getTransactionNo(), r.getConfirmationStatus(),
             r.getPaymentMethod(), r.getPaymentDate(), r.getReviewNote(), r.getConfirmedByName(), r.getConfirmedAt(),
              r.getOwnerId(), r.getOwnerName(), r.getOwnerIdentity()); }

    private Long requireAvailableRentalSpace(Long unitId, Long requestedSpaceId, LocalDate startDate,
            LocalDate endDate, Long excludeLeaseId) {
        Long spaceId = requestedSpaceId != null ? requestedSpaceId : mapper.findWholeRentalSpaceId(unitId);
        // 兼容尚未执行数据库迁移的单元测试；生产数据库迁移后每套房产必有整套空间。
        if (spaceId == null || spaceId <= 0) {
            int overlaps = excludeLeaseId == null
                    ? mapper.countOverlappingLease(unitId, startDate, endDate)
                    : mapper.countOtherOverlappingLease(excludeLeaseId, unitId, startDate, endDate);
            if (overlaps > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "房产已有重叠租约");
            return null;
        }
        RentalSpaceContext space = mapper.findRentalSpace(spaceId);
        if (space == null || !Objects.equals(space.getUnitId(), unitId) || !"active".equals(space.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "出租空间不存在、已停用或不属于所选房产");
        }
        if ("room".equals(space.getSpaceType()) && !"shared".equals(space.getRentalMode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该房产尚未启用合租模式");
        }
        int overlaps = "whole_unit".equals(space.getSpaceType())
                ? mapper.countAnySpaceOverlap(excludeLeaseId, unitId, startDate, endDate)
                : mapper.countRoomOverlap(excludeLeaseId, unitId, spaceId, startDate, endDate);
        if (overlaps > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "所选出租空间在该租期已被占用；整租与房间合租也不能同时存在");
        }
        return spaceId;
    }
    private AdminTenantDirectoryResponse.Item toTenantDirectoryItem(TenantDirectoryRow r) { return new AdminTenantDirectoryResponse.Item(
            r.getTenantId(), r.getFullName(), r.getIdentityNo(), r.getPhone(), r.getEmail(), r.getStatus(),
            Boolean.TRUE.equals(r.getWhatsappEnabled()), r.getWhatsappDestination(), r.getWhatsappOptedInAt(), r.getCurrentLeaseNo(),
            r.getProjectName(), r.getUnitNo(), r.getLeaseStart(), r.getLeaseEnd(), zero(r.getCurrentDepositAmount()),
            zero(r.getCurrentDepositBalance()), r.getCurrentDepositStatus(),
            zero(r.getLeaseCount()), zero(r.getActiveLeaseCount())); }
    private AdminTenantDetailResponse.Lease toTenantLeaseDetail(TenantLeaseHistoryRow r) { return new AdminTenantDetailResponse.Lease(
            r.getLeaseId(), r.getUnitId(), r.getLeaseNo(), r.getProjectName(), r.getUnitNo(), r.getStatus(), r.getStartDate(), r.getEndDate(),
            zero(r.getMonthlyRent()), zero(r.getDepositAmount()), r.getPaymentDay(), zero(r.getUnpaidRent()),
            zero(r.getPendingMaintenanceCount()), r.getSignatureStatus(), r.getWorkflowStep()); }
    private AdminDepositAccountResponse.Item toDepositAccountItem(DepositAccountRow row) {
        return new AdminDepositAccountResponse.Item(row.getLeaseId(), row.getLeaseNo(), row.getTenantId(),
                row.getTenantName(), row.getTenantPhone(), row.getProjectName(), row.getUnitNo(), row.getLeaseStatus(),
                row.getStartDate(), row.getEndDate(), zero(row.getExpectedDeposit()), zero(row.getPostedBalance()),
                zero(row.getAvailableBalance()), row.getDepositEntryStatus(), depositAccountStatus(row));
    }
    private boolean matchesDepositKeyword(DepositAccountRow row, String keyword) {
        if (keyword == null) return true;
        String haystack = String.join(" ", text(row.getLeaseNo()), text(row.getTenantName()), text(row.getTenantPhone()),
                text(row.getProjectName()), text(row.getUnitNo())).toLowerCase();
        return haystack.contains(keyword.toLowerCase());
    }
    private long countDepositStatus(List<DepositAccountRow> rows, String status) {
        return rows.stream().filter(row -> status.equals(depositAccountStatus(row))).count();
    }
    private String depositAccountStatus(DepositAccountRow row) {
        if ("pending".equals(row.getDepositEntryStatus()) || "pending".equals(row.getConfirmationStatus())) return "pending_collection";
        if ("rejected".equals(row.getDepositEntryStatus()) || "rejected".equals(row.getConfirmationStatus())) return "collection_rejected";
        if ("active".equals(row.getLeaseStatus())) return "active";
        if (zero(row.getPendingSettlementCount()) > 0) return "settling";
        return zero(row.getAvailableBalance()).signum() > 0 ? "awaiting_settlement" : "settled";
    }
    private AdminRentFinanceResponse.Item toRentFinanceItem(RentFinanceRow r) { return new AdminRentFinanceResponse.Item(
            r.getId(), r.getTransactionNo(), r.getTenantName(), r.getProjectName(), r.getUnitNo(), r.getLeaseId(), r.getLeaseNo(),
            r.getInvoiceId(), r.getBillingMonth(), r.getDueDate(), zero(r.getInvoiceAmount()), zero(r.getInvoicePaid()),
            zero(r.getAmount()), r.getCurrency(), r.getTransactionDate(), r.getPaymentMethod(), r.getConfirmationStatus(),
            r.getSyncStatus(), r.getProofDocumentId(), r.getProofName(), r.getProofMimeType(), r.getProofSize(), r.getReceiptNo(), r.getReviewNote(), r.getAllocationNote(),
            r.getConfirmedByName(), r.getConfirmedAt(), r.getSubmittedAt()); }
    private AdminRentCollectionResponse.Item toRentCollectionItem(RentCollectionRow r) {
        return new AdminRentCollectionResponse.Item(r.getInvoiceId(), r.getLeaseId(), r.getLeaseNo(), r.getTenantName(),
                r.getProjectName(), r.getUnitNo(), r.getBillingMonth(), r.getDueDate(), zero(r.getAmountDue()),
                zero(r.getAmountPaid()), zero(r.getOutstandingAmount()), r.getCollectionStatus(), zero(r.getOverdueDays()),
                r.getLatestFinanceRecordId(), r.getLatestTransactionNo(), zero(r.getLatestPaymentAmount()),
                r.getLatestPaymentDate(), r.getLatestPaymentMethod(), r.getLatestProofDocumentId(), r.getLatestProofName(),
                r.getLatestProofMimeType(), r.getLatestProofSize(), r.getLatestConfirmedAt(), zero(r.getMonthlyRent()),
                r.getLeaseStartDate(), r.getLeaseEndDate(), r.getRentCalculationMethod());
    }
    private AdminLeaseRentInvoiceResponse toLeaseInvoiceDetail(LeaseInvoiceRow row) {
        return new AdminLeaseRentInvoiceResponse(row.getInvoiceId(), row.getBillingMonth(), row.getDueDate(),
                zero(row.getAmountDue()), zero(row.getAmountPaid()), zero(row.getAmountUnpaid()), row.getRentStatus());
    }
    private NewLeasePeriod leasePeriod(Long leaseId, int periodNo, LocalDate startDate, LocalDate endDate,
            BigDecimal monthlyRent, BigDecimal depositAmount, int paymentDay, String method, Long actorId) {
        NewLeasePeriod period = new NewLeasePeriod();
        period.setLeaseId(leaseId); period.setPeriodNo(periodNo); period.setStartDate(startDate); period.setEndDate(endDate);
        period.setMonthlyRent(monthlyRent); period.setDepositAmount(depositAmount); period.setPaymentDay(paymentDay);
        period.setRentCalculationMethod(method); period.setCreatedBy(actorId);
        return period;
    }
    private AdminLeasePeriodResponse leasePeriodResponse(LeasePeriodRow row) {
        LocalDate today = LocalDate.now(clock);
        String status = today.isBefore(row.getStartDate()) ? "scheduled"
                : today.isAfter(row.getEndDate()) ? "completed" : "current";
        return new AdminLeasePeriodResponse(row.getId(), row.getLeaseId(), row.getPeriodNo(), row.getStartDate(),
                row.getEndDate(), zero(row.getMonthlyRent()), zero(row.getDepositAmount()), row.getPaymentDay(),
                rentCalculationMethod(row.getRentCalculationMethod()), status, row.getContractDocumentId(),
                row.getContractDocumentName());
    }
    private String normalize(String v) { return v == null || v.trim().isEmpty() ? null : v.trim(); }
    private String depositDescription(String type, String leaseNo) {
        String label = switch (type) {
            case "tenant_advance" -> "代付租客费用";
            case "tenant_repayment" -> "租客归还代付款";
            case "refund" -> "退租押金余款返还";
            case "forfeiture" -> "退租押金余款没收";
            case "adjustment_credit" -> "押金调增";
            case "adjustment_debit" -> "押金调减";
            default -> "租客押金异动";
        };
        return label + " · " + text(leaseNo);
    }
    private void createSecurityDeposit(NewLease lease) {
        BigDecimal amount = zero(lease.getDepositAmount());
        if (amount.signum() <= 0) return;
        NewSecurityDeposit deposit = new NewSecurityDeposit();
        deposit.setLeaseId(lease.getId()); deposit.setUnitId(lease.getUnitId()); deposit.setTenantId(lease.getTenantId());
        deposit.setAmount(amount); deposit.setTransactionDate(lease.getStartDate());
        deposit.setTransactionNo("DEPOSIT-" + lease.getLeaseNo());
        deposit.setDescription("租客押金 · " + lease.getLeaseNo());
        if (mapper.insertSecurityDepositFinance(deposit) != 1 || deposit.getFinanceRecordId() == null
                || mapper.insertSecurityDepositEntry(deposit) != 1
                || mapper.insertSecurityDepositCashflow(deposit) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create security deposit finance record");
        }
    }
    private void createRenewalSecurityDeposit(LeaseChangeContext lease, int periodNo, BigDecimal newDepositAmount,
            LocalDate renewalStartDate) {
        BigDecimal increase = zero(newDepositAmount).subtract(zero(lease.getDepositAmount()))
                .setScale(2, RoundingMode.HALF_UP);
        if (increase.signum() <= 0) return;
        NewSecurityDeposit deposit = new NewSecurityDeposit();
        deposit.setLeaseId(lease.getLeaseId()); deposit.setUnitId(lease.getUnitId());
        deposit.setTenantId(lease.getTenantId()); deposit.setAmount(increase);
        deposit.setTransactionDate(renewalStartDate);
        deposit.setTransactionNo("DEPOSIT-RENEW-" + lease.getLeaseNo() + "-P" + periodNo);
        deposit.setDescription("续约补收租客押金 · " + lease.getLeaseNo() + " · 第" + periodNo + "期");
        if (mapper.insertSecurityDepositFinance(deposit) != 1 || deposit.getFinanceRecordId() == null
                || mapper.insertSecurityDepositEntry(deposit) != 1
                || mapper.insertSecurityDepositCashflow(deposit) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Unable to create renewal security deposit finance record");
        }
    }
    private void syncPendingSecurityDeposit(Long leaseId, Long tenantId, Long unitId, BigDecimal amount,
            LocalDate transactionDate, String leaseNo) {
        BigDecimal normalizedAmount = zero(amount);
        if (normalizedAmount.signum() <= 0) {
            mapper.voidPendingSecurityDeposit(leaseId);
            mapper.rejectPendingSecurityDepositEntry(leaseId);
            return;
        }
        int updated = mapper.updatePendingSecurityDepositFinance(leaseId, unitId, tenantId, normalizedAmount, transactionDate);
        if (updated == 1 && mapper.updatePendingSecurityDepositCashflow(leaseId, unitId, tenantId,
                "租客押金 · " + (leaseNo == null ? "租约 " + leaseId : leaseNo), transactionDate) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to synchronize security deposit finance record");
        }
    }
    private String rentCalculationMethod(String value) {
        String normalized = normalize(value);
        if (normalized == null) return "daily_prorated";
        if (!RENT_CALCULATION_METHODS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rent calculation method");
        }
        return normalized;
    }
    private BigDecimal rentAmount(LocalDate billingMonth, LocalDate leaseStart, LocalDate leaseEnd,
            BigDecimal monthlyRent, String calculationMethod) {
        if (!"daily_prorated".equals(calculationMethod)) return monthlyRent;
        LocalDate monthStart = billingMonth.withDayOfMonth(1);
        LocalDate monthEnd = billingMonth.withDayOfMonth(billingMonth.lengthOfMonth());
        LocalDate activeStart = leaseStart.isAfter(monthStart) ? leaseStart : monthStart;
        LocalDate activeEnd = leaseEnd.isBefore(monthEnd) ? leaseEnd : monthEnd;
        if (activeStart.isAfter(activeEnd)) return BigDecimal.ZERO.setScale(2);
        long activeDays = activeEnd.toEpochDay() - activeStart.toEpochDay() + 1;
        BigDecimal prorated = monthlyRent.multiply(BigDecimal.valueOf(activeDays))
                .divide(BigDecimal.valueOf(billingMonth.lengthOfMonth()), 2, RoundingMode.HALF_UP);
        return prorated.signum() == 0 && monthlyRent.signum() > 0
                ? new BigDecimal("0.01") : prorated;
    }
    private void requireAgencyContract(Long unitId, LocalDate startDate, LocalDate endDate) {
        if (mapper.countActiveRentalMandate(unitId, startDate, endDate) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An active rental mandate covering the full lease term is required");
        }
    }
    private BigDecimal zero(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private long zero(Long v) { return v == null ? 0 : v; }
    private String money(BigDecimal v) { return zero(v).setScale(2).toPlainString(); }
    private void validateContract(MultipartFile file) {
        validateContract(file, MAX_CONTRACT_SIZE,
                "Only PDF, JPG or PNG lease contracts up to 10MB are supported");
    }
    private void validateContract(MultipartFile file, long maximumSize, String validationMessage) {
        boolean valid = file != null && !file.isEmpty() && file.getSize() <= maximumSize
                && CONTRACT_EXTENSIONS.containsKey(file.getContentType());
        if (!valid) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                validationMessage);
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
    private String text(String value) { return value == null || value.isBlank() ? "—" : value; }
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
