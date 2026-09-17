package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminFundOperationsRequest.GenerateRemittanceBatch;
import com.ccps.backend.dto.AdminFundOperationsRequest.InternalTransfer;
import com.ccps.backend.dto.AdminFundOperationsRequest.Review;
import com.ccps.backend.dto.AdminRecordCreateResponse;
import com.ccps.backend.mapper.AdminFundOperationsMapper;
import com.ccps.backend.mapper.AdminFundOperationsMapper.InternalTransferRecord;
import com.ccps.backend.mapper.AdminFundOperationsMapper.RemittanceBatchRecord;
import com.ccps.backend.mapper.AdminFundOperationsMapper.RemittanceCandidateRow;
import com.ccps.backend.mapper.AdminFundOperationsMapper.RemittanceSubmitRow;
import com.ccps.backend.mapper.AdminFundOperationsMapper.TransferAccountRow;

@ExtendWith(MockitoExtension.class)
class AdminFundOperationsServiceTest {
    @Mock private AdminFundOperationsMapper mapper;
    @Mock private AdminReserveManagementService reserveService;
    private AdminFundOperationsService service;

    @BeforeEach
    void setUp() {
        service = new AdminFundOperationsService(mapper, reserveService);
    }

    @Test
    void rejectsTransferAcrossDifferentOwners() {
        when(mapper.lockTransferAccounts(1L, 2L)).thenReturn(List.of(
                account(1L, 10L, "1000.00", "200.00"),
                account(2L, 11L, "500.00", "100.00")));

        assertThatThrownBy(() -> service.createTransfer(9L, new InternalTransfer(
                1L, 2L, new BigDecimal("100.00"), LocalDate.of(2026, 9, 1), "同业主资金归集")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("同一业主");
    }

    @Test
    void approvalPostsBalancedTransferEntries() {
        InternalTransferRecord transfer = new InternalTransferRecord();
        transfer.setId(30L); transfer.setSourceReserveAccountId(1L); transfer.setTargetReserveAccountId(2L);
        transfer.setAmount(new BigDecimal("250.00")); transfer.setStatus("pending"); transfer.setCreatedBy(7L);
        when(mapper.lockTransfer(30L)).thenReturn(transfer);
        when(mapper.lockTransferAccounts(1L, 2L)).thenReturn(List.of(
                account(1L, 10L, "1000.00", "200.00"),
                account(2L, 10L, "500.00", "100.00")));
        when(mapper.debitTransferSource(1L, new BigDecimal("250.00"))).thenReturn(1);
        when(mapper.creditTransferTarget(2L, new BigDecimal("250.00"))).thenReturn(1);
        when(mapper.reviewTransfer(30L, "approved", "核准", 9L)).thenReturn(1);

        service.approveTransfer(9L, 30L, new Review("核准"));

        verify(mapper).insertReserveTransaction(1L, 30L, "transfer_out", new BigDecimal("250.00"),
                new BigDecimal("750.00"), "内部调拨 30：核准", 9L);
        verify(mapper).insertReserveTransaction(2L, 30L, "transfer_in", new BigDecimal("250.00"),
                new BigDecimal("750.00"), "内部调拨 30：核准", 9L);
    }

    @Test
    void dueBatchKeepsHigherRetainedAmountAndTax() {
        RemittanceCandidateRow candidate = new RemittanceCandidateRow();
        candidate.setReserveAccountId(1L); candidate.setBankAccountId(8L);
        candidate.setCurrentBalance(new BigDecimal("2000.00")); candidate.setMinimumBalance(new BigDecimal("500.00"));
        candidate.setRetainedAmount(new BigDecimal("600.00")); candidate.setTaxRetainedAmount(new BigDecimal("100.00"));
        when(mapper.lockDueRemittanceCandidates(LocalDate.of(2026, 9, 2))).thenReturn(List.of(candidate));
        when(mapper.insertRemittanceBatch(any(RemittanceBatchRecord.class))).thenAnswer(invocation -> {
            invocation.<RemittanceBatchRecord>getArgument(0).setId(50L); return 1;
        });
        when(mapper.insertRemittanceItem(eq(50L), eq(1L), eq(8L), any(), any(), any())).thenReturn(1);

        service.generateRemittanceBatch(9L,
                new GenerateRemittanceBatch(LocalDate.of(2026, 9, 2), "季度汇款"));

        verify(mapper).insertRemittanceItem(50L, 1L, 8L, new BigDecimal("1300.00"),
                new BigDecimal("600.00"), new BigDecimal("100.00"));
    }

    @Test
    void submittingBatchReusesBankLimitRefundWorkflowAndLinksEveryInstallment() {
        RemittanceBatchRecord batch = new RemittanceBatchRecord();
        batch.setId(50L); batch.setBatchNo("ORB-1"); batch.setScheduledDate(LocalDate.of(2026, 9, 2)); batch.setStatus("draft");
        RemittanceSubmitRow item = new RemittanceSubmitRow();
        item.setId(60L); item.setReserveAccountId(1L); item.setBankAccountId(8L); item.setAmount(new BigDecimal("12000.00"));
        when(mapper.lockRemittanceBatch(50L)).thenReturn(batch);
        when(mapper.lockRemittanceItems(50L)).thenReturn(List.of(item));
        when(reserveService.createRefunds(eq(9L), any())).thenReturn(List.of(
                new AdminRecordCreateResponse(71L, "RRF-1"), new AdminRecordCreateResponse(72L, "RRF-2")));
        when(mapper.linkRemittanceFinance(60L, 71L)).thenReturn(1);
        when(mapper.insertRemittanceFinanceLink(60L, 71L)).thenReturn(1);
        when(mapper.insertRemittanceFinanceLink(60L, 72L)).thenReturn(1);
        when(mapper.reviewRemittanceBatch(50L, "submitted", 9L)).thenReturn(1);

        service.submitRemittanceBatch(9L, 50L, new Review("季度出款"));

        verify(reserveService).createRefunds(eq(9L), any());
        verify(mapper).insertRemittanceFinanceLink(60L, 71L);
        verify(mapper).insertRemittanceFinanceLink(60L, 72L);
    }

    private TransferAccountRow account(Long id, Long ownerId, String balance, String minimum) {
        TransferAccountRow row = new TransferAccountRow();
        row.setId(id); row.setOwnerId(ownerId); row.setCurrentBalance(new BigDecimal(balance));
        row.setMinimumBalance(new BigDecimal(minimum)); row.setPropertyName("测试房产");
        return row;
    }
}
