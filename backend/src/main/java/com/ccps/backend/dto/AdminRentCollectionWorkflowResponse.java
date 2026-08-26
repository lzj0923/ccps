package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminRentCollectionWorkflowResponse(Summary summary, List<Item> items) {
    public record Summary(long trackingCount, long waitingCount, long firstReminderCount,
                          long secondReminderCount, long finalReminderCount,
                          long terminationCount, long onHoldCount, BigDecimal outstandingAmount) {
    }

    public record Item(Long invoiceId, Long leaseId, Long tenantId, String tenantName,
                       String tenantPhone, String tenantEmail, boolean whatsappEnabled,
                       String whatsappDestination, String projectName, String unitNo,
                       String leaseNo, LocalDate billingMonth, LocalDate dueDate,
                       BigDecimal amountDue, BigDecimal amountPaid, BigDecimal outstandingAmount,
                       long overdueDays, String workflowStatus, String holdReason,
                       String currentStage, String currentStageLabel, LocalDate scheduledDate,
                       LocalDate nextActionDate, List<String> sentStages,
                       String lastSentStage, LocalDateTime lastSentAt) {
    }
}
