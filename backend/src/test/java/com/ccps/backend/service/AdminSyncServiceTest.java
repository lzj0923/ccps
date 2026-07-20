package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.dto.AdminSyncRequest;
import com.ccps.backend.mapper.AdminSyncMapper;
import com.ccps.backend.mapper.AdminSyncMapper.BatchRow;
import com.ccps.backend.mapper.AdminSyncMapper.FinanceSyncRow;
import com.ccps.backend.mapper.AdminSyncMapper.NewBatch;
import com.ccps.backend.mapper.AdminSyncMapper.NewItem;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AdminSyncServiceTest {
    @Mock private AdminSyncMapper mapper;
    @Mock private AccountingSyncGateway gateway;
    @Mock private FileAccountingSyncGateway fileGateway;
    private AdminSyncService service;

    @BeforeEach void setUp() { service = new AdminSyncService(mapper, gateway, fileGateway, new ObjectMapper()); }

    @Test void previewReturnsOnlyEligibleConfirmedRecordsFromMapper() {
        when(mapper.findEligibleRecords("rent_payment", List.of())).thenReturn(List.of(row(8L, "RENT-008")));
        var result = service.preview(new AdminSyncRequest("rent_payment", List.of()));
        assertThat(result.eligibleCount()).isEqualTo(1);
        assertThat(result.totalAmount()).isEqualByComparingTo("2100.00");
        assertThat(result.items()).singleElement().extracting(item -> item.transactionNo()).isEqualTo("RENT-008");
    }

    @Test void creatingBatchMarksRecordsExportedRatherThanSynced() throws Exception {
        FinanceSyncRow source = row(8L, "RENT-008");
        when(mapper.findEligibleRecords("rent_payment", List.of())).thenReturn(List.of(source));
        when(mapper.insertBatch(any())).thenAnswer(invocation -> { NewBatch batch = invocation.getArgument(0); batch.setId(31L); return 1; });
        when(mapper.insertItem(any())).thenAnswer(invocation -> { NewItem item = invocation.getArgument(0); item.setId(41L); return 1; });
        when(gateway.export(any(), any())).thenReturn(Path.of("SQL-batch.csv"));
        BatchRow completed = new BatchRow(); completed.setId(31L); completed.setBatchNo("SQL-BATCH"); completed.setSourceModule("rent_payment"); completed.setStatus("exported"); completed.setTotalCount(1); completed.setSuccessCount(1); completed.setFailureCount(0);
        when(mapper.findBatch(31L)).thenReturn(completed);

        var result = service.createBatch(5L, new AdminSyncRequest("rent_payment", List.of()));

        assertThat(result.status()).isEqualTo("exported");
        verify(mapper).updateFinanceSync(8L, 31L, "exported");
        verify(mapper).completeBatch(31L, "exported", 1, 0);
    }

    private FinanceSyncRow row(Long id, String no) {
        FinanceSyncRow row = new FinanceSyncRow(); row.setId(id); row.setTransactionNo(no); row.setRecordType("rent_payment");
        row.setTransactionDate(LocalDate.of(2026, 7, 20)); row.setAmount(new BigDecimal("2100.00")); row.setCurrency("MYR");
        row.setPaymentMethod("bank_transfer"); row.setSyncStatus("not_synced"); row.setPartyCode("OWNER-000001");
        row.setPartyName("Demo Owner"); row.setProjectCode("PRJ-1"); row.setProjectName("Demo Project"); row.setUnitNo("A-01");
        return row;
    }
}
