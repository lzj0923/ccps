package com.ccps.backend.dto;

public record AdminRecordCreateResponse(Long id, String referenceNo, String receiptNo) {
    public AdminRecordCreateResponse(Long id, String referenceNo) {
        this(id, referenceNo, null);
    }
}
