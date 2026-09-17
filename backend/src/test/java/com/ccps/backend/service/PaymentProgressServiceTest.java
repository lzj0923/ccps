package com.ccps.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ccps.backend.dto.PaymentProgressResponse;
import com.ccps.backend.mapper.PaymentProgressMapper;
import com.ccps.backend.mapper.PaymentProgressMapper.Header;
import com.ccps.backend.mapper.PaymentProgressMapper.InstallmentRow;

@ExtendWith(MockitoExtension.class)
class PaymentProgressServiceTest {
    @Mock
    private PaymentProgressMapper mapper;

    private PaymentProgressService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-15T00:00:00Z"), ZoneId.of("UTC"));
        service = new PaymentProgressService(mapper, clock);
    }

    @Test
    void returnsOwnerScopedInstallmentsAndComputedSummary() {
        Header header = header(5L, 10L, "480000.00");
        InstallmentRow paid = installment(1L, 1, "Deposit", "2026-02-15", "120000.00", "120000.00");
        InstallmentRow current = installment(2L, 2, "Foundation", "2026-07-20", "120000.00", "0.00");
        current.setConfirmationStatus("rejected");
        current.setRejectionReason("付款憑證無法辨識");
        paid.setProofDocumentIds("101,102");
        current.setProofDocumentIds("201");
        InstallmentRow future = installment(3L, 3, "Structure", "2026-10-20", "240000.00", "0.00");

        when(mapper.findHeader(42L, 5L)).thenReturn(header);
        when(mapper.findInstallments(10L)).thenReturn(List.of(paid, current, future));

        PaymentProgressResponse result = service.getPaymentProgress(42L, 5L);

        assertThat(result.property().ownerUnitId()).isEqualTo(5L);
        assertThat(result.property().paymentStatus()).isEqualTo("due_soon");
        assertThat(result.summary().purchasePrice()).isEqualByComparingTo("480000.00");
        assertThat(result.summary().scheduledAmount()).isEqualByComparingTo("480000.00");
        assertThat(result.summary().paidAmount()).isEqualByComparingTo("120000.00");
        assertThat(result.summary().remainingAmount()).isEqualByComparingTo("360000.00");
        assertThat(result.summary().paidInstallmentCount()).isEqualTo(1);
        assertThat(result.summary().nextDueDate()).isEqualTo(LocalDate.parse("2026-07-20"));
        assertThat(result.installments()).extracting(PaymentProgressResponse.Installment::status)
                .containsExactly("paid", "current", "pending");
        assertThat(result.installments().get(1).rejectionReason()).isEqualTo("付款憑證無法辨識");
        assertThat(result.installments().get(0).proofDocumentIds()).containsExactly(101L, 102L);
        assertThat(result.installments().get(1).proofDocumentIds()).containsExactly(201L);
        assertThat(result.installments().get(2).proofDocumentIds()).isEmpty();
    }

    @Test
    void returnsNotConfiguredForOwnedPropertyWithoutPaymentPlan() {
        Header header = header(1L, null, null);
        when(mapper.findHeader(42L, 1L)).thenReturn(header);

        PaymentProgressResponse result = service.getPaymentProgress(42L, 1L);

        assertThat(result.property().paymentStatus()).isEqualTo("not_configured");
        assertThat(result.summary().totalInstallmentCount()).isZero();
        assertThat(result.summary().purchasePrice()).isZero();
        assertThat(result.installments()).isEmpty();
        assertThat(result.latestPayment()).isNull();
    }

    private Header header(Long ownerUnitId, Long planId, String purchasePrice) {
        Header header = new Header();
        header.setOwnerUnitId(ownerUnitId);
        header.setProjectName("ADMIN Test Central Suites");
        header.setUnitNo("ADMIN-B-0602");
        header.setOwnerName("Test Owner");
        header.setPhone("+60 12-345 6789");
        header.setContractNo(planId == null ? null : "TEST-C01");
        header.setPaymentPlanId(planId);
        header.setPlanName(planId == null ? null : "Construction Plan");
        header.setPurchasePrice(purchasePrice == null ? null : new BigDecimal(purchasePrice));
        header.setCurrency("MYR");
        header.setSignedDate(planId == null ? null : LocalDate.parse("2026-01-15"));
        return header;
    }

    private InstallmentRow installment(Long id, int no, String milestone, String dueDate,
            String amountDue, String amountPaid) {
        InstallmentRow row = new InstallmentRow();
        row.setId(id);
        row.setInstallmentNo(no);
        row.setMilestone(milestone);
        row.setDueDate(LocalDate.parse(dueDate));
        row.setAmountDue(new BigDecimal(amountDue));
        row.setAmountPaid(new BigDecimal(amountPaid));
        row.setUnpaidAmount(new BigDecimal(amountDue).subtract(new BigDecimal(amountPaid)));
        row.setStatus("pending");
        row.setReceiptCount(0);
        row.setHasProof(false);
        return row;
    }
}
