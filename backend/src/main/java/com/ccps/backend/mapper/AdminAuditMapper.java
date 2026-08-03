package com.ccps.backend.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.ccps.backend.dto.AdminAuditOptionsResponse.Actor;
import com.ccps.backend.dto.AdminAuditResponse.Item;

@Mapper
public interface AdminAuditMapper {
    String AUDIT_FROM = """
            FROM audit_logs al
            LEFT JOIN users u ON u.id = al.actor_user_id
            """;

    @Select({"<script>", "SELECT COUNT(*)", AUDIT_FROM, "<where>",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ', al.action, al.entity_type, al.entity_id, u.display_name, CAST(al.before_data AS CHAR), CAST(al.after_data AS CHAR)) LIKE CONCAT('%', #{keyword}, '%')</if>",
            "<if test=\"action != null and action != ''\">AND al.action = #{action}</if>",
            "<if test=\"actorId != null\">AND al.actor_user_id = #{actorId}</if>",
            "<if test=\"startDate != null\">AND DATE(al.created_at) &gt;= #{startDate}</if>",
            "<if test=\"endDate != null\">AND DATE(al.created_at) &lt;= #{endDate}</if>",
            "</where>", "</script>"})
    long count(@Param("keyword") String keyword, @Param("action") String action, @Param("actorId") Long actorId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select({"<script>",
            "SELECT al.id, al.actor_user_id AS actorUserId, COALESCE(u.display_name, '系統') AS actorName, al.action,",
            "al.entity_type AS entityType, al.entity_id AS entityId, CAST(al.before_data AS CHAR) AS beforeData,",
            "CAST(al.after_data AS CHAR) AS afterData, al.ip_address AS ipAddress, al.created_at AS createdAt",
            AUDIT_FROM, "<where>",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ', al.action, al.entity_type, al.entity_id, u.display_name, CAST(al.before_data AS CHAR), CAST(al.after_data AS CHAR)) LIKE CONCAT('%', #{keyword}, '%')</if>",
            "<if test=\"action != null and action != ''\">AND al.action = #{action}</if>",
            "<if test=\"actorId != null\">AND al.actor_user_id = #{actorId}</if>",
            "<if test=\"startDate != null\">AND DATE(al.created_at) &gt;= #{startDate}</if>",
            "<if test=\"endDate != null\">AND DATE(al.created_at) &lt;= #{endDate}</if>",
            "</where>", "ORDER BY al.created_at DESC, al.id DESC", "LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<Item> find(@Param("keyword") String keyword, @Param("action") String action, @Param("actorId") Long actorId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select("""
            SELECT DISTINCT u.id, u.display_name AS name
            FROM audit_logs al JOIN users u ON u.id = al.actor_user_id
            ORDER BY u.display_name
            """)
    List<Actor> findActors();

    @Select("SELECT DISTINCT action FROM audit_logs ORDER BY action")
    List<String> findActions();

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, before_data, after_data)
            VALUES (#{actorId}, #{action}, #{entityType}, #{entityId}, #{beforeData}, #{afterData})
            """)
    int insert(@Param("actorId") Long actorId, @Param("action") String action,
            @Param("entityType") String entityType, @Param("entityId") Long entityId,
            @Param("beforeData") String beforeData, @Param("afterData") String afterData);
}
