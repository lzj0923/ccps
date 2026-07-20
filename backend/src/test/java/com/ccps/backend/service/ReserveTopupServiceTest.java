package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.ReserveTopupReviewResponse;
import com.ccps.backend.dto.ReserveTopupSubmissionResponse;
import com.ccps.backend.mapper.OwnerReserveMapper;
import com.ccps.backend.mapper.OwnerReserveMapper.ApprovedTopup;
import com.ccps.backend.mapper.OwnerReserveMapper.NewDocument;
import com.ccps.backend.mapper.OwnerReserveMapper.NewTopup;
import com.ccps.backend.mapper.OwnerReserveMapper.TopupContext;
import com.ccps.backend.mapper.OwnerReserveMapper.TopupReviewRow;
import com.ccps.backend.service.ReserveTopupService.Submission;

@ExtendWith(MockitoExtension.class)
class ReserveTopupServiceTest {
    @Mock private OwnerReserveMapper mapper;
    @TempDir Path tempDir;
    private ReserveTopupService service;

    @BeforeEach
    void setUp() {
        service = new ReserveTopupService(mapper, tempDir.toString(),
                Clock.fixed(Instant.parse("2026-07-16T02:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void submitsProofWithoutChangingReserveBalance() throws Exception {
        TopupContext context = new TopupContext();
        context.setReserveAccountId(7L); context.setUnitId(8L); context.setOwnerId(9L);
        when(mapper.findTopupContext(42L, 7L)).thenReturn(context);
        doAnswer(invocation -> { NewTopup row = invocation.getArgument(0); row.setId(101L); return 1; })
                .when(mapper).insertFinanceRecord(any(NewTopup.class));
        doAnswer(invocation -> { NewDocument row = invocation.getArgument(0); row.setId(201L); return 1; })
                .when(mapper).insertDocument(any(NewDocument.class));
        MockMultipartFile proof = new MockMultipartFile("files", "proof.png", "image/png", new byte[] {1, 2, 3});
        Submission submission = new Submission(7L, new BigDecimal("500.00"), LocalDate.parse("2026-07-15"),
                "bank_transfer", "Test Bank", "REF-1", "Owner", "Top up");

        ReserveTopupSubmissionResponse result = service.submit(42L, submission, List.of(proof));

        assertThat(result.financeRecordId()).isEqualTo(101L);
        assertThat(result.confirmationStatus()).isEqualTo("pending");
        assertThat(Files.list(tempDir.resolve("7"))).hasSize(1);
        verify(mapper).insertDocumentLink(201L, 101L);
    }

    @Test
    void approvalAtomicallyCreatesLedgerAndUpdatesBalance() {
        when(mapper.isAdmin(1L)).thenReturn(1);
        TopupReviewRow row = new TopupReviewRow();
        row.setFinanceRecordId(101L); row.setAmount(new BigDecimal("500.00"));
        row.setReserveAccountId(7L); row.setCurrentBalance(new BigDecimal("1000.00"));
        row.setConfirmationStatus("pending");
        when(mapper.findTopupForUpdate(101L)).thenReturn(row);
        when(mapper.updateTopupReview(101L, "confirmed", 1L)).thenReturn(1);

        ReserveTopupReviewResponse result = service.review(1L, 101L, true, "Proof verified");

        assertThat(result.balanceAfter()).isEqualByComparingTo("1500.00");
        verify(mapper).insertReserveTopup(any(ApprovedTopup.class));
        verify(mapper).updateReserveBalance(7L, new BigDecimal("1500.00"));
        verify(mapper).updateTopupDocuments(101L, "active");
    }

    @Test
    void rejectsNonAdminAndRequiresRejectionReason() {
        assertThatThrownBy(() -> service.review(99L, 101L, true, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Administrator");
        when(mapper.isAdmin(1L)).thenReturn(1);
        TopupReviewRow row = new TopupReviewRow();
        row.setFinanceRecordId(101L); row.setReserveAccountId(7L);
        row.setCurrentBalance(BigDecimal.ZERO); row.setAmount(BigDecimal.ONE); row.setConfirmationStatus("pending");
        when(mapper.findTopupForUpdate(101L)).thenReturn(row);
        assertThatThrownBy(() -> service.review(1L, 101L, false, " "))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("reason");
    }
}
