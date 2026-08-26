package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminLeasePaymentResponse(
        Long paymentId,Long financeRecordId,Long invoiceId,Long leaseId,String leaseNo,
        LocalDate billingMonth,String transactionNo,BigDecimal amount,LocalDate paymentDate,
        LocalDate receivedDate,LocalDate postingDate,
        String paymentMethod,String payerName,String paymentReference,String note,
        Long proofDocumentId,String proofName,String syncStatus,LocalDateTime createdAt) {}
