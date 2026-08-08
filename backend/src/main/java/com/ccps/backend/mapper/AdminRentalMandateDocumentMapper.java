package com.ccps.backend.mapper;

import com.ccps.backend.dto.AdminRentalMandateDocumentResponse;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AdminRentalMandateDocumentMapper {
    @Select("""
            SELECT d.id,d.document_no,d.original_name,d.document_type,dl.relation_type,d.mime_type,d.file_size,d.created_at,
                   signature_request.status AS signature_status,
                   signature_request.document_kind AS signature_document_kind,
                   signature_request.signer_role AS signature_signer_role,
                   signature_request.signing_order AS signature_signing_order,
                   signature_request.signer_name AS signature_signer_name,
                   signature_request.signer_email AS signature_signer_email,
                   signature_request.requested_at AS signature_requested_at,
                   signature_request.expires_at AS signature_expires_at,
                   signature_request.signed_at AS signature_signed_at
            FROM documents d
            JOIN document_links dl ON dl.document_id=d.id
            LEFT JOIN electronic_signature_requests signature_request
              ON signature_request.id=(
                SELECT MAX(latest_request.id)
                FROM electronic_signature_requests latest_request
                WHERE COALESCE(latest_request.root_document_id,latest_request.source_document_id)=d.id
                  AND latest_request.entity_type='rental_mandate'
                  AND latest_request.entity_id=#{mandateId}
              )
            WHERE dl.entity_type='rental_mandate' AND dl.entity_id=#{mandateId}
              AND d.status NOT IN ('superseded','voided')
            ORDER BY d.created_at DESC
            """)
    List<AdminRentalMandateDocumentResponse> findDocuments(@Param("mandateId") Long mandateId);
    @Select("SELECT rm.id FROM rental_mandates rm WHERE rm.id=#{mandateId}") Long findMandate(@Param("mandateId") Long mandateId);
    @Insert("INSERT INTO documents(document_no,original_name,storage_key,mime_type,file_size,checksum_sha256,document_type,status,uploaded_by) VALUES(#{documentNo},#{originalName},#{storageKey},#{mimeType},#{fileSize},#{checksumSha256},#{documentType},'pending_review',#{uploadedBy})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertDocument(NewDocument document);
    @Insert("INSERT INTO document_links(document_id,entity_type,entity_id,relation_type) VALUES(#{documentId},'rental_mandate',#{mandateId},#{relationType})") int insertLink(@Param("documentId") Long documentId,@Param("mandateId") Long mandateId,@Param("relationType") String relationType);
    @Update("UPDATE documents d JOIN document_links dl ON dl.document_id=d.id SET d.status='superseded', d.updated_at=CURRENT_TIMESTAMP WHERE dl.entity_type='rental_mandate' AND dl.entity_id=#{mandateId} AND dl.relation_type=#{relationType} AND d.status NOT IN ('superseded','voided')")
    int supersedeCurrent(@Param("mandateId") Long mandateId, @Param("relationType") String relationType);
    @Update("""
            UPDATE documents signed
            JOIN electronic_signature_requests sr ON sr.signed_document_id=signed.id
            JOIN document_links root_link ON root_link.document_id=COALESCE(sr.root_document_id,sr.source_document_id)
            SET signed.status='superseded', signed.updated_at=CURRENT_TIMESTAMP
            WHERE root_link.entity_type='rental_mandate' AND root_link.entity_id=#{mandateId}
              AND root_link.relation_type=#{relationType}
              AND signed.status NOT IN ('superseded','voided')
            """)
    int supersedeSignedPackage(@Param("mandateId") Long mandateId, @Param("relationType") String relationType);
    @Update("UPDATE electronic_signature_requests SET status='cancelled', updated_at=CURRENT_TIMESTAMP WHERE entity_type='rental_mandate' AND entity_id=#{mandateId} AND status='pending'")
    int cancelPendingSignatures(@Param("mandateId") Long mandateId);
    @Update("""
            UPDATE electronic_signature_requests sr
            JOIN document_links dl ON dl.document_id=COALESCE(sr.root_document_id,sr.source_document_id)
            SET sr.status='cancelled', sr.updated_at=CURRENT_TIMESTAMP
            WHERE sr.entity_type='rental_mandate' AND sr.entity_id=#{mandateId}
              AND dl.entity_type='rental_mandate' AND dl.entity_id=#{mandateId}
              AND dl.relation_type=#{relationType} AND sr.status='pending'
            """)
    int cancelPendingSignaturesForRelation(@Param("mandateId") Long mandateId,
            @Param("relationType") String relationType);
    @Select("""
            SELECT COUNT(*)
            FROM electronic_signature_requests sr
            JOIN document_links dl ON dl.document_id=COALESCE(sr.root_document_id,sr.source_document_id)
            JOIN documents d ON d.id=dl.document_id
            WHERE sr.entity_type='rental_mandate' AND sr.entity_id=#{mandateId}
              AND dl.entity_type='rental_mandate' AND dl.entity_id=#{mandateId}
              AND dl.relation_type=#{relationType} AND d.status NOT IN ('superseded','voided')
              AND sr.status IN ('pending','sent','viewed','signed')
            """)
    int countStartedSignaturesForRelation(@Param("mandateId") Long mandateId,
            @Param("relationType") String relationType);
    @Select("SELECT d.original_name,d.storage_key,d.mime_type,d.file_size,d.document_type FROM documents d JOIN document_links dl ON dl.document_id=d.id WHERE d.id=#{documentId} AND dl.entity_type='rental_mandate' AND dl.entity_id=#{mandateId}") AttachmentFile findFile(@Param("mandateId") Long mandateId,@Param("documentId") Long documentId);
    class NewDocument { private Long id,uploadedBy,fileSize; private String documentNo,originalName,storageKey,mimeType,checksumSha256,documentType; public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUploadedBy(){return uploadedBy;} public void setUploadedBy(Long v){uploadedBy=v;} public Long getFileSize(){return fileSize;} public void setFileSize(Long v){fileSize=v;} public String getDocumentNo(){return documentNo;} public void setDocumentNo(String v){documentNo=v;} public String getOriginalName(){return originalName;} public void setOriginalName(String v){originalName=v;} public String getStorageKey(){return storageKey;} public void setStorageKey(String v){storageKey=v;} public String getMimeType(){return mimeType;} public void setMimeType(String v){mimeType=v;} public String getChecksumSha256(){return checksumSha256;} public void setChecksumSha256(String v){checksumSha256=v;} public String getDocumentType(){return documentType;} public void setDocumentType(String v){documentType=v;} }
    class AttachmentFile { private String originalName,storageKey,mimeType,documentType; private Long fileSize; public String getOriginalName(){return originalName;} public void setOriginalName(String v){originalName=v;} public String getStorageKey(){return storageKey;} public void setStorageKey(String v){storageKey=v;} public String getMimeType(){return mimeType;} public void setMimeType(String v){mimeType=v;} public Long getFileSize(){return fileSize;} public void setFileSize(Long v){fileSize=v;} public String getDocumentType(){return documentType;} public void setDocumentType(String v){documentType=v;} }
}
