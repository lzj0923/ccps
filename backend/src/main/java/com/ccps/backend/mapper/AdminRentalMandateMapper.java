package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminRentalMandateMapper {
    String SELECT = """
            SELECT rm.id, rm.mandate_no, rm.owner_unit_id, ou.owner_id, o.full_name AS owner_name,
                   COALESCE(NULLIF(TRIM(o.identity_no), ''), NULLIF(TRIM(o.passport_no), '')) AS owner_identity,
                   o.email AS owner_email,
                   u.project_id, p.name AS project_name, u.unit_no, rm.mandate_type,
                   rm.start_date, rm.end_date, rm.management_fee, rm.commission_percent,
                   rm.responsible_user_id, ru.display_name AS responsible_user_name, rm.status,
                   rm.review_note, rm.termination_reason, rm.submitted_at, rm.reviewed_at,
                   rv.display_name AS reviewed_by_name, rm.created_at
            FROM rental_mandates rm
            JOIN owner_units ou ON ou.id = rm.owner_unit_id
            JOIN owners o ON o.id = ou.owner_id
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN users ru ON ru.id = rm.responsible_user_id
            LEFT JOIN users rv ON rv.id = rm.reviewed_by
            """;

    String WHERE = """
            <where>
              <if test="status != null and status != ''">AND rm.status = #{status}</if>
              <if test="keyword != null and keyword != ''">
                AND CONCAT_WS(' ', rm.mandate_no, o.full_name, p.name, u.unit_no) LIKE CONCAT('%', #{keyword}, '%')
              </if>
            </where>
            """;

    @Select({"<script>", SELECT, WHERE,
            "ORDER BY FIELD(rm.status, 'pending_review', 'active', 'suspended', 'draft', 'terminated'), rm.created_at DESC",
            "LIMIT #{limit} OFFSET #{offset}", "</script>"})
    List<MandateRow> findPage(@Param("status") String status, @Param("keyword") String keyword,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>", "SELECT COUNT(*) FROM rental_mandates rm JOIN owner_units ou ON ou.id = rm.owner_unit_id",
            "JOIN owners o ON o.id = ou.owner_id JOIN units u ON u.id = ou.unit_id JOIN projects p ON p.id = u.project_id",
            WHERE, "</script>"})
    long countPage(@Param("status") String status, @Param("keyword") String keyword);

    @Select("""
            SELECT COUNT(*) AS total,
              SUM(status = 'draft') AS draft,
              SUM(status = 'pending_review') AS pending_review,
              SUM(status = 'active') AS active,
              SUM(status = 'suspended') AS suspended,
              SUM(status = 'terminated') AS `terminated`
            FROM rental_mandates
            """)
    SummaryRow summary();

    @Select("""
            SELECT ou.id, CONCAT(p.name, ' · ', u.unit_no, ' · ', o.full_name) AS label,
                   ou.owner_id, o.full_name AS owner_name, p.id AS project_id,
                   p.name AS project_name, u.unit_no
            FROM owner_units ou
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id AND p.status = 'active'
            WHERE ou.status = 'active' AND ou.asset_stage = 'OPERATING'
              AND NOT EXISTS (SELECT 1 FROM rental_mandates existing WHERE existing.owner_unit_id = ou.id AND existing.status IN ('draft','pending_review','active','suspended'))
            ORDER BY p.name, u.unit_no, o.full_name
            """)
    List<UnitOptionRow> findOperatingUnits();

    @Select("SELECT id, display_name AS name FROM users WHERE status = 'active' ORDER BY display_name")
    List<UserOptionRow> findActiveUsers();

    @Select("SELECT COUNT(*) FROM owner_units ou WHERE ou.id = #{ownerUnitId} AND ou.status = 'active' AND ou.asset_stage = 'OPERATING' AND NOT EXISTS (SELECT 1 FROM rental_mandates existing WHERE existing.owner_unit_id = ou.id AND existing.status IN ('draft','pending_review','active','suspended'))")
    int countOperatingUnit(@Param("ownerUnitId") Long ownerUnitId);

    @Select("SELECT COUNT(*) FROM rental_mandates WHERE owner_unit_id = #{ownerUnitId} AND status IN ('pending_review', 'active', 'suspended') AND (end_date IS NULL OR end_date >= #{startDate}) AND (start_date <= COALESCE(#{endDate}, '9999-12-31'))")
    int countOverlapping(@Param("ownerUnitId") Long ownerUnitId, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("SELECT COUNT(*) FROM users WHERE id = #{userId} AND status = 'active'")
    int countActiveUser(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO rental_mandates (owner_unit_id, mandate_no, mandate_type, start_date, end_date,
              management_fee, commission_percent, responsible_user_id, status, created_by)
            VALUES (#{ownerUnitId}, #{mandateNo}, #{mandateType}, #{startDate}, #{endDate},
              #{managementFee}, #{commissionPercent}, #{responsibleUserId}, 'active', #{createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertMandate(NewMandate mandate);

    @Select(SELECT + " WHERE rm.id = #{id}")
    MandateRow findById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM rental_mandates WHERE owner_unit_id = #{ownerUnitId} AND id <> #{id} AND status IN ('pending_review','active','suspended') AND (end_date IS NULL OR end_date >= #{startDate}) AND start_date <= COALESCE(#{endDate}, '9999-12-31')")
    int countOtherOverlapping(@Param("id") Long id, @Param("ownerUnitId") Long ownerUnitId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("SELECT COUNT(*) FROM leases WHERE rental_mandate_id=#{id} AND status IN ('draft','active') AND (start_date < #{startDate} OR (#{endDate} IS NOT NULL AND end_date > #{endDate}))")
    int countLeasesOutsideTerm(@Param("id") Long id, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Update("UPDATE rental_mandates SET mandate_type=#{request.mandateType}, start_date=#{request.startDate}, end_date=#{request.endDate}, management_fee=#{request.managementFee}, commission_percent=#{request.commissionPercent}, responsible_user_id=#{request.responsibleUserId} WHERE id=#{id} AND status IN ('active','suspended')")
    int updateDetails(@Param("id") Long id, @Param("request") com.ccps.backend.dto.AdminRentalMandateCreateRequest request);

    @Select("SELECT status FROM rental_mandates WHERE id = #{id} FOR UPDATE")
    String lockStatus(@Param("id") Long id);

    @Select("""
            SELECT COUNT(*)
            FROM rental_mandates rm
            JOIN owner_units ou ON ou.id = rm.owner_unit_id
            JOIN leases l ON l.unit_id = ou.unit_id
            WHERE rm.id = #{mandateId}
              AND l.status = 'active'
              AND l.start_date <= CURRENT_DATE
              AND (l.end_date IS NULL OR l.end_date >= CURRENT_DATE)
            """)
    int countActiveLeasesForMandate(@Param("mandateId") Long mandateId);

    @Select("SELECT id, status FROM rental_mandates WHERE end_date IS NOT NULL AND end_date < CURRENT_DATE AND status IN ('active', 'suspended') ORDER BY id")
    List<ExpiredMandateRow> findExpiredMandates();

    @Update("UPDATE rental_mandates SET status = 'expired', termination_reason = '委托期限已到期' WHERE id = #{id} AND status = #{fromStatus} AND end_date IS NOT NULL AND end_date < CURRENT_DATE")
    int expireMandate(@Param("id") Long id, @Param("fromStatus") String fromStatus);

    @Update("UPDATE rental_mandates SET status = #{status}, submitted_at = CASE WHEN #{status} = 'pending_review' THEN CURRENT_TIMESTAMP ELSE submitted_at END, reviewed_by = #{reviewedBy}, reviewed_at = #{reviewedAt}, review_note = #{reviewNote}, termination_reason = #{terminationReason} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("reviewedBy") Long reviewedBy,
            @Param("reviewedAt") LocalDateTime reviewedAt, @Param("reviewNote") String reviewNote,
            @Param("terminationReason") String terminationReason);

    @Update("""
            INSERT INTO owner_unit_services (owner_unit_id, service_type, status, started_at, ended_at)
            SELECT owner_unit_id, 'RENTAL', 'active', CURRENT_DATE, NULL
            FROM rental_mandates WHERE id = #{id}
            ON DUPLICATE KEY UPDATE status = 'active', ended_at = NULL
            """)
    int activateRentalService(@Param("id") Long id);

    @Update("""
            UPDATE owner_unit_services ous
            JOIN rental_mandates rm ON rm.owner_unit_id = ous.owner_unit_id
            SET ous.status = CASE WHEN #{status} = 'terminated' THEN 'ended' ELSE 'paused' END,
                ous.ended_at = CASE WHEN #{status} = 'terminated' THEN CURRENT_DATE ELSE ous.ended_at END
            WHERE rm.id = #{id} AND ous.service_type = 'RENTAL'
            """)
    int updateRentalService(@Param("id") Long id, @Param("status") String status);

    @Update("""
            UPDATE finance_records fr
            JOIN rental_mandates rm ON rm.id = #{mandateId}
            JOIN owner_units ou ON ou.id = rm.owner_unit_id
              AND ou.owner_id = fr.owner_id AND ou.unit_id = fr.unit_id
            SET fr.payment_status = 'voided', fr.confirmation_status = 'rejected',
                fr.sync_status = 'not_synced', fr.sync_batch_id = NULL,
                fr.confirmed_by = #{actorId}, fr.confirmed_at = CURRENT_TIMESTAMP
            WHERE fr.record_type = 'property_expense'
              AND fr.payment_method = 'direct_payment'
              AND fr.payment_status = 'unpaid'
              AND fr.confirmation_status = 'pending'
            """)
    int withdrawPendingDirectPayments(@Param("mandateId") Long mandateId,
            @Param("actorId") Long actorId);

    @Insert("INSERT INTO rental_mandate_status_history (mandate_id, from_status, to_status, reason, changed_by) VALUES (#{mandateId}, #{fromStatus}, #{toStatus}, #{reason}, #{changedBy})")
    int insertHistory(@Param("mandateId") Long mandateId, @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus, @Param("reason") String reason, @Param("changedBy") Long changedBy);

    @Select("SELECT h.id, h.mandate_id, h.from_status, h.to_status, h.reason, h.changed_by, u.display_name AS changed_by_name, h.changed_at FROM rental_mandate_status_history h LEFT JOIN users u ON u.id = h.changed_by WHERE h.mandate_id = #{mandateId} ORDER BY h.changed_at DESC, h.id DESC")
    List<HistoryRow> findHistory(@Param("mandateId") Long mandateId);

    @Insert("INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, before_data, after_data) VALUES (#{actorId}, #{action}, 'rental_mandate', #{mandateId}, #{beforeData}, #{afterData})")
    int insertAudit(@Param("actorId") Long actorId, @Param("action") String action,
            @Param("mandateId") Long mandateId, @Param("beforeData") String beforeData,
            @Param("afterData") String afterData);

    class NewMandate {
        private Long id; private Long ownerUnitId; private String mandateNo; private String mandateType;
        private LocalDate startDate; private LocalDate endDate; private BigDecimal managementFee;
        private BigDecimal commissionPercent; private Long responsibleUserId; private Long createdBy;
        public Long getId() { return id; } public void setId(Long id) { this.id = id; }
        public Long getOwnerUnitId() { return ownerUnitId; } public void setOwnerUnitId(Long v) { ownerUnitId = v; }
        public String getMandateNo() { return mandateNo; } public void setMandateNo(String v) { mandateNo = v; }
        public String getMandateType() { return mandateType; } public void setMandateType(String v) { mandateType = v; }
        public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate v) { startDate = v; }
        public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate v) { endDate = v; }
        public BigDecimal getManagementFee() { return managementFee; } public void setManagementFee(BigDecimal v) { managementFee = v; }
        public BigDecimal getCommissionPercent() { return commissionPercent; } public void setCommissionPercent(BigDecimal v) { commissionPercent = v; }
        public Long getResponsibleUserId() { return responsibleUserId; } public void setResponsibleUserId(Long v) { responsibleUserId = v; }
        public Long getCreatedBy() { return createdBy; } public void setCreatedBy(Long v) { createdBy = v; }
    }

    class MandateRow {
        private Long id, ownerUnitId, ownerId, projectId, responsibleUserId; private String mandateNo, ownerName, ownerIdentity, ownerEmail, projectName, unitNo, mandateType, responsibleUserName, status, reviewNote, terminationReason, reviewedByName; private LocalDate startDate, endDate; private BigDecimal managementFee, commissionPercent; private LocalDateTime submittedAt, reviewedAt, createdAt;
        public Long getId() { return id; } public void setId(Long v) { id = v; } public Long getOwnerUnitId() { return ownerUnitId; } public void setOwnerUnitId(Long v) { ownerUnitId = v; } public Long getOwnerId() { return ownerId; } public void setOwnerId(Long v) { ownerId = v; } public Long getProjectId() { return projectId; } public void setProjectId(Long v) { projectId = v; } public Long getResponsibleUserId() { return responsibleUserId; } public void setResponsibleUserId(Long v) { responsibleUserId = v; } public String getMandateNo() { return mandateNo; } public void setMandateNo(String v) { mandateNo = v; } public String getOwnerName() { return ownerName; } public void setOwnerName(String v) { ownerName = v; } public String getOwnerIdentity() { return ownerIdentity; } public void setOwnerIdentity(String v) { ownerIdentity = v; } public String getOwnerEmail() { return ownerEmail; } public void setOwnerEmail(String v) { ownerEmail = v; } public String getProjectName() { return projectName; } public void setProjectName(String v) { projectName = v; } public String getUnitNo() { return unitNo; } public void setUnitNo(String v) { unitNo = v; } public String getMandateType() { return mandateType; } public void setMandateType(String v) { mandateType = v; } public LocalDate getStartDate() { return startDate; } public void setStartDate(LocalDate v) { startDate = v; } public LocalDate getEndDate() { return endDate; } public void setEndDate(LocalDate v) { endDate = v; } public BigDecimal getManagementFee() { return managementFee; } public void setManagementFee(BigDecimal v) { managementFee = v; } public BigDecimal getCommissionPercent() { return commissionPercent; } public void setCommissionPercent(BigDecimal v) { commissionPercent = v; } public String getResponsibleUserName() { return responsibleUserName; } public void setResponsibleUserName(String v) { responsibleUserName = v; } public String getStatus() { return status; } public void setStatus(String v) { status = v; } public String getReviewNote() { return reviewNote; } public void setReviewNote(String v) { reviewNote = v; } public String getTerminationReason() { return terminationReason; } public void setTerminationReason(String v) { terminationReason = v; } public LocalDateTime getSubmittedAt() { return submittedAt; } public void setSubmittedAt(LocalDateTime v) { submittedAt = v; } public LocalDateTime getReviewedAt() { return reviewedAt; } public void setReviewedAt(LocalDateTime v) { reviewedAt = v; } public String getReviewedByName() { return reviewedByName; } public void setReviewedByName(String v) { reviewedByName = v; } public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    }

    class SummaryRow { private Long total, draft, pendingReview, active, suspended, terminated; public Long getTotal() { return total; } public void setTotal(Long v) { total = v; } public Long getDraft() { return draft; } public void setDraft(Long v) { draft = v; } public Long getPendingReview() { return pendingReview; } public void setPendingReview(Long v) { pendingReview = v; } public Long getActive() { return active; } public void setActive(Long v) { active = v; } public Long getSuspended() { return suspended; } public void setSuspended(Long v) { suspended = v; } public Long getTerminated() { return terminated; } public void setTerminated(Long v) { terminated = v; } }
    class UnitOptionRow { private Long id, ownerId, projectId; private String label, ownerName, projectName, unitNo; public Long getId() { return id; } public void setId(Long v) { id = v; } public Long getOwnerId() { return ownerId; } public void setOwnerId(Long v) { ownerId = v; } public Long getProjectId() { return projectId; } public void setProjectId(Long v) { projectId = v; } public String getLabel() { return label; } public void setLabel(String v) { label = v; } public String getOwnerName() { return ownerName; } public void setOwnerName(String v) { ownerName = v; } public String getProjectName() { return projectName; } public void setProjectName(String v) { projectName = v; } public String getUnitNo() { return unitNo; } public void setUnitNo(String v) { unitNo = v; } }
    class UserOptionRow { private Long id; private String name; public Long getId() { return id; } public void setId(Long v) { id = v; } public String getName() { return name; } public void setName(String v) { name = v; } }
    class HistoryRow { private Long id, mandateId, changedBy; private String fromStatus, toStatus, reason, changedByName; private LocalDateTime changedAt; public Long getId() { return id; } public void setId(Long v) { id = v; } public Long getMandateId() { return mandateId; } public void setMandateId(Long v) { mandateId = v; } public Long getChangedBy() { return changedBy; } public void setChangedBy(Long v) { changedBy = v; } public String getFromStatus() { return fromStatus; } public void setFromStatus(String v) { fromStatus = v; } public String getToStatus() { return toStatus; } public void setToStatus(String v) { toStatus = v; } public String getReason() { return reason; } public void setReason(String v) { reason = v; } public String getChangedByName() { return changedByName; } public void setChangedByName(String v) { changedByName = v; } public LocalDateTime getChangedAt() { return changedAt; } public void setChangedAt(LocalDateTime v) { changedAt = v; } }
    class ExpiredMandateRow { private Long id; private String status; public Long getId() { return id; } public void setId(Long v) { id = v; } public String getStatus() { return status; } public void setStatus(String v) { status = v; } }
}
