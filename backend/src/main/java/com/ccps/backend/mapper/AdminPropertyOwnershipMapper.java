package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminPropertyOwnershipMapper {
    String SELECT_FIELDS = "ou.id AS ownershipId, ou.unit_id AS unitId, ou.owner_id AS ownerId, o.owner_no AS ownerNo, "
            + "o.full_name AS ownerName, o.identity_no AS identityNo, COALESCE(o.mobile_phone,o.phone) AS mobilePhone, "
            + "o.email, ou.ownership_percent AS ownershipPercent, ou.is_primary AS primaryFlag, ou.start_date AS startDate, "
            + "ou.end_date AS endDate, ou.status";

    @Select("SELECT COUNT(*) FROM units WHERE id=#{unitId}")
    int unitExists(@Param("unitId") Long unitId);

    @Select("SELECT COUNT(*) FROM owners WHERE id=#{ownerId} AND status='active'")
    int activeOwnerExists(@Param("ownerId") Long ownerId);

    @Select("SELECT " + SELECT_FIELDS + " FROM owner_units ou JOIN owners o ON o.id=ou.owner_id WHERE ou.unit_id=#{unitId} ORDER BY ou.status='active' DESC, ou.is_primary DESC, ou.id")
    List<Row> list(@Param("unitId") Long unitId);

    @Select("SELECT " + SELECT_FIELDS + " FROM owner_units ou JOIN owners o ON o.id=ou.owner_id WHERE ou.id=#{ownershipId} AND ou.unit_id=#{unitId}")
    Row find(@Param("unitId") Long unitId, @Param("ownershipId") Long ownershipId);

    @Select("SELECT " + SELECT_FIELDS + " FROM owner_units ou JOIN owners o ON o.id=ou.owner_id WHERE ou.owner_id=#{ownerId} AND ou.unit_id=#{unitId} ORDER BY ou.id DESC LIMIT 1")
    Row findByOwner(@Param("unitId") Long unitId, @Param("ownerId") Long ownerId);

    @Select("SELECT COALESCE(SUM(ownership_percent),0) FROM owner_units WHERE unit_id=#{unitId} AND status='active' AND id<>#{excludeId}")
    BigDecimal activePercentExcluding(@Param("unitId") Long unitId, @Param("excludeId") Long excludeId);

    @Select("SELECT COUNT(*) FROM owner_units WHERE unit_id=#{unitId} AND status='active'")
    int activeCount(@Param("unitId") Long unitId);

    @Select("SELECT COUNT(*) FROM owner_units WHERE unit_id=#{unitId} AND status='active' AND is_primary=1")
    int primaryCount(@Param("unitId") Long unitId);

    @Update("UPDATE owner_units SET is_primary=0 WHERE unit_id=#{unitId} AND status='active'")
    int clearPrimary(@Param("unitId") Long unitId);

    @Update("UPDATE owner_units SET is_primary=1 WHERE id=(SELECT id FROM (SELECT id FROM owner_units WHERE unit_id=#{unitId} AND status='active' ORDER BY id LIMIT 1) candidate)")
    int assignFirstPrimary(@Param("unitId") Long unitId);

    @Insert("""
            INSERT INTO owner_units
              (owner_id, unit_id, ownership_percent, is_primary, start_date, end_date,
               asset_stage, expected_handover_date, actual_handover_date, status)
            SELECT #{ownerId}, #{unitId}, #{ownershipPercent}, #{primary}, #{startDate}, #{endDate},
                   asset_stage, expected_handover_date, actual_handover_date, 'active'
            FROM owner_units WHERE unit_id=#{unitId} ORDER BY status='active' DESC, id LIMIT 1
            """)
    @Options(useGeneratedKeys = true, keyProperty = "ownershipId")
    int insert(NewOwnership row);

    @Update("""
            UPDATE owner_units SET ownership_percent=#{ownershipPercent}, is_primary=#{primary},
              start_date=#{startDate}, end_date=#{endDate}, status='active'
            WHERE id=#{ownershipId} AND unit_id=#{unitId}
            """)
    int update(@Param("unitId") Long unitId, @Param("ownershipId") Long ownershipId,
            @Param("ownershipPercent") BigDecimal ownershipPercent, @Param("primary") boolean primary,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Update("""
            UPDATE owner_units SET ownership_percent=#{ownershipPercent}, is_primary=#{primary},
              start_date=#{startDate}, end_date=#{endDate}, status='active'
            WHERE id=#{ownershipId} AND unit_id=#{unitId} AND owner_id=#{ownerId}
            """)
    int reactivate(@Param("unitId") Long unitId, @Param("ownershipId") Long ownershipId,
            @Param("ownerId") Long ownerId, @Param("ownershipPercent") BigDecimal ownershipPercent,
            @Param("primary") boolean primary, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Update("UPDATE owner_units SET status='inactive', is_primary=0, end_date=COALESCE(end_date,CURRENT_DATE) WHERE id=#{ownershipId} AND unit_id=#{unitId} AND status='active'")
    int deactivate(@Param("unitId") Long unitId, @Param("ownershipId") Long ownershipId);

    class Row {
        private Long ownershipId, unitId, ownerId;
        private String ownerNo, ownerName, identityNo, mobilePhone, email, status;
        private BigDecimal ownershipPercent;
        private boolean primaryFlag;
        private LocalDate startDate, endDate;
        public Long getOwnershipId(){return ownershipId;} public void setOwnershipId(Long v){ownershipId=v;}
        public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;}
        public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;}
        public String getOwnerNo(){return ownerNo;} public void setOwnerNo(String v){ownerNo=v;}
        public String getOwnerName(){return ownerName;} public void setOwnerName(String v){ownerName=v;}
        public String getIdentityNo(){return identityNo;} public void setIdentityNo(String v){identityNo=v;}
        public String getMobilePhone(){return mobilePhone;} public void setMobilePhone(String v){mobilePhone=v;}
        public String getEmail(){return email;} public void setEmail(String v){email=v;}
        public BigDecimal getOwnershipPercent(){return ownershipPercent;} public void setOwnershipPercent(BigDecimal v){ownershipPercent=v;}
        public boolean isPrimaryFlag(){return primaryFlag;} public void setPrimaryFlag(boolean v){primaryFlag=v;}
        public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;}
        public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
        public String getStatus(){return status;} public void setStatus(String v){status=v;}
    }

    class NewOwnership {
        private Long ownershipId, ownerId, unitId;
        private BigDecimal ownershipPercent;
        private boolean primary;
        private LocalDate startDate, endDate;
        public Long getOwnershipId(){return ownershipId;} public void setOwnershipId(Long v){ownershipId=v;}
        public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;}
        public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;}
        public BigDecimal getOwnershipPercent(){return ownershipPercent;} public void setOwnershipPercent(BigDecimal v){ownershipPercent=v;}
        public boolean isPrimary(){return primary;} public void setPrimary(boolean v){primary=v;}
        public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;}
        public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
    }
}
