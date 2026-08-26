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

import com.ccps.backend.dto.MaintenanceDetailResponse.Attachment;
import com.ccps.backend.dto.MaintenanceDetailResponse.StatusEvent;
import com.ccps.backend.dto.OwnerExpenseMaintenanceResponse.ExpenseItem;
import com.ccps.backend.dto.OwnerExpenseMaintenanceResponse.MaintenanceItem;
import com.ccps.backend.dto.OwnerExpenseMaintenanceResponse.PropertyOption;

@Mapper
public interface OwnerExpenseMaintenanceMapper {

    @Select("""
            SELECT
              COALESCE(SUM(CASE WHEN ce.direction = 'income' AND fr.record_type <> 'security_deposit'
                                THEN fr.amount ELSE 0 END), 0) AS income_amount,
              COALESCE(SUM(CASE WHEN ce.direction = 'expense' THEN fr.amount ELSE 0 END), 0) AS expense_amount
            FROM cashflow_entries ce
            JOIN finance_records fr ON fr.id = ce.finance_record_id
            JOIN units u ON u.id = ce.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE fr.payment_status <> 'voided'
              AND fr.confirmation_status = 'confirmed'
              AND (#{projectId} IS NULL OR u.project_id = #{projectId})
              AND EXISTS (
                SELECT 1 FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                WHERE ou.unit_id = ce.unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
                  AND o.status = 'active' AND (#{userId} IS NULL OR o.user_id = #{userId})
              )
            """)
    CashflowTotals findCashflowTotals(@Param("userId") Long userId, @Param("projectId") Long projectId);

    @Select("""
            SELECT
              COALESCE(SUM(ce_amount.amount), 0) AS expense_amount,
              COALESCE(SUM(CASE WHEN ce_amount.category = 'maintenance' THEN ce_amount.amount ELSE 0 END), 0) AS maintenance_amount
            FROM (
              SELECT ce.id, ce.category, COALESCE(fr.amount, 0) AS amount
              FROM cashflow_entries ce
              JOIN finance_records fr ON fr.id = ce.finance_record_id
              JOIN units u ON u.id = ce.unit_id
              JOIN projects p ON p.id = u.project_id
              WHERE ce.direction = 'expense'
                AND fr.payment_status <> 'voided'
                AND fr.confirmation_status = 'confirmed'
                AND ce.occurred_on >= #{startDate}
                AND ce.occurred_on < #{endDate}
                AND (#{projectId} IS NULL OR u.project_id = #{projectId})
                AND EXISTS (
                  SELECT 1 FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                  WHERE ou.unit_id = ce.unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
                    AND o.status = 'active' AND (#{userId} IS NULL OR o.user_id = #{userId})
                )
            ) ce_amount
            """)
    ExpenseTotals findTotals(@Param("userId") Long userId, @Param("projectId") Long projectId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COALESCE(SUM(rt.amount), 0) AS amount, COUNT(*) AS entry_count
            FROM reserve_transactions rt
            LEFT JOIN finance_records fr ON fr.id = rt.finance_record_id
            LEFT JOIN cashflow_entries ce ON ce.finance_record_id = fr.id
            LEFT JOIN maintenance_work_orders mwo ON mwo.id = rt.maintenance_work_order_id
            JOIN units u ON u.id = COALESCE(ce.unit_id, mwo.unit_id)
            JOIN projects p ON p.id = u.project_id
            WHERE rt.transaction_type = 'debit'
              AND rt.occurred_at >= #{startDate}
              AND rt.occurred_at < #{endDate}
              AND (#{projectId} IS NULL OR u.project_id = #{projectId})
              AND EXISTS (
                SELECT 1 FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                WHERE ou.unit_id = u.id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
                  AND o.status = 'active' AND (#{userId} IS NULL OR o.user_id = #{userId})
              )
            """)
    ReserveTotals findReserveTotals(@Param("userId") Long userId, @Param("projectId") Long projectId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COUNT(*)
            FROM maintenance_work_orders mwo
            JOIN units u ON u.id = mwo.unit_id
            WHERE mwo.status NOT IN ('completed', 'cancelled')
              AND (#{projectId} IS NULL OR u.project_id = #{projectId})
              AND EXISTS (
                SELECT 1 FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                WHERE ou.unit_id = mwo.unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
                  AND o.status = 'active' AND (#{userId} IS NULL OR o.user_id = #{userId})
              )
            """)
    int countPendingMaintenance(@Param("userId") Long userId, @Param("projectId") Long projectId);

    @Select("""
            SELECT DISTINCT p.id, p.name
            FROM owners o
            JOIN owner_units ou ON ou.owner_id = o.id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE (#{userId} IS NULL OR o.user_id = #{userId}) AND o.status = 'active' AND p.status = 'active'
            ORDER BY p.name
            """)
    List<PropertyOption> findProperties(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT ce.category
            FROM cashflow_entries ce
            JOIN finance_records fr ON fr.id = ce.finance_record_id
            WHERE ce.direction = 'expense'
              AND fr.confirmation_status = 'confirmed'
              AND EXISTS (
                SELECT 1 FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                WHERE ou.unit_id = ce.unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
                  AND o.status = 'active' AND (#{userId} IS NULL OR o.user_id = #{userId})
              )
            UNION
            SELECT DISTINCT mwo.category
            FROM maintenance_work_orders mwo
            WHERE mwo.cashflow_entry_id IS NULL
              AND EXISTS (
                SELECT 1 FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                WHERE ou.unit_id = mwo.unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
                  AND o.status = 'active' AND (#{userId} IS NULL OR o.user_id = #{userId})
              )
            ORDER BY category
            """)
    List<String> findCategories(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT
              ce.id,
              fr.id AS finance_record_id,
              u.id AS unit_id,
              p.id AS project_id,
              p.name AS project_name,
              p.state_name AS state,
              p.city,
              u.unit_no,
              ce.occurred_on,
              ce.category,
              ce.description,
              fr.amount,
              COALESCE((SELECT SUM(rt.amount) FROM reserve_transactions rt
                WHERE rt.transaction_type = 'debit'
                  AND (rt.finance_record_id = fr.id OR rt.maintenance_work_order_id = mwo.id)), 0) AS reserve_deducted_amount,
              fr.payment_status,
              fr.confirmation_status,
              fr.payment_method,
              fr.transaction_date AS payment_date,
              mwo.id AS work_order_id,
              COALESCE(attachment_count.total, 0) AS attachment_count,
              CASE WHEN mwo.id IS NULL AND fr.payment_status &lt;&gt; 'voided'
                     AND fr.sync_status &lt;&gt; 'exported'
                     AND fr.confirmation_status &lt;&gt; 'confirmed' THEN TRUE ELSE FALSE END AS editable,
              COALESCE(pr.payer_name, mwo.payer_name) AS payer_name,
              COALESCE(CASE WHEN INSTR(COALESCE(pr.bank_reference, ''), ' | ') > 0 THEN SUBSTRING_INDEX(pr.bank_reference, ' | ', 1) ELSE NULLIF(pr.bank_reference, '') END, mwo.bank_name) AS bank_name,
              COALESCE(CASE WHEN INSTR(COALESCE(pr.bank_reference, ''), ' | ') > 0 THEN SUBSTRING_INDEX(pr.bank_reference, ' | ', -1) ELSE NULL END, mwo.payment_account_no) AS payment_account_no,
              COALESCE(pr.fee_account_type, mwo.fee_account_type) AS fee_account_key,
              COALESCE(pr.fee_account_no, mwo.fee_account_no) AS fee_account_no
            FROM cashflow_entries ce
            JOIN finance_records fr ON fr.id = ce.finance_record_id
            JOIN units u ON u.id = ce.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN maintenance_work_orders mwo ON mwo.cashflow_entry_id = ce.id
            LEFT JOIN payment_receipts pr ON pr.finance_record_id = fr.id
            LEFT JOIN (
              SELECT dl.entity_id AS work_order_id, COUNT(*) AS total
              FROM document_links dl JOIN documents d ON d.id = dl.document_id
              WHERE dl.entity_type = 'work_order' AND d.document_type = 'maintenance_attachment'
              GROUP BY dl.entity_id
            ) attachment_count ON attachment_count.work_order_id = mwo.id
            WHERE ce.direction = 'expense'
              AND fr.payment_status &lt;&gt; 'voided'
              AND (#{userId} IS NULL OR fr.confirmation_status = 'confirmed')
              AND ce.occurred_on &gt;= #{startDate}
              AND ce.occurred_on &lt; #{endDate}
              AND EXISTS (
                SELECT 1 FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                WHERE ou.unit_id = ce.unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
                  AND o.status = 'active' AND (#{userId} IS NULL OR o.user_id = #{userId})
              )
            <if test="projectId != null">AND p.id = #{projectId}</if>
            <if test="category != null and category != ''">AND ce.category = #{category}</if>
            ORDER BY occurred_on DESC, id DESC
            </script>
            """)
    List<ExpenseItem> findExpenses(@Param("userId") Long userId, @Param("projectId") Long projectId,
            @Param("category") String category, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("""
            <script>
            SELECT
              mwo.id,
              mwo.work_order_no,
              u.id AS unit_id,
              p.id AS project_id,
              p.name AS project_name,
              p.state_name AS state,
              p.city,
              u.unit_no,
              mwo.vendor_id,
              mwo.category,
              mwo.title,
              mwo.description,
              mwo.requested_at,
              mwo.completed_at,
              mwo.status,
              mwo.estimated_amount,
              COALESCE(mwo.actual_amount, mwo.estimated_amount, 0) AS amount,
              mwo.cashflow_entry_id,
              fr.confirmation_status,
              fr.payment_status,
              COALESCE((SELECT SUM(rt.amount) FROM reserve_transactions rt
                WHERE rt.transaction_type = 'debit'
                  AND (rt.maintenance_work_order_id = mwo.id OR rt.finance_record_id = fr.id)), 0) AS reserve_deducted_amount,
              COALESCE(attachment_count.total, 0) AS attachment_count,
              CASE WHEN fr.id IS NULL OR fr.confirmation_status = 'rejected'
                         OR (COALESCE(fr.confirmation_status, 'pending') &lt;&gt; 'confirmed'
                             AND COALESCE(fr.payment_status, 'unpaid') &lt;&gt; 'voided'
                             AND COALESCE(fr.sync_status, 'not_synced') &lt;&gt; 'exported')
                   THEN TRUE ELSE FALSE END AS editable,
              COALESCE(pr.payer_name, mwo.payer_name) AS payer_name,
              COALESCE(CASE WHEN INSTR(COALESCE(pr.bank_reference, ''), ' | ') > 0 THEN SUBSTRING_INDEX(pr.bank_reference, ' | ', 1) ELSE NULLIF(pr.bank_reference, '') END, mwo.bank_name) AS bank_name,
              COALESCE(CASE WHEN INSTR(COALESCE(pr.bank_reference, ''), ' | ') > 0 THEN SUBSTRING_INDEX(pr.bank_reference, ' | ', -1) ELSE NULL END, mwo.payment_account_no) AS payment_account_no,
              COALESCE(pr.fee_account_type, mwo.fee_account_type) AS fee_account_key,
              COALESCE(pr.fee_account_no, mwo.fee_account_no) AS fee_account_no
            FROM maintenance_work_orders mwo
            JOIN units u ON u.id = mwo.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN cashflow_entries ce ON ce.id = mwo.cashflow_entry_id
            LEFT JOIN finance_records fr ON fr.id = ce.finance_record_id
            LEFT JOIN payment_receipts pr ON pr.finance_record_id = fr.id
            LEFT JOIN (
              SELECT dl.entity_id AS work_order_id, COUNT(*) AS total
              FROM document_links dl JOIN documents d ON d.id = dl.document_id
              WHERE dl.entity_type = 'work_order' AND d.document_type = 'maintenance_attachment'
              GROUP BY dl.entity_id
            ) attachment_count ON attachment_count.work_order_id = mwo.id
            WHERE mwo.status &lt;&gt; 'cancelled'
              AND DATE(mwo.requested_at) &gt;= #{startDate}
              AND DATE(mwo.requested_at) &lt; #{endDate}
              AND EXISTS (
                SELECT 1 FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                WHERE ou.unit_id = mwo.unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
                  AND o.status = 'active' AND (#{userId} IS NULL OR o.user_id = #{userId})
              )
            <if test="projectId != null">AND p.id = #{projectId}</if>
            <if test="category != null and category != ''">AND mwo.category = #{category}</if>
            <if test="status != null and status != ''">AND mwo.status = #{status}</if>
            ORDER BY mwo.requested_at DESC, mwo.id DESC
            </script>
            """)
    List<MaintenanceItem> findMaintenance(@Param("userId") Long userId, @Param("projectId") Long projectId,
            @Param("category") String category, @Param("status") String status,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT
              mwo.id, mwo.work_order_no, p.name AS project_name, u.unit_no,
              mwo.category, mwo.title, mwo.description, v.name AS vendor_name,
              mwo.requested_at, mwo.completed_at, mwo.status,
              mwo.estimated_amount, mwo.actual_amount,
              COALESCE((SELECT SUM(rt.amount) FROM reserve_transactions rt
                WHERE rt.transaction_type = 'debit'
                  AND (rt.maintenance_work_order_id = mwo.id OR rt.finance_record_id = fr.id)), 0) AS reserve_deducted_amount,
              fr.payment_status, fr.confirmation_status, fr.payment_method,
              fr.transaction_date AS payment_date,
              COALESCE(pr.payer_name, mwo.payer_name) AS payer_name,
              COALESCE(CASE WHEN INSTR(COALESCE(pr.bank_reference, ''), ' | ') > 0 THEN SUBSTRING_INDEX(pr.bank_reference, ' | ', 1) ELSE NULLIF(pr.bank_reference, '') END, mwo.bank_name) AS bank_name,
              COALESCE(CASE WHEN INSTR(COALESCE(pr.bank_reference, ''), ' | ') > 0 THEN SUBSTRING_INDEX(pr.bank_reference, ' | ', -1) ELSE NULL END, mwo.payment_account_no) AS payment_account_no
            FROM maintenance_work_orders mwo
            JOIN units u ON u.id = mwo.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN vendors v ON v.id = mwo.vendor_id
            LEFT JOIN cashflow_entries ce ON ce.id = mwo.cashflow_entry_id
            LEFT JOIN finance_records fr ON fr.id = ce.finance_record_id
            LEFT JOIN payment_receipts pr ON pr.finance_record_id = fr.id
            WHERE mwo.id = #{workOrderId}
              AND EXISTS (
                SELECT 1 FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                WHERE ou.unit_id = mwo.unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
                  AND o.status = 'active' AND (#{userId} IS NULL OR o.user_id = #{userId})
              )
            """)
    MaintenanceHeader findMaintenanceHeader(@Param("userId") Long userId, @Param("workOrderId") Long workOrderId);

    @Select("""
            SELECT id, status, occurred_at, note
            FROM maintenance_status_history
            WHERE work_order_id = #{workOrderId}
            ORDER BY occurred_at, id
            """)
    List<StatusEvent> findStatusHistory(@Param("workOrderId") Long workOrderId);

    @Select("""
            SELECT d.id AS document_id, d.original_name AS name, d.mime_type,
                   COALESCE(d.file_size, 0) AS size, dl.relation_type, d.created_at AS uploaded_at
            FROM document_links dl
            JOIN documents d ON d.id = dl.document_id
            WHERE dl.entity_type = 'work_order'
              AND dl.entity_id = #{workOrderId}
              AND d.document_type = 'maintenance_attachment'
            ORDER BY d.created_at, d.id
            """)
    List<Attachment> findAttachments(@Param("workOrderId") Long workOrderId);

    @Select("""
            SELECT mwo.id, ou.id AS owner_unit_id, mwo.status, fr.payment_status, fr.confirmation_status
            FROM maintenance_work_orders mwo
            JOIN owner_units ou ON ou.unit_id = mwo.unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            JOIN owners o ON o.id = ou.owner_id
            LEFT JOIN cashflow_entries ce ON ce.id = mwo.cashflow_entry_id
            LEFT JOIN finance_records fr ON fr.id = ce.finance_record_id
            WHERE mwo.id = #{workOrderId} AND o.user_id = #{userId} AND o.status = 'active'
            LIMIT 1
            """)
    WorkOrderAccess findWorkOrderAccess(@Param("userId") Long userId, @Param("workOrderId") Long workOrderId);

    @Select("""
            SELECT mwo.id, ou.id AS owner_unit_id, mwo.status, fr.payment_status, fr.confirmation_status
            FROM maintenance_work_orders mwo
            LEFT JOIN owner_units ou ON ou.unit_id = mwo.unit_id AND ou.status = 'active' AND ou.is_primary = 1
            LEFT JOIN cashflow_entries ce ON ce.id = mwo.cashflow_entry_id
            LEFT JOIN finance_records fr ON fr.id = ce.finance_record_id
            WHERE mwo.id = #{workOrderId}
            LIMIT 1
            """)
    WorkOrderAccess findAdminWorkOrderAccess(@Param("workOrderId") Long workOrderId);

    @Insert("""
            INSERT INTO documents
              (document_no, original_name, storage_key, mime_type, file_size, checksum_sha256,
               document_type, status, uploaded_by)
            VALUES
              (#{documentNo}, #{originalName}, #{storageKey}, #{mimeType}, #{fileSize}, #{checksumSha256},
               'maintenance_attachment', 'active', #{uploadedBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAttachment(NewAttachment attachment);

    @Insert("""
            INSERT INTO document_links (document_id, entity_type, entity_id, relation_type)
            VALUES (#{documentId}, 'work_order', #{workOrderId}, #{relationType})
            """)
    int insertAttachmentLink(@Param("documentId") Long documentId, @Param("workOrderId") Long workOrderId,
            @Param("relationType") String relationType);

    @Select("""
            SELECT d.id AS document_id, d.original_name, d.storage_key, d.mime_type,
                   COALESCE(d.file_size, 0) AS file_size
            FROM documents d
            JOIN document_links dl ON dl.document_id = d.id
            JOIN maintenance_work_orders mwo ON mwo.id = dl.entity_id AND dl.entity_type = 'work_order'
            WHERE d.id = #{documentId} AND d.document_type = 'maintenance_attachment'
              AND EXISTS (
                SELECT 1 FROM owner_units ou JOIN owners o ON o.id = ou.owner_id
                WHERE ou.unit_id = mwo.unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
                  AND o.status = 'active' AND o.user_id = #{userId}
              )
            """)
    AttachmentFile findAttachmentFile(@Param("userId") Long userId, @Param("documentId") Long documentId);

    @Select("""
            SELECT d.id AS document_id, d.original_name, d.storage_key, d.mime_type,
                   COALESCE(d.file_size, 0) AS file_size
            FROM documents d
            JOIN document_links dl ON dl.document_id = d.id
            JOIN maintenance_work_orders mwo ON mwo.id = dl.entity_id AND dl.entity_type = 'work_order'
            WHERE d.id = #{documentId} AND d.document_type = 'maintenance_attachment'
              AND d.status = 'active'
            LIMIT 1
            """)
    AttachmentFile findAdminAttachmentFile(@Param("documentId") Long documentId);

    class ExpenseTotals {
        private BigDecimal expenseAmount;
        private BigDecimal maintenanceAmount;
        public BigDecimal getExpenseAmount() { return expenseAmount; }
        public void setExpenseAmount(BigDecimal expenseAmount) { this.expenseAmount = expenseAmount; }
        public BigDecimal getMaintenanceAmount() { return maintenanceAmount; }
        public void setMaintenanceAmount(BigDecimal maintenanceAmount) { this.maintenanceAmount = maintenanceAmount; }
    }

    class CashflowTotals {
        private BigDecimal incomeAmount;
        private BigDecimal expenseAmount;
        public BigDecimal getIncomeAmount() { return incomeAmount; }
        public void setIncomeAmount(BigDecimal incomeAmount) { this.incomeAmount = incomeAmount; }
        public BigDecimal getExpenseAmount() { return expenseAmount; }
        public void setExpenseAmount(BigDecimal expenseAmount) { this.expenseAmount = expenseAmount; }
    }

    class ReserveTotals {
        private BigDecimal amount;
        private Integer entryCount;
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public Integer getEntryCount() { return entryCount; }
        public void setEntryCount(Integer entryCount) { this.entryCount = entryCount; }
    }

    class MaintenanceHeader {
        private Long id;
        private String workOrderNo;
        private String projectName;
        private String unitNo;
        private String category;
        private String title;
        private String description;
        private String vendorName;
        private LocalDateTime requestedAt;
        private LocalDateTime completedAt;
        private String status;
        private BigDecimal estimatedAmount;
        private BigDecimal actualAmount;
        private BigDecimal reserveDeductedAmount;
        private String paymentStatus;
        private String confirmationStatus;
        private String paymentMethod;
        private LocalDate paymentDate;
        private String payerName;
        private String bankName;
        private String paymentAccountNo;
        public Long getId() { return id; } public void setId(Long id) { this.id = id; }
        public String getWorkOrderNo() { return workOrderNo; } public void setWorkOrderNo(String value) { this.workOrderNo = value; }
        public String getProjectName() { return projectName; } public void setProjectName(String value) { this.projectName = value; }
        public String getUnitNo() { return unitNo; } public void setUnitNo(String value) { this.unitNo = value; }
        public String getCategory() { return category; } public void setCategory(String value) { this.category = value; }
        public String getTitle() { return title; } public void setTitle(String value) { this.title = value; }
        public String getDescription() { return description; } public void setDescription(String value) { this.description = value; }
        public String getVendorName() { return vendorName; } public void setVendorName(String value) { this.vendorName = value; }
        public LocalDateTime getRequestedAt() { return requestedAt; } public void setRequestedAt(LocalDateTime value) { this.requestedAt = value; }
        public LocalDateTime getCompletedAt() { return completedAt; } public void setCompletedAt(LocalDateTime value) { this.completedAt = value; }
        public String getStatus() { return status; } public void setStatus(String value) { this.status = value; }
        public BigDecimal getEstimatedAmount() { return estimatedAmount; } public void setEstimatedAmount(BigDecimal value) { this.estimatedAmount = value; }
        public BigDecimal getActualAmount() { return actualAmount; } public void setActualAmount(BigDecimal value) { this.actualAmount = value; }
        public BigDecimal getReserveDeductedAmount() { return reserveDeductedAmount; } public void setReserveDeductedAmount(BigDecimal value) { this.reserveDeductedAmount = value; }
        public String getPaymentStatus() { return paymentStatus; } public void setPaymentStatus(String value) { this.paymentStatus = value; }
        public String getConfirmationStatus() { return confirmationStatus; } public void setConfirmationStatus(String value) { this.confirmationStatus = value; }
        public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String value) { this.paymentMethod = value; }
        public LocalDate getPaymentDate() { return paymentDate; } public void setPaymentDate(LocalDate value) { this.paymentDate = value; }
        public String getPayerName() { return payerName; } public void setPayerName(String value) { this.payerName = value; }
        public String getBankName() { return bankName; } public void setBankName(String value) { this.bankName = value; }
        public String getPaymentAccountNo() { return paymentAccountNo; } public void setPaymentAccountNo(String value) { this.paymentAccountNo = value; }
    }

    class WorkOrderAccess {
        private Long id;
        private Long ownerUnitId;
        private String status;
        private String paymentStatus;
        private String confirmationStatus;
        public Long getId() { return id; } public void setId(Long id) { this.id = id; }
        public Long getOwnerUnitId() { return ownerUnitId; } public void setOwnerUnitId(Long value) { this.ownerUnitId = value; }
        public String getStatus() { return status; } public void setStatus(String value) { this.status = value; }
        public String getPaymentStatus() { return paymentStatus; } public void setPaymentStatus(String value) { this.paymentStatus = value; }
        public String getConfirmationStatus() { return confirmationStatus; } public void setConfirmationStatus(String value) { this.confirmationStatus = value; }
    }

    class NewAttachment {
        private Long id;
        private String documentNo;
        private String originalName;
        private String storageKey;
        private String mimeType;
        private Long fileSize;
        private String checksumSha256;
        private Long uploadedBy;
        public Long getId() { return id; } public void setId(Long id) { this.id = id; }
        public String getDocumentNo() { return documentNo; } public void setDocumentNo(String value) { this.documentNo = value; }
        public String getOriginalName() { return originalName; } public void setOriginalName(String value) { this.originalName = value; }
        public String getStorageKey() { return storageKey; } public void setStorageKey(String value) { this.storageKey = value; }
        public String getMimeType() { return mimeType; } public void setMimeType(String value) { this.mimeType = value; }
        public Long getFileSize() { return fileSize; } public void setFileSize(Long value) { this.fileSize = value; }
        public String getChecksumSha256() { return checksumSha256; } public void setChecksumSha256(String value) { this.checksumSha256 = value; }
        public Long getUploadedBy() { return uploadedBy; } public void setUploadedBy(Long value) { this.uploadedBy = value; }
    }

    class AttachmentFile {
        private Long documentId;
        private String originalName;
        private String storageKey;
        private String mimeType;
        private Long fileSize;
        public Long getDocumentId() { return documentId; } public void setDocumentId(Long value) { this.documentId = value; }
        public String getOriginalName() { return originalName; } public void setOriginalName(String value) { this.originalName = value; }
        public String getStorageKey() { return storageKey; } public void setStorageKey(String value) { this.storageKey = value; }
        public String getMimeType() { return mimeType; } public void setMimeType(String value) { this.mimeType = value; }
        public Long getFileSize() { return fileSize; } public void setFileSize(Long value) { this.fileSize = value; }
    }
}
