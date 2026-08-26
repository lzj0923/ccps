package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OwnerPropertyCashflowMapper {

    @Select("""
            SELECT ou.id AS owner_unit_id, ou.unit_id, ou.owner_id, p.name AS project_name, u.unit_no
            FROM owner_units ou
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE ou.id = #{ownerUnitId}
              AND ou.status = 'active'
              AND o.user_id = #{userId}
            """)
    PropertyContext findProperty(@Param("userId") Long userId, @Param("ownerUnitId") Long ownerUnitId);

    @Select("""
            SELECT ce.id, ce.direction, ce.category, ce.description, fr.amount,
                   ce.occurred_on, fr.created_at AS occurred_at, fr.confirmation_status AS status,
                   CASE WHEN fr.record_type = 'rent_payment' THEN 'rent' ELSE 'cashflow' END AS source
            FROM cashflow_entries ce
            JOIN finance_records fr ON fr.id = ce.finance_record_id
            WHERE ce.unit_id = #{unitId}
              AND ce.owner_id = #{ownerId}
              AND COALESCE(fr.payment_status, '') <> 'voided'
              AND fr.confirmation_status = 'confirmed'
            ORDER BY ce.occurred_on DESC, ce.id DESC
            """)
    List<CashflowRow> findCashflows(@Param("unitId") Long unitId, @Param("ownerId") Long ownerId);

    @Select("""
            SELECT rt.id,
                   CASE
                     WHEN rt.transaction_type = 'debit' OR rt.amount < 0 THEN 'expense'
                     ELSE 'income'
                   END AS direction,
                   'reserve' AS category,
                   COALESCE(rt.note, '預備金異動') AS description,
                   ABS(rt.amount) AS amount,
                   DATE(rt.occurred_at) AS occurred_on,
                   rt.occurred_at,
                   'confirmed' AS status,
                   'reserve' AS source
            FROM reserve_accounts ra
            JOIN reserve_transactions rt ON rt.reserve_account_id = ra.id
            WHERE ra.owner_unit_id = #{ownerUnitId}
              AND NOT EXISTS (
                SELECT 1
                FROM cashflow_entries linked_ce
                WHERE linked_ce.finance_record_id = rt.finance_record_id
              )
            ORDER BY rt.occurred_at DESC, rt.id DESC
            """)
    List<CashflowRow> findReserveTransactions(@Param("ownerUnitId") Long ownerUnitId);

    class PropertyContext {
        private Long ownerUnitId;
        private Long unitId;
        private Long ownerId;
        private String projectName;
        private String unitNo;

        public Long getOwnerUnitId() { return ownerUnitId; }
        public void setOwnerUnitId(Long value) { ownerUnitId = value; }
        public Long getUnitId() { return unitId; }
        public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long value) { ownerId = value; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String value) { projectName = value; }
        public String getUnitNo() { return unitNo; }
        public void setUnitNo(String value) { unitNo = value; }
    }

    class CashflowRow {
        private Long id;
        private String source;
        private String direction;
        private String category;
        private String description;
        private BigDecimal amount;
        private LocalDate occurredOn;
        private LocalDateTime occurredAt;
        private String status;

        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public String getSource() { return source; }
        public void setSource(String value) { source = value; }
        public String getDirection() { return direction; }
        public void setDirection(String value) { direction = value; }
        public String getCategory() { return category; }
        public void setCategory(String value) { category = value; }
        public String getDescription() { return description; }
        public void setDescription(String value) { description = value; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal value) { amount = value; }
        public LocalDate getOccurredOn() { return occurredOn; }
        public void setOccurredOn(LocalDate value) { occurredOn = value; }
        public LocalDateTime getOccurredAt() { return occurredAt; }
        public void setOccurredAt(LocalDateTime value) { occurredAt = value; }
        public String getStatus() { return status; }
        public void setStatus(String value) { status = value; }
    }
}
