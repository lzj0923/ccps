package com.ccps.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminOwnerResponse;
import com.ccps.backend.dto.AdminOwnerCreateRequest;
import com.ccps.backend.dto.AdminProjectOption;
import com.ccps.backend.dto.AdminPropertyCreateRequest;
import com.ccps.backend.dto.AdminOwnerResponse.Property;
import com.ccps.backend.dto.AdminPropertyUpdateRequest;
import com.ccps.backend.dto.AdminOwnerSummaryResponse;
import com.ccps.backend.dto.AdminPropertyPageResponse;
import com.ccps.backend.mapper.AdminOwnerMapper;
import com.ccps.backend.mapper.AdminOwnerMapper.OwnerPropertyRow;
import com.ccps.backend.mapper.AdminOwnerMapper.NewOwner;
import com.ccps.backend.mapper.AdminOwnerMapper.NewOwnerUnit;
import com.ccps.backend.mapper.AdminOwnerMapper.NewPurchaseContract;
import com.ccps.backend.mapper.AdminOwnerMapper.NewUnit;

@Service
public class AdminOwnerService {
    private static final String PRE_HANDOVER = "PRE_HANDOVER";
    private static final String OPERATING = "OPERATING";
    private static final String DISPOSED = "DISPOSED";
    private static final Set<String> SERVICE_TYPES = Set.of("RENTAL", "RESALE", "MANAGEMENT");
    private final AdminOwnerMapper mapper;
    private final AdminAccountService accountService;

    public AdminOwnerService(AdminOwnerMapper mapper) {
        this(mapper, null);
    }

    @Autowired
    public AdminOwnerService(AdminOwnerMapper mapper, AdminAccountService accountService) {
        this.mapper = mapper;
        this.accountService = accountService;
    }

    @Transactional(readOnly = true)
    public List<AdminOwnerResponse> findOwners() {
        Map<Long, OwnerAccumulator> owners = new LinkedHashMap<>();
        for (OwnerPropertyRow row : mapper.findOwnersWithProperties()) {
            OwnerAccumulator owner = owners.computeIfAbsent(row.getOwnerId(), ignored -> new OwnerAccumulator(row));
            if (row.getOwnerUnitId() != null) owner.properties.add(toProperty(row));
        }
        return owners.values().stream().map(OwnerAccumulator::response).toList();
    }

    @Transactional(readOnly = true)
    public AdminOwnerSummaryResponse findSummary() {
        return mapper.findSummary();
    }

    @Transactional(readOnly = true)
    public AdminPropertyPageResponse findProperties(int requestedPage, int requestedPageSize, String keyword,
            String projectName, String rentalStatus) {
        int pageSize = Math.max(1, Math.min(requestedPageSize, 100));
        String normalizedRentalStatus = trimToNull(rentalStatus);
        if (normalizedRentalStatus != null && !Set.of("pre_handover", "pending_rental", "rented", "not_for_rent").contains(normalizedRentalStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported rental status");
        }
        String normalizedKeyword = trimToNull(keyword);
        String normalizedProject = trimToNull(projectName);
        long totalRows = mapper.countPropertyPage(normalizedKeyword, normalizedProject, normalizedRentalStatus);
        int totalPages = Math.max(1, (int) Math.ceil((double) totalRows / pageSize));
        int page = Math.max(1, Math.min(requestedPage, totalPages));
        List<AdminPropertyPageResponse.Item> rows = mapper.findPropertyPage(normalizedKeyword, normalizedProject,
                normalizedRentalStatus, pageSize, (page - 1) * pageSize).stream()
                .map(row -> new AdminPropertyPageResponse.Item(row.getOwnerId(), row.getFullName(), row.getPhone(),
                        row.getEmail(), rentalStatus(row), toProperty(row)))
                .toList();
        return new AdminPropertyPageResponse(rows,
                new AdminPropertyPageResponse.Page(totalRows, page, pageSize, totalPages));
    }

    private String rentalStatus(OwnerPropertyRow row) {
        if (PRE_HANDOVER.equals(row.getAssetStage())) return "pre_handover";
        if ("rented".equals(row.getListingStatus())) return "rented";
        if (serviceList(row.getServices()).contains("RENTAL")) return "pending_rental";
        return "not_for_rent";
    }

    @Transactional
    public AdminOwnerResponse createOwner(AdminOwnerCreateRequest request) {
        String identityNo = trimToNull(request.identityNo());
        String email = trimToNull(request.email());
        if (identityNo != null && mapper.countOwnersByIdentityNo(identityNo) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Owner identity number already exists");
        }
        if (email != null && mapper.countOwnersByEmail(email) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Owner email already exists");
        }

        NewOwner owner = new NewOwner();
        owner.setFullName(request.fullName().trim());
        owner.setIdentityNo(identityNo);
        owner.setPhone(trimToNull(request.phone()));
        owner.setEmail(email);
        owner.setStatus(request.status());
        if (accountService != null) {
            owner.setUserId(accountService.createOwnerAccount(owner.getPhone(), owner.getFullName(), owner.getEmail()));
        }
        if (mapper.insertOwner(owner) != 1 || owner.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create owner");
        }

        List<OwnerPropertyRow> rows = mapper.findOwnerById(owner.getId());
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Created owner could not be loaded");
        OwnerPropertyRow row = rows.get(0);
        return new AdminOwnerResponse(row.getOwnerId(), row.getFullName(), row.getIdentityNo(), row.getPhone(),
                row.getEmail(), row.getOwnerStatus(), List.of());
    }

    @Transactional(readOnly = true)
    public List<AdminProjectOption> findProjects() {
        return mapper.findActiveProjects();
    }

    @Transactional
    public Property createProperty(Long ownerId, AdminPropertyCreateRequest request) {
        Set<String> services = validateLifecycle(request.assetStage(), request.expectedHandoverDate(),
                request.actualHandoverDate(), request.services(), false);
        if (mapper.countActiveOwner(ownerId) != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Active owner not found");
        }
        if (mapper.countActiveProject(request.projectId()) != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active project not found");
        }

        String unitNo = request.unitNo().trim();
        if (mapper.countUnitNumberExists(request.projectId(), unitNo) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unit number already exists in this project");
        }

        NewUnit unit = new NewUnit();
        unit.setProjectId(request.projectId());
        unit.setBuilding(trimToNull(request.building()));
        unit.setFloorNo(trimToNull(request.floorNo()));
        unit.setUnitNo(unitNo);
        unit.setUnitType(trimToNull(request.unitType()));
        unit.setAreaSqm(request.areaSqm());
        unit.setBedroomCount(request.bedroomCount());
        unit.setListingStatus(request.listingStatus());
        if (mapper.insertUnit(unit) != 1 || unit.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create property unit");
        }

        NewOwnerUnit ownerUnit = new NewOwnerUnit();
        ownerUnit.setOwnerId(ownerId);
        ownerUnit.setUnitId(unit.getId());
        ownerUnit.setOwnershipPercent(request.ownershipPercent());
        ownerUnit.setPrimaryOwnership(request.primary());
        ownerUnit.setStartDate(request.startDate());
        ownerUnit.setAssetStage(request.assetStage());
        ownerUnit.setExpectedHandoverDate(request.expectedHandoverDate());
        ownerUnit.setActualHandoverDate(request.actualHandoverDate());
        if (mapper.insertOwnerUnit(ownerUnit) != 1 || ownerUnit.getId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to bind property to owner");
        }

        NewPurchaseContract contract = new NewPurchaseContract();
        contract.setOwnerUnitId(ownerUnit.getId());
        contract.setContractNo("AUTO-" + ownerId + "-" + unit.getId() + "-" + System.currentTimeMillis());
        contract.setPurchasePrice(request.purchasePrice());
        contract.setSignedDate(request.startDate());
        contract.setHandoverDate(request.actualHandoverDate());
        contract.setStatus(OPERATING.equals(request.assetStage()) ? "completed" : "active");
        if (mapper.insertPurchaseContract(contract) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unable to create purchase contract");
        }

        syncLifecycleResources(ownerUnit.getId(), request.assetStage(), request.actualHandoverDate(), services);

        return toProperty(requireProperty(ownerId, ownerUnit.getId()));
    }

    @Transactional(readOnly = true)
    public Property findProperty(Long ownerId, Long ownerUnitId) {
        OwnerPropertyRow row = requireProperty(ownerId, ownerUnitId);
        return toProperty(row);
    }

    @Transactional
    public Property updateProperty(Long ownerId, Long ownerUnitId, AdminPropertyUpdateRequest request) {
        OwnerPropertyRow current = requireProperty(ownerId, ownerUnitId);
        Set<String> services = validateLifecycle(request.assetStage(), request.expectedHandoverDate(),
                request.actualHandoverDate(), request.services(), true);
        validateTransition(current.getAssetStage(), request.assetStage());
        String unitNo = request.unitNo().trim();
        if (mapper.countUnitNumberConflicts(current.getProjectId(), unitNo, current.getUnitId()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Unit number already exists in this project");
        }

        int updated = mapper.updateUnit(
                current.getUnitId(),
                trimToNull(request.building()),
                trimToNull(request.floorNo()),
                unitNo,
                trimToNull(request.unitType()),
                request.areaSqm(),
                request.bedroomCount(),
                request.listingStatus());
        if (updated != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Property was changed by another request");

        if (mapper.updateOwnerUnitLifecycle(ownerUnitId, request.assetStage(), request.expectedHandoverDate(),
                request.actualHandoverDate()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Property lifecycle was changed by another request");
        }

        BigDecimal currentPrice = zeroIfNull(current.getPurchasePrice());
        if (currentPrice.compareTo(request.purchasePrice()) != 0
                && mapper.updateActivePurchasePrice(ownerUnitId, request.purchasePrice()) == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Purchase contract not found");
        }
        syncLifecycleResources(ownerUnitId, request.assetStage(), request.actualHandoverDate(), services);
        return toProperty(requireProperty(ownerId, ownerUnitId));
    }

    private OwnerPropertyRow requireProperty(Long ownerId, Long ownerUnitId) {
        OwnerPropertyRow row = mapper.findOwnerProperty(ownerId, ownerUnitId);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner property not found");
        return row;
    }

    private Property toProperty(OwnerPropertyRow row) {
        return new Property(
                row.getOwnerUnitId(), row.getUnitId(), row.getProjectId(), row.getProjectName(), row.getAddress(),
                row.getCity(), row.getBuilding(), row.getFloorNo(), row.getUnitNo(), row.getUnitType(), row.getAreaSqm(),
                row.getBedroomCount(), row.getListingStatus(), row.getAssetStage(), row.getExpectedHandoverDate(),
                row.getActualHandoverDate(), serviceList(row.getServices()), row.getOwnershipPercent(),
                Boolean.TRUE.equals(row.getPrimaryOwnership()), row.getStartDate(), row.getEndDate(),
                zeroIfNull(row.getPurchasePrice()), zeroIfNull(row.getPaidAmount()), zeroIfNull(row.getRemainingAmount()),
                row.getPaymentStatus());
    }

    private Set<String> validateLifecycle(String assetStage, LocalDate expectedHandoverDate,
            LocalDate actualHandoverDate, List<String> requestedServices, boolean allowDisposed) {
        if (!PRE_HANDOVER.equals(assetStage) && !OPERATING.equals(assetStage)
                && !(allowDisposed && DISPOSED.equals(assetStage))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported property lifecycle stage");
        }
        Set<String> services = new LinkedHashSet<>(requestedServices == null ? List.of() : requestedServices);
        if (!SERVICE_TYPES.containsAll(services)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported property service");
        }
        if (PRE_HANDOVER.equals(assetStage)) {
            if (actualHandoverDate != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Pre-handover property cannot have an actual handover date");
            }
            if (!services.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Property services are only available after handover");
            }
        }
        if (OPERATING.equals(assetStage) && actualHandoverDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Operating property requires an actual handover date");
        }
        if (DISPOSED.equals(assetStage) && !services.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Disposed property cannot have active services");
        }
        return services;
    }

    private void validateTransition(String currentStage, String targetStage) {
        if (DISPOSED.equals(currentStage) && !DISPOSED.equals(targetStage)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Disposed property cannot be reactivated");
        }
        if (OPERATING.equals(currentStage) && PRE_HANDOVER.equals(targetStage)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Operating property cannot return to pre-handover stage");
        }
    }

    private void syncLifecycleResources(Long ownerUnitId, String assetStage, LocalDate actualHandoverDate,
            Set<String> services) {
        mapper.endOwnerUnitServices(ownerUnitId);
        if (OPERATING.equals(assetStage)) {
            services.forEach(service -> mapper.activateOwnerUnitService(ownerUnitId, service));
            mapper.ensureReserveAccount(ownerUnitId);
            mapper.updateReserveStatus(ownerUnitId, "active");
            mapper.markActiveInstallmentsPaid(ownerUnitId);
            mapper.archiveActivePaymentPlans(ownerUnitId);
            mapper.completePurchaseContracts(ownerUnitId, actualHandoverDate);
        } else {
            mapper.updateReserveStatus(ownerUnitId, "inactive");
        }
    }

    private List<String> serviceList(String services) {
        if (services == null || services.isBlank()) return List.of();
        return List.of(services.split(","));
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class OwnerAccumulator {
        private final OwnerPropertyRow owner;
        private final List<Property> properties = new ArrayList<>();

        private OwnerAccumulator(OwnerPropertyRow owner) {
            this.owner = owner;
        }

        private AdminOwnerResponse response() {
            return new AdminOwnerResponse(owner.getOwnerId(), owner.getFullName(), owner.getIdentityNo(), owner.getPhone(),
                    owner.getEmail(), owner.getOwnerStatus(), List.copyOf(properties));
        }
    }
}
