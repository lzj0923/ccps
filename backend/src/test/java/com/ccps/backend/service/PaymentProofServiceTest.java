package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.PaymentProofSubmissionResponse;
import com.ccps.backend.mapper.PaymentProofMapper;
import com.ccps.backend.mapper.PaymentProofMapper.NewDocument;
import com.ccps.backend.mapper.PaymentProofMapper.NewFinanceRecord;
import com.ccps.backend.mapper.PaymentProofMapper.NewReceipt;
import com.ccps.backend.mapper.PaymentProofMapper.SubmissionContext;
import com.ccps.backend.service.PaymentProofService.Submission;

@ExtendWith(MockitoExtension.class)
class PaymentProofServiceTest {
    @Mock
    private PaymentProofMapper mapper;

    @TempDir
    Path storage;

    private PaymentProofService service;
    private SubmissionContext context;

    @BeforeEach
    void setUp() {
        service = new PaymentProofService(mapper, storage.toString());
        context = new SubmissionContext();
        context.setOwnerUnitId(5L);
        context.setUnitId(15L);
        context.setOwnerId(25L);
        context.setCurrency("MYR");
        context.setAmountDue(new BigDecimal("120000.00"));
        context.setAmountPaid(BigDecimal.ZERO);
        context.setPendingAmount(BigDecimal.ZERO);
    }

    @Test
    void storesFilesAndCreatesPendingReceiptWithoutChangingPaidAmount() throws Exception {
        when(mapper.findSubmissionContext(42L, 5L, 2L)).thenReturn(context);
        when(mapper.insertFinanceRecord(any())).thenAnswer(invocation -> {
            invocation.<NewFinanceRecord>getArgument(0).setId(100L);
            return 1;
        });
        AtomicLong documentIds = new AtomicLong(200L);
        when(mapper.insertDocument(any())).thenAnswer(invocation -> {
            invocation.<NewDocument>getArgument(0).setId(documentIds.getAndIncrement());
            return 1;
        });
        when(mapper.insertReceipt(any())).thenAnswer(invocation -> {
            invocation.<NewReceipt>getArgument(0).setId(300L);
            return 1;
        });
        MockMultipartFile file = new MockMultipartFile("files", "receipt.png", "image/png", "proof".getBytes());

        PaymentProofSubmissionResponse result = service.submit(42L, 5L, submission("120000.00"), List.of(file));

        assertThat(result.receiptId()).isEqualTo(300L);
        assertThat(result.confirmationStatus()).isEqualTo("pending");
        assertThat(result.files()).hasSize(1);
        assertThat(Files.walk(storage).filter(Files::isRegularFile).count()).isEqualTo(1);
        verify(mapper).insertAllocation(300L, 2L, new BigDecimal("120000.00"));
        verify(mapper).insertDocumentLink(200L, 100L);
    }

    @Test
    void rejectsAmountAlreadyCoveredByAnotherPendingSubmission() {
        context.setPendingAmount(new BigDecimal("80000.00"));
        when(mapper.findSubmissionContext(42L, 5L, 2L)).thenReturn(context);
        MockMultipartFile file = new MockMultipartFile("files", "receipt.pdf", "application/pdf", "proof".getBytes());

        assertThatThrownBy(() -> service.submit(42L, 5L, submission("50000.00"), List.of(file)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("exceeds");
        verify(mapper, never()).insertFinanceRecord(any());
    }

    @Test
    void rejectsInstallmentOutsideAuthenticatedOwnersProperties() {
        when(mapper.findSubmissionContext(42L, 999L, 2L)).thenReturn(null);
        MockMultipartFile file = new MockMultipartFile("files", "receipt.pdf", "application/pdf", "proof".getBytes());

        assertThatThrownBy(() -> service.submit(42L, 999L, submission("100.00"), List.of(file)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    private Submission submission(String amount) {
        return new Submission(2L, new BigDecimal(amount), LocalDate.now(), "bank_transfer",
                "Maybank Berhad", "MBE123456", "Test Owner", "submitted by owner");
    }
}
