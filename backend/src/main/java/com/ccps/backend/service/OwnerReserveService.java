package com.ccps.backend.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.OwnerReserveResponse;
import com.ccps.backend.dto.OwnerReserveResponse.Summary;
import com.ccps.backend.dto.OwnerReserveResponse.TransactionItem;
import com.ccps.backend.mapper.OwnerReserveMapper;
import com.ccps.backend.mapper.OwnerReserveMapper.SummaryRow;

@Service
public class OwnerReserveService {
    private static final Set<String> TYPES = Set.of("topup", "debit", "adjustment");
    private final OwnerReserveMapper mapper;
    private final Clock clock;

    @Autowired
    public OwnerReserveService(OwnerReserveMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    OwnerReserveService(OwnerReserveMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OwnerReserveResponse getReserve(Long userId, Long projectId, String requestedType,
            LocalDate requestedStartDate, LocalDate requestedEndDate) {
        LocalDate today = LocalDate.now(clock);
        LocalDate startDate = requestedStartDate == null ? today.minusMonths(6).withDayOfMonth(1) : requestedStartDate;
        LocalDate inclusiveEndDate = requestedEndDate == null ? today : requestedEndDate;
        String type = blankToNull(requestedType);
        validate(projectId, type, startDate, inclusiveEndDate);
        LocalDate endDate = inclusiveEndDate.plusDays(1);

        SummaryRow row = mapper.findSummary(userId);
        if (row == null) row = new SummaryRow();
        Summary summary = new Summary(
                zero(row.getTotalBalance()), zero(row.getAccountingBalance()), zero(row.getMinimumBalance()), zero(row.getTotalTopups()),
                integer(row.getTopupCount()), zero(row.getTotalDebits()), integer(row.getDebitCount()),
                integer(row.getLowBalanceCount()), integer(row.getAccountCount()));

        List<TransactionItem> transactions = new ArrayList<>(safe(mapper.findTransactions(
                userId, projectId, type, startDate, endDate)));
        if (type == null || "topup".equals(type)) {
            transactions.addAll(safe(mapper.findPendingTopups(userId, projectId, startDate, endDate)));
        }
        transactions.sort(Comparator.comparing(TransactionItem::occurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(
                        TransactionItem::id, Comparator.nullsLast(Comparator.reverseOrder())));

        return new OwnerReserveResponse(
                summary,
                safe(mapper.findAccounts(userId)),
                safe(mapper.findProperties(userId)),
                transactions,
                safe(mapper.findNotifications(userId)),
                safe(mapper.findDocuments(userId)));
    }

    private void validate(Long projectId, String type, LocalDate startDate, LocalDate endDate) {
        if (projectId != null && projectId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid project");
        }
        if (type != null && !TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reserve transaction type");
        }
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }
        if (endDate.isAfter(startDate.plusYears(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date range cannot exceed five years");
        }
    }

    private BigDecimal zero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private int integer(Integer value) { return value == null ? 0 : value; }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private <T> List<T> safe(List<T> value) { return value == null ? List.of() : value; }
}
