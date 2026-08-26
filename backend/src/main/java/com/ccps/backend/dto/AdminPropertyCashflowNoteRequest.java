package com.ccps.backend.dto;

public record AdminPropertyCashflowNoteRequest(String note, Boolean reuse) {
    public boolean reuseEnabled() {
        return Boolean.TRUE.equals(reuse);
    }
}
