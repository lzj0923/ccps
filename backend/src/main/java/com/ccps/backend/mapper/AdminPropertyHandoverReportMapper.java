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
public interface AdminPropertyHandoverReportMapper {
    String COLUMNS = "hr.id,hr.owner_unit_id AS ownerUnitId,hr.document_id AS documentId,hr.title,"
            + "hr.report_date AS reportDate,hr.tracking_start_date AS trackingStartDate,"
            + "hr.tracking_end_date AS trackingEndDate,hr.remarks,hr.content_json AS contentJson,hr.completed,hr.created_by AS createdBy,"
            + "COALESCE(u.display_name,u.username,'系統') AS createdByName,hr.created_at AS createdAt,"
            + "hr.updated_at AS updatedAt,d.original_name AS originalName,d.storage_key AS storageKey,"
            + "d.mime_type AS mimeType,d.file_size AS fileSize";
    String JOINS = " FROM property_handover_reports hr LEFT JOIN documents d ON d.id=hr.document_id "
            + "LEFT JOIN users u ON u.id=hr.created_by ";

    @Select("SELECT COUNT(*) FROM owner_units WHERE id=#{ownerUnitId} AND owner_id=#{ownerId} AND status='active'")
    int ownsProperty(@Param("ownerId") Long ownerId, @Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT COUNT(*) FROM owner_units ou JOIN owners o ON o.id=ou.owner_id "
            + "WHERE ou.id=#{ownerUnitId} AND o.user_id=#{userId} AND o.status='active' AND ou.status='active'")
    int ownsPropertyForUser(@Param("userId") Long userId, @Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT p.name AS projectName,u.unit_no AS unitNo,o.full_name AS ownerName FROM owner_units ou "
            + "JOIN units u ON u.id=ou.unit_id JOIN projects p ON p.id=u.project_id JOIN owners o ON o.id=ou.owner_id "
            + "WHERE ou.id=#{ownerUnitId}")
    PropertyInfo propertyInfo(@Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT " + COLUMNS + JOINS + "WHERE hr.owner_unit_id=#{ownerUnitId} ORDER BY hr.report_date DESC,hr.id DESC")
    List<ReportRow> list(@Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT " + COLUMNS + JOINS + "WHERE hr.owner_unit_id=#{ownerUnitId} AND hr.id=#{reportId}")
    ReportRow find(@Param("ownerUnitId") Long ownerUnitId, @Param("reportId") Long reportId);

    @Insert("INSERT INTO documents (document_no,original_name,storage_key,mime_type,file_size,document_type,status,uploaded_by) "
            + "VALUES (#{documentNo},#{originalName},#{storageKey},#{mimeType},#{fileSize},'handover_repair_attachment','approved',#{uploadedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDocument(DocumentRow row);

    @Insert("INSERT INTO property_handover_reports (owner_unit_id,document_id,title,report_date,tracking_start_date,tracking_end_date,remarks,content_json,completed,created_by) "
            + "VALUES (#{ownerUnitId},#{documentId},#{title},#{reportDate},#{trackingStartDate},#{trackingEndDate},#{remarks},#{contentJson},#{completed},#{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReport(ReportRow row);

    @Update("UPDATE property_handover_reports SET title=#{title},report_date=#{reportDate},tracking_start_date=#{trackingStartDate},"
            + "tracking_end_date=#{trackingEndDate},remarks=#{remarks},content_json=#{contentJson},completed=#{completed},document_id=#{documentId} "
            + "WHERE id=#{id} AND owner_unit_id=#{ownerUnitId}")
    int updateReport(ReportRow row);

    @Update("UPDATE documents SET original_name=#{originalName},storage_key=#{storageKey},mime_type=#{mimeType},file_size=#{fileSize} WHERE id=#{id}")
    int updateDocument(DocumentRow row);

    @Delete("DELETE FROM property_handover_reports WHERE id=#{reportId} AND owner_unit_id=#{ownerUnitId}")
    int deleteReport(@Param("ownerUnitId") Long ownerUnitId, @Param("reportId") Long reportId);

    @Delete("DELETE FROM documents WHERE id=#{documentId}")
    int deleteDocument(@Param("documentId") Long documentId);

    class DocumentRow {
        private Long id; private String documentNo,originalName,storageKey,mimeType; private Long fileSize,uploadedBy;
        public Long getId(){return id;} public void setId(Long value){id=value;}
        public String getDocumentNo(){return documentNo;} public void setDocumentNo(String value){documentNo=value;}
        public String getOriginalName(){return originalName;} public void setOriginalName(String value){originalName=value;}
        public String getStorageKey(){return storageKey;} public void setStorageKey(String value){storageKey=value;}
        public String getMimeType(){return mimeType;} public void setMimeType(String value){mimeType=value;}
        public Long getFileSize(){return fileSize;} public void setFileSize(Long value){fileSize=value;}
        public Long getUploadedBy(){return uploadedBy;} public void setUploadedBy(Long value){uploadedBy=value;}
    }

    class ReportRow {
        private Long id,ownerUnitId,documentId,createdBy,fileSize;
        private String title,remarks,contentJson,createdByName,originalName,storageKey,mimeType;
        private LocalDate reportDate,trackingStartDate,trackingEndDate;
        private LocalDateTime createdAt,updatedAt;
        private boolean completed;
        public Long getId(){return id;} public void setId(Long value){id=value;}
        public Long getOwnerUnitId(){return ownerUnitId;} public void setOwnerUnitId(Long value){ownerUnitId=value;}
        public Long getDocumentId(){return documentId;} public void setDocumentId(Long value){documentId=value;}
        public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long value){createdBy=value;}
        public Long getFileSize(){return fileSize;} public void setFileSize(Long value){fileSize=value;}
        public String getTitle(){return title;} public void setTitle(String value){title=value;}
        public String getRemarks(){return remarks;} public void setRemarks(String value){remarks=value;}
        public String getContentJson(){return contentJson;} public void setContentJson(String value){contentJson=value;}
        public String getCreatedByName(){return createdByName;} public void setCreatedByName(String value){createdByName=value;}
        public String getOriginalName(){return originalName;} public void setOriginalName(String value){originalName=value;}
        public String getStorageKey(){return storageKey;} public void setStorageKey(String value){storageKey=value;}
        public String getMimeType(){return mimeType;} public void setMimeType(String value){mimeType=value;}
        public LocalDate getReportDate(){return reportDate;} public void setReportDate(LocalDate value){reportDate=value;}
        public LocalDate getTrackingStartDate(){return trackingStartDate;} public void setTrackingStartDate(LocalDate value){trackingStartDate=value;}
        public LocalDate getTrackingEndDate(){return trackingEndDate;} public void setTrackingEndDate(LocalDate value){trackingEndDate=value;}
        public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
        public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
        public boolean isCompleted(){return completed;} public void setCompleted(boolean value){completed=value;}
    }

    class PropertyInfo {
        private String projectName,unitNo,ownerName;
        public String getProjectName(){return projectName;} public void setProjectName(String value){projectName=value;}
        public String getUnitNo(){return unitNo;} public void setUnitNo(String value){unitNo=value;}
        public String getOwnerName(){return ownerName;} public void setOwnerName(String value){ownerName=value;}
    }
}
