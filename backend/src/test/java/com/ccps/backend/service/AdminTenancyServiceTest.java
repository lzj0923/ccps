package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.ccps.backend.dto.AdminLeaseCreateRequest;
import com.ccps.backend.dto.AdminLeaseUpdateRequest;
import com.ccps.backend.dto.AdminLeaseTransferRequest;
import com.ccps.backend.mapper.AdminTenancyMapper;
import com.ccps.backend.mapper.AdminTenancyMapper.LeaseContractContext;
import com.ccps.backend.mapper.AdminTenancyMapper.LeaseChangeContext;
import com.ccps.backend.mapper.AdminTenancyMapper.NewLease;
import com.ccps.backend.mapper.AdminTenancyMapper.NewContractDocument;
import com.ccps.backend.mapper.AdminTenancyMapper.RentProofContext;

@ExtendWith(MockitoExtension.class)
class AdminTenancyServiceTest {
    @Mock AdminTenancyMapper mapper;
    @TempDir Path tempDir;
    AdminTenancyService service;

    @BeforeEach void setUp() {
        service = new AdminTenancyService(mapper,
                Clock.fixed(Instant.parse("2026-07-19T00:00:00Z"), ZoneOffset.UTC), tempDir);
    }

    @Test void createsCurrentMonthInvoiceForActiveLease() {
        when(mapper.countActiveTenant(5L)).thenReturn(1);
        when(mapper.countOperatingUnit(8L)).thenReturn(1);
        when(mapper.countOverlappingLease(8L, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"))).thenReturn(0);
        when(mapper.insertLease(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            AdminTenancyMapper.NewLease lease = invocation.getArgument(0); lease.setId(44L); return 1;
        });

        service.createLease(new AdminLeaseCreateRequest(5L, 8L, LocalDate.parse("2026-07-01"),
                LocalDate.parse("2027-06-30"), new BigDecimal("3000.00"), new BigDecimal("6000.00"), 5));

        verify(mapper).insertInvoice(44L, LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-05"), new BigDecimal("3000.00"));
        verify(mapper).activateRentalService(8L);
        verify(mapper).markUnitRented(8L);
    }

    @Test void uploadsAndBindsLeaseContract() {
        LeaseContractContext context = new LeaseContractContext(); context.setLeaseId(44L); context.setLeaseNo("LEASE-44");
        when(mapper.lockLeaseContract(44L)).thenReturn(context);
        when(mapper.insertContractDocument(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            NewContractDocument document = invocation.getArgument(0); document.setId(91L); return 1;
        });
        when(mapper.insertContractLink(91L, 44L)).thenReturn(1);
        when(mapper.updateLeaseContract(44L, 91L)).thenReturn(1);
        MockMultipartFile file = new MockMultipartFile("file", "signed-lease.pdf", "application/pdf", "lease".getBytes());

        Long documentId = service.uploadContract(1L, 44L, file);

        assertThat(documentId).isEqualTo(91L);
        ArgumentCaptor<NewContractDocument> captor = ArgumentCaptor.forClass(NewContractDocument.class);
        verify(mapper).insertContractDocument(captor.capture());
        assertThat(captor.getValue().getOriginalName()).isEqualTo("signed-lease.pdf");
        assertThat(Files.isRegularFile(tempDir.resolve(captor.getValue().getStorageKey()))).isTrue();
        verify(mapper).insertContractAudit(1L, 44L, null, 91L, "signed-lease.pdf", "upload_lease_contract");
    }

    @Test void replacingContractPreservesOldDocumentAsHistory() {
        LeaseContractContext context = new LeaseContractContext(); context.setLeaseId(44L);
        context.setLeaseNo("LEASE-44"); context.setContractDocumentId(70L);
        when(mapper.lockLeaseContract(44L)).thenReturn(context);
        when(mapper.insertContractDocument(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            NewContractDocument document = invocation.getArgument(0); document.setId(92L); return 1;
        });
        when(mapper.insertContractLink(92L, 44L)).thenReturn(1);
        when(mapper.updateLeaseContract(44L, 92L)).thenReturn(1);
        when(mapper.supersedeContractDocument(70L)).thenReturn(1);

        service.uploadContract(1L, 44L,
                new MockMultipartFile("file", "renewed.png", "image/png", "contract".getBytes()));

        verify(mapper).supersedeContractDocument(70L);
        verify(mapper).insertContractAudit(1L, 44L, 70L, 92L, "renewed.png", "replace_lease_contract");
    }

    @Test void adminUploadsOfflineApprovedRentProof() {
        RentProofContext context = new RentProofContext(); context.setFinanceRecordId(18L);
        context.setTransactionNo("RENT-18");
        when(mapper.lockRentProof(18L)).thenReturn(context);
        when(mapper.insertRentProofDocument(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            NewContractDocument document = invocation.getArgument(0); document.setId(101L); return 1;
        });
        when(mapper.insertRentProofLink(101L, 18L)).thenReturn(1);

        Long documentId = service.uploadRentProof(1L, 18L,
                new MockMultipartFile("file", "offline-receipt.pdf", "application/pdf", "receipt".getBytes()));

        assertThat(documentId).isEqualTo(101L);
        verify(mapper).insertRentProofAudit(1L, 18L, null, 101L, "offline-receipt.pdf", "upload_rent_proof");
    }

    @Test void adminReplacesRentProofAndKeepsOldVersion() {
        RentProofContext context = new RentProofContext(); context.setFinanceRecordId(18L);
        context.setTransactionNo("RENT-18"); context.setProofDocumentId(88L);
        when(mapper.lockRentProof(18L)).thenReturn(context);
        when(mapper.insertRentProofDocument(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            NewContractDocument document = invocation.getArgument(0); document.setId(102L); return 1;
        });
        when(mapper.insertRentProofLink(102L, 18L)).thenReturn(1);
        when(mapper.supersedeRentProof(88L)).thenReturn(1);

        service.uploadRentProof(1L, 18L,
                new MockMultipartFile("file", "replacement.png", "image/png", "receipt-v2".getBytes()));

        verify(mapper).supersedeRentProof(88L);
        verify(mapper).insertRentProofAudit(1L, 18L, 88L, 102L, "replacement.png", "replace_rent_proof");
    }

    @Test void editsActiveLeaseAndOnlyRefreshesUnpaidInvoiceTerms() {
        LeaseChangeContext lease = activeLease();
        when(mapper.lockLeaseForChange(44L)).thenReturn(lease);
        when(mapper.countOtherOverlappingLease(44L, 8L, LocalDate.parse("2026-01-01"), LocalDate.parse("2027-03-31"))).thenReturn(0);
        when(mapper.updateLeaseTerms(44L, LocalDate.parse("2026-01-01"), LocalDate.parse("2027-03-31"),
                new BigDecimal("3300.00"), new BigDecimal("6600.00"), 8)).thenReturn(1);

        service.updateLease(1L, 44L, new AdminLeaseUpdateRequest(LocalDate.parse("2026-01-01"),
                LocalDate.parse("2027-03-31"), new BigDecimal("3300.00"), new BigDecimal("6600.00"), 8));

        verify(mapper).updateFutureUnpaidInvoiceTerms(44L, LocalDate.parse("2026-07-01"),
                new BigDecimal("3300.00"), 8);
        verify(mapper).insertLeaseUpdateAudit(1L, 44L, lease.getStartDate(), lease.getEndDate(),
                lease.getMonthlyRent(), lease.getDepositAmount(), lease.getPaymentDay(), LocalDate.parse("2026-01-01"),
                LocalDate.parse("2027-03-31"), new BigDecimal("3300.00"), new BigDecimal("6600.00"), 8);
    }

    @Test void transfersLeaseByClosingOldLeaseAndCreatingNewLease() {
        LeaseChangeContext lease = activeLease();
        when(mapper.lockLeaseForChange(44L)).thenReturn(lease);
        when(mapper.countActiveTenant(9L)).thenReturn(1);
        when(mapper.countOtherOverlappingLease(44L, 8L, LocalDate.parse("2026-08-01"), LocalDate.parse("2027-06-30"))).thenReturn(0);
        when(mapper.closeLeaseForTransfer(44L, LocalDate.parse("2026-07-31"))).thenReturn(1);
        when(mapper.insertLease(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            NewLease created = invocation.getArgument(0); created.setId(55L); return 1;
        });

        Long newLeaseId = service.transferLease(1L, 44L, new AdminLeaseTransferRequest(9L,
                LocalDate.parse("2026-08-01"), LocalDate.parse("2027-06-30"),
                new BigDecimal("3200.00"), new BigDecimal("6400.00"), 5));

        assertThat(newLeaseId).isEqualTo(55L);
        verify(mapper).deleteOldFutureInvoices(44L, LocalDate.parse("2026-08-01"));
        verify(mapper).insertLeaseTransferAudit(1L, 44L, 55L, 5L, 9L,
                LocalDate.parse("2026-12-31"), LocalDate.parse("2026-08-01"));
    }

    private LeaseChangeContext activeLease() {
        LeaseChangeContext lease = new LeaseChangeContext();
        lease.setLeaseId(44L); lease.setUnitId(8L); lease.setTenantId(5L); lease.setLeaseNo("LEASE-44");
        lease.setStartDate(LocalDate.parse("2026-01-01")); lease.setEndDate(LocalDate.parse("2026-12-31"));
        lease.setMonthlyRent(new BigDecimal("3000.00")); lease.setDepositAmount(new BigDecimal("6000.00"));
        lease.setPaymentDay(5); lease.setStatus("active"); lease.setProjectName("測試建案"); lease.setUnitNo("A-01-01");
        return lease;
    }

}
