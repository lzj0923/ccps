package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminDashboardMapper {
    @Select("""
            SELECT base.region_name,base.unit_count,COALESCE(lease_data.occupied_count,0) occupied_count,
                   COALESCE(lease_data.total_rent,0) total_rent,COALESCE(lease_data.average_rent,0) average_rent,
                   COALESCE(deposit_data.tenant_deposit,0) tenant_deposit,
                   COALESCE(reserve_data.reserve_balance,0) reserve_balance
            FROM (
              SELECT COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区') region_name,COUNT(DISTINCT u.id) unit_count
              FROM units u JOIN projects p ON p.id=u.project_id
              JOIN owner_units ou ON ou.unit_id=u.id AND ou.status='active' AND ou.asset_stage='OPERATING'
              GROUP BY COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区')
            ) base
            LEFT JOIN (
              SELECT COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区') region_name,COUNT(DISTINCT l.unit_id) occupied_count,
                     SUM(l.monthly_rent) total_rent,AVG(l.monthly_rent) average_rent
              FROM leases l JOIN units u ON u.id=l.unit_id JOIN projects p ON p.id=u.project_id
              WHERE l.status='active' AND CURRENT_DATE BETWEEN l.start_date AND l.end_date
              GROUP BY COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区')
            ) lease_data ON lease_data.region_name=base.region_name
            LEFT JOIN (
              SELECT COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区') region_name,
                     SUM(CASE WHEN tdt.direction='credit' THEN tdt.amount ELSE -tdt.amount END) tenant_deposit
              FROM tenant_deposit_transactions tdt JOIN units u ON u.id=tdt.unit_id JOIN projects p ON p.id=u.project_id
              WHERE tdt.status IN ('posted','pending')
              GROUP BY COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区')
            ) deposit_data ON deposit_data.region_name=base.region_name
            LEFT JOIN (
              SELECT COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区') region_name,SUM(ra.current_balance) reserve_balance
              FROM reserve_accounts ra JOIN owner_units ou ON ou.id=ra.owner_unit_id
              JOIN units u ON u.id=ou.unit_id JOIN projects p ON p.id=u.project_id
              WHERE ra.status='active' AND ou.status='active' AND ou.asset_stage='OPERATING'
              GROUP BY COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区')
            ) reserve_data ON reserve_data.region_name=base.region_name
            ORDER BY base.unit_count DESC,base.region_name
            """)
    List<RegionRow> findRegions();

    @Select("""
            SELECT base.region_name,base.unit_count,COALESCE(lease_data.occupied_count,0) occupied_count,
                   COALESCE(lease_data.total_rent,0) total_rent,COALESCE(lease_data.average_rent,0) average_rent,
                   COALESCE(deposit_data.tenant_deposit,0) tenant_deposit,
                   COALESCE(reserve_data.reserve_balance,0) reserve_balance
            FROM (
              SELECT COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区') region_name,COUNT(DISTINCT u.id) unit_count
              FROM units u JOIN projects p ON p.id=u.project_id
              JOIN owner_units ou ON ou.unit_id=u.id AND ou.status='active' AND ou.asset_stage='OPERATING'
              GROUP BY COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区')
            ) base
            LEFT JOIN (
              SELECT COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区') region_name,COUNT(DISTINCT l.unit_id) occupied_count,
                     SUM(l.monthly_rent) total_rent,AVG(l.monthly_rent) average_rent
              FROM leases l JOIN units u ON u.id=l.unit_id JOIN projects p ON p.id=u.project_id
              WHERE l.status='active'
                AND l.start_date <= COALESCE(#{endDate}, CURRENT_DATE)
                AND l.end_date >= COALESCE(#{startDate}, CURRENT_DATE)
              GROUP BY COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区')
            ) lease_data ON lease_data.region_name=base.region_name
            LEFT JOIN (
              SELECT COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区') region_name,
                     SUM(CASE WHEN tdt.direction='credit' THEN tdt.amount ELSE -tdt.amount END) tenant_deposit
              FROM tenant_deposit_transactions tdt JOIN units u ON u.id=tdt.unit_id JOIN projects p ON p.id=u.project_id
              WHERE tdt.status IN ('posted','pending')
                AND tdt.occurred_on BETWEEN COALESCE(#{startDate}, tdt.occurred_on)
                                        AND COALESCE(#{endDate}, tdt.occurred_on)
              GROUP BY COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区')
            ) deposit_data ON deposit_data.region_name=base.region_name
            LEFT JOIN (
              SELECT COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区') region_name,SUM(ra.current_balance) reserve_balance
              FROM reserve_accounts ra JOIN owner_units ou ON ou.id=ra.owner_unit_id
              JOIN units u ON u.id=ou.unit_id JOIN projects p ON p.id=u.project_id
              WHERE ra.status='active' AND ou.status='active' AND ou.asset_stage='OPERATING'
              GROUP BY COALESCE(NULLIF(TRIM(p.state_name),''),NULLIF(TRIM(p.city),''),'未设置地区')
            ) reserve_data ON reserve_data.region_name=base.region_name
            ORDER BY base.unit_count DESC,base.region_name
            """)
    List<RegionRow> findRegionsByDateRange(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    class RegionRow {
        private String regionName; private Long unitCount,occupiedCount;
        private BigDecimal totalRent,averageRent,tenantDeposit,reserveBalance;
        public String getRegionName(){return regionName;} public void setRegionName(String v){regionName=v;}
        public Long getUnitCount(){return unitCount;} public void setUnitCount(Long v){unitCount=v;}
        public Long getOccupiedCount(){return occupiedCount;} public void setOccupiedCount(Long v){occupiedCount=v;}
        public BigDecimal getTotalRent(){return totalRent;} public void setTotalRent(BigDecimal v){totalRent=v;}
        public BigDecimal getAverageRent(){return averageRent;} public void setAverageRent(BigDecimal v){averageRent=v;}
        public BigDecimal getTenantDeposit(){return tenantDeposit;} public void setTenantDeposit(BigDecimal v){tenantDeposit=v;}
        public BigDecimal getReserveBalance(){return reserveBalance;} public void setReserveBalance(BigDecimal v){reserveBalance=v;}
    }
}
