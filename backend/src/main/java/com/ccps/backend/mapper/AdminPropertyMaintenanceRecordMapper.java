package com.ccps.backend.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.ccps.backend.dto.AdminPropertyMaintenanceRecordResponse;

@Mapper
public interface AdminPropertyMaintenanceRecordMapper {
    @Select("""
            SELECT COUNT(*) FROM owner_units
            WHERE id = #{ownerUnitId} AND owner_id = #{ownerId} AND status = 'active'
            """)
    int countProperty(@Param("ownerId") Long ownerId, @Param("ownerUnitId") Long ownerUnitId);

    @Select("""
            SELECT id, owner_unit_id, work_order_id, record_no, category, title,
                   maintenance_date, duration_minutes, details, result_summary,
                   next_maintenance_date, created_at, updated_at
            FROM property_maintenance_records
            WHERE owner_unit_id = #{ownerUnitId}
            ORDER BY maintenance_date DESC, id DESC
            """)
    List<AdminPropertyMaintenanceRecordResponse> findAll(@Param("ownerUnitId") Long ownerUnitId);

    @Select("""
            SELECT id, owner_unit_id, work_order_id, record_no, category, title,
                   maintenance_date, duration_minutes, details, result_summary,
                   next_maintenance_date, created_at, updated_at
            FROM property_maintenance_records
            WHERE id = #{id} AND owner_unit_id = #{ownerUnitId}
            LIMIT 1
            """)
    AdminPropertyMaintenanceRecordResponse findById(@Param("ownerUnitId") Long ownerUnitId,
            @Param("id") Long id);

    @Insert("""
            INSERT INTO property_maintenance_records
              (owner_unit_id, record_no, category, title, maintenance_date, duration_minutes,
               details, result_summary, next_maintenance_date, created_by)
            VALUES
              (#{ownerUnitId}, #{recordNo}, #{category}, #{title}, #{maintenanceDate},
               #{durationMinutes}, #{details}, #{resultSummary}, #{nextMaintenanceDate}, #{actorId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(NewRecord record);

    @Update("""
            UPDATE property_maintenance_records
            SET category = #{category}, title = #{title}, maintenance_date = #{maintenanceDate},
                duration_minutes = #{durationMinutes}, details = #{details},
                result_summary = #{resultSummary}, next_maintenance_date = #{nextMaintenanceDate}
            WHERE id = #{id} AND owner_unit_id = #{ownerUnitId}
            """)
    int update(NewRecord record);

    @Delete("DELETE FROM property_maintenance_records WHERE id = #{id} AND owner_unit_id = #{ownerUnitId}")
    int delete(@Param("ownerUnitId") Long ownerUnitId, @Param("id") Long id);

    @Insert("""
            INSERT INTO property_maintenance_records
              (owner_unit_id, work_order_id, record_no, category, title, maintenance_date,
               duration_minutes, details, result_summary, created_by)
            SELECT ou.id, mwo.id, CONCAT('PMR-', mwo.work_order_no), mwo.category, mwo.title,
                   DATE(COALESCE(mwo.completed_at, NOW())),
                   GREATEST(0, TIMESTAMPDIFF(MINUTE, mwo.requested_at, COALESCE(mwo.completed_at, NOW()))),
                   mwo.description, #{resultSummary}, #{actorId}
            FROM maintenance_work_orders mwo
            JOIN owner_units ou ON ou.unit_id = mwo.unit_id AND ou.status = 'active'
              AND (ou.owner_id = mwo.owner_id OR (mwo.owner_id IS NULL AND ou.is_primary = 1))
            WHERE mwo.id = #{workOrderId}
            ORDER BY ou.is_primary DESC, ou.id
            LIMIT 1
            ON DUPLICATE KEY UPDATE
              category = VALUES(category), title = VALUES(title), maintenance_date = VALUES(maintenance_date),
              duration_minutes = VALUES(duration_minutes), details = VALUES(details),
              result_summary = VALUES(result_summary), updated_at = NOW()
            """)
    int upsertFromWorkOrder(@Param("workOrderId") Long workOrderId,
            @Param("resultSummary") String resultSummary, @Param("actorId") Long actorId);

    class NewRecord {
        private Long id; private Long ownerUnitId; private String recordNo; private String category;
        private String title; private LocalDate maintenanceDate; private Integer durationMinutes;
        private String details; private String resultSummary; private LocalDate nextMaintenanceDate;
        private Long actorId;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public Long getOwnerUnitId() { return ownerUnitId; } public void setOwnerUnitId(Long value) { ownerUnitId = value; }
        public String getRecordNo() { return recordNo; } public void setRecordNo(String value) { recordNo = value; }
        public String getCategory() { return category; } public void setCategory(String value) { category = value; }
        public String getTitle() { return title; } public void setTitle(String value) { title = value; }
        public LocalDate getMaintenanceDate() { return maintenanceDate; } public void setMaintenanceDate(LocalDate value) { maintenanceDate = value; }
        public Integer getDurationMinutes() { return durationMinutes; } public void setDurationMinutes(Integer value) { durationMinutes = value; }
        public String getDetails() { return details; } public void setDetails(String value) { details = value; }
        public String getResultSummary() { return resultSummary; } public void setResultSummary(String value) { resultSummary = value; }
        public LocalDate getNextMaintenanceDate() { return nextMaintenanceDate; } public void setNextMaintenanceDate(LocalDate value) { nextMaintenanceDate = value; }
        public Long getActorId() { return actorId; } public void setActorId(Long value) { actorId = value; }
    }
}
