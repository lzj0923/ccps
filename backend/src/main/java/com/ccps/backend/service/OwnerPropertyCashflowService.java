package com.ccps.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.OwnerPropertyCashflowResponse;
import com.ccps.backend.dto.OwnerPropertyCashflowResponse.CashflowItem;
import com.ccps.backend.mapper.OwnerPropertyCashflowMapper;
import com.ccps.backend.mapper.OwnerPropertyCashflowMapper.CashflowRow;
import com.ccps.backend.mapper.OwnerPropertyCashflowMapper.PropertyContext;

@Service
public class OwnerPropertyCashflowService {
    private final OwnerPropertyCashflowMapper mapper;

    public OwnerPropertyCashflowService(OwnerPropertyCashflowMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public OwnerPropertyCashflowResponse list(Long userId, Long ownerUnitId) {
        if (ownerUnitId == null || ownerUnitId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid owner unit");
        }
        PropertyContext property = mapper.findProperty(userId, ownerUnitId);
        if (property == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        }

        List<CashflowRow> rows = new ArrayList<>();
        rows.addAll(safe(mapper.findCashflows(property.getUnitId(), property.getOwnerId())));
        rows.addAll(safe(mapper.findReserveTransactions(property.getOwnerUnitId())));
        List<CashflowItem> records = withRunningBalances(rows);

        return new OwnerPropertyCashflowResponse(property.getOwnerUnitId(), property.getProjectName(),
                property.getUnitNo(), records);
    }

    private List<CashflowItem> withRunningBalances(List<CashflowRow> rows) {
        rows.sort(Comparator.comparing(this::occurredAt)
                .thenComparing(CashflowRow::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(row -> row.getSource() == null ? "" : row.getSource()));

        BigDecimal runningBalance = BigDecimal.ZERO;
        List<CashflowItem> records = new ArrayList<>(rows.size());
        for (CashflowRow row : rows) {
            BigDecimal amount = zero(row.getAmount());
            runningBalance = "income".equals(row.getDirection())
                    ? runningBalance.add(amount)
                    : runningBalance.subtract(amount);
            records.add(toItem(row, runningBalance));
        }
        Collections.reverse(records);
        return records;
    }

    private LocalDateTime occurredAt(CashflowRow row) {
        if (row.getOccurredAt() != null) {
            return row.getOccurredAt();
        }
        LocalDate occurredOn = row.getOccurredOn();
        return occurredOn == null ? LocalDate.MIN.atStartOfDay() : occurredOn.atStartOfDay();
    }

    private CashflowItem toItem(CashflowRow row, BigDecimal balanceAfter) {
        String source = row.getSource() == null ? "cashflow" : row.getSource();
        String direction = "income".equals(row.getDirection()) ? "income" : "expense";
        return new CashflowItem(source + "-" + row.getId(), row.getId(), source, direction,
                row.getCategory(), row.getDescription(), zero(row.getAmount()), row.getOccurredOn(), row.getStatus(),
                balanceAfter);
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<CashflowRow> safe(List<CashflowRow> value) {
        return value == null ? List.of() : value;
    }
}
