package com.ccps.backend.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.ccps.backend.dto.ElectronicSignatureParticipantResponse;

@Mapper
public interface ElectronicSignatureMapper {
    @Select("""
            SELECT d.id,d.original_name,d.storage_key,d.mime_type,d.file_size,d.checksum_sha256
            FROM leases l JOIN documents d ON d.id=l.contract_document_id
            WHERE l.id=#{leaseId} AND d.status<>'superseded'
            """)
    DocumentRow findLeaseDocument(@Param("leaseId") Long leaseId);

    @Select("""
            SELECT d.id,d.original_name,d.storage_key,d.mime_type,d.file_size,d.checksum_sha256,dl.relation_type
            FROM documents d JOIN document_links dl ON dl.document_id=d.id
            WHERE dl.entity_type='rental_mandate' AND dl.entity_id=#{mandateId} AND d.id=#{documentId}
              AND d.status NOT IN ('superseded','voided')
            """)
    DocumentRow findMandateDocument(@Param("mandateId") Long mandateId, @Param("documentId") Long documentId);
    @Select("SELECT status FROM rental_mandates WHERE id=#{mandateId}")
    String findMandateStatus(@Param("mandateId") Long mandateId);

    @Select("SELECT COUNT(*) FROM electronic_signature_requests WHERE source_document_id=#{documentId} AND entity_type=#{entityType} AND entity_id=#{entityId} AND status='signed'")
    int countSignedRequests(@Param("documentId") Long documentId, @Param("entityType") String entityType,
            @Param("entityId") Long entityId);

    @Select("""
            SELECT COUNT(*)
            FROM electronic_signature_requests sr
            JOIN documents d ON d.id=sr.signed_document_id
            WHERE sr.entity_type=#{entityType} AND sr.entity_id=#{entityId}
              AND sr.status='signed' AND d.status NOT IN ('voided','superseded')
            """)
    int countActiveSignedByEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    @Select("SELECT COUNT(*) FROM electronic_signature_requests WHERE entity_type=#{entityType} AND entity_id=#{entityId} AND status='pending'")
    int countPendingRequests(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    @Select("""
            SELECT COUNT(*) FROM electronic_signature_requests
            WHERE COALESCE(root_document_id,source_document_id)=#{rootDocumentId}
              AND signer_role=#{signerRole} AND status='signed'
            """)
    int countSignedRole(@Param("rootDocumentId") Long rootDocumentId, @Param("signerRole") String signerRole);

    @Select("""
            SELECT COUNT(*) FROM electronic_signature_requests
            WHERE COALESCE(root_document_id,source_document_id)=#{rootDocumentId} AND status='pending'
            """)
    int countPendingRequestsForRoot(@Param("rootDocumentId") Long rootDocumentId);

    @Select("""
            SELECT d.id,d.original_name,d.storage_key,d.mime_type,d.file_size,d.checksum_sha256,
                   sr.document_kind AS relation_type
            FROM electronic_signature_requests sr
            JOIN documents d ON d.id=sr.signed_document_id
            WHERE COALESCE(sr.root_document_id,sr.source_document_id)=#{rootDocumentId}
              AND sr.signing_order=#{signingOrder} AND sr.status='signed'
              AND d.status NOT IN ('voided','superseded')
            ORDER BY sr.id DESC LIMIT 1
            """)
    DocumentRow findSignedStepDocument(@Param("rootDocumentId") Long rootDocumentId,
            @Param("signingOrder") int signingOrder);

    @Update("""
            UPDATE electronic_signature_requests
            SET status='cancelled', updated_at=CURRENT_TIMESTAMP
            WHERE entity_type=#{entityType} AND entity_id=#{entityId} AND status='pending'
            """)
    int cancelPendingRequests(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    @Update("""
            UPDATE documents d
            JOIN electronic_signature_requests sr ON sr.signed_document_id=d.id
            SET d.status='voided',d.updated_at=CURRENT_TIMESTAMP
            WHERE COALESCE(sr.root_document_id,sr.source_document_id)=#{rootDocumentId}
              AND d.status NOT IN ('voided','superseded')
            """)
    int voidSignedDocumentsForRoot(@Param("rootDocumentId") Long rootDocumentId);

    @Update("""
            UPDATE electronic_signature_requests
            SET status='superseded',updated_at=CURRENT_TIMESTAMP
            WHERE COALESCE(root_document_id,source_document_id)=#{rootDocumentId}
              AND status IN ('pending','signed')
            """)
    int supersedeRequestsForRoot(@Param("rootDocumentId") Long rootDocumentId);

    @Insert("""
            INSERT INTO electronic_signature_participants
              (root_document_id,document_kind,signer_role,signing_order,signer_name,signer_email,
               expires_in_days,updated_by)
            VALUES(#{rootDocumentId},#{documentKind},#{signerRole},#{signingOrder},#{signerName},#{signerEmail},
                   #{expiresInDays},#{updatedBy})
            ON DUPLICATE KEY UPDATE document_kind=VALUES(document_kind),signing_order=VALUES(signing_order),
              signer_name=VALUES(signer_name),signer_email=VALUES(signer_email),
              expires_in_days=VALUES(expires_in_days),updated_by=VALUES(updated_by),updated_at=CURRENT_TIMESTAMP
            """)
    int upsertParticipant(ParticipantRow row);

    @Select("""
            SELECT signer_role,signing_order,signer_name,signer_email,expires_in_days
            FROM electronic_signature_participants
            WHERE root_document_id=#{rootDocumentId}
            ORDER BY signing_order
            """)
    List<ElectronicSignatureParticipantResponse> findParticipants(@Param("rootDocumentId") Long rootDocumentId);

    @Select("""
            SELECT root_document_id,document_kind,signer_role,signing_order,signer_name,signer_email,
                   expires_in_days,updated_by
            FROM electronic_signature_participants
            WHERE root_document_id=#{rootDocumentId} AND signing_order=#{signingOrder}
            """)
    ParticipantRow findParticipant(@Param("rootDocumentId") Long rootDocumentId,
            @Param("signingOrder") int signingOrder);

    @Insert("""
            INSERT INTO electronic_signature_requests
              (source_document_id,root_document_id,entity_type,entity_id,document_kind,signer_role,signing_order,
               signer_name,signer_email,access_token_hash,
               verification_code_hash,verification_expires_at,status,expires_at,requested_by,source_checksum_sha256)
            VALUES (#{sourceDocumentId},#{rootDocumentId},#{entityType},#{entityId},#{documentKind},#{signerRole},#{signingOrder},
                    #{signerName},#{signerEmail},#{accessTokenHash},
                     #{verificationCodeHash},#{verificationExpiresAt},#{status},#{expiresAt},#{requestedBy},#{sourceChecksumSha256})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRequest(NewRequest row);

    @Select("""
            SELECT sr.id,sr.source_document_id,sr.root_document_id,sr.entity_type,sr.entity_id,sr.document_kind,
                   sr.signer_role,sr.signing_order,sr.signer_name,sr.signer_email,sr.requested_by,
                   sr.verification_code_hash,sr.verification_expires_at,sr.status,sr.expires_at,sr.signed_at,
                   sr.signed_document_id,d.original_name,d.storage_key,d.mime_type,d.checksum_sha256,
                   signed.original_name AS signed_original_name,signed.storage_key AS signed_storage_key,
                   signed.status AS signed_document_status
            FROM electronic_signature_requests sr JOIN documents d ON d.id=sr.source_document_id
            LEFT JOIN documents signed ON signed.id=sr.signed_document_id
            WHERE sr.access_token_hash=#{accessTokenHash}
            """)
    RequestRow findByTokenHash(@Param("accessTokenHash") String accessTokenHash);

    @Select("SELECT id FROM documents WHERE id=#{documentId} FOR UPDATE")
    Long lockDocument(@Param("documentId") Long documentId);

    @Select("""
            SELECT id, display_name, email
            FROM users
            WHERE id = #{userId} AND status = 'active'
            """)
    RequesterRow findRequester(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO notifications
              (recipient_user_id, title, body, related_type, related_id, priority, status)
            VALUES
              (#{recipientUserId}, #{title}, #{body}, #{relatedType}, #{relatedId}, #{priority}, 'unread')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSignatureNotification(NewNotification row);

    @Insert("""
            INSERT INTO notification_deliveries
              (notification_id, channel, destination, status, attempt_count, sent_at)
            VALUES (#{notificationId}, 'in_app', NULL, 'sent', 1, NOW())
            ON DUPLICATE KEY UPDATE status = 'sent', sent_at = COALESCE(sent_at, NOW())
            """)
    int insertSignatureInAppDelivery(@Param("notificationId") Long notificationId);

    @Insert("""
            INSERT INTO notification_deliveries
              (notification_id, channel, destination, status, attempt_count)
            VALUES (#{notificationId}, 'email', #{destination}, 'pending', 0)
            ON DUPLICATE KEY UPDATE destination = VALUES(destination), status = 'pending'
            """)
    int insertSignatureEmailDelivery(@Param("notificationId") Long notificationId,
                                     @Param("destination") String destination);

    @Select("""
            SELECT d.id,d.original_name,d.storage_key,d.mime_type,d.file_size,d.checksum_sha256,
                   sr.document_kind AS relation_type
            FROM electronic_signature_requests sr
            JOIN documents d ON d.id=sr.signed_document_id
            WHERE COALESCE(sr.root_document_id,sr.source_document_id)=#{rootDocumentId}
              AND sr.status='signed' AND d.status NOT IN ('voided','superseded')
            ORDER BY sr.signed_at DESC,sr.id DESC LIMIT 1
            """)
    DocumentRow findLatestActiveSignedDocument(@Param("rootDocumentId") Long rootDocumentId);

    @Select("""
            SELECT COUNT(DISTINCT signer_role) FROM electronic_signature_requests
            WHERE COALESCE(root_document_id,source_document_id)=#{rootDocumentId} AND status='signed'
            """)
    int countSignedRolesForRoot(@Param("rootDocumentId") Long rootDocumentId);

    @Update("""
            UPDATE electronic_signature_requests
            SET verification_code_hash=#{codeHash},verification_expires_at=#{expiresAt},updated_at=CURRENT_TIMESTAMP
            WHERE id=#{requestId} AND status='pending'
            """)
    int updateVerificationCode(@Param("requestId") Long requestId, @Param("codeHash") String codeHash,
            @Param("expiresAt") LocalDateTime expiresAt);

    @Insert("""
            INSERT INTO documents(document_no,original_name,storage_key,mime_type,file_size,checksum_sha256,
              document_type,status,uploaded_by,reviewed_by,reviewed_at)
            VALUES(#{documentNo},#{originalName},#{storageKey},'application/pdf',#{fileSize},#{checksumSha256},
              'signed_contract','approved',#{uploadedBy},#{uploadedBy},CURRENT_TIMESTAMP)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertSignedDocument(NewDocument row);

    @Insert("""
            INSERT INTO document_links(document_id,entity_type,entity_id,relation_type)
            VALUES(#{documentId},#{entityType},#{entityId},'signed_contract')
            """)
    int insertSignedDocumentLink(@Param("documentId") Long documentId, @Param("entityType") String entityType,
            @Param("entityId") Long entityId);

    @Insert("""
            INSERT INTO document_links(document_id,entity_type,entity_id,relation_type)
            VALUES(#{documentId},'rental_mandate',#{mandateId},#{relationType})
            """)
    int insertSignedMandateDocumentLink(@Param("documentId") Long documentId,
            @Param("mandateId") Long mandateId, @Param("relationType") String relationType);

    @Update("""
            UPDATE documents d
            JOIN electronic_signature_requests sr ON sr.signed_document_id=d.id
            SET d.status='voided'
            WHERE COALESCE(sr.root_document_id,sr.source_document_id)=#{rootDocumentId}
              AND sr.status='signed' AND d.id<>#{newDocumentId}
              AND d.status NOT IN ('voided','superseded')
            """)
    int supersedePreviousSignedDocuments(@Param("rootDocumentId") Long rootDocumentId,
            @Param("newDocumentId") Long newDocumentId);

    @Update("""
            UPDATE electronic_signature_requests
            SET status='signed',signed_at=#{signedAt},signed_document_id=#{signedDocumentId},signature_hash=#{signatureHash},
                signer_ip=#{signerIp},signer_user_agent=#{signerUserAgent},updated_at=CURRENT_TIMESTAMP
            WHERE id=#{requestId} AND status='pending'
            """)
    int completeRequest(@Param("requestId") Long requestId, @Param("signedAt") LocalDateTime signedAt,
            @Param("signedDocumentId") Long signedDocumentId, @Param("signatureHash") String signatureHash,
            @Param("signerIp") String signerIp, @Param("signerUserAgent") String signerUserAgent);

    @Insert("""
            INSERT INTO electronic_signature_events(signature_request_id,event_type,detail,remote_ip,user_agent)
            VALUES(#{requestId},#{eventType},#{detail},#{remoteIp},#{userAgent})
            """)
    int insertEvent(@Param("requestId") Long requestId, @Param("eventType") String eventType,
            @Param("detail") String detail, @Param("remoteIp") String remoteIp, @Param("userAgent") String userAgent);

    @Insert("""
            INSERT INTO audit_logs(actor_user_id,action,entity_type,entity_id,before_data,after_data)
            VALUES(#{actorId},#{action},#{entityType},#{entityId},NULL,
              JSON_OBJECT('signatureRequestId',#{requestId},'signerName',#{signerName}))
            """)
    int insertAudit(@Param("actorId") Long actorId, @Param("action") String action, @Param("entityType") String entityType,
            @Param("entityId") Long entityId, @Param("requestId") Long requestId, @Param("signerName") String signerName);

    class DocumentRow {
        private Long id, fileSize; private String originalName, storageKey, mimeType, checksumSha256, relationType;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public Long getFileSize() { return fileSize; } public void setFileSize(Long value) { fileSize = value; }
        public String getOriginalName() { return originalName; } public void setOriginalName(String value) { originalName = value; }
        public String getStorageKey() { return storageKey; } public void setStorageKey(String value) { storageKey = value; }
        public String getMimeType() { return mimeType; } public void setMimeType(String value) { mimeType = value; }
        public String getChecksumSha256() { return checksumSha256; } public void setChecksumSha256(String value) { checksumSha256 = value; }
        public String getRelationType() { return relationType; } public void setRelationType(String value) { relationType = value; }
    }
    class RequesterRow {
        private Long id;
        private String displayName, email;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String value) { displayName = value; }
        public String getEmail() { return email; }
        public void setEmail(String value) { email = value; }
    }
    class NewNotification {
        private Long id, recipientUserId, relatedId;
        private String title, body, relatedType, priority;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public Long getRecipientUserId() { return recipientUserId; }
        public void setRecipientUserId(Long value) { recipientUserId = value; }
        public Long getRelatedId() { return relatedId; }
        public void setRelatedId(Long value) { relatedId = value; }
        public String getTitle() { return title; }
        public void setTitle(String value) { title = value; }
        public String getBody() { return body; }
        public void setBody(String value) { body = value; }
        public String getRelatedType() { return relatedType; }
        public void setRelatedType(String value) { relatedType = value; }
        public String getPriority() { return priority; }
        public void setPriority(String value) { priority = value; }
    }
    class NewRequest {
        private Long id, sourceDocumentId, rootDocumentId, entityId, requestedBy; private Integer signingOrder;
        private String entityType, documentKind, signerRole, signerName, signerEmail, accessTokenHash, verificationCodeHash, sourceChecksumSha256, status;
        private LocalDateTime verificationExpiresAt, expiresAt;
        public Long getId(){return id;} public void setId(Long value){id=value;} public Long getSourceDocumentId(){return sourceDocumentId;} public void setSourceDocumentId(Long value){sourceDocumentId=value;} public Long getEntityId(){return entityId;} public void setEntityId(Long value){entityId=value;} public Long getRequestedBy(){return requestedBy;} public void setRequestedBy(Long value){requestedBy=value;} public String getEntityType(){return entityType;} public void setEntityType(String value){entityType=value;} public String getSignerName(){return signerName;} public void setSignerName(String value){signerName=value;} public String getSignerEmail(){return signerEmail;} public void setSignerEmail(String value){signerEmail=value;} public String getAccessTokenHash(){return accessTokenHash;} public void setAccessTokenHash(String value){accessTokenHash=value;} public String getVerificationCodeHash(){return verificationCodeHash;} public void setVerificationCodeHash(String value){verificationCodeHash=value;} public String getSourceChecksumSha256(){return sourceChecksumSha256;} public void setSourceChecksumSha256(String value){sourceChecksumSha256=value;} public LocalDateTime getVerificationExpiresAt(){return verificationExpiresAt;} public void setVerificationExpiresAt(LocalDateTime value){verificationExpiresAt=value;} public LocalDateTime getExpiresAt(){return expiresAt;} public void setExpiresAt(LocalDateTime value){expiresAt=value;}
        public Long getRootDocumentId(){return rootDocumentId;} public void setRootDocumentId(Long value){rootDocumentId=value;} public Integer getSigningOrder(){return signingOrder;} public void setSigningOrder(Integer value){signingOrder=value;} public String getDocumentKind(){return documentKind;} public void setDocumentKind(String value){documentKind=value;} public String getSignerRole(){return signerRole;} public void setSignerRole(String value){signerRole=value;} public String getStatus(){return status;} public void setStatus(String value){status=value;}
    }
    class ParticipantRow {
        private Long rootDocumentId, updatedBy; private Integer signingOrder, expiresInDays;
        private String documentKind, signerRole, signerName, signerEmail;
        public Long getRootDocumentId(){return rootDocumentId;} public void setRootDocumentId(Long value){rootDocumentId=value;}
        public Long getUpdatedBy(){return updatedBy;} public void setUpdatedBy(Long value){updatedBy=value;}
        public Integer getSigningOrder(){return signingOrder;} public void setSigningOrder(Integer value){signingOrder=value;}
        public Integer getExpiresInDays(){return expiresInDays;} public void setExpiresInDays(Integer value){expiresInDays=value;}
        public String getDocumentKind(){return documentKind;} public void setDocumentKind(String value){documentKind=value;}
        public String getSignerRole(){return signerRole;} public void setSignerRole(String value){signerRole=value;}
        public String getSignerName(){return signerName;} public void setSignerName(String value){signerName=value;}
        public String getSignerEmail(){return signerEmail;} public void setSignerEmail(String value){signerEmail=value;}
    }
    class NewDocument {
        private Long id, fileSize, uploadedBy; private String documentNo, originalName, storageKey, checksumSha256;
        public Long getId(){return id;} public void setId(Long value){id=value;} public Long getFileSize(){return fileSize;} public void setFileSize(Long value){fileSize=value;} public Long getUploadedBy(){return uploadedBy;} public void setUploadedBy(Long value){uploadedBy=value;} public String getDocumentNo(){return documentNo;} public void setDocumentNo(String value){documentNo=value;} public String getOriginalName(){return originalName;} public void setOriginalName(String value){originalName=value;} public String getStorageKey(){return storageKey;} public void setStorageKey(String value){storageKey=value;} public String getChecksumSha256(){return checksumSha256;} public void setChecksumSha256(String value){checksumSha256=value;}
    }
    class RequestRow {
        private Long id, sourceDocumentId, rootDocumentId, entityId, signedDocumentId, requestedBy; private Integer signingOrder;
        private String entityType, documentKind, signerRole, signerName, signerEmail, verificationCodeHash, status, originalName, storageKey, mimeType, checksumSha256, signedOriginalName, signedStorageKey, signedDocumentStatus;
        private LocalDateTime verificationExpiresAt, expiresAt, signedAt;
        public Long getId(){return id;} public void setId(Long value){id=value;} public Long getSourceDocumentId(){return sourceDocumentId;} public void setSourceDocumentId(Long value){sourceDocumentId=value;} public Long getEntityId(){return entityId;} public void setEntityId(Long value){entityId=value;} public Long getSignedDocumentId(){return signedDocumentId;} public void setSignedDocumentId(Long value){signedDocumentId=value;} public Long getRequestedBy(){return requestedBy;} public void setRequestedBy(Long value){requestedBy=value;} public String getEntityType(){return entityType;} public void setEntityType(String value){entityType=value;} public String getSignerName(){return signerName;} public void setSignerName(String value){signerName=value;} public String getSignerEmail(){return signerEmail;} public void setSignerEmail(String value){signerEmail=value;} public String getVerificationCodeHash(){return verificationCodeHash;} public void setVerificationCodeHash(String value){verificationCodeHash=value;} public String getStatus(){return status;} public void setStatus(String value){status=value;} public String getOriginalName(){return originalName;} public void setOriginalName(String value){originalName=value;} public String getStorageKey(){return storageKey;} public void setStorageKey(String value){storageKey=value;} public String getMimeType(){return mimeType;} public void setMimeType(String value){mimeType=value;} public String getChecksumSha256(){return checksumSha256;} public void setChecksumSha256(String value){checksumSha256=value;} public String getSignedOriginalName(){return signedOriginalName;} public void setSignedOriginalName(String value){signedOriginalName=value;} public String getSignedStorageKey(){return signedStorageKey;} public void setSignedStorageKey(String value){signedStorageKey=value;} public String getSignedDocumentStatus(){return signedDocumentStatus;} public void setSignedDocumentStatus(String value){signedDocumentStatus=value;} public LocalDateTime getVerificationExpiresAt(){return verificationExpiresAt;} public void setVerificationExpiresAt(LocalDateTime value){verificationExpiresAt=value;} public LocalDateTime getExpiresAt(){return expiresAt;} public void setExpiresAt(LocalDateTime value){expiresAt=value;} public LocalDateTime getSignedAt(){return signedAt;} public void setSignedAt(LocalDateTime value){signedAt=value;}
        public Long getRootDocumentId(){return rootDocumentId;} public void setRootDocumentId(Long value){rootDocumentId=value;} public Integer getSigningOrder(){return signingOrder;} public void setSigningOrder(Integer value){signingOrder=value;} public String getDocumentKind(){return documentKind;} public void setDocumentKind(String value){documentKind=value;} public String getSignerRole(){return signerRole;} public void setSignerRole(String value){signerRole=value;}
    }
}
