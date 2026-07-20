package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminOwnerResponse;
import com.ccps.backend.dto.AdminOwnerCreateRequest;
import com.ccps.backend.dto.AdminPropertyCreateRequest;
import com.ccps.backend.dto.AdminPropertyUpdateRequest;
import com.ccps.backend.mapper.AdminOwnerMapper;
import com.ccps.backend.mapper.AdminOwnerMapper.OwnerPropertyRow;
import com.ccps.backend.mapper.AdminOwnerMapper.NewOwner;
import com.ccps.backend.mapper.AdminOwnerMapper.NewOwnerUnit;
import com.ccps.backend.mapper.AdminOwnerMapper.NewPurchaseContract;
import com.ccps.backend.mapper.AdminOwnerMapper.NewUnit;

@ExtendWith(MockitoExtension.class)
class AdminOwnerServiceTest {
    @Mock private AdminOwnerMapper mapper;
    private AdminOwnerService service;

    @BeforeEach
    void setUp() {
        service = new AdminOwnerService(mapper);
    }

    @Test
    void groupsAllPropertiesUnderTheirOwner() {
        OwnerPropertyRow first = propertyRow(1L, 11L, 101L, "A-01");
        OwnerPropertyRow second = propertyRow(1L, 12L, 102L, "A-02");
        when(mapper.findOwnersWithProperties()).thenReturn(List.of(first, second));

        List<AdminOwnerResponse> result = service.findOwners();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).fullName()).isEqualTo("Test Owner");
        assertThat(result.get(0).properties()).extracting(AdminOwnerResponse.Property::unitNo)
                .containsExactly("A-01", "A-02");
    }

    @Test
    void createsOwnerAndReturnsGeneratedDatabaseRecord() {
        AdminOwnerCreateRequest request = new AdminOwnerCreateRequest(
                " New Owner ", "ID-900", "+60 12-000 0000", "new.owner@example.com", "active");
        when(mapper.insertOwner(any(NewOwner.class))).thenAnswer(invocation -> {
            NewOwner owner = invocation.getArgument(0);
            owner.setId(99L);
            return 1;
        });
        OwnerPropertyRow created = propertyRow(99L, null, null, null);
        created.setFullName("New Owner");
        created.setIdentityNo("ID-900");
        created.setPhone("+60 12-000 0000");
        created.setEmail("new.owner@example.com");
        when(mapper.findOwnerById(99L)).thenReturn(List.of(created));

        AdminOwnerResponse result = service.createOwner(request);

        assertThat(result.id()).isEqualTo(99L);
        assertThat(result.fullName()).isEqualTo("New Owner");
        assertThat(result.properties()).isEmpty();
    }

    @Test
    void rejectsDuplicateOwnerIdentityNumber() {
        AdminOwnerCreateRequest request = new AdminOwnerCreateRequest(
                "New Owner", "ID-900", null, null, "active");
        when(mapper.countOwnersByIdentityNo("ID-900")).thenReturn(1);

        assertThatThrownBy(() -> service.createOwner(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("identity number already exists");
    }

    @Test
    void createsPropertyAndBindsItToTheSelectedOwner() {
        LocalDate startDate = LocalDate.of(2026, 7, 17);
        AdminPropertyCreateRequest request = new AdminPropertyCreateRequest(
                7L, "Tower A", "12", "A-12-08", "2BR", new BigDecimal("88.50"), 2,
                "available", "PRE_HANDOVER", LocalDate.of(2027, 6, 30), null, List.of(),
                new BigDecimal("900000.00"), new BigDecimal("100.00"), true, startDate);
        when(mapper.countActiveOwner(1L)).thenReturn(1);
        when(mapper.countActiveProject(7L)).thenReturn(1);
        when(mapper.insertUnit(any(NewUnit.class))).thenAnswer(invocation -> {
            NewUnit unit = invocation.getArgument(0);
            unit.setId(101L);
            return 1;
        });
        when(mapper.insertOwnerUnit(any(NewOwnerUnit.class))).thenAnswer(invocation -> {
            NewOwnerUnit ownerUnit = invocation.getArgument(0);
            ownerUnit.setId(11L);
            return 1;
        });
        when(mapper.insertPurchaseContract(any(NewPurchaseContract.class))).thenReturn(1);
        OwnerPropertyRow created = propertyRow(1L, 11L, 101L, "A-12-08");
        created.setPurchasePrice(new BigDecimal("900000.00"));
        when(mapper.findOwnerProperty(1L, 11L)).thenReturn(created);

        AdminOwnerResponse.Property result = service.createProperty(1L, request);

        assertThat(result.ownerUnitId()).isEqualTo(11L);
        assertThat(result.unitNo()).isEqualTo("A-12-08");
        verify(mapper).insertUnit(any(NewUnit.class));
        verify(mapper).insertOwnerUnit(any(NewOwnerUnit.class));
        verify(mapper).insertPurchaseContract(any(NewPurchaseContract.class));
    }

    @Test
    void rejectsCreatingPropertyBeforeSelectingAnActiveOwner() {
        AdminPropertyCreateRequest request = new AdminPropertyCreateRequest(
                7L, null, null, "A-12-08", null, null, null, "available",
                "PRE_HANDOVER", null, null, List.of(), BigDecimal.ZERO,
                new BigDecimal("100.00"), true, null);
        when(mapper.countActiveOwner(99L)).thenReturn(0);

        assertThatThrownBy(() -> service.createProperty(99L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Active owner not found");
    }

    @Test
    void updatesUnitAndActivePurchaseContract() {
        OwnerPropertyRow before = propertyRow(1L, 11L, 101L, "A-01");
        OwnerPropertyRow after = propertyRow(1L, 11L, 101L, "A-01A");
        after.setPurchasePrice(new BigDecimal("1200000.00"));
        when(mapper.findOwnerProperty(1L, 11L)).thenReturn(before, after);
        when(mapper.countUnitNumberConflicts(7L, "A-01A", 101L)).thenReturn(0);
        when(mapper.updateUnit(101L, "Tower A", "10", "A-01A", "Condo", new BigDecimal("88.50"), 3, "sold"))
                .thenReturn(1);
        when(mapper.updateActivePurchasePrice(11L, new BigDecimal("1200000.00"))).thenReturn(1);
        when(mapper.updateOwnerUnitLifecycle(11L, "PRE_HANDOVER", LocalDate.of(2027, 6, 30), null)).thenReturn(1);

        AdminPropertyUpdateRequest request = new AdminPropertyUpdateRequest(
                "Tower A", "10", "A-01A", "Condo", new BigDecimal("88.50"), 3, "sold",
                "PRE_HANDOVER", LocalDate.of(2027, 6, 30), null, List.of(),
                new BigDecimal("1200000.00"));

        AdminOwnerResponse.Property result = service.updateProperty(1L, 11L, request);

        assertThat(result.unitNo()).isEqualTo("A-01A");
        assertThat(result.purchasePrice()).isEqualByComparingTo("1200000.00");
        verify(mapper).updateActivePurchasePrice(11L, new BigDecimal("1200000.00"));
    }

    @Test
    void rejectsDuplicateUnitNumberInTheSameProject() {
        OwnerPropertyRow before = propertyRow(1L, 11L, 101L, "A-01");
        when(mapper.findOwnerProperty(1L, 11L)).thenReturn(before);
        when(mapper.countUnitNumberConflicts(7L, "A-02", 101L)).thenReturn(1);

        AdminPropertyUpdateRequest request = new AdminPropertyUpdateRequest(
                null, null, "A-02", null, null, null, "available", "PRE_HANDOVER", null, null,
                List.of(), new BigDecimal("1000000.00"));

        assertThatThrownBy(() -> service.updateProperty(1L, 11L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unit number already exists");
    }

    @Test
    void handoverActivatesSelectedServicesAndOperatingResources() {
        LocalDate handoverDate = LocalDate.of(2026, 7, 18);
        OwnerPropertyRow before = propertyRow(1L, 11L, 101L, "A-01");
        OwnerPropertyRow after = propertyRow(1L, 11L, 101L, "A-01");
        after.setAssetStage("OPERATING");
        after.setActualHandoverDate(handoverDate);
        after.setServices("MANAGEMENT,RENTAL");
        after.setPaymentStatus("not_applicable");
        when(mapper.findOwnerProperty(1L, 11L)).thenReturn(before, after);
        when(mapper.updateUnit(101L, null, null, "A-01", null, null, null, "available")).thenReturn(1);
        when(mapper.updateOwnerUnitLifecycle(11L, "OPERATING", null, handoverDate)).thenReturn(1);

        AdminPropertyUpdateRequest request = new AdminPropertyUpdateRequest(
                null, null, "A-01", null, null, null, "available", "OPERATING", null,
                handoverDate, List.of("RENTAL", "MANAGEMENT"), new BigDecimal("1000000.00"));

        AdminOwnerResponse.Property result = service.updateProperty(1L, 11L, request);

        assertThat(result.assetStage()).isEqualTo("OPERATING");
        assertThat(result.services()).containsExactly("MANAGEMENT", "RENTAL");
        verify(mapper).activateOwnerUnitService(11L, "RENTAL");
        verify(mapper).activateOwnerUnitService(11L, "MANAGEMENT");
        verify(mapper).ensureReserveAccount(11L);
        verify(mapper).archiveActivePaymentPlans(11L);
        verify(mapper).completePurchaseContracts(11L, handoverDate);
    }

    @Test
    void rejectsOperatingServicesBeforeHandover() {
        AdminPropertyCreateRequest request = new AdminPropertyCreateRequest(
                7L, null, null, "A-09", null, null, null, "available", "PRE_HANDOVER",
                null, null, List.of("RENTAL"), BigDecimal.ZERO, new BigDecimal("100.00"), true, null);

        assertThatThrownBy(() -> service.createProperty(1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("only available after handover");
    }

    private OwnerPropertyRow propertyRow(Long ownerId, Long ownerUnitId, Long unitId, String unitNo) {
        OwnerPropertyRow row = new OwnerPropertyRow();
        row.setOwnerId(ownerId);
        row.setFullName("Test Owner");
        row.setOwnerStatus("active");
        row.setOwnerUnitId(ownerUnitId);
        row.setUnitId(unitId);
        row.setProjectId(unitId == null ? null : 7L);
        row.setProjectName("Pavilion Square");
        row.setUnitNo(unitNo);
        row.setListingStatus(unitId == null ? null : "available");
        row.setOwnershipPercent(new BigDecimal("100.00"));
        row.setPrimaryOwnership(true);
        row.setPurchasePrice(new BigDecimal("1000000.00"));
        row.setPaidAmount(new BigDecimal("600000.00"));
        row.setRemainingAmount(new BigDecimal("400000.00"));
        row.setPaymentStatus("paying");
        row.setAssetStage("PRE_HANDOVER");
        return row;
    }
}
