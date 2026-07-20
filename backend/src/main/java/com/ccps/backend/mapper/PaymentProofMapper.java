package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PaymentProofMapper {

    @Select("""
            SELECT
              ou.id AS owner_unit_id,
              u.id AS unit_id,
              o.id AS owner_id,
              pc.currency,
              pi.amount_due,
              pi.amount_paid,
              COALESCE(SUM(CASE WHEN fr.confirmation_status <> 'rejected' THEN pra.allocated_amount ELSE 0 END), 0) AS pending_amount
            FROM owners o
            JOIN owner_units ou ON ou.owner_id = o.id AND ou.status = 'active' AND ou.asset_stage = 'PRE_HANDOVER'
            JOIN units u ON u.id = ou.unit_id
            JOIN purchase_contracts pc ON pc.owner_unit_id = ou.id AND pc.status = 'active'
            JOIN payment_plans pp ON pp.purchase_contract_id = pc.id AND pp.status = 'active'
            JOIN payment_installments pi ON pi.payment_plan_id = pp.id
            LEFT JOIN payment_receipt_allocations pra ON pra.installment_id = pi.id
            LEFT JOIN payment_receipts pr ON pr.id = pra.receipt_id
            LEFT JOIN finance_records fr ON fr.id = pr.finance_record_id
            WHERE o.user_id = #{userId}
              AND o.status = 'active'
              AND ou.id = #{ownerUnitId}
              AND pi.id = #{installmentId}
            GROUP BY ou.id, u.id, o.id, pc.currency, pi.amount_due, pi.amount_paid
            """)
    SubmissionContext findSubmissionContext(@Param("userId") Long userId,
            @Param("ownerUnitId") Long ownerUnitId, @Param("installmentId") Long installmentId);

    @Insert("""
            INSERT INTO finance_records
              (transaction_no, record_type, unit_id, owner_id, amount, currency,
               transaction_date, payment_method, payment_status, confirmation_status,
               sync_status, created_by)
            VALUES
              (#{transactionNo}, 'property_payment', #{unitId}, #{ownerId}, #{amount}, #{currency},
               #{paymentDate}, #{paymentMethod}, 'pending', 'pending', 'not_synced', #{createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertFinanceRecord(NewFinanceRecord record);

    @Insert("""
            INSERT INTO documents
              (document_no, original_name, storage_key, mime_type, file_size, checksum_sha256,
               document_type, status, uploaded_by)
            VALUES
              (#{documentNo}, #{originalName}, #{storageKey}, #{mimeType}, #{fileSize}, #{checksumSha256},
               'payment_proof', 'pending_review', #{uploadedBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDocument(NewDocument document);

    @Insert("""
            INSERT INTO payment_receipts
              (finance_record_id, receipt_no, payer_name, bank_reference, proof_document_id, review_note)
            VALUES
              (#{financeRecordId}, #{receiptNo}, #{payerName}, #{bankReference}, #{proofDocumentId}, #{submissionNote})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReceipt(NewReceipt receipt);

    @Insert("""
            INSERT INTO payment_receipt_allocations (receipt_id, installment_id, allocated_amount)
            VALUES (#{receiptId}, #{installmentId}, #{amount})
            """)
    int insertAllocation(@Param("receiptId") Long receiptId, @Param("installmentId") Long installmentId,
            @Param("amount") BigDecimal amount);

    @Insert("""
            INSERT INTO document_links (document_id, entity_type, entity_id, relation_type)
            VALUES (#{documentId}, 'finance', #{financeRecordId}, 'payment_proof')
            """)
    int insertDocumentLink(@Param("documentId") Long documentId, @Param("financeRecordId") Long financeRecordId);

    class SubmissionContext {
        private Long ownerUnitId;
        private Long unitId;
        private Long ownerId;
        private String currency;
        private BigDecimal amountDue;
        private BigDecimal amountPaid;
        private BigDecimal pendingAmount;

        public Long getOwnerUnitId() { return ownerUnitId; }
        public void setOwnerUnitId(Long ownerUnitId) { this.ownerUnitId = ownerUnitId; }
        public Long getUnitId() { return unitId; }
        public void setUnitId(Long unitId) { this.unitId = unitId; }
        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public BigDecimal getAmountDue() { return amountDue; }
        public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }
        public BigDecimal getAmountPaid() { return amountPaid; }
        public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
        public BigDecimal getPendingAmount() { return pendingAmount; }
        public void setPendingAmount(BigDecimal pendingAmount) { this.pendingAmount = pendingAmount; }
    }

    class NewFinanceRecord {
        private Long id;
        private String transactionNo;
        private Long unitId;
        private Long ownerId;
        private BigDecimal amount;
        private String currency;
        private LocalDate paymentDate;
        private String paymentMethod;
        private Long createdBy;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTransactionNo() { return transactionNo; }
        public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }
        public Long getUnitId() { return unitId; }
        public void setUnitId(Long unitId) { this.unitId = unitId; }
        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public LocalDate getPaymentDate() { return paymentDate; }
        public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public Long getCreatedBy() { return createdBy; }
        public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    }

    class NewDocument {
        private Long id;
        private String documentNo;
        private String originalName;
        private String storageKey;
        private String mimeType;
        private Long fileSize;
        private String checksumSha256;
        private Long uploadedBy;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getDocumentNo() { return documentNo; }
        public void setDocumentNo(String documentNo) { this.documentNo = documentNo; }
        public String getOriginalName() { return originalName; }
        public void setOriginalName(String originalName) { this.originalName = originalName; }
        public String getStorageKey() { return storageKey; }
        public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        public String getChecksumSha256() { return checksumSha256; }
        public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }
        public Long getUploadedBy() { return uploadedBy; }
        public void setUploadedBy(Long uploadedBy) { this.uploadedBy = uploadedBy; }
    }

    class NewReceipt {
        private Long id;
        private Long financeRecordId;
        private String receiptNo;
        private String payerName;
        private String bankReference;
        private Long proofDocumentId;
        private String submissionNote;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getFinanceRecordId() { return financeRecordId; }
        public void setFinanceRecordId(Long financeRecordId) { this.financeRecordId = financeRecordId; }
        public String getReceiptNo() { return receiptNo; }
        public void setReceiptNo(String receiptNo) { this.receiptNo = receiptNo; }
        public String getPayerName() { return payerName; }
        public void setPayerName(String payerName) { this.payerName = payerName; }
        public String getBankReference() { return bankReference; }
        public void setBankReference(String bankReference) { this.bankReference = bankReference; }
        public Long getProofDocumentId() { return proofDocumentId; }
        public void setProofDocumentId(Long proofDocumentId) { this.proofDocumentId = proofDocumentId; }
        public String getSubmissionNote() { return submissionNote; }
        public void setSubmissionNote(String submissionNote) { this.submissionNote = submissionNote; }
    }
}
