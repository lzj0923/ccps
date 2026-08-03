package com.ccps.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdminLeaseRentInvoiceResponse(Long invoiceId, LocalDate billingMonth, LocalDate dueDate,
        BigDecimal amountDue, BigDecimal amountPaid, BigDecimal amountUnpaid, String rentStatus) { }
