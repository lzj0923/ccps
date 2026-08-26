package com.ccps.backend.service;

import com.ccps.backend.mapper.AdminRentalMandateDocumentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRentalMandateDocumentServiceTest {
    @Mock AdminRentalMandateDocumentMapper mapper;
    @TempDir Path tempDir;
    AdminRentalMandateDocumentService service;

    @BeforeEach void setUp() {
        service = new AdminRentalMandateDocumentService(mapper,
                tempDir.resolve("mandates").toString(), tempDir.resolve("signatures").toString());
    }

    @Test void regeneratesWhileDocumentSigningIsStillActiveAndCancelsOldRequest() {
        when(mapper.findMandate(25L)).thenReturn(25L);
        when(mapper.findDocuments(25L)).thenReturn(List.of());

        var result = service.upload(1L, 25L, "management_authorization_draft",
                new MockMultipartFile("file", "authorization.pdf", "application/pdf", "pdf".getBytes()));

        assertThat(result).isEmpty();
        verify(mapper).cancelPendingSignaturesForRelation(25L, "management_authorization_draft");
        verify(mapper).supersedeCurrent(25L, "management_authorization_draft");
        verify(mapper).insertDocument(any());
    }

    @Test void regeneratesAfterSigningCompletedWithoutSupersedingSignedHistory() {
        when(mapper.findMandate(25L)).thenReturn(25L);
        when(mapper.findDocuments(25L)).thenReturn(List.of());

        var result = service.upload(1L, 25L, "property_management_agreement_draft",
                new MockMultipartFile("file", "pma.pdf", "application/pdf", "pdf".getBytes()));

        assertThat(result).isEmpty();
        verify(mapper).cancelPendingSignaturesForRelation(25L, "property_management_agreement_draft");
        verify(mapper).supersedeCurrent(25L, "property_management_agreement_draft");
        verify(mapper, never()).supersedeSignedPackage(25L, "property_management_agreement_draft");
        verify(mapper).insertDocument(any());
    }
}
