package com.ccps.backend.mapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.ccps.backend.dto.AdminTenancyOptionsResponse;
import com.ccps.backend.dto.AdminTenantDepositTransactionResponse;

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
            LEFT JOIN payment_receipts receipt ON receipt.finance_record_id = fr.id
            """;
    String LIST_FROM = """
            FROM tenants t
            LEFT JOIN leases l ON l.tenant_id = t.id
            LEFT JOIN units u ON u.id = l.unit_id
            LEFT JOIN rental_spaces rs ON rs.id = l.rental_space_id
            LEFT JOIN projects p ON p.id = u.project_id
            LEFT JOIN documents contract_doc ON contract_doc.id = l.contract_document_id
            LEFT JOIN owner_units primary_owner_unit ON primary_owner_unit.unit_id = u.id
              AND primary_owner_unit.is_primary = 1 AND primary_owner_unit.status = 'active'
            LEFT JOIN owners primary_owner ON primary_owner.id = primary_owner_unit.owner_id
              AND primary_owner.status = 'active'
            LEFT JOIN rent_invoices ri ON ri.id = (
              SELECT ri2.id FROM rent_invoices ri2
              WHERE ri2.lease_id = l.id
              <if test="startDate != null">AND ri2.billing_month &gt;= DATE_FORMAT(#{startDate}, '%Y-%m-01')</if>
              <if test="endDate != null">AND ri2.billing_month &lt;= DATE_FORMAT(#{endDate}, '%Y-%m-01')</if>
              <if test="status == 'paid'">AND ri2.amount_due &gt; 0 AND ri2.amount_paid &gt;= ri2.amount_due</if>
              <if test="status == 'partial'">AND ri2.amount_paid &gt; 0 AND ri2.amount_paid &lt; ri2.amount_due</if>
              <if test="status == 'overdue'">AND ri2.amount_paid &lt; ri2.amount_due AND CURRENT_DATE &gt; ri2.due_date</if>
              <if test="status == 'unpaid'">AND ri2.amount_paid = 0 AND CURRENT_DATE &lt;= ri2.due_date</if>
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

    String DEPOSIT_ACCOUNT_SELECT = """
            SELECT l.id AS lease_id,l.lease_no,l.tenant_id,t.full_name AS tenant_name,t.phone AS tenant_phone,
                   p.name AS project_name,u.unit_no,l.status AS lease_status,l.start_date,l.end_date,
                   COALESCE(l.deposit_amount,0) AS expected_deposit,
                   COALESCE((SELECT SUM(CASE WHEN tx.direction='credit' THEN tx.amount ELSE -tx.amount END)
                               FROM tenant_deposit_transactions tx
                              WHERE tx.lease_id=l.id AND tx.status='posted'),0) AS posted_balance,
                   COALESCE((SELECT SUM(CASE WHEN tx.direction='credit' THEN tx.amount ELSE -tx.amount END)
                               FROM tenant_deposit_transactions tx
                              WHERE tx.lease_id=l.id AND tx.status IN ('posted','pending')),0) AS available_balance,
                   COALESCE((SELECT COUNT(*) FROM tenant_deposit_transactions tx
                              WHERE tx.lease_id=l.id AND tx.status='pending'
                                AND tx.transaction_type IN ('refund','forfeiture')),0) AS pending_settlement_count,
                   sde.finance_record_id,fr.transaction_no,sde.amount AS bill_amount,fr.transaction_date AS bill_date,
                   COALESCE(sde.status,'unbilled') AS deposit_entry_status,
                   COALESCE(fr.confirmation_status,'unbilled') AS confirmation_status,
                   COALESCE(fr.payment_status,'unbilled') AS payment_status,
                   o.id AS owner_id,o.full_name AS owner_name,ra.id AS reserve_account_id,
                   COALESCE(ra.current_balance,0) AS reserve_balance
              FROM leases l
              JOIN tenants t ON t.id=l.tenant_id
              JOIN units u ON u.id=l.unit_id
              JOIN projects p ON p.id=u.project_id
              LEFT JOIN security_deposit_entries sde ON sde.id=(
                SELECT sde2.id FROM security_deposit_entries sde2
                 WHERE sde2.lease_id=l.id AND sde2.status<>'rejected'
                 ORDER BY (sde2.status='pending') DESC,sde2.id DESC LIMIT 1)
              LEFT JOIN finance_records fr ON fr.id=sde.finance_record_id
              LEFT JOIN owner_units ou ON ou.id=(SELECT ou2.id FROM owner_units ou2
                                                  WHERE ou2.unit_id=l.unit_id AND ou2.status='active'
                                                  ORDER BY ou2.is_primary DESC,ou2.id LIMIT 1)
              LEFT JOIN owners o ON o.id=ou.owner_id
              LEFT JOIN reserve_accounts ra ON ra.owner_unit_id=ou.id AND ra.status='active'
            """;

    @Select({
            "<script>",
            "SELECT t.id AS tenant_id, t.full_name AS tenant_name, t.identity_no, t.phone, t.email, t.status AS tenant_status,",
            "       l.id AS lease_id, l.lease_no, p.id AS project_id, p.name AS project_name, u.id AS unit_id, u.unit_no,",
            "       rs.id AS rental_space_id,rs.space_name AS rental_space_name,rs.space_type AS rental_space_type,",
            "       l.start_date AS lease_start, l.end_date AS lease_end, l.monthly_rent, l.deposit_amount, l.payment_day, l.rent_calculation_method,",
            "       l.status AS lease_status, l.contract_document_id, contract_doc.original_name AS contract_document_name,",
            "       contract_doc.mime_type AS contract_document_mime_type, contract_doc.file_size AS contract_document_size,",
            "       CASE WHEN (SELECT COUNT(DISTINCT signed_request.signer_role) FROM electronic_signature_requests signed_request WHERE COALESCE(signed_request.root_document_id,signed_request.source_document_id) = l.contract_document_id AND signed_request.entity_type = 'lease' AND signed_request.entity_id = l.id AND signed_request.status = 'signed') >= GREATEST(2, (SELECT COUNT(*) FROM electronic_signature_participants expected_signer WHERE expected_signer.root_document_id = l.contract_document_id AND expected_signer.document_kind = 'lease_contract')) THEN TRUE ELSE FALSE END AS contract_signed,",
            "       CASE WHEN l.contract_document_id IS NULL THEN 'missing'",
            "            WHEN (SELECT COUNT(DISTINCT signed_request.signer_role) FROM electronic_signature_requests signed_request WHERE COALESCE(signed_request.root_document_id,signed_request.source_document_id) = l.contract_document_id AND signed_request.entity_type = 'lease' AND signed_request.entity_id = l.id AND signed_request.status = 'signed') >= GREATEST(2, (SELECT COUNT(*) FROM electronic_signature_participants expected_signer WHERE expected_signer.root_document_id = l.contract_document_id AND expected_signer.document_kind = 'lease_contract')) THEN 'signed'",
            "            WHEN EXISTS (SELECT 1 FROM electronic_signature_requests pending_request WHERE pending_request.source_document_id = l.contract_document_id AND pending_request.entity_type = 'lease' AND pending_request.entity_id = l.id AND pending_request.status = 'pending') THEN 'pending'",
            "            ELSE 'uploaded' END AS contract_status,",
            "       ri.id AS invoice_id, ri.billing_month, ri.due_date,",
            "       COALESCE(ri.amount_due, 0) AS amount_due, COALESCE(ri.amount_paid, 0) AS amount_paid,",
            "       GREATEST(COALESCE(ri.amount_due, 0) - COALESCE(ri.amount_paid, 0), 0) AS amount_unpaid,",
            "       COALESCE((SELECT SUM(GREATEST(ri_total.amount_due - ri_total.amount_paid, 0)) FROM rent_invoices ri_total",
            "                 WHERE ri_total.lease_id = l.id AND ri_total.billing_month &lt;= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')), 0) AS total_unpaid,",
            "       CASE WHEN ri.id IS NULL THEN 'no_invoice' WHEN ri.amount_paid >= ri.amount_due THEN 'paid'",
            "            WHEN ri.amount_paid > 0 THEN 'partial' WHEN CURRENT_DATE &gt; ri.due_date THEN 'overdue' ELSE 'unpaid' END AS rent_status,",
            "       COALESCE((SELECT SUM(credit.remaining_amount) FROM lease_rent_credits credit",
            "                 WHERE credit.lease_id=l.id AND credit.status='available'),0) AS prepaid_rent_balance,",
            "       fr.id AS finance_record_id, fr.transaction_no, fr.confirmation_status, fr.payment_method,",
            "       fr.transaction_date AS payment_date, confirmer.display_name AS confirmed_by_name, fr.confirmed_at,",
            "       primary_owner.id AS owner_id, primary_owner.full_name AS owner_name,",
            "       COALESCE(NULLIF(TRIM(primary_owner.identity_no), ''), NULLIF(TRIM(primary_owner.passport_no), '')) AS owner_identity,",
            "       (SELECT JSON_UNQUOTE(JSON_EXTRACT(al.after_data, '$.note')) FROM audit_logs al",
            "        WHERE al.entity_type = 'finance_record' AND al.entity_id = fr.id ORDER BY al.id DESC LIMIT 1) AS review_note",
            LIST_FROM,
            "<where>",
            "  <if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ', t.full_name, t.phone, t.email, l.lease_no, p.name, u.unit_no,rs.space_name,rs.space_code) LIKE CONCAT('%', #{keyword}, '%')</if>",
            "  <if test=\"projectName != null and projectName != ''\">AND p.name = #{projectName}</if>",
            "  <if test=\"status == 'paid'\">AND ri.amount_due > 0 AND ri.amount_paid >= ri.amount_due</if>",
            "  <if test=\"status == 'partial'\">AND ri.amount_paid > 0 AND ri.amount_paid &lt; ri.amount_due</if>",
            "  <if test=\"status == 'overdue'\">AND ri.amount_paid &lt; ri.amount_due AND CURRENT_DATE &gt; ri.due_date</if>",
            "  <if test=\"status == 'unpaid'\">AND ri.amount_paid = 0 AND CURRENT_DATE &lt;= ri.due_date</if>",
            "  <if test=\"status == 'pending_review'\">AND fr.confirmation_status = 'pending'</if>",
            "</where>",
            "ORDER BY CASE WHEN fr.confirmation_status = 'pending' THEN 0 WHEN CURRENT_DATE &gt; ri.due_date AND ri.amount_paid &lt; ri.amount_due THEN 1 ELSE 2 END, CASE WHEN l.status = 'active' THEN 0 ELSE 1 END, t.full_name, l.end_date DESC, l.id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<TenancyRow> findPage(@Param("keyword") String keyword, @Param("projectName") String projectName,
            @Param("status") String status, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, @Param("limit") int limit, @Param("offset") int offset);

    @Select({
            "<script>", "SELECT COUNT(*)", LIST_FROM, "<where>",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ', t.full_name, t.phone, t.email, l.lease_no, p.name, u.unit_no,rs.space_name,rs.space_code) LIKE CONCAT('%', #{keyword}, '%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name = #{projectName}</if>",
            "<if test=\"status == 'paid'\">AND ri.amount_due > 0 AND ri.amount_paid >= ri.amount_due</if>",
            "<if test=\"status == 'partial'\">AND ri.amount_paid > 0 AND ri.amount_paid &lt; ri.amount_due</if>",
            "<if test=\"status == 'overdue'\">AND ri.amount_paid &lt; ri.amount_due AND CURRENT_DATE &gt; ri.due_date</if>",
            "<if test=\"status == 'unpaid'\">AND ri.amount_paid = 0 AND CURRENT_DATE &lt;= ri.due_date</if>",
            "<if test=\"status == 'pending_review'\">AND fr.confirmation_status = 'pending'</if>",
            "</where>", "</script>"
    })
    Long countPage(@Param("keyword") String keyword, @Param("projectName") String projectName,
            @Param("status") String status, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM tenants WHERE status = 'active') AS tenant_count,
              (SELECT COUNT(*) FROM leases WHERE status = 'active' AND CURRENT_DATE BETWEEN start_date AND end_date) AS active_lease_count,
              COALESCE(SUM(ri.amount_due), 0) AS current_due,
              COALESCE(SUM(ri.amount_paid), 0) AS current_paid,
              COALESCE(SUM(GREATEST(ri.amount_due - ri.amount_paid, 0)), 0) AS current_unpaid,
              (SELECT COALESCE(SUM(GREATEST(ri_all.amount_due - ri_all.amount_paid, 0)), 0) FROM rent_invoices ri_all
               WHERE ri_all.billing_month <= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')) AS total_unpaid,
              SUM(ri.amount_paid > 0 AND ri.amount_paid < ri.amount_due) AS partial_count,
              SUM(ri.amount_paid < ri.amount_due AND CURRENT_DATE > ri.due_date) AS overdue_count,
              (SELECT COUNT(DISTINCT rp.rent_invoice_id) FROM rent_payments rp
               JOIN finance_records fr ON fr.id = rp.finance_record_id
               WHERE fr.confirmation_status = 'pending') AS pending_review_count
            FROM rent_invoices ri
            JOIN leases l ON l.id = ri.lease_id
            WHERE ri.billing_month = DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
            """)
    SummaryRow findSummary();

    @Select("""
            SELECT ri.id AS invoice_id, ri.billing_month, ri.due_date, ri.amount_due, ri.amount_paid,
                   GREATEST(ri.amount_due - ri.amount_paid, 0) AS amount_unpaid,
                   CASE WHEN ri.amount_paid >= ri.amount_due THEN 'paid'
                        WHEN ri.amount_paid > 0 THEN 'partial'
                         WHEN CURRENT_DATE > ri.due_date THEN 'overdue' ELSE 'unpaid' END AS rent_status
            FROM rent_invoices ri JOIN leases l ON l.id = ri.lease_id
            WHERE ri.lease_id = #{leaseId}
              AND ri.billing_month <= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
            ORDER BY ri.billing_month DESC, ri.id DESC
            """)
    List<LeaseInvoiceRow> findLeaseInvoiceDetails(@Param("leaseId") Long leaseId);

    @Select("SELECT DISTINCT p.name FROM projects p JOIN units u ON u.project_id = p.id JOIN leases l ON l.unit_id = u.id ORDER BY p.name")
    List<String> findProjects();

    @Select("SELECT id, full_name AS name FROM tenants WHERE status = 'active' ORDER BY full_name")
    List<AdminTenancyOptionsResponse.Tenant> findTenantOptions();

    @Select("""
            SELECT DISTINCT u.id, p.name AS project_name, u.unit_no,u.rental_mode,
                   rm.start_date AS mandate_start_date, rm.end_date AS mandate_end_date
            FROM units u
            JOIN projects p ON p.id = u.project_id AND p.status = 'active'
            JOIN owner_units ou ON ou.unit_id = u.id AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
            JOIN rental_mandates rm ON rm.owner_unit_id = ou.id
              AND rm.status IN ('draft','pending_review','active','suspended')
              AND rm.start_date <= CURRENT_DATE
              AND (rm.end_date IS NULL OR rm.end_date >= CURRENT_DATE)
            ORDER BY p.name, u.unit_no
            """)
    List<AdminTenancyOptionsResponse.Unit> findAvailableUnits();

    @Select("""
            SELECT rs.id,rs.unit_id,rs.space_code,rs.space_name,rs.space_type,rs.status,
                   l.id AS current_lease_id,l.end_date AS current_lease_end
            FROM rental_spaces rs
            JOIN units u ON u.id=rs.unit_id
            JOIN owner_units ou ON ou.unit_id=u.id AND ou.status='active' AND ou.asset_stage='OPERATING'
            LEFT JOIN leases l ON l.id=(SELECT l2.id FROM leases l2 WHERE l2.rental_space_id=rs.id
              AND l2.status='active' AND CURRENT_DATE BETWEEN l2.start_date AND l2.end_date
              ORDER BY l2.id DESC LIMIT 1)
            WHERE rs.status='active'
            ORDER BY rs.unit_id,CASE rs.space_type WHEN 'whole_unit' THEN 0 ELSE 1 END,rs.sort_order,rs.id
            """)
    List<AdminTenancyOptionsResponse.RentalSpace> findRentalSpaceOptions();

    @Select({
            "<script>",
            "SELECT fr.id, fr.transaction_no, t.full_name AS tenant_name, p.name AS project_name, u.unit_no,",
            "       l.id AS lease_id, l.lease_no, ri.id AS invoice_id, ri.billing_month, ri.due_date,",
            "       ri.amount_due AS invoice_amount, ri.amount_paid AS invoice_paid, fr.amount, fr.currency,",
            "       fr.transaction_date, fr.payment_method, fr.confirmation_status, fr.sync_status, fr.allocation_note,",
            "       proof.id AS proof_document_id, proof.original_name AS proof_name, proof.mime_type AS proof_mime_type, proof.file_size AS proof_size, receipt.receipt_no,",
            "       (SELECT JSON_UNQUOTE(JSON_EXTRACT(al.after_data, '$.note')) FROM audit_logs al",
            "        WHERE al.entity_type = 'finance_record' AND al.entity_id = fr.id ORDER BY al.id DESC LIMIT 1) AS review_note,",
            "       confirmer.display_name AS confirmed_by_name, fr.confirmed_at, fr.created_at AS submitted_at",
            RENT_FINANCE_FROM,
            "<where>",
            "  fr.record_type = 'rent_payment'",
            "  <if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ', fr.transaction_no, t.full_name, p.name, u.unit_no, l.lease_no) LIKE CONCAT('%', #{keyword}, '%')</if>",
            "  <if test=\"projectName != null and projectName != ''\">AND p.name = #{projectName}</if>",
            "  <if test=\"billingMonth != null\">AND ri.billing_month = #{billingMonth}</if>",
            "  <if test=\"tenantName != null and tenantName != ''\">AND t.full_name LIKE CONCAT('%', #{tenantName}, '%')</if>",
            "  <if test=\"unitNo != null and unitNo != ''\">AND u.unit_no LIKE CONCAT('%', #{unitNo}, '%')</if>",
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
            @Param("proofStatus") String proofStatus, @Param("billingMonth") LocalDate billingMonth,
            @Param("tenantName") String tenantName, @Param("unitNo") String unitNo,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select({
            "<script>", "SELECT COUNT(*)", RENT_FINANCE_FROM, "<where>",
            "fr.record_type = 'rent_payment'",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ', fr.transaction_no, t.full_name, p.name, u.unit_no, l.lease_no) LIKE CONCAT('%', #{keyword}, '%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name = #{projectName}</if>",
            "<if test=\"billingMonth != null\">AND ri.billing_month = #{billingMonth}</if>",
            "<if test=\"tenantName != null and tenantName != ''\">AND t.full_name LIKE CONCAT('%', #{tenantName}, '%')</if>",
            "<if test=\"unitNo != null and unitNo != ''\">AND u.unit_no LIKE CONCAT('%', #{unitNo}, '%')</if>",
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
            @Param("proofStatus") String proofStatus, @Param("billingMonth") LocalDate billingMonth,
            @Param("tenantName") String tenantName, @Param("unitNo") String unitNo,
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
            "l.monthly_rent,l.start_date AS lease_start_date,l.end_date AS lease_end_date,l.rent_calculation_method,",
            "ri.billing_month,ri.due_date,ri.amount_due,ri.amount_paid,GREATEST(ri.amount_due-ri.amount_paid,0) AS outstanding_amount,",
            "CASE WHEN ri.amount_paid&gt;0 THEN 'partial' WHEN CURRENT_DATE &gt; ri.due_date THEN 'overdue' ELSE 'unpaid' END AS collection_status,",
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
            "<if test=\"status == 'overdue'\">AND CURRENT_DATE &gt; ri.due_date</if>",
            "<if test=\"startDate != null\">AND ri.due_date&gt;=#{startDate}</if>",
            "<if test=\"endDate != null\">AND ri.due_date&lt;=#{endDate}</if>",
            "<if test=\"invoiceId != null\">AND ri.id=#{invoiceId}</if>",
            "</where>",
            "ORDER BY (CURRENT_DATE &gt; ri.due_date) DESC,(ri.amount_paid&gt;0) DESC,ri.due_date,ri.id LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<RentCollectionRow> findRentCollections(@Param("keyword") String keyword,
            @Param("projectName") String projectName, @Param("status") String status,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            @Param("invoiceId") Long invoiceId,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select({"<script>","SELECT COUNT(*)",RENT_COLLECTION_FROM,"<where>",
            "ri.amount_paid &lt; ri.amount_due",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ',t.full_name,p.name,u.unit_no,l.lease_no) LIKE CONCAT('%',#{keyword},'%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name=#{projectName}</if>",
            "<if test=\"status == 'unpaid'\">AND ri.amount_paid=0</if>",
            "<if test=\"status == 'partial'\">AND ri.amount_paid&gt;0 AND ri.amount_paid&lt;ri.amount_due</if>",
            "<if test=\"status == 'overdue'\">AND CURRENT_DATE &gt; ri.due_date</if>",
            "<if test=\"startDate != null\">AND ri.due_date&gt;=#{startDate}</if>",
            "<if test=\"endDate != null\">AND ri.due_date&lt;=#{endDate}</if>",
            "<if test=\"invoiceId != null\">AND ri.id=#{invoiceId}</if>","</where>","</script>"})
    Long countRentCollections(@Param("keyword") String keyword, @Param("projectName") String projectName,
            @Param("status") String status, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, @Param("invoiceId") Long invoiceId);

    @Select("""
            SELECT COUNT(*) AS outstanding_count,
                   COALESCE(SUM(ri.amount_paid=0),0) AS unpaid_count,
                   COALESCE(SUM(ri.amount_paid>0 AND ri.amount_paid<ri.amount_due),0) AS partial_count,
                   COALESCE(SUM(CURRENT_DATE > ri.due_date),0) AS overdue_count,
                   COALESCE(SUM(GREATEST(ri.amount_due-ri.amount_paid,0)),0) AS outstanding_amount,
                   COALESCE((SELECT SUM(fr.amount) FROM finance_records fr
                     WHERE fr.record_type='rent_payment' AND fr.confirmation_status='confirmed'
                       AND fr.transaction_date>=DATE_FORMAT(CURRENT_DATE,'%Y-%m-01')),0) AS month_received
            FROM rent_invoices ri JOIN leases l ON l.id=ri.lease_id WHERE ri.amount_paid<ri.amount_due
            """)
    RentCollectionSummaryRow findRentCollectionSummary();

    @Select("SELECT DISTINCT p.name FROM rent_invoices ri JOIN leases l ON l.id=ri.lease_id JOIN units u ON u.id=l.unit_id JOIN projects p ON p.id=u.project_id WHERE ri.amount_paid<ri.amount_due ORDER BY p.name")
    List<String> findRentCollectionProjects();

    @Select("""
            SELECT ri.id AS invoice_id,ri.billing_month,ri.amount_due,ri.amount_paid,l.id AS lease_id,l.lease_no,
                   l.start_date,l.end_date,l.monthly_rent,l.payment_day,l.rent_calculation_method,
                   t.id AS tenant_id,t.user_id,t.full_name AS tenant_name,u.id AS unit_id,u.unit_no,p.name AS project_name,
                   (SELECT ou.owner_id FROM owner_units ou WHERE ou.unit_id=u.id AND ou.status='active' ORDER BY ou.id DESC LIMIT 1) AS owner_id
            FROM rent_invoices ri JOIN leases l ON l.id=ri.lease_id JOIN tenants t ON t.id=l.tenant_id
            JOIN units u ON u.id=l.unit_id JOIN projects p ON p.id=u.project_id
            WHERE ri.id=#{invoiceId} FOR UPDATE
            """)
    RentCollectionContext lockRentCollection(@Param("invoiceId") Long invoiceId);

    @Select("""
            SELECT id AS invoice_id,billing_month,amount_due,amount_paid
            FROM rent_invoices
            WHERE lease_id=#{leaseId} AND billing_month BETWEEN #{fromMonth} AND #{toMonth}
            ORDER BY billing_month,id FOR UPDATE
            """)
    List<RentInvoiceAdvanceRow> lockRentInvoicesForAdvance(@Param("leaseId") Long leaseId,
            @Param("fromMonth") LocalDate fromMonth, @Param("toMonth") LocalDate toMonth);

    @Insert("""
            INSERT INTO finance_records
              (transaction_no,record_type,unit_id,owner_id,tenant_id,amount,currency,transaction_date,receipt_date,payment_method,
               allocation_note,payment_status,confirmation_status,confirmed_by,confirmed_at,sync_status,created_by)
            VALUES (#{transactionNo},'rent_payment',#{unitId},#{ownerId},#{tenantId},#{amount},'MYR',#{postingDate},#{receivedDate},#{paymentMethod},#{allocationNote},
                    'paid','confirmed',#{actorId},NOW(),'pending',#{actorId})
            """)
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insertConfirmedRentPayment(NewRentCollection record);

    @Insert("INSERT INTO rent_payments (rent_invoice_id,finance_record_id,allocated_amount) VALUES (#{invoiceId},#{financeRecordId},#{allocatedAmount})")
    int linkRentPayment(@Param("invoiceId") Long invoiceId,@Param("financeRecordId") Long financeRecordId,
            @Param("allocatedAmount") BigDecimal allocatedAmount);

    @Insert("""
            INSERT INTO finance_records
              (transaction_no,record_type,unit_id,owner_id,tenant_id,amount,currency,transaction_date,
               payment_method,payment_status,confirmation_status,sync_status,created_by)
            VALUES
              (#{transactionNo},'security_deposit',#{unitId},
               (SELECT ou.owner_id FROM owner_units ou WHERE ou.unit_id=#{unitId} AND ou.status='active'
                ORDER BY ou.is_primary DESC,ou.id LIMIT 1),
               #{tenantId},#{amount},'MYR',#{transactionDate},'internal_accrual','unpaid','pending','not_synced',NULL)
            """)
    @Options(useGeneratedKeys=true,keyProperty="financeRecordId")
    int insertSecurityDepositFinance(NewSecurityDeposit record);

    @Insert("INSERT INTO security_deposit_entries (lease_id,finance_record_id,amount,status) VALUES (#{leaseId},#{financeRecordId},#{amount},'pending')")
    int insertSecurityDepositEntry(NewSecurityDeposit record);

    @Insert("""
            INSERT INTO cashflow_entries
              (finance_record_id,unit_id,lease_id,owner_id,tenant_id,direction,category,description,occurred_on,attachment_status)
            VALUES
              (#{financeRecordId},#{unitId},#{leaseId},
               (SELECT ou.owner_id FROM owner_units ou WHERE ou.unit_id=#{unitId} AND ou.status='active'
                ORDER BY ou.is_primary DESC,ou.id LIMIT 1),
               #{tenantId},'income','deposit',#{description},#{transactionDate},'not_required')
            """)
    int insertSecurityDepositCashflow(NewSecurityDeposit record);

    @Update("""
            UPDATE finance_records fr
            JOIN security_deposit_entries sde ON sde.finance_record_id=fr.id
            SET fr.amount=#{amount},fr.unit_id=#{unitId},fr.tenant_id=#{tenantId},
                fr.owner_id=(SELECT ou.owner_id FROM owner_units ou WHERE ou.unit_id=#{unitId} AND ou.status='active'
                             ORDER BY ou.is_primary DESC,ou.id LIMIT 1),
                fr.transaction_date=#{transactionDate}
            WHERE sde.lease_id=#{leaseId} AND sde.status='pending' AND fr.confirmation_status='pending'
            """)
    int updatePendingSecurityDepositFinance(@Param("leaseId") Long leaseId,@Param("unitId") Long unitId,
            @Param("tenantId") Long tenantId,@Param("amount") BigDecimal amount,@Param("transactionDate") LocalDate transactionDate);

    @Update("""
            UPDATE cashflow_entries ce
            JOIN security_deposit_entries sde ON sde.finance_record_id=ce.finance_record_id
            SET ce.unit_id=#{unitId},ce.lease_id=#{leaseId},ce.tenant_id=#{tenantId},ce.owner_id=(SELECT ou.owner_id FROM owner_units ou WHERE ou.unit_id=#{unitId} AND ou.status='active'
                                                   ORDER BY ou.is_primary DESC,ou.id LIMIT 1),
                ce.description=#{description},ce.occurred_on=#{transactionDate}
            WHERE sde.lease_id=#{leaseId} AND sde.status='pending'
            """)
    int updatePendingSecurityDepositCashflow(@Param("leaseId") Long leaseId,@Param("unitId") Long unitId,
            @Param("tenantId") Long tenantId,@Param("description") String description,@Param("transactionDate") LocalDate transactionDate);

    @Update("""
            UPDATE finance_records fr
            JOIN security_deposit_entries sde ON sde.finance_record_id=fr.id
            SET fr.payment_status='voided',fr.confirmation_status='rejected',fr.sync_status='not_synced'
            WHERE sde.lease_id=#{leaseId} AND sde.status='pending' AND fr.confirmation_status='pending'
            """)
    int voidPendingSecurityDeposit(@Param("leaseId") Long leaseId);

    @Update("UPDATE security_deposit_entries SET status='rejected' WHERE lease_id=#{leaseId} AND status='pending'")
    int rejectPendingSecurityDepositEntry(@Param("leaseId") Long leaseId);

    @Select("""
            SELECT COALESCE(SUM(CASE WHEN direction='credit' THEN amount ELSE -amount END),0)
            FROM tenant_deposit_transactions
            WHERE lease_id=#{leaseId} AND status IN ('posted','pending')
            """)
    BigDecimal findLeaseDepositBalance(@Param("leaseId") Long leaseId);

    @Select({ DEPOSIT_ACCOUNT_SELECT,
            "ORDER BY CASE WHEN sde.status='pending' THEN 0 WHEN l.status='active' THEN 1 ELSE 2 END,l.end_date DESC,l.id DESC" })
    List<DepositAccountRow> findDepositAccounts();

    @Select({ DEPOSIT_ACCOUNT_SELECT, "WHERE l.id=#{leaseId}" })
    DepositAccountRow findDepositAccount(@Param("leaseId") Long leaseId);

    @Select("""
            SELECT sde.finance_record_id,fr.transaction_no,sde.amount,fr.transaction_date AS bill_date,
                   sde.status AS entry_status,fr.confirmation_status,fr.payment_status
              FROM security_deposit_entries sde
              JOIN finance_records fr ON fr.id=sde.finance_record_id
             WHERE sde.lease_id=#{leaseId}
             ORDER BY (sde.status='pending') DESC,fr.transaction_date DESC,sde.id DESC
            """)
    List<com.ccps.backend.dto.AdminDepositAccountDetailResponse.Bill> findDepositBills(@Param("leaseId") Long leaseId);

    @Select("""
            SELECT tdt.id,tdt.lease_id,l.lease_no,tdt.tenant_id,tdt.unit_id,p.name AS project_name,u.unit_no,
                   tdt.finance_record_id,tdt.transaction_type,tdt.direction,tdt.amount,
                   (SELECT COALESCE(SUM(CASE WHEN prior.direction='credit' THEN prior.amount ELSE -prior.amount END),0)
                      FROM tenant_deposit_transactions prior
                     WHERE prior.lease_id=tdt.lease_id AND prior.status IN ('posted','pending')
                       AND (prior.occurred_on<tdt.occurred_on
                         OR (prior.occurred_on=tdt.occurred_on AND prior.id<=tdt.id))) AS balance_after,
                   tdt.occurred_on,tdt.description,tdt.status
              FROM tenant_deposit_transactions tdt
              JOIN leases l ON l.id=tdt.lease_id
              JOIN units u ON u.id=tdt.unit_id
              JOIN projects p ON p.id=u.project_id
             WHERE tdt.lease_id=#{leaseId} AND tdt.status IN ('posted','pending')
             ORDER BY tdt.occurred_on DESC,tdt.id DESC
            """)
    List<AdminTenantDepositTransactionResponse> findLeaseDepositTransactions(@Param("leaseId") Long leaseId);

    @Select("""
            SELECT tdt.id,tdt.lease_id,l.lease_no,tdt.tenant_id,tdt.unit_id,p.name AS project_name,u.unit_no,
                   tdt.finance_record_id,tdt.transaction_type,tdt.direction,tdt.amount,
                   (SELECT COALESCE(SUM(CASE WHEN prior.direction='credit' THEN prior.amount ELSE -prior.amount END),0)
                      FROM tenant_deposit_transactions prior
                     WHERE prior.tenant_id = tdt.tenant_id
                       AND prior.status IN ('posted','pending')
                       AND (prior.occurred_on < tdt.occurred_on
                         OR (prior.occurred_on = tdt.occurred_on AND prior.id <= tdt.id))) AS balance_after,
                   tdt.occurred_on,tdt.description,tdt.status
            FROM tenant_deposit_transactions tdt
            JOIN leases l ON l.id=tdt.lease_id
            JOIN units u ON u.id=tdt.unit_id
            JOIN projects p ON p.id=u.project_id
            WHERE tdt.tenant_id=#{tenantId} AND tdt.status IN ('posted','pending')
            ORDER BY tdt.occurred_on DESC,tdt.id DESC
            """)
    List<AdminTenantDepositTransactionResponse> findTenantDepositTransactions(@Param("tenantId") Long tenantId);

    @Insert("""
            INSERT INTO tenant_deposit_transactions
              (lease_id,tenant_id,unit_id,finance_record_id,transaction_type,direction,amount,occurred_on,
               description,status,created_by)
            VALUES (#{leaseId},#{tenantId},#{unitId},#{financeRecordId},#{transactionType},#{direction},
                    #{amount},#{occurredOn},#{description},#{status},#{createdBy})
            """)
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insertTenantDepositTransaction(NewTenantDepositTransaction transaction);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,after_data)
            VALUES (#{actorId},'create_tenant_deposit_transaction','tenant_deposit_transaction',#{transactionId},
              JSON_OBJECT('leaseId',#{leaseId},'transactionType',#{transactionType},'direction',#{direction},
                          'amount',#{amount},'financeRecordId',#{financeRecordId},'description',#{description}))
            """)
    int insertTenantDepositAudit(@Param("actorId") Long actorId,@Param("transactionId") Long transactionId,
            @Param("leaseId") Long leaseId,@Param("transactionType") String transactionType,
            @Param("direction") String direction,@Param("amount") BigDecimal amount,
            @Param("financeRecordId") Long financeRecordId,@Param("description") String description);

    @Update({
            "<script>",
            "UPDATE tenant_deposit_transactions",
            "SET status='cancelled'",
            "WHERE lease_id=#{leaseId} AND status='posted' AND finance_record_id IS NULL",
            "  AND transaction_type IN ('adjustment_credit','adjustment_debit','tenant_advance','tenant_repayment')",
            "  AND id IN",
            "<foreach collection='transactionIds' item='transactionId' open='(' separator=',' close=')'>#{transactionId}</foreach>",
            "</script>"
    })
    int cancelTenantDepositTransactions(@Param("leaseId") Long leaseId,
            @Param("transactionIds") List<Long> transactionIds);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,before_data,after_data)
            VALUES (#{actorId},'delete_tenant_deposit_transaction','tenant_deposit_transaction',#{transactionId},
              JSON_OBJECT('leaseId',#{leaseId},'transactionType',#{transactionType},'direction',#{direction},
                          'amount',#{amount},'status','posted'),
              JSON_OBJECT('status','cancelled'))
            """)
    int insertTenantDepositDeleteAudit(@Param("actorId") Long actorId,@Param("transactionId") Long transactionId,
            @Param("leaseId") Long leaseId,@Param("transactionType") String transactionType,
            @Param("direction") String direction,@Param("amount") BigDecimal amount);

    @Insert("""
            INSERT INTO finance_records
              (transaction_no,record_type,unit_id,owner_id,tenant_id,amount,currency,transaction_date,
               payment_method,payment_status,confirmation_status,sync_status,created_by)
            VALUES (#{transactionNo},'reserve_refund',#{unitId},
              (SELECT ou.owner_id FROM owner_units ou WHERE ou.unit_id=#{unitId} AND ou.status='active'
               ORDER BY ou.is_primary DESC,ou.id LIMIT 1),#{tenantId},#{amount},'MYR',#{occurredOn},
              'security_deposit','unpaid','pending','not_synced',#{createdBy})
            """)
    @Options(useGeneratedKeys=true,keyProperty="financeRecordId")
    int insertTenantDepositRefundFinance(NewTenantDepositTransaction transaction);

    @Insert("""
            INSERT INTO cashflow_entries
              (finance_record_id,unit_id,lease_id,owner_id,tenant_id,direction,category,description,
               occurred_on,attachment_status)
            VALUES (#{financeRecordId},#{unitId},#{leaseId},
              (SELECT ou.owner_id FROM owner_units ou WHERE ou.unit_id=#{unitId} AND ou.status='active'
               ORDER BY ou.is_primary DESC,ou.id LIMIT 1),#{tenantId},'expense','deposit_refund',
              #{description},#{occurredOn},'not_required')
            """)
    int insertTenantDepositRefundCashflow(NewTenantDepositTransaction transaction);

    @Insert("""
            INSERT INTO finance_records
              (transaction_no,record_type,unit_id,owner_id,tenant_id,amount,currency,transaction_date,
               payment_method,payment_status,confirmation_status,sync_status,created_by)
            VALUES (#{transactionNo},'security_deposit_forfeiture',#{unitId},
              (SELECT ou.owner_id FROM owner_units ou WHERE ou.unit_id=#{unitId} AND ou.status='active'
               ORDER BY ou.is_primary DESC,ou.id LIMIT 1),#{tenantId},#{amount},'MYR',#{occurredOn},
              'security_deposit','unpaid','pending','not_synced',#{createdBy})
            """)
    @Options(useGeneratedKeys=true,keyProperty="financeRecordId")
    int insertTenantDepositForfeitureFinance(NewTenantDepositTransaction transaction);

    @Insert("""
            INSERT INTO cashflow_entries
              (finance_record_id,unit_id,lease_id,owner_id,tenant_id,direction,category,description,
               occurred_on,attachment_status)
            VALUES (#{financeRecordId},#{unitId},#{leaseId},
              (SELECT ou.owner_id FROM owner_units ou WHERE ou.unit_id=#{unitId} AND ou.status='active'
               ORDER BY ou.is_primary DESC,ou.id LIMIT 1),#{tenantId},'income','deposit_forfeiture',
              #{description},#{occurredOn},'not_required')
            """)
    int insertTenantDepositForfeitureCashflow(NewTenantDepositTransaction transaction);

    @Insert("INSERT INTO cashflow_entries (finance_record_id,unit_id,owner_id,tenant_id,direction,category,description,occurred_on,attachment_status) VALUES (#{financeRecordId},#{unitId},#{ownerId},#{tenantId},'income','rent',#{description},#{occurredOn},'missing')")
    int insertRentCashflow(@Param("financeRecordId") Long financeRecordId,@Param("unitId") Long unitId,@Param("ownerId") Long ownerId,@Param("tenantId") Long tenantId,@Param("description") String description,@Param("occurredOn") LocalDate occurredOn);

    @Update("UPDATE cashflow_entries SET allocation_note=#{allocationNote} WHERE finance_record_id=#{financeRecordId}")
    int updateRentCashflowAllocationNote(@Param("financeRecordId") Long financeRecordId,@Param("allocationNote") String allocationNote);

    @Select("SELECT note FROM finance_allocation_note_defaults WHERE unit_id=#{unitId} AND record_type='rent_payment' LIMIT 1")
    String findRentAllocationNoteDefault(@Param("unitId") Long unitId);

    @Insert("INSERT INTO finance_allocation_note_defaults(unit_id,record_type,note,created_by,updated_by) VALUES (#{unitId},'rent_payment',#{note},#{actorId},#{actorId}) ON DUPLICATE KEY UPDATE note=VALUES(note),updated_by=VALUES(updated_by),updated_at=CURRENT_TIMESTAMP")
    int upsertRentAllocationNoteDefault(@Param("unitId") Long unitId,@Param("note") String note,@Param("actorId") Long actorId);

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
            INSERT INTO lease_rent_credits (lease_id,finance_record_id,received_amount,allocated_amount,remaining_amount,status,created_by)
            VALUES (#{leaseId},#{financeRecordId},#{receivedAmount},0,#{remainingAmount},'available',#{actorId})
            """)
    @Options(useGeneratedKeys=true,keyProperty="id")
    int insertRentCredit(NewRentCredit credit);

    @Select("SELECT id FROM lease_rent_credits WHERE status='available' AND remaining_amount>0 ORDER BY lease_id")
    List<Long> findLeaseIdsWithAvailableRentCredit();

    @Select("""
            SELECT id,remaining_amount FROM lease_rent_credits
            WHERE lease_id=#{leaseId} AND status='available' AND remaining_amount>0
            ORDER BY created_at,id FOR UPDATE
            """)
    List<RentCreditRow> lockAvailableRentCredits(@Param("leaseId") Long leaseId);

    @Select("""
            SELECT id AS invoice_id,amount_due,amount_paid FROM rent_invoices
            WHERE lease_id=#{leaseId} AND amount_paid<amount_due
            ORDER BY billing_month,id FOR UPDATE
            """)
    List<RentInvoiceCreditRow> lockOutstandingInvoicesForCredit(@Param("leaseId") Long leaseId);

    @Update("""
            UPDATE lease_rent_credits SET allocated_amount=allocated_amount+#{amount},remaining_amount=remaining_amount-#{amount},
              status=CASE WHEN remaining_amount-#{amount}<=0 THEN 'allocated' ELSE 'available' END
            WHERE id=#{creditId} AND status='available' AND remaining_amount>=#{amount}
            """)
    int consumeRentCredit(@Param("creditId") Long creditId,@Param("amount") BigDecimal amount);

    @Insert("""
            INSERT INTO lease_rent_credit_allocations (rent_credit_id,rent_invoice_id,amount,allocated_by)
            VALUES (#{creditId},#{invoiceId},#{amount},#{actorId})
            """)
    int insertRentCreditAllocation(@Param("creditId") Long creditId,@Param("invoiceId") Long invoiceId,
            @Param("amount") BigDecimal amount,@Param("actorId") Long actorId);

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

    @Select("SELECT COUNT(*) FROM tenants WHERE identity_no = #{value} AND id <> #{tenantId}")
    int countTenantIdentityExcluding(@Param("value") String value,@Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM tenants WHERE id=#{tenantId}")
    int countTenant(@Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(*) FROM leases WHERE tenant_id = #{tenantId}")
    int countTenantLeases(@Param("tenantId") Long tenantId);

    @Delete("DELETE FROM tenants WHERE id = #{tenantId}")
    int deleteTenant(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT t.id AS tenant_id, t.full_name, t.identity_no, t.phone, t.email, t.status,
              COALESCE((SELECT ws.enabled FROM tenant_whatsapp_subscriptions ws WHERE ws.tenant_id=t.id), 0) AS whatsapp_enabled,
              (SELECT ws.destination FROM tenant_whatsapp_subscriptions ws WHERE ws.tenant_id=t.id) AS whatsapp_destination,
              (SELECT ws.opted_in_at FROM tenant_whatsapp_subscriptions ws WHERE ws.tenant_id=t.id) AS whatsapp_opted_in_at,
              (SELECT l.lease_no FROM leases l WHERE l.tenant_id=t.id AND l.status='active'
                 AND l.start_date <= CURRENT_DATE() AND l.end_date >= CURRENT_DATE()
               ORDER BY l.start_date DESC, l.id DESC LIMIT 1) AS current_lease_no,
              (SELECT p.name FROM leases l JOIN units u ON u.id=l.unit_id JOIN projects p ON p.id=u.project_id
               WHERE l.tenant_id=t.id AND l.status='active' AND l.start_date <= CURRENT_DATE() AND l.end_date >= CURRENT_DATE()
               ORDER BY l.start_date DESC, l.id DESC LIMIT 1) AS project_name,
              (SELECT u.unit_no FROM leases l JOIN units u ON u.id=l.unit_id
               WHERE l.tenant_id=t.id AND l.status='active' AND l.start_date <= CURRENT_DATE() AND l.end_date >= CURRENT_DATE()
               ORDER BY l.start_date DESC, l.id DESC LIMIT 1) AS unit_no,
              (SELECT l.start_date FROM leases l WHERE l.tenant_id=t.id AND l.status='active'
               AND l.start_date <= CURRENT_DATE() AND l.end_date >= CURRENT_DATE() ORDER BY l.start_date DESC, l.id DESC LIMIT 1) AS lease_start,
              (SELECT l.end_date FROM leases l WHERE l.tenant_id=t.id AND l.status='active'
               AND l.start_date <= CURRENT_DATE() AND l.end_date >= CURRENT_DATE() ORDER BY l.start_date DESC, l.id DESC LIMIT 1) AS lease_end,
              (SELECT l.deposit_amount FROM leases l WHERE l.tenant_id=t.id AND l.status='active'
                 AND l.start_date <= CURRENT_DATE() AND l.end_date >= CURRENT_DATE()
               ORDER BY l.start_date DESC, l.id DESC LIMIT 1) AS current_deposit_amount,
              COALESCE((SELECT SUM(CASE WHEN tdt.direction='credit' THEN tdt.amount ELSE -tdt.amount END)
                FROM tenant_deposit_transactions tdt
               WHERE tdt.lease_id = (SELECT active_lease.id FROM leases active_lease
                       WHERE active_lease.tenant_id=t.id AND active_lease.status='active'
                         AND active_lease.start_date <= CURRENT_DATE() AND active_lease.end_date >= CURRENT_DATE()
                       ORDER BY active_lease.start_date DESC, active_lease.id DESC LIMIT 1)
                 AND tdt.status IN ('posted','pending')), 0) AS current_deposit_balance,
              (SELECT COALESCE(fr.confirmation_status,sde.status) FROM leases l
                 LEFT JOIN security_deposit_entries sde ON sde.lease_id=l.id
                 LEFT JOIN finance_records fr ON fr.id=sde.finance_record_id
               WHERE l.tenant_id=t.id AND l.status='active'
                 AND l.start_date <= CURRENT_DATE() AND l.end_date >= CURRENT_DATE()
               ORDER BY l.start_date DESC, l.id DESC LIMIT 1) AS current_deposit_status,
              (SELECT COUNT(*) FROM leases l WHERE l.tenant_id=t.id) AS lease_count,
              (SELECT COUNT(*) FROM leases l WHERE l.tenant_id=t.id AND l.status='active'
                 AND l.start_date <= CURRENT_DATE() AND l.end_date >= CURRENT_DATE()) AS active_lease_count
            FROM tenants t
            WHERE (#{status} IS NULL OR #{status} = '' OR t.status = #{status})
              AND (#{keyword} IS NULL OR #{keyword} = ''
                OR t.full_name LIKE CONCAT('%', #{keyword}, '%')
                OR t.identity_no LIKE CONCAT('%', #{keyword}, '%')
                OR t.phone LIKE CONCAT('%', #{keyword}, '%')
                OR t.email LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY t.status='active' DESC, t.full_name ASC, t.id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<TenantDirectoryRow> findTenantDirectoryPage(@Param("keyword") String keyword, @Param("status") String status,
            @Param("limit") int limit, @Param("offset") int offset);

    @Select("""
            SELECT COUNT(*) FROM tenants t
            WHERE (#{status} IS NULL OR #{status} = '' OR t.status = #{status})
              AND (#{keyword} IS NULL OR #{keyword} = ''
                OR t.full_name LIKE CONCAT('%', #{keyword}, '%')
                OR t.identity_no LIKE CONCAT('%', #{keyword}, '%')
                OR t.phone LIKE CONCAT('%', #{keyword}, '%')
                OR t.email LIKE CONCAT('%', #{keyword}, '%'))
            """)
    long countTenantDirectory(@Param("keyword") String keyword, @Param("status") String status);

    @Select("""
            SELECT COUNT(*) AS total_count,
              COALESCE(SUM(CASE WHEN t.status='active' THEN 1 ELSE 0 END), 0) AS active_count,
              COALESCE(SUM(CASE WHEN t.status='inactive' THEN 1 ELSE 0 END), 0) AS inactive_count,
              COALESCE(SUM(CASE WHEN EXISTS(SELECT 1 FROM leases l WHERE l.tenant_id=t.id AND l.status='active'
                  AND l.start_date <= CURRENT_DATE() AND l.end_date >= CURRENT_DATE()) THEN 1 ELSE 0 END), 0) AS active_lease_tenant_count
            FROM tenants t
            """)
    TenantDirectorySummaryRow findTenantDirectorySummary();

    @Select("""
            SELECT l.id AS lease_id, u.id AS unit_id, l.lease_no, p.name AS project_name, u.unit_no, l.status,
                   l.start_date, l.end_date, l.monthly_rent, l.deposit_amount, l.payment_day,
                   COALESCE((SELECT SUM(GREATEST(ri.amount_due-ri.amount_paid,0)) FROM rent_invoices ri WHERE ri.lease_id=l.id),0) AS unpaid_rent,
                   (SELECT COUNT(*) FROM maintenance_work_orders m WHERE m.lease_id=l.id AND m.status NOT IN ('completed','cancelled')) AS pending_maintenance_count,
                   CASE WHEN l.contract_document_id IS NULL THEN 'not_generated'
                        WHEN (SELECT COUNT(DISTINCT sr.signer_role) FROM electronic_signature_requests sr WHERE sr.entity_type='lease' AND sr.entity_id=l.id AND COALESCE(sr.root_document_id,sr.source_document_id)=l.contract_document_id AND sr.status='signed') >= GREATEST(2, (SELECT COUNT(*) FROM electronic_signature_participants expected_signer WHERE expected_signer.root_document_id=l.contract_document_id AND expected_signer.document_kind='lease_contract')) THEN 'signed'
                        WHEN EXISTS(SELECT 1 FROM electronic_signature_requests sr WHERE sr.entity_type='lease' AND sr.entity_id=l.id AND sr.status='pending') THEN 'pending_signature'
                        ELSE 'ready_to_sign' END AS signature_status,
                   CASE WHEN l.status = 'expired' THEN 'completed'
                        WHEN l.status <> 'active' THEN 'closed'
                        WHEN l.contract_document_id IS NULL THEN 'contract_pending'
                        WHEN EXISTS(SELECT 1 FROM electronic_signature_requests sr WHERE sr.entity_type='lease' AND sr.entity_id=l.id AND sr.status='pending') THEN 'signature_pending'
                        WHEN l.end_date < CURRENT_DATE() THEN 'completed'
                        WHEN l.start_date > CURRENT_DATE() THEN 'ready_to_start'
                        ELSE 'active_tenancy' END AS workflow_step
            FROM leases l
            JOIN units u ON u.id = l.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE l.tenant_id = #{tenantId}
            ORDER BY l.start_date DESC, l.id DESC
            """)
    List<TenantLeaseHistoryRow> findTenantLeaseHistory(@Param("tenantId") Long tenantId);

    @Select("""
            SELECT
              COALESCE((SELECT SUM(GREATEST(ri.amount_due-ri.amount_paid,0)) FROM rent_invoices ri
                JOIN leases l ON l.id=ri.lease_id WHERE l.tenant_id=#{tenantId}),0) AS unpaid_rent,
              (SELECT COUNT(*) FROM maintenance_work_orders m WHERE m.tenant_id=#{tenantId}
                AND m.status NOT IN ('completed','cancelled')) AS pending_maintenance_count,
              (SELECT COUNT(*) FROM electronic_signature_requests sr JOIN leases l ON l.id=sr.entity_id
                WHERE sr.entity_type='lease' AND l.tenant_id=#{tenantId} AND sr.status='pending') AS pending_signature_count
            """)
    TenantDirectoryOverviewRow findTenantDirectoryOverview(@Param("tenantId") Long tenantId);

    @Update("UPDATE tenants SET full_name=#{fullName},identity_no=#{identityNo},phone=#{phone},email=#{email},status=#{status} WHERE id=#{id}")
    int updateTenant(NewTenant tenant);

    @Update("UPDATE tenants SET status=#{status} WHERE id=#{tenantId}")
    int updateTenantStatus(@Param("tenantId") Long tenantId, @Param("status") String status);

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
            SELECT rs.id,rs.unit_id,rs.space_type,rs.status,u.rental_mode
            FROM rental_spaces rs JOIN units u ON u.id=rs.unit_id
            WHERE rs.id=#{spaceId}
            """)
    RentalSpaceContext findRentalSpace(@Param("spaceId") Long spaceId);

    @Select("SELECT id FROM rental_spaces WHERE unit_id=#{unitId} AND space_type='whole_unit' LIMIT 1")
    Long findWholeRentalSpaceId(@Param("unitId") Long unitId);

    @Select("""
            SELECT COUNT(*) FROM leases l
            WHERE l.unit_id=#{unitId} AND l.status='active' AND l.id<>COALESCE(#{excludeLeaseId},0)
              AND l.start_date<=#{endDate} AND l.end_date>=#{startDate}
            """)
    int countAnySpaceOverlap(@Param("excludeLeaseId") Long excludeLeaseId,@Param("unitId") Long unitId,
            @Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COUNT(*) FROM leases l JOIN rental_spaces rs ON rs.id=l.rental_space_id
            WHERE l.unit_id=#{unitId} AND l.status='active' AND l.id<>COALESCE(#{excludeLeaseId},0)
              AND l.start_date<=#{endDate} AND l.end_date>=#{startDate}
              AND (l.rental_space_id=#{spaceId} OR rs.space_type='whole_unit')
            """)
    int countRoomOverlap(@Param("excludeLeaseId") Long excludeLeaseId,@Param("unitId") Long unitId,
            @Param("spaceId") Long spaceId,@Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COUNT(*)
            FROM rental_mandates rm
            JOIN owner_units ou ON ou.id=rm.owner_unit_id
            WHERE ou.unit_id=#{unitId}
              AND rm.status IN ('draft','pending_review','active','suspended')
              AND rm.start_date<=#{startDate} AND (rm.end_date IS NULL OR rm.end_date>=#{endDate})
            """)
    int countActiveRentalMandate(@Param("unitId") Long unitId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COUNT(*)
            FROM rental_mandates rm
            JOIN owner_units ou ON ou.id = rm.owner_unit_id
            WHERE rm.id = #{mandateId} AND ou.unit_id = #{unitId}
              AND rm.status IN ('draft','pending_review','active','suspended')
              AND rm.start_date <= #{startDate} AND (rm.end_date IS NULL OR rm.end_date >= #{endDate})
            """)
    int countCurrentRentalMandate(@Param("mandateId") Long mandateId, @Param("unitId") Long unitId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COUNT(*)
            FROM leases l
            JOIN owner_units ou ON ou.unit_id=l.unit_id AND ou.status='active'
            JOIN rental_mandates rm ON rm.owner_unit_id=ou.id AND rm.status='active'
              AND rm.start_date<=l.start_date AND (rm.end_date IS NULL OR rm.end_date>=l.end_date)
            WHERE l.id=#{leaseId}
            """)
    int countLeaseRentalMandate(@Param("leaseId") Long leaseId);

    @Select("""
            SELECT l.id AS lease_id,l.unit_id,l.rental_space_id,l.tenant_id,l.rental_mandate_id,l.lease_no,l.start_date,l.end_date,
                   l.monthly_rent,l.deposit_amount,l.payment_day,l.rent_calculation_method,l.status,p.name AS project_name,u.unit_no,
                   rs.space_name AS rental_space_name,rs.space_type AS rental_space_type
            FROM leases l JOIN units u ON u.id=l.unit_id JOIN projects p ON p.id=u.project_id
            JOIN rental_spaces rs ON rs.id=l.rental_space_id
            WHERE l.id=#{leaseId} FOR UPDATE
            """)
    LeaseChangeContext lockLeaseForChange(@Param("leaseId") Long leaseId);

    @Select("SELECT COUNT(*) FROM leases WHERE id=#{leaseId}")
    int countLeaseById(@Param("leaseId") Long leaseId);

    @Select("SELECT rental_mandate_id FROM leases WHERE id=#{leaseId}")
    Long findRentalMandateIdByLease(@Param("leaseId") Long leaseId);

    @Select("""
            SELECT COUNT(*) FROM leases
            WHERE unit_id=#{unitId} AND id<>#{leaseId} AND status='active'
              AND start_date<=#{endDate} AND end_date>=#{startDate}
            """)
    int countOtherOverlappingLease(@Param("leaseId") Long leaseId,@Param("unitId") Long unitId,
            @Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate);

    @Update("""
            UPDATE leases SET start_date=#{startDate},end_date=#{endDate},monthly_rent=#{monthlyRent},
              deposit_amount=#{depositAmount},payment_day=#{paymentDay},rent_calculation_method=#{rentCalculationMethod}
            WHERE id=#{leaseId} AND status='active'
            """)
    int updateLeaseTerms(@Param("leaseId") Long leaseId,@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,@Param("monthlyRent") BigDecimal monthlyRent,
            @Param("depositAmount") BigDecimal depositAmount,@Param("paymentDay") int paymentDay,
            @Param("rentCalculationMethod") String rentCalculationMethod);

    @Update("""
            UPDATE leases SET tenant_id=#{tenantId},unit_id=#{unitId},rental_space_id=#{rentalSpaceId},start_date=#{startDate},end_date=#{endDate},monthly_rent=#{monthlyRent},
              deposit_amount=#{depositAmount},payment_day=#{paymentDay},rent_calculation_method=#{rentCalculationMethod}
            WHERE id=#{leaseId} AND status='active'
            """)
    int updateLeasePartiesAndTerms(@Param("leaseId") Long leaseId,@Param("tenantId") Long tenantId,
            @Param("unitId") Long unitId,@Param("rentalSpaceId") Long rentalSpaceId,@Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate,
            @Param("monthlyRent") BigDecimal monthlyRent,@Param("depositAmount") BigDecimal depositAmount,
            @Param("paymentDay") int paymentDay,@Param("rentCalculationMethod") String rentCalculationMethod);

    default int updateLeasePartiesAndTerms(Long leaseId,Long tenantId,Long unitId,LocalDate startDate,
            LocalDate endDate,BigDecimal monthlyRent,BigDecimal depositAmount,int paymentDay,String rentCalculationMethod) {
        return updateLeasePartiesAndTerms(leaseId,tenantId,unitId,null,startDate,endDate,monthlyRent,
                depositAmount,paymentDay,rentCalculationMethod);
    }

    @Update("""
            UPDATE rent_invoices SET amount_due=CASE WHEN #{rentCalculationMethod}='daily_prorated'
                THEN ROUND(#{monthlyRent} / DAY(LAST_DAY(billing_month)) *
                  GREATEST(DATEDIFF(LEAST(LAST_DAY(billing_month), #{endDate}),
                    GREATEST(billing_month, #{startDate})) + 1, 0), 2)
                ELSE #{monthlyRent} END,
              due_date=DATE_ADD(billing_month,INTERVAL (LEAST(#{paymentDay},DAY(LAST_DAY(billing_month)))-1) DAY)
            WHERE lease_id=#{leaseId} AND billing_month>=#{fromMonth} AND amount_paid=0
              AND NOT EXISTS (SELECT 1 FROM rent_payments rp WHERE rp.rent_invoice_id=rent_invoices.id)
            """)
    int updateFutureUnpaidInvoiceTerms(@Param("leaseId") Long leaseId,@Param("fromMonth") LocalDate fromMonth,
            @Param("monthlyRent") BigDecimal monthlyRent,@Param("paymentDay") int paymentDay,
            @Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate,
            @Param("rentCalculationMethod") String rentCalculationMethod);

    @org.apache.ibatis.annotations.Delete("""
            DELETE FROM rent_invoices
            WHERE lease_id = #{leaseId}
              AND (billing_month < #{startMonth} OR billing_month > #{endMonth})
              AND amount_paid = 0
              AND NOT EXISTS (SELECT 1 FROM rent_payments rp WHERE rp.rent_invoice_id = rent_invoices.id)
            """)
    int deleteUnpaidInvoicesOutsideLeasePeriod(@Param("leaseId") Long leaseId,
            @Param("startMonth") LocalDate startMonth, @Param("endMonth") LocalDate endMonth);

    @Update("UPDATE leases SET end_date=#{endDate},status='transferred' WHERE id=#{leaseId} AND status='active'")
    int closeLeaseForTransfer(@Param("leaseId") Long leaseId,@Param("endDate") LocalDate endDate);

    @Update("UPDATE leases SET end_date=#{endDate},status=#{status} WHERE id=#{leaseId} AND status='active'")
    int closeLease(@Param("leaseId") Long leaseId, @Param("endDate") LocalDate endDate,
            @Param("status") String status);

    @Select("SELECT id AS lease_id, unit_id, end_date FROM leases WHERE status='active' AND end_date <= #{graceCutoff} ORDER BY id")
    List<ExpiredLeaseRow> findExpiredLeases(@Param("graceCutoff") LocalDate graceCutoff);

    @Update("UPDATE leases SET status='expired' WHERE id=#{leaseId} AND status='active' AND end_date <= #{graceCutoff}")
    int expireLease(@Param("leaseId") Long leaseId, @Param("graceCutoff") LocalDate graceCutoff);

    @Update("UPDATE units SET listing_status='available' WHERE id=#{unitId} AND NOT EXISTS "
            + "(SELECT 1 FROM leases WHERE unit_id=#{unitId} AND status='active')")
    int markUnitAvailableIfNoActiveLease(@Param("unitId") Long unitId);

    @Insert("INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,before_data,after_data) "
            + "VALUES (#{actorId},'close_lease','lease',#{leaseId},"
            + "JSON_OBJECT('endDate',#{oldEndDate}),"
            + "JSON_OBJECT('endDate',#{endDate},'reason',#{reason},'notes',#{notes}))")
    int insertLeaseClosureAudit(@Param("actorId") Long actorId, @Param("leaseId") Long leaseId,
            @Param("oldEndDate") LocalDate oldEndDate, @Param("endDate") LocalDate endDate,
            @Param("reason") String reason, @Param("notes") String notes);

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
            VALUES (#{actorId},'update_lease','lease',#{leaseId},
              JSON_OBJECT('tenantId',#{oldTenantId},'unitId',#{oldUnitId},'startDate',#{oldStart},'endDate',#{oldEnd},'monthlyRent',#{oldRent},'depositAmount',#{oldDeposit},'paymentDay',#{oldPaymentDay}),
              JSON_OBJECT('tenantId',#{newTenantId},'unitId',#{newUnitId},'startDate',#{newStart},'endDate',#{newEnd},'monthlyRent',#{newRent},'depositAmount',#{newDeposit},'paymentDay',#{newPaymentDay}))
            """)
    int insertLeasePartyUpdateAudit(@Param("actorId") Long actorId,@Param("leaseId") Long leaseId,
            @Param("oldTenantId") Long oldTenantId,@Param("oldUnitId") Long oldUnitId,
            @Param("oldStart") LocalDate oldStart,@Param("oldEnd") LocalDate oldEnd,@Param("oldRent") BigDecimal oldRent,
            @Param("oldDeposit") BigDecimal oldDeposit,@Param("oldPaymentDay") int oldPaymentDay,
            @Param("newTenantId") Long newTenantId,@Param("newUnitId") Long newUnitId,
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
            INSERT INTO leases (unit_id, rental_space_id, tenant_id, rental_mandate_id, lease_no, start_date, end_date,
                                monthly_rent, deposit_amount, payment_day, rent_calculation_method, status)
            VALUES (#{unitId}, #{rentalSpaceId}, #{tenantId}, #{rentalMandateId}, #{leaseNo}, #{startDate}, #{endDate},
                    #{monthlyRent}, #{depositAmount}, #{paymentDay}, #{rentCalculationMethod}, 'active')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLease(NewLease lease);

    @Insert("""
            INSERT INTO lease_periods
              (lease_id,period_no,start_date,end_date,monthly_rent,deposit_amount,payment_day,
               rent_calculation_method,contract_document_id,created_by)
            VALUES
              (#{leaseId},#{periodNo},#{startDate},#{endDate},#{monthlyRent},#{depositAmount},#{paymentDay},
               #{rentCalculationMethod},#{contractDocumentId},#{createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLeasePeriod(NewLeasePeriod period);

    @Select("""
            SELECT lp.id,lp.lease_id,lp.period_no,lp.start_date,lp.end_date,lp.monthly_rent,
                   lp.deposit_amount,lp.payment_day,lp.rent_calculation_method,lp.contract_document_id,
                   d.original_name AS contract_document_name
            FROM lease_periods lp
            LEFT JOIN documents d ON d.id=lp.contract_document_id
            WHERE lp.lease_id=#{leaseId}
            ORDER BY lp.period_no DESC LIMIT 1 FOR UPDATE
            """)
    LeasePeriodRow lockLatestLeasePeriod(@Param("leaseId") Long leaseId);

    @Select("""
            SELECT lp.id,lp.lease_id,lp.period_no,lp.start_date,lp.end_date,lp.monthly_rent,
                   lp.deposit_amount,lp.payment_day,lp.rent_calculation_method,lp.contract_document_id,
                   d.original_name AS contract_document_name
            FROM lease_periods lp
            LEFT JOIN documents d ON d.id=lp.contract_document_id
            WHERE lp.lease_id=#{leaseId}
            ORDER BY lp.period_no
            """)
    List<LeasePeriodRow> findLeasePeriods(@Param("leaseId") Long leaseId);

    @Select("""
            SELECT lp.id,lp.lease_id,lp.period_no,lp.start_date,lp.end_date,lp.monthly_rent,
                   lp.deposit_amount,lp.payment_day,lp.rent_calculation_method,lp.contract_document_id,
                   d.original_name AS contract_document_name
            FROM lease_periods lp
            LEFT JOIN documents d ON d.id=lp.contract_document_id
            WHERE lp.lease_id=#{leaseId}
              AND lp.start_date<=LAST_DAY(#{billingMonth}) AND lp.end_date>=#{billingMonth}
            ORDER BY lp.period_no DESC LIMIT 1
            """)
    LeasePeriodRow findLeasePeriodForBillingMonth(@Param("leaseId") Long leaseId,
            @Param("billingMonth") LocalDate billingMonth);

    @Update("""
            UPDATE leases SET end_date=#{endDate},monthly_rent=#{monthlyRent},deposit_amount=#{depositAmount},
              payment_day=#{paymentDay},rent_calculation_method=#{rentCalculationMethod}
            WHERE id=#{leaseId} AND status='active' AND end_date=#{expectedEndDate}
            """)
    int extendLeaseForRenewal(@Param("leaseId") Long leaseId,@Param("expectedEndDate") LocalDate expectedEndDate,
            @Param("endDate") LocalDate endDate,@Param("monthlyRent") BigDecimal monthlyRent,
            @Param("depositAmount") BigDecimal depositAmount,@Param("paymentDay") int paymentDay,
            @Param("rentCalculationMethod") String rentCalculationMethod);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,before_data,after_data)
            VALUES (#{actorId},'renew_lease','lease',#{leaseId},
              JSON_OBJECT('endDate',#{oldEndDate}),
              JSON_OBJECT('periodId',#{periodId},'periodNo',#{periodNo},'startDate',#{startDate},
                          'endDate',#{endDate},'monthlyRent',#{monthlyRent},'depositAmount',#{depositAmount},
                          'paymentDay',#{paymentDay}))
            """)
    int insertLeaseRenewalAudit(@Param("actorId") Long actorId,@Param("leaseId") Long leaseId,
            @Param("periodId") Long periodId,@Param("periodNo") int periodNo,
            @Param("oldEndDate") LocalDate oldEndDate,@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,@Param("monthlyRent") BigDecimal monthlyRent,
            @Param("depositAmount") BigDecimal depositAmount,@Param("paymentDay") int paymentDay);

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
            INSERT IGNORE INTO rent_invoices (lease_id, billing_month, due_date, amount_due, amount_paid, status)
            VALUES (#{leaseId}, #{billingMonth}, #{dueDate}, #{amountDue}, 0, 'unpaid')
            """)
    int insertInvoice(@Param("leaseId") Long leaseId, @Param("billingMonth") LocalDate billingMonth,
            @Param("dueDate") LocalDate dueDate, @Param("amountDue") BigDecimal amountDue);

    @Update("""
            UPDATE rent_invoices
            SET amount_due = #{amountDue}, status = 'unpaid'
            WHERE lease_id = #{leaseId} AND billing_month = #{billingMonth}
              AND amount_paid = 0 AND amount_due <= 0
            """)
    int repairZeroAmountInvoice(@Param("leaseId") Long leaseId, @Param("billingMonth") LocalDate billingMonth,
            @Param("dueDate") LocalDate dueDate, @Param("amountDue") BigDecimal amountDue);

    @Insert("""
            INSERT IGNORE INTO rent_invoices
                (lease_id, billing_month, due_date, amount_due, amount_paid, status)
            SELECT l.id, #{billingMonth},
                   DATE_ADD(#{billingMonth}, INTERVAL (LEAST(lp.payment_day, DAY(LAST_DAY(#{billingMonth}))) - 1) DAY),
                   CASE WHEN lp.rent_calculation_method='daily_prorated'
                     THEN GREATEST(ROUND(lp.monthly_rent / DAY(LAST_DAY(#{billingMonth})) *
                       (DATEDIFF(LEAST(lp.end_date, LAST_DAY(#{billingMonth})), GREATEST(lp.start_date, #{billingMonth})) + 1), 2), 0.01)
                     ELSE lp.monthly_rent END, 0, 'unpaid'
            FROM leases l
            JOIN lease_periods lp ON lp.id=(SELECT lp2.id FROM lease_periods lp2
              WHERE lp2.lease_id=l.id AND lp2.start_date<=LAST_DAY(#{billingMonth})
                AND lp2.end_date>=#{billingMonth} ORDER BY lp2.period_no DESC LIMIT 1)
            WHERE l.status = 'active'
              AND l.start_date <= LAST_DAY(#{billingMonth})
              AND l.end_date >= #{billingMonth}
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

    @Update("""
            UPDATE electronic_signature_requests
            SET status='cancelled', updated_at=CURRENT_TIMESTAMP
            WHERE entity_type='lease' AND entity_id=#{leaseId}
              AND COALESCE(root_document_id,source_document_id)=#{documentId}
              AND status IN ('pending','sent','viewed')
            """)
    int cancelActiveLeaseSignatures(@Param("leaseId") Long leaseId, @Param("documentId") Long documentId);

    @Update("UPDATE leases SET contract_document_id = #{documentId} WHERE id = #{leaseId}")
    int updateLeaseContract(@Param("leaseId") Long leaseId, @Param("documentId") Long documentId);

    @Update("UPDATE lease_periods SET contract_document_id=#{documentId} WHERE lease_id=#{leaseId} AND period_no=1")
    int updateInitialLeasePeriodContract(@Param("leaseId") Long leaseId,@Param("documentId") Long documentId);

    @Select("""
            SELECT lp.id AS period_id,lp.lease_id,lp.period_no,lp.contract_document_id,l.lease_no
            FROM lease_periods lp JOIN leases l ON l.id=lp.lease_id
            WHERE lp.id=#{periodId} AND lp.lease_id=#{leaseId} FOR UPDATE
            """)
    LeasePeriodContractContext lockLeasePeriodContract(@Param("leaseId") Long leaseId,
            @Param("periodId") Long periodId);

    @Insert("""
            INSERT INTO document_links (document_id,entity_type,entity_id,relation_type)
            VALUES (#{documentId},'lease_period',#{periodId},'renewal_contract')
            """)
    int insertLeasePeriodContractLink(@Param("documentId") Long documentId,@Param("periodId") Long periodId);

    @Update("UPDATE lease_periods SET contract_document_id=#{documentId} WHERE id=#{periodId} AND lease_id=#{leaseId}")
    int updateLeasePeriodContract(@Param("leaseId") Long leaseId,@Param("periodId") Long periodId,
            @Param("documentId") Long documentId);

    @Select("""
            SELECT d.id,d.original_name,d.storage_key,d.mime_type,d.file_size,'lease' AS storage_area
            FROM lease_periods lp JOIN documents d ON d.id=lp.contract_document_id
            WHERE lp.lease_id=#{leaseId} AND lp.id=#{periodId} AND d.status<>'superseded'
            LIMIT 1
            """)
    ContractFile findLeasePeriodContractFile(@Param("leaseId") Long leaseId,@Param("periodId") Long periodId);

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
            SELECT COALESCE(signed.id, d.id) AS id,
                   COALESCE(signed.original_name, d.original_name) AS original_name,
                   COALESCE(signed.storage_key, d.storage_key) AS storage_key,
                   COALESCE(signed.mime_type, d.mime_type) AS mime_type,
                   COALESCE(signed.file_size, d.file_size) AS file_size,
                   CASE WHEN signed.id IS NULL THEN 'lease' ELSE 'electronic_signature' END AS storage_area
            FROM leases l JOIN documents d ON d.id = l.contract_document_id
            JOIN document_links dl ON dl.document_id = d.id AND dl.entity_type = 'lease'
              AND dl.entity_id = l.id AND dl.relation_type = 'contract'
            LEFT JOIN electronic_signature_requests sr ON sr.source_document_id = d.id
              AND sr.entity_type = 'lease' AND sr.entity_id = l.id AND sr.status = 'signed'
              AND (SELECT COUNT(DISTINCT completed.signer_role) FROM electronic_signature_requests completed
                   WHERE COALESCE(completed.root_document_id,completed.source_document_id)=d.id
                     AND completed.entity_type='lease' AND completed.entity_id=l.id AND completed.status='signed') >= GREATEST(2, (SELECT COUNT(*) FROM electronic_signature_participants expected_signer WHERE expected_signer.root_document_id=d.id AND expected_signer.document_kind='lease_contract'))
            LEFT JOIN documents signed ON signed.id = sr.signed_document_id
              AND signed.status NOT IN ('voided', 'superseded')
            WHERE l.id = #{leaseId} AND d.document_type = 'lease'
            ORDER BY sr.signed_at DESC, sr.id DESC
            LIMIT 1
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
        private BigDecimal currentDue, currentPaid, currentUnpaid, totalUnpaid;
        public Long getTenantCount() { return tenantCount; } public void setTenantCount(Long v) { tenantCount = v; }
        public Long getActiveLeaseCount() { return activeLeaseCount; } public void setActiveLeaseCount(Long v) { activeLeaseCount = v; }
        public Long getPartialCount() { return partialCount; } public void setPartialCount(Long v) { partialCount = v; }
        public Long getOverdueCount() { return overdueCount; } public void setOverdueCount(Long v) { overdueCount = v; }
        public Long getPendingReviewCount() { return pendingReviewCount; } public void setPendingReviewCount(Long v) { pendingReviewCount = v; }
        public BigDecimal getCurrentDue() { return currentDue; } public void setCurrentDue(BigDecimal v) { currentDue = v; }
        public BigDecimal getCurrentPaid() { return currentPaid; } public void setCurrentPaid(BigDecimal v) { currentPaid = v; }
        public BigDecimal getCurrentUnpaid() { return currentUnpaid; } public void setCurrentUnpaid(BigDecimal v) { currentUnpaid = v; }
        public BigDecimal getTotalUnpaid() { return totalUnpaid; } public void setTotalUnpaid(BigDecimal v) { totalUnpaid = v; }
    }

    class TenancyRow {
        private Long tenantId, leaseId, projectId, unitId, rentalSpaceId, contractDocumentId, invoiceId, financeRecordId, ownerId;
        private String tenantName, identityNo, phone, email, tenantStatus, leaseNo, projectName, unitNo, leaseStatus,
                rentCalculationMethod, contractDocumentName, contractDocumentMimeType,
                rentStatus, transactionNo, confirmationStatus, paymentMethod, reviewNote, confirmedByName,
                ownerName, ownerIdentity, rentalSpaceName, rentalSpaceType;
        private LocalDate leaseStart, leaseEnd, billingMonth, dueDate, paymentDate;
        private LocalDateTime confirmedAt;
        private BigDecimal monthlyRent, depositAmount, amountDue, amountPaid, amountUnpaid, totalUnpaid, prepaidRentBalance;
        private Long contractDocumentSize;
        private Boolean contractSigned;
        private String contractStatus;
        public String getContractStatus() {
            return contractStatus;
        }

        public void setContractStatus(String contractStatus) {
            this.contractStatus = contractStatus;
        }

        private Integer paymentDay;
        public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;}
        public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;}
        public Long getRentalSpaceId(){return rentalSpaceId;} public void setRentalSpaceId(Long v){rentalSpaceId=v;} public String getRentalSpaceName(){return rentalSpaceName;} public void setRentalSpaceName(String v){rentalSpaceName=v;} public String getRentalSpaceType(){return rentalSpaceType;} public void setRentalSpaceType(String v){rentalSpaceType=v;}
        public Long getContractDocumentId(){return contractDocumentId;} public void setContractDocumentId(Long v){contractDocumentId=v;} public Long getInvoiceId(){return invoiceId;} public void setInvoiceId(Long v){invoiceId=v;}
        public String getContractDocumentName(){return contractDocumentName;} public void setContractDocumentName(String v){contractDocumentName=v;} public String getContractDocumentMimeType(){return contractDocumentMimeType;} public void setContractDocumentMimeType(String v){contractDocumentMimeType=v;} public Long getContractDocumentSize(){return contractDocumentSize;} public void setContractDocumentSize(Long v){contractDocumentSize=v;} public Boolean getContractSigned(){return contractSigned;} public void setContractSigned(Boolean v){contractSigned=v;}
        public Long getFinanceRecordId(){return financeRecordId;} public void setFinanceRecordId(Long v){financeRecordId=v;} public String getTenantName(){return tenantName;} public void setTenantName(String v){tenantName=v;}
        public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;} public String getOwnerName(){return ownerName;} public void setOwnerName(String v){ownerName=v;}
        public String getOwnerIdentity(){return ownerIdentity;} public void setOwnerIdentity(String v){ownerIdentity=v;}
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
        public BigDecimal getAmountUnpaid(){return amountUnpaid;} public void setAmountUnpaid(BigDecimal v){amountUnpaid=v;} public Integer getPaymentDay(){return paymentDay;} public void setPaymentDay(Integer v){paymentDay=v;} public String getRentCalculationMethod(){return rentCalculationMethod;} public void setRentCalculationMethod(String v){rentCalculationMethod=v;}
        public BigDecimal getTotalUnpaid(){return totalUnpaid;} public void setTotalUnpaid(BigDecimal v){totalUnpaid=v;}
        public BigDecimal getPrepaidRentBalance(){return prepaidRentBalance;} public void setPrepaidRentBalance(BigDecimal v){prepaidRentBalance=v;}
    }

    class LeaseInvoiceRow {
        private Long invoiceId; private LocalDate billingMonth, dueDate; private BigDecimal amountDue, amountPaid, amountUnpaid; private String rentStatus;
        public Long getInvoiceId(){return invoiceId;} public void setInvoiceId(Long v){invoiceId=v;} public LocalDate getBillingMonth(){return billingMonth;} public void setBillingMonth(LocalDate v){billingMonth=v;}
        public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){dueDate=v;} public BigDecimal getAmountDue(){return amountDue;} public void setAmountDue(BigDecimal v){amountDue=v;}
        public BigDecimal getAmountPaid(){return amountPaid;} public void setAmountPaid(BigDecimal v){amountPaid=v;} public BigDecimal getAmountUnpaid(){return amountUnpaid;} public void setAmountUnpaid(BigDecimal v){amountUnpaid=v;}
        public String getRentStatus(){return rentStatus;} public void setRentStatus(String v){rentStatus=v;}
    }

    class DepositAccountRow {
        private Long leaseId, tenantId, financeRecordId, ownerId, reserveAccountId, pendingSettlementCount;
        private String leaseNo, tenantName, tenantPhone, projectName, unitNo, leaseStatus, transactionNo,
                depositEntryStatus, confirmationStatus, paymentStatus, ownerName;
        private LocalDate startDate, endDate, billDate;
        private BigDecimal expectedDeposit, postedBalance, availableBalance, billAmount, reserveBalance;
        public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;} public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;}
        public Long getFinanceRecordId(){return financeRecordId;} public void setFinanceRecordId(Long v){financeRecordId=v;} public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;}
        public Long getReserveAccountId(){return reserveAccountId;} public void setReserveAccountId(Long v){reserveAccountId=v;} public Long getPendingSettlementCount(){return pendingSettlementCount;} public void setPendingSettlementCount(Long v){pendingSettlementCount=v;}
        public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;} public String getTenantName(){return tenantName;} public void setTenantName(String v){tenantName=v;}
        public String getTenantPhone(){return tenantPhone;} public void setTenantPhone(String v){tenantPhone=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;}
        public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} public String getLeaseStatus(){return leaseStatus;} public void setLeaseStatus(String v){leaseStatus=v;}
        public String getTransactionNo(){return transactionNo;} public void setTransactionNo(String v){transactionNo=v;} public String getDepositEntryStatus(){return depositEntryStatus;} public void setDepositEntryStatus(String v){depositEntryStatus=v;}
        public String getConfirmationStatus(){return confirmationStatus;} public void setConfirmationStatus(String v){confirmationStatus=v;} public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String v){paymentStatus=v;}
        public String getOwnerName(){return ownerName;} public void setOwnerName(String v){ownerName=v;} public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;}
        public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;} public LocalDate getBillDate(){return billDate;} public void setBillDate(LocalDate v){billDate=v;}
        public BigDecimal getExpectedDeposit(){return expectedDeposit;} public void setExpectedDeposit(BigDecimal v){expectedDeposit=v;} public BigDecimal getPostedBalance(){return postedBalance;} public void setPostedBalance(BigDecimal v){postedBalance=v;}
        public BigDecimal getAvailableBalance(){return availableBalance;} public void setAvailableBalance(BigDecimal v){availableBalance=v;} public BigDecimal getBillAmount(){return billAmount;} public void setBillAmount(BigDecimal v){billAmount=v;}
        public BigDecimal getReserveBalance(){return reserveBalance;} public void setReserveBalance(BigDecimal v){reserveBalance=v;}
    }

    class NewTenant { private Long id; private String fullName, identityNo, phone, email, status;
        public Long getId(){return id;} public void setId(Long v){id=v;} public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
        public String getIdentityNo(){return identityNo;} public void setIdentityNo(String v){identityNo=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;}
        public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}}
    class TenantDirectoryRow { private Long tenantId, leaseCount, activeLeaseCount; private BigDecimal currentDepositAmount, currentDepositBalance; private String fullName, identityNo, phone, email, status, whatsappDestination, currentLeaseNo, projectName, unitNo, currentDepositStatus; private Boolean whatsappEnabled; private LocalDateTime whatsappOptedInAt; private LocalDate leaseStart, leaseEnd;
        public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;} public String getIdentityNo(){return identityNo;} public void setIdentityNo(String v){identityNo=v;} public String getPhone(){return phone;} public void setPhone(String v){phone=v;} public String getEmail(){return email;} public void setEmail(String v){email=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public Boolean getWhatsappEnabled(){return whatsappEnabled;} public void setWhatsappEnabled(Boolean v){whatsappEnabled=v;} public String getWhatsappDestination(){return whatsappDestination;} public void setWhatsappDestination(String v){whatsappDestination=v;} public LocalDateTime getWhatsappOptedInAt(){return whatsappOptedInAt;} public void setWhatsappOptedInAt(LocalDateTime v){whatsappOptedInAt=v;} public String getCurrentLeaseNo(){return currentLeaseNo;} public void setCurrentLeaseNo(String v){currentLeaseNo=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;} public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} public LocalDate getLeaseStart(){return leaseStart;} public void setLeaseStart(LocalDate v){leaseStart=v;} public LocalDate getLeaseEnd(){return leaseEnd;} public void setLeaseEnd(LocalDate v){leaseEnd=v;} public BigDecimal getCurrentDepositAmount(){return currentDepositAmount;} public void setCurrentDepositAmount(BigDecimal v){currentDepositAmount=v;} public BigDecimal getCurrentDepositBalance(){return currentDepositBalance;} public void setCurrentDepositBalance(BigDecimal v){currentDepositBalance=v;} public String getCurrentDepositStatus(){return currentDepositStatus;} public void setCurrentDepositStatus(String v){currentDepositStatus=v;} public Long getLeaseCount(){return leaseCount;} public void setLeaseCount(Long v){leaseCount=v;} public Long getActiveLeaseCount(){return activeLeaseCount;} public void setActiveLeaseCount(Long v){activeLeaseCount=v;} }
    class TenantDirectorySummaryRow { private Long totalCount, activeCount, inactiveCount, activeLeaseTenantCount;
        public Long getTotalCount(){return totalCount;} public void setTotalCount(Long v){totalCount=v;} public Long getActiveCount(){return activeCount;} public void setActiveCount(Long v){activeCount=v;} public Long getInactiveCount(){return inactiveCount;} public void setInactiveCount(Long v){inactiveCount=v;} public Long getActiveLeaseTenantCount(){return activeLeaseTenantCount;} public void setActiveLeaseTenantCount(Long v){activeLeaseTenantCount=v;} }
    class TenantLeaseHistoryRow { private Long leaseId, unitId, pendingMaintenanceCount; private String leaseNo, projectName, unitNo, status, signatureStatus, workflowStep; private LocalDate startDate, endDate; private BigDecimal monthlyRent, depositAmount, unpaidRent; private Integer paymentDay;
        public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;} public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;} public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;} public BigDecimal getMonthlyRent(){return monthlyRent;} public void setMonthlyRent(BigDecimal v){monthlyRent=v;} public BigDecimal getDepositAmount(){return depositAmount;} public void setDepositAmount(BigDecimal v){depositAmount=v;} public Integer getPaymentDay(){return paymentDay;} public void setPaymentDay(Integer v){paymentDay=v;} public BigDecimal getUnpaidRent(){return unpaidRent;} public void setUnpaidRent(BigDecimal v){unpaidRent=v;} public Long getPendingMaintenanceCount(){return pendingMaintenanceCount;} public void setPendingMaintenanceCount(Long v){pendingMaintenanceCount=v;} public String getSignatureStatus(){return signatureStatus;} public void setSignatureStatus(String v){signatureStatus=v;} public String getWorkflowStep(){return workflowStep;} public void setWorkflowStep(String v){workflowStep=v;} }
    class TenantDirectoryOverviewRow { private BigDecimal unpaidRent; private Long pendingMaintenanceCount, pendingSignatureCount;
        public BigDecimal getUnpaidRent(){return unpaidRent;} public void setUnpaidRent(BigDecimal v){unpaidRent=v;} public Long getPendingMaintenanceCount(){return pendingMaintenanceCount;} public void setPendingMaintenanceCount(Long v){pendingMaintenanceCount=v;} public Long getPendingSignatureCount(){return pendingSignatureCount;} public void setPendingSignatureCount(Long v){pendingSignatureCount=v;} }
    class ExpiredLeaseRow { private Long leaseId, unitId; private LocalDate endDate;
        public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;} }
    class NewLease { private Long id, unitId, rentalSpaceId, tenantId, rentalMandateId; private String leaseNo,rentCalculationMethod; private LocalDate startDate,endDate; private BigDecimal monthlyRent,depositAmount; private Integer paymentDay;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;} public Long getRentalSpaceId(){return rentalSpaceId;} public void setRentalSpaceId(Long v){rentalSpaceId=v;} public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public Long getRentalMandateId(){return rentalMandateId;} public void setRentalMandateId(Long v){rentalMandateId=v;}
        public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;} public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;}
        public BigDecimal getMonthlyRent(){return monthlyRent;} public void setMonthlyRent(BigDecimal v){monthlyRent=v;} public BigDecimal getDepositAmount(){return depositAmount;} public void setDepositAmount(BigDecimal v){depositAmount=v;} public Integer getPaymentDay(){return paymentDay;} public void setPaymentDay(Integer v){paymentDay=v;} public String getRentCalculationMethod(){return rentCalculationMethod;} public void setRentCalculationMethod(String v){rentCalculationMethod=v;}}
    class NewLeasePeriod { private Long id,leaseId,contractDocumentId,createdBy; private Integer periodNo,paymentDay; private LocalDate startDate,endDate; private BigDecimal monthlyRent,depositAmount; private String rentCalculationMethod;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;} public Long getContractDocumentId(){return contractDocumentId;} public void setContractDocumentId(Long v){contractDocumentId=v;} public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;} public Integer getPeriodNo(){return periodNo;} public void setPeriodNo(Integer v){periodNo=v;} public Integer getPaymentDay(){return paymentDay;} public void setPaymentDay(Integer v){paymentDay=v;} public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;} public BigDecimal getMonthlyRent(){return monthlyRent;} public void setMonthlyRent(BigDecimal v){monthlyRent=v;} public BigDecimal getDepositAmount(){return depositAmount;} public void setDepositAmount(BigDecimal v){depositAmount=v;} public String getRentCalculationMethod(){return rentCalculationMethod;} public void setRentCalculationMethod(String v){rentCalculationMethod=v;}}
    class LeasePeriodRow { private Long id,leaseId,contractDocumentId; private Integer periodNo,paymentDay; private LocalDate startDate,endDate; private BigDecimal monthlyRent,depositAmount; private String rentCalculationMethod,contractDocumentName;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;} public Long getContractDocumentId(){return contractDocumentId;} public void setContractDocumentId(Long v){contractDocumentId=v;} public Integer getPeriodNo(){return periodNo;} public void setPeriodNo(Integer v){periodNo=v;} public Integer getPaymentDay(){return paymentDay;} public void setPaymentDay(Integer v){paymentDay=v;} public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;} public BigDecimal getMonthlyRent(){return monthlyRent;} public void setMonthlyRent(BigDecimal v){monthlyRent=v;} public BigDecimal getDepositAmount(){return depositAmount;} public void setDepositAmount(BigDecimal v){depositAmount=v;} public String getRentCalculationMethod(){return rentCalculationMethod;} public void setRentCalculationMethod(String v){rentCalculationMethod=v;} public String getContractDocumentName(){return contractDocumentName;} public void setContractDocumentName(String v){contractDocumentName=v;}}
    class LeasePeriodContractContext { private Long periodId,leaseId,contractDocumentId; private Integer periodNo; private String leaseNo;
        public Long getPeriodId(){return periodId;} public void setPeriodId(Long v){periodId=v;} public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;} public Long getContractDocumentId(){return contractDocumentId;} public void setContractDocumentId(Long v){contractDocumentId=v;} public Integer getPeriodNo(){return periodNo;} public void setPeriodNo(Integer v){periodNo=v;} public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;}}
    class NewSecurityDeposit {
        private Long leaseId,financeRecordId,unitId,tenantId; private String transactionNo,description; private BigDecimal amount; private LocalDate transactionDate;
        public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;} public Long getFinanceRecordId(){return financeRecordId;} public void setFinanceRecordId(Long v){financeRecordId=v;}
        public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;} public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;}
        public String getTransactionNo(){return transactionNo;} public void setTransactionNo(String v){transactionNo=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
        public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public LocalDate getTransactionDate(){return transactionDate;} public void setTransactionDate(LocalDate v){transactionDate=v;}
    }
    class NewTenantDepositTransaction {
        private Long id,leaseId,tenantId,unitId,financeRecordId,createdBy;
        private String transactionNo,transactionType,direction,description,status;
        private BigDecimal amount; private LocalDate occurredOn;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;}
        public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;}
        public Long getFinanceRecordId(){return financeRecordId;} public void setFinanceRecordId(Long v){financeRecordId=v;} public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;}
        public String getTransactionNo(){return transactionNo;} public void setTransactionNo(String v){transactionNo=v;} public String getTransactionType(){return transactionType;} public void setTransactionType(String v){transactionType=v;}
        public String getDirection(){return direction;} public void setDirection(String v){direction=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;}
        public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public LocalDate getOccurredOn(){return occurredOn;} public void setOccurredOn(LocalDate v){occurredOn=v;}
    }
    class ReminderContext { private Long invoiceId,tenantId; private String tenantName,projectName,unitNo; private BigDecimal amountDue,amountPaid; private LocalDate dueDate;
        public Long getInvoiceId(){return invoiceId;} public void setInvoiceId(Long v){invoiceId=v;} public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public String getTenantName(){return tenantName;} public void setTenantName(String v){tenantName=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;} public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} public BigDecimal getAmountDue(){return amountDue;} public void setAmountDue(BigDecimal v){amountDue=v;} public BigDecimal getAmountPaid(){return amountPaid;} public void setAmountPaid(BigDecimal v){amountPaid=v;} public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){dueDate=v;}}
    class LeaseContractContext { private Long leaseId, contractDocumentId; private String leaseNo;
        public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;} public Long getContractDocumentId(){return contractDocumentId;} public void setContractDocumentId(Long v){contractDocumentId=v;} public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;}}
    class LeaseChangeContext {
        private Long leaseId,unitId,rentalSpaceId,tenantId,rentalMandateId; private String leaseNo,status,projectName,unitNo,rentalSpaceName,rentalSpaceType;
        private LocalDate startDate,endDate; private BigDecimal monthlyRent,depositAmount; private Integer paymentDay; private String rentCalculationMethod;
        public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;} public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;}
        public Long getRentalSpaceId(){return rentalSpaceId;} public void setRentalSpaceId(Long v){rentalSpaceId=v;} public String getRentalSpaceName(){return rentalSpaceName;} public void setRentalSpaceName(String v){rentalSpaceName=v;} public String getRentalSpaceType(){return rentalSpaceType;} public void setRentalSpaceType(String v){rentalSpaceType=v;}
        public Long getRentalMandateId(){return rentalMandateId;} public void setRentalMandateId(Long v){rentalMandateId=v;}
        public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;} public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;}
        public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;} public BigDecimal getMonthlyRent(){return monthlyRent;} public void setMonthlyRent(BigDecimal v){monthlyRent=v;} public BigDecimal getDepositAmount(){return depositAmount;} public void setDepositAmount(BigDecimal v){depositAmount=v;} public Integer getPaymentDay(){return paymentDay;} public void setPaymentDay(Integer v){paymentDay=v;} public String getRentCalculationMethod(){return rentCalculationMethod;} public void setRentCalculationMethod(String v){rentCalculationMethod=v;}
    }
    class RentalSpaceContext {
        private Long id,unitId; private String spaceType,status,rentalMode;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;}
        public String getSpaceType(){return spaceType;} public void setSpaceType(String v){spaceType=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getRentalMode(){return rentalMode;} public void setRentalMode(String v){rentalMode=v;}
    }
    class NewContractDocument { private Long id, fileSize, uploadedBy; private String documentNo,originalName,storageKey,mimeType,checksumSha256;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getFileSize(){return fileSize;} public void setFileSize(Long v){fileSize=v;} public Long getUploadedBy(){return uploadedBy;} public void setUploadedBy(Long v){uploadedBy=v;} public String getDocumentNo(){return documentNo;} public void setDocumentNo(String v){documentNo=v;} public String getOriginalName(){return originalName;} public void setOriginalName(String v){originalName=v;} public String getStorageKey(){return storageKey;} public void setStorageKey(String v){storageKey=v;} public String getMimeType(){return mimeType;} public void setMimeType(String v){mimeType=v;} public String getChecksumSha256(){return checksumSha256;} public void setChecksumSha256(String v){checksumSha256=v;}}
    class ContractFile { private Long id,fileSize; private String originalName,storageKey,mimeType,storageArea;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getFileSize(){return fileSize;} public void setFileSize(Long v){fileSize=v;} public String getOriginalName(){return originalName;} public void setOriginalName(String v){originalName=v;} public String getStorageKey(){return storageKey;} public void setStorageKey(String v){storageKey=v;} public String getMimeType(){return mimeType;} public void setMimeType(String v){mimeType=v;} public String getStorageArea(){return storageArea;} public void setStorageArea(String v){storageArea=v;}}
    class RentFinanceRow { private Long id,leaseId,invoiceId,proofDocumentId,proofSize; private String transactionNo,tenantName,projectName,unitNo,leaseNo,currency,paymentMethod,confirmationStatus,syncStatus,proofName,proofMimeType,receiptNo,reviewNote,allocationNote,confirmedByName; private LocalDate billingMonth,dueDate,transactionDate; private LocalDateTime confirmedAt,submittedAt; private BigDecimal invoiceAmount,invoicePaid,amount;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;} public Long getInvoiceId(){return invoiceId;} public void setInvoiceId(Long v){invoiceId=v;} public Long getProofDocumentId(){return proofDocumentId;} public void setProofDocumentId(Long v){proofDocumentId=v;} public Long getProofSize(){return proofSize;} public void setProofSize(Long v){proofSize=v;} public String getTransactionNo(){return transactionNo;} public void setTransactionNo(String v){transactionNo=v;} public String getTenantName(){return tenantName;} public void setTenantName(String v){tenantName=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;} public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;} public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;} public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;} public String getConfirmationStatus(){return confirmationStatus;} public void setConfirmationStatus(String v){confirmationStatus=v;} public String getSyncStatus(){return syncStatus;} public void setSyncStatus(String v){syncStatus=v;} public String getProofName(){return proofName;} public void setProofName(String v){proofName=v;} public String getProofMimeType(){return proofMimeType;} public void setProofMimeType(String v){proofMimeType=v;} public String getReceiptNo(){return receiptNo;} public void setReceiptNo(String v){receiptNo=v;} public String getReviewNote(){return reviewNote;} public void setReviewNote(String v){reviewNote=v;} public String getAllocationNote(){return allocationNote;} public void setAllocationNote(String v){allocationNote=v;} public String getConfirmedByName(){return confirmedByName;} public void setConfirmedByName(String v){confirmedByName=v;} public LocalDate getBillingMonth(){return billingMonth;} public void setBillingMonth(LocalDate v){billingMonth=v;} public LocalDate getDueDate(){return dueDate;} public void setDueDate(LocalDate v){dueDate=v;} public LocalDate getTransactionDate(){return transactionDate;} public void setTransactionDate(LocalDate v){transactionDate=v;} public LocalDateTime getConfirmedAt(){return confirmedAt;} public void setConfirmedAt(LocalDateTime v){confirmedAt=v;} public LocalDateTime getSubmittedAt(){return submittedAt;} public void setSubmittedAt(LocalDateTime v){submittedAt=v;} public BigDecimal getInvoiceAmount(){return invoiceAmount;} public void setInvoiceAmount(BigDecimal v){invoiceAmount=v;} public BigDecimal getInvoicePaid(){return invoicePaid;} public void setInvoicePaid(BigDecimal v){invoicePaid=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}}
    class RentFinanceSummaryRow { private Long totalCount,withProofCount,missingProofCount,pendingSyncCount; private BigDecimal totalAmount,monthAmount;
        public Long getTotalCount(){return totalCount;} public void setTotalCount(Long v){totalCount=v;} public Long getWithProofCount(){return withProofCount;} public void setWithProofCount(Long v){withProofCount=v;} public Long getMissingProofCount(){return missingProofCount;} public void setMissingProofCount(Long v){missingProofCount=v;} public Long getPendingSyncCount(){return pendingSyncCount;} public void setPendingSyncCount(Long v){pendingSyncCount=v;} public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal v){totalAmount=v;} public BigDecimal getMonthAmount(){return monthAmount;} public void setMonthAmount(BigDecimal v){monthAmount=v;}}
    class RentProofContext { private Long financeRecordId,proofDocumentId; private String transactionNo;
        public Long getFinanceRecordId(){return financeRecordId;} public void setFinanceRecordId(Long v){financeRecordId=v;} public Long getProofDocumentId(){return proofDocumentId;} public void setProofDocumentId(Long v){proofDocumentId=v;} public String getTransactionNo(){return transactionNo;} public void setTransactionNo(String v){transactionNo=v;}}
    class RentCollectionRow {
        private Long invoiceId,leaseId,latestFinanceRecordId,latestProofDocumentId,latestProofSize,overdueDays;
        private String leaseNo,tenantName,projectName,unitNo,collectionStatus,latestTransactionNo,latestPaymentMethod,
                latestProofName,latestProofMimeType;
        private LocalDate billingMonth,dueDate,latestPaymentDate; private LocalDateTime latestConfirmedAt;
        private LocalDate leaseStartDate,leaseEndDate; private String rentCalculationMethod;
        private BigDecimal amountDue,amountPaid,outstandingAmount,latestPaymentAmount,monthlyRent;
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
        public BigDecimal getMonthlyRent(){return monthlyRent;} public void setMonthlyRent(BigDecimal v){monthlyRent=v;} public LocalDate getLeaseStartDate(){return leaseStartDate;} public void setLeaseStartDate(LocalDate v){leaseStartDate=v;} public LocalDate getLeaseEndDate(){return leaseEndDate;} public void setLeaseEndDate(LocalDate v){leaseEndDate=v;} public String getRentCalculationMethod(){return rentCalculationMethod;} public void setRentCalculationMethod(String v){rentCalculationMethod=v;}
    }
    class RentCollectionSummaryRow {
        private Long outstandingCount,unpaidCount,partialCount,overdueCount; private BigDecimal outstandingAmount,monthReceived;
        public Long getOutstandingCount(){return outstandingCount;} public void setOutstandingCount(Long v){outstandingCount=v;} public Long getUnpaidCount(){return unpaidCount;} public void setUnpaidCount(Long v){unpaidCount=v;}
        public Long getPartialCount(){return partialCount;} public void setPartialCount(Long v){partialCount=v;} public Long getOverdueCount(){return overdueCount;} public void setOverdueCount(Long v){overdueCount=v;}
        public BigDecimal getOutstandingAmount(){return outstandingAmount;} public void setOutstandingAmount(BigDecimal v){outstandingAmount=v;} public BigDecimal getMonthReceived(){return monthReceived;} public void setMonthReceived(BigDecimal v){monthReceived=v;}
    }
    class RentCollectionContext {
        private Long invoiceId,leaseId,tenantId,userId,unitId,ownerId; private String leaseNo,tenantName,unitNo,projectName;
        private LocalDate billingMonth,startDate,endDate; private Integer paymentDay; private String rentCalculationMethod;
        private BigDecimal amountDue,amountPaid,monthlyRent;
        public Long getInvoiceId(){return invoiceId;} public void setInvoiceId(Long v){invoiceId=v;} public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;}
        public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
        public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;} public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;}
        public String getLeaseNo(){return leaseNo;} public void setLeaseNo(String v){leaseNo=v;} public String getTenantName(){return tenantName;} public void setTenantName(String v){tenantName=v;}
        public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;}
        public BigDecimal getAmountDue(){return amountDue;} public void setAmountDue(BigDecimal v){amountDue=v;} public BigDecimal getAmountPaid(){return amountPaid;} public void setAmountPaid(BigDecimal v){amountPaid=v;}
        public LocalDate getBillingMonth(){return billingMonth;} public void setBillingMonth(LocalDate v){billingMonth=v;} public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;} public LocalDate getEndDate(){return endDate;} public void setEndDate(LocalDate v){endDate=v;} public Integer getPaymentDay(){return paymentDay;} public void setPaymentDay(Integer v){paymentDay=v;} public String getRentCalculationMethod(){return rentCalculationMethod;} public void setRentCalculationMethod(String v){rentCalculationMethod=v;} public BigDecimal getMonthlyRent(){return monthlyRent;} public void setMonthlyRent(BigDecimal v){monthlyRent=v;}
    }
    class RentInvoiceAdvanceRow {
        private Long invoiceId; private LocalDate billingMonth; private BigDecimal amountDue,amountPaid;
        public Long getInvoiceId(){return invoiceId;} public void setInvoiceId(Long v){invoiceId=v;} public LocalDate getBillingMonth(){return billingMonth;} public void setBillingMonth(LocalDate v){billingMonth=v;} public BigDecimal getAmountDue(){return amountDue;} public void setAmountDue(BigDecimal v){amountDue=v;} public BigDecimal getAmountPaid(){return amountPaid;} public void setAmountPaid(BigDecimal v){amountPaid=v;}
    }
    class NewRentCollection {
        private Long id,unitId,ownerId,tenantId,actorId; private String transactionNo,paymentMethod,allocationNote; private LocalDate receivedDate,postingDate; private BigDecimal amount;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;} public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;}
        public Long getTenantId(){return tenantId;} public void setTenantId(Long v){tenantId=v;} public Long getActorId(){return actorId;} public void setActorId(Long v){actorId=v;}
        public String getTransactionNo(){return transactionNo;} public void setTransactionNo(String v){transactionNo=v;} public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;}
        public String getAllocationNote(){return allocationNote;} public void setAllocationNote(String v){allocationNote=v;}
        public LocalDate getReceivedDate(){return receivedDate;} public void setReceivedDate(LocalDate v){receivedDate=v;} public LocalDate getPostingDate(){return postingDate;} public void setPostingDate(LocalDate v){postingDate=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    }
    class NewRentCredit {
        private Long id,leaseId,financeRecordId,actorId; private BigDecimal receivedAmount,remainingAmount;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getLeaseId(){return leaseId;} public void setLeaseId(Long v){leaseId=v;}
        public Long getFinanceRecordId(){return financeRecordId;} public void setFinanceRecordId(Long v){financeRecordId=v;} public Long getActorId(){return actorId;} public void setActorId(Long v){actorId=v;}
        public BigDecimal getReceivedAmount(){return receivedAmount;} public void setReceivedAmount(BigDecimal v){receivedAmount=v;} public BigDecimal getRemainingAmount(){return remainingAmount;} public void setRemainingAmount(BigDecimal v){remainingAmount=v;}
    }
    class RentCreditRow {
        private Long id; private BigDecimal remainingAmount;
        public Long getId(){return id;} public void setId(Long v){id=v;} public BigDecimal getRemainingAmount(){return remainingAmount;} public void setRemainingAmount(BigDecimal v){remainingAmount=v;}
    }
    class RentInvoiceCreditRow {
        private Long invoiceId; private BigDecimal amountDue,amountPaid;
        public Long getInvoiceId(){return invoiceId;} public void setInvoiceId(Long v){invoiceId=v;} public BigDecimal getAmountDue(){return amountDue;} public void setAmountDue(BigDecimal v){amountDue=v;}
        public BigDecimal getAmountPaid(){return amountPaid;} public void setAmountPaid(BigDecimal v){amountPaid=v;}
    }
}
