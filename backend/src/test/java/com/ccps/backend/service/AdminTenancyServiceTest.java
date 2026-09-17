package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import com.ccps.backend.dto.AdminLeaseCloseRequest;
import com.ccps.backend.dto.AdminLeaseUpdateRequest;
import com.ccps.backend.dto.AdminLeaseRenewalRequest;
import com.ccps.backend.dto.AdminLeaseTransferRequest;
import com.ccps.backend.dto.AdminRentCollectionRequest;
import com.ccps.backend.dto.AdminRentCollectionBatchRequest;
import com.ccps.backend.dto.AdminTenantCreateRequest;
import com.ccps.backend.dto.AdminTenantDepositTransactionRequest;
import com.ccps.backend.dto.AdminTenantDepositTransactionResponse;
import com.ccps.backend.mapper.AdminTenancyMapper;
import com.ccps.backend.mapper.AdminTenancyMapper.DepositAccountRow;
import com.ccps.backend.mapper.AdminTenancyMapper.LeaseContractContext;
import com.ccps.backend.mapper.AdminTenancyMapper.LeaseChangeContext;
import com.ccps.backend.mapper.AdminTenancyMapper.NewLease;
import com.ccps.backend.mapper.AdminTenancyMapper.NewLeasePeriod;
import com.ccps.backend.mapper.AdminTenancyMapper.NewTenant;
import com.ccps.backend.mapper.AdminTenancyMapper.LeasePeriodRow;
import com.ccps.backend.mapper.AdminTenancyMapper.NewSecurityDeposit;
import com.ccps.backend.mapper.AdminTenancyMapper.NewContractDocument;
import com.ccps.backend.mapper.AdminTenancyMapper.ContractFile;
import com.ccps.backend.mapper.AdminTenancyMapper.RentProofContext;
import com.ccps.backend.mapper.AdminTenancyMapper.RentCollectionContext;
import com.ccps.backend.mapper.AdminTenancyMapper.RentInvoiceAdvanceRow;
import com.ccps.backend.mapper.AdminTenancyMapper.RentalSpaceContext;

@ExtendWith(MockitoExtension.class)
class AdminTenancyServiceTest {
    @Mock AdminTenancyMapper mapper;
    @Mock PropertyExpensePostingService propertyExpensePostingService;
    @Mock TenantWhatsAppSubscriptionService tenantWhatsAppSubscriptionService;
    @TempDir Path tempDir;
    AdminTenancyService service;

    @BeforeEach void setUp() {
        service = new AdminTenancyService(mapper,
                Clock.fixed(Instant.parse("2026-07-19T00:00:00Z"), ZoneOffset.UTC), tempDir);
        service.setTenantWhatsAppSubscriptionService(tenantWhatsAppSubscriptionService);
        org.mockito.Mockito.lenient().when(mapper.insertLeasePeriod(org.mockito.ArgumentMatchers.any(NewLeasePeriod.class))).thenAnswer(invocation -> {
            NewLeasePeriod period = invocation.getArgument(0);
            if (period.getId() == null) period.setId(700L + period.getPeriodNo());
            return 1;
        });
        stubSecurityDepositWrites();
    }

    @Test void allowsDifferentTenantsToShareEmailAndPhone() {
        when(mapper.countTenantIdentity("ID-SECOND")).thenReturn(0);
        when(mapper.insertTenant(org.mockito.ArgumentMatchers.any(NewTenant.class))).thenAnswer(invocation -> {
            NewTenant tenant = invocation.getArgument(0);
            tenant.setId(52L);
            return 1;
        });

        Long tenantId = service.createTenant(new AdminTenantCreateRequest(
                "Second Tenant", "ID-SECOND", "18981712596", "shared@example.com", "active"));

        assertThat(tenantId).isEqualTo(52L);
        ArgumentCaptor<NewTenant> tenantCaptor = ArgumentCaptor.forClass(NewTenant.class);
        verify(mapper).insertTenant(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getEmail()).isEqualTo("shared@example.com");
        assertThat(tenantCaptor.getValue().getPhone()).isEqualTo("18981712596");
        verify(tenantWhatsAppSubscriptionService).synchronizeTenant(52L, "18981712596", "active", null);
    }

    @Test void updatesTenantStatusWithoutRevalidatingLegacyContactFields() {
        when(mapper.countTenant(52L)).thenReturn(1);
        when(mapper.updateTenantStatus(52L, "inactive")).thenReturn(1);

        service.updateTenantStatus(52L, "inactive");

        verify(mapper).updateTenantStatus(52L, "inactive");
        verify(tenantWhatsAppSubscriptionService).synchronizeTenant(52L);
    }

    @Test void createsTenantWithExplicitOptOutInTheSameSave() {
        when(mapper.insertTenant(org.mockito.ArgumentMatchers.any(NewTenant.class))).thenAnswer(invocation -> {
            NewTenant tenant = invocation.getArgument(0); tenant.setId(53L); return 1;
        });
        service.createTenant(new AdminTenantCreateRequest("Tenant", null, null, null, "active", false));
        verify(tenantWhatsAppSubscriptionService).synchronizeTenant(53L, null, "active", false);
    }

    @Test void tenantPhoneEditPassesNullPreferenceToPreserveOptOut() {
        when(mapper.countTenant(52L)).thenReturn(1);
        when(mapper.updateTenant(org.mockito.ArgumentMatchers.any(NewTenant.class))).thenReturn(1);
        service.updateTenant(52L, new AdminTenantCreateRequest("Tenant", null, "+60123456789", null, "active"));
        verify(tenantWhatsAppSubscriptionService).synchronizeTenant(52L, "+60123456789", "active", null);
    }

    @Test void deletingTenantWithoutLeaseHistoryAlsoRemovesSubscription() {
        when(mapper.countTenant(52L)).thenReturn(1);
        when(mapper.deleteTenant(52L)).thenReturn(1);
        service.deleteTenant(52L);
        var order = org.mockito.Mockito.inOrder(tenantWhatsAppSubscriptionService, mapper);
        order.verify(tenantWhatsAppSubscriptionService).deleteForTenant(52L);
        order.verify(mapper).deleteTenant(52L);
    }

    @Test void createsLeaseWithTwoAndAHalfMonthsDepositForTheExplicitCurrentRentalMandate() {
        when(mapper.countActiveTenant(5L)).thenReturn(1);
        when(mapper.countCurrentRentalMandate(42L, 8L, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"))).thenReturn(1);
        when(mapper.countOperatingUnit(8L)).thenReturn(1);
        when(mapper.countOverlappingLease(8L, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"))).thenReturn(0);
        when(mapper.insertLease(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            AdminTenancyMapper.NewLease lease = invocation.getArgument(0); lease.setId(47L); return 1;
        });
        stubSecurityDepositWrites();

        service.createLease(new AdminLeaseCreateRequest(5L, 8L, LocalDate.parse("2026-07-01"),
                LocalDate.parse("2027-06-30"), new BigDecimal("3000.00"), new BigDecimal("6000.00"), 5, "daily_prorated", 42L));

        ArgumentCaptor<NewLease> captor = ArgumentCaptor.forClass(NewLease.class);
        verify(mapper).insertLease(captor.capture());
        assertThat(captor.getValue().getRentalMandateId()).isEqualTo(42L);
        ArgumentCaptor<NewSecurityDeposit> depositCaptor = ArgumentCaptor.forClass(NewSecurityDeposit.class);
        verify(mapper).insertSecurityDepositFinance(depositCaptor.capture());
        assertThat(depositCaptor.getValue().getLeaseId()).isEqualTo(47L);
        assertThat(depositCaptor.getValue().getAmount()).isEqualByComparingTo("7500.00");
        verify(mapper).insertSecurityDepositEntry(org.mockito.ArgumentMatchers.any(NewSecurityDeposit.class));
        verify(mapper).insertSecurityDepositCashflow(org.mockito.ArgumentMatchers.any(NewSecurityDeposit.class));
    }

    @Test void createsCurrentMonthInvoiceForActiveLease() {
        when(mapper.countActiveTenant(5L)).thenReturn(1);
        when(mapper.countActiveRentalMandate(8L, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"))).thenReturn(1);
        when(mapper.countOperatingUnit(8L)).thenReturn(1);
        when(mapper.countOverlappingLease(8L, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"))).thenReturn(0);
        when(mapper.insertLease(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            AdminTenancyMapper.NewLease lease = invocation.getArgument(0); lease.setId(44L); return 1;
        });
        stubSecurityDepositWrites();

        service.createLease(new AdminLeaseCreateRequest(5L, 8L, LocalDate.parse("2026-07-01"),
                LocalDate.parse("2027-06-30"), new BigDecimal("3000.00"), new BigDecimal("6000.00"), 5));

        verify(mapper).insertInvoice(44L, LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-05"), new BigDecimal("3000.00"));
        verify(mapper).activateRentalService(8L);
        verify(mapper).markUnitRented(8L);
    }

    @Test void createsEveryRentInvoiceThroughCurrentMonthWhenLeaseIsEnteredLate() {
        when(mapper.countActiveTenant(5L)).thenReturn(1);
        when(mapper.countActiveRentalMandate(8L, LocalDate.parse("2026-05-01"), LocalDate.parse("2026-12-31"))).thenReturn(1);
        when(mapper.countOperatingUnit(8L)).thenReturn(1);
        when(mapper.countOverlappingLease(8L, LocalDate.parse("2026-05-01"), LocalDate.parse("2026-12-31"))).thenReturn(0);
        when(mapper.insertLease(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            AdminTenancyMapper.NewLease lease = invocation.getArgument(0); lease.setId(45L); return 1;
        });

        service.createLease(new AdminLeaseCreateRequest(5L, 8L, LocalDate.parse("2026-05-01"),
                LocalDate.parse("2026-12-31"), new BigDecimal("5000.00"), BigDecimal.ZERO, 1));

        verify(mapper).insertInvoice(45L, LocalDate.parse("2026-05-01"),
                LocalDate.parse("2026-05-01"), new BigDecimal("5000.00"));
        verify(mapper).insertInvoice(45L, LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-01"), new BigDecimal("5000.00"));
        verify(mapper).insertInvoice(45L, LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-01"), new BigDecimal("5000.00"));
    }

    @Test void proratesFirstMonthByActualDaysWhenRequested() {
        service = new AdminTenancyService(mapper,
                Clock.fixed(Instant.parse("2026-09-30T00:00:00Z"), ZoneOffset.UTC), tempDir);
        when(mapper.countActiveTenant(5L)).thenReturn(1);
        when(mapper.countActiveRentalMandate(8L, LocalDate.parse("2026-07-16"), LocalDate.parse("2027-06-30"))).thenReturn(1);
        when(mapper.countOperatingUnit(8L)).thenReturn(1);
        when(mapper.countOverlappingLease(8L, LocalDate.parse("2026-07-16"), LocalDate.parse("2027-06-30"))).thenReturn(0);
        when(mapper.insertLease(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            AdminTenancyMapper.NewLease lease = invocation.getArgument(0); lease.setId(46L); return 1;
        });

        service.createLease(new AdminLeaseCreateRequest(5L, 8L, LocalDate.parse("2026-07-16"),
                LocalDate.parse("2027-06-30"), new BigDecimal("5000.00"), BigDecimal.ZERO, 1, "daily_prorated"));

        verify(mapper).insertInvoice(46L, LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-01"), new BigDecimal("2580.65"));
        verify(mapper).insertInvoice(46L, LocalDate.parse("2026-08-01"),
                LocalDate.parse("2026-08-01"), new BigDecimal("5000.00"));
    }

    @Test void neverCreatesAZeroRentInvoiceAfterDailyProration() {
        when(mapper.countActiveTenant(5L)).thenReturn(1);
        when(mapper.countCurrentRentalMandate(42L, 8L, LocalDate.parse("2026-07-30"), LocalDate.parse("2026-07-31"))).thenReturn(1);
        when(mapper.countOperatingUnit(8L)).thenReturn(1);
        when(mapper.countOverlappingLease(8L, LocalDate.parse("2026-07-30"), LocalDate.parse("2026-07-31"))).thenReturn(0);
        when(mapper.insertLease(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            AdminTenancyMapper.NewLease lease = invocation.getArgument(0); lease.setId(48L); return 1;
        });

        service.createLease(new AdminLeaseCreateRequest(5L, 8L, LocalDate.parse("2026-07-30"),
                LocalDate.parse("2026-07-31"), new BigDecimal("0.01"), BigDecimal.ZERO, 1,
                "daily_prorated", 42L));

        verify(mapper).insertInvoice(48L, LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-01"), new BigDecimal("0.01"));
    }

    @Test void repairsAnExistingZeroAmountFirstInvoice() {
        LeaseChangeContext context = new LeaseChangeContext();
        context.setLeaseId(49L);
        context.setStatus("active");
        context.setStartDate(LocalDate.parse("2026-07-30"));
        context.setEndDate(LocalDate.parse("2026-07-31"));
        context.setPaymentDay(1);
        context.setMonthlyRent(new BigDecimal("0.01"));
        context.setRentCalculationMethod("daily_prorated");
        when(mapper.lockLeaseForChange(49L)).thenReturn(context);
        when(mapper.repairZeroAmountInvoice(49L, LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-01"), new BigDecimal("0.01"))).thenReturn(1);

        service.createLeaseInvoice(49L, new com.ccps.backend.dto.AdminLeaseInvoiceCreateRequest(
                LocalDate.parse("2026-07-01")));

        verify(mapper).repairZeroAmountInvoice(49L, LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-01"), new BigDecimal("0.01"));
        org.mockito.Mockito.verify(mapper, org.mockito.Mockito.never()).insertInvoice(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test void filtersTenantRentRowsByBillingDateRange() {
        LocalDate startDate = LocalDate.parse("2026-01-01");
        LocalDate endDate = LocalDate.parse("2026-12-31");
        when(mapper.countPage(null, null, null, startDate, endDate)).thenReturn(0L);
        when(mapper.findPage(null, null, null, startDate, endDate, 10, 0)).thenReturn(java.util.List.of());

        service.find(1, 10, null, null, null, startDate, endDate);

        verify(mapper).countPage(null, null, null, startDate, endDate);
        verify(mapper).findPage(null, null, null, startDate, endDate, 10, 0);
    }

    @Test void uploadsAndBindsLeaseContract() {
        when(mapper.countLeaseRentalMandate(44L)).thenReturn(1);
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

    @Test void acceptsSystemGeneratedLeaseContractLargerThanTenMegabytes() {
        when(mapper.countLeaseRentalMandate(44L)).thenReturn(1);
        LeaseContractContext context = new LeaseContractContext(); context.setLeaseId(44L); context.setLeaseNo("LEASE-44");
        when(mapper.lockLeaseContract(44L)).thenReturn(context);
        when(mapper.insertContractDocument(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            NewContractDocument document = invocation.getArgument(0); document.setId(94L); return 1;
        });
        when(mapper.insertContractLink(94L, 44L)).thenReturn(1);
        when(mapper.updateLeaseContract(44L, 94L)).thenReturn(1);
        MockMultipartFile generated = new MockMultipartFile("file", "generated-lease.pdf", "application/pdf",
                new byte[13 * 1024 * 1024]);

        Long documentId = service.uploadGeneratedContract(1L, 44L, generated);

        assertThat(documentId).isEqualTo(94L);
    }

    @Test void keepsManualLeaseContractLimitAtTenMegabytes() {
        MockMultipartFile oversizedContract = new MockMultipartFile("file", "oversized-contract.pdf", "application/pdf",
                new byte[11 * 1024 * 1024]);

        assertThatThrownBy(() -> service.uploadContract(1L, 44L, oversizedContract))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("up to 10MB");
    }

    @Test void downloadsLatestSignedLeaseContractWhenAvailable() throws Exception {
        ContractFile file = new ContractFile();
        file.setOriginalName("LEASE-44-已簽署.pdf");
        file.setStorageKey("10/signed-contract.pdf");
        file.setMimeType("application/pdf");
        file.setStorageArea("electronic_signature");
        Path signed = tempDir.getParent().resolve("electronic-signatures/10/signed-contract.pdf");
        Files.createDirectories(signed.getParent());
        Files.writeString(signed, "signed contract");
        file.setFileSize(Files.size(signed));
        when(mapper.findContractFile(44L)).thenReturn(file);

        AdminTenancyService.Download download = service.downloadContract(44L);

        assertThat(download.path()).isEqualTo(signed);
        assertThat(download.originalName()).isEqualTo("LEASE-44-已簽署.pdf");
    }

    @Test void replacingContractPreservesOldDocumentAsHistory() {
        when(mapper.countLeaseRentalMandate(44L)).thenReturn(1);
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

    @Test void monthlyRentInvoiceJobAlsoGeneratesTheMonthlyOwnerManagementFee() {
        service.setPropertyExpensePostingService(propertyExpensePostingService);
        when(mapper.findExpiredLeases(LocalDate.parse("2026-05-19"))).thenReturn(java.util.List.of());
        when(mapper.findLeaseIdsWithAvailableRentCredit()).thenReturn(java.util.List.of());

        service.generateCurrentMonthInvoices();

        verify(mapper).generateMonthlyInvoices(LocalDate.parse("2026-07-01"));
        verify(propertyExpensePostingService).postDueExpenses();
    }

    @Test void createsIndependentLeaseForAvailableRoomInSharedUnit() {
        when(mapper.countActiveTenant(5L)).thenReturn(1);
        when(mapper.countActiveRentalMandate(8L, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"))).thenReturn(1);
        when(mapper.countOperatingUnit(8L)).thenReturn(1);
        RentalSpaceContext room = new RentalSpaceContext(); room.setId(82L); room.setUnitId(8L);
        room.setSpaceType("room"); room.setStatus("active"); room.setRentalMode("shared");
        when(mapper.findRentalSpace(82L)).thenReturn(room);
        when(mapper.countRoomOverlap(null, 8L, 82L, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"))).thenReturn(0);
        when(mapper.insertLease(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            NewLease lease = invocation.getArgument(0); lease.setId(82L); return 1;
        });

        service.createLease(new AdminLeaseCreateRequest(5L, 8L, LocalDate.parse("2026-07-01"),
                LocalDate.parse("2027-06-30"), new BigDecimal("1200.00"), BigDecimal.ZERO, 1,
                "daily_prorated", null, 82L));

        ArgumentCaptor<NewLease> captor = ArgumentCaptor.forClass(NewLease.class);
        verify(mapper).insertLease(captor.capture());
        assertThat(captor.getValue().getRentalSpaceId()).isEqualTo(82L);
    }

    @Test void rejectsWholeUnitLeaseWhenAnyRoomOverlaps() {
        when(mapper.countActiveTenant(5L)).thenReturn(1);
        when(mapper.countActiveRentalMandate(8L, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"))).thenReturn(1);
        when(mapper.countOperatingUnit(8L)).thenReturn(1);
        RentalSpaceContext whole = new RentalSpaceContext(); whole.setId(80L); whole.setUnitId(8L);
        whole.setSpaceType("whole_unit"); whole.setStatus("active"); whole.setRentalMode("whole_unit");
        when(mapper.findRentalSpace(80L)).thenReturn(whole);
        when(mapper.countAnySpaceOverlap(null, 8L, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"))).thenReturn(1);

        assertThatThrownBy(() -> service.createLease(new AdminLeaseCreateRequest(5L, 8L,
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"), new BigDecimal("3000.00"),
                BigDecimal.ZERO, 1, "daily_prorated", null, 80L)))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("整租与房间合租也不能同时存在");
    }

    @Test void replacesLeaseContractAfterSigningHasStartedAndCancelsOldRequest() {
        when(mapper.countLeaseRentalMandate(44L)).thenReturn(1);
        LeaseContractContext context = new LeaseContractContext(); context.setLeaseId(44L);
        context.setLeaseNo("LEASE-44"); context.setContractDocumentId(70L);
        when(mapper.lockLeaseContract(44L)).thenReturn(context);
        when(mapper.insertContractDocument(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            NewContractDocument document = invocation.getArgument(0); document.setId(93L); return 1;
        });
        when(mapper.insertContractLink(93L, 44L)).thenReturn(1);
        when(mapper.updateLeaseContract(44L, 93L)).thenReturn(1);
        when(mapper.supersedeContractDocument(70L)).thenReturn(1);

        Long documentId = service.uploadContract(1L, 44L,
                new MockMultipartFile("file", "replacement.pdf", "application/pdf", "contract".getBytes()));

        assertThat(documentId).isEqualTo(93L);
        verify(mapper).cancelActiveLeaseSignatures(44L, 70L);
        verify(mapper).supersedeContractDocument(70L);
        verify(mapper).insertContractAudit(1L, 44L, 70L, 93L, "replacement.pdf", "replace_lease_contract");
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

    @Test void keepsRentPaymentProofLimitAtTenMegabytes() {
        MockMultipartFile oversizedProof = new MockMultipartFile("file", "oversized-proof.pdf", "application/pdf",
                new byte[11 * 1024 * 1024]);

        assertThatThrownBy(() -> service.uploadRentProof(1L, 18L, oversizedProof))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("up to 10MB");
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

    @Test void confirmsAdvanceRentByCreatingOneRentPaymentRecordForEachCoveredMonth() {
        RentCollectionContext context = new RentCollectionContext();
        context.setInvoiceId(1L); context.setLeaseId(10L); context.setTenantId(5L); context.setUnitId(8L); context.setOwnerId(9L);
        context.setLeaseNo("LEASE-10"); context.setTenantName("租客"); context.setProjectName("建案"); context.setUnitNo("A-01");
        context.setBillingMonth(LocalDate.parse("2026-07-01")); context.setStartDate(LocalDate.parse("2026-07-01"));
        context.setEndDate(LocalDate.parse("2026-09-30")); context.setMonthlyRent(new BigDecimal("1000.00"));
        context.setPaymentDay(5); context.setRentCalculationMethod("daily_prorated");
        context.setAmountDue(new BigDecimal("1000.00")); context.setAmountPaid(BigDecimal.ZERO);
        when(mapper.lockRentCollection(1L)).thenReturn(context);

        RentInvoiceAdvanceRow current = invoiceAdvanceRow(1L, "2026-07-01", "1000.00", "0.00");
        RentInvoiceAdvanceRow august = invoiceAdvanceRow(2L, "2026-08-01", "1000.00", "0.00");
        when(mapper.lockRentInvoicesForAdvance(10L, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-09-01")))
                .thenReturn(java.util.List.of(current), java.util.List.of(current, august));
        when(mapper.insertInvoice(10L, LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-05"), new BigDecimal("1000.00")))
                .thenReturn(1);
        long[] nextFinanceId = { 50L };
        when(mapper.insertConfirmedRentPayment(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            AdminTenancyMapper.NewRentCollection record = invocation.getArgument(0); record.setId(nextFinanceId[0]++); return 1;
        });
        when(mapper.linkRentPayment(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(BigDecimal.class))).thenReturn(1);
        when(mapper.insertRentCollectionReceipt(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        when(mapper.insertRentCashflow(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(LocalDate.class))).thenReturn(1);
        when(mapper.applyRentCollection(1L, new BigDecimal("1000.00"))).thenReturn(1);
        when(mapper.applyRentCollection(2L, new BigDecimal("1000.00"))).thenReturn(1);
        when(mapper.insertRentCollectionAudit(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(BigDecimal.class),
                org.mockito.ArgumentMatchers.any(BigDecimal.class), org.mockito.ArgumentMatchers.any(BigDecimal.class),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(1);

        service.confirmRentCollection(1L, 1L, new AdminRentCollectionRequest(new BigDecimal("2000.00"),
                LocalDate.parse("2026-07-19"), "cash", "租客", null, null, null), null);

        verify(mapper).insertInvoice(10L, LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-05"), new BigDecimal("1000.00"));
        ArgumentCaptor<AdminTenancyMapper.NewRentCollection> records = ArgumentCaptor.forClass(AdminTenancyMapper.NewRentCollection.class);
        verify(mapper, org.mockito.Mockito.times(2)).insertConfirmedRentPayment(records.capture());
        assertThat(records.getAllValues()).extracting(AdminTenancyMapper.NewRentCollection::getAmount)
                .containsExactly(new BigDecimal("1000.00"), new BigDecimal("1000.00"));
        assertThat(records.getAllValues()).extracting(AdminTenancyMapper.NewRentCollection::getReceivedDate)
                .containsExactly(LocalDate.parse("2026-07-19"), LocalDate.parse("2026-07-19"));
        assertThat(records.getAllValues()).extracting(AdminTenancyMapper.NewRentCollection::getPostingDate)
                .containsExactly(LocalDate.parse("2026-07-19"), LocalDate.parse("2026-08-01"));
        verify(mapper).linkRentPayment(1L, 50L, new BigDecimal("1000.00"));
        verify(mapper).linkRentPayment(2L, 51L, new BigDecimal("1000.00"));
        verify(mapper).applyRentCollection(1L, new BigDecimal("1000.00"));
        verify(mapper).applyRentCollection(2L, new BigDecimal("1000.00"));
        verify(mapper, org.mockito.Mockito.never()).insertRentCredit(org.mockito.ArgumentMatchers.any());
    }

    @Test void doesNotCreateMonthlyRentPaymentForPartiallyCoveredFutureMonth() {
        RentCollectionContext context = new RentCollectionContext();
        context.setInvoiceId(1L); context.setLeaseId(10L); context.setTenantId(5L); context.setUnitId(8L); context.setOwnerId(9L);
        context.setLeaseNo("LEASE-10"); context.setTenantName("租客"); context.setProjectName("建案"); context.setUnitNo("A-01");
        context.setBillingMonth(LocalDate.parse("2026-07-01")); context.setStartDate(LocalDate.parse("2026-07-01"));
        context.setEndDate(LocalDate.parse("2026-09-30")); context.setMonthlyRent(new BigDecimal("1000.00"));
        context.setPaymentDay(5); context.setRentCalculationMethod("daily_prorated");
        context.setAmountDue(new BigDecimal("1000.00")); context.setAmountPaid(BigDecimal.ZERO);
        when(mapper.lockRentCollection(1L)).thenReturn(context);
        RentInvoiceAdvanceRow current = invoiceAdvanceRow(1L, "2026-07-01", "1000.00", "0.00");
        RentInvoiceAdvanceRow august = invoiceAdvanceRow(2L, "2026-08-01", "1000.00", "0.00");
        when(mapper.lockRentInvoicesForAdvance(10L, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-09-01")))
                .thenReturn(java.util.List.of(current), java.util.List.of(current, august));
        when(mapper.insertInvoice(10L, LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-05"), new BigDecimal("1000.00")))
                .thenReturn(1);
        when(mapper.insertConfirmedRentPayment(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            AdminTenancyMapper.NewRentCollection record = invocation.getArgument(0); record.setId(50L); return 1;
        });
        when(mapper.linkRentPayment(1L, 50L, new BigDecimal("1000.00"))).thenReturn(1);
        when(mapper.insertRentCollectionReceipt(org.mockito.ArgumentMatchers.eq(50L), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        when(mapper.insertRentCashflow(org.mockito.ArgumentMatchers.eq(50L), org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(LocalDate.parse("2026-07-19")))).thenReturn(1);
        when(mapper.applyRentCollection(1L, new BigDecimal("1000.00"))).thenReturn(1);
        when(mapper.insertRentCollectionAudit(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(50L),
                org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(BigDecimal.class),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("1000.00")), org.mockito.ArgumentMatchers.eq(new BigDecimal("1000.00")),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(1);
        when(mapper.insertRentCredit(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            AdminTenancyMapper.NewRentCredit credit = invocation.getArgument(0); credit.setId(70L); return 1;
        });
        AdminTenancyMapper.RentCreditRow credit = new AdminTenancyMapper.RentCreditRow();
        credit.setId(70L); credit.setRemainingAmount(new BigDecimal("500.00"));
        when(mapper.lockAvailableRentCredits(10L)).thenReturn(java.util.List.of(credit));
        AdminTenancyMapper.RentInvoiceCreditRow future = new AdminTenancyMapper.RentInvoiceCreditRow();
        future.setInvoiceId(2L); future.setAmountDue(new BigDecimal("1000.00")); future.setAmountPaid(BigDecimal.ZERO);
        when(mapper.lockOutstandingInvoicesForCredit(10L)).thenReturn(java.util.List.of(future));
        when(mapper.applyRentCollection(2L, new BigDecimal("500.00"))).thenReturn(1);
        when(mapper.consumeRentCredit(70L, new BigDecimal("500.00"))).thenReturn(1);
        when(mapper.insertRentCreditAllocation(70L, 2L, new BigDecimal("500.00"), 1L)).thenReturn(1);

        service.confirmRentCollection(1L, 1L, new AdminRentCollectionRequest(new BigDecimal("1500.00"),
                LocalDate.parse("2026-07-19"), "cash", "租客", null, null, null), null);

        ArgumentCaptor<AdminTenancyMapper.NewRentCollection> records = ArgumentCaptor.forClass(AdminTenancyMapper.NewRentCollection.class);
        verify(mapper, org.mockito.Mockito.times(1)).insertConfirmedRentPayment(records.capture());
        assertThat(records.getValue().getAmount()).isEqualByComparingTo("1500.00");
        verify(mapper, org.mockito.Mockito.never()).linkRentPayment(org.mockito.ArgumentMatchers.eq(2L),
                org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.any(BigDecimal.class));
        verify(mapper).insertRentCredit(org.mockito.ArgumentMatchers.any());
    }

    @Test void batchConfirmsBankTransferWithoutOptionalReferenceAndIgnoresDuplicateIds() {
        RentCollectionContext context = new RentCollectionContext();
        context.setInvoiceId(1L); context.setLeaseId(10L); context.setTenantId(5L); context.setUnitId(8L); context.setOwnerId(9L);
        context.setLeaseNo("LEASE-10"); context.setTenantName("租客"); context.setProjectName("建案"); context.setUnitNo("A-01");
        context.setBillingMonth(LocalDate.parse("2026-07-01")); context.setStartDate(LocalDate.parse("2026-07-01"));
        context.setEndDate(LocalDate.parse("2026-09-30")); context.setMonthlyRent(new BigDecimal("1000.00"));
        context.setPaymentDay(5); context.setRentCalculationMethod("daily_prorated");
        context.setAmountDue(new BigDecimal("1000.00")); context.setAmountPaid(new BigDecimal("250.00"));
        when(mapper.lockRentCollection(1L)).thenReturn(context);
        when(mapper.lockRentInvoicesForAdvance(10L, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-09-01")))
                .thenReturn(java.util.List.of(invoiceAdvanceRow(1L, "2026-07-01", "1000.00", "250.00")));
        when(mapper.insertConfirmedRentPayment(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            AdminTenancyMapper.NewRentCollection record = invocation.getArgument(0); record.setId(50L); return 1;
        });
        when(mapper.linkRentPayment(1L, 50L, new BigDecimal("750.00"))).thenReturn(1);
        when(mapper.insertRentCollectionReceipt(org.mockito.ArgumentMatchers.eq(50L), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq("租客"), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq("批量核对无误"))).thenReturn(1);
        when(mapper.insertRentCashflow(org.mockito.ArgumentMatchers.eq(50L), org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.eq(5L), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(LocalDate.parse("2026-07-19")))).thenReturn(1);
        when(mapper.applyRentCollection(1L, new BigDecimal("750.00"))).thenReturn(1);

        var results = service.confirmRentCollections(1L, new AdminRentCollectionBatchRequest(
                java.util.List.of(1L, 1L), LocalDate.parse("2026-07-19"), "bank_transfer",
                null, "批量核对无误"));

        assertThat(results).hasSize(1);
        verify(mapper).applyRentCollection(1L, new BigDecimal("750.00"));
    }

    private RentInvoiceAdvanceRow invoiceAdvanceRow(Long id, String month, String amountDue, String amountPaid) {
        RentInvoiceAdvanceRow row = new RentInvoiceAdvanceRow(); row.setInvoiceId(id);
        row.setBillingMonth(LocalDate.parse(month)); row.setAmountDue(new BigDecimal(amountDue)); row.setAmountPaid(new BigDecimal(amountPaid));
        return row;
    }

    @Test void editsActiveLeaseAndSynchronizesUnpaidInvoiceTerms() {
        LeaseChangeContext lease = activeLease();
        when(mapper.lockLeaseForChange(44L)).thenReturn(lease);
        when(mapper.countActiveRentalMandate(8L, LocalDate.parse("2026-01-01"), LocalDate.parse("2027-03-31"))).thenReturn(1);
        when(mapper.countOtherOverlappingLease(44L, 8L, LocalDate.parse("2026-01-01"), LocalDate.parse("2027-03-31"))).thenReturn(0);
        when(mapper.updateLeaseTerms(44L, LocalDate.parse("2026-01-01"), LocalDate.parse("2027-03-31"),
                new BigDecimal("3300.00"), new BigDecimal("6600.00"), 8, "daily_prorated")).thenReturn(1);

        service.updateLease(1L, 44L, new AdminLeaseUpdateRequest(LocalDate.parse("2026-01-01"),
                LocalDate.parse("2027-03-31"), new BigDecimal("3300.00"), new BigDecimal("6600.00"), 8));

        verify(mapper).updateFutureUnpaidInvoiceTerms(44L, LocalDate.parse("2026-01-01"),
                new BigDecimal("3300.00"), 8, LocalDate.parse("2026-01-01"), LocalDate.parse("2027-03-31"), "daily_prorated");
        verify(mapper).insertInvoice(44L, LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-08"), new BigDecimal("3300.00"));
        verify(mapper).insertLeaseUpdateAudit(1L, 44L, lease.getStartDate(), lease.getEndDate(),
                lease.getMonthlyRent(), lease.getDepositAmount(), lease.getPaymentDay(), LocalDate.parse("2026-01-01"),
                LocalDate.parse("2027-03-31"), new BigDecimal("3300.00"), new BigDecimal("6600.00"), 8);
    }

    @Test void removesUnpaidInvoicesOutsideCorrectedLeasePeriod() {
        LeaseChangeContext lease = activeLease();
        when(mapper.lockLeaseForChange(44L)).thenReturn(lease);
        when(mapper.countActiveRentalMandate(8L, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-03-31"))).thenReturn(1);
        when(mapper.countOtherOverlappingLease(44L, 8L, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-03-31"))).thenReturn(0);
        when(mapper.updateLeaseTerms(44L, LocalDate.parse("2026-07-01"), LocalDate.parse("2027-03-31"),
                new BigDecimal("3300.00"), new BigDecimal("6600.00"), 8, "daily_prorated")).thenReturn(1);

        service.updateLease(1L, 44L, new AdminLeaseUpdateRequest(LocalDate.parse("2026-07-01"),
                LocalDate.parse("2027-03-31"), new BigDecimal("3300.00"), new BigDecimal("6600.00"), 8));

        verify(mapper).deleteUnpaidInvoicesOutsideLeasePeriod(44L, LocalDate.parse("2026-07-01"),
                LocalDate.parse("2027-03-01"));
    }

    @Test void exposesDepositBillsAccountsAndMoveOutSettlementStates() {
        DepositAccountRow pending = depositAccount(1L, "active", "pending", "0.00", "0.00");
        DepositAccountRow awaiting = depositAccount(2L, "expired", "confirmed", "6000.00", "6000.00");
        when(mapper.findDepositAccounts()).thenReturn(java.util.List.of(pending, awaiting));

        var response = service.findDepositAccounts(1, 20, null, null);

        assertThat(response.summary().pendingCollectionCount()).isEqualTo(1);
        assertThat(response.summary().awaitingSettlementCount()).isEqualTo(1);
        assertThat(response.summary().totalHeld()).isEqualByComparingTo("6000.00");
        assertThat(response.rows()).extracting(item -> item.accountStatus())
                .containsExactly("pending_collection", "awaiting_settlement");
    }

    @Test void returnsAllDepositBillsWithoutReplacingTheExistingReviewTarget() {
        var account = depositAccount(44L, "expired", "confirmed", "3500.00", "3500.00");
        when(mapper.findDepositAccount(44L)).thenReturn(account);
        when(mapper.findLeaseDepositTransactions(44L)).thenReturn(java.util.List.of());
        var first = new com.ccps.backend.dto.AdminDepositAccountDetailResponse.Bill(90L, "DEPOSIT-4000",
                new BigDecimal("4000.00"), LocalDate.parse("2026-08-01"), "confirmed", "confirmed", "paid");
        var second = new com.ccps.backend.dto.AdminDepositAccountDetailResponse.Bill(91L, "DEPOSIT-500",
                new BigDecimal("500.00"), LocalDate.parse("2026-08-02"), "confirmed", "confirmed", "paid");
        when(mapper.findDepositBills(44L)).thenReturn(java.util.List.of(second, first));
        var result = service.findDepositAccount(44L);
        assertThat(result.bills()).containsExactly(second, first);
        assertThat(result.bill().financeRecordId()).isEqualTo(account.getFinanceRecordId());
        assertThat(result.account().postedBalance()).isEqualByComparingTo("3500.00");
    }

    @Test void rejectsDepositSettlementWhileLeaseIsStillActive() {
        LeaseChangeContext lease = activeLease();
        when(mapper.lockLeaseForChange(44L)).thenReturn(lease);
        when(mapper.findDepositAccount(44L)).thenReturn(depositAccount(44L, "active", "confirmed", "6000.00", "6000.00"));
        when(mapper.findLeaseDepositBalance(44L)).thenReturn(new BigDecimal("6000.00"));

        assertThatThrownBy(() -> service.createTenantDepositTransaction(1L, 44L,
                new AdminTenantDepositTransactionRequest("refund", new BigDecimal("1000.00"),
                        LocalDate.parse("2026-07-19"), "提前返还")))
                .hasMessageContaining("after the lease has ended");
    }

    @Test void allowsDepositRefundWhenOwnerReserveWillBecomeNegative() {
        LeaseChangeContext lease = activeLease(); lease.setStatus("expired");
        DepositAccountRow account = depositAccount(44L, "expired", "confirmed", "6000.00", "6000.00");
        account.setReserveAccountId(30L); account.setReserveBalance(new BigDecimal("500.00"));
        when(mapper.lockLeaseForChange(44L)).thenReturn(lease);
        when(mapper.findDepositAccount(44L)).thenReturn(account);
        when(mapper.findLeaseDepositBalance(44L)).thenReturn(new BigDecimal("6000.00"));
        when(mapper.insertTenantDepositRefundFinance(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    var transaction = invocation.getArgument(0,
                            AdminTenancyMapper.NewTenantDepositTransaction.class);
                    transaction.setFinanceRecordId(91L);
                    return 1;
                });
        when(mapper.insertTenantDepositRefundCashflow(org.mockito.ArgumentMatchers.any())).thenReturn(1);
        when(mapper.insertTenantDepositTransaction(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    var transaction = invocation.getArgument(0,
                            AdminTenancyMapper.NewTenantDepositTransaction.class);
                    transaction.setId(92L);
                    return 1;
                });

        var result = service.createTenantDepositTransaction(1L, 44L,
                new AdminTenantDepositTransactionRequest("refund", new BigDecimal("1000.00"),
                        LocalDate.parse("2026-07-19"), "退租返还"));

        assertThat(result.id()).isEqualTo(92L);
        verify(mapper).insertTenantDepositRefundFinance(org.mockito.ArgumentMatchers.any());
    }

    @Test void softDeletesSelectedManualDepositTransactionsAndWritesAudit() {
        when(mapper.lockLeaseForChange(44L)).thenReturn(activeLease());
        when(mapper.findLeaseDepositTransactions(44L)).thenReturn(java.util.List.of(
                depositTransaction(1L, 80L, "collection", "credit", "1000.00", "2026-07-01"),
                depositTransaction(2L, null, "tenant_advance", "debit", "100.00", "2026-07-02")));
        when(mapper.cancelTenantDepositTransactions(44L, java.util.List.of(2L))).thenReturn(1);

        service.deleteTenantDepositTransactions(9L, 44L, java.util.List.of(2L));

        verify(mapper).cancelTenantDepositTransactions(44L, java.util.List.of(2L));
        verify(mapper).insertTenantDepositDeleteAudit(9L, 2L, 44L,
                "tenant_advance", "debit", new BigDecimal("100.00"));
    }

    @Test void rejectsDeletingFinanceLinkedDepositTransactions() {
        when(mapper.lockLeaseForChange(44L)).thenReturn(activeLease());
        when(mapper.findLeaseDepositTransactions(44L)).thenReturn(java.util.List.of(
                depositTransaction(1L, 80L, "collection", "credit", "1000.00", "2026-07-01")));

        assertThatThrownBy(() -> service.deleteTenantDepositTransactions(9L, 44L, java.util.List.of(1L)))
                .hasMessageContaining("Only manual deposit account transactions can be deleted");
    }

    @Test void rejectsDeleteThatWouldMakeAnEarlierDepositBalanceNegative() {
        when(mapper.lockLeaseForChange(44L)).thenReturn(activeLease());
        when(mapper.findLeaseDepositTransactions(44L)).thenReturn(java.util.List.of(
                depositTransaction(1L, 80L, "collection", "credit", "100.00", "2026-07-01"),
                depositTransaction(2L, null, "adjustment_credit", "credit", "100.00", "2026-07-02"),
                depositTransaction(3L, null, "tenant_advance", "debit", "150.00", "2026-07-03"),
                depositTransaction(4L, null, "tenant_repayment", "credit", "100.00", "2026-07-04")));

        assertThatThrownBy(() -> service.deleteTenantDepositTransactions(9L, 44L, java.util.List.of(2L)))
                .hasMessageContaining("deposit balance negative");
    }

    @Test void editsLeaseTenantAndUnitWithOperationalValidation() {
        LeaseChangeContext lease = activeLease();
        when(mapper.lockLeaseForChange(44L)).thenReturn(lease);
        when(mapper.countActiveTenant(9L)).thenReturn(1);
        when(mapper.countOperatingUnit(10L)).thenReturn(1);
        when(mapper.countActiveRentalMandate(10L, LocalDate.parse("2026-08-01"), LocalDate.parse("2027-06-30"))).thenReturn(1);
        when(mapper.countOtherOverlappingLease(44L, 10L, LocalDate.parse("2026-08-01"), LocalDate.parse("2027-06-30"))).thenReturn(0);
        when(mapper.updateLeasePartiesAndTerms(44L, 9L, 10L, LocalDate.parse("2026-08-01"), LocalDate.parse("2027-06-30"),
                new BigDecimal("3200.00"), new BigDecimal("6400.00"), 5, "daily_prorated")).thenReturn(1);

        service.updateLease(1L, 44L, new AdminLeaseUpdateRequest(LocalDate.parse("2026-08-01"),
                LocalDate.parse("2027-06-30"), new BigDecimal("3200.00"), new BigDecimal("6400.00"), 5,
                "daily_prorated", 9L, 10L));

        verify(mapper).markUnitAvailableIfNoActiveLease(8L);
        verify(mapper).activateRentalService(10L);
        verify(mapper).markUnitRented(10L);
        verify(mapper).insertLeasePartyUpdateAudit(1L, 44L, 5L, 8L,
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31"), new BigDecimal("3000.00"),
                new BigDecimal("6000.00"), 5, 9L, 10L, LocalDate.parse("2026-08-01"), LocalDate.parse("2027-06-30"),
                new BigDecimal("3200.00"), new BigDecimal("6400.00"), 5);
    }

    @Test void transfersLeaseByClosingOldLeaseAndCreatingNewLease() {
        LeaseChangeContext lease = activeLease();
        when(mapper.lockLeaseForChange(44L)).thenReturn(lease);
        when(mapper.countActiveTenant(9L)).thenReturn(1);
        when(mapper.countActiveRentalMandate(8L, LocalDate.parse("2026-08-01"), LocalDate.parse("2027-06-30"))).thenReturn(1);
        when(mapper.countOtherOverlappingLease(44L, 8L, LocalDate.parse("2026-08-01"), LocalDate.parse("2027-06-30"))).thenReturn(0);
        when(mapper.closeLeaseForTransfer(44L, LocalDate.parse("2026-07-31"))).thenReturn(1);
        when(mapper.insertLease(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            NewLease created = invocation.getArgument(0); created.setId(55L); return 1;
        });
        stubSecurityDepositWrites();

        Long newLeaseId = service.transferLease(1L, 44L, new AdminLeaseTransferRequest(9L,
                LocalDate.parse("2026-08-01"), LocalDate.parse("2027-06-30"),
                new BigDecimal("3200.00"), new BigDecimal("6400.00"), 5));

        assertThat(newLeaseId).isEqualTo(55L);
        verify(mapper).deleteOldFutureInvoices(44L, LocalDate.parse("2026-08-01"));
        verify(mapper).insertLeaseTransferAudit(1L, 44L, 55L, 5L, 9L,
                LocalDate.parse("2026-12-31"), LocalDate.parse("2026-08-01"));
    }

    @Test void closesLeaseForEarlyTerminationAndMakesUnitAvailable() {
        LeaseChangeContext lease = activeLease();
        when(mapper.lockLeaseForChange(44L)).thenReturn(lease);
        when(mapper.closeLease(44L, LocalDate.parse("2026-07-19"), "terminated")).thenReturn(1);

        service.closeLease(1L, 44L, new AdminLeaseCloseRequest(
                LocalDate.parse("2026-07-19"), "early_termination", "租客提前退租"));

        verify(mapper).deleteOldFutureInvoices(44L, LocalDate.parse("2026-08-01"));
        verify(mapper).markUnitAvailableIfNoActiveLease(8L);
        verify(mapper).insertLeaseClosureAudit(1L, 44L, LocalDate.parse("2026-12-31"),
                LocalDate.parse("2026-07-19"), "early_termination", "租客提前退租");
    }

    @Test void renewsTheSameLeaseByAppendingASecondPeriod() {
        LeaseChangeContext lease = activeLease();
        LeasePeriodRow latest = leasePeriod(1, "2026-01-01", "2026-12-31");
        when(mapper.lockLeaseForChange(44L)).thenReturn(lease);
        when(mapper.lockLatestLeasePeriod(44L)).thenReturn(latest);
        when(mapper.countActiveRentalMandate(8L, LocalDate.parse("2027-01-01"), LocalDate.parse("2027-12-31"))).thenReturn(1);
        when(mapper.extendLeaseForRenewal(44L, LocalDate.parse("2026-12-31"), LocalDate.parse("2027-12-31"),
                new BigDecimal("3200.00"), new BigDecimal("6000.00"), 5, "daily_prorated")).thenReturn(1);

        var result = service.renewLease(1L, 44L, new AdminLeaseRenewalRequest(LocalDate.parse("2027-01-01"),
                LocalDate.parse("2027-12-31"), new BigDecimal("3200.00"), new BigDecimal("6000.00"), 5,
                "daily_prorated"));

        assertThat(result.leaseId()).isEqualTo(44L);
        assertThat(result.periodNo()).isEqualTo(2);
        verify(mapper, org.mockito.Mockito.never()).insertLease(org.mockito.ArgumentMatchers.any(NewLease.class));
        verify(mapper).insertLeaseRenewalAudit(1L, 44L, result.id(), 2, LocalDate.parse("2026-12-31"),
                LocalDate.parse("2027-01-01"), LocalDate.parse("2027-12-31"), new BigDecimal("3200.00"),
                new BigDecimal("6000.00"), 5);
        verify(mapper, org.mockito.Mockito.never()).insertSecurityDepositFinance(
                org.mockito.ArgumentMatchers.any(NewSecurityDeposit.class));
    }

    @Test void renewalCreatesPendingFinanceRecordOnlyForTheIncreasedDepositAmount() {
        LeaseChangeContext lease = activeLease();
        lease.setDepositAmount(new BigDecimal("20000.00"));
        LeasePeriodRow latest = leasePeriod(1, "2026-01-01", "2026-12-31");
        latest.setDepositAmount(new BigDecimal("20000.00"));
        when(mapper.lockLeaseForChange(44L)).thenReturn(lease);
        when(mapper.lockLatestLeasePeriod(44L)).thenReturn(latest);
        when(mapper.countActiveRentalMandate(8L, LocalDate.parse("2027-01-01"), LocalDate.parse("2027-12-31"))).thenReturn(1);
        when(mapper.extendLeaseForRenewal(44L, LocalDate.parse("2026-12-31"), LocalDate.parse("2027-12-31"),
                new BigDecimal("3200.00"), new BigDecimal("25000.00"), 5, "daily_prorated")).thenReturn(1);
        stubSecurityDepositWrites();

        service.renewLease(1L, 44L, new AdminLeaseRenewalRequest(LocalDate.parse("2027-01-01"),
                LocalDate.parse("2027-12-31"), new BigDecimal("3200.00"), new BigDecimal("25000.00"), 5,
                "daily_prorated"));

        ArgumentCaptor<NewSecurityDeposit> depositCaptor = ArgumentCaptor.forClass(NewSecurityDeposit.class);
        verify(mapper).insertSecurityDepositFinance(depositCaptor.capture());
        assertThat(depositCaptor.getValue().getLeaseId()).isEqualTo(44L);
        assertThat(depositCaptor.getValue().getAmount()).isEqualByComparingTo("5000.00");
        assertThat(depositCaptor.getValue().getTransactionDate()).isEqualTo(LocalDate.parse("2027-01-01"));
        assertThat(depositCaptor.getValue().getDescription()).contains("续约补收租客押金");
        verify(mapper).insertSecurityDepositEntry(depositCaptor.getValue());
        verify(mapper).insertSecurityDepositCashflow(depositCaptor.getValue());
    }

    @Test void automaticallyExpiresLeaseAfterTwoFullGraceMonths() {
        AdminTenancyMapper.ExpiredLeaseRow lease = new AdminTenancyMapper.ExpiredLeaseRow();
        lease.setLeaseId(44L); lease.setUnitId(8L); lease.setEndDate(LocalDate.parse("2026-05-19"));
        when(mapper.findExpiredLeases(LocalDate.parse("2026-05-19"))).thenReturn(java.util.List.of(lease));
        when(mapper.expireLease(44L, LocalDate.parse("2026-05-19"))).thenReturn(1);

        assertThat(service.expireEndedLeases()).isEqualTo(1);

        verify(mapper).markUnitAvailableIfNoActiveLease(8L);
        verify(mapper).insertLeaseClosureAudit(null, 44L, LocalDate.parse("2026-05-19"),
                LocalDate.parse("2026-05-19"), "租约自然到期", "系统于租约结束满2个月宽限期后自动解约");
    }

    @Test void continuousRenewalAppendsTheNextPeriodNumber() {
        LeaseChangeContext lease = activeLease();
        lease.setEndDate(LocalDate.parse("2027-12-31"));
        when(mapper.lockLeaseForChange(44L)).thenReturn(lease);
        when(mapper.lockLatestLeasePeriod(44L)).thenReturn(leasePeriod(2, "2027-01-01", "2027-12-31"));
        when(mapper.countActiveRentalMandate(8L, LocalDate.parse("2028-01-01"), LocalDate.parse("2028-12-31"))).thenReturn(1);
        when(mapper.extendLeaseForRenewal(44L, LocalDate.parse("2027-12-31"), LocalDate.parse("2028-12-31"),
                new BigDecimal("3400.00"), new BigDecimal("6000.00"), 5, "daily_prorated")).thenReturn(1);

        var result = service.renewLease(1L, 44L, new AdminLeaseRenewalRequest(LocalDate.parse("2028-01-01"),
                LocalDate.parse("2028-12-31"), new BigDecimal("3400.00"), new BigDecimal("6000.00"), 5,
                "daily_prorated"));

        assertThat(result.periodNo()).isEqualTo(3);
    }

    @Test void rejectsRenewalThatLeavesAGapBetweenPeriods() {
        when(mapper.lockLeaseForChange(44L)).thenReturn(activeLease());
        when(mapper.lockLatestLeasePeriod(44L)).thenReturn(leasePeriod(1, "2026-01-01", "2026-12-31"));

        assertThatThrownBy(() -> service.renewLease(1L, 44L, new AdminLeaseRenewalRequest(
                LocalDate.parse("2027-01-02"), LocalDate.parse("2027-12-31"), new BigDecimal("3200.00"),
                new BigDecimal("6000.00"), 5, "daily_prorated")))
                .hasMessageContaining("day after");
    }

    @Test void rejectsLeaseWhenActiveRentalMandateIsMissing() {
        when(mapper.countActiveTenant(5L)).thenReturn(1);
        when(mapper.countActiveRentalMandate(8L, LocalDate.parse("2026-07-01"),
                LocalDate.parse("2027-06-30"))).thenReturn(0);

        assertThatThrownBy(() -> service.createLease(new AdminLeaseCreateRequest(5L, 8L,
                LocalDate.parse("2026-07-01"), LocalDate.parse("2027-06-30"),
                new BigDecimal("3000.00"), new BigDecimal("6000.00"), 5)))
                .hasMessageContaining("active rental mandate");
    }

    private AdminTenantDepositTransactionResponse depositTransaction(Long id, Long financeRecordId,
            String type, String direction, String amount, String occurredOn) {
        return new AdminTenantDepositTransactionResponse(id, 44L, "LEASE-44", 5L, 8L,
                "测试建案", "A-01", financeRecordId, type, direction, new BigDecimal(amount),
                BigDecimal.ZERO, LocalDate.parse(occurredOn), "测试记录", "posted");
    }

    private LeaseChangeContext activeLease() {
        LeaseChangeContext lease = new LeaseChangeContext();
        lease.setLeaseId(44L); lease.setUnitId(8L); lease.setTenantId(5L); lease.setLeaseNo("LEASE-44");
        lease.setStartDate(LocalDate.parse("2026-01-01")); lease.setEndDate(LocalDate.parse("2026-12-31"));
        lease.setMonthlyRent(new BigDecimal("3000.00")); lease.setDepositAmount(new BigDecimal("6000.00"));
        lease.setPaymentDay(5); lease.setStatus("active"); lease.setProjectName("測試建案"); lease.setUnitNo("A-01-01");
        return lease;
    }

    private LeasePeriodRow leasePeriod(int periodNo, String start, String end) {
        LeasePeriodRow period = new LeasePeriodRow();
        period.setId(700L + periodNo); period.setLeaseId(44L); period.setPeriodNo(periodNo);
        period.setStartDate(LocalDate.parse(start)); period.setEndDate(LocalDate.parse(end));
        period.setMonthlyRent(new BigDecimal("3000.00")); period.setDepositAmount(new BigDecimal("6000.00"));
        period.setPaymentDay(5); period.setRentCalculationMethod("daily_prorated");
        return period;
    }

    private DepositAccountRow depositAccount(Long leaseId, String leaseStatus, String confirmationStatus,
            String postedBalance, String availableBalance) {
        DepositAccountRow row = new DepositAccountRow();
        row.setLeaseId(leaseId); row.setLeaseNo("LEASE-" + leaseId); row.setTenantId(5L); row.setTenantName("测试租客");
        row.setProjectName("测试建案"); row.setUnitNo("A-01"); row.setLeaseStatus(leaseStatus);
        row.setStartDate(LocalDate.parse("2026-01-01")); row.setEndDate(LocalDate.parse("2026-06-30"));
        row.setExpectedDeposit(new BigDecimal("6000.00")); row.setBillAmount(new BigDecimal("6000.00"));
        row.setDepositEntryStatus(confirmationStatus); row.setConfirmationStatus(confirmationStatus);
        row.setPostedBalance(new BigDecimal(postedBalance)); row.setAvailableBalance(new BigDecimal(availableBalance));
        row.setPendingSettlementCount(0L); row.setReserveBalance(new BigDecimal("10000.00"));
        return row;
    }

    private void stubSecurityDepositWrites() {
        org.mockito.Mockito.lenient().when(mapper.insertSecurityDepositFinance(org.mockito.ArgumentMatchers.any(NewSecurityDeposit.class)))
                .thenAnswer(invocation -> { NewSecurityDeposit deposit = invocation.getArgument(0); deposit.setFinanceRecordId(900L); return 1; });
        org.mockito.Mockito.lenient().when(mapper.insertSecurityDepositEntry(org.mockito.ArgumentMatchers.any(NewSecurityDeposit.class))).thenReturn(1);
        org.mockito.Mockito.lenient().when(mapper.insertSecurityDepositCashflow(org.mockito.ArgumentMatchers.any(NewSecurityDeposit.class))).thenReturn(1);
    }

}
