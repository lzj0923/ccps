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
            JOIN cashflow_entries ce ON ce.finance_record_id = fr.id
            JOIN owners o ON o.id = fr.owner_id
            LEFT JOIN tenants t ON t.id = fr.tenant_id
            JOIN units u ON u.id = fr.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN users confirmer ON confirmer.id = fr.confirmed_by
            """;

    @Select({
            "<script>",
            "SELECT fr.id, fr.transaction_no, fr.record_type, p.name AS project_name, u.unit_no,",
            "       COALESCE(pr.payer_name, o.full_name) AS payer_name, fr.amount, fr.currency,",
            "       fr.transaction_date, fr.payment_method, fr.payment_status, fr.confirmation_status, fr.sync_status,",
            "       pr.id AS receipt_id, pr.receipt_no, pr.bank_reference, pr.submission_note, pr.review_note,",
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
            "       pr.id AS receipt_id, pr.receipt_no, pr.bank_reference, pr.submission_note, pr.review_note,",
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
            "SELECT fr.id,fr.transaction_no,fr.record_type,p.name AS project_name,u.unit_no,COALESCE(t.full_name,o.full_name) AS payer_name,fr.amount,fr.currency,fr.transaction_date,fr.payment_method,fr.payment_status,fr.confirmation_status,fr.sync_status,CASE WHEN fr.record_type='reserve_refund' THEN '業主預備金返還' ELSE ce.category END AS receipt_no,ce.description AS milestone,confirmer.display_name AS confirmed_by_name,fr.confirmed_at,fr.created_at AS submitted_at",
            EXPENSE_FROM,"<where>","fr.record_type IN ('property_expense','reserve_refund','security_deposit')",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ',fr.transaction_no,p.name,u.unit_no,o.full_name,t.full_name,ce.category,ce.description) LIKE CONCAT('%',#{keyword},'%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name=#{projectName}</if>",
            "<if test=\"confirmationStatus == 'history'\">AND fr.confirmation_status&lt;&gt;'pending'</if>",
            "<if test=\"confirmationStatus != null and confirmationStatus != '' and confirmationStatus != 'history'\">AND fr.confirmation_status=#{confirmationStatus}</if>",
            "<if test=\"syncStatus != null and syncStatus != ''\">AND fr.sync_status=#{syncStatus}</if>",
            "<if test=\"startDate != null\">AND fr.transaction_date&gt;=#{startDate}</if>",
            "<if test=\"endDate != null\">AND fr.transaction_date&lt;=#{endDate}</if>",
            "</where>","ORDER BY CASE fr.confirmation_status WHEN 'pending' THEN 0 WHEN 'rejected' THEN 1 ELSE 2 END,fr.created_at DESC,fr.id DESC LIMIT #{limit} OFFSET #{offset}","</script>"})
    List<FinanceReviewRow> findExpensePage(@Param("keyword") String keyword,@Param("projectName") String projectName,@Param("confirmationStatus") String confirmationStatus,@Param("syncStatus") String syncStatus,@Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate,@Param("limit") int limit,@Param("offset") int offset);

    @Select({"<script>","SELECT COUNT(*)",EXPENSE_FROM,"<where>","fr.record_type IN ('property_expense','reserve_refund','security_deposit')",
            "<if test=\"keyword != null and keyword != ''\">AND CONCAT_WS(' ',fr.transaction_no,p.name,u.unit_no,o.full_name,t.full_name,ce.category,ce.description) LIKE CONCAT('%',#{keyword},'%')</if>",
            "<if test=\"projectName != null and projectName != ''\">AND p.name=#{projectName}</if>",
            "<if test=\"confirmationStatus == 'history'\">AND fr.confirmation_status&lt;&gt;'pending'</if>",
            "<if test=\"confirmationStatus != null and confirmationStatus != '' and confirmationStatus != 'history'\">AND fr.confirmation_status=#{confirmationStatus}</if>",
            "<if test=\"syncStatus != null and syncStatus != ''\">AND fr.sync_status=#{syncStatus}</if>",
            "<if test=\"startDate != null\">AND fr.transaction_date&gt;=#{startDate}</if>","<if test=\"endDate != null\">AND fr.transaction_date&lt;=#{endDate}</if>","</where>","</script>"})
    Long countExpensePage(@Param("keyword") String keyword,@Param("projectName") String projectName,@Param("confirmationStatus") String confirmationStatus,@Param("syncStatus") String syncStatus,@Param("startDate") LocalDate startDate,@Param("endDate") LocalDate endDate);

    @Select("SELECT SUM(confirmation_status='pending') pending_count,SUM(confirmation_status='confirmed') confirmed_count,SUM(confirmation_status='rejected') rejected_count,SUM(confirmation_status='confirmed' AND sync_status IN ('not_synced','pending','failed')) pending_sync_count,COALESCE(SUM(CASE WHEN confirmation_status='pending' THEN amount ELSE 0 END),0) pending_amount,COALESCE(SUM(CASE WHEN confirmation_status='confirmed' AND YEAR(confirmed_at)=YEAR(CURRENT_DATE) AND MONTH(confirmed_at)=MONTH(CURRENT_DATE) THEN amount ELSE 0 END),0) confirmed_month_amount FROM finance_records WHERE record_type IN ('property_expense','reserve_refund','security_deposit')")
    FinanceSummaryRow findExpenseSummary();

    @Select("SELECT DISTINCT p.name FROM finance_records fr JOIN units u ON u.id=fr.unit_id JOIN projects p ON p.id=u.project_id WHERE fr.record_type IN ('property_expense','reserve_refund','security_deposit') ORDER BY p.name")
    List<String> findExpenseProjects();

    @Select("SELECT record_type FROM finance_records WHERE id=#{financeRecordId} FOR UPDATE")
    String lockRecordType(@Param("financeRecordId") Long financeRecordId);

    /** A single finance row used for generating a printable invoice or receipt. */
    @Select("""
            SELECT fr.id, fr.transaction_no, fr.record_type, p.name AS project_name, u.unit_no,
                   COALESCE(pr.payer_name, o.full_name) AS payer_name, fr.amount, fr.currency,
                   fr.transaction_date, fr.payment_method, fr.payment_status, fr.confirmation_status, fr.sync_status,
                   pr.id AS receipt_id, pr.receipt_no, pr.bank_reference, pr.submission_note, pr.review_note,
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

    @Update("UPDATE finance_records SET confirmation_status='confirmed',payment_status='unpaid',sync_status='pending',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='property_expense' AND confirmation_status='pending'")
    int confirmExpense(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Update("UPDATE finance_records SET confirmation_status='rejected',payment_status='voided',sync_status='not_synced',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='property_expense' AND confirmation_status='pending'")
    int rejectExpense(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Update("UPDATE finance_records SET confirmation_status='confirmed',payment_status='paid',sync_status='pending',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='security_deposit' AND confirmation_status='pending'")
    int confirmSecurityDeposit(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Update("UPDATE security_deposit_entries SET status='confirmed' WHERE finance_record_id=#{financeRecordId} AND status='pending'")
    int confirmSecurityDepositEntry(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE finance_records SET confirmation_status='rejected',payment_status='voided',sync_status='not_synced',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='security_deposit' AND confirmation_status='pending'")
    int rejectSecurityDeposit(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Update("UPDATE security_deposit_entries SET status='rejected' WHERE finance_record_id=#{financeRecordId} AND status='pending'")
    int rejectSecurityDepositEntry(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE finance_records SET confirmation_status='rejected',payment_status='voided',sync_status='not_synced',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='reserve_refund' AND confirmation_status='pending'")
    int rejectReserveRefund(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Select("SELECT ra.id AS reserve_account_id, ra.current_balance, fr.amount, fr.owner_id, o.user_id, p.name AS project_name, u.unit_no FROM finance_records fr JOIN owners o ON o.id=fr.owner_id JOIN units u ON u.id=fr.unit_id JOIN projects p ON p.id=u.project_id JOIN owner_units ou ON ou.owner_id=fr.owner_id AND ou.unit_id=fr.unit_id AND ou.status='active' JOIN reserve_accounts ra ON ra.owner_unit_id=ou.id AND ra.status='active' WHERE fr.id=#{financeRecordId} AND fr.record_type='reserve_refund' AND fr.confirmation_status='pending' FOR UPDATE")
    ReserveRefundContext lockReserveRefund(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE finance_records SET confirmation_status='confirmed',payment_status='paid',sync_status='pending',confirmed_by=#{reviewerId},confirmed_at=CURRENT_TIMESTAMP WHERE id=#{financeRecordId} AND record_type='reserve_refund' AND confirmation_status='pending'")
    int confirmReserveRefund(@Param("financeRecordId") Long financeRecordId,@Param("reviewerId") Long reviewerId);

    @Update("UPDATE reserve_accounts SET current_balance=current_balance-#{amount} WHERE id=#{reserveAccountId} AND current_balance>=#{amount}")
    int debitReserveBalance(@Param("reserveAccountId") Long reserveAccountId,@Param("amount") BigDecimal amount);

    @Insert("INSERT INTO reserve_transactions (reserve_account_id,finance_record_id,transaction_type,amount,occurred_at,balance_after,note,created_by) VALUES (#{reserveAccountId},#{financeRecordId},'debit',#{amount},CURRENT_TIMESTAMP,#{balanceAfter},'業主預備金返還',#{reviewerId})")
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
        private Long receiptId; private String receiptNo; private String bankReference; private String submissionNote; private String reviewNote;
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

    class ProofFile {
        private Long id; private String originalName; private String storageKey; private String mimeType; private Long fileSize;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public String getOriginalName() { return originalName; } public void setOriginalName(String v) { originalName = v; }
        public String getStorageKey() { return storageKey; } public void setStorageKey(String v) { storageKey = v; }
        public String getMimeType() { return mimeType; } public void setMimeType(String v) { mimeType = v; }
        public Long getFileSize() { return fileSize; } public void setFileSize(Long v) { fileSize = v; }
    }
}
