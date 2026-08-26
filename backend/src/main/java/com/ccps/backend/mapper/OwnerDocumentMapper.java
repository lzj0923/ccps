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
                   p.name AS project_name, p.city, u.unit_no,
                   CASE
                     WHEN EXISTS (SELECT 1 FROM document_links ctx WHERE ctx.document_id = d.id
                                  AND ctx.entity_type IN ('lease', 'lease_period')) THEN 'lease'
                     WHEN EXISTS (SELECT 1 FROM document_links ctx WHERE ctx.document_id = d.id
                                  AND ctx.entity_type = 'rental_mandate') THEN 'rental_mandate'
                     WHEN EXISTS (SELECT 1 FROM document_links ctx WHERE ctx.document_id = d.id
                                  AND ctx.entity_type = 'cashflow') THEN 'cashflow'
                     WHEN EXISTS (SELECT 1 FROM document_links ctx WHERE ctx.document_id = d.id
                                  AND ctx.entity_type = 'finance') THEN 'finance'
                     WHEN EXISTS (SELECT 1 FROM document_links ctx WHERE ctx.document_id = d.id
                                  AND ctx.entity_type = 'work_order') THEN 'work_order'
                     WHEN EXISTS (SELECT 1 FROM document_links ctx WHERE ctx.document_id = d.id
                                  AND ctx.entity_type = 'unit') THEN 'unit'
                     WHEN EXISTS (SELECT 1 FROM document_links ctx WHERE ctx.document_id = d.id
                                  AND ctx.entity_type = 'owner') THEN 'owner'
                     ELSE NULL
                   END AS link_entity_type
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
            LEFT JOIN document_links cashflow_link
              ON cashflow_link.document_id = d.id AND cashflow_link.entity_type = 'cashflow'
            LEFT JOIN cashflow_entries cashflow_entry ON cashflow_entry.id = cashflow_link.entity_id
            LEFT JOIN document_links lease_link
              ON lease_link.document_id = d.id AND lease_link.entity_type = 'lease'
            LEFT JOIN leases lease ON lease.id = lease_link.entity_id
            LEFT JOIN document_links lease_period_link
              ON lease_period_link.document_id = d.id AND lease_period_link.entity_type = 'lease_period'
            LEFT JOIN lease_periods lease_period ON lease_period.id = lease_period_link.entity_id
            LEFT JOIN leases lease_for_period ON lease_for_period.id = lease_period.lease_id
            LEFT JOIN document_links mandate_link
              ON mandate_link.document_id = d.id AND mandate_link.entity_type = 'rental_mandate'
            LEFT JOIN rental_mandates mandate ON mandate.id = mandate_link.entity_id
            LEFT JOIN owner_units mandate_owner_unit ON mandate_owner_unit.id = mandate.owner_unit_id
            LEFT JOIN units u ON u.id = COALESCE(unit_link.entity_id, work_order.unit_id, finance_record.unit_id,
                                                   cashflow_entry.unit_id, lease.unit_id, lease_for_period.unit_id,
                                                   mandate_owner_unit.unit_id)
            LEFT JOIN projects p ON p.id = u.project_id
            WHERE COALESCE(d.status, '') NOT IN ('superseded', 'voided')
               AND NOT EXISTS (
                    SELECT 1 FROM electronic_signature_requests signed_request
                    JOIN documents signed_document ON signed_document.id = signed_request.signed_document_id
                    WHERE signed_request.source_document_id = d.id
                      AND signed_request.status = 'signed'
                      AND signed_document.status NOT IN ('superseded', 'voided')
               )
               AND (d.uploaded_by = #{userId}
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
                        OR
                        (dl.entity_type = 'cashflow' AND dl.entity_id IN
                          (SELECT ce.id FROM cashflow_entries ce
                           WHERE ce.owner_id IN (SELECT id FROM owners WHERE user_id = #{userId} AND status = 'active')
                              OR ce.unit_id IN
                               (SELECT ou.unit_id FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                                WHERE o.user_id = #{userId} AND o.status = 'active' AND ou.status = 'active')))
                        OR
                        (dl.entity_type = 'lease' AND dl.entity_id IN
                          (SELECT l.id FROM leases l
                           WHERE l.unit_id IN
                            (SELECT ou.unit_id FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                             WHERE o.user_id = #{userId} AND o.status = 'active' AND ou.status = 'active')
                              OR l.rental_mandate_id IN
                               (SELECT rm.id FROM rental_mandates rm JOIN owner_units ou ON ou.id = rm.owner_unit_id
                                JOIN owners o ON o.id = ou.owner_id
                                WHERE o.user_id = #{userId} AND o.status = 'active' AND ou.status = 'active')))
                        OR
                        (dl.entity_type = 'lease_period' AND dl.entity_id IN
                          (SELECT lp.id FROM lease_periods lp JOIN leases l ON l.id = lp.lease_id
                           WHERE l.unit_id IN
                            (SELECT ou.unit_id FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                             WHERE o.user_id = #{userId} AND o.status = 'active' AND ou.status = 'active')))
                        OR
                        (dl.entity_type = 'rental_mandate' AND dl.entity_id IN
                          (SELECT rm.id FROM rental_mandates rm JOIN owner_units ou ON ou.id = rm.owner_unit_id
                           JOIN owners o ON o.id = ou.owner_id
                           WHERE o.user_id = #{userId} AND o.status = 'active' AND ou.status = 'active'))
                      )
               ))
            ORDER BY d.created_at DESC, d.id DESC
            """)
    List<DocumentRow> findDocuments(@Param("userId") Long userId);

    @Select("""
            SELECT d.id, d.original_name, d.storage_key, d.mime_type, d.file_size
            FROM documents d
            WHERE d.id = #{documentId}
              AND COALESCE(d.status, '') NOT IN ('superseded', 'voided')
              AND NOT EXISTS (
                SELECT 1 FROM electronic_signature_requests signed_request
                JOIN documents signed_document ON signed_document.id = signed_request.signed_document_id
                WHERE signed_request.source_document_id = d.id
                  AND signed_request.status = 'signed'
                  AND signed_document.status NOT IN ('superseded', 'voided')
              )
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
                      OR (dl.entity_type = 'cashflow' AND dl.entity_id IN
                        (SELECT ce.id FROM cashflow_entries ce
                         WHERE ce.owner_id IN (SELECT id FROM owners WHERE user_id = #{userId} AND status = 'active')
                            OR ce.unit_id IN
                             (SELECT ou.unit_id FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                              WHERE o.user_id = #{userId} AND o.status = 'active' AND ou.status = 'active')))
                      OR (dl.entity_type = 'lease' AND dl.entity_id IN
                        (SELECT l.id FROM leases l
                         WHERE l.unit_id IN
                          (SELECT ou.unit_id FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                           WHERE o.user_id = #{userId} AND o.status = 'active' AND ou.status = 'active')
                            OR l.rental_mandate_id IN
                             (SELECT rm.id FROM rental_mandates rm JOIN owner_units ou ON ou.id = rm.owner_unit_id
                              JOIN owners o ON o.id = ou.owner_id
                              WHERE o.user_id = #{userId} AND o.status = 'active' AND ou.status = 'active')))
                      OR (dl.entity_type = 'lease_period' AND dl.entity_id IN
                        (SELECT lp.id FROM lease_periods lp JOIN leases l ON l.id = lp.lease_id
                         WHERE l.unit_id IN
                          (SELECT ou.unit_id FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                           WHERE o.user_id = #{userId} AND o.status = 'active' AND ou.status = 'active')))
                      OR (dl.entity_type = 'rental_mandate' AND dl.entity_id IN
                        (SELECT rm.id FROM rental_mandates rm JOIN owner_units ou ON ou.id = rm.owner_unit_id
                         JOIN owners o ON o.id = ou.owner_id
                         WHERE o.user_id = #{userId} AND o.status = 'active' AND ou.status = 'active'))
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
        private String linkEntityType;
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
        public String getLinkEntityType() { return linkEntityType; } public void setLinkEntityType(String v) { linkEntityType = v; }
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
