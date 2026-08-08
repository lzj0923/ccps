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

@Mapper
public interface AdminReportMapper {
    @Insert("""
            INSERT IGNORE INTO report_definitions
              (report_code, name, report_type, default_format, default_filters, enabled)
            VALUES
              ('PROPERTY_PAYMENT', '房款收款與未收款', 'property_payment', 'XLSX', JSON_OBJECT(), 1),
              ('RENT_COLLECTION', '租金收款進度', 'rent_collection', 'XLSX', JSON_OBJECT(), 1),
              ('INCOME_EXPENSE', '收入與支出明細', 'income_expense', 'XLSX', JSON_OBJECT(), 1),
              ('MAINTENANCE', '維修費用統計', 'maintenance', 'PDF', JSON_OBJECT(), 1),
              ('RESERVE', '預備金餘額及流水', 'reserve', 'XLSX', JSON_OBJECT(), 1),
              ('RESERVE_REFUND', '業主預備金返還清單', 'reserve_refund', 'XLSX', JSON_OBJECT(), 1),
              ('FINANCE', '財務確認記錄', 'finance', 'XLSX', JSON_OBJECT(), 1),
              ('SYNC', 'SQL Account 匯出結果', 'sync', 'PDF', JSON_OBJECT(), 1),
              ('OWNER_STATEMENT', '業主帳單', 'owner_statement', 'XLSX', JSON_OBJECT(), 1),
              ('TENANT_STATEMENT', '租客帳單', 'tenant_statement', 'PDF', JSON_OBJECT(), 1)
            """)
    int ensureDefinitions();

    @Select("""
            SELECT (SELECT COUNT(*) FROM report_definitions WHERE enabled = 1) AS definition_count,
                   (SELECT COUNT(*) FROM report_runs WHERE status = 'completed') AS completed_count,
                   (SELECT COUNT(*) FROM report_runs WHERE status = 'failed') AS failed_count,
                   (SELECT COUNT(*) FROM report_runs WHERE status = 'completed'
                     AND YEAR(created_at) = YEAR(CURDATE()) AND MONTH(created_at) = MONTH(CURDATE())) AS generated_this_month
            """)
    SummaryRow findSummary();

    @Select("""
            SELECT id, report_code, name, report_type, default_format,
                   CAST(default_filters AS CHAR) AS default_filters, schedule_cron, enabled, updated_at
            FROM report_definitions ORDER BY id
            """)
    List<DefinitionRow> findDefinitions();

    @Select("SELECT id, report_code, name, report_type, default_format, CAST(default_filters AS CHAR) AS default_filters, schedule_cron, enabled, updated_at FROM report_definitions WHERE report_type = #{reportType} AND enabled = 1 LIMIT 1")
    DefinitionRow findDefinitionByType(@Param("reportType") String reportType);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM report_runs rr LEFT JOIN users u ON u.id = rr.requested_by
            LEFT JOIN projects p ON p.id = rr.project_id
            LEFT JOIN owners filter_owner ON filter_owner.id = CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(rr.filters, '$.ownerId')), 'null') AS UNSIGNED)
            LEFT JOIN tenants filter_tenant ON filter_tenant.id = CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(rr.filters, '$.tenantId')), 'null') AS UNSIGNED)
            LEFT JOIN units filter_unit ON filter_unit.id = CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(rr.filters, '$.unitId')), 'null') AS UNSIGNED)
            LEFT JOIN projects filter_unit_project ON filter_unit_project.id = filter_unit.project_id
            WHERE 1 = 1
            <if test="keyword != null">
              AND (rr.report_name LIKE CONCAT('%', #{keyword}, '%')
                   OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                   OR p.name LIKE CONCAT('%', #{keyword}, '%')
                   OR filter_owner.full_name LIKE CONCAT('%', #{keyword}, '%')
                   OR filter_tenant.full_name LIKE CONCAT('%', #{keyword}, '%')
                   OR filter_unit.unit_no LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="project != null">AND p.name = #{project}</if>
            <if test="status != null and status != 'active'">AND rr.status = #{status}</if>
            <if test="status == 'active'">AND rr.status IN ('queued', 'processing')</if>
            </script>
            """)
    long countRuns(@Param("keyword") String keyword, @Param("project") String project,
                   @Param("status") String status);

    @Select("""
            <script>
            SELECT rr.id, rr.report_definition_id AS definition_id, rr.report_name,
                   u.display_name AS requested_by_name, rr.date_start, rr.date_end,
                   rr.project_id, p.name AS project_name, rr.output_format, rr.status,
                   CASE WHEN rr.project_id IS NOT NULL THEN 'project'
                        WHEN filter_owner.id IS NOT NULL THEN 'owner'
                        WHEN filter_tenant.id IS NOT NULL THEN 'tenant'
                        WHEN filter_unit.id IS NOT NULL THEN 'unit' ELSE 'all' END AS scope_type,
                   COALESCE(p.name, filter_owner.full_name, filter_tenant.full_name,
                            CONCAT(filter_unit_project.name, ' · ', filter_unit.unit_no), '全部範圍') AS scope_name,
                   rr.record_count, rr.storage_key, rr.error_message, rr.started_at,
                   rr.completed_at, rr.created_at
            FROM report_runs rr LEFT JOIN users u ON u.id = rr.requested_by
            LEFT JOIN projects p ON p.id = rr.project_id
            LEFT JOIN owners filter_owner ON filter_owner.id = CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(rr.filters, '$.ownerId')), 'null') AS UNSIGNED)
            LEFT JOIN tenants filter_tenant ON filter_tenant.id = CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(rr.filters, '$.tenantId')), 'null') AS UNSIGNED)
            LEFT JOIN units filter_unit ON filter_unit.id = CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(rr.filters, '$.unitId')), 'null') AS UNSIGNED)
            LEFT JOIN projects filter_unit_project ON filter_unit_project.id = filter_unit.project_id
            WHERE 1 = 1
            <if test="keyword != null">
              AND (rr.report_name LIKE CONCAT('%', #{keyword}, '%')
                   OR u.display_name LIKE CONCAT('%', #{keyword}, '%')
                   OR p.name LIKE CONCAT('%', #{keyword}, '%')
                   OR filter_owner.full_name LIKE CONCAT('%', #{keyword}, '%')
                   OR filter_tenant.full_name LIKE CONCAT('%', #{keyword}, '%')
                   OR filter_unit.unit_no LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="project != null">AND p.name = #{project}</if>
            <if test="status != null and status != 'active'">AND rr.status = #{status}</if>
            <if test="status == 'active'">AND rr.status IN ('queued', 'processing')</if>
            ORDER BY rr.created_at DESC, rr.id DESC LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<RunRow> findRuns(@Param("keyword") String keyword, @Param("project") String project,
                          @Param("status") String status, @Param("limit") int limit,
                          @Param("offset") int offset);

    @Select("""
            SELECT rr.id, rr.report_definition_id AS definition_id, rr.report_name,
                   u.display_name AS requested_by_name, rr.date_start, rr.date_end,
                   rr.project_id, p.name AS project_name, rr.output_format, rr.status,
                   CASE WHEN rr.project_id IS NOT NULL THEN 'project'
                        WHEN filter_owner.id IS NOT NULL THEN 'owner'
                        WHEN filter_tenant.id IS NOT NULL THEN 'tenant'
                        WHEN filter_unit.id IS NOT NULL THEN 'unit' ELSE 'all' END AS scope_type,
                   COALESCE(p.name, filter_owner.full_name, filter_tenant.full_name,
                            CONCAT(filter_unit_project.name, ' · ', filter_unit.unit_no), '全部範圍') AS scope_name,
                   rr.record_count, rr.storage_key, rr.error_message, rr.started_at,
                   rr.completed_at, rr.created_at
            FROM report_runs rr LEFT JOIN users u ON u.id = rr.requested_by
            LEFT JOIN projects p ON p.id = rr.project_id
            LEFT JOIN owners filter_owner ON filter_owner.id = CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(rr.filters, '$.ownerId')), 'null') AS UNSIGNED)
            LEFT JOIN tenants filter_tenant ON filter_tenant.id = CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(rr.filters, '$.tenantId')), 'null') AS UNSIGNED)
            LEFT JOIN units filter_unit ON filter_unit.id = CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(rr.filters, '$.unitId')), 'null') AS UNSIGNED)
            LEFT JOIN projects filter_unit_project ON filter_unit_project.id = filter_unit.project_id
            WHERE rr.id = #{runId}
            """)
    RunRow findRun(@Param("runId") Long runId);

    @Select("SELECT id, name FROM projects WHERE status = 'active' ORDER BY name")
    List<ProjectRow> findProjects();

    @Select("SELECT id, full_name AS name FROM owners WHERE status = 'active' ORDER BY full_name, id")
    List<OwnerRow> findOwners();

    @Select("SELECT id, full_name AS name FROM tenants WHERE status = 'active' ORDER BY full_name, id")
    List<TenantRow> findTenants();

    @Select("SELECT u.id, u.project_id, p.name AS project_name, u.unit_no FROM units u JOIN projects p ON p.id=u.project_id WHERE p.status='active' ORDER BY p.name,u.unit_no,u.id")
    List<UnitRow> findUnits();

    @Insert("""
            INSERT INTO report_runs
              (report_definition_id, report_name, requested_by, date_start, date_end,
               project_id, output_format, filters, status, started_at)
            VALUES (#{definitionId}, #{reportName}, #{requestedBy}, #{dateStart}, #{dateEnd},
                    #{projectId}, #{outputFormat}, CAST(#{filtersJson} AS JSON), 'processing', NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRun(NewRun row);

    @Update("""
            UPDATE report_runs SET status = 'completed', record_count = #{recordCount},
              storage_key = #{storageKey}, error_message = NULL, completed_at = NOW()
            WHERE id = #{runId}
            """)
    int completeRun(@Param("runId") Long runId, @Param("recordCount") int recordCount,
                    @Param("storageKey") String storageKey);

    @Update("UPDATE report_runs SET status = 'failed', error_message = LEFT(#{error}, 1000), completed_at = NOW() WHERE id = #{runId}")
    int failRun(@Param("runId") Long runId, @Param("error") String error);

    @Select("""
            <script>
            SELECT fr.transaction_date AS record_date, fr.transaction_no AS reference_no,
                   fr.record_type AS category, p.name AS project_name, u.unit_no,
                   COALESCE(o.full_name, t.full_name, '—') AS party_name,
                   CONCAT(COALESCE(fr.payment_method, '—'), ' / ', fr.currency) AS description,
                   fr.amount, fr.payment_status AS status, fr.sync_status AS extra_status
            FROM finance_records fr LEFT JOIN units u ON u.id = fr.unit_id
            LEFT JOIN projects p ON p.id = u.project_id LEFT JOIN owners o ON o.id = fr.owner_id
            LEFT JOIN tenants t ON t.id = fr.tenant_id
            WHERE fr.transaction_date BETWEEN #{start} AND #{end}
            <if test="projectId != null">AND p.id = #{projectId}</if>
            <if test="ownerId != null">AND (fr.owner_id = #{ownerId} OR EXISTS (SELECT 1 FROM owner_units filter_ou WHERE filter_ou.unit_id=fr.unit_id AND filter_ou.owner_id=#{ownerId} AND filter_ou.status='active'))</if>
            <if test="unitId != null">AND fr.unit_id = #{unitId}</if>
            <choose>
              <when test="reportType == 'property_payment'">AND fr.record_type = 'property_payment'</when>
              <when test="reportType == 'rent_collection'">AND fr.record_type = 'rent_payment'</when>
              <when test="reportType == 'reserve_refund'">AND fr.record_type = 'reserve_refund'</when>
              <when test="reportType == 'income_expense'">AND fr.confirmation_status = 'confirmed'</when>
            </choose>
            ORDER BY fr.transaction_date, fr.id
            </script>
            """)
    List<ReportDataRow> findFinanceReportRows(@Param("reportType") String reportType,
                                               @Param("start") LocalDate start, @Param("end") LocalDate end,
                                               @Param("projectId") Long projectId,
                                               @Param("ownerId") Long ownerId, @Param("unitId") Long unitId);

    @Select("""
            SELECT ri.billing_month AS record_date, CONCAT('INV-', LPAD(ri.id, 6, '0')) AS reference_no,
                   '租金帳單' AS category, p.name AS project_name, u.unit_no,
                   t.full_name AS party_name,
                   CONCAT('租約 ', l.lease_no, ' · 到期日 ', DATE_FORMAT(ri.due_date, '%Y-%m-%d')) AS description,
                   ri.amount_due AS amount,
                   CASE WHEN ri.amount_paid >= ri.amount_due THEN 'paid'
                        WHEN ri.amount_paid > 0 THEN 'partial'
                        WHEN ri.due_date < CURRENT_DATE THEN 'overdue' ELSE 'unpaid' END AS status,
                   CONCAT('已收 RM ', FORMAT(ri.amount_paid, 2),
                          ' · 未收 RM ', FORMAT(GREATEST(ri.amount_due - ri.amount_paid, 0), 2)) AS extra_status
            FROM rent_invoices ri
            JOIN leases l ON l.id = ri.lease_id
            JOIN tenants t ON t.id = l.tenant_id
            JOIN units u ON u.id = l.unit_id
            JOIN projects p ON p.id = u.project_id
            WHERE t.id = #{tenantId} AND ri.billing_month BETWEEN #{start} AND #{end}
            ORDER BY ri.billing_month, ri.id
            """)
    List<ReportDataRow> findTenantStatementRows(@Param("start") LocalDate start,
                                                 @Param("end") LocalDate end,
                                                 @Param("tenantId") Long tenantId);

    @Select("""
            <script>
            SELECT CONCAT(
                     CASE WHEN COALESCE(ce.direction, CASE WHEN fr.record_type='reserve_debit' THEN 'expense' ELSE 'income' END)='expense'
                          THEN '支出' ELSE '收入' END,
                     ' ', CASE COALESCE(ce.category, fr.record_type)
                       WHEN 'rent' THEN '租金'
                       WHEN 'maintenance' THEN '維修費'
                       WHEN 'utilities' THEN '水電雜費'
                       WHEN 'management' THEN '管理費'
                       WHEN 'cleaning' THEN '清潔費'
                       WHEN 'deposit' THEN '押金'
                       WHEN 'service_fee' THEN '服務費'
                       WHEN 'insurance' THEN '保險'
                       WHEN 'tax' THEN '稅費'
                       WHEN 'balance_refund' THEN '餘額退款'
                       WHEN 'reserve_topup' THEN '預備金充值'
                       WHEN 'reserve_debit' THEN '預備金支出'
                       ELSE COALESCE(ce.category, fr.record_type)
                     END
                   ) AS item,
                   p.name AS object_name, u.unit_no AS item_name,
                   COALESCE(
                     NULLIF(ce.description,''),
                     NULLIF(pr.submission_note,''),
                     NULLIF(pr.review_note,''),
                     CASE COALESCE(ce.category, fr.record_type)
                       WHEN 'rent' THEN '租金'
                       WHEN 'maintenance' THEN '維修費'
                       WHEN 'utilities' THEN '水電雜費'
                       WHEN 'management' THEN '管理費'
                       WHEN 'cleaning' THEN '清潔費'
                       WHEN 'deposit' THEN '押金'
                       WHEN 'service_fee' THEN '服務費'
                       WHEN 'insurance' THEN '保險'
                       WHEN 'tax' THEN '稅費'
                       WHEN 'balance_refund' THEN '餘額退款'
                       WHEN 'reserve_topup' THEN '預備金充值'
                       WHEN 'reserve_debit' THEN '預備金支出'
                       ELSE COALESCE(ce.category, fr.record_type)
                     END,
                     CASE fr.record_type
                       WHEN 'property_payment' THEN '房款'
                       WHEN 'rent_payment' THEN '租金'
                       WHEN 'reserve_topup' THEN '預備金充值'
                       WHEN 'reserve_debit' THEN '預備金支出'
                       ELSE '收支交易'
                     END
                   ) AS payment_name,
                   fr.payment_status AS status, fr.payment_method,
                   fr.transaction_date AS real_date, DATE(fr.confirmed_at) AS pay_date,
                   CASE WHEN COALESCE(ce.direction, CASE WHEN fr.record_type='reserve_debit' THEN 'expense' ELSE 'income' END)='income'
                        THEN fr.amount ELSE 0 END AS income,
                   CASE WHEN COALESCE(ce.direction, CASE WHEN fr.record_type='reserve_debit' THEN 'expense' ELSE 'income' END)='expense'
                        THEN fr.amount ELSE 0 END AS expense,
                   CAST(0 AS DECIMAL(18,2)) AS balance,
                   fr.currency, lease_context.start_date AS lease_start, lease_context.end_date AS lease_end,
                   COALESCE(NULLIF(pr.submission_note,''), NULLIF(ce.description,''), NULLIF(pr.review_note,''), '') AS note,
                   COALESCE(
                     NULLIF(CONCAT_WS(CHAR(10),
                       CASE WHEN property_bank.item_name IS NULL OR property_bank.item_name='' THEN NULL ELSE CONCAT('銀行名稱: ', property_bank.item_name) END,
                       CASE WHEN property_bank.payment_name IS NULL OR property_bank.payment_name='' THEN NULL ELSE CONCAT('銀行帳戶: ', property_bank.payment_name) END,
                       CASE WHEN property_bank.account_no IS NULL OR property_bank.account_no='' THEN NULL ELSE CONCAT('銀行帳號: ', property_bank.account_no) END
                     ), ''),
                     NULLIF(pr.bank_reference,''),
                     ''
                   ) AS bank_info
            FROM finance_records fr
            LEFT JOIN cashflow_entries ce ON ce.finance_record_id=fr.id
            LEFT JOIN units u ON u.id=fr.unit_id
            LEFT JOIN projects p ON p.id=u.project_id
            LEFT JOIN owners o ON o.id=fr.owner_id
            LEFT JOIN tenants t ON t.id=fr.tenant_id
            LEFT JOIN vendors v ON v.id=ce.vendor_id
            LEFT JOIN payment_receipts pr ON pr.finance_record_id=fr.id
            LEFT JOIN rent_payments rent_payment ON rent_payment.finance_record_id=fr.id
            LEFT JOIN rent_invoices rent_invoice ON rent_invoice.id=rent_payment.rent_invoice_id
            LEFT JOIN leases lease_context ON lease_context.id=COALESCE(
              rent_invoice.lease_id,
              (SELECT l2.id FROM leases l2 WHERE l2.unit_id=fr.unit_id
                 AND fr.transaction_date BETWEEN l2.start_date AND l2.end_date
               ORDER BY CASE WHEN l2.tenant_id=fr.tenant_id THEN 0 ELSE 1 END,l2.id DESC LIMIT 1))
            LEFT JOIN owner_units report_ou ON report_ou.id=(
              SELECT ou2.id FROM owner_units ou2 WHERE ou2.unit_id=fr.unit_id
                AND (fr.owner_id IS NULL OR ou2.owner_id=fr.owner_id)
              ORDER BY ou2.is_primary DESC,ou2.id DESC LIMIT 1)
            LEFT JOIN property_bank_accounts property_bank ON property_bank.id=(
              SELECT pba2.id FROM property_bank_accounts pba2 WHERE pba2.owner_unit_id=report_ou.id ORDER BY pba2.id DESC LIMIT 1)
            WHERE fr.confirmation_status='confirmed'
              AND fr.transaction_date BETWEEN #{start} AND #{end}
            <if test="projectId != null">AND p.id=#{projectId}</if>
            <if test="ownerId != null">AND (fr.owner_id=#{ownerId} OR EXISTS (SELECT 1 FROM owner_units filter_ou WHERE filter_ou.unit_id=fr.unit_id AND filter_ou.owner_id=#{ownerId} AND filter_ou.status='active'))</if>
            <if test="unitId != null">AND fr.unit_id=#{unitId}</if>
            ORDER BY fr.transaction_date,fr.id
            </script>
            """)
    List<ReportDataRow> findIncomeExpenseRows(@Param("start") LocalDate start, @Param("end") LocalDate end,
                                               @Param("projectId") Long projectId,
                                               @Param("ownerId") Long ownerId, @Param("unitId") Long unitId);

    @Select("""
            <script>
            SELECT DATE(m.requested_at) AS record_date, m.work_order_no AS reference_no,
                   m.category, p.name AS project_name, u.unit_no, v.name AS party_name,
                   m.title AS description, COALESCE(m.actual_amount, m.estimated_amount, 0) AS amount,
                   m.status, CASE WHEN m.completed_at IS NULL THEN '未完成' ELSE '已完成' END AS extra_status
            FROM maintenance_work_orders m JOIN units u ON u.id = m.unit_id JOIN projects p ON p.id = u.project_id
            LEFT JOIN vendors v ON v.id = m.vendor_id
            WHERE DATE(m.requested_at) BETWEEN #{start} AND #{end}
            <if test="projectId != null">AND p.id = #{projectId}</if>
            <if test="ownerId != null">AND (m.owner_id=#{ownerId} OR EXISTS (SELECT 1 FROM owner_units filter_ou WHERE filter_ou.unit_id=m.unit_id AND filter_ou.owner_id=#{ownerId} AND filter_ou.status='active'))</if>
            <if test="unitId != null">AND m.unit_id=#{unitId}</if>
            ORDER BY m.requested_at, m.id
            </script>
            """)
    List<ReportDataRow> findMaintenanceRows(@Param("start") LocalDate start, @Param("end") LocalDate end,
                                             @Param("projectId") Long projectId,
                                             @Param("ownerId") Long ownerId, @Param("unitId") Long unitId);

    @Select("""
            <script>
            SELECT DATE(rt.occurred_at) AS record_date, CONCAT('RES-', rt.id) AS reference_no,
                   rt.transaction_type AS category, p.name AS project_name, u.unit_no,
                   o.full_name AS party_name, COALESCE(rt.note, '預備金異動') AS description,
                   rt.amount, ra.status, CONCAT('餘額 RM ', rt.balance_after) AS extra_status
            FROM reserve_transactions rt JOIN reserve_accounts ra ON ra.id = rt.reserve_account_id
            JOIN owner_units ou ON ou.id = ra.owner_unit_id JOIN owners o ON o.id = ou.owner_id
            JOIN units u ON u.id = ou.unit_id JOIN projects p ON p.id = u.project_id
            WHERE DATE(rt.occurred_at) BETWEEN #{start} AND #{end}
            <if test="projectId != null">AND p.id = #{projectId}</if>
            <if test="ownerId != null">AND ou.owner_id=#{ownerId}</if>
            <if test="unitId != null">AND ou.unit_id=#{unitId}</if>
            ORDER BY rt.occurred_at, rt.id
            </script>
            """)
    List<ReportDataRow> findReserveRows(@Param("start") LocalDate start, @Param("end") LocalDate end,
                                         @Param("projectId") Long projectId,
                                         @Param("ownerId") Long ownerId, @Param("unitId") Long unitId);

    @Select("""
            SELECT DATE(sb.created_at) AS record_date, sb.batch_no AS reference_no,
                   sb.source_module AS category, '—' AS project_name, '—' AS unit_no,
                   COALESCE(u.display_name, '系統') AS party_name, sb.trigger_mode AS description,
                   sb.total_count AS amount, sb.status,
                   CONCAT('成功 ', sb.success_count, ' / 失敗 ', sb.failure_count) AS extra_status
            FROM sync_batches sb LEFT JOIN users u ON u.id = sb.created_by
            WHERE DATE(sb.created_at) BETWEEN #{start} AND #{end}
            ORDER BY sb.created_at, sb.id
            """)
    List<ReportDataRow> findSyncRows(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, after_data)
            VALUES (#{actorId}, 'generate_report', 'report_run', #{runId}, JSON_OBJECT('reportName', #{reportName}))
            """)
    int insertAudit(@Param("actorId") Long actorId, @Param("runId") Long runId,
                    @Param("reportName") String reportName);

    class SummaryRow { private Long definitionCount, completedCount, failedCount, generatedThisMonth;
        public Long getDefinitionCount(){return definitionCount;} public void setDefinitionCount(Long v){definitionCount=v;}
        public Long getCompletedCount(){return completedCount;} public void setCompletedCount(Long v){completedCount=v;}
        public Long getFailedCount(){return failedCount;} public void setFailedCount(Long v){failedCount=v;}
        public Long getGeneratedThisMonth(){return generatedThisMonth;} public void setGeneratedThisMonth(Long v){generatedThisMonth=v;} }
    class DefinitionRow { private Long id; private String reportCode,name,reportType,defaultFormat,defaultFilters,scheduleCron; private Boolean enabled; private LocalDateTime updatedAt;
        public Long getId(){return id;} public void setId(Long v){id=v;} public String getReportCode(){return reportCode;} public void setReportCode(String v){reportCode=v;}
        public String getName(){return name;} public void setName(String v){name=v;} public String getReportType(){return reportType;} public void setReportType(String v){reportType=v;}
        public String getDefaultFormat(){return defaultFormat;} public void setDefaultFormat(String v){defaultFormat=v;} public String getDefaultFilters(){return defaultFilters;} public void setDefaultFilters(String v){defaultFilters=v;}
        public String getScheduleCron(){return scheduleCron;} public void setScheduleCron(String v){scheduleCron=v;} public Boolean getEnabled(){return enabled;} public void setEnabled(Boolean v){enabled=v;}
        public LocalDateTime getUpdatedAt(){return updatedAt;} public void setUpdatedAt(LocalDateTime v){updatedAt=v;} }
    class RunRow { private Long id,definitionId,projectId; private String reportName,requestedByName,projectName,scopeType,scopeName,outputFormat,status,storageKey,errorMessage; private Integer recordCount; private LocalDate dateStart,dateEnd; private LocalDateTime startedAt,completedAt,createdAt;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getDefinitionId(){return definitionId;} public void setDefinitionId(Long v){definitionId=v;} public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;}
        public String getReportName(){return reportName;} public void setReportName(String v){reportName=v;} public String getRequestedByName(){return requestedByName;} public void setRequestedByName(String v){requestedByName=v;}
        public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;} public String getOutputFormat(){return outputFormat;} public void setOutputFormat(String v){outputFormat=v;}
        public String getScopeType(){return scopeType;} public void setScopeType(String v){scopeType=v;} public String getScopeName(){return scopeName;} public void setScopeName(String v){scopeName=v;}
        public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getStorageKey(){return storageKey;} public void setStorageKey(String v){storageKey=v;}
        public String getErrorMessage(){return errorMessage;} public void setErrorMessage(String v){errorMessage=v;} public Integer getRecordCount(){return recordCount;} public void setRecordCount(Integer v){recordCount=v;}
        public LocalDate getDateStart(){return dateStart;} public void setDateStart(LocalDate v){dateStart=v;} public LocalDate getDateEnd(){return dateEnd;} public void setDateEnd(LocalDate v){dateEnd=v;}
        public LocalDateTime getStartedAt(){return startedAt;} public void setStartedAt(LocalDateTime v){startedAt=v;} public LocalDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(LocalDateTime v){completedAt=v;}
        public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} }
    class ProjectRow { private Long id; private String name; public Long getId(){return id;} public void setId(Long v){id=v;} public String getName(){return name;} public void setName(String v){name=v;} }
    class OwnerRow { private Long id; private String name; public Long getId(){return id;} public void setId(Long v){id=v;} public String getName(){return name;} public void setName(String v){name=v;} }
    class TenantRow { private Long id; private String name; public Long getId(){return id;} public void setId(Long v){id=v;} public String getName(){return name;} public void setName(String v){name=v;} }
    class UnitRow { private Long id,projectId; private String projectName,unitNo;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;}
        public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;} public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} }
    class ReportDataRow { private LocalDate recordDate,realDate,payDate,leaseStart,leaseEnd; private String referenceNo,category,projectName,unitNo,partyName,description,status,extraStatus,item,objectName,itemName,paymentName,paymentMethod,currency,note,bankInfo; private BigDecimal amount,income,expense,balance;
        public LocalDate getRecordDate(){return recordDate;} public void setRecordDate(LocalDate v){recordDate=v;} public String getReferenceNo(){return referenceNo;} public void setReferenceNo(String v){referenceNo=v;}
        public String getCategory(){return category;} public void setCategory(String v){category=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;}
        public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} public String getPartyName(){return partyName;} public void setPartyName(String v){partyName=v;}
        public String getDescription(){return description;} public void setDescription(String v){description=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
        public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getExtraStatus(){return extraStatus;} public void setExtraStatus(String v){extraStatus=v;}
        public String getItem(){return item;} public void setItem(String v){item=v;} public String getObjectName(){return objectName;} public void setObjectName(String v){objectName=v;}
        public String getItemName(){return itemName;} public void setItemName(String v){itemName=v;} public String getPaymentName(){return paymentName;} public void setPaymentName(String v){paymentName=v;}
        public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String v){paymentMethod=v;} public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;}
        public String getNote(){return note;} public void setNote(String v){note=v;} public String getBankInfo(){return bankInfo;} public void setBankInfo(String v){bankInfo=v;}
        public LocalDate getRealDate(){return realDate;} public void setRealDate(LocalDate v){realDate=v;} public LocalDate getPayDate(){return payDate;} public void setPayDate(LocalDate v){payDate=v;}
        public LocalDate getLeaseStart(){return leaseStart;} public void setLeaseStart(LocalDate v){leaseStart=v;} public LocalDate getLeaseEnd(){return leaseEnd;} public void setLeaseEnd(LocalDate v){leaseEnd=v;}
        public BigDecimal getIncome(){return income;} public void setIncome(BigDecimal v){income=v;} public BigDecimal getExpense(){return expense;} public void setExpense(BigDecimal v){expense=v;}
        public BigDecimal getBalance(){return balance;} public void setBalance(BigDecimal v){balance=v;} }
    class NewRun { private Long id,definitionId,requestedBy,projectId; private String reportName,outputFormat,filtersJson; private LocalDate dateStart,dateEnd;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getDefinitionId(){return definitionId;} public void setDefinitionId(Long v){definitionId=v;}
        public Long getRequestedBy(){return requestedBy;} public void setRequestedBy(Long v){requestedBy=v;} public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;}
        public String getReportName(){return reportName;} public void setReportName(String v){reportName=v;} public String getOutputFormat(){return outputFormat;} public void setOutputFormat(String v){outputFormat=v;}
        public String getFiltersJson(){return filtersJson;} public void setFiltersJson(String v){filtersJson=v;} public LocalDate getDateStart(){return dateStart;} public void setDateStart(LocalDate v){dateStart=v;}
        public LocalDate getDateEnd(){return dateEnd;} public void setDateEnd(LocalDate v){dateEnd=v;} }
}
