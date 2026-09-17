package com.ccps.backend.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface MetaDataDeletionMapper {
    record RequestRow(Long id, String requestHash, String confirmationCode, String metaUserId,
            String status, LocalDateTime receivedAt, LocalDateTime updatedAt, Long reviewedBy, String reviewNote) {}

    String COLUMNS = "id, request_hash AS requestHash, confirmation_code AS confirmationCode, "
            + "meta_user_id AS metaUserId, status, received_at AS receivedAt, updated_at AS updatedAt, "
            + "reviewed_by AS reviewedBy, review_note AS reviewNote";

    // A provider retry must never reset an existing request's status or confirmation code.
    @Insert("""
        INSERT INTO meta_data_deletion_requests (request_hash, confirmation_code, meta_user_id)
        VALUES (#{hash}, #{code}, #{userId})
        ON DUPLICATE KEY UPDATE request_hash = request_hash
        """)
    int insertIfAbsent(@Param("hash") String hash, @Param("code") String code, @Param("userId") String userId);

    @Select("SELECT " + COLUMNS + " FROM meta_data_deletion_requests WHERE request_hash = #{hash} FOR UPDATE")
    RequestRow byHash(String hash);

    @Select("SELECT " + COLUMNS + " FROM meta_data_deletion_requests WHERE confirmation_code = #{code}")
    RequestRow byCode(String code);

    @Select("SELECT " + COLUMNS + " FROM meta_data_deletion_requests WHERE id = #{id} FOR UPDATE")
    RequestRow lockById(Long id);

    @Select("SELECT " + COLUMNS + " FROM meta_data_deletion_requests ORDER BY received_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}")
    List<RequestRow> list(@Param("limit") int limit, @Param("offset") int offset);

    @Update("""
        UPDATE meta_data_deletion_requests
        SET status = #{status}, review_note = #{note}, reviewed_by = #{actor}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
        """)
    int review(@Param("id") Long id, @Param("status") String status,
            @Param("note") String note, @Param("actor") Long actor);
}
