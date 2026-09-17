package com.ccps.backend.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface OwnerSignatureMapper {
    // Both an explicit assignment and current property ownership are required.
    String OWNERSHIP = """
        o.status='active' AND u.status='active' AND EXISTS (
          SELECT 1 FROM owner_units ou
          WHERE ou.owner_id=o.id AND ou.status='active' AND (
            (sr.entity_type='rental_mandate' AND EXISTS (
              SELECT 1 FROM rental_mandates rm WHERE rm.id=sr.entity_id AND rm.owner_unit_id=ou.id))
            OR (sr.entity_type='lease' AND EXISTS (
              SELECT 1 FROM leases l WHERE l.id=sr.entity_id AND l.unit_id=ou.unit_id))
          )
        )
        """;
    String ACTIVE_DOCUMENT = """
        d.status NOT IN ('superseded','voided') AND root.status NOT IN ('superseded','voided')
        """;

    @Select("""
        SELECT DISTINCT o.id AS owner_id,o.user_id,o.full_name
        FROM electronic_signature_requests sr
        JOIN owners o ON o.user_id IS NOT NULL JOIN users u ON u.id=o.user_id
        WHERE sr.id=#{requestId} AND sr.signer_role IN ('owner','second_owner') AND
        """ + OWNERSHIP + " ORDER BY o.full_name,o.id")
    List<Recipient> recipients(Long requestId);

    @Select("SELECT owner_id FROM owner_signature_tasks WHERE request_id=#{requestId}")
    Long assignedOwner(Long requestId);

    @Insert("""
        INSERT INTO owner_signature_tasks(request_id,owner_id,recipient_user_id,assigned_by)
        VALUES(#{requestId},#{ownerId},#{userId},#{actorId})
        """)
    int assign(@Param("requestId") Long requestId, @Param("ownerId") Long ownerId,
               @Param("userId") Long userId, @Param("actorId") Long actorId);

    @Select("SELECT id FROM electronic_signature_requests WHERE id=#{requestId} FOR UPDATE")
    Long lockRequest(Long requestId);

    String TASK_FROM = """
        FROM owner_signature_tasks t
        JOIN electronic_signature_requests sr ON sr.id=t.request_id
        JOIN owners o ON o.id=t.owner_id AND o.user_id=t.recipient_user_id
        JOIN users u ON u.id=t.recipient_user_id
        JOIN documents d ON d.id=sr.source_document_id
        JOIN documents root ON root.id=COALESCE(sr.root_document_id,sr.source_document_id)
        WHERE t.recipient_user_id=#{userId} AND sr.signer_role IN ('owner','second_owner') AND
        """ + OWNERSHIP;

    @Select("""
        SELECT sr.id,d.original_name AS document_name,sr.signer_name,sr.status,sr.expires_at,sr.signed_at,
               (sr.status='pending' AND sr.expires_at>CURRENT_TIMESTAMP AND
        """ + ACTIVE_DOCUMENT + ") AS can_sign " + TASK_FROM + " ORDER BY t.created_at DESC,sr.id DESC")
    List<Task> tasks(Long userId);

    @Select("SELECT COUNT(*) " + TASK_FROM + " AND sr.id=#{requestId} AND " + ACTIVE_DOCUMENT)
    int authorized(@Param("userId") Long userId, @Param("requestId") Long requestId);

    @Select("""
        SELECT COUNT(*) FROM electronic_signature_requests sr
        JOIN documents d ON d.id=sr.source_document_id
        JOIN documents root ON root.id=COALESCE(sr.root_document_id,sr.source_document_id)
        WHERE sr.id=#{requestId} AND
        """ + ACTIVE_DOCUMENT)
    int activeDocument(Long requestId);

    record Recipient(Long ownerId, Long userId, String fullName) {}
    record Task(Long id, String documentName, String signerName, String status,
                LocalDateTime expiresAt, LocalDateTime signedAt, boolean canSign) {}
}
