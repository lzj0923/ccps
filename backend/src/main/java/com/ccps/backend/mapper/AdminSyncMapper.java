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
public interface AdminSyncMapper {
    @Select("""
            SELECT
              (SELECT COUNT(*) FROM finance_records WHERE confirmation_status = 'confirmed'
                AND sync_status IN ('not_synced','failed')) AS eligible_count,
              (SELECT COUNT(*) FROM finance_records WHERE sync_status = 'exported') AS exported_count,
              (SELECT COUNT(*) FROM finance_records WHERE sync_status = 'failed') AS failed_count,
              (SELECT COUNT(*) FROM sync_batches) AS batch_count,
              (SELECT COUNT(*) FROM sync_batch_items) AS item_count
            """)
    SummaryRow findSummary();

    @Select("""
            SELECT sb.id, sb.batch_no, sb.source_module, sb.trigger_mode, sb.status,
                   sb.total_count, sb.success_count, sb.failure_count, sb.started_at,
                   sb.completed_at, u.display_name AS created_by_name, sb.created_at
            FROM sync_batches sb LEFT JOIN users u ON u.id = sb.created_by
            ORDER BY sb.created_at DESC, sb.id DESC LIMIT 100
            """)
    List<BatchRow> findBatches();

    @Select("""
            SELECT si.id, si.batch_id, sb.batch_no, si.entity_type, si.entity_id,
                   si.operation, si.status, si.external_id, si.error_message,
                   JSON_UNQUOTE(JSON_EXTRACT(si.source_payload, '$.transactionNo')) AS transaction_no,
                   si.synced_at
            FROM sync_batch_items si JOIN sync_batches sb ON sb.id = si.batch_id
            ORDER BY si.id DESC LIMIT 500
            """)
    List<ItemRow> findItems();

    @Select("""
            <script>
            SELECT fr.id, fr.transaction_no, fr.record_type, fr.transaction_date,
                   fr.amount, fr.currency, fr.payment_method, fr.sync_status,
                   COALESCE(o.full_name, t.full_name, '未指定往來單位') AS party_name,
                   COALESCE(CONCAT('OWNER-', LPAD(o.id, 6, '0')), CONCAT('TENANT-', LPAD(t.id, 6, '0')), 'GENERAL') AS party_code,
                   p.project_code, p.name AS project_name, u.unit_no
            FROM finance_records fr
            LEFT JOIN owners o ON o.id = fr.owner_id
            LEFT JOIN tenants t ON t.id = fr.tenant_id
            LEFT JOIN units u ON u.id = fr.unit_id
            LEFT JOIN projects p ON p.id = u.project_id
            WHERE fr.confirmation_status = 'confirmed' AND fr.sync_status IN ('not_synced','failed')
            <choose>
              <when test="sourceModule == 'reserve'">AND fr.record_type IN ('reserve_topup','reserve_debit')</when>
              <when test="sourceModule == 'cashflow'">AND fr.record_type = 'cashflow'</when>
              <when test="sourceModule != 'all'">AND fr.record_type = #{sourceModule}</when>
            </choose>
            <if test="recordIds != null and recordIds.size() > 0">
              AND fr.id IN <foreach collection="recordIds" item="id" open="(" separator="," close=")">#{id}</foreach>
            </if>
            ORDER BY fr.transaction_date, fr.id
            LIMIT 1000
            </script>
            """)
    List<FinanceSyncRow> findEligibleRecords(@Param("sourceModule") String sourceModule,
                                             @Param("recordIds") List<Long> recordIds);

    @Insert("""
            INSERT INTO sync_batches
              (batch_no, source_module, trigger_mode, status, total_count, started_at, created_by)
            VALUES (#{batchNo}, #{sourceModule}, 'manual', 'processing', #{totalCount}, NOW(), #{createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertBatch(NewBatch row);

    @Insert("""
            INSERT INTO sync_batch_items
              (batch_id, entity_type, entity_id, operation, status, source_payload)
            VALUES (#{batchId}, 'finance_record', #{entityId}, 'upsert', 'pending', CAST(#{payloadJson} AS JSON))
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertItem(NewItem row);

    @Update("""
            UPDATE sync_batch_items SET status = 'success', external_id = #{externalId},
              error_message = NULL, synced_at = NOW() WHERE id = #{itemId}
            """)
    int markItemSuccess(@Param("itemId") Long itemId, @Param("externalId") String externalId);

    @Update("""
            UPDATE sync_batch_items SET status = 'failed', error_message = LEFT(#{error}, 1000)
            WHERE id = #{itemId}
            """)
    int markItemFailed(@Param("itemId") Long itemId, @Param("error") String error);

    @Update("""
            UPDATE finance_records SET sync_status = #{status}, sync_batch_id = #{batchId}
            WHERE id = #{recordId}
            """)
    int updateFinanceSync(@Param("recordId") Long recordId, @Param("batchId") Long batchId,
                          @Param("status") String status);

    @Update("""
            UPDATE sync_batches SET status = #{status}, success_count = #{successCount},
              failure_count = #{failureCount}, completed_at = NOW() WHERE id = #{batchId}
            """)
    int completeBatch(@Param("batchId") Long batchId, @Param("status") String status,
                      @Param("successCount") int successCount, @Param("failureCount") int failureCount);

    @Select("SELECT id, batch_no, source_module, status, total_count FROM sync_batches WHERE id = #{batchId}")
    BatchRow findBatch(@Param("batchId") Long batchId);

    @Select("""
            SELECT fr.id, fr.transaction_no, fr.record_type, fr.transaction_date,
                   fr.amount, fr.currency, fr.payment_method, fr.sync_status,
                   COALESCE(o.full_name, t.full_name, '未指定往來單位') AS party_name,
                   COALESCE(CONCAT('OWNER-', LPAD(o.id, 6, '0')), CONCAT('TENANT-', LPAD(t.id, 6, '0')), 'GENERAL') AS party_code,
                   p.project_code, p.name AS project_name, u.unit_no
            FROM sync_batch_items si JOIN finance_records fr ON fr.id = si.entity_id
            LEFT JOIN owners o ON o.id = fr.owner_id LEFT JOIN tenants t ON t.id = fr.tenant_id
            LEFT JOIN units u ON u.id = fr.unit_id LEFT JOIN projects p ON p.id = u.project_id
            WHERE si.batch_id = #{batchId} AND si.status = 'failed'
            ORDER BY si.id
            """)
    List<FinanceSyncRow> findFailedBatchRecords(@Param("batchId") Long batchId);

    @Select("SELECT id FROM sync_batch_items WHERE batch_id = #{batchId} AND entity_id = #{entityId} LIMIT 1")
    Long findBatchItemId(@Param("batchId") Long batchId, @Param("entityId") Long entityId);

    @Update("UPDATE sync_batches SET status = 'processing', started_at = NOW(), completed_at = NULL WHERE id = #{batchId}")
    int restartBatch(@Param("batchId") Long batchId);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, after_data)
            VALUES (#{actorId}, #{action}, 'sync_batch', #{batchId}, JSON_OBJECT('batchNo', #{batchNo}))
            """)
    int insertAudit(@Param("actorId") Long actorId, @Param("action") String action,
                    @Param("batchId") Long batchId, @Param("batchNo") String batchNo);

    class SummaryRow {
        private Long eligibleCount, exportedCount, failedCount, batchCount, itemCount;
        public Long getEligibleCount() { return eligibleCount; } public void setEligibleCount(Long v) { eligibleCount = v; }
        public Long getExportedCount() { return exportedCount; } public void setExportedCount(Long v) { exportedCount = v; }
        public Long getFailedCount() { return failedCount; } public void setFailedCount(Long v) { failedCount = v; }
        public Long getBatchCount() { return batchCount; } public void setBatchCount(Long v) { batchCount = v; }
        public Long getItemCount() { return itemCount; } public void setItemCount(Long v) { itemCount = v; }
    }
    class BatchRow {
        private Long id; private String batchNo, sourceModule, triggerMode, status, createdByName;
        private Integer totalCount, successCount, failureCount; private LocalDateTime startedAt, completedAt, createdAt;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public String getBatchNo() { return batchNo; } public void setBatchNo(String v) { batchNo = v; }
        public String getSourceModule() { return sourceModule; } public void setSourceModule(String v) { sourceModule = v; }
        public String getTriggerMode() { return triggerMode; } public void setTriggerMode(String v) { triggerMode = v; }
        public String getStatus() { return status; } public void setStatus(String v) { status = v; }
        public String getCreatedByName() { return createdByName; } public void setCreatedByName(String v) { createdByName = v; }
        public Integer getTotalCount() { return totalCount; } public void setTotalCount(Integer v) { totalCount = v; }
        public Integer getSuccessCount() { return successCount; } public void setSuccessCount(Integer v) { successCount = v; }
        public Integer getFailureCount() { return failureCount; } public void setFailureCount(Integer v) { failureCount = v; }
        public LocalDateTime getStartedAt() { return startedAt; } public void setStartedAt(LocalDateTime v) { startedAt = v; }
        public LocalDateTime getCompletedAt() { return completedAt; } public void setCompletedAt(LocalDateTime v) { completedAt = v; }
        public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    }
    class ItemRow {
        private Long id, batchId, entityId; private String batchNo, entityType, operation, status,
                externalId, errorMessage, transactionNo; private LocalDateTime syncedAt;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public Long getBatchId() { return batchId; } public void setBatchId(Long v) { batchId = v; }
        public Long getEntityId() { return entityId; } public void setEntityId(Long v) { entityId = v; }
        public String getBatchNo() { return batchNo; } public void setBatchNo(String v) { batchNo = v; }
        public String getEntityType() { return entityType; } public void setEntityType(String v) { entityType = v; }
        public String getOperation() { return operation; } public void setOperation(String v) { operation = v; }
        public String getStatus() { return status; } public void setStatus(String v) { status = v; }
        public String getExternalId() { return externalId; } public void setExternalId(String v) { externalId = v; }
        public String getErrorMessage() { return errorMessage; } public void setErrorMessage(String v) { errorMessage = v; }
        public String getTransactionNo() { return transactionNo; } public void setTransactionNo(String v) { transactionNo = v; }
        public LocalDateTime getSyncedAt() { return syncedAt; } public void setSyncedAt(LocalDateTime v) { syncedAt = v; }
    }
    class FinanceSyncRow {
        private Long id; private String transactionNo, recordType, currency, paymentMethod, syncStatus,
                partyName, partyCode, projectCode, projectName, unitNo;
        private LocalDate transactionDate; private BigDecimal amount;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public String getTransactionNo() { return transactionNo; } public void setTransactionNo(String v) { transactionNo = v; }
        public String getRecordType() { return recordType; } public void setRecordType(String v) { recordType = v; }
        public String getCurrency() { return currency; } public void setCurrency(String v) { currency = v; }
        public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String v) { paymentMethod = v; }
        public String getSyncStatus() { return syncStatus; } public void setSyncStatus(String v) { syncStatus = v; }
        public String getPartyName() { return partyName; } public void setPartyName(String v) { partyName = v; }
        public String getPartyCode() { return partyCode; } public void setPartyCode(String v) { partyCode = v; }
        public String getProjectCode() { return projectCode; } public void setProjectCode(String v) { projectCode = v; }
        public String getProjectName() { return projectName; } public void setProjectName(String v) { projectName = v; }
        public String getUnitNo() { return unitNo; } public void setUnitNo(String v) { unitNo = v; }
        public LocalDate getTransactionDate() { return transactionDate; } public void setTransactionDate(LocalDate v) { transactionDate = v; }
        public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal v) { amount = v; }
    }
    class NewBatch {
        private Long id, createdBy; private String batchNo, sourceModule; private int totalCount;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public Long getCreatedBy() { return createdBy; } public void setCreatedBy(Long v) { createdBy = v; }
        public String getBatchNo() { return batchNo; } public void setBatchNo(String v) { batchNo = v; }
        public String getSourceModule() { return sourceModule; } public void setSourceModule(String v) { sourceModule = v; }
        public int getTotalCount() { return totalCount; } public void setTotalCount(int v) { totalCount = v; }
    }
    class NewItem {
        private Long id, batchId, entityId; private String payloadJson;
        public Long getId() { return id; } public void setId(Long v) { id = v; }
        public Long getBatchId() { return batchId; } public void setBatchId(Long v) { batchId = v; }
        public Long getEntityId() { return entityId; } public void setEntityId(Long v) { entityId = v; }
        public String getPayloadJson() { return payloadJson; } public void setPayloadJson(String v) { payloadJson = v; }
    }
}
