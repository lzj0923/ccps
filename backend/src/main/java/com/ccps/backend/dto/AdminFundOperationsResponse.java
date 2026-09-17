package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminFundOperationsResponse(
        List<ReserveAccountOption> reserveAccounts,
        List<InternalTransfer> internalTransfers,
        List<RemittanceSetting> remittanceSettings,
        List<RemittanceBatch> remittanceBatches) {

    public record ReserveAccountOption(Long id, Long ownerId, Long ownerUnitId, String ownerName,
            String projectName, String unitNo, BigDecimal currentBalance, BigDecimal minimumBalance,
            Long defaultBankAccountId, String defaultBankName, String defaultBankAccountNo) { }

    public record InternalTransfer(Long id, String transferNo, Long sourceReserveAccountId,
            Long targetReserveAccountId, String ownerName, String sourceProperty, String targetProperty,
            BigDecimal amount, LocalDate requestedDate, String reason, String status, String reviewNote,
            String createdByName, String reviewedByName, LocalDateTime reviewedAt, LocalDateTime reversedAt,
            LocalDateTime createdAt) { }

    public record RemittanceSetting(Long id, Long reserveAccountId, String ownerName, String propertyName,
            String cycle, LocalDate nextRemittanceDate, Boolean enabled, Boolean holdEnabled, String holdReason,
            LocalDate holdUntil, BigDecimal retainedAmount, BigDecimal taxRetainedAmount,
            Long defaultBankAccountId, String bankName, String bankAccountNo) { }

    public record RemittanceBatch(Long id, String batchNo, LocalDate scheduledDate, String status,
            BigDecimal totalAmount, Integer itemCount, String note, String createdByName, String reviewedByName,
            LocalDateTime reviewedAt, LocalDateTime createdAt, List<RemittanceItem> items) { }

    public record RemittanceItem(Long id, Long reserveAccountId, String ownerName, String propertyName,
            String bankName, String bankAccountNo, BigDecimal amount, BigDecimal retainedAmount,
            BigDecimal taxRetainedAmount, Long financeRecordId, String transactionNo,
            String paymentStatus, String confirmationStatus) { }
}
