package com.ccps.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.OwnerRentIncomeResponse;
import com.ccps.backend.dto.RentReceiptConfirmationResponse;
import com.ccps.backend.dto.OwnerRentIncomeResponse.Summary;
import com.ccps.backend.dto.OwnerRentIncomeResponse.YearlyTrend;
import com.ccps.backend.mapper.OwnerRentIncomeMapper;
import com.ccps.backend.mapper.OwnerRentIncomeMapper.PeriodTotals;

@Service
public class OwnerRentIncomeService {
    private static final Set<String> ALLOWED_STATUSES = Set.of("paid", "partial", "unpaid", "overdue");

    private final OwnerRentIncomeMapper mapper;
    private final Clock clock;

    @Autowired
    public OwnerRentIncomeService(OwnerRentIncomeMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    OwnerRentIncomeService(OwnerRentIncomeMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OwnerRentIncomeResponse getRentIncome(
            Long userId, Integer requestedYear, Integer requestedMonth, Long projectId, String status) {
        LocalDate today = LocalDate.now(clock);
        int year = requestedYear == null ? today.getYear() : requestedYear;
        int month = requestedMonth == null ? today.getMonthValue() : requestedMonth;
        validateFilters(year, month, projectId, status);

        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate previousMonthStart = monthStart.minusMonths(1);
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate previousYearStart = yearStart.minusYears(1);
        LocalDate selectedMonthEnd = monthStart.plusMonths(1);

        PeriodTotals currentMonth = totals(userId, projectId, monthStart, selectedMonthEnd);
        PeriodTotals previousMonth = totals(userId, projectId, previousMonthStart, monthStart);
        PeriodTotals currentYear = totals(userId, projectId, yearStart, selectedMonthEnd);
        PeriodTotals previousYear = totals(userId, projectId, previousYearStart, selectedMonthEnd.minusYears(1));

        Summary summary = new Summary(
                year,
                month,
                zero(currentMonth.getAmountDue()),
                zero(currentMonth.getAmountPaid()),
                zero(currentMonth.getUnpaidAmount()),
                zero(currentYear.getAmountPaid()),
                percentChange(currentMonth.getAmountDue(), previousMonth.getAmountDue()),
                percentChange(currentMonth.getAmountPaid(), previousMonth.getAmountPaid()),
                percentChange(currentMonth.getUnpaidAmount(), previousMonth.getUnpaidAmount()),
                percentChange(currentYear.getAmountPaid(), previousYear.getAmountPaid()));

        List<YearlyTrend> yearlyTrend = mapper.findYearlyTrend(userId, projectId);
        List<Integer> availableYears = availableYears(year, yearlyTrend);
        return new OwnerRentIncomeResponse(
                summary,
                mapper.findProperties(userId),
                availableYears,
                yearlyTrend,
                mapper.findRecords(userId, year, month, projectId, emptyToNull(status)),
                mapper.findRecentReceipts(userId, projectId));
    }

    @Transactional
    public RentReceiptConfirmationResponse confirmReceipt(Long userId, Long invoiceId) {
        if (invoiceId == null || invoiceId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rent invoice");
        }
        int confirmed = mapper.confirmPendingReceipt(userId, invoiceId);
        if (confirmed == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No pending rent receipt is available for confirmation");
        }
        return new RentReceiptConfirmationResponse(invoiceId, confirmed);
    }

    private PeriodTotals totals(Long userId, Long projectId, LocalDate startDate, LocalDate endDate) {
        PeriodTotals result = mapper.findPeriodTotals(userId, projectId, startDate, endDate);
        return result == null ? new PeriodTotals() : result;
    }

    private void validateFilters(int year, int month, Long projectId, String status) {
        if (year < 2000 || year > 2100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Year must be between 2000 and 2100");
        }
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must be between 1 and 12");
        }
        if (projectId != null && projectId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid project");
        }
        String normalizedStatus = emptyToNull(status);
        if (normalizedStatus != null && !ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid rent status");
        }
    }

    private List<Integer> availableYears(int selectedYear, List<YearlyTrend> yearlyTrend) {
        Set<Integer> years = new HashSet<>();
        years.add(selectedYear);
        yearlyTrend.forEach(item -> years.add(item.year()));
        List<Integer> result = new ArrayList<>(years);
        result.sort(Comparator.reverseOrder());
        return result;
    }

    private BigDecimal percentChange(BigDecimal current, BigDecimal previous) {
        BigDecimal currentValue = zero(current);
        BigDecimal previousValue = zero(previous);
        if (previousValue.signum() == 0) {
            return currentValue.signum() == 0 ? BigDecimal.ZERO : new BigDecimal("100.00");
        }
        return currentValue.subtract(previousValue)
                .multiply(new BigDecimal("100"))
                .divide(previousValue, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
