package com.ccps.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class TenancyAgreementWordServiceTest {
    @Test
    void dormantLegacyWordGeneratorFailsFastAndPointsToTheActivePdfService() {
        TenancyAgreementWordService service = new TenancyAgreementWordService();

        assertFalse(service.isTemplateAvailable());
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.generate(Map.of("caseNo", "LS-2026-001")));
        assertTrue(error.getMessage().contains("current PDF tenancy agreement service"));
    }
}
