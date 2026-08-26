package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.dto.AdminPropertyBankAccountRequest;
import com.ccps.backend.mapper.AdminPropertyBankAccountMapper;
import com.ccps.backend.mapper.AdminPropertyBankAccountMapper.AccountRow;

@ExtendWith(MockitoExtension.class)
class AdminPropertyBankAccountServiceTest {
    @Mock private AdminPropertyBankAccountMapper mapper;
    private AdminPropertyBankAccountService service;

    @BeforeEach
    void setUp() { service = new AdminPropertyBankAccountService(mapper); }

    @Test
    void storesAndReturnsCompletePmaBankDetails() {
        when(mapper.ownsProperty(7L, 12L)).thenReturn(1);
        when(mapper.insert(any(AccountRow.class))).thenAnswer(invocation -> {
            invocation.<AccountRow>getArgument(0).setId(91L);
            return 1;
        });
        when(mapper.find(12L, 91L)).thenAnswer(invocation -> {
            AccountRow row = new AccountRow();
            row.setId(91L); row.setOwnerUnitId(12L); row.setItemName("Maybank");
            row.setPaymentName("张业主"); row.setAccountNo("123456789");
            row.setBankAddress("Kuala Lumpur"); row.setBranchCode("MB-001");
            row.setSwiftCode("MBBEMYKL"); row.setTransferLimit(new java.math.BigDecimal("10000.00"));
            row.setOverseasBank(true); row.setOverseasTransferFee(new java.math.BigDecimal("25.00")); row.setRemarks("主要收款账户");
            return row;
        });

        var result = service.create(1L, 7L, 12L, new AdminPropertyBankAccountRequest(
                "Maybank", "张业主", "123456789", "Kuala Lumpur", "MB-001", "MBBEMYKL",
                new java.math.BigDecimal("10000.00"), true, new java.math.BigDecimal("25.00"), "主要收款账户"));

        ArgumentCaptor<AccountRow> captor = ArgumentCaptor.forClass(AccountRow.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getBankAddress()).isEqualTo("Kuala Lumpur");
        assertThat(captor.getValue().getBranchCode()).isEqualTo("MB-001");
        assertThat(captor.getValue().getSwiftCode()).isEqualTo("MBBEMYKL");
        assertThat(captor.getValue().getTransferLimit()).isEqualByComparingTo("10000.00");
        assertThat(captor.getValue().getOverseasBank()).isTrue();
        assertThat(captor.getValue().getOverseasTransferFee()).isEqualByComparingTo("25.00");
        assertThat(result.bankAddress()).isEqualTo("Kuala Lumpur");
        assertThat(result.branchCode()).isEqualTo("MB-001");
        assertThat(result.swiftCode()).isEqualTo("MBBEMYKL");
        assertThat(result.transferLimit()).isEqualByComparingTo("10000.00");
        assertThat(result.overseasBank()).isTrue();
        assertThat(result.overseasTransferFee()).isEqualByComparingTo("25.00");
    }
}
