package com.ccps.backend.service;

import java.math.BigDecimal;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.math.RoundingMode;
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
import com.ccps.backend.dto.AdminLeaseCloseRequest;
import com.ccps.backend.dto.AdminLeaseInvoiceCreateRequest;
import com.ccps.backend.dto.AdminLeaseRentInvoiceResponse;
import com.ccps.backend.dto.AdminLeaseUpdateRequest;
import com.ccps.backend.dto.AdminLeaseTransferRequest;
import com.ccps.backend.dto.AdminRentFinanceResponse;
import com.ccps.backend.dto.AdminRentCollectionResponse;
import com.ccps.backend.dto.AdminRentCollectionRequest;
import com.ccps.backend.dto.AdminRecordCreateResponse;
import com.ccps.backend.dto.AdminTenancyOptionsResponse;
import com.ccps.backend.dto.AdminTenancyResponse;
import com.ccps.backend.dto.AdminTenantCreateRequest;
import com.ccps.backend.dto.AdminTenantDirectoryResponse;
import com.ccps.backend.dto.AdminTenantDetailResponse;
import com.ccps.backend.mapper.AdminTenancyMapper;
import com.ccps.backend.mapper.AdminTenancyMapper.ContractFile;
import com.ccps.backend.mapper.AdminTenancyMapper.LeaseContractContext;
import com.ccps.backend.mapper.AdminTenancyMapper.LeaseChangeContext;
import com.ccps.backend.mapper.AdminTenancyMapper.LeaseInvoiceRow;
import com.ccps.backend.mapper.AdminTenancyMapper.NewLease;
import com.ccps.backend.mapper.AdminTenancyMapper.NewContractDocument;
import com.ccps.backend.mapper.AdminTenancyMapper.NewTenant;
import com.ccps.backend.mapper.AdminTenancyMapper.RentFinanceRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentFinanceSummaryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentProofContext;
import com.ccps.backend.mapper.AdminTenancyMapper.RentCollectionRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentCollectionSummaryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentCollectionContext;
import com.ccps.backend.mapper.AdminTenancyMapper.RentInvoiceAdvanceRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentReceiptRow;
import com.ccps.backend.mapper.AdminTenancyMapper.NewRentCollection;
import com.ccps.backend.mapper.AdminTenancyMapper.NewRentCredit;
import com.ccps.backend.mapper.AdminTenancyMapper.RentCreditRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentInvoiceCreditRow;
import com.ccps.backend.mapper.AdminTenancyMapper.SummaryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.TenancyRow;
import com.ccps.backend.mapper.AdminTenancyMapper.TenantDirectoryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.TenantDirectorySummaryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.TenantDirectoryOverviewRow;
import com.ccps.backend.mapper.AdminTenancyMapper.TenantLeaseHistoryRow;
import com.ccps.backend.mapper.AdminTenancyMapper.ExpiredLeaseRow;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class AdminTenancyService {
    private static final Set<String> STATUSES = Set.of("paid", "partial", "unpaid", "overdue", "pending_review");
    private static final Set<String> RENT_PAYMENT_METHODS = Set.of("bank_transfer", "online_payment", "cash", "cheque");
    private static final Set<String> RENT_CALCULATION_METHODS = Set.of("daily_prorated");
    private static final long MAX_CONTRACT_SIZE = 10L * 1024 * 1024;
    private static final Map<String, String> CONTRACT_EXTENSIONS = Map.of(
            "application/pdf", ".pdf", "image/jpeg", ".jpg", "image/png", ".png");
    private final AdminTenancyMapper mapper;
    private final Clock clock;
    private final Path contractStorageRoot;
    private final Path rentProofStorageRoot;
    private final Path electronicSignatureStorageRoot;

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
                overview == null ? 0 : zero(overview.getPendingSignatureCount())),
                mapper.findTenantLeaseHistory(tenantId).stream().map(this::toTenantLeaseDetail).toList());
    }

    @Transactional
    public AdminTenancyOptionsResponse options() {
        expireEndedLeases();
        return new AdminTenancyOptionsResponse(mapper.findProjects(), mapper.findTenantOptions(), mapper.findAvailableUnits());
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
        List<RentInvoiceAdvanceRow> lockedInvoices = mapper.lockRentInvoicesForAdvance(context.getLeaseId(),
                context.getBillingMonth(), context.getEndDate().withDayOfMonth(1));
        BigDecimal leaseOutstanding = outstandingThroughLease(context, lockedInvoices);
        if (request.amount().compareTo(leaseOutstanding) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Received amount exceeds the remaining rent for this lease");
        }
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String timestamp = LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String transactionNo = "RENT-ADM-" + timestamp + "-" + token;
        String receiptNo = "RENT-RCP-" + timestamp + "-" + token;
        String payerName = normalize(request.payerName()) == null ? context.getTenantName() : request.payerName().trim();
        String reference = normalize(request.paymentReference());
        String note = normalize(request.note()) == null ? "管理員確認租金收款" : request.note().trim();
        BigDecimal appliedToCurrentInvoice = request.amount().min(outstanding);
        BigDecimal prepaymentAmount = request.amount().subtract(appliedToCurrentInvoice);
        BigDecimal afterPaid = beforePaid.add(appliedToCurrentInvoice);
        if (prepaymentAmount.signum() > 0) {
            ensureAdvanceInvoices(context, prepaymentAmount, lockedInvoices);
        }

        NewRentCollection record = new NewRentCollection();
        record.setTransactionNo(transactionNo); record.setUnitId(context.getUnitId()); record.setOwnerId(context.getOwnerId());
        record.setTenantId(context.getTenantId()); record.setAmount(request.amount()); record.setPaymentDate(request.paymentDate());
        record.setPaymentMethod(request.paymentMethod()); record.setActorId(actorId);
        if (mapper.insertConfirmedRentPayment(record) != 1 || record.getId() == null
                || mapper.linkRentPayment(invoiceId, record.getId()) != 1
                || mapper.insertRentCollectionReceipt(record.getId(), receiptNo, payerName, reference, note) != 1
                || mapper.insertRentCashflow(record.getId(),context.getUnitId(),context.getOwnerId(),context.getTenantId(),
                        prepaymentAmount.signum() > 0 ? "租金收款及預收 · "+context.getLeaseNo() : "租金收款 · "+context.getLeaseNo(),request.paymentDate()) != 1
                || mapper.applyRentCollection(invoiceId, appliedToCurrentInvoice) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Rent collection state changed; reload and try again");
        }
        if (prepaymentAmount.signum() > 0) {
            NewRentCredit credit = new NewRentCredit();
            credit.setLeaseId(context.getLeaseId()); credit.setFinanceRecordId(record.getId());
            credit.setReceivedAmount(prepaymentAmount); credit.setRemainingAmount(prepaymentAmount); credit.setActorId(actorId);
            if (mapper.insertRentCredit(credit) != 1 || credit.getId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create rent prepayment balance");
            }
            applyAvailableRentCredits(actorId, context.getLeaseId());
        }
        mapper.insertRentCollectionAudit(actorId, record.getId(), invoiceId, beforePaid, appliedToCurrentInvoice, afterPaid,
                prepaymentAmount.signum() > 0 ? note + "；預收租金 RM " + money(prepaymentAmount) : note);
        mapper.insertTenantNotification(context.getTenantId(), "租金收款已確認",
                "%s %s 已確認收到租金 RM %s，本期尚欠 RM %s。".formatted(context.getProjectName(), context.getUnitNo(),
                        money(appliedToCurrentInvoice), money(outstanding.subtract(appliedToCurrentInvoice))),
                "rent_invoice", invoiceId, "normal");
        if (proof != null && !proof.isEmpty()) uploadRentProof(actorId, record.getId(), proof);
        return new AdminRecordCreateResponse(record.getId(), transactionNo, receiptNo);
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
    public void updateTenant(Long tenantId,AdminTenantCreateRequest request) {
        if(mapper.countTenant(tenantId)!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Tenant not found");
        String identity=normalize(request.identityNo()); String email=normalize(request.email());
        if(identity!=null&&mapper.countTenantIdentityExcluding(identity,tenantId)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Tenant identity number already exists");
        if(email!=null&&mapper.countTenantEmailExcluding(email,tenantId)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Tenant email already exists");
        NewTenant tenant=new NewTenant();tenant.setId(tenantId);tenant.setFullName(request.fullName().trim());tenant.setIdentityNo(identity);
        tenant.setPhone(normalize(request.phone()));tenant.setEmail(email);tenant.setStatus(request.status());
        if(mapper.updateTenant(tenant)!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Unable to update tenant");
    }

    @Transactional
    public void deleteTenant(Long tenantId) {
        if (mapper.countTenant(tenantId) != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found");
        if (mapper.countTenantLeases(tenantId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant has lease history and cannot be deleted; deactivate the tenant instead");
        }
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
        if (mapper.countOverlappingLease(request.unitId(), request.startDate(), request.endDate()) > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Unit already has an overlapping active lease");
        String rentCalculationMethod = rentCalculationMethod(request.rentCalculationMethod());
        NewLease lease = new NewLease(); lease.setTenantId(request.tenantId()); lease.setUnitId(request.unitId());
        lease.setRentalMandateId(request.rentalMandateId());
        lease.setLeaseNo("LEASE-" + LocalDate.now(clock).toString().replace("-", "") + "-" + Long.toHexString(System.nanoTime()).toUpperCase());
        lease.setStartDate(request.startDate()); lease.setEndDate(request.endDate()); lease.setMonthlyRent(request.monthlyRent());
        lease.setDepositAmount(request.depositAmount()); lease.setPaymentDay(request.paymentDay()); lease.setRentCalculationMethod(rentCalculationMethod);
        if (mapper.insertLease(lease) != 1 || lease.getId() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create lease");
        mapper.activateRentalService(request.unitId()); mapper.markUnitRented(request.unitId());
        LocalDate currentMonth = LocalDate.now(clock).withDayOfMonth(1);
        LocalDate lastBillingMonth = request.endDate().withDayOfMonth(1).isBefore(currentMonth)
                ? request.endDate().withDayOfMonth(1) : currentMonth;
        for (LocalDate month = request.startDate().withDayOfMonth(1);
                !month.isAfter(lastBillingMonth); month = month.plusMonths(1)) {
            LocalDate due = month.withDayOfMonth(Math.min(request.paymentDay(), month.lengthOfMonth()));
            mapper.insertInvoice(lease.getId(), month, due,
                    rentAmount(month, request.startDate(), request.endDate(), request.monthlyRent(), rentCalculationMethod));
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
        LocalDate dueDate = billingMonth.withDayOfMonth(Math.min(lease.getPaymentDay(), billingMonth.lengthOfMonth()));
        BigDecimal amountDue = rentAmount(billingMonth, lease.getStartDate(), lease.getEndDate(),
                lease.getMonthlyRent(), rentCalculationMethod(lease.getRentCalculationMethod()));
        if (mapper.repairZeroAmountInvoice(leaseId, billingMonth, dueDate, amountDue) == 1) {
            applyAvailableRentCredits(null, leaseId);
            return;
        }
        if (mapper.insertInvoice(leaseId, billingMonth, dueDate, amountDue) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice already exists or could not be created");
        }
        applyAvailableRentCredits(null, leaseId);
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
        requireAgencyContract(current.getUnitId(), request.startDate(), request.endDate());
        if (mapper.countOtherOverlappingLease(leaseId, current.getUnitId(), request.startDate(), request.endDate()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unit has another overlapping active lease");
        }
        String rentCalculationMethod = rentCalculationMethod(request.rentCalculationMethod());
        if (mapper.updateLeaseTerms(leaseId, request.startDate(), request.endDate(), request.monthlyRent(),
                request.depositAmount(), request.paymentDay(), rentCalculationMethod) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lease state changed; reload and try again");
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
            mapper.insertInvoice(leaseId, month, due,
                    rentAmount(month, request.startDate(), request.endDate(), request.monthlyRent(), rentCalculationMethod));
        }
        mapper.insertLeaseUpdateAudit(actorId, leaseId, current.getStartDate(), current.getEndDate(),
                current.getMonthlyRent(), current.getDepositAmount(), current.getPaymentDay(), request.startDate(),
                request.endDate(), request.monthlyRent(), request.depositAmount(), request.paymentDay());
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
        String rentCalculationMethod = rentCalculationMethod(request.rentCalculationMethod());
        transferred.setPaymentDay(request.paymentDay()); transferred.setRentCalculationMethod(rentCalculationMethod);
        if (mapper.insertLease(transferred) != 1 || transferred.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create transferred lease");
        }
        LocalDate firstBillingMonth = request.transferDate().getDayOfMonth() == 1
                ? request.transferDate().withDayOfMonth(1) : request.transferDate().plusMonths(1).withDayOfMonth(1);
        if (!today.isBefore(request.transferDate()) && !today.isAfter(request.endDate())
                && !firstBillingMonth.isAfter(today.withDayOfMonth(1))) {
            LocalDate due = firstBillingMonth.withDayOfMonth(Math.min(request.paymentDay(), firstBillingMonth.lengthOfMonth()));
            mapper.insertInvoice(transferred.getId(), firstBillingMonth, due,
                    rentAmount(firstBillingMonth, request.transferDate(), request.endDate(), request.monthlyRent(), rentCalculationMethod));
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
    }

    @Transactional
    public int expireEndedLeases() {
        LocalDate today = LocalDate.now(clock);
        int expired = 0;
        for (ExpiredLeaseRow lease : mapper.findExpiredLeases(today)) {
            if (mapper.expireLease(lease.getLeaseId(), today) != 1) continue;
            mapper.markUnitAvailableIfNoActiveLease(lease.getUnitId());
            mapper.insertLeaseClosureAudit(null, lease.getLeaseId(), lease.getEndDate(), lease.getEndDate(),
                    "租约自然到期", "系统于到期日次日自动结束租约");
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

    private void ensureAdvanceInvoices(RentCollectionContext context, BigDecimal advanceAmount,
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
        validateContract(file);
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
            if (mapper.insertContractDocument(document) != 1 || document.getId() == null
                    || mapper.insertContractLink(document.getId(), leaseId) != 1
                    || mapper.updateLeaseContract(leaseId, document.getId()) != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save lease contract");
            }
            Long oldDocumentId = context.getContractDocumentId();
            if (oldDocumentId != null && mapper.supersedeContractDocument(oldDocumentId) != 1) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to replace lease contract");
            }
            // Replacing the blank contract invalidates any old signing link for this lease.
            mapper.cancelPendingLeaseSignatures(leaseId);
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

    @Transactional(readOnly = true)
    public Download downloadRentReceipt(Long financeRecordId) {
        RentReceiptRow row = mapper.findRentReceipt(financeRecordId);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rent receipt not found");
        Path directory = rentProofStorageRoot.resolve("receipts").normalize();
        Path target = directory.resolve(safeFileName(row.getReceiptNo()) + ".pdf").normalize();
        if (!directory.startsWith(rentProofStorageRoot) || !target.startsWith(directory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid receipt path");
        }
        try {
            Files.createDirectories(directory);
            try (OutputStream output = Files.newOutputStream(target)) {
                Document document = new Document();
                PdfWriter.getInstance(document, output);
                document.open();
                BaseFont base = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
                com.lowagie.text.Font title = new com.lowagie.text.Font(base, 18, com.lowagie.text.Font.BOLD);
                com.lowagie.text.Font body = new com.lowagie.text.Font(base, 10);
                Paragraph heading = new Paragraph("Official Receipt / 收據", title); heading.setAlignment(Element.ALIGN_CENTER); document.add(heading);
                Paragraph no = new Paragraph("收據編號：" + text(row.getReceiptNo()), body); no.setAlignment(Element.ALIGN_RIGHT); document.add(no);
                document.add(new Paragraph(" ", body));
                PdfPTable table = new PdfPTable(2); table.setWidthPercentage(100); table.setWidths(new float[]{1.2f, 2.8f});
                receiptCell(table, "租客", row.getTenantName(), body); receiptCell(table, "建案／單位", text(row.getProjectName()) + " / " + text(row.getUnitNo()), body);
                receiptCell(table, "租約", row.getLeaseNo(), body); receiptCell(table, "帳單月份", monthText(row.getBillingMonth()), body);
                receiptCell(table, "收款日期", dateText(row.getTransactionDate()), body); receiptCell(table, "收款金額", text(row.getCurrency()) + " " + money(row.getAmount()), body);
                receiptCell(table, "付款方式", paymentMethodText(row.getPaymentMethod()), body); receiptCell(table, "付款人", row.getPayerName(), body);
                receiptCell(table, "付款參考", row.getBankReference(), body); receiptCell(table, "備註", row.getSubmissionNote(), body);
                document.add(table); document.add(new Paragraph("\n此收據由系統在確認收款後自動產生。", body)); document.close();
            }
            return new Download(target, safeFileName(row.getReceiptNo()) + ".pdf", "application/pdf", Files.size(target));
        } catch (Exception exception) {
            deleteQuietly(target);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to generate rent receipt", exception);
        }
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
            r.getLeaseStart(), r.getLeaseEnd(), zero(r.getMonthlyRent()), zero(r.getDepositAmount()), r.getPaymentDay(),
            rentCalculationMethod(r.getRentCalculationMethod()), r.getLeaseStatus(),
            r.getContractDocumentId(), r.getContractDocumentName(), r.getContractDocumentMimeType(), r.getContractDocumentSize(),
            r.getInvoiceId(), r.getBillingMonth(), r.getDueDate(), zero(r.getAmountDue()), zero(r.getAmountPaid()),
            zero(r.getAmountUnpaid()), zero(r.getTotalUnpaid()), zero(r.getPrepaidRentBalance()), r.getRentStatus(), r.getFinanceRecordId(), r.getTransactionNo(), r.getConfirmationStatus(),
             r.getPaymentMethod(), r.getPaymentDate(), r.getReviewNote(), r.getConfirmedByName(), r.getConfirmedAt(),
              r.getOwnerId(), r.getOwnerName(), r.getOwnerIdentity()); }
    private AdminTenantDirectoryResponse.Item toTenantDirectoryItem(TenantDirectoryRow r) { return new AdminTenantDirectoryResponse.Item(
            r.getTenantId(), r.getFullName(), r.getIdentityNo(), r.getPhone(), r.getEmail(), r.getStatus(), r.getCurrentLeaseNo(),
            r.getProjectName(), r.getUnitNo(), r.getLeaseStart(), r.getLeaseEnd(), zero(r.getLeaseCount()), zero(r.getActiveLeaseCount())); }
    private AdminTenantDetailResponse.Lease toTenantLeaseDetail(TenantLeaseHistoryRow r) { return new AdminTenantDetailResponse.Lease(
            r.getLeaseId(), r.getUnitId(), r.getLeaseNo(), r.getProjectName(), r.getUnitNo(), r.getStatus(), r.getStartDate(), r.getEndDate(),
            zero(r.getMonthlyRent()), zero(r.getDepositAmount()), r.getPaymentDay(), zero(r.getUnpaidRent()),
            zero(r.getPendingMaintenanceCount()), r.getSignatureStatus(), r.getWorkflowStep()); }
    private AdminRentFinanceResponse.Item toRentFinanceItem(RentFinanceRow r) { return new AdminRentFinanceResponse.Item(
            r.getId(), r.getTransactionNo(), r.getTenantName(), r.getProjectName(), r.getUnitNo(), r.getLeaseId(), r.getLeaseNo(),
            r.getInvoiceId(), r.getBillingMonth(), r.getDueDate(), zero(r.getInvoiceAmount()), zero(r.getInvoicePaid()),
            zero(r.getAmount()), r.getCurrency(), r.getTransactionDate(), r.getPaymentMethod(), r.getConfirmationStatus(),
            r.getSyncStatus(), r.getProofDocumentId(), r.getProofName(), r.getProofMimeType(), r.getProofSize(), r.getReceiptNo(), r.getReviewNote(),
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
    private String normalize(String v) { return v == null || v.trim().isEmpty() ? null : v.trim(); }
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
    private void receiptCell(PdfPTable table, String label, String value, com.lowagie.text.Font font) { table.addCell(new Phrase(label, font)); table.addCell(new Phrase(text(value), font)); }
    private String text(String value) { return value == null || value.isBlank() ? "—" : value; }
    private String dateText(LocalDate value) { return value == null ? "—" : value.toString(); }
    private String monthText(LocalDate value) { return value == null ? "—" : value.toString().substring(0, 7); }
    private String paymentMethodText(String value) { return switch (value == null ? "" : value) { case "bank_transfer" -> "銀行轉帳"; case "online_payment" -> "線上支付"; case "cash" -> "現金"; case "cheque" -> "支票"; default -> text(value); }; }
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
