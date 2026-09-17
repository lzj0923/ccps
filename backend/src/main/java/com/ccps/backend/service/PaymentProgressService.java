package com.ccps.backend.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.PaymentProgressResponse;
import com.ccps.backend.dto.PaymentProgressResponse.Installment;
import com.ccps.backend.dto.PaymentProgressResponse.LatestPayment;
import com.ccps.backend.dto.PaymentProgressResponse.Property;
import com.ccps.backend.dto.PaymentProgressResponse.Summary;
import com.ccps.backend.mapper.PaymentProgressMapper;
import com.ccps.backend.mapper.PaymentProgressMapper.Header;
import com.ccps.backend.mapper.PaymentProgressMapper.InstallmentRow;
import com.ccps.backend.mapper.PaymentProgressMapper.LatestPaymentRow;

@Service
public class PaymentProgressService {
    private final PaymentProgressMapper mapper;
    private final Clock clock;

    @Autowired
    public PaymentProgressService(PaymentProgressMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    PaymentProgressService(PaymentProgressMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PaymentProgressResponse getPaymentProgress(Long userId, Long ownerUnitId) {
        Header header = mapper.findHeader(userId, ownerUnitId);
        if (header == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        }

        List<InstallmentRow> rows = header.getPaymentPlanId() == null
                ? List.of()
                : mapper.findInstallments(header.getPaymentPlanId());
        LocalDate today = LocalDate.now(clock);

        BigDecimal scheduledAmount = rows.stream()
                .map(row -> zero(row.getAmountDue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidAmount = rows.stream()
                .map(row -> zero(row.getAmountPaid()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal purchasePrice = zero(header.getPurchasePrice());
        BigDecimal remainingAmount = purchasePrice.subtract(paidAmount).max(BigDecimal.ZERO);
        InstallmentRow nextDue = rows.stream()
                .filter(row -> zero(row.getAmountPaid()).compareTo(zero(row.getAmountDue())) < 0)
                .findFirst()
                .orElse(null);
        int paidInstallmentCount = (int) rows.stream()
                .filter(row -> zero(row.getAmountDue()).signum() > 0
                        && zero(row.getAmountPaid()).compareTo(zero(row.getAmountDue())) >= 0)
                .count();
        String paymentStatus = paymentStatus(rows, purchasePrice, paidAmount, nextDue, today);

        List<Installment> installments = new ArrayList<>(rows.size());
        for (InstallmentRow row : rows) {
            BigDecimal amountDue = zero(row.getAmountDue());
            BigDecimal rowPaid = zero(row.getAmountPaid());
            String status = installmentStatus(row, nextDue, today);
            installments.add(new Installment(
                    row.getId(),
                    row.getInstallmentNo() == null ? 0 : row.getInstallmentNo(),
                    row.getMilestone(),
                    row.getDueDate(),
                    amountDue,
                    rowPaid,
                    amountDue.subtract(rowPaid).max(BigDecimal.ZERO),
                    status,
                    row.getPaymentDate(),
                    row.getConfirmationStatus(),
                    row.getRejectionReason(),
                    row.getReceiptCount() == null ? 0 : row.getReceiptCount(),
                    Boolean.TRUE.equals(row.getHasProof()),
                    parseDocumentIds(row.getProofDocumentIds())));
        }

        Property property = new Property(
                header.getOwnerUnitId(), header.getProjectName(), header.getUnitNo(),
                header.getOwnerName(), header.getPhone(), header.getContractNo(),
                header.getPlanName(), header.getSignedDate(),
                header.getCurrency() == null ? "MYR" : header.getCurrency(), paymentStatus);
        Summary summary = new Summary(
                purchasePrice, scheduledAmount, paidAmount, remainingAmount,
                paidInstallmentCount, rows.size(),
                nextDue == null ? null : nextDue.getDueDate(),
                nextDue == null ? BigDecimal.ZERO : zero(nextDue.getAmountDue()).subtract(zero(nextDue.getAmountPaid())).max(BigDecimal.ZERO));

        return new PaymentProgressResponse(property, summary, installments, latestPayment(header.getPaymentPlanId()));
    }

    private LatestPayment latestPayment(Long paymentPlanId) {
        if (paymentPlanId == null) return null;
        LatestPaymentRow row = mapper.findLatestPayment(paymentPlanId);
        if (row == null) return null;
        return new LatestPayment(row.getReceiptId(), row.getReceiptNo(), row.getPaymentDate(),
                row.getPaymentMethod(), row.getConfirmationStatus(),
                "rejected".equals(row.getConfirmationStatus()) ? row.getReviewNote() : null,
                row.getProofDocumentId());
    }

    private String paymentStatus(List<InstallmentRow> rows, BigDecimal purchasePrice,
            BigDecimal paidAmount, InstallmentRow nextDue, LocalDate today) {
        if (rows.isEmpty()) return "not_configured";
        if (purchasePrice.signum() > 0 && paidAmount.compareTo(purchasePrice) >= 0) return "paid";
        boolean overdue = rows.stream().anyMatch(row ->
                zero(row.getAmountPaid()).compareTo(zero(row.getAmountDue())) < 0
                        && row.getDueDate() != null && row.getDueDate().isBefore(today));
        if (overdue) return "overdue";
        if (nextDue != null && nextDue.getDueDate() != null
                && !nextDue.getDueDate().isAfter(today.plusDays(30))) return "due_soon";
        return "paying";
    }

    private String installmentStatus(InstallmentRow row, InstallmentRow nextDue, LocalDate today) {
        if (zero(row.getAmountDue()).signum() > 0
                && zero(row.getAmountPaid()).compareTo(zero(row.getAmountDue())) >= 0) return "paid";
        if (row.getDueDate() != null && row.getDueDate().isBefore(today)) return "overdue";
        if (nextDue != null && row.getId().equals(nextDue.getId())) return "current";
        return "pending";
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<Long> parseDocumentIds(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split(",")).map(String::trim)
                .filter(id -> id.matches("[0-9]+"))
                .map(Long::valueOf).distinct().toList();
    }
}
