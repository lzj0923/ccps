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

import com.ccps.backend.dto.AdminTenancyOptionsResponse;

@Mapper
public interface AdminTenancyMapper {
    String RENT_COLLECTION_FROM = """
            FROM rent_invoices ri
            JOIN leases l ON l.id=ri.lease_id
            JOIN tenants t ON t.id=l.tenant_id
            JOIN units u ON u.id=l.unit_id
            JOIN projects p ON p.id=u.project_id
            LEFT JOIN finance_records latest_fr ON latest_fr.id=(
              SELECT fr2.id FROM rent_payments rp2 JOIN finance_records fr2 ON fr2.id=rp2.finance_record_id
              WHERE rp2.rent_invoice_id=ri.id AND fr2.confirmation_status='confirmed'
              ORDER BY fr2.transaction_date DESC,fr2.created_at DESC,fr2.id DESC LIMIT 1)
            LEFT JOIN documents latest_proof ON latest_proof.id=(
              SELECT dl.document_id FROM document_links dl JOIN documents d ON d.id=dl.document_id
              WHERE dl.entity_type='finance' AND dl.entity_id=latest_fr.id
                AND d.document_type='payment_proof' AND d.status&lt;&gt;'superseded'
              ORDER BY dl.id DESC LIMIT 1)
            """;
    String RENT_FINANCE_FROM = """
            FROM finance_records fr
            JOIN rent_payments rp ON rp.finance_record_id = fr.id
            JOIN rent_invoices ri ON ri.id = rp.rent_invoice_id
            JOIN leases l ON l.id = ri.lease_id
            JOIN tenants t ON t.id = l.tenant_id
            JOIN units u ON u.id = l.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN users confirmer ON confirmer.id = fr.confirmed_by
            LEFT JOIN documents proof ON proof.id = (
              SELECT dl.document_id FROM document_links dl
              WHERE dl.entity_type = 'finance' AND dl.entity_id = fr.id
              ORDER BY dl.id DESC LIMIT 1
            )
            """;
    String LIST_FROM = """
            FROM tenants t
            LEFT JOIN leases l ON l.id = (
              SELECT l2.id FROM leases l2 WHERE l2.tenant_id = t.id
              ORDER BY CASE WHEN l2.status = 'active' THEN 0 ELSE 1 END, l2.end_date DESC, l2.id DESC LIMIT 1
            )
            LEFT JOIN units u ON u.id = l.unit_id
            LEFT JOIN projects p ON p.id = u.project_id
            LEFT JOIN documents contract_doc ON contract_doc.id = l.contract_document_id
            LEFT JOIN rent_invoices ri ON ri.id = (
              SELECT ri2.id FROM rent_invoices ri2 WHERE ri2.lease_id = l.id
              ORDER BY ri2.billing_month DESC, ri2.id DESC LIMIT 1
            )
            LEFT JOIN finance_records fr ON fr.id = (
              SELECT rp2.finance_record_id FROM rent_payments rp2
              JOIN finance_records fr2 ON fr2.id = rp2.finance_record_id
              WHERE rp2.rent_invoice_id = ri.id
              ORDER BY fr2.created_at DESC, fr2.id DESC LIMIT 1
            )
            LEFT JOIN users confirmer ON confirmer.id = fr.confirmed_by
            """;

    @Select({
            "<script>",
            "SELECT t.id AS tenant_id, t.full_name AS tenant_name, t.identity_no, t.phone, t.email, t.status AS tenant_status,",
            "       l.id AS lease_id, l.lease_no, p.id AS project_id, p.name AS project_name, u.id AS unit_id, u.unit_no,",
            "       l.start_date AS lease_start, l.end_date AS lease_end, l.monthly_rent, l.deposit_amount, l.payment_day,",
            "       l.status AS lease_status, l.contract_document_id, contract_doc.original_name AS contract_document_name,",
            "       contract_doc.mime_type AS contract_document_mime_type, contract_doc.file_size AS contract_document_size,",
            "       ri.id AS invoice_id, ri.billing_month, ri.due_date,",
            "       COALESCE(ri.amount_due, 0) AS amount_due, COALESCE(ri.amount_paid, 0) AS amount_paid,",
            "       GREATEST(COALESCE(ri.amount_due, 0) - COALESCE(ri.amount_paid, 0), 0) AS amount_unpaid,",
            "       CASE WHEN ri.id IS NULL THEN 'no_invoice' WHEN ri.amount_paid >= ri.amount_due THEN 'paid'",
            "            WHEN ri.amount_paid > 0 THEN 'partial' WHEN ri.due_date &lt; CURRENT_DATE THEN 'overdue' ELSE 'unpaid' END AS rent_status,",
            "       fr.id AS finance_record_id, fr.transaction_no, fr.confirmation_status, fr.payment_method,",
            "       fr.transaction_date AS payment_date, confirmer.display_name AS confirmed_by_name, fr.confirmed_at,",
            "       (SELECT JSON_UNQUOTE(JSON_EXTRACT(al.after_data, '$.note')) FROM audit_logs al",
            "        WHERE al.entity_type = 'finance_record' AND al.entity_id = fr.id ORDER BY al.id DESC LIMIT 1) AS review_note",
            LIST_FROM,
            "<where>",
            "  <if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ', t.full_name, t.phone, t.email, l.lease_no, p.name, u.unit_no) LIKE CONCAT('%', #{keyword}, '%')</if>",
            "  <if test=\"projectName != null and projectName != ''\">AND p.name = #{projectName}</if>",
            "  <if test=\"status == 'paid'\">AND ri.amount_due > 0 AND ri.amount_paid >= ri.amount_due</if>",
            "  <if test=\"status == 'partial'\">AND ri.amount_paid > 0 AND ri.amount_paid &lt; ri.amount_due</if>",
            "  <if test=\"status == 'overdue'\">AND ri.amount_paid &lt; ri.amount_due AND ri.due_date &lt; CURRENT_DATE</if>",
            "  <if test=\"status == 'unpaid'\">AND ri.amount_paid = 0 AND ri.due_date &gt;= CURRENT_DATE</if>",
            "  <if test=\"status == 'pending_review'\">AND fr.confirmation_status = 'pending'</if>",
            "</where>",
            "ORDER BY CASE WHEN fr.confirmation_status = 'pending' THEN 0 WHEN ri.due_date &lt; CURRENT_DATE AND ri.amount_paid &lt; ri.amount_due THEN 1 ELSE 2 END, t.full_name, t.id",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<TenancyRow> findPage(@Param("keyword") String keyword, @Param("projectName") String projectName,
            @Param("status") String status, @Param("limit") int limit, @Param("offset") int offset);

    @Select({
            "<script>", "SELECT COUNT(*)", LIST_FROM, "<where>",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ', t.full_name, t.phone, t.email, l.lease_no, p.name, u.unit_no) LIKE CONCAT('%', #{keyword}, '%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name = #{projectName}</if>",
            "<if test=\"status == 'paid'\">AND ri.amount_due > 0 AND ri.amount_paid >= ri.amount_due</if>",
            "<if test=\"status == 'partial'\">AND ri.amount_paid > 0 AND ri.amount_paid &lt; ri.amount_due</if>",
            "<if test=\"status == 'overdue'\">AND ri.amount_paid &lt; ri.amount_due AND ri.due_date &lt; CURRENT_DATE</if>",
            "<if test=\"status == 'unpaid'\">AND ri.amount_paid = 0 AND ri.due_date &gt;= CURRENT_DATE</if>",
            "<if test=\"status == 'pending_review'\">AND fr.confirmation_status = 'pending'</if>",
            "</where>", "</script>"
    })
    Long countPage(@Param("keyword") String keyword, @Param("projectName") String projectName,
            @Param("status") String status);

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM tenants WHERE status = 'active') AS tenant_count,
              (SELECT COUNT(*) FROM leases WHERE status = 'active' AND CURRENT_DATE BETWEEN start_date AND end_date) AS active_lease_count,
              COALESCE(SUM(ri.amount_due), 0) AS current_due,
              COALESCE(SUM(ri.amount_paid), 0) AS current_paid,
              COALESCE(SUM(GREATEST(ri.amount_due - ri.amount_paid, 0)), 0) AS current_unpaid,
              SUM(ri.amount_paid > 0 AND ri.amount_paid < ri.amount_due) AS partial_count,
              SUM(ri.amount_paid < ri.amount_due AND ri.due_date < CURRENT_DATE) AS overdue_count,
              (SELECT COUNT(DISTINCT rp.rent_invoice_id) FROM rent_payments rp
               JOIN finance_records fr ON fr.id = rp.finance_record_id
               WHERE fr.confirmation_status = 'pending') AS pending_review_count
            FROM rent_invoices ri
            WHERE ri.billing_month = DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
            """)
    SummaryRow findSummary();

    @Select("SELECT DISTINCT p.name FROM projects p JOIN units u ON u.project_id = p.id JOIN leases l ON l.unit_id = u.id ORDER BY p.name")
    List<String> findProjects();

    @Select("SELECT id, full_name AS name FROM tenants WHERE status = 'active' ORDER BY full_name")
    List<AdminTenancyOptionsResponse.Tenant> findTenantOptions();

    @Select("""
            SELECT u.id, p.name AS project_name, u.unit_no
            FROM units u
            JOIN projects p ON p.id = u.project_id AND p.status = 'active'
            JOIN owner_units ou ON ou.unit_id = u.id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            WHERE NOT EXISTS (
              SELECT 1 FROM leases l WHERE l.unit_id = u.id AND l.status = 'active'
                AND l.end_date >= CURRENT_DATE
            )
            ORDER BY p.name, u.unit_no
            """)
    List<AdminTenancyOptionsResponse.Unit> findAvailableUnits();

    @Select({
            "<script>",
            "SELECT fr.id, fr.transaction_no, t.full_name AS tenant_name, p.name AS project_name, u.unit_no,",
            "       l.id AS lease_id, l.lease_no, ri.id AS invoice_id, ri.billing_month, ri.due_date,",
            "       ri.amount_due AS invoice_amount, ri.amount_paid AS invoice_paid, fr.amount, fr.currency,",
            "       fr.transaction_date, fr.payment_method, fr.confirmation_status, fr.sync_status,",
            "       proof.id AS proof_document_id, proof.original_name AS proof_name, proof.mime_type AS proof_mime_type, proof.file_size AS proof_size,",
            "       (SELECT JSON_UNQUOTE(JSON_EXTRACT(al.after_data, '$.note')) FROM audit_logs al",
            "        WHERE al.entity_type = 'finance_record' AND al.entity_id = fr.id ORDER BY al.id DESC LIMIT 1) AS review_note,",
            "       confirmer.display_name AS confirmed_by_name, fr.confirmed_at, fr.created_at AS submitted_at",
            RENT_FINANCE_FROM,
            "<where>",
            "  fr.record_type = 'rent_payment'",
            "  <if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ', fr.transaction_no, t.full_name, p.name, u.unit_no, l.lease_no) LIKE CONCAT('%', #{keyword}, '%')</if>",
            "  <if test=\"projectName != null and projectName != ''\">AND p.name = #{projectName}</if>",
            "  <if test=\"confirmationStatus == 'history'\">AND fr.confirmation_status &lt;&gt; 'pending'</if>",
            "  <if test=\"confirmationStatus != null and confirmationStatus != '' and confirmationStatus != 'history'\">AND fr.confirmation_status = #{confirmationStatus}</if>",
            "  <if test=\"syncStatus != null and syncStatus != ''\">AND fr.sync_status = #{syncStatus}</if>",
            "  <if test=\"proofStatus == 'proof_uploaded'\">AND proof.id IS NOT NULL</if>",
            "  <if test=\"proofStatus == 'proof_missing'\">AND proof.id IS NULL</if>",
            "  <if test=\"startDate != null\">AND fr.transaction_date &gt;= #{startDate}</if>",
            "  <if test=\"endDate != null\">AND fr.transaction_date &lt;= #{endDate}</if>",
            "</where>",
            "ORDER BY CASE fr.confirmation_status WHEN 'pending' THEN 0 WHEN 'rejected' THEN 1 ELSE 2 END, fr.created_at DESC, fr.id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<RentFinanceRow> findRentFinancePage(@Param("keyword") String keyword, @Param("projectName") String projectName,
            @Param("confirmationStatus") String confirmationStatus, @Param("syncStatus") String syncStatus,
            @Param("proofStatus") String proofStatus,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select({
            "<script>", "SELECT COUNT(*)", RENT_FINANCE_FROM, "<where>",
            "fr.record_type = 'rent_payment'",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ', fr.transaction_no, t.full_name, p.name, u.unit_no, l.lease_no) LIKE CONCAT('%', #{keyword}, '%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name = #{projectName}</if>",
            "<if test=\"confirmationStatus == 'history'\">AND fr.confirmation_status &lt;&gt; 'pending'</if>",
            "<if test=\"confirmationStatus != null and confirmationStatus != '' and confirmationStatus != 'history'\">AND fr.confirmation_status = #{confirmationStatus}</if>",
            "<if test=\"syncStatus != null and syncStatus != ''\">AND fr.sync_status = #{syncStatus}</if>",
            "<if test=\"proofStatus == 'proof_uploaded'\">AND proof.id IS NOT NULL</if>",
            "<if test=\"proofStatus == 'proof_missing'\">AND proof.id IS NULL</if>",
            "<if test=\"startDate != null\">AND fr.transaction_date &gt;= #{startDate}</if>",
            "<if test=\"endDate != null\">AND fr.transaction_date &lt;= #{endDate}</if>",
            "</where>", "</script>"
    })
    Long countRentFinancePage(@Param("keyword") String keyword, @Param("projectName") String projectName,
            @Param("confirmationStatus") String confirmationStatus, @Param("syncStatus") String syncStatus,
            @Param("proofStatus") String proofStatus,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COUNT(*) AS total_count,
                   SUM(EXISTS(SELECT 1 FROM document_links dl JOIN documents d ON d.id = dl.document_id
                              WHERE dl.entity_type = 'finance' AND dl.entity_id = fr.id
                                AND d.document_type = 'payment_proof' AND d.status <> 'superseded')) AS with_proof_count,
                   SUM(NOT EXISTS(SELECT 1 FROM document_links dl JOIN documents d ON d.id = dl.document_id
                                  WHERE dl.entity_type = 'finance' AND dl.entity_id = fr.id
                                    AND d.document_type = 'payment_proof' AND d.status <> 'superseded')) AS missing_proof_count,
                   SUM(fr.sync_status IN ('not_synced','pending','failed')) AS pending_sync_count,
                   COALESCE(SUM(fr.amount), 0) AS total_amount,
                   COALESCE(SUM(CASE WHEN YEAR(fr.transaction_date) = YEAR(CURRENT_DATE)
                                     AND MONTH(fr.transaction_date) = MONTH(CURRENT_DATE) THEN fr.amount ELSE 0 END), 0) AS month_amount
            FROM finance_records fr WHERE fr.record_type = 'rent_payment'
            """)
    RentFinanceSummaryRow findRentFinanceSummary();

    @Select("SELECT DISTINCT p.name FROM finance_records fr JOIN units u ON u.id = fr.unit_id JOIN projects p ON p.id = u.project_id WHERE fr.record_type = 'rent_payment' ORDER BY p.name")
    List<String> findRentFinanceProjects();

    @Select({
            "<script>",
            "SELECT ri.id AS invoice_id,l.id AS lease_id,l.lease_no,t.full_name AS tenant_name,p.name AS project_name,u.unit_no,",
            "ri.billing_month,ri.due_date,ri.amount_due,ri.amount_paid,GREATEST(ri.amount_due-ri.amount_paid,0) AS outstanding_amount,",
            "CASE WHEN ri.amount_paid&gt;0 THEN 'partial' WHEN ri.due_date&lt;CURRENT_DATE THEN 'overdue' ELSE 'unpaid' END AS collection_status,",
            "GREATEST(DATEDIFF(CURRENT_DATE,ri.due_date),0) AS overdue_days,latest_fr.id AS latest_finance_record_id,",
            "latest_fr.transaction_no AS latest_transaction_no,latest_fr.amount AS latest_payment_amount,",
            "latest_fr.transaction_date AS latest_payment_date,latest_fr.payment_method AS latest_payment_method,",
            "latest_proof.id AS latest_proof_document_id,latest_proof.original_name AS latest_proof_name,",
            "latest_proof.mime_type AS latest_proof_mime_type,latest_proof.file_size AS latest_proof_size,latest_fr.confirmed_at AS latest_confirmed_at",
            RENT_COLLECTION_FROM,
            "<where>",
            "ri.amount_paid &lt; ri.amount_due",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ',t.full_name,p.name,u.unit_no,l.lease_no) LIKE CONCAT('%',#{keyword},'%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name=#{projectName}</if>",
            "<if test=\"status == 'unpaid'\">AND ri.amount_paid=0</if>",
            "<if test=\"status == 'partial'\">AND ri.amount_paid&gt;0 AND ri.amount_paid&lt;ri.amount_due</if>",
            "<if test=\"status == 'overdue'\">AND ri.due_date&lt;CURRENT_DATE</if>",
            "<if test=\"startDate != null\">AND ri.due_date&gt;=#{startDate}</if>",
            "<if test=\"endDate != null\">AND ri.due_date&lt;=#{endDate}</if>",
            "</where>",
            "ORDER BY (ri.due_date&lt;CURRENT_DATE) DESC,(ri.amount_paid&gt;0) DESC,ri.due_date,ri.id LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<RentCollectionRow> findRentCollections(@Param("keyword") String keyword,
            @Param("projectName") String projectName, @Param("status") String status,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>","SELECT COUNT(*)",RENT_COLLECTION_FROM,"<where>",
            "ri.amount_paid &lt; ri.amount_due",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ',t.full_name,p.name,u.unit_no,l.lease_no) LIKE CONCAT('%',#{keyword},'%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name=#{projectName}</if>",
            "<if test=\"status == 'unpaid'\">AND ri.amount_paid=0</if>",
            "<if test=\"status == 'partial'\">AND ri.amount_paid&gt;0 AND ri.amount_paid&lt;ri.amount_due</if>",
            "<if test=\"status == 'overdue'\">AND ri.due_date&lt;CURRENT_DATE</if>",
            "<if test=\"startDate != null\">AND ri.due_date&gt;=#{startDate}</if>",
            "<if test=\"endDate != null\">AND ri.due_date&lt;=#{endDate}</if>","</where>","</script>"})
    Long countRentCollections(@Param("keyword") String keyword, @Param("projectName") String projectName,
            @Param("status") String status, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COUNT(*) AS outstanding_count,
                   COALESCE(SUM(ri.amount_paid=0),0) AS unpaid_count,
                   COALESCE(SUM(ri.amount_paid>0 AND ri.amount_paid<ri.amount_due),0) AS partial_count,
                   COALESCE(SUM(ri.due_date<CURRENT_DATE),0) AS overdue_count,
                   COALESCE(SUM(GREATEST(ri.amount_due-ri.amount_paid,0)),0) AS outstanding_amount,
                   COALESCE((SELECT SUM(fr.amount) FROM finance_records fr
                     WHERE fr.record_type='rent_payment' AND fr.confirmation_status='confirmed'
                       AND fr.transaction_date>=DATE_FORMAT(CURRENT_DATE,'%Y-%m-01')),0) AS month_received
            FROM rent_invoices ri WHERE ri.amount_paid<ri.amount_due
            """)
    RentCollectionSummaryRow findRentCollectionSummary();

    @Select("SELECT DISTINCT p.name FROM rent_invoices ri JOIN leases l ON l.id=ri.lease_id JOIN units u ON u.id=l.unit_id JOIN projects p ON p.id=u.project_id WHERE ri.amount_paid<ri.amount_due ORDER BY p.name")
    List<String> findRentCollectionProjects();

    @Select("""
            SELECT ri.id AS invoice_id,ri.amount_due,ri.amount_paid,l.id AS lease_id,l.lease_no,
                   t.id AS tenant_id,t.user_id,t.full_name AS tenant_name,u.id AS unit_id,u.unit_no,p.name AS project_name,
                   (SELECT ou.owner_id FROM owner_units ou WHERE ou.unit_id=u.id AND ou.status='active' ORDER BY ou.id DESC LIMIT 1) AS owner_id
            FROM rent_invoices ri JOIN leases l ON l.id=ri.lease_id JOIN tenants t ON t.id=l.tenant_id
            JOIN units u ON u.id=l.unit_id JOIN projects p ON p.id=u.project_id
            WHERE ri.id=#{invoiceId} FOR UPDATE
            """)
    RentCollectionContext lockRentCollection(@Param("invoiceId") Long invoiceId);

    @Insert("""
            INSERT INTO finance_records
              (transaction_no,record_type,unit_id,owner_id,tenant_id,amount,currency,transaction_date,payment_method,
               payment_status,confirmation_status,confirmed_by,confirmed_at,sync_status,created_by)
            VALUES (#{transactionNo},'rent_payment',#{unitId},#{ownerId},#{tenantId},#{amount},'MYR',#{paymentDate},#{paymentMethod},
                    'paid','confirmed',#{actorId},NOW(),'pending',#{actorId})
            """)
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insertConfirmedRentPayment(NewRentCollection record);

    @Insert("INSERT INTO rent_payments (rent_invoice_id,finance_record_id) VALUES (#{invoiceId},#{financeRecordId})")
    int linkRentPayment(@Param("invoiceId") Long invoiceId,@Param("financeRecordId") Long financeRecordId);

    @Insert("""
            INSERT INTO payment_receipts (finance_record_id,receipt_no,payer_name,bank_reference,submission_note,review_note)
            VALUES (#{financeRecordId},#{receiptNo},#{payerName},#{paymentReference},#{note},'管理員確認租金收款')
            """)
    int insertRentCollectionReceipt(@Param("financeRecordId") Long financeRecordId,@Param("receiptNo") String receiptNo,
            @Param("payerName") String payerName,@Param("paymentReference") String paymentReference,@Param("note") String note);

    @Update("""
            UPDATE rent_invoices SET amount_paid=amount_paid+#{amount},
              status=CASE WHEN amount_paid+#{amount}>=amount_due THEN 'paid' ELSE 'partial' END
            WHERE id=#{invoiceId} AND amount_paid+#{amount}<=amount_due
            """)
    int applyRentCollection(@Param("invoiceId") Long invoiceId,@Param("amount") BigDecimal amount);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,before_data,after_data)
            VALUES (#{actorId},'confirm_rent_collection','finance_record',#{financeRecordId},
              JSON_OBJECT('invoiceId',#{invoiceId},'amountPaid',#{beforePaid}),
              JSON_OBJECT('invoiceId',#{invoiceId},'receivedAmount',#{amount},'amountPaid',#{afterPaid},'note',#{note}))
            """)
    int insertRentCollectionAudit(@Param("actorId") Long actorId,@Param("financeRecordId") Long financeRecordId,
            @Param("invoiceId") Long invoiceId,@Param("beforePaid") BigDecimal beforePaid,
            @Param("amount") BigDecimal amount,@Param("afterPaid") BigDecimal afterPaid,@Param("note") String note);

    @Update("UPDATE payment_receipts SET proof_document_id=#{documentId} WHERE finance_record_id=#{financeRecordId}")
    int updateRentReceiptProof(@Param("financeRecordId") Long financeRecordId,@Param("documentId") Long documentId);

    @Select("""
            SELECT d.id, d.original_name, d.storage_key, d.mime_type, d.file_size
            FROM documents d JOIN document_links dl ON dl.document_id = d.id
            JOIN finance_records fr ON fr.id = dl.entity_id
            WHERE d.id = #{documentId} AND dl.entity_type = 'finance' AND fr.record_type = 'rent_payment'
            """)
    ContractFile findRentProofFile(@Param("documentId") Long documentId);

    @Select("""
            SELECT fr.id AS finance_record_id, fr.transaction_no,
                   (SELECT dl.document_id FROM document_links dl JOIN documents d ON d.id = dl.document_id
                    WHERE dl.entity_type = 'finance' AND dl.entity_id = fr.id AND d.document_type = 'payment_proof'
                    ORDER BY dl.id DESC LIMIT 1) AS proof_document_id
            FROM finance_records fr
            WHERE fr.id = #{financeRecordId} AND fr.record_type = 'rent_payment'
            FOR UPDATE
            """)
    RentProofContext lockRentProof(@Param("financeRecordId") Long financeRecordId);

    @Insert("""
            INSERT INTO documents
              (document_no, original_name, storage_key, mime_type, file_size, checksum_sha256,
               document_type, status, uploaded_by, reviewed_by, reviewed_at)
            VALUES
              (#{documentNo}, #{originalName}, #{storageKey}, #{mimeType}, #{fileSize}, #{checksumSha256},
               'payment_proof', 'approved', #{uploadedBy}, #{uploadedBy}, CURRENT_TIMESTAMP)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRentProofDocument(NewContractDocument document);

    @Insert("""
            INSERT INTO document_links (document_id, entity_type, entity_id, relation_type)
            VALUES (#{documentId}, 'finance', #{financeRecordId}, 'payment_proof')
            """)
    int insertRentProofLink(@Param("documentId") Long documentId, @Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE documents SET status = 'superseded' WHERE id = #{documentId} AND document_type = 'payment_proof'")
    int supersedeRentProof(@Param("documentId") Long documentId);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, before_data, after_data)
            VALUES (#{actorId}, #{action}, 'finance_record', #{financeRecordId},
                    JSON_OBJECT('proofDocumentId', #{oldDocumentId}),
                    JSON_OBJECT('proofDocumentId', #{newDocumentId}, 'fileName', #{fileName}))
            """)
    int insertRentProofAudit(@Param("actorId") Long actorId, @Param("financeRecordId") Long financeRecordId,
            @Param("oldDocumentId") Long oldDocumentId, @Param("newDocumentId") Long newDocumentId,
            @Param("fileName") String fileName, @Param("action") String action);

    @Select("SELECT COUNT(*) FROM tenants WHERE identity_no = #{value}")
    int countTenantIdentity(@Param("value") String value);

    @Select("SELECT COUNT(*) FROM tenants WHERE email = #{value}")
    int countTenantEmail(@Param("value") String value);

    @Insert("""
            INSERT INTO tenants (full_name, identity_no, phone, email, status)
            VALUES (#{fullName}, #{identityNo}, #{phone}, #{email}, #{status})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTenant(NewTenant tenant);

    @Select("SELECT COUNT(*) FROM tenants WHERE id = #{tenantId} AND status = 'active'")
    int countActiveTenant(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT COUNT(*) FROM owner_units ou
            WHERE ou.unit_id = #{unitId} AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            """)
    int countOperatingUnit(@Param("unitId") Long unitId);

    @Select("""
            SELECT COUNT(*) FROM leases
            WHERE unit_id = #{unitId} AND status = 'active'
              AND start_date <= #{endDate} AND end_date >= #{startDate}
            """)
    int countOverlappingLease(@Param("unitId") Long unitId, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT l.id AS lease_id,l.unit_id,l.tenant_id,l.lease_no,l.start_date,l.end_date,
                   l.monthly_rent,l.deposit_amount,l.payment_day,l.status,p.name AS project_name,u.unit_no
            FROM leases l JOIN units u ON u.id=l.unit_id JOIN projects p ON p.id=u.project_id
            WHERE l.id=#{leaseId} FOR UPDATE
            """)
    LeaseChangeContext lockLeaseForChange(@Param("leaseId") Long leaseId);

    @Select("""
            SELECT COUNT(*) FROM leases
            WHERE unit_id=#{unitId} AND id<>#{leaseId} AND status='active'
              AND start_date<=#{endDate} AND end_date>=#{startDate}
            """)
    int countOtherOverlappingLease(@Param("leaseId") Long leaseId,@Param("unitId") Long unitId,
            @Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate);

    @Update("""
            UPDATE leases SET start_date=#{startDate},end_date=#{endDate},monthly_rent=#{monthlyRent},
              deposit_amount=#{depositAmount},payment_day=#{paymentDay}
            WHERE id=#{leaseId} AND status='active'
            """)
    int updateLeaseTerms(@Param("leaseId") Long leaseId,@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,@Param("monthlyRent") BigDecimal monthlyRent,
            @Param("depositAmount") BigDecimal depositAmount,@Param("paymentDay") int paymentDay);

    @Update("""
            UPDATE rent_invoices SET amount_due=#{monthlyRent},
              due_date=DATE_ADD(billing_month,INTERVAL (LEAST(#{paymentDay},DAY(LAST_DAY(billing_month)))-1) DAY)
            WHERE lease_id=#{leaseId} AND billing_month>=#{fromMonth} AND amount_paid=0
              AND NOT EXISTS (SELECT 1 FROM rent_payments rp WHERE rp.rent_invoice_id=rent_invoices.id)
            """)
    int updateFutureUnpaidInvoiceTerms(@Param("leaseId") Long leaseId,@Param("fromMonth") LocalDate fromMonth,
            @Param("monthlyRent") BigDecimal monthlyRent,@Param("paymentDay") int paymentDay);

    @Update("UPDATE leases SET end_date=#{endDate},status='transferred' WHERE id=#{leaseId} AND status='active'")
    int closeLeaseForTransfer(@Param("leaseId") Long leaseId,@Param("endDate") LocalDate endDate);

    @org.apache.ibatis.annotations.Delete("""
            DELETE FROM rent_invoices WHERE lease_id=#{leaseId} AND billing_month>#{transferMonth}
              AND amount_paid=0 AND NOT EXISTS (SELECT 1 FROM rent_payments rp WHERE rp.rent_invoice_id=rent_invoices.id)
            """)
    int deleteOldFutureInvoices(@Param("leaseId") Long leaseId,@Param("transferMonth") LocalDate transferMonth);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,before_data,after_data)
            VALUES (#{actorId},'update_lease','lease',#{leaseId},
              JSON_OBJECT('startDate',#{oldStart},'endDate',#{oldEnd},'monthlyRent',#{oldRent},'depositAmount',#{oldDeposit},'paymentDay',#{oldPaymentDay}),
              JSON_OBJECT('startDate',#{newStart},'endDate',#{newEnd},'monthlyRent',#{newRent},'depositAmount',#{newDeposit},'paymentDay',#{newPaymentDay}))
            """)
    int insertLeaseUpdateAudit(@Param("actorId") Long actorId,@Param("leaseId") Long leaseId,
            @Param("oldStart") LocalDate oldStart,@Param("oldEnd") LocalDate oldEnd,@Param("oldRent") BigDecimal oldRent,
            @Param("oldDeposit") BigDecimal oldDeposit,@Param("oldPaymentDay") int oldPaymentDay,
            @Param("newStart") LocalDate newStart,@Param("newEnd") LocalDate newEnd,@Param("newRent") BigDecimal newRent,
            @Param("newDeposit") BigDecimal newDeposit,@Param("newPaymentDay") int newPaymentDay);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,before_data,after_data)
            VALUES (#{actorId},'transfer_lease','lease',#{oldLeaseId},
              JSON_OBJECT('oldTenantId',#{oldTenantId},'originalEndDate',#{originalEndDate}),
              JSON_OBJECT('newLeaseId',#{newLeaseId},'newTenantId',#{newTenantId},'transferDate',#{transferDate}))
            """)
    int insertLeaseTransferAudit(@Param("actorId") Long actorId,@Param("oldLeaseId") Long oldLeaseId,
            @Param("newLeaseId") Long newLeaseId,@Param("oldTenantId") Long oldTenantId,
            @Param("newTenantId") Long newTenantId,@Param("originalEndDate") LocalDate originalEndDate,
            @Param("transferDate") LocalDate transferDate);

    @Insert("""
            INSERT INTO leases (unit_id, tenant_id, lease_no, start_date, end_date,
                                monthly_rent, deposit_amount, payment_day, status)
            VALUES (#{unitId}, #{tenantId}, #{leaseNo}, #{startDate}, #{endDate},
                    #{monthlyRent}, #{depositAmount}, #{paymentDay}, 'active')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLease(NewLease lease);

    @Insert("""
            INSERT INTO owner_unit_services (owner_unit_id, service_type, status, started_at, ended_at)
            SELECT ou.id, 'RENTAL', 'active', CURRENT_DATE, NULL
            FROM owner_units ou WHERE ou.unit_id = #{unitId} AND ou.status = 'active'
            ON DUPLICATE KEY UPDATE status = 'active', ended_at = NULL
            """)
    int activateRentalService(@Param("unitId") Long unitId);

    @Update("UPDATE units SET listing_status = 'rented' WHERE id = #{unitId}")
    int markUnitRented(@Param("unitId") Long unitId);

    @Insert("""
            INSERT INTO rent_invoices (lease_id, billing_month, due_date, amount_due, amount_paid, status)
            VALUES (#{leaseId}, #{billingMonth}, #{dueDate}, #{amountDue}, 0, 'unpaid')
            """)
    int insertInvoice(@Param("leaseId") Long leaseId, @Param("billingMonth") LocalDate billingMonth,
            @Param("dueDate") LocalDate dueDate, @Param("amountDue") BigDecimal amountDue);

    @Insert("""
            INSERT IGNORE INTO rent_invoices
                (lease_id, billing_month, due_date, amount_due, amount_paid, status)
            SELECT l.id, #{billingMonth},
                   DATE_ADD(#{billingMonth}, INTERVAL (LEAST(l.payment_day, DAY(LAST_DAY(#{billingMonth}))) - 1) DAY),
                   l.monthly_rent, 0, 'unpaid'
            FROM leases l
            WHERE l.status = 'active'
              AND l.start_date &lt;= LAST_DAY(#{billingMonth})
              AND l.end_date &gt;= #{billingMonth}
            """)
    int generateMonthlyInvoices(@Param("billingMonth") LocalDate billingMonth);

    @Select("""
            SELECT id AS lease_id, lease_no, contract_document_id
            FROM leases WHERE id = #{leaseId} FOR UPDATE
            """)
    LeaseContractContext lockLeaseContract(@Param("leaseId") Long leaseId);

    @Insert("""
            INSERT INTO documents
              (document_no, original_name, storage_key, mime_type, file_size, checksum_sha256,
               document_type, status, uploaded_by, reviewed_by, reviewed_at)
            VALUES
              (#{documentNo}, #{originalName}, #{storageKey}, #{mimeType}, #{fileSize}, #{checksumSha256},
               'lease', 'approved', #{uploadedBy}, #{uploadedBy}, CURRENT_TIMESTAMP)
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertContractDocument(NewContractDocument document);

    @Insert("""
            INSERT INTO document_links (document_id, entity_type, entity_id, relation_type)
            VALUES (#{documentId}, 'lease', #{leaseId}, 'contract')
            """)
    int insertContractLink(@Param("documentId") Long documentId, @Param("leaseId") Long leaseId);

    @Update("UPDATE documents SET status = 'superseded' WHERE id = #{documentId} AND document_type = 'lease'")
    int supersedeContractDocument(@Param("documentId") Long documentId);

    @Update("UPDATE leases SET contract_document_id = #{documentId} WHERE id = #{leaseId}")
    int updateLeaseContract(@Param("leaseId") Long leaseId, @Param("documentId") Long documentId);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, before_data, after_data)
            VALUES (#{actorId}, #{action}, 'lease', #{leaseId},
                    JSON_OBJECT('contractDocumentId', #{oldDocumentId}),
                    JSON_OBJECT('contractDocumentId', #{newDocumentId}, 'fileName', #{fileName}))
            """)
    int insertContractAudit(@Param("actorId") Long actorId, @Param("leaseId") Long leaseId,
            @Param("oldDocumentId") Long oldDocumentId, @Param("newDocumentId") Long newDocumentId,
            @Param("fileName") String fileName, @Param("action") String action);

    @Select("""
            SELECT d.id, d.original_name, d.storage_key, d.mime_type, d.file_size
            FROM leases l JOIN documents d ON d.id = l.contract_document_id
            JOIN document_links dl ON dl.document_id = d.id AND dl.entity_type = 'lease'
              AND dl.entity_id = l.id AND dl.relation_type = 'contract'
            WHERE l.id = #{leaseId} AND d.document_type = 'lease'
            """)
    ContractFile findContractFile(@Param("leaseId") Long leaseId);

    @Insert("""
            INSERT INTO notifications (recipient_user_id, title, body, related_type, related_id, priority, status)
            SELECT user_id, #{title}, #{body}, #{relatedType}, #{relatedId}, #{priority}, 'unread'
            FROM tenants WHERE id = #{tenantId} AND user_id IS NOT NULL
            """)
    int insertTenantNotification(@Param("tenantId") Long tenantId, @Param("title") String title,
            @Param("body") String body, @Param("relatedType") String relatedType,
            @Param("relatedId") Long relatedId, @Param("priority") String priority);

    @Select("""
            SELECT ri.id AS invoice_id, t.id AS tenant_id, t.full_name AS tenant_name,
                   p.name AS project_name, u.unit_no, ri.amount_due, ri.amount_paid, ri.due_date
            FROM rent_invoices ri JOIN leases l ON l.id = ri.lease_id JOIN tenants t ON t.id = l.tenant_id
            JOIN units u ON u.id = l.unit_id JOIN projects p ON p.id = u.project_id
            WHERE ri.id = #{invoiceId}
            """)
    ReminderContext findReminder(@Param("invoiceId") Long invoiceId);

    class SummaryRow {
        private Long tenantCount, activeLeaseCount, partialCount, overdueCount, pendingReviewCount;
        private BigDecimal currentDue, currentPaid, currentUnpaid;
        public Long getTenantCount() { return tenantCount; } public void setTenantCount(Long v) { tenantCount = v; }
        public Long getActiveLeaseCount() { return activeLeaseCount; } public void setActiveLeaseCount(Long v) { activeLeaseCount = v; }
        public Long getPartialCount() { return partialCount; } public void setPartialCount(Long v) { partialCount = v; }
        public Long getOverdueCount() { return overdueCount; } public void setOverdueCount(Long v) { overdueCount = v; }
        public Long getPendingReviewCount() { return pendingReviewCount; } public void setPendingReviewCount(Long v) { pendingReviewCount = v; }
        public BigDecimal getCurrentDue() { return currentDue; } public void setCurrentDue(BigDecimal v) { currentDue = v; }
        public BigDecimal getCurrentPaid() { return currentPaid; } public void setCurrentPaid(BigDecimal v) { currentPaid = v; }
        public BigDecimal getCurrentUnpaid() { return currentUnpaid; } public void setCurrentUnpaid(BigDecimal v) { currentUnpaid = v; }
    }

    class TenancyRow {
        private Long tenantId, leaseId, projectId, unitId, contractDocumentId, invoiceId, financeRecordId;
        private String tenantName, identityNo, phone, email, tenantStatus, leaseNo, projectName, unitNo, leaseStatus,
                contractDocumentName, contractDocumentMimeType,
                rentStatus, transactionNo, confirmationStatus, paymentMethod, reviewNote, confirmedByName;
        private LocalDate leaseStart, leaseEnd, billingMonth, dueDate, paymentDate;
        private LocalDateTime confirmedAt;
        private BigDecimal monthlyRent, depositAmount, amountDue, amountPaid, amountUnpaid;
        private Long contractDocumentSize;
        private Integer paymentDay;
        public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;}
        public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;}
        public Long getContractDocumentId(){return contractDocumentId;} public void setContractDocumentId(Long v){contractDocumentId=v;} public Long getInvoiceId(){return invoiceId;} public void setInvoiceId(Long v){invoiceId=v;}
        public String getContractDocumentName(){return contractDocumentName;} public void setContractDocumentName(String v){contractDocumentName=v;} public String getContractDocumentMimeType(){return contractDocumentMimeType;} public void setContractDocumentMimeType(String v){contractDocumentMimeType=v;} public Long getContractDocumentSize(){return contractDocumentSize;} public void setContractDocumentSize(Long v){contractDocumentSize=v;}
        public Long getFinanceRecordId(){return financeRecordId;} public void setFinanceRecordId(Long v){financeRecordId=v;} public String getTenantName(){return tenantName;} public void setTenantName(String v){tenantName=v;}
        public String getIdentityNo(){return identityNo;} public void setIdentityNo(String v){identityNo=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
        public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getTenantStatus(){return tenantStatus;} public void setTenantStatus(String v){tenantStatus=v;}
        public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;}
        public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} public String getLeaseStatus(){return leaseStatus;} public void setLeaseStatus(String v){leaseStatus=v;}
        public String getRentStatus(){return rentStatus;} public void setRentStatus(String v){rentStatus=v;} public String getTransactionNo(){return transactionNo;} public void setTransactionNo(String v){transactionNo=v;}
        public String getConfirmationStatus(){return confirmationStatus;} public void setConfirmationStatus(String v){confirmationStatus=v;} public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;}
        public String getReviewNote(){return reviewNote;} public void setReviewNote(String v){reviewNote=v;} public String getConfirmedByName(){return confirmedByName;} public void setConfirmedByName(String v){confirmedByName=v;}
        public LocalDate getLeaseStart(){return leaseStart;} public void setLeaseStart(LocalDate v){leaseStart=v;} public LocalDate getLeaseEnd(){return leaseEnd;} public void setLeaseEnd(LocalDate v){leaseEnd=v;}
        public LocalDate getBillingMonth(){return billingMonth;} public void setBillingMonth(LocalDate v){billingMonth=v;} public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){dueDate=v;}
        public LocalDate getPaymentDate(){return paymentDate;} public void setPaymentDate(LocalDate v){paymentDate=v;} public LocalDateTime getConfirmedAt(){return confirmedAt;} public void setConfirmedAt(LocalDateTime v){confirmedAt=v;}
        public BigDecimal getMonthlyRent(){return monthlyRent;} public void setMonthlyRent(BigDecimal v){monthlyRent=v;} public BigDecimal getDepositAmount(){return depositAmount;} public void setDepositAmount(BigDecimal v){depositAmount=v;}
        public BigDecimal getAmountDue(){return amountDue;} public void setAmountDue(BigDecimal v){amountDue=v;} public BigDecimal getAmountPaid(){return amountPaid;} public void setAmountPaid(BigDecimal v){amountPaid=v;}
        public BigDecimal getAmountUnpaid(){return amountUnpaid;} public void setAmountUnpaid(BigDecimal v){amountUnpaid=v;} public Integer getPaymentDay(){return paymentDay;} public void setPaymentDay(Integer v){paymentDay=v;}
    }

    class NewTenant { private Long id; private String fullName, identityNo, phone, email, status;
        public Long getId(){return id;} public void setId(Long v){id=v;} public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
        public String getIdentityNo(){return identityNo;} public void setIdentityNo(String v){identityNo=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
        public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}}
    class NewLease { private Long id, unitId, tenantId; private String leaseNo; private LocalDate startDate,endDate; private BigDecimal monthlyRent,depositAmount; private Integer paymentDay;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;} public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;}
        public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;} public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
        public BigDecimal getMonthlyRent(){return monthlyRent;} public void setMonthlyRent(BigDecimal v){monthlyRent=v;} public BigDecimal getDepositAmount(){return depositAmount;} public void setDepositAmount(BigDecimal v){depositAmount=v;} public Integer getPaymentDay(){return paymentDay;} public void setPaymentDay(Integer v){paymentDay=v;}}
    class ReminderContext { private Long invoiceId,tenantId; private String tenantName,projectName,unitNo; private BigDecimal amountDue,amountPaid; private LocalDate dueDate;
        public Long getInvoiceId(){return invoiceId;} public void setInvoiceId(Long v){invoiceId=v;} public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public String getTenantName(){return tenantName;} public void setTenantName(String v){tenantName=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;} public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} public BigDecimal getAmountDue(){return amountDue;} public void setAmountDue(BigDecimal v){amountDue=v;} public BigDecimal getAmountPaid(){return amountPaid;} public void setAmountPaid(BigDecimal v){amountPaid=v;} public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){dueDate=v;}}
    class LeaseContractContext { private Long leaseId, contractDocumentId; private String leaseNo;
        public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;} public Long getContractDocumentId(){return contractDocumentId;} public void setContractDocumentId(Long v){contractDocumentId=v;} public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;}}
    class LeaseChangeContext {
        private Long leaseId,unitId,tenantId; private String leaseNo,status,projectName,unitNo;
        private LocalDate startDate,endDate; private BigDecimal monthlyRent,depositAmount; private Integer paymentDay;
        public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;} public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;}
        public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;} public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;}
        public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;} public BigDecimal getMonthlyRent(){return monthlyRent;} public void setMonthlyRent(BigDecimal v){monthlyRent=v;} public BigDecimal getDepositAmount(){return depositAmount;} public void setDepositAmount(BigDecimal v){depositAmount=v;} public Integer getPaymentDay(){return paymentDay;} public void setPaymentDay(Integer v){paymentDay=v;}
    }
    class NewContractDocument { private Long id, fileSize, uploadedBy; private String documentNo,originalName,storageKey,mimeType,checksumSha256;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getFileSize(){return fileSize;} public void setFileSize(Long v){fileSize=v;} public Long getUploadedBy(){return uploadedBy;} public void setUploadedBy(Long v){uploadedBy=v;} public String getDocumentNo(){return documentNo;} public void setDocumentNo(String v){documentNo=v;} public String getOriginalName(){return originalName;} public void setOriginalName(String v){originalName=v;} public String getStorageKey(){return storageKey;} public void setStorageKey(String v){storageKey=v;} public String getMimeType(){return mimeType;} public void setMimeType(String v){mimeType=v;} public String getChecksumSha256(){return checksumSha256;} public void setChecksumSha256(String v){checksumSha256=v;}}
    class ContractFile { private Long id,fileSize; private String originalName,storageKey,mimeType;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getFileSize(){return fileSize;} public void setFileSize(Long v){fileSize=v;} public String getOriginalName(){return originalName;} public void setOriginalName(String v){originalName=v;} public String getStorageKey(){return storageKey;} public void setStorageKey(String v){storageKey=v;} public String getMimeType(){return mimeType;} public void setMimeType(String v){mimeType=v;}}
    class RentFinanceRow { private Long id,leaseId,invoiceId,proofDocumentId,proofSize; private String transactionNo,tenantName,projectName,unitNo,leaseNo,currency,paymentMethod,confirmationStatus,syncStatus,proofName,proofMimeType,reviewNote,confirmedByName; private LocalDate billingMonth,dueDate,transactionDate; private LocalDateTime confirmedAt,submittedAt; private BigDecimal invoiceAmount,invoicePaid,amount;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;} public Long getInvoiceId(){return invoiceId;} public void setInvoiceId(Long v){invoiceId=v;} public Long getProofDocumentId(){return proofDocumentId;} public void setProofDocumentId(Long v){proofDocumentId=v;} public Long getProofSize(){return proofSize;} public void setProofSize(Long v){proofSize=v;} public String getTransactionNo(){return transactionNo;} public void setTransactionNo(String v){transactionNo=v;} public String getTenantName(){return tenantName;} public void setTenantName(String v){tenantName=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;} public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;} public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;} public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;} public String getConfirmationStatus(){return confirmationStatus;} public void setConfirmationStatus(String v){confirmationStatus=v;} public String getSyncStatus(){return syncStatus;} public void setSyncStatus(String v){syncStatus=v;} public String getProofName(){return proofName;} public void setProofName(String v){proofName=v;} public String getProofMimeType(){return proofMimeType;} public void setProofMimeType(String v){proofMimeType=v;} public String getReviewNote(){return reviewNote;} public void setReviewNote(String v){reviewNote=v;} public String getConfirmedByName(){return confirmedByName;} public void setConfirmedByName(String v){confirmedByName=v;} public LocalDate getBillingMonth(){return billingMonth;} public void setBillingMonth(LocalDate v){billingMonth=v;} public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){dueDate=v;} public LocalDate getTransactionDate(){return transactionDate;} public void setTransactionDate(LocalDate v){transactionDate=v;} public LocalDateTime getConfirmedAt(){return confirmedAt;} public void setConfirmedAt(LocalDateTime v){confirmedAt=v;} public LocalDateTime getSubmittedAt(){return submittedAt;} public void setSubmittedAt(LocalDateTime v){submittedAt=v;} public BigDecimal getInvoiceAmount(){return invoiceAmount;} public void setInvoiceAmount(BigDecimal v){invoiceAmount=v;} public BigDecimal getInvoicePaid(){return invoicePaid;} public void setInvoicePaid(BigDecimal v){invoicePaid=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}}
    class RentFinanceSummaryRow { private Long totalCount,withProofCount,missingProofCount,pendingSyncCount; private BigDecimal totalAmount,monthAmount;
        public Long getTotalCount(){return totalCount;} public void setTotalCount(Long v){totalCount=v;} public Long getWithProofCount(){return withProofCount;} public void setWithProofCount(Long v){withProofCount=v;} public Long getMissingProofCount(){return missingProofCount;} public void setMissingProofCount(Long v){missingProofCount=v;} public Long getPendingSyncCount(){return pendingSyncCount;} public void setPendingSyncCount(Long v){pendingSyncCount=v;} public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal v){totalAmount=v;} public BigDecimal getMonthAmount(){return monthAmount;} public void setMonthAmount(BigDecimal v){monthAmount=v;}}
    class RentProofContext { private Long financeRecordId,proofDocumentId; private String transactionNo;
        public Long getFinanceRecordId(){return financeRecordId;} public void setFinanceRecordId(Long v){financeRecordId=v;} public Long getProofDocumentId(){return proofDocumentId;} public void setProofDocumentId(Long v){proofDocumentId=v;} public String getTransactionNo(){return transactionNo;} public void setTransactionNo(String v){transactionNo=v;}}
    class RentCollectionRow {
        private Long invoiceId,leaseId,latestFinanceRecordId,latestProofDocumentId,latestProofSize,overdueDays;
        private String leaseNo,tenantName,projectName,unitNo,collectionStatus,latestTransactionNo,latestPaymentMethod,
                latestProofName,latestProofMimeType;
        private LocalDate billingMonth,dueDate,latestPaymentDate; private LocalDateTime latestConfirmedAt;
        private BigDecimal amountDue,amountPaid,outstandingAmount,latestPaymentAmount;
        public Long getInvoiceId(){return invoiceId;} public void setInvoiceId(Long v){invoiceId=v;} public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;}
        public Long getLatestFinanceRecordId(){return latestFinanceRecordId;} public void setLatestFinanceRecordId(Long v){latestFinanceRecordId=v;} public Long getLatestProofDocumentId(){return latestProofDocumentId;} public void setLatestProofDocumentId(Long v){latestProofDocumentId=v;}
        public Long getLatestProofSize(){return latestProofSize;} public void setLatestProofSize(Long v){latestProofSize=v;} public Long getOverdueDays(){return overdueDays;} public void setOverdueDays(Long v){overdueDays=v;}
        public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;} public String getTenantName(){return tenantName;} public void setTenantName(String v){tenantName=v;}
        public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;} public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;}
        public String getCollectionStatus(){return collectionStatus;} public void setCollectionStatus(String v){collectionStatus=v;} public String getLatestTransactionNo(){return latestTransactionNo;} public void setLatestTransactionNo(String v){latestTransactionNo=v;}
        public String getLatestPaymentMethod(){return latestPaymentMethod;} public void setLatestPaymentMethod(String v){latestPaymentMethod=v;} public String getLatestProofName(){return latestProofName;} public void setLatestProofName(String v){latestProofName=v;}
        public String getLatestProofMimeType(){return latestProofMimeType;} public void setLatestProofMimeType(String v){latestProofMimeType=v;} public LocalDate getBillingMonth(){return billingMonth;} public void setBillingMonth(LocalDate v){billingMonth=v;}
        public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){dueDate=v;} public LocalDate getLatestPaymentDate(){return latestPaymentDate;} public void setLatestPaymentDate(LocalDate v){latestPaymentDate=v;}
        public LocalDateTime getLatestConfirmedAt(){return latestConfirmedAt;} public void setLatestConfirmedAt(LocalDateTime v){latestConfirmedAt=v;} public BigDecimal getAmountDue(){return amountDue;} public void setAmountDue(BigDecimal v){amountDue=v;}
        public BigDecimal getAmountPaid(){return amountPaid;} public void setAmountPaid(BigDecimal v){amountPaid=v;} public BigDecimal getOutstandingAmount(){return outstandingAmount;} public void setOutstandingAmount(BigDecimal v){outstandingAmount=v;}
        public BigDecimal getLatestPaymentAmount(){return latestPaymentAmount;} public void setLatestPaymentAmount(BigDecimal v){latestPaymentAmount=v;}
    }
    class RentCollectionSummaryRow {
        private Long outstandingCount,unpaidCount,partialCount,overdueCount; private BigDecimal outstandingAmount,monthReceived;
        public Long getOutstandingCount(){return outstandingCount;} public void setOutstandingCount(Long v){outstandingCount=v;} public Long getUnpaidCount(){return unpaidCount;} public void setUnpaidCount(Long v){unpaidCount=v;}
        public Long getPartialCount(){return partialCount;} public void setPartialCount(Long v){partialCount=v;} public Long getOverdueCount(){return overdueCount;} public void setOverdueCount(Long v){overdueCount=v;}
        public BigDecimal getOutstandingAmount(){return outstandingAmount;} public void setOutstandingAmount(BigDecimal v){outstandingAmount=v;} public BigDecimal getMonthReceived(){return monthReceived;} public void setMonthReceived(BigDecimal v){monthReceived=v;}
    }
    class RentCollectionContext {
        private Long invoiceId,leaseId,tenantId,userId,unitId,ownerId; private String leaseNo,tenantName,unitNo,projectName;
        private BigDecimal amountDue,amountPaid;
        public Long getInvoiceId(){return invoiceId;} public void setInvoiceId(Long v){invoiceId=v;} public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;}
        public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
        public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;} public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;}
        public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;} public String getTenantName(){return tenantName;} public void setTenantName(String v){tenantName=v;}
        public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;}
        public BigDecimal getAmountDue(){return amountDue;} public void setAmountDue(BigDecimal v){amountDue=v;} public BigDecimal getAmountPaid(){return amountPaid;} public void setAmountPaid(BigDecimal v){amountPaid=v;}
    }
    class NewRentCollection {
        private Long id,unitId,ownerId,tenantId,actorId; private String transactionNo,paymentMethod; private LocalDate paymentDate; private BigDecimal amount;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;} public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;}
        public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public Long getActorId(){return actorId;} public void setActorId(Long v){actorId=v;}
        public String getTransactionNo(){return transactionNo;} public void setTransactionNo(String v){transactionNo=v;} public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;}
        public LocalDate getPaymentDate(){return paymentDate;} public void setPaymentDate(LocalDate v){paymentDate=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    }
}
