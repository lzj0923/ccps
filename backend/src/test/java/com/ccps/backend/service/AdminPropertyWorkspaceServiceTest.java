package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.dto.AdminOwnerResponse;
import com.ccps.backend.dto.AdminOwnerResponse.Property;
import com.ccps.backend.dto.AdminPropertyLeaseOptionResponse;
import com.ccps.backend.dto.AdminPropertyOwnershipResponse;
import com.ccps.backend.service.AdminOwnerService.PropertyReference;

@ExtendWith(MockitoExtension.class)
class AdminPropertyWorkspaceServiceTest {
    @Mock private AdminOwnerService ownerService;
    @Mock private AdminPropertyOwnershipService ownershipService;
    @Mock private AdminPropertyContractRecordService contractService;
    private AdminPropertyWorkspaceService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-21T00:00:00Z"), ZoneOffset.UTC);
        service = new AdminPropertyWorkspaceService(ownerService, ownershipService, contractService, clock);
    }

    @Test
    void assemblesAuthoritativeModulesAndDetectsMissingLeaseContract() {
        Property property = new Property(12L, 120L, 1L, "吉隆坡中環閣", "Address", "KL", null,
                null, "E-09-03", "一房", new BigDecimal("52.6"), 1, "available", "OPERATING",
                null, LocalDate.of(2024, 6, 18), List.of("RENTAL"), new BigDecimal("100"), true,
                LocalDate.of(2024, 6, 18), null, new BigDecimal("680000"), new BigDecimal("680000"),
                BigDecimal.ZERO, "not_applicable", null, null, null, null, null, null, null);
        AdminOwnerResponse owner = new AdminOwnerResponse(7L, "000397", "陳偉明", null, "+60123",
                "+60123", null, null, null, "owner@example.com", "Owner address", "active", List.of(property));
        AdminPropertyOwnershipResponse ownership = new AdminPropertyOwnershipResponse(1L, 120L, 7L,
                "000397", "陳偉明", null, "+60123", "owner@example.com", new BigDecimal("100"), true,
                LocalDate.of(2024, 6, 18), null, "active");
        AdminPropertyLeaseOptionResponse lease = new AdminPropertyLeaseOptionResponse(88L, null, "LS-88", 9L,
                "租客甲", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30), new BigDecimal("2600"),
                new BigDecimal("5200"), 5, "active", false, "not_generated");

        when(ownerService.findPropertyReference(120L)).thenReturn(new PropertyReference(7L, 12L));
        when(ownerService.findProperty(7L, 12L)).thenReturn(property);
        when(ownerService.findOwner(7L)).thenReturn(owner);
        when(ownerService.findPropertyBasicProfile(7L, 12L)).thenReturn(Map.of("countryCode", "MY"));
        when(ownershipService.list(120L)).thenReturn(List.of(ownership));
        when(contractService.list(7L, 12L)).thenReturn(List.of());
        when(contractService.leaseOptions(7L, 12L)).thenReturn(List.of(lease));

        var result = service.loadByUnitId(120L);

        assertThat(result.property()).isSameAs(property);
        assertThat(result.intelligence().effectiveRentalStatus()).isEqualTo("rented");
        assertThat(result.intelligence().effectiveLeaseId()).isEqualTo(88L);
        assertThat(result.intelligence().ownershipComplete()).isTrue();
        assertThat(result.intelligence().leaseContractMissing()).isTrue();
        assertThat(result.intelligence().warnings()).anyMatch(value -> value.contains("L.租賃合約"));
    }
}
