package com.ccps.backend.mapper;

import com.ccps.backend.dto.AdminRentalMandateDocumentResponse;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AdminRentalMandateDocumentMapper {
    @Select("SELECT d.id,d.document_no,d.original_name,d.document_type,dl.relation_type,d.mime_type,d.file_size,d.created_at FROM documents d JOIN document_links dl ON dl.document_id=d.id WHERE dl.entity_type='rental_mandate' AND dl.entity_id=#{mandateId} AND d.status NOT IN ('superseded','voided') ORDER BY d.created_at DESC")
    List<AdminRentalMandateDocumentResponse> findDocuments(@Param("mandateId") Long mandateId);
    @Select("SELECT rm.id FROM rental_mandates rm WHERE rm.id=#{mandateId}") Long findMandate(@Param("mandateId") Long mandateId);
    @Insert("INSERT INTO documents(document_no,original_name,storage_key,mime_type,file_size,checksum_sha256,document_type,status,uploaded_by) VALUES(#{documentNo},#{originalName},#{storageKey},#{mimeType},#{fileSize},#{checksumSha256},#{documentType},'pending_review',#{uploadedBy})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertDocument(NewDocument document);
    @Insert("INSERT INTO document_links(document_id,entity_type,entity_id,relation_type) VALUES(#{documentId},'rental_mandate',#{mandateId},#{relationType})") int insertLink(@Param("documentId") Long documentId,@Param("mandateId") Long mandateId,@Param("relationType") String relationType);
    @Update("UPDATE documents d JOIN document_links dl ON dl.document_id=d.id SET d.status='superseded', d.updated_at=CURRENT_TIMESTAMP WHERE dl.entity_type='rental_mandate' AND dl.entity_id=#{mandateId} AND dl.relation_type=#{relationType} AND d.status NOT IN ('superseded','voided')")
    int supersedeCurrent(@Param("mandateId") Long mandateId, @Param("relationType") String relationType);
    @Update("UPDATE electronic_signature_requests SET status='cancelled', updated_at=CURRENT_TIMESTAMP WHERE entity_type='rental_mandate' AND entity_id=#{mandateId} AND status='pending'")
    int cancelPendingSignatures(@Param("mandateId") Long mandateId);
    @Select("SELECT d.original_name,d.storage_key,d.mime_type,d.file_size,d.document_type FROM documents d JOIN document_links dl ON dl.document_id=d.id WHERE d.id=#{documentId} AND dl.entity_type='rental_mandate' AND dl.entity_id=#{mandateId}") AttachmentFile findFile(@Param("mandateId") Long mandateId,@Param("documentId") Long documentId);
    class NewDocument { private Long id,uploadedBy,fileSize; private String documentNo,originalName,storageKey,mimeType,checksumSha256,documentType; public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUploadedBy(){return uploadedBy;} public void setUploadedBy(Long v){uploadedBy=v;} public Long getFileSize(){return fileSize;} public void setFileSize(Long v){fileSize=v;} public String getDocumentNo(){return documentNo;} public void setDocumentNo(String v){documentNo=v;} public String getOriginalName(){return originalName;} public void setOriginalName(String v){originalName=v;} public String getStorageKey(){return storageKey;} public void setStorageKey(String v){storageKey=v;} public String getMimeType(){return mimeType;} public void setMimeType(String v){mimeType=v;} public String getChecksumSha256(){return checksumSha256;} public void setChecksumSha256(String v){checksumSha256=v;} public String getDocumentType(){return documentType;} public void setDocumentType(String v){documentType=v;} }
    class AttachmentFile { private String originalName,storageKey,mimeType,documentType; private Long fileSize; public String getOriginalName(){return originalName;} public void setOriginalName(String v){originalName=v;} public String getStorageKey(){return storageKey;} public void setStorageKey(String v){storageKey=v;} public String getMimeType(){return mimeType;} public void setMimeType(String v){mimeType=v;} public Long getFileSize(){return fileSize;} public void setFileSize(Long v){fileSize=v;} public String getDocumentType(){return documentType;} public void setDocumentType(String v){documentType=v;} }
}
