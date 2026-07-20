package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentProgressMapper {

    @Select("""
            SELECT
              ou.id AS owner_unit_id,
              p.name AS project_name,
              u.unit_no,
              o.full_name AS owner_name,
              o.phone,
              pc.contract_no,
              pc.purchase_price,
              pc.currency,
              pc.signed_date,
              pp.id AS payment_plan_id,
              pp.plan_name
            FROM owners o
            JOIN owner_units ou ON ou.owner_id = o.id AND ou.status = 'active' AND ou.asset_stage = 'PRE_HANDOVER'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id AND p.status = 'active'
            LEFT JOIN purchase_contracts pc ON pc.owner_unit_id = ou.id AND pc.status = 'active'
            LEFT JOIN payment_plans pp ON pp.purchase_contract_id = pc.id AND pp.status = 'active'
            WHERE o.user_id = #{userId}
              AND o.status = 'active'
              AND ou.id = #{ownerUnitId}
            ORDER BY pc.id DESC, pp.id DESC
            LIMIT 1
            """)
    Header findHeader(@Param("userId") Long userId, @Param("ownerUnitId") Long ownerUnitId);

    @Select("""
            SELECT
              pi.id,
              pi.installment_no,
              pi.milestone,
              pi.due_date,
              pi.amount_due,
              pi.amount_paid,
              GREATEST(pi.amount_due - pi.amount_paid, 0) AS unpaid_amount,
              pi.status,
              MAX(CASE WHEN fr.confirmation_status = 'confirmed' THEN fr.transaction_date END) AS payment_date,
              (
                SELECT fr_latest.confirmation_status
                FROM payment_receipt_allocations pra_latest
                JOIN payment_receipts pr_latest ON pr_latest.id = pra_latest.receipt_id
                JOIN finance_records fr_latest ON fr_latest.id = pr_latest.finance_record_id
                WHERE pra_latest.installment_id = pi.id
                ORDER BY fr_latest.created_at DESC, fr_latest.id DESC
                LIMIT 1
              ) AS confirmation_status,
              (
                SELECT CASE WHEN fr_latest.confirmation_status = 'rejected' THEN pr_latest.review_note END
                FROM payment_receipt_allocations pra_latest
                JOIN payment_receipts pr_latest ON pr_latest.id = pra_latest.receipt_id
                JOIN finance_records fr_latest ON fr_latest.id = pr_latest.finance_record_id
                WHERE pra_latest.installment_id = pi.id
                ORDER BY fr_latest.created_at DESC, fr_latest.id DESC
                LIMIT 1
              ) AS rejection_reason,
              COUNT(DISTINCT pr.id) AS receipt_count,
              MAX(CASE WHEN pr.proof_document_id IS NOT NULL THEN 1 ELSE 0 END) AS has_proof
            FROM payment_installments pi
            LEFT JOIN payment_receipt_allocations pra ON pra.installment_id = pi.id
            LEFT JOIN payment_receipts pr ON pr.id = pra.receipt_id
            LEFT JOIN finance_records fr ON fr.id = pr.finance_record_id
            WHERE pi.payment_plan_id = #{paymentPlanId}
            GROUP BY pi.id, pi.installment_no, pi.milestone, pi.due_date,
                     pi.amount_due, pi.amount_paid, pi.status
            ORDER BY pi.installment_no
            """)
    List<InstallmentRow> findInstallments(@Param("paymentPlanId") Long paymentPlanId);

    @Select("""
            SELECT
              pr.id AS receipt_id,
              pr.receipt_no,
              fr.transaction_date AS payment_date,
              fr.payment_method,
              fr.confirmation_status,
              pr.review_note,
              pr.proof_document_id
            FROM payment_installments pi
            JOIN payment_receipt_allocations pra ON pra.installment_id = pi.id
            JOIN payment_receipts pr ON pr.id = pra.receipt_id
            JOIN finance_records fr ON fr.id = pr.finance_record_id
            WHERE pi.payment_plan_id = #{paymentPlanId}
            ORDER BY fr.transaction_date DESC, fr.id DESC
            LIMIT 1
            """)
    LatestPaymentRow findLatestPayment(@Param("paymentPlanId") Long paymentPlanId);

    class Header {
        private Long ownerUnitId;
        private String projectName;
        private String unitNo;
        private String ownerName;
        private String phone;
        private String contractNo;
        private BigDecimal purchasePrice;
        private String currency;
        private LocalDate signedDate;
        private Long paymentPlanId;
        private String planName;

        public Long getOwnerUnitId() { return ownerUnitId; }
        public void setOwnerUnitId(Long ownerUnitId) { this.ownerUnitId = ownerUnitId; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getUnitNo() { return unitNo; }
        public void setUnitNo(String unitNo) { this.unitNo = unitNo; }
        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getContractNo() { return contractNo; }
        public void setContractNo(String contractNo) { this.contractNo = contractNo; }
        public BigDecimal getPurchasePrice() { return purchasePrice; }
        public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public LocalDate getSignedDate() { return signedDate; }
        public void setSignedDate(LocalDate signedDate) { this.signedDate = signedDate; }
        public Long getPaymentPlanId() { return paymentPlanId; }
        public void setPaymentPlanId(Long paymentPlanId) { this.paymentPlanId = paymentPlanId; }
        public String getPlanName() { return planName; }
        public void setPlanName(String planName) { this.planName = planName; }
    }

    class InstallmentRow {
        private Long id;
        private Integer installmentNo;
        private String milestone;
        private LocalDate dueDate;
        private BigDecimal amountDue;
        private BigDecimal amountPaid;
        private BigDecimal unpaidAmount;
        private String status;
        private LocalDate paymentDate;
        private String confirmationStatus;
        private String rejectionReason;
        private Integer receiptCount;
        private Boolean hasProof;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getInstallmentNo() { return installmentNo; }
        public void setInstallmentNo(Integer installmentNo) { this.installmentNo = installmentNo; }
        public String getMilestone() { return milestone; }
        public void setMilestone(String milestone) { this.milestone = milestone; }
        public LocalDate getDueDate() { return dueDate; }
        public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
        public BigDecimal getAmountDue() { return amountDue; }
        public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }
        public BigDecimal getAmountPaid() { return amountPaid; }
        public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
        public BigDecimal getUnpaidAmount() { return unpaidAmount; }
        public void setUnpaidAmount(BigDecimal unpaidAmount) { this.unpaidAmount = unpaidAmount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDate getPaymentDate() { return paymentDate; }
        public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
        public String getConfirmationStatus() { return confirmationStatus; }
        public void setConfirmationStatus(String confirmationStatus) { this.confirmationStatus = confirmationStatus; }
        public String getRejectionReason() { return rejectionReason; }
        public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
        public Integer getReceiptCount() { return receiptCount; }
        public void setReceiptCount(Integer receiptCount) { this.receiptCount = receiptCount; }
        public Boolean getHasProof() { return hasProof; }
        public void setHasProof(Boolean hasProof) { this.hasProof = hasProof; }
    }

    class LatestPaymentRow {
        private Long receiptId;
        private String receiptNo;
        private LocalDate paymentDate;
        private String paymentMethod;
        private String confirmationStatus;
        private String reviewNote;
        private Long proofDocumentId;

        public Long getReceiptId() { return receiptId; }
        public void setReceiptId(Long receiptId) { this.receiptId = receiptId; }
        public String getReceiptNo() { return receiptNo; }
        public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }
        public LocalDate getPaymentDate() { return paymentDate; }
        public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getConfirmationStatus() { return confirmationStatus; }
        public void setConfirmationStatus(String confirmationStatus) { this.confirmationStatus = confirmationStatus; }
        public String getReviewNote() { return reviewNote; }
        public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
        public Long getProofDocumentId() { return proofDocumentId; }
        public void setProofDocumentId(Long proofDocumentId) { this.proofDocumentId = proofDocumentId; }
    }
}
