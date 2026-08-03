package com.ccps.backend.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminPropertyHandoverChecklistMapper {
    String COLUMNS = "id,owner_unit_id AS ownerUnitId,category,item_name AS itemName,default_quantity AS defaultQuantity,notes,sort_order AS sortOrder,enabled,created_at AS createdAt,updated_at AS updatedAt";

    @Select("SELECT " + COLUMNS + " FROM property_handover_checklist_items WHERE owner_unit_id=#{ownerUnitId} ORDER BY category,sort_order,id")
    List<Row> list(@Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT COUNT(*) FROM owner_units WHERE id=#{ownerUnitId} AND owner_id=#{ownerId} AND status='active'")
    int ownsProperty(@Param("ownerId") Long ownerId, @Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT ou.id AS ownerUnitId, ou.owner_id AS ownerId "
            + "FROM leases l JOIN owner_units ou ON ou.unit_id=l.unit_id AND ou.status='active' "
            + "WHERE l.id=#{leaseId} ORDER BY ou.is_primary DESC, ou.id DESC LIMIT 1")
    LeaseOwnerUnitRow findOwnerUnitForLease(@Param("leaseId") Long leaseId);

    @Select("SELECT " + COLUMNS + " FROM property_handover_checklist_items WHERE id=#{id} AND owner_unit_id=#{ownerUnitId}")
    Row find(@Param("ownerUnitId") Long ownerUnitId, @Param("id") Long id);

    @Insert("INSERT INTO property_handover_checklist_items (owner_unit_id,category,item_name,default_quantity,notes,sort_order,enabled) VALUES (#{ownerUnitId},#{category},#{itemName},#{defaultQuantity},#{notes},#{sortOrder},#{enabled})")
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insert(Row row);

    @Update("UPDATE property_handover_checklist_items SET category=#{category},item_name=#{itemName},default_quantity=#{defaultQuantity},notes=#{notes},sort_order=#{sortOrder},enabled=#{enabled} WHERE id=#{id} AND owner_unit_id=#{ownerUnitId}")
    int update(Row row);

    @Delete("DELETE FROM property_handover_checklist_items WHERE id=#{id} AND owner_unit_id=#{ownerUnitId}")
    int delete(@Param("ownerUnitId") Long ownerUnitId, @Param("id") Long id);

    class Row {
        private Long id,ownerUnitId; private String category,itemName,defaultQuantity,notes; private Integer sortOrder; private boolean enabled; private LocalDateTime createdAt,updatedAt;
        public Long getId(){return id;} public void setId(Long value){id=value;}
        public Long getOwnerUnitId(){return ownerUnitId;} public void setOwnerUnitId(Long value){ownerUnitId=value;}
        public String getCategory(){return category;} public void setCategory(String value){category=value;}
        public String getItemName(){return itemName;} public void setItemName(String value){itemName=value;}
        public String getDefaultQuantity(){return defaultQuantity;} public void setDefaultQuantity(String value){defaultQuantity=value;}
        public String getNotes(){return notes;} public void setNotes(String value){notes=value;}
        public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer value){sortOrder=value;}
        public boolean isEnabled(){return enabled;} public void setEnabled(boolean value){enabled=value;}
        public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
        public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
    }

    class LeaseOwnerUnitRow {
        private Long ownerUnitId, ownerId;
        public Long getOwnerUnitId(){return ownerUnitId;} public void setOwnerUnitId(Long value){ownerUnitId=value;}
        public Long getOwnerId(){return ownerId;} public void setOwnerId(Long value){ownerId=value;}
    }
}
