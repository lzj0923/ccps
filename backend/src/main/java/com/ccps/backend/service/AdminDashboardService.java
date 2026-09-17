package com.ccps.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ccps.backend.dto.AdminDashboardResponse;
import com.ccps.backend.mapper.AdminDashboardMapper;
import com.ccps.backend.mapper.AdminDashboardMapper.RegionRow;

@Service
public class AdminDashboardService {
    private final AdminDashboardMapper mapper;
    public AdminDashboardService(AdminDashboardMapper mapper) { this.mapper = mapper; }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard() {
        return response(mapper.findRegions());
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must not be before start date");
        }
        if (startDate == null && endDate == null) return dashboard();
        return response(mapper.findRegionsByDateRange(startDate, endDate));
    }

    private AdminDashboardResponse response(List<RegionRow> rows) {
        List<AdminDashboardResponse.Region> regions = rows.stream().map(this::toRegion).toList();
        long units = regions.stream().mapToLong(AdminDashboardResponse.Region::unitCount).sum();
        long occupied = regions.stream().mapToLong(AdminDashboardResponse.Region::occupiedCount).sum();
        BigDecimal totalRent = regions.stream().map(AdminDashboardResponse.Region::totalRent).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deposit = regions.stream().map(AdminDashboardResponse.Region::tenantDeposit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal reserve = regions.stream().map(AdminDashboardResponse.Region::reserveBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averageRent = occupied == 0 ? BigDecimal.ZERO : totalRent.divide(BigDecimal.valueOf(occupied), 2, RoundingMode.HALF_UP);
        return new AdminDashboardResponse(new AdminDashboardResponse.Summary(units, occupied, rate(occupied, units),
                totalRent, averageRent, deposit, reserve), regions);
    }

    private AdminDashboardResponse.Region toRegion(RegionRow row) {
        long units = row.getUnitCount() == null ? 0 : row.getUnitCount();
        long occupied = row.getOccupiedCount() == null ? 0 : row.getOccupiedCount();
        return new AdminDashboardResponse.Region(row.getRegionName(), units, occupied, rate(occupied, units),
                zero(row.getTotalRent()), zero(row.getAverageRent()), zero(row.getTenantDeposit()), zero(row.getReserveBalance()));
    }
    private BigDecimal rate(long occupied,long units){return units==0?BigDecimal.ZERO:BigDecimal.valueOf(occupied*100d/units).setScale(1,RoundingMode.HALF_UP);}
    private BigDecimal zero(BigDecimal value){return value==null?BigDecimal.ZERO:value;}
}
