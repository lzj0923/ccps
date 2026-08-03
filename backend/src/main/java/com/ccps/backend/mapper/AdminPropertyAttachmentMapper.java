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
public interface AdminPropertyAttachmentMapper {
    String COLUMNS = "pa.id,pa.owner_unit_id AS ownerUnitId,pa.document_id AS documentId,pa.title,pa.remarks,"
            + "pa.enabled,pa.created_by AS createdBy,COALESCE(u.display_name,u.username,'系統') AS createdByName,"
            + "pa.created_at AS createdAt,pa.updated_at AS updatedAt,d.original_name AS originalName,"
            + "d.storage_key AS storageKey,d.mime_type AS mimeType,d.file_size AS fileSize";
    String JOINS = " FROM property_attachments pa JOIN documents d ON d.id=pa.document_id "
            + "LEFT JOIN users u ON u.id=pa.created_by ";

    @Select("SELECT COUNT(*) FROM owner_units WHERE id=#{ownerUnitId} AND owner_id=#{ownerId} AND status='active'")
    int ownsProperty(@Param("ownerId") Long ownerId, @Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT " + COLUMNS + JOINS + "WHERE pa.owner_unit_id=#{ownerUnitId} ORDER BY pa.created_at DESC,pa.id DESC")
    List<AttachmentRow> list(@Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT " + COLUMNS + JOINS + "WHERE pa.owner_unit_id=#{ownerUnitId} AND pa.id=#{attachmentId}")
    AttachmentRow find(@Param("ownerUnitId") Long ownerUnitId, @Param("attachmentId") Long attachmentId);

    @Insert("INSERT INTO documents (document_no,original_name,storage_key,mime_type,file_size,document_type,status,uploaded_by) "
            + "VALUES (#{documentNo},#{originalName},#{storageKey},#{mimeType},#{fileSize},'property_attachment','approved',#{uploadedBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDocument(DocumentRow row);

    @Insert("INSERT INTO property_attachments (owner_unit_id,document_id,title,remarks,enabled,created_by) "
            + "VALUES (#{ownerUnitId},#{documentId},#{title},#{remarks},#{enabled},#{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAttachment(AttachmentRow row);

    @Update("UPDATE property_attachments SET title=#{title},remarks=#{remarks},enabled=#{enabled} "
            + "WHERE id=#{id} AND owner_unit_id=#{ownerUnitId}")
    int updateAttachment(AttachmentRow row);

    @Update("UPDATE documents SET original_name=#{originalName},storage_key=#{storageKey},mime_type=#{mimeType},file_size=#{fileSize} WHERE id=#{id}")
    int updateDocument(DocumentRow row);

    @Delete("DELETE FROM property_attachments WHERE id=#{attachmentId} AND owner_unit_id=#{ownerUnitId}")
    int deleteAttachment(@Param("ownerUnitId") Long ownerUnitId, @Param("attachmentId") Long attachmentId);

    @Delete("DELETE FROM documents WHERE id=#{documentId}")
    int deleteDocument(@Param("documentId") Long documentId);

    class DocumentRow {
        private Long id;
        private String documentNo;
        private String originalName;
        private String storageKey;
        private String mimeType;
        private Long fileSize;
        private Long uploadedBy;
        public Long getId(){return id;} public void setId(Long value){id=value;}
        public String getDocumentNo(){return documentNo;} public void setDocumentNo(String value){documentNo=value;}
        public String getOriginalName(){return originalName;} public void setOriginalName(String value){originalName=value;}
        public String getStorageKey(){return storageKey;} public void setStorageKey(String value){storageKey=value;}
        public String getMimeType(){return mimeType;} public void setMimeType(String value){mimeType=value;}
        public Long getFileSize(){return fileSize;} public void setFileSize(Long value){fileSize=value;}
        public Long getUploadedBy(){return uploadedBy;} public void setUploadedBy(Long value){uploadedBy=value;}
    }

    class AttachmentRow {
        private Long id;
        private Long ownerUnitId;
        private Long documentId;
        private String title;
        private String remarks;
        private boolean enabled;
        private Long createdBy;
        private String createdByName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String originalName;
        private String storageKey;
        private String mimeType;
        private Long fileSize;
        public Long getId(){return id;} public void setId(Long value){id=value;}
        public Long getOwnerUnitId(){return ownerUnitId;} public void setOwnerUnitId(Long value){ownerUnitId=value;}
        public Long getDocumentId(){return documentId;} public void setDocumentId(Long value){documentId=value;}
        public String getTitle(){return title;} public void setTitle(String value){title=value;}
        public String getRemarks(){return remarks;} public void setRemarks(String value){remarks=value;}
        public boolean isEnabled(){return enabled;} public void setEnabled(boolean value){enabled=value;}
        public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long value){createdBy=value;}
        public String getCreatedByName(){return createdByName;} public void setCreatedByName(String value){createdByName=value;}
        public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime value){createdAt=value;}
        public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime value){updatedAt=value;}
        public String getOriginalName(){return originalName;} public void setOriginalName(String value){originalName=value;}
        public String getStorageKey(){return storageKey;} public void setStorageKey(String value){storageKey=value;}
        public String getMimeType(){return mimeType;} public void setMimeType(String value){mimeType=value;}
        public Long getFileSize(){return fileSize;} public void setFileSize(Long value){fileSize=value;}
    }
}
