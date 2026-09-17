package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.mapper.OwnerPropertyDetailMapper;

@ExtendWith(MockitoExtension.class)
class OwnerPropertyDetailServiceTest {
    @Mock OwnerPropertyDetailMapper mapper;
    OwnerPropertyDetailService service;

    @BeforeEach void setUp(){ service=new OwnerPropertyDetailService(mapper); }

    @Test void masksBankAccountAndReturnsOwnerScopedDetail(){
        var property=new OwnerPropertyDetailMapper.PropertyRow(); property.setOwnerUnitId(18L); property.setProjectName("CCPS KL"); property.setUnitNo("A-18"); property.setAreaSqm(new BigDecimal("88.50"));
        var bank=new OwnerPropertyDetailMapper.BankRow(); bank.setId(3L); bank.setItemName("Maybank"); bank.setPaymentName("CCPS Owner"); bank.setAccountNo("123456789012");
        when(mapper.findProperty(42L,18L)).thenReturn(property); when(mapper.findLatestBank(18L)).thenReturn(bank); when(mapper.findMandates(18L)).thenReturn(List.of()); when(mapper.findLeases(18L)).thenReturn(List.of()); when(mapper.findPhotos(18L)).thenReturn(List.of());
        var result=service.get(42L,18L);
        assertThat(result.property().projectName()).isEqualTo("CCPS KL");
        assertThat(result.bankAccount().maskedAccountNo()).isEqualTo("•••• •••• 9012");
        assertThat(result.bankAccount().accountName()).isEqualTo("CCPS Owner");
    }

    @Test void refusesPropertyOutsideCurrentOwner(){
        when(mapper.findProperty(42L,99L)).thenReturn(null);
        assertThatThrownBy(()->service.get(42L,99L)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("Owner property not found");
    }

    @Test void usesSignedPreviewDocumentInsteadOfHiddenUnsignedSource(){
        var property = new OwnerPropertyDetailMapper.PropertyRow(); property.setOwnerUnitId(26L);
        var lease = new OwnerPropertyDetailMapper.LeaseRow(); lease.setId(18L); lease.setContractDocumentId(141L);
        when(mapper.findProperty(42L,26L)).thenReturn(property);
        when(mapper.findMandates(26L)).thenReturn(List.of());
        when(mapper.findPhotos(26L)).thenReturn(List.of());
        when(mapper.findLeases(26L)).thenReturn(List.of(lease));
        when(mapper.findPreviewContractId(141L)).thenReturn(143L);
        assertThat(service.get(42L,26L).leases().get(0).contractDocumentId()).isEqualTo(143L);
        assertThat(lease.getContractDocumentId()).isEqualTo(141L);
    }

    @Test void unavailableOrVoidedContractHasNoPreviewLink(){
        var property = new OwnerPropertyDetailMapper.PropertyRow(); property.setOwnerUnitId(26L);
        var lease = new OwnerPropertyDetailMapper.LeaseRow(); lease.setId(18L); lease.setContractDocumentId(141L);
        when(mapper.findProperty(42L,26L)).thenReturn(property);
        when(mapper.findMandates(26L)).thenReturn(List.of());
        when(mapper.findPhotos(26L)).thenReturn(List.of());
        when(mapper.findLeases(26L)).thenReturn(List.of(lease));
        when(mapper.findPreviewContractId(141L)).thenReturn(null);
        assertThat(service.get(42L,26L).leases().get(0).contractDocumentId()).isNull();
    }
}
