package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AdminFinanceReviewMapper {

    String BASE_FROM = """
            FROM finance_records fr
            JOIN payment_receipts pr ON pr.finance_record_id = fr.id
            JOIN payment_receipt_allocations pra ON pra.receipt_id = pr.id
            JOIN payment_installments pi ON pi.id = pra.installment_id
            JOIN payment_plans pp ON pp.id = pi.payment_plan_id
            JOIN purchase_contracts pc ON pc.id = pp.purchase_contract_id
            JOIN owner_units ou ON ou.id = pc.owner_unit_id
            JOIN owners o ON o.id = fr.owner_id
            JOIN units u ON u.id = fr.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN documents d ON d.id = pr.proof_document_id
            LEFT JOIN users confirmer ON confirmer.id = fr.confirmed_by
            """;

    String RESERVE_FROM = """
            FROM finance_records fr
            JOIN owners o ON o.id = fr.owner_id
            JOIN units u ON u.id = fr.unit_id
            JOIN projects p ON p.id = u.project_id
            JOIN owner_units rou ON rou.owner_id = fr.owner_id AND rou.unit_id = fr.unit_id
              AND rou.status = 'active' AND rou.asset_stage = 'OPERATING'
            JOIN reserve_accounts ra ON ra.owner_unit_id = rou.id AND ra.status = 'active'
            LEFT JOIN payment_receipts pr ON pr.finance_record_id = fr.id
            LEFT JOIN documents d ON d.id = pr.proof_document_id
            LEFT JOIN users confirmer ON confirmer.id = fr.confirmed_by
            """;

    String EXPENSE_FROM = """
            FROM finance_records fr
            LEFT JOIN cashflow_entries ce ON ce.finance_record_id = fr.id
            LEFT JOIN rent_invoice_items rii ON rii.finance_record_id = fr.id
            LEFT JOIN rent_invoices ri ON ri.id = rii.invoice_id
            JOIN owners o ON o.id = fr.owner_id
            LEFT JOIN tenants t ON t.id = fr.tenant_id
            JOIN units u ON u.id = fr.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN users confirmer ON confirmer.id = fr.confirmed_by
            """;

    String TENANT_CHARGE_FROM = """
            FROM finance_records fr
            JOIN rent_invoice_items rii ON rii.finance_record_id = fr.id
            JOIN rent_invoices ri ON ri.id = rii.invoice_id
            JOIN leases l ON l.id = ri.lease_id
            JOIN tenants t ON t.id = fr.tenant_id
            JOIN owners o ON o.id = fr.owner_id
            JOIN units u ON u.id = fr.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN users confirmer ON confirmer.id = fr.confirmed_by
            """;

    @Select({
            "<script>",
            "SELECT fr.id, fr.transaction_no, fr.record_type, p.name AS project_name, u.unit_no,",
            "       COALESCE(pr.payer_name, o.full_name) AS payer_name, fr.amount, fr.currency,",
            "       fr.transaction_date, fr.payment_method, fr.payment_status, fr.confirmation_status, fr.sync_status,",
            "       pr.id AS receipt_id, pr.receipt_no, pr.bank_reference, pr.submission_note, pr.review_note, fr.allocation_note,",
            "       d.id AS proof_document_id, d.original_name AS proof_name, d.mime_type AS proof_mime_type, d.file_size AS proof_size,",
            "       pi.id AS installment_id, pi.installment_no, pi.milestone, pi.due_date,",
            "       pi.amount_due AS installment_amount, pi.amount_paid AS installment_paid, pra.allocated_amount,",
            "       confirmer.display_name AS confirmed_by_name, fr.confirmed_at, fr.created_at AS submitted_at",
            BASE_FROM,
            "<where>",
            "  fr.record_type = 'property_payment'",
            "  <if test=\"keyword != null and keyword != ''\">",
            "    AND CONCAT_WS(' ', fr.transaction_no, pr.receipt_no, p.name, u.unit_no, o.full_name, pr.payer_name, pr.bank_reference) LIKE CONCAT('%', #{keyword}, '%')",
            "  </if>",
            "  <if test=\"projectName != null and projectName != ''\">AND p.name = #{projectName}</if>",
            "  <if test=\"confirmationStatus == 'history'\">AND fr.confirmation_status &lt;&gt; 'pending'</if>",
            "  <if test=\"confirmationStatus != null and confirmationStatus != '' and confirmationStatus != 'history'\">AND fr.confirmation_status = #{confirmationStatus}</if>",
            "  <if test=\"syncStatus != null and syncStatus != ''\">AND fr.sync_status = #{syncStatus}</if>",
            "  <if test=\"startDate != null\">AND fr.transaction_date &gt;= #{startDate}</if>",
            "  <if test=\"endDate != null\">AND fr.transaction_date &lt;= #{endDate}</if>",
            "</where>",
            "ORDER BY CASE fr.confirmation_status WHEN 'pending' THEN 0 WHEN 'rejected' THEN 1 ELSE 2 END, fr.created_at DESC, fr.id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<FinanceReviewRow> findPage(@Param("keyword") String keyword,
                                    @Param("projectName") String projectName,
                                    @Param("confirmationStatus") String confirmationStatus,
                                    @Param("syncStatus") String syncStatus,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate,
                                    @Param("limit") int limit,
                                    @Param("offset") int offset);

    @Select({
            "<script>",
            "SELECT COUNT(DISTINCT fr.id)", BASE_FROM,
            "<where>",
            "  fr.record_type = 'property_payment'",
            "  <if test=\"keyword != null and keyword != ''\">",
            "    AND CONCAT_WS(' ', fr.transaction_no, pr.receipt_no, p.name, u.unit_no, o.full_name, pr.payer_name, pr.bank_reference) LIKE CONCAT('%', #{keyword}, '%')",
            "  </if>",
            "  <if test=\"projectName != null and projectName != ''\">AND p.name = #{projectName}</if>",
            "  <if test=\"confirmationStatus == 'history'\">AND fr.confirmation_status &lt;&gt; 'pending'</if>",
            "  <if test=\"confirmationStatus != null and confirmationStatus != '' and confirmationStatus != 'history'\">AND fr.confirmation_status = #{confirmationStatus}</if>",
            "  <if test=\"syncStatus != null and syncStatus != ''\">AND fr.sync_status = #{syncStatus}</if>",
            "  <if test=\"startDate != null\">AND fr.transaction_date &gt;= #{startDate}</if>",
            "  <if test=\"endDate != null\">AND fr.transaction_date &lt;= #{endDate}</if>",
            "</where>",
            "</script>"
    })
    Long countPage(@Param("keyword") String keyword,
                   @Param("projectName") String projectName,
                   @Param("confirmationStatus") String confirmationStatus,
                   @Param("syncStatus") String syncStatus,
                   @Param("startDate") LocalDate startDate,
                   @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT
              SUM(fr.confirmation_status = 'pending') AS pending_count,
              SUM(fr.confirmation_status = 'confirmed') AS confirmed_count,
              SUM(fr.confirmation_status = 'rejected') AS rejected_count,
              SUM(fr.confirmation_status = 'confirmed' AND fr.sync_status IN ('not_synced', 'pending', 'failed')) AS pending_sync_count,
              COALESCE(SUM(CASE WHEN fr.confirmation_status = 'pending' THEN fr.amount ELSE 0 END), 0) AS pending_amount,
              COALESCE(SUM(CASE WHEN fr.confirmation_status = 'confirmed'
                                AND YEAR(fr.confirmed_at) = YEAR(CURRENT_DATE)
                                AND MONTH(fr.confirmed_at) = MONTH(CURRENT_DATE)
                                THEN fr.amount ELSE 0 END), 0) AS confirmed_month_amount
            FROM finance_records fr
            WHERE fr.record_type = 'property_payment'
            """)
    FinanceSummaryRow findSummary();

    @Select("SELECT DISTINCT p.name FROM finance_records fr JOIN units u ON u.id = fr.unit_id JOIN projects p ON p.id = u.project_id WHERE fr.record_type = 'property_payment' ORDER BY p.name")
    List<String> findProjects();

    @Select({
            "<script>",
            "SELECT fr.id, fr.transaction_no, fr.record_type, p.name AS project_name, u.unit_no,",
            "       COALESCE(pr.payer_name, o.full_name) AS payer_name, fr.amount, fr.currency,",
            "       fr.transaction_date, fr.payment_method, fr.payment_status, fr.confirmation_status, fr.sync_status,",
            "       pr.id AS receipt_id, pr.receipt_no, pr.bank_reference, pr.submission_note, pr.review_note, fr.allocation_note,",
            "       d.id AS proof_document_id, d.original_name AS proof_name, d.mime_type AS proof_mime_type, d.file_size AS proof_size,",
            "       ra.current_balance AS account_balance, ra.minimum_balance AS account_minimum_balance,",
            "       confirmer.display_name AS confirmed_by_name, fr.confirmed_at, fr.created_at AS submitted_at",
            RESERVE_FROM,
            "<where>",
            "  fr.record_type = 'reserve_topup'",
            "  <if test=\"keyword != null and keyword != ''\">",
            "    AND CONCAT_WS(' ', fr.transaction_no, pr.receipt_no, p.name, u.unit_no, o.full_name, pr.payer_name, pr.bank_reference) LIKE CONCAT('%', #{keyword}, '%')",
            "  </if>",
            "  <if test=\"projectName != null and projectName != ''\">AND p.name = #{projectName}</if>",
            "  <if test=\"confirmationStatus == 'history'\">AND fr.confirmation_status &lt;&gt; 'pending'</if>",
            "  <if test=\"confirmationStatus != null and confirmationStatus != '' and confirmationStatus != 'history'\">AND fr.confirmation_status = #{confirmationStatus}</if>",
            "  <if test=\"syncStatus != null and syncStatus != ''\">AND fr.sync_status = #{syncStatus}</if>",
            "  <if test=\"startDate != null\">AND fr.transaction_date &gt;= #{startDate}</if>",
            "  <if test=\"endDate != null\">AND fr.transaction_date &lt;= #{endDate}</if>",
            "</where>",
            "ORDER BY CASE fr.confirmation_status WHEN 'pending' THEN 0 WHEN 'rejected' THEN 1 ELSE 2 END, fr.created_at DESC, fr.id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<FinanceReviewRow> findReservePage(@Param("keyword") String keyword,
            @Param("projectName") String projectName, @Param("confirmationStatus") String confirmationStatus,
            @Param("syncStatus") String syncStatus, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate, @Param("limit") int limit, @Param("offset") int offset);

    @Select({
            "<script>", "SELECT COUNT(*)", RESERVE_FROM, "<where>",
            "  fr.record_type = 'reserve_topup'",
            "  <if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ', fr.transaction_no, pr.receipt_no, p.name, u.unit_no, o.full_name, pr.payer_name, pr.bank_reference) LIKE CONCAT('%', #{keyword}, '%')</if>",
            "  <if test=\"projectName != null and projectName != ''\">AND p.name = #{projectName}</if>",
            "  <if test=\"confirmationStatus == 'history'\">AND fr.confirmation_status &lt;&gt; 'pending'</if>",
            "  <if test=\"confirmationStatus != null and confirmationStatus != '' and confirmationStatus != 'history'\">AND fr.confirmation_status = #{confirmationStatus}</if>",
            "  <if test=\"syncStatus != null and syncStatus != ''\">AND fr.sync_status = #{syncStatus}</if>",
            "  <if test=\"startDate != null\">AND fr.transaction_date &gt;= #{startDate}</if>",
            "  <if test=\"endDate != null\">AND fr.transaction_date &lt;= #{endDate}</if>",
            "</where>", "</script>"
    })
    Long countReservePage(@Param("keyword") String keyword, @Param("projectName") String projectName,
            @Param("confirmationStatus") String confirmationStatus, @Param("syncStatus") String syncStatus,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT SUM(fr.confirmation_status = 'pending') AS pending_count,
                   SUM(fr.confirmation_status = 'confirmed') AS confirmed_count,
                   SUM(fr.confirmation_status = 'rejected') AS rejected_count,
                   SUM(fr.confirmation_status = 'confirmed' AND fr.sync_status IN ('not_synced','pending','failed')) AS pending_sync_count,
                   COALESCE(SUM(CASE WHEN fr.confirmation_status = 'pending' THEN fr.amount ELSE 0 END), 0) AS pending_amount,
                   COALESCE(SUM(CASE WHEN fr.confirmation_status = 'confirmed'
                     AND YEAR(fr.confirmed_at) = YEAR(CURRENT_DATE) AND MONTH(fr.confirmed_at) = MONTH(CURRENT_DATE)
                     THEN fr.amount ELSE 0 END), 0) AS confirmed_month_amount
            FROM finance_records fr WHERE fr.record_type = 'reserve_topup'
            """)
    FinanceSummaryRow findReserveSummary();

    @Select("SELECT DISTINCT p.name FROM finance_records fr JOIN units u ON u.id=fr.unit_id JOIN projects p ON p.id=u.project_id WHERE fr.record_type='reserve_topup' ORDER BY p.name")
    List<String> findReserveProjects();

    @Select({"<script>",
            "SELECT fr.id,fr.transaction_no,fr.record_type,p.name AS project_name,u.unit_no,COALESCE(t.full_name,o.full_name) AS payer_name,fr.amount,fr.currency,fr.transaction_date,fr.payment_method,fr.payment_status,fr.confirmation_status,fr.sync_status,COALESCE(rii.charge_type,ce.category) AS receipt_no,COALESCE(rii.description,ce.description) AS milestone,ri.due_date,fr.allocation_note,confirmer.display_name AS confirmed_by_name,fr.confirmed_at,fr.created_at AS submitted_at",
            EXPENSE_FROM,"<where>","fr.record_type IN ('property_expense','tenant_charge','cashflow')",
            "<choose>",
            "<when test=\"reviewType == 'cashflow_maintenance'\">AND fr.record_type IN ('property_expense','cashflow')</when>",
            "<when test=\"reviewType == 'expense'\">AND fr.record_type = 'tenant_charge'</when>",
            "</choose>",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ',fr.transaction_no,p.name,u.unit_no,o.full_name,t.full_name,ce.category,ce.description,rii.charge_type,rii.description) LIKE CONCAT('%',#{keyword},'%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name=#{projectName}</if>",
            "<if test=\"confirmationStatus == 'history'\">AND fr.confirmation_status&lt;&gt;'pending'</if>",
            "<if test=\"confirmationStatus != null and confirmationStatus != '' and confirmationStatus != 'history'\">AND fr.confirmation_status=#{confirmationStatus}</if>",
            "<if test=\"syncStatus != null and syncStatus != ''\">AND fr.sync_status=#{syncStatus}</if>",
            "<if test=\"startDate != null\">AND fr.transaction_date&gt;=#{startDate}</if>",
            "<if test=\"endDate != null\">AND fr.transaction_date&lt;=#{endDate}</if>",
            "</where>","ORDER BY CASE fr.confirmation_status WHEN 'pending' THEN 0 WHEN 'rejected' THEN 1 ELSE 2 END,fr.created_at DESC,fr.id DESC LIMIT #{limit} OFFSET #{offset}","</script>"})
    List<FinanceReviewRow> findExpensePage(@Param("reviewType") String reviewType,@Param("keyword") String keyword,@Param("projectName") String projectName,@Param("confirmationStatus") String confirmationStatus,@Param("syncStatus") String syncStatus,@Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate,@Param("limit") int limit,@Param("offset") int offset);

    @Select({"<script>","SELECT COUNT(*)",EXPENSE_FROM,"<where>","fr.record_type IN ('property_expense','tenant_charge','cashflow')",
            "<choose>",
            "<when test=\"reviewType == 'cashflow_maintenance'\">AND fr.record_type IN ('property_expense','cashflow')</when>",
            "<when test=\"reviewType == 'expense'\">AND fr.record_type = 'tenant_charge'</when>",
            "</choose>",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ',fr.transaction_no,p.name,u.unit_no,o.full_name,t.full_name,ce.category,ce.description,rii.charge_type,rii.description) LIKE CONCAT('%',#{keyword},'%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name=#{projectName}</if>",
            "<if test=\"confirmationStatus == 'history'\">AND fr.confirmation_status&lt;&gt;'pending'</if>",
            "<if test=\"confirmationStatus != null and confirmationStatus != '' and confirmationStatus != 'history'\">AND fr.confirmation_status=#{confirmationStatus}</if>",
            "<if test=\"syncStatus != null and syncStatus != ''\">AND fr.sync_status=#{syncStatus}</if>",
            "<if test=\"startDate != null\">AND fr.transaction_date&gt;=#{startDate}</if>","<if test=\"endDate != null\">AND fr.transaction_date&lt;=#{endDate}</if>","</where>","</script>"})
    Long countExpensePage(@Param("reviewType") String reviewType,@Param("keyword") String keyword,@Param("projectName") String projectName,@Param("confirmationStatus") String confirmationStatus,@Param("syncStatus") String syncStatus,@Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate);

    @Select({"<script>","SELECT SUM(fr.confirmation_status='pending') pending_count,SUM(fr.confirmation_status='confirmed') confirmed_count,SUM(fr.confirmation_status='rejected') rejected_count,SUM(fr.confirmation_status='confirmed' AND fr.sync_status IN ('not_synced','pending','failed')) pending_sync_count,COALESCE(SUM(CASE WHEN fr.confirmation_status='pending' THEN fr.amount ELSE 0 END),0) pending_amount,COALESCE(SUM(CASE WHEN fr.confirmation_status='confirmed' AND YEAR(fr.confirmed_at)=YEAR(CURRENT_DATE) AND MONTH(fr.confirmed_at)=MONTH(CURRENT_DATE) THEN fr.amount ELSE 0 END),0) confirmed_month_amount FROM finance_records fr WHERE fr.record_type IN ('property_expense','tenant_charge','cashflow')",
            "<choose>",
            "<when test=\"reviewType == 'cashflow_maintenance'\">AND fr.record_type IN ('property_expense','cashflow')</when>",
            "<when test=\"reviewType == 'expense'\">AND fr.record_type = 'tenant_charge'</when>",
            "</choose>","</script>"})
    FinanceSummaryRow findExpenseSummary(@Param("reviewType") String reviewType);

    @Select({"<script>","SELECT DISTINCT p.name FROM finance_records fr JOIN units u ON u.id=fr.unit_id JOIN projects p ON p.id=u.project_id WHERE fr.record_type IN ('property_expense','tenant_charge','cashflow')",
            "<choose>",
            "<when test=\"reviewType == 'cashflow_maintenance'\">AND fr.record_type IN ('property_expense','cashflow')</when>",
            "<when test=\"reviewType == 'expense'\">AND fr.record_type = 'tenant_charge'</when>",
            "</choose>","ORDER BY p.name","</script>"})
    List<String> findExpenseProjects(@Param("reviewType") String reviewType);

    @Select({"<script>",
            "SELECT fr.id,fr.transaction_no,fr.record_type,p.name AS project_name,u.unit_no,t.full_name AS payer_name,",
            "fr.amount,fr.currency,fr.transaction_date,fr.payment_method,fr.payment_status,fr.confirmation_status,fr.sync_status,",
            "rii.charge_type AS receipt_no,rii.description AS milestone,ri.due_date,fr.allocation_note,",
            "confirmer.display_name AS confirmed_by_name,fr.confirmed_at,fr.created_at AS submitted_at",
            TENANT_CHARGE_FROM,"<where>","fr.record_type = 'tenant_charge'",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ',fr.transaction_no,p.name,u.unit_no,t.full_name,rii.charge_type,rii.description) LIKE CONCAT('%',#{keyword},'%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name=#{projectName}</if>",
            "<if test=\"confirmationStatus == 'history'\">AND fr.confirmation_status&lt;&gt;'pending'</if>",
            "<if test=\"confirmationStatus != null and confirmationStatus != '' and confirmationStatus != 'history'\">AND fr.confirmation_status=#{confirmationStatus}</if>",
            "<if test=\"syncStatus != null and syncStatus != ''\">AND fr.sync_status=#{syncStatus}</if>",
            "<if test=\"startDate != null\">AND fr.transaction_date&gt;=#{startDate}</if>",
            "<if test=\"endDate != null\">AND fr.transaction_date&lt;=#{endDate}</if>",
            "</where>","ORDER BY CASE fr.confirmation_status WHEN 'pending' THEN 0 WHEN 'rejected' THEN 1 ELSE 2 END,fr.created_at DESC,fr.id DESC LIMIT #{limit} OFFSET #{offset}","</script>"})
    List<FinanceReviewRow> findTenantChargePage(@Param("keyword") String keyword,@Param("projectName") String projectName,
            @Param("confirmationStatus") String confirmationStatus,@Param("syncStatus") String syncStatus,
            @Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate,
            @Param("limit") int limit,@Param("offset") int offset);

    @Select({"<script>","SELECT COUNT(*)",TENANT_CHARGE_FROM,"<where>","fr.record_type = 'tenant_charge'",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ',fr.transaction_no,p.name,u.unit_no,t.full_name,rii.charge_type,rii.description) LIKE CONCAT('%',#{keyword},'%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name=#{projectName}</if>",
            "<if test=\"confirmationStatus == 'history'\">AND fr.confirmation_status&lt;&gt;'pending'</if>",
            "<if test=\"confirmationStatus != null and confirmationStatus != '' and confirmationStatus != 'history'\">AND fr.confirmation_status=#{confirmationStatus}</if>",
            "<if test=\"syncStatus != null and syncStatus != ''\">AND fr.sync_status=#{syncStatus}</if>",
            "<if test=\"startDate != null\">AND fr.transaction_date&gt;=#{startDate}</if>",
            "<if test=\"endDate != null\">AND fr.transaction_date&lt;=#{endDate}</if>","</where>","</script>"})
    Long countTenantChargePage(@Param("keyword") String keyword,@Param("projectName") String projectName,
            @Param("confirmationStatus") String confirmationStatus,@Param("syncStatus") String syncStatus,
            @Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate);

    @Select("SELECT SUM(confirmation_status='pending') pending_count,SUM(confirmation_status='confirmed') confirmed_count,SUM(confirmation_status='rejected') rejected_count,SUM(confirmation_status='confirmed' AND sync_status IN ('not_synced','pending','failed')) pending_sync_count,COALESCE(SUM(CASE WHEN confirmation_status='pending' THEN amount ELSE 0 END),0) pending_amount,COALESCE(SUM(CASE WHEN confirmation_status='confirmed' AND YEAR(confirmed_at)=YEAR(CURRENT_DATE) AND MONTH(confirmed_at)=MONTH(CURRENT_DATE) THEN amount ELSE 0 END),0) confirmed_month_amount FROM finance_records WHERE record_type='tenant_charge'")
    FinanceSummaryRow findTenantChargeSummary();

    @Select("SELECT DISTINCT p.name FROM finance_records fr JOIN units u ON u.id=fr.unit_id JOIN projects p ON p.id=u.project_id WHERE fr.record_type='tenant_charge' ORDER BY p.name")
    List<String> findTenantChargeProjects();

    @Select({"<script>",
            "SELECT fr.id,fr.transaction_no,fr.record_type,p.name AS project_name,u.unit_no,COALESCE(t.full_name,o.full_name) AS payer_name,fr.amount,fr.currency,fr.transaction_date,fr.payment_method,fr.payment_status,fr.confirmation_status,fr.sync_status,CASE WHEN fr.record_type='reserve_refund' AND fr.tenant_id IS NULL THEN '業主預備金返還' ELSE ce.category END AS receipt_no,ce.description AS milestone,fr.allocation_note,confirmer.display_name AS confirmed_by_name,fr.confirmed_at,fr.created_at AS submitted_at",
            EXPENSE_FROM,"<where>",
            "<choose><when test=\"reviewType == 'reserve_refund'\">fr.record_type = 'reserve_refund' AND fr.tenant_id IS NULL</when><otherwise>(fr.record_type IN ('security_deposit','security_deposit_forfeiture') OR (fr.record_type = 'reserve_refund' AND fr.tenant_id IS NOT NULL))</otherwise></choose>",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ',fr.transaction_no,p.name,u.unit_no,o.full_name,t.full_name,ce.category,ce.description) LIKE CONCAT('%',#{keyword},'%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name=#{projectName}</if>",
            "<if test=\"confirmationStatus == 'history'\">AND fr.confirmation_status&lt;&gt;'pending'</if>",
            "<if test=\"confirmationStatus != null and confirmationStatus != '' and confirmationStatus != 'history'\">AND fr.confirmation_status=#{confirmationStatus}</if>",
            "<if test=\"syncStatus != null and syncStatus != ''\">AND fr.sync_status=#{syncStatus}</if>",
            "<if test=\"startDate != null\">AND fr.transaction_date&gt;=#{startDate}</if>",
            "<if test=\"endDate != null\">AND fr.transaction_date&lt;=#{endDate}</if>",
            "</where>","ORDER BY CASE fr.confirmation_status WHEN 'pending' THEN 0 WHEN 'rejected' THEN 1 ELSE 2 END,fr.created_at DESC,fr.id DESC LIMIT #{limit} OFFSET #{offset}","</script>"})
    List<FinanceReviewRow> findSettlementPage(@Param("reviewType") String reviewType,@Param("keyword") String keyword,@Param("projectName") String projectName,@Param("confirmationStatus") String confirmationStatus,@Param("syncStatus") String syncStatus,@Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate,@Param("limit") int limit,@Param("offset") int offset);

    @Select({"<script>","SELECT COUNT(*)",EXPENSE_FROM,"<where>",
            "<choose><when test=\"reviewType == 'reserve_refund'\">fr.record_type = 'reserve_refund' AND fr.tenant_id IS NULL</when><otherwise>(fr.record_type IN ('security_deposit','security_deposit_forfeiture') OR (fr.record_type = 'reserve_refund' AND fr.tenant_id IS NOT NULL))</otherwise></choose>",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ',fr.transaction_no,p.name,u.unit_no,o.full_name,t.full_name,ce.category,ce.description) LIKE CONCAT('%',#{keyword},'%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name=#{projectName}</if>",
            "<if test=\"confirmationStatus == 'history'\">AND fr.confirmation_status&lt;&gt;'pending'</if>",
            "<if test=\"confirmationStatus != null and confirmationStatus != '' and confirmationStatus != 'history'\">AND fr.confirmation_status=#{confirmationStatus}</if>",
            "<if test=\"syncStatus != null and syncStatus != ''\">AND fr.sync_status=#{syncStatus}</if>",
            "<if test=\"startDate != null\">AND fr.transaction_date&gt;=#{startDate}</if>","<if test=\"endDate != null\">AND fr.transaction_date&lt;=#{endDate}</if>","</where>","</script>"})
    Long countSettlementPage(@Param("reviewType") String reviewType,@Param("keyword") String keyword,@Param("projectName") String projectName,@Param("confirmationStatus") String confirmationStatus,@Param("syncStatus") String syncStatus,@Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate);

    @Select({"<script>","SELECT SUM(confirmation_status='pending') pending_count,SUM(confirmation_status='confirmed') confirmed_count,SUM(confirmation_status='rejected') rejected_count,SUM(confirmation_status='confirmed' AND sync_status IN ('not_synced','pending','failed')) pending_sync_count,COALESCE(SUM(CASE WHEN confirmation_status='pending' THEN amount ELSE 0 END),0) pending_amount,COALESCE(SUM(CASE WHEN confirmation_status='confirmed' AND YEAR(confirmed_at)=YEAR(CURRENT_DATE) AND MONTH(confirmed_at)=MONTH(CURRENT_DATE) THEN amount ELSE 0 END),0) confirmed_month_amount FROM finance_records WHERE",
            "<choose><when test=\"reviewType == 'reserve_refund'\">record_type = 'reserve_refund' AND tenant_id IS NULL</when><otherwise>(record_type IN ('security_deposit','security_deposit_forfeiture') OR (record_type = 'reserve_refund' AND tenant_id IS NOT NULL))</otherwise></choose>","</script>"})
    FinanceSummaryRow findSettlementSummary(@Param("reviewType") String reviewType);

    @Select({"<script>","SELECT DISTINCT p.name FROM finance_records fr JOIN units u ON u.id=fr.unit_id JOIN projects p ON p.id=u.project_id WHERE",
            "<choose><when test=\"reviewType == 'reserve_refund'\">fr.record_type = 'reserve_refund' AND fr.tenant_id IS NULL</when><otherwise>(fr.record_type IN ('security_deposit','security_deposit_forfeiture') OR (fr.record_type = 'reserve_refund' AND fr.tenant_id IS NOT NULL))</otherwise></choose>","ORDER BY p.name","</script>"})
    List<String> findSettlementProjects(@Param("reviewType") String reviewType);

    @Select("SELECT record_type FROM finance_records WHERE id=#{financeRecordId} FOR UPDATE")
    String lockRecordType(@Param("financeRecordId") Long financeRecordId);

    @Select({"<script>", """
            SELECT source.finance_record_id,source.source_type,source.source_id
            FROM (
              SELECT pr.finance_record_id,'payment_installment' source_type,pra.installment_id source_id,1 priority
                FROM payment_receipts pr JOIN payment_receipt_allocations pra ON pra.receipt_id=pr.id
              UNION ALL SELECT finance_record_id,'rent_invoice',rent_invoice_id,2 FROM rent_payments
              UNION ALL SELECT finance_record_id,'rent_invoice',invoice_id,3 FROM rent_invoice_items WHERE finance_record_id IS NOT NULL
              UNION ALL SELECT finance_record_id,'cashflow_entry',id,4 FROM cashflow_entries
              UNION ALL SELECT finance_record_id,'security_deposit',id,5 FROM security_deposit_entries
              UNION ALL SELECT finance_record_id,'tenant_deposit_transaction',id,6 FROM tenant_deposit_transactions WHERE finance_record_id IS NOT NULL
              UNION ALL SELECT finance_record_id,'reserve_transaction',id,7 FROM reserve_transactions WHERE finance_record_id IS NOT NULL
              UNION ALL SELECT finance_record_id,'reserve_refund_transfer',id,8 FROM reserve_refund_transfers
              UNION ALL SELECT fee_finance_record_id,'reserve_refund_transfer',id,9 FROM reserve_refund_transfers WHERE fee_finance_record_id IS NOT NULL
              UNION ALL SELECT finance_record_id,'property_expense_posting',id,10 FROM property_expense_postings
              UNION ALL SELECT finance_record_id,'payment_receipt',id,11 FROM payment_receipts
            ) source
            WHERE source.finance_record_id IN
            """, "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "ORDER BY source.finance_record_id,source.priority", "</script>"})
    List<FinanceSourceLink> findSourceLinks(@Param("ids") List<Long> ids);

    @Update("""
            UPDATE finance_records
            SET requested_transaction_date=COALESCE(requested_transaction_date,transaction_date),
                transaction_date=#{transactionDate},
                receipt_date=CASE
                  WHEN record_type IN ('property_payment','rent_payment','reserve_topup','security_deposit')
                    THEN COALESCE(#{receiptDate}, receipt_date, #{transactionDate})
                  ELSE receipt_date
                END
            WHERE id=#{financeRecordId} AND confirmation_status='pending'
            """)
    int setFinanceConfirmedDate(@Param("financeRecordId") Long financeRecordId,
            @Param("transactionDate") LocalDate transactionDate,
            @Param("receiptDate") LocalDate receiptDate);

    @Update("UPDATE cashflow_entries SET occurred_on=#{transactionDate} WHERE finance_record_id=#{financeRecordId}")
    int syncCashflowDate(@Param("financeRecordId") Long financeRecordId,
            @Param("transactionDate") LocalDate transactionDate);

    @Update("UPDATE tenant_deposit_transactions SET occurred_on=#{transactionDate} WHERE finance_record_id=#{financeRecordId}")
    int syncTenantDepositDate(@Param("financeRecordId") Long financeRecordId,
            @Param("transactionDate") LocalDate transactionDate);

    @Select("""
            SELECT fr.id AS finance_record_id,fr.unit_id,fr.record_type,
                   COALESCE(ce.direction,CASE WHEN fr.record_type IN ('property_expense','reserve_refund') THEN 'expense' ELSE 'income' END) AS direction,
                   COALESCE(ce.category,CASE fr.record_type
                     WHEN 'property_payment' THEN 'sale' WHEN 'rent_payment' THEN 'rent'
                     WHEN 'reserve_topup' THEN 'reserve' WHEN 'reserve_refund' THEN 'reserve_refund'
                     WHEN 'security_deposit' THEN 'deposit' WHEN 'security_deposit_forfeiture' THEN 'deposit_forfeiture'
                     ELSE 'other' END) AS category
            FROM finance_records fr
            LEFT JOIN cashflow_entries ce ON ce.finance_record_id=fr.id
            WHERE fr.id=#{financeRecordId}
            ORDER BY ce.id DESC LIMIT 1 FOR UPDATE
            """)
    FinanceAllocationNoteContext lockAllocationNoteContext(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE finance_records SET allocation_note=#{note} WHERE id=#{financeRecordId}")
    int updateAllocationNote(@Param("financeRecordId") Long financeRecordId,@Param("note") String note);

    @Update("UPDATE cashflow_entries SET allocation_note=#{note} WHERE finance_record_id=#{financeRecordId}")
    int updateLinkedCashflowAllocationNote(@Param("financeRecordId") Long financeRecordId,@Param("note") String note);

    @Insert("""
            INSERT INTO finance_allocation_note_defaults(unit_id,record_type,note,created_by,updated_by)
            VALUES (#{unitId},#{recordType},#{note},#{actorId},#{actorId})
            ON DUPLICATE KEY UPDATE note=VALUES(note),updated_by=VALUES(updated_by),updated_at=CURRENT_TIMESTAMP
            """)
    int upsertAllocationNoteDefault(@Param("unitId") Long unitId,@Param("recordType") String recordType,
            @Param("note") String note,@Param("actorId") Long actorId);

    @Delete("DELETE FROM finance_allocation_note_defaults WHERE unit_id=#{unitId} AND record_type=#{recordType}")
    int deleteAllocationNoteDefault(@Param("unitId") Long unitId,@Param("recordType") String recordType);

    @Insert("INSERT INTO audit_logs(actor_user_id,action,entity_type,entity_id,before_data,after_data) VALUES (#{actorId},'update_finance_allocation_note','finance_record',#{financeRecordId},JSON_OBJECT(),JSON_OBJECT('allocationNote',#{note},'reuseEnabled',#{reuseEnabled}))")
    int insertAllocationNoteAudit(@Param("actorId") Long actorId,@Param("financeRecordId") Long financeRecordId,
            @Param("note") String note,@Param("reuseEnabled") boolean reuseEnabled);

    /** A single finance row used for generating a printable invoice or receipt. */
    @Select("""
            SELECT fr.id, fr.transaction_no, fr.record_type, p.name AS project_name, u.unit_no,
                   COALESCE(pr.payer_name, o.full_name) AS payer_name, fr.amount, fr.currency,
                   fr.transaction_date, fr.payment_method, fr.payment_status, fr.confirmation_status, fr.sync_status,
                   pr.id AS receipt_id, pr.receipt_no, pr.bank_reference, pr.submission_note, pr.review_note, fr.allocation_note,
                   pi.installment_no, COALESCE(ce.description, pi.milestone, ce.category, fr.record_type) AS milestone,
                   COALESCE(pi.due_date, fr.transaction_date) AS due_date,
                   confirmer.display_name AS confirmed_by_name, fr.confirmed_at, fr.created_at AS submitted_at
            FROM finance_records fr
            JOIN owners o ON o.id = fr.owner_id
            JOIN units u ON u.id = fr.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN payment_receipts pr ON pr.finance_record_id = fr.id
            LEFT JOIN payment_receipt_allocations pra ON pra.receipt_id = pr.id
            LEFT JOIN payment_installments pi ON pi.id = pra.installment_id
            LEFT JOIN cashflow_entries ce ON ce.finance_record_id = fr.id
            LEFT JOIN users confirmer ON confirmer.id = fr.confirmed_by
            WHERE fr.id = #{financeRecordId}
            ORDER BY pra.id DESC
            LIMIT 1
            """)
    FinanceReviewRow findDocumentRow(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE finance_records SET confirmation_status='confirmed',payment_status='paid',sync_status='pending',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='property_expense' AND confirmation_status='pending'")
    int confirmExpense(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Update("UPDATE finance_records SET confirmation_status='confirmed',payment_status='paid',sync_status='pending',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='cashflow' AND confirmation_status='pending'")
    int confirmCashflow(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Update("UPDATE finance_records SET confirmation_status='rejected',payment_status='voided',sync_status='not_synced',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='cashflow' AND confirmation_status='pending'")
    int rejectCashflow(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Select("""
            SELECT COUNT(*)
            FROM finance_records fr
            WHERE fr.id = #{financeRecordId}
              AND fr.record_type = 'property_expense'
              AND fr.payment_method = 'direct_payment'
              AND EXISTS (
                SELECT 1 FROM owner_units ou
                JOIN owner_unit_services ous ON ous.owner_unit_id = ou.id
                WHERE ou.owner_id = fr.owner_id AND ou.unit_id = fr.unit_id
                  AND ous.service_type = 'RENTAL' AND ous.status = 'ended'
              )
            """)
    int countDirectPaymentBlockedByTerminatedMandate(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE finance_records SET confirmation_status='rejected',payment_status='voided',sync_status='not_synced',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='property_expense' AND confirmation_status='pending'")
    int rejectExpense(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Select("SELECT mwo.id FROM maintenance_work_orders mwo JOIN cashflow_entries ce ON ce.id = mwo.cashflow_entry_id WHERE ce.finance_record_id = #{financeRecordId} LIMIT 1")
    Long findMaintenanceWorkOrderId(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE maintenance_work_orders SET status = 'submitted', completed_at = NULL WHERE id = #{workOrderId} AND status = 'completed'")
    int resetMaintenanceAfterFinanceRejection(@Param("workOrderId") Long workOrderId);

    @Insert("INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by) VALUES (#{workOrderId}, 'submitted', NOW(), #{note}, #{reviewerId})")
    int insertMaintenanceRejectionHistory(@Param("workOrderId") Long workOrderId,
            @Param("reviewerId") Long reviewerId, @Param("note") String note);

    @Select("SELECT rii.invoice_id,rii.amount FROM finance_records fr JOIN rent_invoice_items rii ON rii.finance_record_id=fr.id WHERE fr.id=#{financeRecordId} AND fr.record_type='tenant_charge' AND fr.confirmation_status='pending' FOR UPDATE")
    TenantChargeReviewContext lockTenantChargeReview(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE finance_records SET confirmation_status='confirmed',payment_status='unpaid',sync_status='pending',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='tenant_charge' AND confirmation_status='pending'")
    int confirmTenantCharge(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Update("UPDATE rent_invoices SET status=CASE WHEN amount_paid>=amount_due+#{amount} THEN 'paid' WHEN amount_paid>0 THEN 'partial' ELSE 'unpaid' END,amount_due=amount_due+#{amount} WHERE id=#{invoiceId}")
    int increaseTenantChargeInvoice(@Param("invoiceId") Long invoiceId,@Param("amount") BigDecimal amount);

    @Update("UPDATE finance_records SET confirmation_status='rejected',payment_status='voided',sync_status='not_synced',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='tenant_charge' AND confirmation_status='pending'")
    int rejectTenantCharge(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Select("SELECT rii.invoice_id,rii.amount FROM finance_records fr JOIN rent_invoice_items rii ON rii.finance_record_id=fr.id WHERE fr.id=#{financeRecordId} AND fr.record_type='tenant_charge' AND fr.confirmation_status='confirmed' FOR UPDATE")
    TenantChargeReviewContext lockConfirmedTenantCharge(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE rent_invoices SET status=CASE WHEN amount_paid>=amount_due-#{amount} THEN 'paid' WHEN amount_paid>0 THEN 'partial' ELSE 'unpaid' END,amount_due=amount_due-#{amount} WHERE id=#{invoiceId} AND amount_due-#{amount}>=amount_paid")
    int reverseTenantChargeInvoice(@Param("invoiceId") Long invoiceId,@Param("amount") BigDecimal amount);

    @Update("UPDATE finance_records SET confirmation_status='confirmed',payment_status='paid',sync_status='pending',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='security_deposit' AND confirmation_status='pending'")
    int confirmSecurityDeposit(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Update("UPDATE security_deposit_entries SET status='confirmed' WHERE finance_record_id=#{financeRecordId} AND status='pending'")
    int confirmSecurityDepositEntry(@Param("financeRecordId") Long financeRecordId);

    @Insert("""
            INSERT IGNORE INTO tenant_deposit_transactions
              (lease_id,tenant_id,unit_id,finance_record_id,transaction_type,direction,amount,occurred_on,
               description,status,created_by)
            SELECT sde.lease_id,l.tenant_id,l.unit_id,fr.id,'collection','credit',sde.amount,fr.transaction_date,
                   CONCAT('租客押金 · ',l.lease_no),'posted',#{reviewerId}
            FROM security_deposit_entries sde
            JOIN leases l ON l.id=sde.lease_id
            JOIN finance_records fr ON fr.id=sde.finance_record_id
            WHERE fr.id=#{financeRecordId} AND sde.status='confirmed' AND fr.confirmation_status='confirmed'
            ON DUPLICATE KEY UPDATE amount=VALUES(amount),occurred_on=VALUES(occurred_on),
              description=VALUES(description),status='posted',created_by=VALUES(created_by)
            """)
    int insertConfirmedSecurityDepositLedger(@Param("financeRecordId") Long financeRecordId,
            @Param("reviewerId") Long reviewerId);

    @Update("UPDATE finance_records SET confirmation_status='rejected',payment_status='voided',sync_status='not_synced',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='security_deposit' AND confirmation_status='pending'")
    int rejectSecurityDeposit(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Update("UPDATE security_deposit_entries SET status='rejected' WHERE finance_record_id=#{financeRecordId} AND status='pending'")
    int rejectSecurityDepositEntry(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE finance_records SET confirmation_status='confirmed',payment_status='paid',sync_status='pending',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='security_deposit_forfeiture' AND confirmation_status='pending'")
    int confirmSecurityDepositForfeiture(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Update("UPDATE finance_records SET confirmation_status='rejected',payment_status='voided',sync_status='not_synced',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='security_deposit_forfeiture' AND confirmation_status='pending'")
    int rejectSecurityDepositForfeiture(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Update("UPDATE tenant_deposit_transactions SET status=#{status} WHERE finance_record_id=#{financeRecordId} AND transaction_type='forfeiture' AND status=#{fromStatus}")
    int updateSecurityDepositForfeitureLedger(@Param("financeRecordId") Long financeRecordId,
            @Param("fromStatus") String fromStatus,@Param("status") String status);

    @Update("UPDATE finance_records SET confirmation_status='rejected',payment_status='voided',sync_status='not_synced',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='reserve_refund' AND confirmation_status='pending'")
    int rejectReserveRefund(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Update("UPDATE tenant_deposit_transactions SET status='posted' WHERE finance_record_id=#{financeRecordId} AND transaction_type='refund' AND status='pending'")
    int postTenantDepositRefund(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE tenant_deposit_transactions SET status='cancelled' WHERE finance_record_id=#{financeRecordId} AND transaction_type='refund' AND status='pending'")
    int cancelTenantDepositRefund(@Param("financeRecordId") Long financeRecordId);

    @Select("SELECT id AS finance_record_id,record_type,confirmation_status,sync_status,sync_batch_id FROM finance_records WHERE id=#{financeRecordId} FOR UPDATE")
    ReopenRecordContext lockReopenRecord(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE finance_records SET confirmation_status='pending',payment_status='unpaid',sync_status='not_synced',sync_batch_id=NULL,confirmed_by=NULL,confirmed_at=NULL WHERE id=#{financeRecordId} AND confirmation_status='confirmed' AND sync_status<>'synced' AND sync_batch_id IS NULL")
    int reopenFinanceRecord(@Param("financeRecordId") Long financeRecordId);

    @Select("SELECT rt.id AS transaction_id,rt.reserve_account_id,ra.current_balance,fr.amount FROM finance_records fr JOIN reserve_transactions rt ON rt.finance_record_id=fr.id AND rt.transaction_type='topup' JOIN reserve_accounts ra ON ra.id=rt.reserve_account_id WHERE fr.id=#{financeRecordId} AND fr.record_type='reserve_topup' AND fr.confirmation_status='confirmed' ORDER BY rt.id DESC LIMIT 1 FOR UPDATE")
    ReserveTopupReopenContext lockReserveTopupReopen(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE reserve_accounts SET current_balance=current_balance-#{amount} WHERE id=#{reserveAccountId}")
    int reverseReserveTopupBalance(@Param("reserveAccountId") Long reserveAccountId,
            @Param("amount") BigDecimal amount);

    @Update("UPDATE reserve_transactions SET balance_after=balance_after-#{amount} WHERE reserve_account_id=#{reserveAccountId} AND id>#{transactionId}")
    int shiftLaterReserveBalances(@Param("reserveAccountId") Long reserveAccountId,
            @Param("transactionId") Long transactionId,@Param("amount") BigDecimal amount);

    @Delete("DELETE FROM reserve_transactions WHERE finance_record_id=#{financeRecordId} AND transaction_type='topup'")
    int deleteReserveTopupTransaction(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE documents d JOIN document_links dl ON dl.document_id=d.id SET d.status='pending_review',d.reviewed_by=NULL,d.reviewed_at=NULL,d.review_note=#{note} WHERE dl.entity_type='finance' AND dl.entity_id=#{financeRecordId} AND d.document_type='reserve_topup_proof'")
    int reopenReserveTopupDocuments(@Param("financeRecordId") Long financeRecordId,
            @Param("note") String note);

    @Select("SELECT fr.amount,fr.payment_method,rp.rent_invoice_id,COALESCE(lrc.id,0) AS rent_credit_id,COALESCE(lrc.received_amount,0) AS credit_amount FROM finance_records fr JOIN rent_payments rp ON rp.finance_record_id=fr.id LEFT JOIN lease_rent_credits lrc ON lrc.finance_record_id=fr.id WHERE fr.id=#{financeRecordId} AND fr.record_type='rent_payment' AND fr.confirmation_status='confirmed' FOR UPDATE")
    RentPaymentReopenContext lockRentPaymentReopen(@Param("financeRecordId") Long financeRecordId);

    @Select("SELECT lrca.rent_invoice_id,lrca.amount FROM lease_rent_credit_allocations lrca JOIN lease_rent_credits lrc ON lrc.id=lrca.rent_credit_id WHERE lrc.finance_record_id=#{financeRecordId} ORDER BY lrca.id FOR UPDATE")
    List<RentCreditAllocationReopenContext> lockRentCreditAllocations(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE rent_invoices SET amount_paid=GREATEST(0,amount_paid-#{amount}),status=CASE WHEN amount_paid-#{amount}>=amount_due THEN 'paid' WHEN amount_paid-#{amount}>0 THEN 'partial' WHEN due_date<CURRENT_DATE THEN 'overdue' ELSE 'unpaid' END WHERE id=#{invoiceId} AND amount_paid>=#{amount}")
    int reverseRentInvoicePayment(@Param("invoiceId") Long invoiceId,@Param("amount") BigDecimal amount);

    @Delete("DELETE lrca FROM lease_rent_credit_allocations lrca JOIN lease_rent_credits lrc ON lrc.id=lrca.rent_credit_id WHERE lrc.finance_record_id=#{financeRecordId}")
    int deleteRentCreditAllocations(@Param("financeRecordId") Long financeRecordId);

    @Delete("DELETE FROM lease_rent_credits WHERE finance_record_id=#{financeRecordId}")
    int deleteRentCredit(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE tenant_deposit_transactions SET status='cancelled' WHERE finance_record_id=#{financeRecordId} AND transaction_type='rent_deduction' AND status='posted'")
    int cancelRentDepositDeduction(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE finance_records SET confirmation_status='rejected',payment_status='voided',sync_status=CASE WHEN sync_batch_id IS NULL THEN 'not_synced' ELSE 'pending' END,confirmed_by=NULL,confirmed_at=NULL WHERE id=#{financeRecordId} AND record_type='rent_payment' AND confirmation_status='confirmed'")
    int voidReopenedRentPayment(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE payment_installments SET status=CASE WHEN amount_paid-#{amount}<=0 THEN 'pending' WHEN amount_paid-#{amount}>=amount_due THEN 'paid' ELSE 'partial' END,amount_paid=GREATEST(0,amount_paid-#{amount}),updated_at=CURRENT_TIMESTAMP WHERE id=#{installmentId} AND amount_paid>=#{amount}")
    int reverseConfirmedPayment(@Param("installmentId") Long installmentId,@Param("amount") BigDecimal amount);

    @Update("UPDATE documents SET status='pending',reviewed_by=NULL,reviewed_at=NULL,review_note=#{note},updated_at=CURRENT_TIMESTAMP WHERE id=#{documentId}")
    int reopenDocument(@Param("documentId") Long documentId,@Param("note") String note);

    @Update("UPDATE security_deposit_entries SET status='pending' WHERE finance_record_id=#{financeRecordId} AND status='confirmed'")
    int reopenSecurityDepositEntry(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE tenant_deposit_transactions SET status='cancelled' WHERE finance_record_id=#{financeRecordId} AND transaction_type='collection' AND status='posted'")
    int reopenSecurityDepositLedger(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE tenant_deposit_transactions SET status='pending' WHERE finance_record_id=#{financeRecordId} AND transaction_type='refund' AND status='posted'")
    int reopenTenantDepositRefund(@Param("financeRecordId") Long financeRecordId);

    @Select("SELECT rt.reserve_account_id,ra.current_balance,fr.amount FROM finance_records fr JOIN reserve_transactions rt ON rt.finance_record_id=fr.id AND rt.transaction_type='debit' JOIN reserve_accounts ra ON ra.id=rt.reserve_account_id WHERE fr.id=#{financeRecordId} AND fr.record_type='reserve_refund' AND fr.confirmation_status='confirmed' ORDER BY rt.id DESC LIMIT 1 FOR UPDATE")
    ReserveRefundReopenContext lockReserveRefundReopen(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE reserve_accounts SET current_balance=current_balance+#{amount} WHERE id=#{reserveAccountId}")
    int restoreReserveBalance(@Param("reserveAccountId") Long reserveAccountId,@Param("amount") BigDecimal amount);

    @Insert("INSERT INTO reserve_transactions (reserve_account_id,finance_record_id,transaction_type,amount,occurred_at,balance_after,note,created_by) VALUES (#{reserveAccountId},#{financeRecordId},'adjustment',#{amount},CURRENT_TIMESTAMP,#{balanceAfter},#{note},#{reviewerId})")
    int insertReserveRefundReversal(@Param("reserveAccountId") Long reserveAccountId,
            @Param("financeRecordId") Long financeRecordId,@Param("amount") BigDecimal amount,
            @Param("balanceAfter") BigDecimal balanceAfter,@Param("note") String note,
            @Param("reviewerId") Long reviewerId);

    @Select("SELECT ra.id AS reserve_account_id, ra.current_balance, fr.amount, fr.owner_id, o.user_id, p.name AS project_name, u.unit_no FROM finance_records fr JOIN owners o ON o.id=fr.owner_id JOIN units u ON u.id=fr.unit_id JOIN projects p ON p.id=u.project_id JOIN owner_units ou ON ou.owner_id=fr.owner_id AND ou.unit_id=fr.unit_id AND ou.status='active' JOIN reserve_accounts ra ON ra.owner_unit_id=ou.id AND ra.status='active' WHERE fr.id=#{financeRecordId} AND fr.record_type='reserve_refund' AND fr.confirmation_status='pending' FOR UPDATE")
    ReserveRefundContext lockReserveRefund(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE finance_records SET confirmation_status='confirmed',payment_status='paid',sync_status='pending',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='reserve_refund' AND confirmation_status='pending'")
    int confirmReserveRefund(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Update("UPDATE reserve_accounts SET current_balance=current_balance-#{amount} WHERE id=#{reserveAccountId}")
    int debitReserveBalance(@Param("reserveAccountId") Long reserveAccountId,@Param("amount") BigDecimal amount);

    @Insert("INSERT INTO reserve_transactions (reserve_account_id,finance_record_id,transaction_type,amount,occurred_at,balance_after,note,created_by) VALUES (#{reserveAccountId},#{financeRecordId},'debit',#{amount},TIMESTAMP((SELECT transaction_date FROM finance_records WHERE id=#{financeRecordId})),#{balanceAfter},'業主預備金返還',#{reviewerId})")
    int insertReserveRefundTransaction(@Param("reserveAccountId") Long reserveAccountId,@Param("financeRecordId") Long financeRecordId,@Param("amount") BigDecimal amount,@Param("balanceAfter") BigDecimal balanceAfter,@Param("reviewerId") Long reviewerId);

    @Select("""
            SELECT fr.id AS finance_record_id, fr.confirmation_status, fr.amount, fr.owner_id, o.user_id,
                   fr.sync_status, pr.id AS receipt_id, pr.proof_document_id,
                   pra.installment_id, pra.allocated_amount, pi.amount_due, pi.amount_paid,
                   p.name AS project_name, u.unit_no, pi.installment_no, pi.milestone
            FROM finance_records fr
            JOIN owners o ON o.id = fr.owner_id
            JOIN payment_receipts pr ON pr.finance_record_id = fr.id
            JOIN payment_receipt_allocations pra ON pra.receipt_id = pr.id
            JOIN payment_installments pi ON pi.id = pra.installment_id
            JOIN units u ON u.id = fr.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE fr.id = #{financeRecordId} AND fr.record_type = 'property_payment'
            FOR UPDATE
            """)
    ReviewActionContext lockReview(@Param("financeRecordId") Long financeRecordId);

    @Update("""
            UPDATE finance_records
            SET confirmation_status = 'confirmed', payment_status = 'paid', sync_status = 'pending',
                confirmed_by = #{reviewerId}, confirmed_at = CURRENT_TIMESTAMP
            WHERE id = #{financeRecordId} AND confirmation_status = 'pending'
            """)
    int confirmFinanceRecord(@Param("financeRecordId") Long financeRecordId, @Param("reviewerId") Long reviewerId);

    @Update("""
            UPDATE finance_records
            SET confirmation_status = 'rejected', payment_status = 'failed', sync_status = 'not_synced',
                confirmed_by = #{reviewerId}, confirmed_at = CURRENT_TIMESTAMP
            WHERE id = #{financeRecordId} AND confirmation_status = 'pending'
            """)
    int rejectFinanceRecord(@Param("financeRecordId") Long financeRecordId, @Param("reviewerId") Long reviewerId);

    @Update("""
            UPDATE payment_installments
            SET amount_paid = amount_paid + #{amount},
                status = CASE WHEN amount_paid + #{amount} >= amount_due THEN 'paid' ELSE 'partial' END,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{installmentId}
            """)
    int allocateConfirmedPayment(@Param("installmentId") Long installmentId, @Param("amount") BigDecimal amount);

    @Update("UPDATE payment_receipts SET review_note = #{note} WHERE id = #{receiptId}")
    int updateReceiptReview(@Param("receiptId") Long receiptId, @Param("note") String note);

    @Update("""
            UPDATE documents SET status = #{status}, reviewed_by = #{reviewerId},
                reviewed_at = CURRENT_TIMESTAMP, review_note = #{note}, updated_at = CURRENT_TIMESTAMP
            WHERE id = #{documentId}
            """)
    int reviewDocument(@Param("documentId") Long documentId,
                       @Param("status") String status,
                       @Param("reviewerId") Long reviewerId,
                       @Param("note") String note);

    @Insert("""
            INSERT INTO notifications
              (recipient_user_id, recipient_owner_id, title, body, related_type, related_id, priority, status)
            VALUES
              (#{userId}, #{ownerId}, #{title}, #{body}, 'finance_record', #{financeRecordId}, #{priority}, 'unread')
            """)
    int insertNotification(@Param("userId") Long userId,
                           @Param("ownerId") Long ownerId,
                           @Param("financeRecordId") Long financeRecordId,
                           @Param("title") String title,
                           @Param("body") String body,
                           @Param("priority") String priority);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, before_data, after_data)
            VALUES (#{actorId}, #{action}, 'finance_record', #{financeRecordId},
                    JSON_OBJECT('confirmationStatus', 'pending'),
                    JSON_OBJECT('confirmationStatus', #{status}, 'note', #{note}))
            """)
    int insertAudit(@Param("actorId") Long actorId,
                    @Param("financeRecordId") Long financeRecordId,
                    @Param("action") String action,
                    @Param("status") String status,
                    @Param("note") String note);

    @Insert("INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,before_data,after_data) VALUES (#{actorId},'reopen_finance_record','finance_record',#{financeRecordId},JSON_OBJECT('confirmationStatus','confirmed'),JSON_OBJECT('confirmationStatus','pending','note',#{note}))")
    int insertReopenAudit(@Param("actorId") Long actorId,@Param("financeRecordId") Long financeRecordId,
            @Param("note") String note);

    @Insert("INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,before_data,after_data) VALUES (#{actorId},'reopen_rent_payment','finance_record',#{financeRecordId},JSON_OBJECT('confirmationStatus','confirmed'),JSON_OBJECT('confirmationStatus','rejected','paymentStatus','voided','note',#{note}))")
    int insertRentReopenAudit(@Param("actorId") Long actorId,@Param("financeRecordId") Long financeRecordId,
            @Param("note") String note);

    @Select("""
            SELECT d.id, d.original_name, d.storage_key, d.mime_type, d.file_size
            FROM documents d
            JOIN payment_receipts pr ON pr.proof_document_id = d.id
            JOIN finance_records fr ON fr.id = pr.finance_record_id
            WHERE d.id = #{documentId} AND fr.record_type = 'property_payment'
            """)
    ProofFile findProofFile(@Param("documentId") Long documentId);

    class FinanceSummaryRow {
        private Long pendingCount; private Long confirmedCount; private Long rejectedCount; private Long pendingSyncCount;
        private BigDecimal pendingAmount; private BigDecimal confirmedMonthAmount;
        public Long getPendingCount() { return pendingCount; } public void setPendingCount(Long v) { pendingCount = v; }
        public Long getConfirmedCount() { return confirmedCount; } public void setConfirmedCount(Long v) { confirmedCount = v; }
        public Long getRejectedCount() { return rejectedCount; } public void setRejectedCount(Long v) { rejectedCount = v; }
        public Long getPendingSyncCount() { return pendingSyncCount; } public void setPendingSyncCount(Long v) { pendingSyncCount = v; }
        public BigDecimal getPendingAmount() { return pendingAmount; } public void setPendingAmount(BigDecimal v) { pendingAmount = v; }
        public BigDecimal getConfirmedMonthAmount() { return confirmedMonthAmount; } public void setConfirmedMonthAmount(BigDecimal v) { confirmedMonthAmount = v; }
    }

    class FinanceReviewRow {
        private Long id; private String transactionNo; private String recordType; private String projectName; private String unitNo;
        private String payerName; private BigDecimal amount; private String currency; private LocalDate transactionDate;
        private String paymentMethod; private String paymentStatus; private String confirmationStatus; private String syncStatus;
        private Long receiptId; private String receiptNo; private String bankReference; private String submissionNote; private String reviewNote; private String allocationNote;
        private Long proofDocumentId; private String proofName; private String proofMimeType; private Long proofSize;
        private Long installmentId; private Integer installmentNo; private String milestone; private LocalDate dueDate;
        private BigDecimal installmentAmount; private BigDecimal installmentPaid; private BigDecimal allocatedAmount;
        private BigDecimal accountBalance; private BigDecimal accountMinimumBalance;
        private String confirmedByName; private LocalDateTime confirmedAt; private LocalDateTime submittedAt;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public String getTransactionNo() { return transactionNo; } public void setTransactionNo(String v) { transactionNo = v; }
        public String getRecordType() { return recordType; } public void setRecordType(String v) { recordType = v; }
        public String getProjectName() { return projectName; } public void setProjectName(String v) { projectName = v; }
        public String getUnitNo() { return unitNo; } public void setUnitNo(String v) { unitNo = v; }
        public String getPayerName() { return payerName; } public void setPayerName(String v) { payerName = v; }
        public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { amount = v; }
        public String getCurrency() { return currency; } public void setCurrency(String v) { currency = v; }
        public LocalDate getTransactionDate() { return transactionDate; } public void setTransactionDate(LocalDate v) { transactionDate = v; }
        public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String v) { paymentMethod = v; }
        public String getPaymentStatus() { return paymentStatus; } public void setPaymentStatus(String v) { paymentStatus = v; }
        public String getConfirmationStatus() { return confirmationStatus; } public void setConfirmationStatus(String v) { confirmationStatus = v; }
        public String getSyncStatus() { return syncStatus; } public void setSyncStatus(String v) { syncStatus = v; }
        public Long getReceiptId() { return receiptId; } public void setReceiptId(Long v) { receiptId = v; }
        public String getReceiptNo() { return receiptNo; } public void setReceiptNo(String v) { receiptNo = v; }
        public String getBankReference() { return bankReference; } public void setBankReference(String v) { bankReference = v; }
        public String getSubmissionNote() { return submissionNote; } public void setSubmissionNote(String v) { submissionNote = v; }
        public String getReviewNote() { return reviewNote; } public void setReviewNote(String v) { reviewNote = v; }
        public String getAllocationNote() { return allocationNote; } public void setAllocationNote(String v) { allocationNote = v; }
        public Long getProofDocumentId() { return proofDocumentId; } public void setProofDocumentId(Long v) { proofDocumentId = v; }
        public String getProofName() { return proofName; } public void setProofName(String v) { proofName = v; }
        public String getProofMimeType() { return proofMimeType; } public void setProofMimeType(String v) { proofMimeType = v; }
        public Long getProofSize() { return proofSize; } public void setProofSize(Long v) { proofSize = v; }
        public Long getInstallmentId() { return installmentId; } public void setInstallmentId(Long v) { installmentId = v; }
        public Integer getInstallmentNo() { return installmentNo; } public void setInstallmentNo(Integer v) { installmentNo = v; }
        public String getMilestone() { return milestone; } public void setMilestone(String v) { milestone = v; }
        public LocalDate getDueDate() { return dueDate; } public void setDueDate(LocalDate v) { dueDate = v; }
        public BigDecimal getInstallmentAmount() { return installmentAmount; } public void setInstallmentAmount(BigDecimal v) { installmentAmount = v; }
        public BigDecimal getInstallmentPaid() { return installmentPaid; } public void setInstallmentPaid(BigDecimal v) { installmentPaid = v; }
        public BigDecimal getAllocatedAmount() { return allocatedAmount; } public void setAllocatedAmount(BigDecimal v) { allocatedAmount = v; }
        public BigDecimal getAccountBalance() { return accountBalance; } public void setAccountBalance(BigDecimal v) { accountBalance = v; }
        public BigDecimal getAccountMinimumBalance() { return accountMinimumBalance; } public void setAccountMinimumBalance(BigDecimal v) { accountMinimumBalance = v; }
        public String getConfirmedByName() { return confirmedByName; } public void setConfirmedByName(String v) { confirmedByName = v; }
        public LocalDateTime getConfirmedAt() { return confirmedAt; } public void setConfirmedAt(LocalDateTime v) { confirmedAt = v; }
        public LocalDateTime getSubmittedAt() { return submittedAt; } public void setSubmittedAt(LocalDateTime v) { submittedAt = v; }
    }

    class ReviewActionContext {
        private Long financeRecordId; private String confirmationStatus; private BigDecimal amount; private Long ownerId; private Long userId;
        private String syncStatus; private Long receiptId; private Long proofDocumentId; private Long installmentId;
        private BigDecimal allocatedAmount; private BigDecimal amountDue; private BigDecimal amountPaid;
        private String projectName; private String unitNo; private Integer installmentNo; private String milestone;
        public Long getFinanceRecordId() { return financeRecordId; } public void setFinanceRecordId(Long v) { financeRecordId = v; }
        public String getConfirmationStatus() { return confirmationStatus; } public void setConfirmationStatus(String v) { confirmationStatus = v; }
        public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { amount = v; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long v) { ownerId = v; }
        public Long getUserId() { return userId; } public void setUserId(Long v) { userId = v; }
        public String getSyncStatus() { return syncStatus; } public void setSyncStatus(String v) { syncStatus = v; }
        public Long getReceiptId() { return receiptId; } public void setReceiptId(Long v) { receiptId = v; }
        public Long getProofDocumentId() { return proofDocumentId; } public void setProofDocumentId(Long v) { proofDocumentId = v; }
        public Long getInstallmentId() { return installmentId; } public void setInstallmentId(Long v) { installmentId = v; }
        public BigDecimal getAllocatedAmount() { return allocatedAmount; } public void setAllocatedAmount(BigDecimal v) { allocatedAmount = v; }
        public BigDecimal getAmountDue() { return amountDue; } public void setAmountDue(BigDecimal v) { amountDue = v; }
        public BigDecimal getAmountPaid() { return amountPaid; } public void setAmountPaid(BigDecimal v) { amountPaid = v; }
        public String getProjectName() { return projectName; } public void setProjectName(String v) { projectName = v; }
        public String getUnitNo() { return unitNo; } public void setUnitNo(String v) { unitNo = v; }
        public Integer getInstallmentNo() { return installmentNo; } public void setInstallmentNo(Integer v) { installmentNo = v; }
        public String getMilestone() { return milestone; } public void setMilestone(String v) { milestone = v; }
    }

    class ReserveRefundContext {
        private Long reserveAccountId, ownerId, userId; private BigDecimal currentBalance, amount; private String projectName, unitNo;
        public Long getReserveAccountId(){return reserveAccountId;} public void setReserveAccountId(Long v){reserveAccountId=v;}
        public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;}
        public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
        public BigDecimal getCurrentBalance(){return currentBalance;} public void setCurrentBalance(BigDecimal v){currentBalance=v;}
        public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
        public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;}
        public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;}
    }

    class FinanceSourceLink {
        private Long financeRecordId; private String sourceType; private Long sourceId;
        public Long getFinanceRecordId(){return financeRecordId;} public void setFinanceRecordId(Long value){financeRecordId=value;}
        public String getSourceType(){return sourceType;} public void setSourceType(String value){sourceType=value;}
        public Long getSourceId(){return sourceId;} public void setSourceId(Long value){sourceId=value;}
    }

    class TenantChargeReviewContext {
        private Long invoiceId;
        private BigDecimal amount;
        public Long getInvoiceId() { return invoiceId; }
        public void setInvoiceId(Long value) { invoiceId = value; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal value) { amount = value; }
    }

    class FinanceAllocationNoteContext {
        private Long financeRecordId,unitId; private String recordType,direction,category;
        public Long getFinanceRecordId(){return financeRecordId;} public void setFinanceRecordId(Long v){financeRecordId=v;}
        public Long getUnitId(){return unitId;} public void setUnitId(Long v){unitId=v;}
        public String getRecordType(){return recordType;} public void setRecordType(String v){recordType=v;}
        public String getDirection(){return direction;} public void setDirection(String v){direction=v;}
        public String getCategory(){return category;} public void setCategory(String v){category=v;}
    }

    class ReopenRecordContext {
        private Long financeRecordId, syncBatchId; private String recordType, confirmationStatus, syncStatus;
        public Long getFinanceRecordId(){return financeRecordId;} public void setFinanceRecordId(Long v){financeRecordId=v;}
        public Long getSyncBatchId(){return syncBatchId;} public void setSyncBatchId(Long v){syncBatchId=v;}
        public String getRecordType(){return recordType;} public void setRecordType(String v){recordType=v;}
        public String getConfirmationStatus(){return confirmationStatus;} public void setConfirmationStatus(String v){confirmationStatus=v;}
        public String getSyncStatus(){return syncStatus;} public void setSyncStatus(String v){syncStatus=v;}
    }

    class ReserveRefundReopenContext {
        private Long reserveAccountId; private BigDecimal currentBalance, amount;
        public Long getReserveAccountId(){return reserveAccountId;} public void setReserveAccountId(Long v){reserveAccountId=v;}
        public BigDecimal getCurrentBalance(){return currentBalance;} public void setCurrentBalance(BigDecimal v){currentBalance=v;}
        public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    }

    class ReserveTopupReopenContext {
        private Long transactionId,reserveAccountId; private BigDecimal currentBalance,amount;
        public Long getTransactionId(){return transactionId;} public void setTransactionId(Long v){transactionId=v;}
        public Long getReserveAccountId(){return reserveAccountId;} public void setReserveAccountId(Long v){reserveAccountId=v;}
        public BigDecimal getCurrentBalance(){return currentBalance;} public void setCurrentBalance(BigDecimal v){currentBalance=v;}
        public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    }

    class RentPaymentReopenContext {
        private Long rentInvoiceId,rentCreditId; private BigDecimal amount,creditAmount; private String paymentMethod;
        public Long getRentInvoiceId(){return rentInvoiceId;} public void setRentInvoiceId(Long v){rentInvoiceId=v;}
        public Long getRentCreditId(){return rentCreditId;} public void setRentCreditId(Long v){rentCreditId=v;}
        public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
        public BigDecimal getCreditAmount(){return creditAmount;} public void setCreditAmount(BigDecimal v){creditAmount=v;}
        public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;}
    }

    class RentCreditAllocationReopenContext {
        private Long rentInvoiceId; private BigDecimal amount;
        public Long getRentInvoiceId(){return rentInvoiceId;} public void setRentInvoiceId(Long v){rentInvoiceId=v;}
        public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    }

    class ProofFile {
        private Long id; private String originalName; private String storageKey; private String mimeType; private Long fileSize;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public String getOriginalName() { return originalName; } public void setOriginalName(String v) { originalName = v; }
        public String getStorageKey() { return storageKey; } public void setStorageKey(String v) { storageKey = v; }
        public String getMimeType() { return mimeType; } public void setMimeType(String v) { mimeType = v; }
        public Long getFileSize() { return fileSize; } public void setFileSize(Long v) { fileSize = v; }
    }
}
