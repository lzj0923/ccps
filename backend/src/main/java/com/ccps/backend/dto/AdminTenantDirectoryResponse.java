package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminTenantDirectoryResponse(Summary summary, List<Item> rows, Page page) {
    public record Summary(long totalCount, long activeCount, long inactiveCount, long activeLeaseTenantCount) { }

    public record Item(Long tenantId, String fullName, String identityNo, String phone, String email,
            String status, String currentLeaseNo, String projectName, String unitNo,
            LocalDate leaseStart, LocalDate leaseEnd, BigDecimal currentDepositAmount, BigDecimal currentDepositBalance,
            String currentDepositStatus,
            long leaseCount, long activeLeaseCount) { }

    public record Page(long totalRows, int page, int pageSize, int totalPages) { }
}
