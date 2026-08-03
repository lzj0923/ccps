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
import com.ccps.backend.dto.AdminMaintenanceOptionsResponse.UnitOption;
import com.ccps.backend.dto.AdminMaintenanceOptionsResponse.VendorOption;
import com.ccps.backend.dto.AdminPropertyMaintenanceResponse;

@Mapper
public interface AdminMaintenanceMapper {
    @Select("""
            SELECT u.id AS unit_id, o.id AS owner_id, o.full_name AS owner_name,
                   p.name AS project_name, u.unit_no, ra.id AS reserve_account_id,
                   COALESCE(ra.current_balance, 0) AS reserve_balance
            FROM owner_units ou
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN reserve_accounts ra ON ra.owner_unit_id = ou.id AND ra.status = 'active'
            WHERE ou.status = 'active' AND ou.asset_stage = 'OPERATING' AND ou.is_primary = 1
            ORDER BY p.name, u.unit_no
            """)
    List<UnitOption> findUnitOptions();

    @Select("SELECT id, name, contact_name, phone FROM vendors WHERE status = 'active' ORDER BY name")
    List<VendorOption> findVendorOptions();

    @Select("""
            SELECT ou.id AS owner_unit_id, ou.owner_id, ou.unit_id
            FROM owner_units ou
            WHERE ou.id = #{ownerUnitId} AND ou.owner_id = #{ownerId} AND ou.status = 'active'
            LIMIT 1
            """)
    PropertyContext findPropertyContext(@Param("ownerId") Long ownerId,
            @Param("ownerUnitId") Long ownerUnitId);

    @Select("""
            SELECT mwo.id, mwo.work_order_no, mwo.vendor_id, v.name AS vendor_name,
                   mwo.category, mwo.title, mwo.description, mwo.requested_at,
                   mwo.completed_at, mwo.status, mwo.estimated_amount, mwo.actual_amount,
                   mwo.cashflow_entry_id,
                   (SELECT COUNT(*) FROM document_links dl JOIN documents d ON d.id = dl.document_id
                    WHERE dl.entity_type = 'work_order' AND dl.entity_id = mwo.id
                      AND d.status = 'active') AS attachment_count,
                   mwo.updated_at
            FROM maintenance_work_orders mwo
            LEFT JOIN vendors v ON v.id = mwo.vendor_id
            WHERE mwo.unit_id = #{unitId} AND mwo.status <> 'cancelled'
            ORDER BY mwo.requested_at DESC, mwo.id DESC
            """)
    List<AdminPropertyMaintenanceResponse> findPropertyMaintenance(@Param("unitId") Long unitId);

    @Select("""
            SELECT mwo.id, mwo.work_order_no, mwo.vendor_id, v.name AS vendor_name,
                   mwo.category, mwo.title, mwo.description, mwo.requested_at,
                   mwo.completed_at, mwo.status, mwo.estimated_amount, mwo.actual_amount,
                   mwo.cashflow_entry_id,
                   (SELECT COUNT(*) FROM document_links dl JOIN documents d ON d.id = dl.document_id
                    WHERE dl.entity_type = 'work_order' AND dl.entity_id = mwo.id
                      AND d.status = 'active') AS attachment_count,
                   mwo.updated_at
            FROM maintenance_work_orders mwo
            LEFT JOIN vendors v ON v.id = mwo.vendor_id
            WHERE mwo.id = #{workOrderId} AND mwo.unit_id = #{unitId}
            LIMIT 1
            """)
    AdminPropertyMaintenanceResponse findPropertyMaintenanceById(@Param("unitId") Long unitId,
            @Param("workOrderId") Long workOrderId);

    @Update("""
            UPDATE maintenance_work_orders
            SET vendor_id = #{vendorId}, category = #{category}, title = #{title},
                description = #{description}, requested_at = #{requestedAt},
                estimated_amount = #{estimatedAmount}, status = #{status}
            WHERE id = #{workOrderId} AND unit_id = #{unitId}
              AND status NOT IN ('completed', 'cancelled')
            """)
    int updatePropertyMaintenance(@Param("unitId") Long unitId,
            @Param("workOrderId") Long workOrderId, @Param("vendorId") Long vendorId,
            @Param("category") String category, @Param("title") String title,
            @Param("description") String description, @Param("requestedAt") LocalDateTime requestedAt,
            @Param("estimatedAmount") BigDecimal estimatedAmount, @Param("status") String status);

    @Update("""
            UPDATE maintenance_work_orders
            SET status = 'cancelled'
            WHERE id = #{workOrderId} AND unit_id = #{unitId} AND status <> 'cancelled'
            """)
    int cancelPropertyMaintenance(@Param("unitId") Long unitId,
            @Param("workOrderId") Long workOrderId);

    @Insert("""
            INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by)
            VALUES (#{workOrderId}, #{status}, NOW(), #{note}, #{actorId})
            """)
    int insertPropertyHistory(@Param("workOrderId") Long workOrderId, @Param("status") String status,
            @Param("note") String note, @Param("actorId") Long actorId);

    @Select("""
            SELECT u.id AS unit_id, o.id AS owner_id, ou.id AS owner_unit_id,
                   ra.id AS reserve_account_id, COALESCE(ra.current_balance, 0) AS reserve_balance
            FROM units u
            JOIN owner_units ou ON ou.unit_id = u.id AND ou.status = 'active'
              AND ou.asset_stage = 'OPERATING' AND ou.is_primary = 1
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            LEFT JOIN reserve_accounts ra ON ra.owner_unit_id = ou.id AND ra.status = 'active'
            WHERE u.id = #{unitId}
            LIMIT 1 FOR UPDATE
            """)
    UnitContext lockUnitContext(@Param("unitId") Long unitId);

    @Select("""
            SELECT mwo.id, mwo.status, mwo.unit_id, COALESCE(mwo.owner_id, ou.owner_id) AS owner_id, mwo.vendor_id,
                   mwo.cashflow_entry_id, ce.finance_record_id,
                   ou.id AS owner_unit_id, ra.id AS reserve_account_id,
                   COALESCE(ra.current_balance, 0) AS reserve_balance,
                   COALESCE((SELECT SUM(rt.amount) FROM reserve_transactions rt
                     WHERE rt.transaction_type = 'debit'
                       AND (rt.maintenance_work_order_id = mwo.id
                            OR rt.finance_record_id = ce.finance_record_id)), 0)
                     AS reserve_deducted_amount
            FROM maintenance_work_orders mwo
            LEFT JOIN cashflow_entries ce ON ce.id = mwo.cashflow_entry_id
            LEFT JOIN owner_units ou ON ou.unit_id = mwo.unit_id AND ou.status = 'active' AND ou.is_primary = 1
            LEFT JOIN reserve_accounts ra ON ra.owner_unit_id = ou.id AND ra.status = 'active'
            WHERE mwo.id = #{workOrderId}
            LIMIT 1
            FOR UPDATE
            """)
    CompletionContext lockContext(@Param("workOrderId") Long workOrderId);

    @Select("""
            SELECT COUNT(*) FROM document_links dl
            JOIN documents d ON d.id = dl.document_id
            WHERE dl.entity_type = 'work_order' AND dl.entity_id = #{workOrderId}
              AND dl.relation_type = #{relationType}
              AND d.document_type = 'maintenance_attachment' AND d.status = 'active'
            """)
    int countPhotos(@Param("workOrderId") Long workOrderId, @Param("relationType") String relationType);

    @Insert("""
            INSERT INTO finance_records
              (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date,
               payment_method, payment_status, confirmation_status, confirmed_by, confirmed_at,
               sync_status, created_by)
            VALUES
              (#{transactionNo}, 'cashflow', #{unitId}, #{ownerId}, #{amount}, 'MYR', CURRENT_DATE,
               #{paymentMethod}, 'paid', #{confirmationStatus},
               CASE WHEN #{confirmationStatus} = 'confirmed' THEN #{actorId} ELSE NULL END,
               CASE WHEN #{confirmationStatus} = 'confirmed' THEN NOW() ELSE NULL END,
               'not_synced', #{actorId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertFinance(NewFinance finance);

    @Update("""
            UPDATE finance_records
            SET amount = #{amount}, transaction_date = CURRENT_DATE, payment_method = #{paymentMethod},
                payment_status = 'paid', confirmation_status = #{confirmationStatus},
                confirmed_by = CASE WHEN #{confirmationStatus} = 'confirmed' THEN #{actorId} ELSE NULL END,
                confirmed_at = CASE WHEN #{confirmationStatus} = 'confirmed' THEN NOW() ELSE NULL END,
                sync_status = 'not_synced'
            WHERE id = #{financeRecordId}
            """)
    int updateFinance(@Param("financeRecordId") Long financeRecordId, @Param("amount") BigDecimal amount,
            @Param("paymentMethod") String paymentMethod,
            @Param("confirmationStatus") String confirmationStatus, @Param("actorId") Long actorId);

    @Insert("""
            INSERT INTO cashflow_entries
              (finance_record_id, unit_id, owner_id, vendor_id, direction, category, description,
               occurred_on, reserve_account_id, attachment_status)
            VALUES
              (#{financeRecordId}, #{unitId}, #{ownerId}, #{vendorId}, 'expense', 'maintenance',
               #{description}, CURRENT_DATE, #{reserveAccountId}, 'complete')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCashflow(NewCashflow cashflow);

    @Update("""
            UPDATE cashflow_entries
            SET reserve_account_id = #{reserveAccountId}, attachment_status = 'complete',
                description = #{description}, occurred_on = CURRENT_DATE
            WHERE id = #{cashflowEntryId}
            """)
    int updateCashflow(@Param("cashflowEntryId") Long cashflowEntryId,
            @Param("reserveAccountId") Long reserveAccountId, @Param("description") String description);

    @Update("""
            UPDATE reserve_transactions
            SET maintenance_work_order_id = #{workOrderId}
            WHERE finance_record_id = #{financeRecordId} AND transaction_type = 'debit'
              AND maintenance_work_order_id IS NULL
            """)
    int linkReserveDebitToWorkOrder(@Param("financeRecordId") Long financeRecordId,
            @Param("workOrderId") Long workOrderId);

    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM reserve_transactions
            WHERE transaction_type = 'debit'
              AND (finance_record_id = #{financeRecordId} OR maintenance_work_order_id = #{workOrderId})
            """)
    BigDecimal findReserveDebitAmount(@Param("financeRecordId") Long financeRecordId,
            @Param("workOrderId") Long workOrderId);

    @Update("""
            UPDATE maintenance_work_orders
            SET cashflow_entry_id = #{cashflowEntryId}, actual_amount = #{amount},
                completed_at = NOW(), status = 'completed'
            WHERE id = #{workOrderId}
            """)
    int completeWorkOrder(@Param("workOrderId") Long workOrderId,
            @Param("cashflowEntryId") Long cashflowEntryId, @Param("amount") BigDecimal amount);

    @Insert("""
            INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by)
            VALUES (#{workOrderId}, 'completed', NOW(), #{note}, #{actorId})
            """)
    int insertHistory(@Param("workOrderId") Long workOrderId, @Param("note") String note,
            @Param("actorId") Long actorId);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, after_data)
            VALUES (#{actorId}, 'complete_maintenance', 'maintenance_work_order', #{workOrderId},
                    JSON_OBJECT('amount', #{amount}, 'settlementMethod', #{settlementMethod}))
            """)
    int insertAudit(@Param("actorId") Long actorId, @Param("workOrderId") Long workOrderId,
            @Param("amount") BigDecimal amount, @Param("settlementMethod") String settlementMethod);

    @Insert("""
            INSERT INTO finance_records
              (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date,
               payment_method, payment_status, confirmation_status, confirmed_by, confirmed_at,
               sync_status, created_by)
            VALUES
              (#{transactionNo}, 'cashflow', #{unitId}, #{ownerId}, #{amount}, 'MYR', #{occurredOn},
               #{paymentMethod}, #{paymentStatus}, #{confirmationStatus},
               CASE WHEN #{confirmationStatus} = 'confirmed' THEN #{actorId} ELSE NULL END,
               CASE WHEN #{confirmationStatus} = 'confirmed' THEN NOW() ELSE NULL END,
               'not_synced', #{actorId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertExpenseFinance(NewExpenseFinance finance);

    @Insert("""
            INSERT INTO cashflow_entries
              (finance_record_id, unit_id, owner_id, direction, category, description,
               occurred_on, reserve_account_id, attachment_status)
            VALUES
              (#{financeRecordId}, #{unitId}, #{ownerId}, 'expense', #{category}, #{description},
               #{occurredOn}, #{reserveAccountId}, 'not_required')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertExpenseCashflow(NewExpenseCashflow cashflow);

    @Insert("""
            INSERT INTO maintenance_work_orders
              (work_order_no, unit_id, owner_id, vendor_id, category, title, description,
               requested_at, status, estimated_amount, created_by)
            VALUES
              (#{workOrderNo}, #{unitId}, #{ownerId}, #{vendorId}, #{category}, #{title},
               #{description}, #{requestedAt}, 'submitted', #{estimatedAmount}, #{actorId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertWorkOrder(NewWorkOrder workOrder);

    @Insert("""
            INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by)
            VALUES (#{workOrderId}, 'submitted', #{occurredAt}, '管理員建立維修工單', #{actorId})
            """)
    int insertSubmittedHistory(@Param("workOrderId") Long workOrderId,
            @Param("occurredAt") LocalDateTime occurredAt, @Param("actorId") Long actorId);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, after_data)
            VALUES (#{actorId}, #{action}, #{entityType}, #{entityId}, #{afterData})
            """)
    int insertCreateAudit(@Param("actorId") Long actorId, @Param("action") String action,
            @Param("entityType") String entityType, @Param("entityId") Long entityId,
            @Param("afterData") String afterData);

    class PropertyContext {
        private Long ownerUnitId; private Long ownerId; private Long unitId;
        public Long getOwnerUnitId() { return ownerUnitId; } public void setOwnerUnitId(Long value) { ownerUnitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
    }

    class UnitContext {
        private Long unitId; private Long ownerId; private Long ownerUnitId;
        private Long reserveAccountId; private BigDecimal reserveBalance;
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public Long getOwnerUnitId() { return ownerUnitId; } public void setOwnerUnitId(Long value) { ownerUnitId = value; }
        public Long getReserveAccountId() { return reserveAccountId; } public void setReserveAccountId(Long value) { reserveAccountId = value; }
        public BigDecimal getReserveBalance() { return reserveBalance; } public void setReserveBalance(BigDecimal value) { reserveBalance = value; }
    }

    class NewExpenseFinance {
        private Long id; private String transactionNo; private Long unitId; private Long ownerId;
        private BigDecimal amount; private LocalDate occurredOn; private String paymentMethod;
        private String paymentStatus; private String confirmationStatus; private Long actorId;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public String getTransactionNo() { return transactionNo; } public void setTransactionNo(String value) { transactionNo = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal value) { amount = value; }
        public LocalDate getOccurredOn() { return occurredOn; } public void setOccurredOn(LocalDate value) { occurredOn = value; }
        public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String value) { paymentMethod = value; }
        public String getPaymentStatus() { return paymentStatus; } public void setPaymentStatus(String value) { paymentStatus = value; }
        public String getConfirmationStatus() { return confirmationStatus; } public void setConfirmationStatus(String value) { confirmationStatus = value; }
        public Long getActorId() { return actorId; } public void setActorId(Long value) { actorId = value; }
    }

    class NewExpenseCashflow {
        private Long id; private Long financeRecordId; private Long unitId; private Long ownerId;
        private String category; private String description; private LocalDate occurredOn; private Long reserveAccountId;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public Long getFinanceRecordId() { return financeRecordId; } public void setFinanceRecordId(Long value) { financeRecordId = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public String getCategory() { return category; } public void setCategory(String value) { category = value; }
        public String getDescription() { return description; } public void setDescription(String value) { description = value; }
        public LocalDate getOccurredOn() { return occurredOn; } public void setOccurredOn(LocalDate value) { occurredOn = value; }
        public Long getReserveAccountId() { return reserveAccountId; } public void setReserveAccountId(Long value) { reserveAccountId = value; }
    }

    class NewWorkOrder {
        private Long id; private String workOrderNo; private Long unitId; private Long ownerId; private Long vendorId;
        private String category; private String title; private String description; private LocalDateTime requestedAt;
        private BigDecimal estimatedAmount; private Long actorId;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public String getWorkOrderNo() { return workOrderNo; } public void setWorkOrderNo(String value) { workOrderNo = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public Long getVendorId() { return vendorId; } public void setVendorId(Long value) { vendorId = value; }
        public String getCategory() { return category; } public void setCategory(String value) { category = value; }
        public String getTitle() { return title; } public void setTitle(String value) { title = value; }
        public String getDescription() { return description; } public void setDescription(String value) { description = value; }
        public LocalDateTime getRequestedAt() { return requestedAt; } public void setRequestedAt(LocalDateTime value) { requestedAt = value; }
        public BigDecimal getEstimatedAmount() { return estimatedAmount; } public void setEstimatedAmount(BigDecimal value) { estimatedAmount = value; }
        public Long getActorId() { return actorId; } public void setActorId(Long value) { actorId = value; }
    }

    class CompletionContext {
        private Long id; private String status; private Long unitId; private Long ownerId; private Long vendorId;
        private Long cashflowEntryId; private Long financeRecordId; private Long ownerUnitId;
        private Long reserveAccountId; private BigDecimal reserveBalance; private BigDecimal reserveDeductedAmount;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public String getStatus() { return status; } public void setStatus(String value) { status = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public Long getVendorId() { return vendorId; } public void setVendorId(Long value) { vendorId = value; }
        public Long getCashflowEntryId() { return cashflowEntryId; } public void setCashflowEntryId(Long value) { cashflowEntryId = value; }
        public Long getFinanceRecordId() { return financeRecordId; } public void setFinanceRecordId(Long value) { financeRecordId = value; }
        public Long getOwnerUnitId() { return ownerUnitId; } public void setOwnerUnitId(Long value) { ownerUnitId = value; }
        public Long getReserveAccountId() { return reserveAccountId; } public void setReserveAccountId(Long value) { reserveAccountId = value; }
        public BigDecimal getReserveBalance() { return reserveBalance; } public void setReserveBalance(BigDecimal value) { reserveBalance = value; }
        public BigDecimal getReserveDeductedAmount() { return reserveDeductedAmount; } public void setReserveDeductedAmount(BigDecimal value) { reserveDeductedAmount = value; }
    }

    class NewFinance {
        private Long id; private String transactionNo; private Long unitId; private Long ownerId;
        private BigDecimal amount; private String paymentMethod; private String confirmationStatus; private Long actorId;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public String getTransactionNo() { return transactionNo; } public void setTransactionNo(String value) { transactionNo = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal value) { amount = value; }
        public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String value) { paymentMethod = value; }
        public String getConfirmationStatus() { return confirmationStatus; } public void setConfirmationStatus(String value) { confirmationStatus = value; }
        public Long getActorId() { return actorId; } public void setActorId(Long value) { actorId = value; }
    }

    class NewCashflow {
        private Long id; private Long financeRecordId; private Long unitId; private Long ownerId;
        private Long vendorId; private String description; private Long reserveAccountId;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public Long getFinanceRecordId() { return financeRecordId; } public void setFinanceRecordId(Long value) { financeRecordId = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public Long getVendorId() { return vendorId; } public void setVendorId(Long value) { vendorId = value; }
        public String getDescription() { return description; } public void setDescription(String value) { description = value; }
        public Long getReserveAccountId() { return reserveAccountId; } public void setReserveAccountId(Long value) { reserveAccountId = value; }
    }
}
