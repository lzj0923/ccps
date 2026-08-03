package com.ccps.backend.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.ccps.backend.dto.AdminOwnerResponse;
import com.ccps.backend.dto.AdminOwnerResponse.Property;
import com.ccps.backend.dto.AdminPropertyBasicSaveRequest;
import com.ccps.backend.dto.AdminPropertyContractRecordResponse;
import com.ccps.backend.dto.AdminPropertyLeaseOptionResponse;
import com.ccps.backend.dto.AdminPropertyOwnershipResponse;
import com.ccps.backend.dto.AdminPropertyWorkspaceResponse;
import com.ccps.backend.service.AdminOwnerService.PropertyReference;

@Service
public class AdminPropertyWorkspaceService {
    private final AdminOwnerService ownerService;
    private final AdminPropertyOwnershipService ownershipService;
    private final AdminPropertyContractRecordService contractService;
    private final PropertyExpensePostingService expensePostingService;
    private final Clock clock;

    @Autowired
    public AdminPropertyWorkspaceService(AdminOwnerService ownerService,
            AdminPropertyOwnershipService ownershipService,
            AdminPropertyContractRecordService contractService,
            PropertyExpensePostingService expensePostingService) {
        this(ownerService, ownershipService, contractService, expensePostingService, Clock.systemDefaultZone());
    }

    AdminPropertyWorkspaceService(AdminOwnerService ownerService,
            AdminPropertyOwnershipService ownershipService,
            AdminPropertyContractRecordService contractService) {
        this(ownerService, ownershipService, contractService, null, Clock.systemDefaultZone());
    }

    AdminPropertyWorkspaceService(AdminOwnerService ownerService,
            AdminPropertyOwnershipService ownershipService,
            AdminPropertyContractRecordService contractService, Clock clock) {
        this(ownerService, ownershipService, contractService, null, clock);
    }

    private AdminPropertyWorkspaceService(AdminOwnerService ownerService,
            AdminPropertyOwnershipService ownershipService,
            AdminPropertyContractRecordService contractService,
            PropertyExpensePostingService expensePostingService, Clock clock) {
        this.ownerService = ownerService;
        this.ownershipService = ownershipService;
        this.contractService = contractService;
        this.expensePostingService = expensePostingService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminPropertyWorkspaceResponse load(Long ownerId, Long ownerUnitId) {
        Property property = ownerService.findProperty(ownerId, ownerUnitId);
        AdminOwnerResponse owner = ownerService.findOwner(ownerId);
        List<AdminPropertyOwnershipResponse> ownerships = ownershipService.list(property.unitId());
        List<AdminPropertyContractRecordResponse> contracts = contractService.list(ownerId, ownerUnitId);
        List<AdminPropertyLeaseOptionResponse> leases = contractService.leaseOptions(ownerId, ownerUnitId);
        return new AdminPropertyWorkspaceResponse(property, owner,
                ownerService.findPropertyBasicProfile(ownerId, ownerUnitId), ownerships, contracts, leases,
                intelligence(property, ownerships, contracts, leases));
    }

    @Transactional(readOnly = true)
    public AdminPropertyWorkspaceResponse loadByUnitId(Long unitId) {
        PropertyReference reference = ownerService.findPropertyReference(unitId);
        return load(reference.ownerId(), reference.ownerUnitId());
    }

    /**
     * Saves the three parts shown by the basic-information form as one database
     * transaction. A failure in any part rolls the complete change back.
     */
    @Transactional
    public AdminPropertyWorkspaceResponse saveBasicByUnitId(Long unitId,
            AdminPropertyBasicSaveRequest request) {
        return saveBasicByUnitId(unitId, request, null);
    }

    @Transactional
    public AdminPropertyWorkspaceResponse saveBasicByUnitId(Long unitId,
            AdminPropertyBasicSaveRequest request, Long actorId) {
        PropertyReference reference = ownerService.findPropertyReference(unitId);
        ownerService.updateProperty(reference.ownerId(), reference.ownerUnitId(), request.property());
        ownerService.savePropertyBasicProfile(reference.ownerId(), reference.ownerUnitId(), request.profile());
        ownerService.updateOwner(reference.ownerId(), request.owner());
        if (expensePostingService != null) {
            expensePostingService.syncCurrent(reference.ownerUnitId(), request.profile(), actorId);
        }
        return load(reference.ownerId(), reference.ownerUnitId());
    }

    private AdminPropertyWorkspaceResponse.Intelligence intelligence(Property property,
            List<AdminPropertyOwnershipResponse> ownerships,
            List<AdminPropertyContractRecordResponse> contracts,
            List<AdminPropertyLeaseOptionResponse> leases) {
        LocalDate today = LocalDate.now(clock);
        AdminPropertyLeaseOptionResponse effectiveLease = leases.stream()
                .filter(lease -> "active".equals(lease.status()))
                .filter(lease -> lease.startDate() == null || !lease.startDate().isAfter(today))
                .filter(lease -> lease.endDate() == null || !lease.endDate().isBefore(today))
                .max(Comparator.comparing(AdminPropertyLeaseOptionResponse::startDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
        AdminPropertyLeaseOptionResponse upcomingLease = leases.stream()
                .filter(lease -> "active".equals(lease.status()) && lease.startDate() != null
                        && lease.startDate().isAfter(today))
                .min(Comparator.comparing(AdminPropertyLeaseOptionResponse::startDate))
                .orElse(null);

        String rentalStatus = effectiveLease != null ? "rented"
                : upcomingLease != null ? "reserved" : property.listingStatus();
        BigDecimal ownershipTotal = ownerships.stream().filter(item -> "active".equals(item.status()))
                .map(AdminPropertyOwnershipResponse::ownershipPercent).filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean ownershipComplete = ownershipTotal.compareTo(new BigDecimal("100.00")) == 0;
        boolean leaseContractMissing = effectiveLease != null && contracts.stream().noneMatch(contract ->
                "L_LEASE".equals(contract.contractType())
                        && effectiveLease.leaseId().equals(contract.leaseId())
                        && !"cancelled".equals(contract.status())
                        && contract.originalName() != null);

        List<String> warnings = new ArrayList<>();
        if (!ownershipComplete) warnings.add("有效所有權比例目前為 " + ownershipTotal.stripTrailingZeros().toPlainString() + "%");
        if (leaseContractMissing) warnings.add("有效租約尚未建立 L.租賃合約及附件");
        if (effectiveLease != null && !AdminOwnerService.isRentedListingStatus(property.listingStatus())) {
            warnings.add("房產存在有效租約，出租狀態已按租約判定為出租中");
        }
        if (effectiveLease == null && AdminOwnerService.isRentedListingStatus(property.listingStatus())) {
            warnings.add("房產標記為出租中，但目前沒有生效中的租約");
        }
        return new AdminPropertyWorkspaceResponse.Intelligence(rentalStatus,
                effectiveLease == null ? null : effectiveLease.leaseId(), ownershipTotal,
                ownershipComplete, leaseContractMissing, List.copyOf(warnings));
    }
}
