package com.ccps.backend.dto;

public record RentReceiptConfirmationResponse(Long invoiceId, int confirmedPaymentCount) {
}
