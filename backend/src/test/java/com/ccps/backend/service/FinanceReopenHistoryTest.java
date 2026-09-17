package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import com.ccps.backend.mapper.AdminFinanceReviewMapper;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReopenRecordContext;
import com.ccps.backend.mapper.AdminFinanceReviewMapper.ReserveTopupReopenContext;

class FinanceReopenHistoryTest {
    @TempDir Path directory;
    private AdminFinanceReviewMapper mapper;
    private AdminFinanceReviewService service;
    private boolean snapshotAvailable = true;

    @BeforeEach
    void setUp() {
        mapper = mock(AdminFinanceReviewMapper.class, call -> {
            if (call.getMethod().getName().equals("insertFinanceReopenSnapshot")) {
                return snapshotAvailable ? 1 : 0;
            }
            return Answers.RETURNS_DEFAULTS.answer(call);
        });
        service = new AdminFinanceReviewService(mapper, directory.toString());
        ReopenRecordContext record = new ReopenRecordContext();
        record.setRecordType("reserve_topup");
        record.setConfirmationStatus("confirmed");
        record.setSyncStatus("not_synced");
        when(mapper.lockReopenRecord(21L)).thenReturn(record);
        ReserveTopupReopenContext topup = new ReserveTopupReopenContext();
        topup.setTransactionId(81L);
        topup.setReserveAccountId(71L);
        topup.setCurrentBalance(new BigDecimal("1800.00"));
        topup.setAmount(new BigDecimal("1000.00"));
        when(mapper.lockReserveTopupReopen(21L)).thenReturn(topup);
        when(mapper.reverseReserveTopupBalance(71L, topup.getAmount())).thenReturn(1);
        when(mapper.deleteReserveTopupTransaction(21L)).thenReturn(1);
        when(mapper.reopenFinanceRecord(21L)).thenReturn(1);
    }

    @Test
    void preservesOriginalSnapshotBeforeRemovingPostedReserveTransaction() {
        service.reopen(99L, 21L, "收款资料错误");
        List<String> calls = mockingDetails(mapper).getInvocations().stream()
                .map(call -> call.getMethod().getName()).toList();
        assertThat(calls).contains("insertFinanceReopenSnapshot");
        assertThat(calls.indexOf("insertFinanceReopenSnapshot"))
                .isLessThan(calls.indexOf("reverseReserveTopupBalance"));
        assertThat(calls.indexOf("insertFinanceReopenSnapshot"))
                .isLessThan(calls.indexOf("deleteReserveTopupTransaction"));
    }

    @Test
    void refusesToChangeMoneyWhenOriginalSnapshotCannotBeSaved() {
        snapshotAvailable = false;
        assertThatThrownBy(() -> service.reopen(99L, 21L, "收款资料错误"))
                .hasMessageContaining("snapshot");
        verify(mapper, never()).reverseReserveTopupBalance(anyLong(), any());
        verify(mapper, never()).deleteReserveTopupTransaction(anyLong());
        verify(mapper, never()).reopenFinanceRecord(anyLong());
    }
}
