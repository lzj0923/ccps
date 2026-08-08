package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminTenantDetailResponse(Overview overview, List<Lease> leases,
        List<AdminTenantDepositTransactionResponse> depositTransactions) {
    public record Overview(BigDecimal unpaidRent, long pendingMaintenanceCount, long pendingSignatureCount,
            BigDecimal depositBalance) { }
    public record Lease(Long leaseId, Long unitId, String leaseNo, String projectName, String unitNo, String status,
            LocalDate startDate, LocalDate endDate, BigDecimal monthlyRent, BigDecimal depositAmount,
            Integer paymentDay, BigDecimal unpaidRent, long pendingMaintenanceCount,
            String signatureStatus, String workflowStep) { }
}
