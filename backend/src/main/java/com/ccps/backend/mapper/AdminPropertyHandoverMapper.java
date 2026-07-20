package com.ccps.backend.mapper;

import com.ccps.backend.dto.AdminPropertyHandoverResponse;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AdminPropertyHandoverMapper {
    @Select("""
        SELECT ph.id, ph.mandate_id, rm.mandate_no, rm.owner_unit_id, p.name project_name, u.unit_no,
               ph.handover_date, ph.condition_summary, ph.key_count, ph.access_card_count, ph.water_meter,
               ph.electricity_meter, CAST(ph.inventory AS CHAR) inventory, ph.received_by, ph.notes, ph.status,
               ph.completed_by, cu.display_name completed_by_name, ph.completed_at
        FROM property_handovers ph JOIN rental_mandates rm ON rm.id=ph.mandate_id
        JOIN owner_units ou ON ou.id=rm.owner_unit_id JOIN units u ON u.id=ou.unit_id
        JOIN projects p ON p.id=u.project_id LEFT JOIN users cu ON cu.id=ph.completed_by
        WHERE ph.mandate_id=#{mandateId}
        """)
    AdminPropertyHandoverResponse findByMandateId(@Param("mandateId") Long mandateId);

    @Select("SELECT COUNT(*) FROM rental_mandates WHERE id=#{mandateId} AND status IN ('active','suspended')")
    int countEligibleMandate(@Param("mandateId") Long mandateId);

    @Insert("""
        INSERT INTO property_handovers
          (mandate_id,handover_date,condition_summary,key_count,access_card_count,water_meter,electricity_meter,inventory,received_by,notes,status,completed_by,completed_at,created_by)
        VALUES (#{mandateId},#{handoverDate},#{conditionSummary},#{keyCount},#{accessCardCount},#{waterMeter},#{electricityMeter},CAST(#{inventory} AS JSON),#{receivedBy},#{notes},#{status},#{completedBy},IF(#{status}='completed',NOW(),NULL),#{createdBy})
        ON DUPLICATE KEY UPDATE handover_date=VALUES(handover_date), condition_summary=VALUES(condition_summary), key_count=VALUES(key_count), access_card_count=VALUES(access_card_count), water_meter=VALUES(water_meter), electricity_meter=VALUES(electricity_meter), inventory=VALUES(inventory), received_by=VALUES(received_by), notes=VALUES(notes), status=VALUES(status), completed_by=VALUES(completed_by), completed_at=IF(VALUES(status)='completed',NOW(),NULL)
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int upsert(HandoverRecord record);

    @Insert("INSERT INTO audit_logs(actor_user_id,action,entity_type,entity_id,after_data) VALUES(#{actorId},#{action},'property_handover',#{entityId},CAST(#{data} AS JSON))")
    int insertAudit(@Param("actorId") Long actorId, @Param("action") String action, @Param("entityId") Long entityId, @Param("data") String data);

    class HandoverRecord {
        private Long id, mandateId, createdBy, completedBy; private java.time.LocalDate handoverDate; private String conditionSummary, waterMeter, electricityMeter, inventory, receivedBy, notes, status; private Integer keyCount, accessCardCount;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getMandateId(){return mandateId;} public void setMandateId(Long v){mandateId=v;} public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;} public Long getCompletedBy(){return completedBy;} public void setCompletedBy(Long v){completedBy=v;} public java.time.LocalDate getHandoverDate(){return handoverDate;} public void setHandoverDate(java.time.LocalDate v){handoverDate=v;} public String getConditionSummary(){return conditionSummary;} public void setConditionSummary(String v){conditionSummary=v;} public String getWaterMeter(){return waterMeter;} public void setWaterMeter(String v){waterMeter=v;} public String getElectricityMeter(){return electricityMeter;} public void setElectricityMeter(String v){electricityMeter=v;} public String getInventory(){return inventory;} public void setInventory(String v){inventory=v;} public String getReceivedBy(){return receivedBy;} public void setReceivedBy(String v){receivedBy=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public Integer getKeyCount(){return keyCount;} public void setKeyCount(Integer v){keyCount=v;} public Integer getAccessCardCount(){return accessCardCount;} public void setAccessCardCount(Integer v){accessCardCount=v;}
    }
}
