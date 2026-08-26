package com.ccps.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.mapper.ReserveTargetPolicyMapper;
import com.ccps.backend.mapper.ReserveTargetPolicyMapper.TargetContext;

@Service
public class ReserveTargetPolicyService {
    static final int EXPENSE_BUFFER_MONTHS = 3;

    private final ReserveTargetPolicyMapper mapper;

    public ReserveTargetPolicyService(ReserveTargetPolicyMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<Long> activeAccountIds() {
        return mapper.findActiveAccountIds();
    }

    @Transactional
    public void recalculate(Long accountId) {
        TargetContext row = mapper.lockTarget(accountId);
        if (row == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserve account is not active");
        }
        BigDecimal rent = money(row.getMonthlyRent());
        int trackedMonths = Math.max(1, row.getTrackedMonths() == null ? 1 : row.getTrackedMonths());
        BigDecimal average = money(row.getExpenseTotal()).divide(BigDecimal.valueOf(trackedMonths), 2,
                RoundingMode.HALF_UP);
        BigDecimal twoMonthManagementFee = money(row.getBuildingManagementFee()).multiply(BigDecimal.valueOf(2));
        BigDecimal calculated = twoMonthManagementFee.setScale(2, RoundingMode.HALF_UP);
        if (mapper.updateCalculation(accountId, calculated, rent, average, EXPENSE_BUFFER_MONTHS) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Reserve target changed while recalculating");
        }
        BigDecimal before = money(row.getMinimumBalance());
        boolean calculationChanged = money(row.getCalculatedMinimumBalance()).compareTo(calculated) != 0
                || money(row.getRentBufferAmount()).compareTo(rent) != 0
                || money(row.getMonthlyExpenseAverage()).compareTo(average) != 0
                || !Integer.valueOf(EXPENSE_BUFFER_MONTHS).equals(row.getExpenseBufferMonths());
        if (calculationChanged) {
            mapper.insertCalculationAudit(accountId, before, row.getMinimumBalanceMode(), calculated,
                    rent, average, EXPENSE_BUFFER_MONTHS);
        }
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
