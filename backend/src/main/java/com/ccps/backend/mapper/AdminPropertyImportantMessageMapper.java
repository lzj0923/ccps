package com.ccps.backend.mapper;

import java.time.LocalDate;
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
public interface AdminPropertyImportantMessageMapper {
    String COLUMNS = "m.id,m.owner_unit_id AS ownerUnitId,m.subject,m.content,"
            + "m.announcement_start_date AS announcementStartDate,m.announcement_end_date AS announcementEndDate,"
            + "m.importance,m.is_read AS readFlag,m.created_by AS createdBy,"
            + "COALESCE(u.display_name,u.username,'系統') AS createdByName,m.created_at AS createdAt,m.updated_at AS updatedAt";
    String JOINS = " FROM property_important_messages m LEFT JOIN users u ON u.id=m.created_by ";

    @Select("SELECT COUNT(*) FROM owner_units WHERE id=#{ownerUnitId} AND owner_id=#{ownerId} AND status='active'")
    int ownsProperty(@Param("ownerId") Long ownerId,@Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT COUNT(*) FROM owner_units ou JOIN owners o ON o.id=ou.owner_id "
            + "WHERE ou.id=#{ownerUnitId} AND o.user_id=#{userId} AND o.status='active' AND ou.status='active'")
    int ownsPropertyForUser(@Param("userId") Long userId,@Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT "+COLUMNS+JOINS+"WHERE m.owner_unit_id=#{ownerUnitId} ORDER BY m.announcement_start_date DESC,m.id DESC")
    List<MessageRow> list(@Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT "+COLUMNS+JOINS+"WHERE m.owner_unit_id=#{ownerUnitId} AND m.id=#{messageId}")
    MessageRow find(@Param("ownerUnitId") Long ownerUnitId,@Param("messageId") Long messageId);

    @Insert("INSERT INTO property_important_messages (owner_unit_id,subject,content,announcement_start_date,announcement_end_date,importance,is_read,created_by) "
            + "VALUES (#{ownerUnitId},#{subject},#{content},#{announcementStartDate},#{announcementEndDate},#{importance},#{readFlag},#{createdBy})")
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insert(MessageRow row);

    @Update("UPDATE property_important_messages SET subject=#{subject},content=#{content},announcement_start_date=#{announcementStartDate},"
            + "announcement_end_date=#{announcementEndDate},importance=#{importance},is_read=#{readFlag} WHERE id=#{id} AND owner_unit_id=#{ownerUnitId}")
    int update(MessageRow row);

    @Delete("DELETE FROM property_important_messages WHERE id=#{messageId} AND owner_unit_id=#{ownerUnitId}")
    int delete(@Param("ownerUnitId") Long ownerUnitId,@Param("messageId") Long messageId);

    class MessageRow {
        private Long id,ownerUnitId,createdBy;
        private String subject,content,importance,createdByName;
        private LocalDate announcementStartDate,announcementEndDate;
        private LocalDateTime createdAt,updatedAt;
        private boolean readFlag;
        public Long getId(){return id;} public void setId(Long value){id=value;}
        public Long getOwnerUnitId(){return ownerUnitId;} public void setOwnerUnitId(Long value){ownerUnitId=value;}
        public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long value){createdBy=value;}
        public String getSubject(){return subject;} public void setSubject(String value){subject=value;}
        public String getContent(){return content;} public void setContent(String value){content=value;}
        public String getImportance(){return importance;} public void setImportance(String value){importance=value;}
        public String getCreatedByName(){return createdByName;} public void setCreatedByName(String value){createdByName=value;}
        public LocalDate getAnnouncementStartDate(){return announcementStartDate;} public void setAnnouncementStartDate(LocalDate value){announcementStartDate=value;}
        public LocalDate getAnnouncementEndDate(){return announcementEndDate;} public void setAnnouncementEndDate(LocalDate value){announcementEndDate=value;}
        public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
        public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
        public boolean isReadFlag(){return readFlag;} public void setReadFlag(boolean value){readFlag=value;}
    }
}
