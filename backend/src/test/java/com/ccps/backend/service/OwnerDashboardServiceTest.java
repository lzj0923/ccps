package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.dto.OwnerDashboardResponse;
import com.ccps.backend.dto.OwnerDashboardResponse.Property;
import com.ccps.backend.dto.OwnerPropertyServicesResponse;
import com.ccps.backend.dto.OwnerPropertyServicesUpdateRequest;
import com.ccps.backend.mapper.OwnerDashboardMapper;

@ExtendWith(MockitoExtension.class)
class OwnerDashboardServiceTest {
    @Mock
    private OwnerDashboardMapper mapper;

    private OwnerDashboardService service;

    @BeforeEach
    void setUp() {
        service = new OwnerDashboardService(mapper);
    }

    @Test
    void buildsSummaryAndPendingItemsFromOwnerScopedQueries() {
        Long userId = 42L;
        Property first = property(1L, "350000.00");
        Property second = property(2L, "125000.50");
        Property operating = property(3L, "0");
        operating.setAssetStage("OPERATING");
        operating.setTenantDepositAmount(new BigDecimal("13750.00"));
        operating.setPendingMaintenanceCount(3);

        when(mapper.findPropertiesByUserId(userId)).thenReturn(List.of(first, second, operating));
        when(mapper.findMonthlyRentIncome(userId)).thenReturn(new BigDecimal("8450.00"));
        when(mapper.findReserveBalance(userId)).thenReturn(new BigDecimal("12860.50"));
        when(mapper.findRecentNotifications(userId)).thenReturn(List.of());
        when(mapper.countMissingPaymentProofs(userId)).thenReturn(2);
        when(mapper.countPendingRentConfirmations(userId)).thenReturn(1);
        when(mapper.countPendingDocuments(userId)).thenReturn(0);

        OwnerDashboardResponse result = service.getDashboard(userId);

        assertThat(result.summary().propertyCount()).isEqualTo(3);
        assertThat(result.summary().monthlyRentIncome()).isEqualByComparingTo("8450.00");
        assertThat(result.summary().unpaidPropertyAmount()).isEqualByComparingTo("475000.50");
        assertThat(result.summary().tenantDepositAmount()).isEqualByComparingTo("13750.00");
        assertThat(result.summary().reserveBalance()).isEqualByComparingTo("12860.50");
        assertThat(result.summary().pendingMaintenanceCount()).isEqualTo(3);
        assertThat(result.pendingItems()).extracting(OwnerDashboardResponse.PendingItem::count)
                .containsExactly(2, 1, 0);
        assertThat(result.pendingItems().get(2).detail()).isEqualTo("目前沒有待處理項目");
    }

    @Test
    void updatesMultipleServicesForAnOwnedOperatingProperty() {
        Long userId = 42L;
        Long ownerUnitId = 18L;
        when(mapper.findOwnedPropertyStage(userId, ownerUnitId)).thenReturn("OPERATING");

        OwnerPropertyServicesResponse result = service.updatePropertyServices(userId, ownerUnitId,
                new OwnerPropertyServicesUpdateRequest(List.of("RENTAL", "MANAGEMENT")));

        verify(mapper).endOwnedPropertyServices(ownerUnitId);
        verify(mapper).activateOwnedPropertyService(ownerUnitId, "RENTAL");
        verify(mapper).activateOwnedPropertyService(ownerUnitId, "MANAGEMENT");
        assertThat(result.ownerUnitId()).isEqualTo(ownerUnitId);
        assertThat(result.services()).containsExactly("RENTAL", "MANAGEMENT");
    }

    @Test
    void refusesServiceChangesForPreHandoverProperty() {
        Long userId = 42L;
        Long ownerUnitId = 19L;
        when(mapper.findOwnedPropertyStage(userId, ownerUnitId)).thenReturn("PRE_HANDOVER");

        assertThatThrownBy(() -> service.updatePropertyServices(userId, ownerUnitId,
                new OwnerPropertyServicesUpdateRequest(List.of("RESALE"))))
                .hasMessageContaining("only be changed for operating properties");
    }

    private Property property(Long id, String remainingAmount) {
        Property property = new Property();
        property.setOwnerUnitId(id);
        property.setAssetStage("PRE_HANDOVER");
        property.setRemainingAmount(new BigDecimal(remainingAmount));
        return property;
    }
}
