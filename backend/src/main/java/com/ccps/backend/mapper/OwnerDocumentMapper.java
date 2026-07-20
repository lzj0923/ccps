package com.ccps.backend.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OwnerDocumentMapper {

    @Select("""
            SELECT DISTINCT d.id, d.document_no, d.original_name, d.document_type, d.status,
                   d.mime_type, d.file_size, d.storage_key, d.expires_at, d.created_at, d.updated_at,
                   uploader.display_name AS uploader_name,
                   p.name AS project_name, p.city, u.unit_no
            FROM documents d
            LEFT JOIN users uploader ON uploader.id = d.uploaded_by
            LEFT JOIN document_links unit_link
              ON unit_link.document_id = d.id AND unit_link.entity_type = 'unit'
            LEFT JOIN document_links work_link
              ON work_link.document_id = d.id AND work_link.entity_type = 'work_order'
            LEFT JOIN maintenance_work_orders work_order ON work_order.id = work_link.entity_id
            LEFT JOIN document_links finance_link
              ON finance_link.document_id = d.id AND finance_link.entity_type = 'finance'
            LEFT JOIN finance_records finance_record ON finance_record.id = finance_link.entity_id
            LEFT JOIN units u ON u.id = COALESCE(unit_link.entity_id, work_order.unit_id, finance_record.unit_id)
            LEFT JOIN projects p ON p.id = u.project_id
            WHERE d.uploaded_by = #{userId}
               OR EXISTS (
                    SELECT 1
                    FROM document_links dl
                    WHERE dl.document_id = d.id
                      AND (
                        (dl.entity_type = 'owner' AND dl.entity_id IN
                          (SELECT id FROM owners WHERE user_id = #{userId} AND status = 'active'))
                        OR
                        (dl.entity_type = 'unit' AND dl.entity_id IN
                          (SELECT ou.unit_id FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                           WHERE o.user_id = #{userId} AND o.status = 'active' AND ou.status = 'active'))
                        OR
                        (dl.entity_type = 'finance' AND dl.entity_id IN
                          (SELECT id FROM finance_records WHERE owner_id IN
                           (SELECT id FROM owners WHERE user_id = #{userId} AND status = 'active')))
                        OR
                        (dl.entity_type = 'work_order' AND dl.entity_id IN
                          (SELECT mwo.id FROM maintenance_work_orders mwo
                           WHERE mwo.unit_id IN
                            (SELECT ou.unit_id FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                             WHERE o.user_id = #{userId} AND o.status = 'active' AND ou.status = 'active')))
                      )
               )
            ORDER BY d.created_at DESC, d.id DESC
            """)
    List<DocumentRow> findDocuments(@Param("userId") Long userId);

    @Select("""
            SELECT d.id, d.original_name, d.storage_key, d.mime_type, d.file_size
            FROM documents d
            WHERE d.id = #{documentId}
              AND (
                d.uploaded_by = #{userId}
                OR EXISTS (
                  SELECT 1 FROM document_links dl
                  WHERE dl.document_id = d.id
                    AND (
                      (dl.entity_type = 'owner' AND dl.entity_id IN
                        (SELECT id FROM owners WHERE user_id = #{userId} AND status = 'active'))
                      OR (dl.entity_type = 'unit' AND dl.entity_id IN
                        (SELECT ou.unit_id FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                         WHERE o.user_id = #{userId} AND o.status = 'active' AND ou.status = 'active'))
                      OR (dl.entity_type = 'finance' AND dl.entity_id IN
                        (SELECT id FROM finance_records WHERE owner_id IN
                         (SELECT id FROM owners WHERE user_id = #{userId} AND status = 'active')))
                      OR (dl.entity_type = 'work_order' AND dl.entity_id IN
                        (SELECT mwo.id FROM maintenance_work_orders mwo WHERE mwo.unit_id IN
                         (SELECT ou.unit_id FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                          WHERE o.user_id = #{userId} AND o.status = 'active' AND ou.status = 'active')))
                    )
                )
              )
            """)
    DocumentFile findDocumentFile(@Param("userId") Long userId, @Param("documentId") Long documentId);

    class DocumentRow {
        private Long id; private String documentNo; private String originalName; private String documentType;
        private String status; private String mimeType; private Long fileSize; private String storageKey;
        private java.time.LocalDate expiresAt; private LocalDateTime createdAt; private LocalDateTime updatedAt;
        private String uploaderName; private String projectName; private String city; private String unitNo;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public String getDocumentNo() { return documentNo; } public void setDocumentNo(String v) { documentNo = v; }
        public String getOriginalName() { return originalName; } public void setOriginalName(String v) { originalName = v; }
        public String getDocumentType() { return documentType; } public void setDocumentType(String v) { documentType = v; }
        public String getStatus() { return status; } public void setStatus(String v) { status = v; }
        public String getMimeType() { return mimeType; } public void setMimeType(String v) { mimeType = v; }
        public Long getFileSize() { return fileSize; } public void setFileSize(Long v) { fileSize = v; }
        public String getStorageKey() { return storageKey; } public void setStorageKey(String v) { storageKey = v; }
        public java.time.LocalDate getExpiresAt() { return expiresAt; } public void setExpiresAt(java.time.LocalDate v) { expiresAt = v; }
        public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt = v; }
        public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
        public String getUploaderName() { return uploaderName; } public void setUploaderName(String v) { uploaderName = v; }
        public String getProjectName() { return projectName; } public void setProjectName(String v) { projectName = v; }
        public String getCity() { return city; } public void setCity(String v) { city = v; }
        public String getUnitNo() { return unitNo; } public void setUnitNo(String v) { unitNo = v; }
    }

    class DocumentFile {
        private Long id; private String originalName; private String storageKey; private String mimeType; private Long fileSize;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public String getOriginalName() { return originalName; } public void setOriginalName(String v) { originalName = v; }
        public String getStorageKey() { return storageKey; } public void setStorageKey(String v) { storageKey = v; }
        public String getMimeType() { return mimeType; } public void setMimeType(String v) { mimeType = v; }
        public Long getFileSize() { return fileSize; } public void setFileSize(Long v) { fileSize = v; }
    }
}
