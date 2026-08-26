package com.ccps.backend.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.ccps.backend.dto.AdminRentalSpaceResponse;

@Mapper
public interface AdminRentalSpaceMapper {
    @Select("SELECT rental_mode FROM units WHERE id=#{unitId}")
    String findRentalMode(@Param("unitId") Long unitId);

    @Select("""
            SELECT rs.id,rs.unit_id,rs.space_code,rs.space_name,rs.space_type,rs.capacity,
                   rs.area_sqm,rs.recommended_rent,rs.status,
                   l.id AS current_lease_id,l.lease_no AS current_lease_no,t.full_name AS tenant_name,
                   l.start_date AS lease_start,l.end_date AS lease_end
            FROM rental_spaces rs
            LEFT JOIN leases l ON l.id=(
              SELECT l2.id FROM leases l2
              WHERE l2.rental_space_id=rs.id AND l2.status='active'
                AND CURRENT_DATE BETWEEN l2.start_date AND l2.end_date
              ORDER BY l2.start_date DESC,l2.id DESC LIMIT 1)
            LEFT JOIN tenants t ON t.id=l.tenant_id
            WHERE rs.unit_id=#{unitId}
            ORDER BY CASE rs.space_type WHEN 'whole_unit' THEN 0 ELSE 1 END,rs.sort_order,rs.id
            """)
    List<AdminRentalSpaceResponse> findByUnit(@Param("unitId") Long unitId);

    @Select("SELECT COUNT(*) FROM units WHERE id=#{unitId}")
    int countUnit(@Param("unitId") Long unitId);

    @Insert("""
            INSERT INTO rental_spaces
              (unit_id, space_code, space_name, space_type, capacity, area_sqm, recommended_rent, status, sort_order)
            SELECT id, 'WHOLE', '整套房产', 'whole_unit', 1, area_sqm, NULL, 'active', 0
            FROM units
            WHERE id = #{unitId}
              AND NOT EXISTS (
                SELECT 1 FROM rental_spaces
                WHERE unit_id = #{unitId} AND space_type = 'whole_unit'
              )
            """)
    int ensureWholeUnitSpace(@Param("unitId") Long unitId);

    @Select("SELECT COUNT(*) FROM rental_spaces WHERE unit_id=#{unitId} AND UPPER(space_code)=UPPER(#{code}) AND id<>COALESCE(#{excludeId},0)")
    int countCode(@Param("unitId") Long unitId,@Param("code") String code,@Param("excludeId") Long excludeId);

    @Select("SELECT COUNT(*) FROM leases WHERE unit_id=#{unitId} AND status='active' AND CURRENT_DATE<=end_date AND rental_space_id IN (SELECT id FROM rental_spaces WHERE unit_id=#{unitId} AND space_type=#{spaceType})")
    int countOpenLeasesByType(@Param("unitId") Long unitId,@Param("spaceType") String spaceType);

    @Select("SELECT COUNT(*) FROM leases WHERE rental_space_id=#{spaceId} AND status='active' AND CURRENT_DATE<=end_date")
    int countOpenLeases(@Param("spaceId") Long spaceId);

    @Insert("""
            INSERT INTO rental_spaces(unit_id,space_code,space_name,space_type,capacity,area_sqm,recommended_rent,status,sort_order)
            VALUES(#{unitId},#{spaceCode},#{spaceName},'room',#{capacity},#{areaSqm},#{recommendedRent},#{status},
                   (SELECT COALESCE(MAX(x.sort_order),0)+1 FROM rental_spaces x WHERE x.unit_id=#{unitId}))
            """)
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insert(Room room);

    @Update("""
            UPDATE rental_spaces SET space_code=#{spaceCode},space_name=#{spaceName},capacity=#{capacity},
              area_sqm=#{areaSqm},recommended_rent=#{recommendedRent},status=#{status}
            WHERE id=#{id} AND unit_id=#{unitId} AND space_type='room'
            """)
    int update(Room room);

    @Update("UPDATE rental_spaces SET status='disabled' WHERE id=#{spaceId} AND unit_id=#{unitId} AND space_type='room'")
    int disable(@Param("unitId") Long unitId,@Param("spaceId") Long spaceId);

    @Update("UPDATE units SET rental_mode=#{mode} WHERE id=#{unitId}")
    int updateMode(@Param("unitId") Long unitId,@Param("mode") String mode);

    class Room {
        private Long id,unitId; private String spaceCode,spaceName,status; private Integer capacity;
        private java.math.BigDecimal areaSqm,recommendedRent;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;}
        public String getSpaceCode(){return spaceCode;} public void setSpaceCode(String v){spaceCode=v;} public String getSpaceName(){return spaceName;} public void setSpaceName(String v){spaceName=v;}
        public String getStatus(){return status;} public void setStatus(String v){status=v;} public Integer getCapacity(){return capacity;} public void setCapacity(Integer v){capacity=v;}
        public java.math.BigDecimal getAreaSqm(){return areaSqm;} public void setAreaSqm(java.math.BigDecimal v){areaSqm=v;} public java.math.BigDecimal getRecommendedRent(){return recommendedRent;} public void setRecommendedRent(java.math.BigDecimal v){recommendedRent=v;}
    }
}
