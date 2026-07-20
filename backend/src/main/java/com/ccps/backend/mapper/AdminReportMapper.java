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
              ('FINANCE', '財務確認記錄', 'finance', 'XLSX', JSON_OBJECT(), 1),
              ('SYNC', 'SQL Account 匯出結果', 'sync', 'PDF', JSON_OBJECT(), 1)
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
            SELECT rr.id, rr.report_definition_id AS definition_id, rr.report_name,
                   u.display_name AS requested_by_name, rr.date_start, rr.date_end,
                   rr.project_id, p.name AS project_name, rr.output_format, rr.status,
                   rr.record_count, rr.storage_key, rr.error_message, rr.started_at,
                   rr.completed_at, rr.created_at
            FROM report_runs rr LEFT JOIN users u ON u.id = rr.requested_by
            LEFT JOIN projects p ON p.id = rr.project_id
            ORDER BY rr.created_at DESC, rr.id DESC LIMIT 100
            """)
    List<RunRow> findRuns();

    @Select("""
            SELECT rr.id, rr.report_definition_id AS definition_id, rr.report_name,
                   u.display_name AS requested_by_name, rr.date_start, rr.date_end,
                   rr.project_id, p.name AS project_name, rr.output_format, rr.status,
                   rr.record_count, rr.storage_key, rr.error_message, rr.started_at,
                   rr.completed_at, rr.created_at
            FROM report_runs rr LEFT JOIN users u ON u.id = rr.requested_by
            LEFT JOIN projects p ON p.id = rr.project_id WHERE rr.id = #{runId}
            """)
    RunRow findRun(@Param("runId") Long runId);

    @Select("SELECT id, name FROM projects WHERE status = 'active' ORDER BY name")
    List<ProjectRow> findProjects();

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
            <choose>
              <when test="reportType == 'property_payment'">AND fr.record_type = 'property_payment'</when>
              <when test="reportType == 'rent_collection'">AND fr.record_type = 'rent_payment'</when>
              <when test="reportType == 'income_expense'">AND fr.confirmation_status = 'confirmed'</when>
            </choose>
            ORDER BY fr.transaction_date, fr.id
            </script>
            """)
    List<ReportDataRow> findFinanceReportRows(@Param("reportType") String reportType,
                                              @Param("start") LocalDate start, @Param("end") LocalDate end,
                                              @Param("projectId") Long projectId);

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
            ORDER BY m.requested_at, m.id
            </script>
            """)
    List<ReportDataRow> findMaintenanceRows(@Param("start") LocalDate start, @Param("end") LocalDate end,
                                            @Param("projectId") Long projectId);

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
            ORDER BY rt.occurred_at, rt.id
            </script>
            """)
    List<ReportDataRow> findReserveRows(@Param("start") LocalDate start, @Param("end") LocalDate end,
                                        @Param("projectId") Long projectId);

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
    class RunRow { private Long id,definitionId,projectId; private String reportName,requestedByName,projectName,outputFormat,status,storageKey,errorMessage; private Integer recordCount; private LocalDate dateStart,dateEnd; private LocalDateTime startedAt,completedAt,createdAt;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getDefinitionId(){return definitionId;} public void setDefinitionId(Long v){definitionId=v;} public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;}
        public String getReportName(){return reportName;} public void setReportName(String v){reportName=v;} public String getRequestedByName(){return requestedByName;} public void setRequestedByName(String v){requestedByName=v;}
        public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;} public String getOutputFormat(){return outputFormat;} public void setOutputFormat(String v){outputFormat=v;}
        public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getStorageKey(){return storageKey;} public void setStorageKey(String v){storageKey=v;}
        public String getErrorMessage(){return errorMessage;} public void setErrorMessage(String v){errorMessage=v;} public Integer getRecordCount(){return recordCount;} public void setRecordCount(Integer v){recordCount=v;}
        public LocalDate getDateStart(){return dateStart;} public void setDateStart(LocalDate v){dateStart=v;} public LocalDate getDateEnd(){return dateEnd;} public void setDateEnd(LocalDate v){dateEnd=v;}
        public LocalDateTime getStartedAt(){return startedAt;} public void setStartedAt(LocalDateTime v){startedAt=v;} public LocalDateTime getCompletedAt(){return completedAt;} public void setCompletedAt(LocalDateTime v){completedAt=v;}
        public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;} }
    class ProjectRow { private Long id; private String name; public Long getId(){return id;} public void setId(Long v){id=v;} public String getName(){return name;} public void setName(String v){name=v;} }
    class ReportDataRow { private LocalDate recordDate; private String referenceNo,category,projectName,unitNo,partyName,description,status,extraStatus; private BigDecimal amount;
        public LocalDate getRecordDate(){return recordDate;} public void setRecordDate(LocalDate v){recordDate=v;} public String getReferenceNo(){return referenceNo;} public void setReferenceNo(String v){referenceNo=v;}
        public String getCategory(){return category;} public void setCategory(String v){category=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;}
        public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} public String getPartyName(){return partyName;} public void setPartyName(String v){partyName=v;}
        public String getDescription(){return description;} public void setDescription(String v){description=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
        public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getExtraStatus(){return extraStatus;} public void setExtraStatus(String v){extraStatus=v;} }
    class NewRun { private Long id,definitionId,requestedBy,projectId; private String reportName,outputFormat,filtersJson; private LocalDate dateStart,dateEnd;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getDefinitionId(){return definitionId;} public void setDefinitionId(Long v){definitionId=v;}
        public Long getRequestedBy(){return requestedBy;} public void setRequestedBy(Long v){requestedBy=v;} public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;}
        public String getReportName(){return reportName;} public void setReportName(String v){reportName=v;} public String getOutputFormat(){return outputFormat;} public void setOutputFormat(String v){outputFormat=v;}
        public String getFiltersJson(){return filtersJson;} public void setFiltersJson(String v){filtersJson=v;} public LocalDate getDateStart(){return dateStart;} public void setDateStart(LocalDate v){dateStart=v;}
        public LocalDate getDateEnd(){return dateEnd;} public void setDateEnd(LocalDate v){dateEnd=v;} }
}
