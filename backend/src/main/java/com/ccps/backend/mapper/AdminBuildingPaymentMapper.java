package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.ccps.backend.dto.AdminPaymentContractOption;

@Mapper
public interface AdminBuildingPaymentMapper {

    @Select("SELECT COUNT(*) FROM projects WHERE project_code = #{projectCode}")
    int countProjectCode(@Param("projectCode") String projectCode);

    @Insert("""
            INSERT INTO projects (project_code, name, address, city, country_code, status)
            VALUES (#{projectCode}, #{name}, #{address}, #{city}, #{countryCode}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertProject(NewProject project);

    @Select("""
            SELECT pc.id AS contract_id, ou.id AS owner_unit_id, p.id AS project_id,
                   p.name AS project_name, u.unit_no, o.full_name AS owner_name,
                   pc.contract_no, pc.purchase_price, pc.currency,
                   EXISTS (SELECT 1 FROM payment_plans active_pp
                           WHERE active_pp.purchase_contract_id = pc.id AND active_pp.status = 'active') AS has_active_plan
            FROM purchase_contracts pc
            JOIN owner_units ou ON ou.id = pc.owner_unit_id
                AND ou.status = 'active' AND ou.asset_stage = 'PRE_HANDOVER'
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id AND p.status = 'active'
            WHERE pc.status = 'active'
            ORDER BY p.name, u.unit_no, o.full_name
            """)
    List<AdminPaymentContractOption> findPaymentContracts();

    @Select("""
            SELECT pc.id AS contract_id, ou.id AS owner_unit_id, p.id AS project_id,
                   p.name AS project_name, u.unit_no, o.full_name AS owner_name,
                   pc.contract_no, pc.purchase_price, pc.currency,
                   EXISTS (SELECT 1 FROM payment_plans active_pp
                           WHERE active_pp.purchase_contract_id = pc.id AND active_pp.status = 'active') AS has_active_plan
            FROM purchase_contracts pc
            JOIN owner_units ou ON ou.id = pc.owner_unit_id
                AND ou.status = 'active' AND ou.asset_stage = 'PRE_HANDOVER'
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id AND p.status = 'active'
            WHERE pc.id = #{contractId} AND pc.status = 'active'
            """)
    AdminPaymentContractOption findPaymentContract(@Param("contractId") Long contractId);

    @Select("SELECT id FROM purchase_contracts WHERE id = #{contractId} AND status = 'active' FOR UPDATE")
    Long lockPurchaseContract(@Param("contractId") Long contractId);

    @Select("SELECT COUNT(*) FROM payment_plans WHERE purchase_contract_id = #{contractId} AND status = 'active'")
    int countActivePaymentPlans(@Param("contractId") Long contractId);

    @Insert("""
            INSERT INTO payment_plans
                (purchase_contract_id, plan_name, installment_count, total_amount, start_date, status)
            VALUES
                (#{purchaseContractId}, #{planName}, #{installmentCount}, #{totalAmount}, #{startDate}, 'active')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertPaymentPlan(NewPaymentPlan plan);

    @Insert("""
            INSERT INTO payment_installments
                (payment_plan_id, installment_no, milestone, due_date, amount_due, amount_paid, status)
            VALUES
                (#{paymentPlanId}, #{installmentNo}, #{milestone}, #{dueDate}, #{amountDue}, 0, 'pending')
            """)
    int insertPaymentInstallment(NewPaymentInstallment installment);

    @Select("""
            SELECT
              pi.id,
              pp.id AS payment_plan_id,
              pc.id AS contract_id,
              p.name AS project_name,
              u.unit_no,
              o.full_name AS owner_name,
              pc.contract_no,
              pp.plan_name,
              pi.installment_no,
              pi.milestone,
              pi.due_date,
              pi.amount_due,
              pi.amount_paid,
              GREATEST(pi.amount_due - pi.amount_paid, 0) AS unpaid_amount,
              MAX(fr.transaction_date) AS payment_date,
              MAX(fr.id) AS finance_record_id,
              MAX(fr.confirmation_status) AS confirmation_status,
              MAX(fr.payment_method) AS payment_method,
              MAX(fr.amount) AS submitted_amount,
              CASE
                WHEN pi.amount_paid >= pi.amount_due THEN 'paid'
                WHEN pi.amount_paid > 0 THEN 'partial'
                WHEN pi.due_date < CURRENT_DATE THEN 'overdue'
                ELSE 'pending'
              END AS status,
              MAX(pr.receipt_no) AS receipt_no,
              MAX(pr.bank_reference) AS bank_reference,
              MAX(pr.submission_note) AS submission_note,
              MAX(pr.proof_document_id) AS proof_document_id,
              pc.purchase_price
            FROM payment_installments pi
            JOIN payment_plans pp ON pp.id = pi.payment_plan_id AND pp.status = 'active'
            JOIN purchase_contracts pc ON pc.id = pp.purchase_contract_id AND pc.status = 'active'
            JOIN owner_units ou ON ou.id = pc.owner_unit_id
                AND ou.status = 'active' AND ou.asset_stage = 'PRE_HANDOVER'
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id AND p.status = 'active'
            LEFT JOIN payment_receipt_allocations pra ON pra.installment_id = pi.id
            LEFT JOIN payment_receipts pr ON pr.id = pra.receipt_id
            LEFT JOIN finance_records fr ON fr.id = pr.finance_record_id
            GROUP BY pi.id, pp.id, pc.id, p.name, u.unit_no, o.full_name,
                     pc.contract_no, pp.plan_name, pi.installment_no, pi.milestone,
                     pi.due_date, pi.amount_due, pi.amount_paid, pc.purchase_price
            ORDER BY pi.due_date, pi.installment_no, pi.id
            """)
    List<InstallmentRow> findRows();

    @Select({
            "<script>",
            "SELECT pi.id, pp.id AS payment_plan_id, pc.id AS contract_id, p.name AS project_name, u.unit_no,",
            "       o.full_name AS owner_name, pc.contract_no, pp.plan_name, pi.installment_no, pi.milestone,",
            "       pi.due_date, pi.amount_due, pi.amount_paid, GREATEST(pi.amount_due - pi.amount_paid, 0) AS unpaid_amount,",
            "       MAX(fr.transaction_date) AS payment_date,",
            "       MAX(fr.id) AS finance_record_id, MAX(fr.confirmation_status) AS confirmation_status,",
            "       MAX(fr.payment_method) AS payment_method, MAX(pr.bank_reference) AS bank_reference,",
            "       MAX(fr.amount) AS submitted_amount,",
            "       MAX(pr.submission_note) AS submission_note, MAX(pr.proof_document_id) AS proof_document_id,",
            "       CASE WHEN pi.amount_paid >= pi.amount_due THEN 'paid' WHEN pi.amount_paid > 0 THEN 'partial'",
            "            WHEN pi.due_date &lt; CURRENT_DATE THEN 'overdue' ELSE 'pending' END AS status,",
            "       MAX(pr.receipt_no) AS receipt_no, pc.purchase_price",
            "FROM payment_installments pi",
            "JOIN payment_plans pp ON pp.id = pi.payment_plan_id AND pp.status = 'active'",
            "JOIN purchase_contracts pc ON pc.id = pp.purchase_contract_id AND pc.status = 'active'",
            "JOIN owner_units ou ON ou.id = pc.owner_unit_id AND ou.status = 'active' AND ou.asset_stage = 'PRE_HANDOVER'",
            "JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'",
            "JOIN units u ON u.id = ou.unit_id",
            "JOIN projects p ON p.id = u.project_id AND p.status = 'active'",
            "LEFT JOIN payment_receipt_allocations pra ON pra.installment_id = pi.id",
            "LEFT JOIN payment_receipts pr ON pr.id = pra.receipt_id",
            "LEFT JOIN finance_records fr ON fr.id = pr.finance_record_id",
            "<where>",
            "  <if test=\"keyword != null and keyword != ''\">",
            "    CONCAT_WS(' ', p.name, u.unit_no, o.full_name, pi.milestone, pr.receipt_no) LIKE CONCAT('%', #{keyword}, '%')",
            "  </if>",
            "  <if test=\"projectName != null and projectName != ''\">AND p.name = #{projectName}</if>",
            "  <if test=\"status != null and status != ''\">",
            "    AND ((#{status} = 'paid' AND pi.amount_paid >= pi.amount_due)",
            "      OR (#{status} = 'partial' AND pi.amount_paid > 0 AND pi.amount_paid &lt; pi.amount_due)",
            "      OR (#{status} = 'overdue' AND pi.amount_paid &lt; pi.amount_due AND pi.due_date &lt; CURRENT_DATE)",
            "      OR (#{status} = 'pending' AND pi.amount_paid &lt; pi.amount_due AND pi.due_date &gt;= CURRENT_DATE))",
            "  </if>",
            "</where>",
            "GROUP BY pi.id, pp.id, pc.id, p.name, u.unit_no, o.full_name, pc.contract_no, pp.plan_name,",
            "         pi.installment_no, pi.milestone, pi.due_date, pi.amount_due, pi.amount_paid, pc.purchase_price",
            "ORDER BY pi.due_date, pi.installment_no, pi.id",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<InstallmentRow> findPageRows(@Param("keyword") String keyword,
                                      @Param("projectName") String projectName,
                                      @Param("status") String status,
                                      @Param("limit") int limit,
                                      @Param("offset") int offset);

    @Select({
            "<script>",
            "SELECT COUNT(DISTINCT pi.id)",
            "FROM payment_installments pi",
            "JOIN payment_plans pp ON pp.id = pi.payment_plan_id AND pp.status = 'active'",
            "JOIN purchase_contracts pc ON pc.id = pp.purchase_contract_id AND pc.status = 'active'",
            "JOIN owner_units ou ON ou.id = pc.owner_unit_id AND ou.status = 'active' AND ou.asset_stage = 'PRE_HANDOVER'",
            "JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'",
            "JOIN units u ON u.id = ou.unit_id",
            "JOIN projects p ON p.id = u.project_id AND p.status = 'active'",
            "LEFT JOIN payment_receipt_allocations pra ON pra.installment_id = pi.id",
            "LEFT JOIN payment_receipts pr ON pr.id = pra.receipt_id",
            "<where>",
            "  <if test=\"keyword != null and keyword != ''\">",
            "    CONCAT_WS(' ', p.name, u.unit_no, o.full_name, pi.milestone, pr.receipt_no) LIKE CONCAT('%', #{keyword}, '%')",
            "  </if>",
            "  <if test=\"projectName != null and projectName != ''\">AND p.name = #{projectName}</if>",
            "  <if test=\"status != null and status != ''\">",
            "    AND ((#{status} = 'paid' AND pi.amount_paid >= pi.amount_due)",
            "      OR (#{status} = 'partial' AND pi.amount_paid > 0 AND pi.amount_paid &lt; pi.amount_due)",
            "      OR (#{status} = 'overdue' AND pi.amount_paid &lt; pi.amount_due AND pi.due_date &lt; CURRENT_DATE)",
            "      OR (#{status} = 'pending' AND pi.amount_paid &lt; pi.amount_due AND pi.due_date &gt;= CURRENT_DATE))",
            "  </if>",
            "</where>",
            "</script>"
    })
    Long countPageRows(@Param("keyword") String keyword,
                       @Param("projectName") String projectName,
                       @Param("status") String status);

    @Select("""
            SELECT pi.id, pi.amount_due, pi.amount_paid,
                   EXISTS (SELECT 1 FROM payment_receipt_allocations pra
                           JOIN payment_receipts pr ON pr.id = pra.receipt_id
                           JOIN finance_records fr ON fr.id = pr.finance_record_id
                           WHERE pra.installment_id = pi.id AND fr.confirmation_status = 'pending') AS has_pending_proof
            FROM payment_installments pi
            JOIN payment_plans pp ON pp.id = pi.payment_plan_id AND pp.status = 'active'
            JOIN purchase_contracts pc ON pc.id = pp.purchase_contract_id AND pc.status = 'active'
            WHERE pi.id = #{installmentId}
            FOR UPDATE
            """)
    InstallmentActionContext lockInstallment(@Param("installmentId") Long installmentId);

    @Update("""
            UPDATE payment_installments
            SET milestone = #{milestone}, due_date = #{dueDate}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{installmentId}
            """)
    int updateInstallment(@Param("installmentId") Long installmentId,
                          @Param("milestone") String milestone,
                          @Param("dueDate") LocalDate dueDate);

    @Select("""
            SELECT pi.id, pi.due_date, pi.amount_due, pi.amount_paid, pi.milestone,
                   p.name AS project_name, u.unit_no, o.id AS owner_id, o.user_id
            FROM payment_installments pi
            JOIN payment_plans pp ON pp.id = pi.payment_plan_id AND pp.status = 'active'
            JOIN purchase_contracts pc ON pc.id = pp.purchase_contract_id AND pc.status = 'active'
            JOIN owner_units ou ON ou.id = pc.owner_unit_id AND ou.status = 'active'
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id AND p.status = 'active'
            WHERE pi.id = #{installmentId}
            """)
    ReminderContext findReminderContext(@Param("installmentId") Long installmentId);

    @Select("""
            SELECT COUNT(*) FROM notifications
            WHERE related_type = 'payment_installment' AND related_id = #{installmentId}
              AND created_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 HOUR)
            """)
    int countRecentReminders(@Param("installmentId") Long installmentId);

    @Insert("""
            INSERT INTO notifications
                (recipient_user_id, recipient_owner_id, title, body, related_type, related_id, priority, status)
            VALUES
                (#{recipientUserId}, #{recipientOwnerId}, #{title}, #{body}, 'payment_installment',
                 #{installmentId}, #{priority}, 'unread')
            """)
    int insertPaymentReminder(@Param("recipientUserId") Long recipientUserId,
                              @Param("recipientOwnerId") Long recipientOwnerId,
                              @Param("installmentId") Long installmentId,
                              @Param("title") String title,
                              @Param("body") String body,
                              @Param("priority") String priority);

    class InstallmentActionContext {
        private Long id;
        private BigDecimal amountDue;
        private BigDecimal amountPaid;
        private boolean hasPendingProof;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public BigDecimal getAmountDue() { return amountDue; }
        public void setAmountDue(BigDecimal value) { amountDue = value; }
        public BigDecimal getAmountPaid() { return amountPaid; }
        public void setAmountPaid(BigDecimal value) { amountPaid = value; }
        public boolean isHasPendingProof() { return hasPendingProof; }
        public void setHasPendingProof(boolean value) { hasPendingProof = value; }
    }

    class ReminderContext {
        private Long id;
        private LocalDate dueDate;
        private BigDecimal amountDue;
        private BigDecimal amountPaid;
        private String milestone;
        private String projectName;
        private String unitNo;
        private Long ownerId;
        private Long userId;
        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate value) { dueDate = value; }
        public BigDecimal getAmountDue() { return amountDue; }
        public void setAmountDue(BigDecimal value) { amountDue = value; }
        public BigDecimal getAmountPaid() { return amountPaid; }
        public void setAmountPaid(BigDecimal value) { amountPaid = value; }
        public String getMilestone() { return milestone; }
        public void setMilestone(String value) { milestone = value; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String value) { projectName = value; }
        public String getUnitNo() { return unitNo; }
        public void setUnitNo(String value) { unitNo = value; }
        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long value) { ownerId = value; }
        public Long getUserId() { return userId; }
        public void setUserId(Long value) { userId = value; }
    }

    class NewProject {
        private Long id;
        private String projectCode;
        private String name;
        private String address;
        private String city;
        private String countryCode;
        private String status;

        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public String getProjectCode() { return projectCode; }
        public void setProjectCode(String value) { projectCode = value; }
        public String getName() { return name; }
        public void setName(String value) { name = value; }
        public String getAddress() { return address; }
        public void setAddress(String value) { address = value; }
        public String getCity() { return city; }
        public void setCity(String value) { city = value; }
        public String getCountryCode() { return countryCode; }
        public void setCountryCode(String value) { countryCode = value; }
        public String getStatus() { return status; }
        public void setStatus(String value) { status = value; }
    }

    class NewPaymentPlan {
        private Long id;
        private Long purchaseContractId;
        private String planName;
        private int installmentCount;
        private BigDecimal totalAmount;
        private LocalDate startDate;

        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public Long getPurchaseContractId() { return purchaseContractId; }
        public void setPurchaseContractId(Long value) { purchaseContractId = value; }
        public String getPlanName() { return planName; }
        public void setPlanName(String value) { planName = value; }
        public int getInstallmentCount() { return installmentCount; }
        public void setInstallmentCount(int value) { installmentCount = value; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal value) { totalAmount = value; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate value) { startDate = value; }
    }

    class NewPaymentInstallment {
        private Long paymentPlanId;
        private int installmentNo;
        private String milestone;
        private LocalDate dueDate;
        private BigDecimal amountDue;

        public Long getPaymentPlanId() { return paymentPlanId; }
        public void setPaymentPlanId(Long value) { paymentPlanId = value; }
        public int getInstallmentNo() { return installmentNo; }
        public void setInstallmentNo(int value) { installmentNo = value; }
        public String getMilestone() { return milestone; }
        public void setMilestone(String value) { milestone = value; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate value) { dueDate = value; }
        public BigDecimal getAmountDue() { return amountDue; }
        public void setAmountDue(BigDecimal value) { amountDue = value; }
    }

    class InstallmentRow {
        private Long id;
        private Long paymentPlanId;
        private Long contractId;
        private String projectName;
        private String unitNo;
        private String ownerName;
        private String contractNo;
        private String planName;
        private Integer installmentNo;
        private String milestone;
        private LocalDate dueDate;
        private BigDecimal amountDue;
        private BigDecimal amountPaid;
        private BigDecimal unpaidAmount;
        private LocalDate paymentDate;
        private String status;
        private String receiptNo;
        private BigDecimal purchasePrice;
        private Long financeRecordId;
        private String confirmationStatus;
        private String paymentMethod;
        private String bankReference;
        private String submissionNote;
        private Long proofDocumentId;
        private BigDecimal submittedAmount;

        public Long getId() { return id; }
        public void setId(Long value) { id = value; }
        public Long getPaymentPlanId() { return paymentPlanId; }
        public void setPaymentPlanId(Long value) { paymentPlanId = value; }
        public Long getContractId() { return contractId; }
        public void setContractId(Long value) { contractId = value; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String value) { projectName = value; }
        public String getUnitNo() { return unitNo; }
        public void setUnitNo(String value) { unitNo = value; }
        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String value) { ownerName = value; }
        public String getContractNo() { return contractNo; }
        public void setContractNo(String value) { contractNo = value; }
        public String getPlanName() { return planName; }
        public void setPlanName(String value) { planName = value; }
        public Integer getInstallmentNo() { return installmentNo; }
        public void setInstallmentNo(Integer value) { installmentNo = value; }
        public String getMilestone() { return milestone; }
        public void setMilestone(String value) { milestone = value; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate value) { dueDate = value; }
        public BigDecimal getAmountDue() { return amountDue; }
        public void setAmountDue(BigDecimal value) { amountDue = value; }
        public BigDecimal getAmountPaid() { return amountPaid; }
        public void setAmountPaid(BigDecimal value) { amountPaid = value; }
        public BigDecimal getUnpaidAmount() { return unpaidAmount; }
        public void setUnpaidAmount(BigDecimal value) { unpaidAmount = value; }
        public LocalDate getPaymentDate() { return paymentDate; }
        public void setPaymentDate(LocalDate value) { paymentDate = value; }
        public String getStatus() { return status; }
        public void setStatus(String value) { status = value; }
        public String getReceiptNo() { return receiptNo; }
        public void setReceiptNo(String value) { receiptNo = value; }
        public BigDecimal getPurchasePrice() { return purchasePrice; }
        public void setPurchasePrice(BigDecimal value) { purchasePrice = value; }
        public Long getFinanceRecordId() { return financeRecordId; }
        public void setFinanceRecordId(Long value) { financeRecordId = value; }
        public String getConfirmationStatus() { return confirmationStatus; }
        public void setConfirmationStatus(String value) { confirmationStatus = value; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String value) { paymentMethod = value; }
        public String getBankReference() { return bankReference; }
        public void setBankReference(String value) { bankReference = value; }
        public String getSubmissionNote() { return submissionNote; }
        public void setSubmissionNote(String value) { submissionNote = value; }
        public Long getProofDocumentId() { return proofDocumentId; }
        public void setProofDocumentId(Long value) { proofDocumentId = value; }
        public BigDecimal getSubmittedAmount() { return submittedAmount; }
        public void setSubmittedAmount(BigDecimal value) { submittedAmount = value; }
    }
}
