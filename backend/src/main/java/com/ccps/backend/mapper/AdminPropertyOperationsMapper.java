package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminPropertyOperationsMapper {
    @Select("""
            SELECT l.id, u.id AS unit_id, l.lease_no, l.tenant_id, t.full_name AS tenant_name,
                   l.start_date, l.end_date, l.monthly_rent,
                   l.rental_space_id, rs.space_name AS rental_space_name, rs.space_type AS rental_space_type
            FROM owner_units ou
            JOIN units u ON u.id = ou.unit_id
            JOIN leases l ON l.unit_id = u.id AND l.status = 'active'
            JOIN tenants t ON t.id = l.tenant_id
            LEFT JOIN rental_spaces rs ON rs.id = l.rental_space_id
            WHERE ou.id = #{ownerUnitId} AND ou.owner_id = #{ownerId}
            ORDER BY l.start_date DESC, l.id DESC
            LIMIT 1
            """)
    LeaseRow findActiveLease(@Param("ownerId") Long ownerId, @Param("ownerUnitId") Long ownerUnitId);

    @Select("""
            SELECT l.id, u.id AS unit_id, l.lease_no, l.tenant_id, t.full_name AS tenant_name,
                   l.start_date, l.end_date, l.monthly_rent,
                   l.rental_space_id, rs.space_name AS rental_space_name, rs.space_type AS rental_space_type
            FROM owner_units ou
            JOIN units u ON u.id = ou.unit_id
            JOIN leases l ON l.unit_id = u.id AND l.status = 'active'
            JOIN tenants t ON t.id = l.tenant_id
            LEFT JOIN rental_spaces rs ON rs.id = l.rental_space_id
            WHERE ou.id = #{ownerUnitId} AND ou.owner_id = #{ownerId}
            ORDER BY CASE WHEN rs.space_type = 'whole_unit' THEN 0 ELSE 1 END,
                     rs.sort_order, rs.id, l.start_date DESC, l.id DESC
            """)
    List<LeaseRow> findActiveLeases(@Param("ownerId") Long ownerId, @Param("ownerUnitId") Long ownerUnitId);

    @Select("""
            SELECT id, billing_month, due_date, amount_due, amount_paid,
                   GREATEST(amount_due - amount_paid, 0) AS amount_unpaid,
                   CASE WHEN amount_paid >= amount_due THEN 'paid'
                        WHEN amount_paid > 0 THEN 'partial'
                        WHEN due_date < CURRENT_DATE THEN 'overdue'
                        ELSE 'unpaid' END AS status
            FROM rent_invoices
            WHERE lease_id = #{leaseId} AND billing_month = #{billingMonth}
            LIMIT 1
            """)
    InvoiceRow findInvoice(@Param("leaseId") Long leaseId, @Param("billingMonth") LocalDate billingMonth);

    @Select("SELECT id, billing_month, due_date, amount_due, amount_paid, GREATEST(amount_due - amount_paid, 0) AS amount_unpaid, "
            + "CASE WHEN amount_paid >= amount_due THEN 'paid' WHEN amount_paid > 0 THEN 'partial' "
            + "WHEN due_date < CURRENT_DATE THEN 'overdue' ELSE 'unpaid' END AS status "
            + "FROM rent_invoices WHERE id=#{invoiceId} AND lease_id=#{leaseId} LIMIT 1 FOR UPDATE")
    InvoiceRow lockInvoice(@Param("leaseId") Long leaseId, @Param("invoiceId") Long invoiceId);

    @Insert("INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, tenant_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status, created_by) "
            + "VALUES (#{transactionNo}, 'tenant_charge', #{unitId}, #{ownerId}, #{tenantId}, #{amount}, 'MYR', #{transactionDate}, 'tenant_invoice', 'unpaid', 'pending', 'not_synced', #{actorId})")
    @Options(useGeneratedKeys = true, keyProperty = "financeRecordId")
    int insertChargeFinanceReview(ChargeWriteRow row);

    @Insert("INSERT INTO rent_invoice_items (invoice_id, finance_record_id, charge_type, description, amount, payer, source_type, source_id, created_by) "
            + "VALUES (#{invoiceId}, #{financeRecordId}, #{chargeType}, #{description}, #{amount}, #{payer}, #{sourceType}, #{sourceId}, #{actorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCharge(ChargeWriteRow row);

    @Update("UPDATE rent_invoices SET amount_due=amount_due+#{amount} WHERE id=#{invoiceId} AND amount_paid+#{amount} >= amount_paid")
    int increaseInvoiceAmount(@Param("invoiceId") Long invoiceId, @Param("amount") BigDecimal amount);

    @Select("""
            SELECT rii.id, rii.finance_record_id, rii.charge_type, rii.description, rii.amount, rii.payer,
                   rii.source_type, rii.source_id, rii.created_at,
                   COALESCE(fr.confirmation_status, 'confirmed') AS confirmation_status
            FROM rent_invoice_items rii
            LEFT JOIN finance_records fr ON fr.id = rii.finance_record_id
            WHERE rii.invoice_id = #{invoiceId}
            ORDER BY rii.id
            """)
    List<ChargeRow> findCharges(@Param("invoiceId") Long invoiceId);

    @Select("""
            SELECT mwo.id, mwo.work_order_no, mwo.category, mwo.title, mwo.description,
                   mwo.requested_at, mwo.status, mwo.estimated_amount, mwo.actual_amount,
                   mwo.vendor_id, v.name AS vendor_name
            FROM maintenance_work_orders mwo
            LEFT JOIN vendors v ON v.id = mwo.vendor_id
            WHERE mwo.lease_id = #{leaseId} AND mwo.status <> 'cancelled'
            ORDER BY mwo.requested_at DESC, mwo.id DESC
            """)
    List<WorkOrderRow> findWorkOrders(@Param("leaseId") Long leaseId);

    @Insert("INSERT INTO maintenance_work_orders (work_order_no, unit_id, lease_id, owner_id, tenant_id, vendor_id, category, title, description, requested_at, status, estimated_amount, created_by) "
            + "VALUES (#{workOrderNo}, #{unitId}, #{leaseId}, #{ownerId}, #{tenantId}, #{vendorId}, #{category}, #{title}, #{description}, #{requestedAt}, 'submitted', #{estimatedAmount}, #{actorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertWorkOrder(WorkOrderWriteRow row);

    @Insert("INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by) "
            + "VALUES (#{workOrderId}, 'submitted', #{requestedAt}, '运营中心建立维修工单', #{actorId})")
    int insertWorkOrderHistory(@Param("workOrderId") Long workOrderId, @Param("requestedAt") LocalDateTime requestedAt,
            @Param("actorId") Long actorId);

    @Insert("INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, after_data) "
            + "VALUES (#{actorId}, 'create_operations_work_order', 'maintenance_work_order', #{workOrderId}, #{afterData})")
    int insertWorkOrderAudit(@Param("actorId") Long actorId, @Param("workOrderId") Long workOrderId,
            @Param("afterData") String afterData);

    class LeaseRow {
        private Long id, unitId, tenantId, rentalSpaceId;
        private String leaseNo, tenantName, rentalSpaceName, rentalSpaceType;
        private LocalDate startDate, endDate;
        private BigDecimal monthlyRent;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public Long getUnitId() { return unitId; }
        public void setUnitId(Long value) { unitId = value; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long value) { tenantId = value; }
        public String getLeaseNo() { return leaseNo; }
        public void setLeaseNo(String value) { leaseNo = value; }
        public String getTenantName() { return tenantName; }
        public void setTenantName(String value) { tenantName = value; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate value) { startDate = value; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate value) { endDate = value; }
        public BigDecimal getMonthlyRent() { return monthlyRent; }
        public void setMonthlyRent(BigDecimal value) { monthlyRent = value; }
        public Long getRentalSpaceId() { return rentalSpaceId; }
        public void setRentalSpaceId(Long value) { rentalSpaceId = value; }
        public String getRentalSpaceName() { return rentalSpaceName; }
        public void setRentalSpaceName(String value) { rentalSpaceName = value; }
        public String getRentalSpaceType() { return rentalSpaceType; }
        public void setRentalSpaceType(String value) { rentalSpaceType = value; }
    }

    class InvoiceRow {
        private Long id;
        private LocalDate billingMonth, dueDate;
        private BigDecimal amountDue, amountPaid, amountUnpaid;
        private String status;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public LocalDate getBillingMonth() { return billingMonth; }
        public void setBillingMonth(LocalDate value) { billingMonth = value; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate value) { dueDate = value; }
        public BigDecimal getAmountDue() { return amountDue; }
        public void setAmountDue(BigDecimal value) { amountDue = value; }
        public BigDecimal getAmountPaid() { return amountPaid; }
        public void setAmountPaid(BigDecimal value) { amountPaid = value; }
        public BigDecimal getAmountUnpaid() { return amountUnpaid; }
        public void setAmountUnpaid(BigDecimal value) { amountUnpaid = value; }
        public String getStatus() { return status; }
        public void setStatus(String value) { status = value; }
    }

    class ChargeRow {
        private Long id, sourceId, financeRecordId;
        private String chargeType, description, payer, sourceType, confirmationStatus;
        private BigDecimal amount;
        private LocalDateTime createdAt;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public Long getSourceId() { return sourceId; }
        public void setSourceId(Long value) { sourceId = value; }
        public Long getFinanceRecordId() { return financeRecordId; }
        public void setFinanceRecordId(Long value) { financeRecordId = value; }
        public String getChargeType() { return chargeType; }
        public void setChargeType(String value) { chargeType = value; }
        public String getDescription() { return description; }
        public void setDescription(String value) { description = value; }
        public String getPayer() { return payer; }
        public void setPayer(String value) { payer = value; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String value) { sourceType = value; }
        public String getConfirmationStatus() { return confirmationStatus; }
        public void setConfirmationStatus(String value) { confirmationStatus = value; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal value) { amount = value; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    }

    class WorkOrderRow {
        private Long id, vendorId;
        private String workOrderNo, category, title, description, status, vendorName;
        private LocalDateTime requestedAt;
        private BigDecimal estimatedAmount, actualAmount;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public Long getVendorId() { return vendorId; }
        public void setVendorId(Long value) { vendorId = value; }
        public String getWorkOrderNo() { return workOrderNo; }
        public void setWorkOrderNo(String value) { workOrderNo = value; }
        public String getCategory() { return category; }
        public void setCategory(String value) { category = value; }
        public String getTitle() { return title; }
        public void setTitle(String value) { title = value; }
        public String getDescription() { return description; }
        public void setDescription(String value) { description = value; }
        public String getStatus() { return status; }
        public void setStatus(String value) { status = value; }
        public String getVendorName() { return vendorName; }
        public void setVendorName(String value) { vendorName = value; }
        public LocalDateTime getRequestedAt() { return requestedAt; }
        public void setRequestedAt(LocalDateTime value) { requestedAt = value; }
        public BigDecimal getEstimatedAmount() { return estimatedAmount; }
        public void setEstimatedAmount(BigDecimal value) { estimatedAmount = value; }
        public BigDecimal getActualAmount() { return actualAmount; }
        public void setActualAmount(BigDecimal value) { actualAmount = value; }
    }

    class WorkOrderWriteRow {
        private Long id, unitId, leaseId, ownerId, tenantId, vendorId, actorId;
        private String workOrderNo, category, title, description;
        private LocalDateTime requestedAt;
        private BigDecimal estimatedAmount;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public Long getUnitId() { return unitId; }
        public void setUnitId(Long value) { unitId = value; }
        public Long getLeaseId() { return leaseId; }
        public void setLeaseId(Long value) { leaseId = value; }
        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long value) { ownerId = value; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long value) { tenantId = value; }
        public Long getVendorId() { return vendorId; }
        public void setVendorId(Long value) { vendorId = value; }
        public Long getActorId() { return actorId; }
        public void setActorId(Long value) { actorId = value; }
        public String getWorkOrderNo() { return workOrderNo; }
        public void setWorkOrderNo(String value) { workOrderNo = value; }
        public String getCategory() { return category; }
        public void setCategory(String value) { category = value; }
        public String getTitle() { return title; }
        public void setTitle(String value) { title = value; }
        public String getDescription() { return description; }
        public void setDescription(String value) { description = value; }
        public LocalDateTime getRequestedAt() { return requestedAt; }
        public void setRequestedAt(LocalDateTime value) { requestedAt = value; }
        public BigDecimal getEstimatedAmount() { return estimatedAmount; }
        public void setEstimatedAmount(BigDecimal value) { estimatedAmount = value; }
    }

    class ChargeWriteRow {
        private Long id, invoiceId, sourceId, actorId, financeRecordId, unitId, ownerId, tenantId;
        private String chargeType, description, payer, sourceType;
        private BigDecimal amount;
        private String transactionNo;
        private LocalDate transactionDate;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public Long getInvoiceId() { return invoiceId; }
        public void setInvoiceId(Long value) { invoiceId = value; }
        public Long getSourceId() { return sourceId; }
        public void setSourceId(Long value) { sourceId = value; }
        public Long getActorId() { return actorId; }
        public void setActorId(Long value) { actorId = value; }
        public Long getFinanceRecordId() { return financeRecordId; }
        public void setFinanceRecordId(Long value) { financeRecordId = value; }
        public Long getUnitId() { return unitId; }
        public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long value) { ownerId = value; }
        public Long getTenantId() { return tenantId; }
        public void setTenantId(Long value) { tenantId = value; }
        public String getTransactionNo() { return transactionNo; }
        public void setTransactionNo(String value) { transactionNo = value; }
        public LocalDate getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDate value) { transactionDate = value; }
        public String getChargeType() { return chargeType; }
        public void setChargeType(String value) { chargeType = value; }
        public String getDescription() { return description; }
        public void setDescription(String value) { description = value; }
        public String getPayer() { return payer; }
        public void setPayer(String value) { payer = value; }
        public String getSourceType() { return sourceType; }
        public void setSourceType(String value) { sourceType = value; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal value) { amount = value; }
    }
}
