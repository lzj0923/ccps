package com.ccps.backend.service;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.OwnerDashboardResponse;
import com.ccps.backend.dto.OwnerDashboardResponse.PendingItem;
import com.ccps.backend.dto.OwnerDashboardResponse.Property;
import com.ccps.backend.dto.OwnerDashboardResponse.Summary;
import com.ccps.backend.dto.OwnerPropertyServicesResponse;
import com.ccps.backend.dto.OwnerPropertyServicesUpdateRequest;
import com.ccps.backend.mapper.OwnerDashboardMapper;

@Service
public class OwnerDashboardService {
    private static final Set<String> SERVICE_TYPES = Set.of("RENTAL", "RESALE", "MANAGEMENT");
    private final OwnerDashboardMapper mapper;

    public OwnerDashboardService(OwnerDashboardMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public OwnerDashboardResponse getDashboard(Long userId) {
        List<Property> properties = mapper.findPropertiesByUserId(userId);
        BigDecimal unpaidAmount = properties.stream()
                .filter(property -> "PRE_HANDOVER".equals(property.getAssetStage()))
                .map(Property::getRemainingAmount)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tenantDepositAmount = properties.stream()
                .map(Property::getTenantDepositAmount)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int pendingMaintenanceCount = properties.stream()
                .filter(property -> "OPERATING".equals(property.getAssetStage()))
                .map(Property::getPendingMaintenanceCount)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .sum();

        Summary summary = new Summary(
                properties.size(),
                zeroIfNull(mapper.findMonthlyRentIncome(userId)),
                unpaidAmount,
                tenantDepositAmount,
                zeroIfNull(mapper.findReserveBalance(userId)),
                pendingMaintenanceCount);

        List<PendingItem> pendingItems = List.of(
                pendingItem("payment_proof", "待上傳繳費憑證", mapper.countMissingPaymentProofs(userId)),
                pendingItem("rent_confirmation", "租金收款確認", mapper.countPendingRentConfirmations(userId)),
                pendingItem("document_signature", "合約／文件待簽署", mapper.countPendingDocuments(userId)));

        return new OwnerDashboardResponse(
                summary,
                properties,
                mapper.findRecentNotifications(userId),
                pendingItems);
    }

    @Transactional
    public OwnerPropertyServicesResponse updatePropertyServices(Long userId, Long ownerUnitId,
            OwnerPropertyServicesUpdateRequest request) {
        String assetStage = mapper.findOwnedPropertyStage(userId, ownerUnitId);
        if (assetStage == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner property not found");
        }
        if (!"OPERATING".equals(assetStage)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Property services can only be changed for operating properties");
        }

        LinkedHashSet<String> services = new LinkedHashSet<>(request.services());
        if (!SERVICE_TYPES.containsAll(services)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported property service");
        }
        mapper.endOwnedPropertyServices(ownerUnitId);
        services.forEach(service -> mapper.activateOwnedPropertyService(ownerUnitId, service));
        return new OwnerPropertyServicesResponse(ownerUnitId, List.copyOf(services));
    }

    private PendingItem pendingItem(String type, String title, int count) {
        String detail = count == 0 ? "目前沒有待處理項目" : "有 " + count + " 筆項目需要處理";
        return new PendingItem(type, title, detail, count);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
