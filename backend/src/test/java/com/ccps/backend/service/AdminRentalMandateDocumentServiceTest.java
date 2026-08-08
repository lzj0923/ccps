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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test void rejectsRegenerationAfterDocumentSigningHasStarted() {
        when(mapper.findMandate(25L)).thenReturn(25L);
        when(mapper.countStartedSignaturesForRelation(25L, "management_authorization_draft")).thenReturn(1);

        assertThatThrownBy(() -> service.upload(1L, 25L, "management_authorization_draft",
                new MockMultipartFile("file", "authorization.pdf", "application/pdf", "pdf".getBytes())))
                .hasMessageContaining("cannot be regenerated after signing has started");

        verify(mapper, never()).insertDocument(any());
    }
}
