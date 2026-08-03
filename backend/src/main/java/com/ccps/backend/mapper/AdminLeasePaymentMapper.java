package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminLeasePaymentMapper {
    @Select("""
        SELECT rp.id AS paymentId,fr.id AS financeRecordId,ri.id AS invoiceId,l.id AS leaseId,l.lease_no AS leaseNo,
          ri.billing_month AS billingMonth,fr.transaction_no AS transactionNo,fr.amount,fr.transaction_date AS paymentDate,
          fr.payment_method AS paymentMethod,pr.payer_name AS payerName,pr.bank_reference AS paymentReference,
          pr.submission_note AS note,proof.documentId AS proofDocumentId,proof.originalName AS proofName,
          fr.sync_status AS syncStatus,fr.created_at AS createdAt
        FROM rent_payments rp JOIN rent_invoices ri ON ri.id=rp.rent_invoice_id JOIN leases l ON l.id=ri.lease_id
        JOIN finance_records fr ON fr.id=rp.finance_record_id LEFT JOIN payment_receipts pr ON pr.finance_record_id=fr.id
        LEFT JOIN (SELECT dl.entity_id AS financeId,MAX(d.id) AS documentId,MAX(d.original_name) AS originalName
          FROM document_links dl JOIN documents d ON d.id=dl.document_id
          WHERE dl.entity_type='finance' AND dl.relation_type='payment_proof' AND d.status<>'superseded' GROUP BY dl.entity_id) proof ON proof.financeId=fr.id
        WHERE l.id=#{leaseId} AND fr.payment_status<>'voided' ORDER BY fr.transaction_date DESC,fr.id DESC
        """)
    List<PaymentRow> list(@Param("leaseId") Long leaseId);

    @Select("""
        SELECT rp.id AS paymentId,fr.id AS financeRecordId,ri.id AS invoiceId,l.id AS leaseId,l.lease_no AS leaseNo,
          ri.billing_month AS billingMonth,ri.amount_due AS invoiceAmountDue,ri.amount_paid AS invoiceAmountPaid,
          fr.transaction_no AS transactionNo,fr.amount,fr.transaction_date AS paymentDate,fr.payment_method AS paymentMethod,
          fr.payment_status AS paymentStatus,fr.sync_status AS syncStatus,pr.payer_name AS payerName,
          pr.bank_reference AS paymentReference,pr.submission_note AS note,fr.created_at AS createdAt
        FROM rent_payments rp JOIN rent_invoices ri ON ri.id=rp.rent_invoice_id JOIN leases l ON l.id=ri.lease_id
        JOIN finance_records fr ON fr.id=rp.finance_record_id LEFT JOIN payment_receipts pr ON pr.finance_record_id=fr.id
        WHERE l.id=#{leaseId} AND rp.id=#{paymentId} FOR UPDATE
        """)
    PaymentRow lock(@Param("leaseId") Long leaseId,@Param("paymentId") Long paymentId);

    @Update("""
        UPDATE rent_invoices SET amount_paid=amount_paid+#{delta},status=CASE
          WHEN amount_paid+#{delta}>=amount_due THEN 'paid' WHEN amount_paid+#{delta}>0 THEN 'partial'
          WHEN due_date<CURRENT_DATE THEN 'overdue' ELSE 'unpaid' END
        WHERE id=#{invoiceId} AND amount_paid+#{delta}>=0 AND amount_paid+#{delta}<=amount_due
        """)
    int adjustInvoice(@Param("invoiceId") Long invoiceId,@Param("delta") BigDecimal delta);

    @Update("UPDATE finance_records SET amount=#{amount},transaction_date=#{paymentDate},payment_method=#{paymentMethod},confirmed_by=#{actorId},confirmed_at=NOW() WHERE id=#{financeRecordId} AND payment_status<>'voided'")
    int updateFinance(@Param("financeRecordId") Long financeRecordId,@Param("amount") BigDecimal amount,@Param("paymentDate") LocalDate paymentDate,@Param("paymentMethod") String paymentMethod,@Param("actorId") Long actorId);
    @Update("UPDATE payment_receipts SET payer_name=#{payerName},bank_reference=#{paymentReference},submission_note=#{note},review_note='管理員修改租金收款' WHERE finance_record_id=#{financeRecordId}")
    int updateReceipt(@Param("financeRecordId") Long financeRecordId,@Param("payerName") String payerName,@Param("paymentReference") String paymentReference,@Param("note") String note);
    @Update("UPDATE cashflow_entries SET occurred_on=#{paymentDate},description=#{description} WHERE finance_record_id=#{financeRecordId}")
    int updateCashflow(@Param("financeRecordId") Long financeRecordId,@Param("paymentDate") LocalDate paymentDate,@Param("description") String description);
    @Update("UPDATE finance_records SET payment_status='voided',confirmation_status='rejected',sync_status='pending',confirmed_by=#{actorId},confirmed_at=NOW() WHERE id=#{financeRecordId} AND payment_status<>'voided'")
    int voidFinance(@Param("financeRecordId") Long financeRecordId,@Param("actorId") Long actorId);
    @Insert("INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,before_data,after_data) VALUES (#{actorId},#{action},'finance_record',#{financeRecordId},CAST(#{beforeData} AS JSON),CAST(#{afterData} AS JSON))")
    int audit(@Param("actorId") Long actorId,@Param("action") String action,@Param("financeRecordId") Long financeRecordId,@Param("beforeData") String beforeData,@Param("afterData") String afterData);

    class PaymentRow {
        private Long paymentId,financeRecordId,invoiceId,leaseId,proofDocumentId;private String leaseNo,transactionNo,paymentMethod,payerName,paymentReference,note,proofName,syncStatus,paymentStatus;
        private BigDecimal amount,invoiceAmountDue,invoiceAmountPaid;private LocalDate billingMonth,paymentDate;private LocalDateTime createdAt;
        public Long getPaymentId(){return paymentId;}public void setPaymentId(Long v){paymentId=v;}public Long getFinanceRecordId(){return financeRecordId;}public void setFinanceRecordId(Long v){financeRecordId=v;}public Long getInvoiceId(){return invoiceId;}public void setInvoiceId(Long v){invoiceId=v;}public Long getLeaseId(){return leaseId;}public void setLeaseId(Long v){leaseId=v;}public Long getProofDocumentId(){return proofDocumentId;}public void setProofDocumentId(Long v){proofDocumentId=v;}
        public String getLeaseNo(){return leaseNo;}public void setLeaseNo(String v){leaseNo=v;}public String getTransactionNo(){return transactionNo;}public void setTransactionNo(String v){transactionNo=v;}public String getPaymentMethod(){return paymentMethod;}public void setPaymentMethod(String v){paymentMethod=v;}public String getPayerName(){return payerName;}public void setPayerName(String v){payerName=v;}public String getPaymentReference(){return paymentReference;}public void setPaymentReference(String v){paymentReference=v;}public String getNote(){return note;}public void setNote(String v){note=v;}public String getProofName(){return proofName;}public void setProofName(String v){proofName=v;}public String getSyncStatus(){return syncStatus;}public void setSyncStatus(String v){syncStatus=v;}public String getPaymentStatus(){return paymentStatus;}public void setPaymentStatus(String v){paymentStatus=v;}
        public BigDecimal getAmount(){return amount;}public void setAmount(BigDecimal v){amount=v;}public BigDecimal getInvoiceAmountDue(){return invoiceAmountDue;}public void setInvoiceAmountDue(BigDecimal v){invoiceAmountDue=v;}public BigDecimal getInvoiceAmountPaid(){return invoiceAmountPaid;}public void setInvoiceAmountPaid(BigDecimal v){invoiceAmountPaid=v;}public LocalDate getBillingMonth(){return billingMonth;}public void setBillingMonth(LocalDate v){billingMonth=v;}public LocalDate getPaymentDate(){return paymentDate;}public void setPaymentDate(LocalDate v){paymentDate=v;}public LocalDateTime getCreatedAt(){return createdAt;}public void setCreatedAt(LocalDateTime v){createdAt=v;}
    }
}
