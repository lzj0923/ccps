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

import com.ccps.backend.dto.OwnerReserveResponse.Account;
import com.ccps.backend.dto.OwnerReserveResponse.DocumentItem;
import com.ccps.backend.dto.OwnerReserveResponse.NotificationItem;
import com.ccps.backend.dto.OwnerReserveResponse.PropertyOption;
import com.ccps.backend.dto.OwnerReserveResponse.TransactionItem;

@Mapper
public interface OwnerReserveMapper {

    @Select("""
            SELECT
              COALESCE(SUM(ra.current_balance), 0) + COALESCE((SELECT SUM(rp.allocated_amount)
                FROM rent_payments rp JOIN finance_records fr ON fr.id=rp.finance_record_id
                JOIN owner_units rou ON rou.owner_id=fr.owner_id AND rou.unit_id=fr.unit_id
                  AND rou.status='active' AND rou.asset_stage='OPERATING'
                JOIN owners ro ON ro.id=rou.owner_id JOIN reserve_accounts rra ON rra.owner_unit_id=rou.id AND rra.status='active'
                WHERE ro.user_id=#{userId} AND fr.record_type='rent_payment' AND fr.payment_status='paid'
                  AND fr.confirmation_status='confirmed' AND fr.transaction_date<=CURRENT_DATE),0) AS total_balance,
              COALESCE(SUM(ra.current_balance), 0) + COALESCE((SELECT SUM(fr.amount)
                FROM rent_payments rp JOIN finance_records fr ON fr.id=rp.finance_record_id
                JOIN owner_units rou ON rou.owner_id=fr.owner_id AND rou.unit_id=fr.unit_id
                  AND rou.status='active' AND rou.asset_stage='OPERATING'
                JOIN owners ro ON ro.id=rou.owner_id JOIN reserve_accounts rra ON rra.owner_unit_id=rou.id AND rra.status='active'
                WHERE ro.user_id=#{userId} AND fr.record_type='rent_payment' AND fr.payment_status='paid'
                  AND fr.confirmation_status='confirmed' AND COALESCE(fr.receipt_date,fr.transaction_date)<=CURRENT_DATE),0) AS accounting_balance,
              COALESCE(SUM(ra.minimum_balance), 0) AS minimum_balance,
              COUNT(*) AS account_count,
              COALESCE(SUM(CASE WHEN ra.current_balance < ra.minimum_balance THEN 1 ELSE 0 END), 0) AS low_balance_count,
              COALESCE((SELECT SUM(rt.amount) FROM reserve_transactions rt
                JOIN reserve_accounts xra ON xra.id = rt.reserve_account_id
                JOIN owner_units xou ON xou.id = xra.owner_unit_id AND xou.status = 'active' AND xou.asset_stage = 'OPERATING'
                JOIN owners xo ON xo.id = xou.owner_id
                WHERE xo.user_id = #{userId} AND rt.transaction_type = 'topup'), 0) AS total_topups,
              COALESCE((SELECT COUNT(*) FROM reserve_transactions rt
                JOIN reserve_accounts xra ON xra.id = rt.reserve_account_id
                JOIN owner_units xou ON xou.id = xra.owner_unit_id AND xou.status = 'active' AND xou.asset_stage = 'OPERATING'
                JOIN owners xo ON xo.id = xou.owner_id
                WHERE xo.user_id = #{userId} AND rt.transaction_type = 'topup'), 0) AS topup_count,
              COALESCE((SELECT SUM(rt.amount) FROM reserve_transactions rt
                JOIN reserve_accounts xra ON xra.id = rt.reserve_account_id
                JOIN owner_units xou ON xou.id = xra.owner_unit_id AND xou.status = 'active' AND xou.asset_stage = 'OPERATING'
                JOIN owners xo ON xo.id = xou.owner_id
                WHERE xo.user_id = #{userId} AND rt.transaction_type = 'debit'), 0) AS total_debits,
              COALESCE((SELECT COUNT(*) FROM reserve_transactions rt
                JOIN reserve_accounts xra ON xra.id = rt.reserve_account_id
                JOIN owner_units xou ON xou.id = xra.owner_unit_id AND xou.status = 'active' AND xou.asset_stage = 'OPERATING'
                JOIN owners xo ON xo.id = xou.owner_id
                WHERE xo.user_id = #{userId} AND rt.transaction_type = 'debit'), 0) AS debit_count
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id = ra.owner_unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            JOIN owners o ON o.id = ou.owner_id
            WHERE o.user_id = #{userId} AND o.status = 'active' AND ra.status = 'active'
            """)
    SummaryRow findSummary(@Param("userId") Long userId);

    @Select("""
             SELECT ra.id, ou.id AS owner_unit_id, p.id AS project_id, p.name AS project_name,
                    u.unit_no, ra.minimum_balance,
                    ra.current_balance + COALESCE((SELECT SUM(rp.allocated_amount) FROM rent_payments rp
                      JOIN finance_records fr ON fr.id=rp.finance_record_id
                      WHERE fr.owner_id=o.id AND fr.unit_id=u.id AND fr.record_type='rent_payment'
                        AND fr.payment_status='paid' AND fr.confirmation_status='confirmed'
                        AND fr.transaction_date<=CURRENT_DATE),0) AS current_balance,
                    ra.current_balance + COALESCE((SELECT SUM(fr.amount) FROM rent_payments rp
                      JOIN finance_records fr ON fr.id=rp.finance_record_id
                      WHERE fr.owner_id=o.id AND fr.unit_id=u.id AND fr.record_type='rent_payment'
                        AND fr.payment_status='paid' AND fr.confirmation_status='confirmed'
                        AND COALESCE(fr.receipt_date,fr.transaction_date)<=CURRENT_DATE),0) AS accounting_balance,
                    CASE WHEN ra.current_balance + COALESCE((SELECT SUM(rp.allocated_amount) FROM rent_payments rp
                      JOIN finance_records fr ON fr.id=rp.finance_record_id
                      WHERE fr.owner_id=o.id AND fr.unit_id=u.id AND fr.record_type='rent_payment'
                        AND fr.payment_status='paid' AND fr.confirmation_status='confirmed'
                        AND fr.transaction_date<=CURRENT_DATE),0) < ra.minimum_balance THEN 'low' ELSE 'sufficient' END AS balance_status,
                   ra.low_balance_alert_enabled
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id = ra.owner_unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            JOIN owners o ON o.id = ou.owner_id
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE o.user_id = #{userId} AND o.status = 'active' AND ra.status = 'active'
            ORDER BY p.name, u.unit_no
            """)
    List<Account> findAccounts(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT p.id, p.name
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id = ra.owner_unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            JOIN owners o ON o.id = ou.owner_id
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE o.user_id = #{userId} AND o.status = 'active' AND ra.status = 'active'
            ORDER BY p.name
            """)
    List<PropertyOption> findProperties(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT rt.id, rt.finance_record_id, ra.id AS reserve_account_id,
                   p.id AS project_id, p.name AS project_name, u.unit_no,
                   rt.occurred_at, rt.transaction_type,
                   COALESCE(rt.note, CASE rt.transaction_type WHEN 'topup' THEN '預備金充值' WHEN 'debit' THEN '預備金扣款'
                     WHEN 'transfer_in' THEN '房產間內部調撥轉入' WHEN 'transfer_out' THEN '房產間內部調撥轉出'
                     WHEN 'transfer_reverse_in' THEN '內部調撥沖正轉入' WHEN 'transfer_reverse_out' THEN '內部調撥沖正轉出'
                     ELSE '預備金調整' END) AS description,
                   rt.amount, rt.balance_after, 'confirmed' AS status, 'confirmed' AS confirmation_status,
                   COALESCE(doc.total, 0) AS attachment_count
            FROM reserve_transactions rt
            JOIN reserve_accounts ra ON ra.id = rt.reserve_account_id
            JOIN owner_units ou ON ou.id = ra.owner_unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            JOIN owners o ON o.id = ou.owner_id
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN (
              SELECT dl.entity_id AS finance_record_id, COUNT(*) AS total
              FROM document_links dl JOIN documents d ON d.id = dl.document_id
              WHERE dl.entity_type = 'finance' AND d.document_type = 'reserve_topup_proof'
              GROUP BY dl.entity_id
            ) doc ON doc.finance_record_id = rt.finance_record_id
            WHERE o.user_id = #{userId} AND o.status = 'active'
              AND rt.occurred_at &gt;= #{startDate} AND rt.occurred_at &lt; #{endDate}
            <if test="projectId != null">AND p.id = #{projectId}</if>
            <if test="type != null and type != ''">AND rt.transaction_type = #{type}</if>
            ORDER BY rt.occurred_at DESC, rt.id DESC
            </script>
            """)
    List<TransactionItem> findTransactions(@Param("userId") Long userId,
            @Param("projectId") Long projectId, @Param("type") String type,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            <script>
            SELECT -fr.id AS id, fr.id AS finance_record_id, ra.id AS reserve_account_id,
                   p.id AS project_id, p.name AS project_name, u.unit_no,
                   fr.created_at AS occurred_at, 'topup' AS transaction_type,
                   CASE WHEN fr.confirmation_status = 'rejected'
                     THEN CONCAT('充值申請已退回：', COALESCE(pr.review_note, '請聯絡財務部'))
                     ELSE CONCAT('充值申請 · ', COALESCE(pr.bank_reference, fr.transaction_no)) END AS description,
                   fr.amount, ra.current_balance AS balance_after,
                   fr.confirmation_status AS status, fr.confirmation_status,
                   COALESCE(doc.total, 0) AS attachment_count
            FROM finance_records fr
            JOIN owners o ON o.id = fr.owner_id
            JOIN units u ON u.id = fr.unit_id
            JOIN projects p ON p.id = u.project_id
            JOIN owner_units ou ON ou.owner_id = o.id AND ou.unit_id = u.id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            JOIN reserve_accounts ra ON ra.owner_unit_id = ou.id AND ra.status = 'active'
            LEFT JOIN payment_receipts pr ON pr.finance_record_id = fr.id
            LEFT JOIN (
              SELECT dl.entity_id AS finance_record_id, COUNT(*) AS total
              FROM document_links dl JOIN documents d ON d.id = dl.document_id
              WHERE dl.entity_type = 'finance' AND d.document_type = 'reserve_topup_proof'
              GROUP BY dl.entity_id
            ) doc ON doc.finance_record_id = fr.id
            WHERE o.user_id = #{userId} AND fr.record_type = 'reserve_topup'
              AND fr.confirmation_status &lt;&gt; 'confirmed'
              AND fr.transaction_date &gt;= #{startDate} AND fr.transaction_date &lt; #{endDate}
            <if test="projectId != null">AND p.id = #{projectId}</if>
            ORDER BY fr.created_at DESC, fr.id DESC
            </script>
            """)
    List<TransactionItem> findPendingTopups(@Param("userId") Long userId,
            @Param("projectId") Long projectId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT n.id, n.title, n.body, n.priority, n.status, n.created_at
            FROM notifications n
            WHERE n.recipient_user_id = #{userId}
               OR n.recipient_owner_id IN (SELECT id FROM owners WHERE user_id = #{userId})
            ORDER BY n.created_at DESC LIMIT 6
            """)
    List<NotificationItem> findNotifications(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT d.id, d.original_name AS name, d.mime_type, COALESCE(d.file_size, 0) AS size,
                   d.document_type, d.status, d.created_at AS uploaded_at
            FROM documents d
            JOIN document_links dl ON dl.document_id = d.id AND dl.entity_type = 'finance'
            JOIN finance_records fr ON fr.id = dl.entity_id
            JOIN owners o ON o.id = fr.owner_id
            WHERE o.user_id = #{userId} AND fr.record_type = 'reserve_topup'
              AND d.document_type = 'reserve_topup_proof'
            ORDER BY d.created_at DESC, d.id DESC LIMIT 8
            """)
    List<DocumentItem> findDocuments(@Param("userId") Long userId);

    @Select("""
            SELECT ra.id AS reserve_account_id, ou.id AS owner_unit_id, u.id AS unit_id,
                   o.id AS owner_id, ra.current_balance
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id = ra.owner_unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            JOIN owners o ON o.id = ou.owner_id
            JOIN units u ON u.id = ou.unit_id
            WHERE ra.id = #{reserveAccountId} AND o.user_id = #{userId}
              AND o.status = 'active' AND ra.status = 'active'
            """)
    TopupContext findTopupContext(@Param("userId") Long userId, @Param("reserveAccountId") Long reserveAccountId);

    @Insert("""
            INSERT INTO finance_records
              (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date,
               payment_method, payment_status, confirmation_status, sync_status, created_by)
            VALUES
              (#{transactionNo}, 'reserve_topup', #{unitId}, #{ownerId}, #{amount}, 'MYR', #{paymentDate},
               #{paymentMethod}, 'paid', 'pending', 'not_synced', #{createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertFinanceRecord(NewTopup record);

    @Insert("""
            INSERT INTO documents
              (document_no, original_name, storage_key, mime_type, file_size, checksum_sha256,
               document_type, status, uploaded_by)
            VALUES
              (#{documentNo}, #{originalName}, #{storageKey}, #{mimeType}, #{fileSize}, #{checksumSha256},
               'reserve_topup_proof', 'pending_review', #{uploadedBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDocument(NewDocument document);

    @Insert("""
            INSERT INTO document_links (document_id, entity_type, entity_id, relation_type)
            VALUES (#{documentId}, 'finance', #{financeRecordId}, 'reserve_topup_proof')
            """)
    int insertDocumentLink(@Param("documentId") Long documentId, @Param("financeRecordId") Long financeRecordId);

    @Insert("""
            INSERT INTO payment_receipts
              (finance_record_id, receipt_no, payer_name, bank_reference, proof_document_id, submission_note)
            VALUES
              (#{financeRecordId}, #{receiptNo}, #{payerName}, #{bankReference}, #{proofDocumentId}, #{note})
            """)
    int insertReceipt(TopupReceipt receipt);

    @Select("""
            SELECT d.id AS document_id, d.original_name, d.storage_key, d.mime_type,
                   COALESCE(d.file_size, 0) AS file_size
            FROM documents d
            JOIN document_links dl ON dl.document_id = d.id AND dl.entity_type = 'finance'
            JOIN finance_records fr ON fr.id = dl.entity_id AND fr.record_type = 'reserve_topup'
            JOIN owners o ON o.id = fr.owner_id
            WHERE d.id = #{documentId} AND d.document_type = 'reserve_topup_proof'
              AND o.user_id = #{userId}
            """)
    DocumentFile findDocumentFile(@Param("userId") Long userId, @Param("documentId") Long documentId);

    @Select("""
            SELECT d.id AS document_id, d.original_name, d.storage_key, d.mime_type,
                   COALESCE(d.file_size, 0) AS file_size
            FROM documents d
            JOIN document_links dl ON dl.document_id = d.id AND dl.entity_type = 'finance'
            JOIN finance_records fr ON fr.id = dl.entity_id AND fr.record_type = 'reserve_topup'
            WHERE d.id = #{documentId} AND d.document_type = 'reserve_topup_proof'
            """)
    DocumentFile findAdminDocumentFile(@Param("documentId") Long documentId);

    @Select("""
            SELECT COUNT(*) FROM user_roles ur JOIN roles r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND r.code = 'ADMIN'
            """)
    int isAdmin(@Param("userId") Long userId);

    @Select("""
            SELECT fr.id AS finance_record_id, fr.transaction_no, fr.amount, fr.confirmation_status,
                   ra.id AS reserve_account_id, ra.current_balance, o.id AS owner_id, o.user_id,
                   p.name AS project_name, u.unit_no
            FROM finance_records fr
            JOIN owners o ON o.id = fr.owner_id
            JOIN owner_units ou ON ou.owner_id = o.id AND ou.unit_id = fr.unit_id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            JOIN reserve_accounts ra ON ra.owner_unit_id = ou.id AND ra.status = 'active'
            JOIN units u ON u.id = fr.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE fr.id = #{financeRecordId} AND fr.record_type = 'reserve_topup'
            FOR UPDATE
            """)
    TopupReviewRow findTopupForUpdate(@Param("financeRecordId") Long financeRecordId);

    @Insert("""
            INSERT INTO reserve_transactions
              (reserve_account_id, finance_record_id, transaction_type, amount, occurred_at,
               balance_after, note, created_by)
            VALUES
              (#{reserveAccountId}, #{financeRecordId}, 'topup', #{amount}, #{occurredAt},
               #{balanceAfter}, #{note}, #{createdBy})
            """)
    int insertReserveTopup(ApprovedTopup topup);

    @Update("UPDATE reserve_accounts SET current_balance = #{balance} WHERE id = #{reserveAccountId}")
    int updateReserveBalance(@Param("reserveAccountId") Long reserveAccountId, @Param("balance") BigDecimal balance);

    @Update("""
            UPDATE finance_records
            SET confirmation_status = #{status}, confirmed_by = #{reviewerId}, confirmed_at = NOW(),
                sync_status = CASE WHEN #{status} = 'confirmed' THEN 'pending' ELSE 'not_synced' END
            WHERE id = #{financeRecordId} AND confirmation_status = 'pending'
            """)
    int updateTopupReview(@Param("financeRecordId") Long financeRecordId,
            @Param("status") String status, @Param("reviewerId") Long reviewerId);

    @Update("UPDATE payment_receipts SET review_note = #{note} WHERE finance_record_id = #{financeRecordId}")
    int updateReviewNote(@Param("financeRecordId") Long financeRecordId, @Param("note") String note);

    @Insert("""
            INSERT INTO notifications
              (recipient_user_id, recipient_owner_id, title, body, related_type, related_id, priority, status)
            VALUES (#{userId}, #{ownerId}, #{title}, #{body}, 'finance_record', #{financeRecordId}, #{priority}, 'unread')
            """)
    int insertTopupNotification(@Param("userId") Long userId, @Param("ownerId") Long ownerId,
            @Param("financeRecordId") Long financeRecordId, @Param("title") String title,
            @Param("body") String body, @Param("priority") String priority);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, before_data, after_data)
            VALUES (#{actorId}, #{action}, 'finance_record', #{financeRecordId},
              JSON_OBJECT('confirmationStatus','pending'), JSON_OBJECT('confirmationStatus',#{status},'note',#{note}))
            """)
    int insertTopupAudit(@Param("actorId") Long actorId, @Param("financeRecordId") Long financeRecordId,
            @Param("action") String action, @Param("status") String status, @Param("note") String note);

    @Update("""
            UPDATE documents d JOIN document_links dl ON dl.document_id = d.id
            SET d.status = #{status}
            WHERE dl.entity_type = 'finance' AND dl.entity_id = #{financeRecordId}
              AND d.document_type = 'reserve_topup_proof'
            """)
    int updateTopupDocuments(@Param("financeRecordId") Long financeRecordId, @Param("status") String status);

    class SummaryRow {
        private BigDecimal totalBalance; private BigDecimal accountingBalance; private BigDecimal minimumBalance;
        private BigDecimal totalTopups; private Integer topupCount;
        private BigDecimal totalDebits; private Integer debitCount;
        private Integer lowBalanceCount; private Integer accountCount;
        public BigDecimal getTotalBalance() { return totalBalance; } public void setTotalBalance(BigDecimal v) { totalBalance = v; }
        public BigDecimal getAccountingBalance() { return accountingBalance; } public void setAccountingBalance(BigDecimal v) { accountingBalance = v; }
        public BigDecimal getMinimumBalance() { return minimumBalance; } public void setMinimumBalance(BigDecimal v) { minimumBalance = v; }
        public BigDecimal getTotalTopups() { return totalTopups; } public void setTotalTopups(BigDecimal v) { totalTopups = v; }
        public Integer getTopupCount() { return topupCount; } public void setTopupCount(Integer v) { topupCount = v; }
        public BigDecimal getTotalDebits() { return totalDebits; } public void setTotalDebits(BigDecimal v) { totalDebits = v; }
        public Integer getDebitCount() { return debitCount; } public void setDebitCount(Integer v) { debitCount = v; }
        public Integer getLowBalanceCount() { return lowBalanceCount; } public void setLowBalanceCount(Integer v) { lowBalanceCount = v; }
        public Integer getAccountCount() { return accountCount; } public void setAccountCount(Integer v) { accountCount = v; }
    }

    class TopupContext {
        private Long reserveAccountId; private Long ownerUnitId; private Long unitId; private Long ownerId; private BigDecimal currentBalance;
        public Long getReserveAccountId() { return reserveAccountId; } public void setReserveAccountId(Long v) { reserveAccountId = v; }
        public Long getOwnerUnitId() { return ownerUnitId; } public void setOwnerUnitId(Long v) { ownerUnitId = v; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long v) { unitId = v; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long v) { ownerId = v; }
        public BigDecimal getCurrentBalance() { return currentBalance; } public void setCurrentBalance(BigDecimal v) { currentBalance = v; }
    }

    class NewTopup {
        private Long id; private String transactionNo; private Long unitId; private Long ownerId;
        private BigDecimal amount; private LocalDate paymentDate; private String paymentMethod; private Long createdBy;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public String getTransactionNo() { return transactionNo; } public void setTransactionNo(String v) { transactionNo = v; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long v) { unitId = v; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long v) { ownerId = v; }
        public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { amount = v; }
        public LocalDate getPaymentDate() { return paymentDate; } public void setPaymentDate(LocalDate v) { paymentDate = v; }
        public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String v) { paymentMethod = v; }
        public Long getCreatedBy() { return createdBy; } public void setCreatedBy(Long v) { createdBy = v; }
    }

    class NewDocument {
        private Long id; private String documentNo; private String originalName; private String storageKey;
        private String mimeType; private Long fileSize; private String checksumSha256; private Long uploadedBy;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public String getDocumentNo() { return documentNo; } public void setDocumentNo(String v) { documentNo = v; }
        public String getOriginalName() { return originalName; } public void setOriginalName(String v) { originalName = v; }
        public String getStorageKey() { return storageKey; } public void setStorageKey(String v) { storageKey = v; }
        public String getMimeType() { return mimeType; } public void setMimeType(String v) { mimeType = v; }
        public Long getFileSize() { return fileSize; } public void setFileSize(Long v) { fileSize = v; }
        public String getChecksumSha256() { return checksumSha256; } public void setChecksumSha256(String v) { checksumSha256 = v; }
        public Long getUploadedBy() { return uploadedBy; } public void setUploadedBy(Long v) { uploadedBy = v; }
    }

    class TopupReceipt {
        private Long financeRecordId; private String receiptNo; private String payerName;
        private String bankReference; private Long proofDocumentId; private String note;
        public Long getFinanceRecordId() { return financeRecordId; } public void setFinanceRecordId(Long v) { financeRecordId = v; }
        public String getReceiptNo() { return receiptNo; } public void setReceiptNo(String v) { receiptNo = v; }
        public String getPayerName() { return payerName; } public void setPayerName(String v) { payerName = v; }
        public String getBankReference() { return bankReference; } public void setBankReference(String v) { bankReference = v; }
        public Long getProofDocumentId() { return proofDocumentId; } public void setProofDocumentId(Long v) { proofDocumentId = v; }
        public String getNote() { return note; } public void setNote(String v) { note = v; }
    }

    class DocumentFile {
        private Long documentId; private String originalName; private String storageKey; private String mimeType; private Long fileSize;
        public Long getDocumentId() { return documentId; } public void setDocumentId(Long v) { documentId = v; }
        public String getOriginalName() { return originalName; } public void setOriginalName(String v) { originalName = v; }
        public String getStorageKey() { return storageKey; } public void setStorageKey(String v) { storageKey = v; }
        public String getMimeType() { return mimeType; } public void setMimeType(String v) { mimeType = v; }
        public Long getFileSize() { return fileSize; } public void setFileSize(Long v) { fileSize = v; }
    }

    class TopupReviewRow {
        private Long financeRecordId; private String transactionNo; private BigDecimal amount;
        private String confirmationStatus; private Long reserveAccountId; private BigDecimal currentBalance;
        private Long ownerId; private Long userId; private String projectName; private String unitNo;
        public Long getFinanceRecordId() { return financeRecordId; } public void setFinanceRecordId(Long v) { financeRecordId = v; }
        public String getTransactionNo() { return transactionNo; } public void setTransactionNo(String v) { transactionNo = v; }
        public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { amount = v; }
        public String getConfirmationStatus() { return confirmationStatus; } public void setConfirmationStatus(String v) { confirmationStatus = v; }
        public Long getReserveAccountId() { return reserveAccountId; } public void setReserveAccountId(Long v) { reserveAccountId = v; }
        public BigDecimal getCurrentBalance() { return currentBalance; } public void setCurrentBalance(BigDecimal v) { currentBalance = v; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long v) { ownerId = v; }
        public Long getUserId() { return userId; } public void setUserId(Long v) { userId = v; }
        public String getProjectName() { return projectName; } public void setProjectName(String v) { projectName = v; }
        public String getUnitNo() { return unitNo; } public void setUnitNo(String v) { unitNo = v; }
    }

    class ApprovedTopup {
        private Long reserveAccountId; private Long financeRecordId; private BigDecimal amount;
        private LocalDateTime occurredAt; private BigDecimal balanceAfter; private String note; private Long createdBy;
        public Long getReserveAccountId() { return reserveAccountId; } public void setReserveAccountId(Long v) { reserveAccountId = v; }
        public Long getFinanceRecordId() { return financeRecordId; } public void setFinanceRecordId(Long v) { financeRecordId = v; }
        public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { amount = v; }
        public LocalDateTime getOccurredAt() { return occurredAt; } public void setOccurredAt(LocalDateTime v) { occurredAt = v; }
        public BigDecimal getBalanceAfter() { return balanceAfter; } public void setBalanceAfter(BigDecimal v) { balanceAfter = v; }
        public String getNote() { return note; } public void setNote(String v) { note = v; }
        public Long getCreatedBy() { return createdBy; } public void setCreatedBy(Long v) { createdBy = v; }
    }
}
