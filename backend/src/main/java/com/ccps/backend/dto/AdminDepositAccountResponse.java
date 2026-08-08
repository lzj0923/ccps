package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminDepositAccountResponse(Summary summary, List<Item> rows, Page page) {
    public record Summary(long totalAccounts, long pendingCollectionCount, long activeCount,
            long awaitingSettlementCount, long settlingCount, long settledCount, BigDecimal totalHeld) { }

    public record Item(Long leaseId, String leaseNo, Long tenantId, String tenantName, String tenantPhone,
            String projectName, String unitNo, String leaseStatus, LocalDate startDate, LocalDate endDate,
            BigDecimal expectedDeposit, BigDecimal postedBalance, BigDecimal availableBalance,
            String collectionStatus, String accountStatus) { }

    public record Page(long totalRows, int page, int pageSize, int totalPages) { }
}
