package com.ccps.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.MaintenanceDetailResponse;
import com.ccps.backend.dto.OwnerExpenseMaintenanceResponse;
import com.ccps.backend.dto.OwnerExpenseMaintenanceResponse.Summary;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper.CashflowTotals;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper.ExpenseTotals;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper.MaintenanceHeader;
import com.ccps.backend.mapper.OwnerExpenseMaintenanceMapper.ReserveTotals;

@Service
public class OwnerExpenseMaintenanceService {
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "submitted", "assigned", "in_progress", "inspection", "completed", "cancelled");

    private final OwnerExpenseMaintenanceMapper mapper;
    private final Clock clock;

    @Autowired
    public OwnerExpenseMaintenanceService(OwnerExpenseMaintenanceMapper mapper) {
        this(mapper, Clock.systemDefaultZone());
    }

    OwnerExpenseMaintenanceService(OwnerExpenseMaintenanceMapper mapper, Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public OwnerExpenseMaintenanceResponse getOverview(Long userId, Long projectId, String category,
            String status, LocalDate requestedStartDate, LocalDate requestedEndDate) {
        LocalDate today = LocalDate.now(clock);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1);
        LocalDate startDate = requestedStartDate == null ? monthStart : requestedStartDate;
        LocalDate inclusiveEndDate = requestedEndDate == null ? monthEnd.minusDays(1) : requestedEndDate;
        validateFilters(projectId, status, startDate, inclusiveEndDate);
        LocalDate endDate = inclusiveEndDate.plusDays(1);

        ExpenseTotals current = totals(userId, projectId, monthStart, monthEnd);
        ExpenseTotals previous = totals(userId, projectId, monthStart.minusMonths(1), monthStart);
        CashflowTotals lifetime = cashflowTotals(userId, projectId);
        ReserveTotals reserve = reserveTotals(userId, projectId, monthStart, monthEnd);
        Summary summary = new Summary(
                zero(current.getExpenseAmount()),
                zero(lifetime.getIncomeAmount()),
                zero(lifetime.getExpenseAmount()),
                percentChange(current.getExpenseAmount(), previous.getExpenseAmount()),
                zero(current.getMaintenanceAmount()),
                percentChange(current.getMaintenanceAmount(), previous.getMaintenanceAmount()),
                zero(reserve.getAmount()),
                reserve.getEntryCount() == null ? 0 : reserve.getEntryCount(),
                mapper.countPendingMaintenance(userId, projectId));

        String normalizedCategory = emptyToNull(category);
        String normalizedStatus = emptyToNull(status);
        return new OwnerExpenseMaintenanceResponse(
                summary,
                mapper.findProperties(userId),
                mapper.findCategories(userId),
                mapper.findExpenses(userId, projectId, normalizedCategory, startDate, endDate),
                mapper.findMaintenance(userId, projectId, normalizedCategory, normalizedStatus, startDate, endDate));
    }

    @Transactional(readOnly = true)
    public MaintenanceDetailResponse getMaintenanceDetail(Long userId, Long workOrderId) {
        if (workOrderId == null || workOrderId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid maintenance work order");
        }
        MaintenanceHeader header = mapper.findMaintenanceHeader(userId, workOrderId);
        if (header == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Maintenance work order not found");
        }
        return new MaintenanceDetailResponse(
                header.getId(), header.getWorkOrderNo(), header.getProjectName(), header.getUnitNo(),
                header.getCategory(), header.getTitle(), header.getDescription(), header.getVendorName(),
                header.getRequestedAt(), header.getCompletedAt(), header.getStatus(),
                zero(header.getEstimatedAmount()), zero(header.getActualAmount()),
                zero(header.getReserveDeductedAmount()), header.getPaymentStatus(),
                header.getConfirmationStatus(), header.getPaymentMethod(), header.getPaymentDate(),
                safeList(mapper.findStatusHistory(workOrderId)), safeList(mapper.findAttachments(workOrderId)));
    }

    private ExpenseTotals totals(Long userId, Long projectId, LocalDate start, LocalDate end) {
        ExpenseTotals result = mapper.findTotals(userId, projectId, start, end);
        return result == null ? new ExpenseTotals() : result;
    }

    private CashflowTotals cashflowTotals(Long userId, Long projectId) {
        CashflowTotals result = mapper.findCashflowTotals(userId, projectId);
        return result == null ? new CashflowTotals() : result;
    }

    private ReserveTotals reserveTotals(Long userId, Long projectId, LocalDate start, LocalDate end) {
        ReserveTotals result = mapper.findReserveTotals(userId, projectId, start, end);
        return result == null ? new ReserveTotals() : result;
    }

    private void validateFilters(Long projectId, String status, LocalDate startDate, LocalDate endDate) {
        if (projectId != null && projectId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid project");
        }
        String normalizedStatus = emptyToNull(status);
        if (normalizedStatus != null && !ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid maintenance status");
        }
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }
        if (startDate.isBefore(LocalDate.of(2000, 1, 1)) || endDate.isAfter(LocalDate.of(2100, 12, 31))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date range is outside the supported period");
        }
        if (endDate.isAfter(startDate.plusYears(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date range cannot exceed five years");
        }
    }

    private BigDecimal percentChange(BigDecimal current, BigDecimal previous) {
        BigDecimal currentValue = zero(current);
        BigDecimal previousValue = zero(previous);
        if (previousValue.signum() == 0) {
            return currentValue.signum() == 0 ? BigDecimal.ZERO : new BigDecimal("100.00");
        }
        return currentValue.subtract(previousValue).multiply(new BigDecimal("100"))
                .divide(previousValue, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private <T> List<T> safeList(List<T> value) {
        return value == null ? List.of() : value;
    }
}
