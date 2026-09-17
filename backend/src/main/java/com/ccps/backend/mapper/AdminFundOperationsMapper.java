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
public interface AdminFundOperationsMapper {

    @Select("""
            SELECT ra.id,ou.id AS owner_unit_id,o.id AS owner_id,o.full_name AS owner_name,
                   p.name AS project_name,u.unit_no,ra.current_balance,ra.minimum_balance,
                   COALESCE(ors.default_bank_account_id,
                     (SELECT pba.id FROM property_bank_accounts pba WHERE pba.owner_unit_id=ou.id ORDER BY pba.id DESC LIMIT 1)) AS default_bank_account_id,
                   pba.item_name AS default_bank_name,pba.account_no AS default_bank_account_no
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id=ra.owner_unit_id AND ou.status='active' AND ou.asset_stage='OPERATING'
            JOIN owners o ON o.id=ou.owner_id AND o.status='active'
            JOIN units u ON u.id=ou.unit_id JOIN projects p ON p.id=u.project_id
            LEFT JOIN owner_remittance_settings ors ON ors.reserve_account_id=ra.id
            LEFT JOIN property_bank_accounts pba ON pba.id=COALESCE(ors.default_bank_account_id,
              (SELECT pba2.id FROM property_bank_accounts pba2 WHERE pba2.owner_unit_id=ou.id ORDER BY pba2.id DESC LIMIT 1))
            WHERE ra.status='active'
            ORDER BY o.full_name,p.name,u.unit_no
            """)
    List<AccountOptionRow> findAccountOptions();

    @Select("""
            SELECT ra.id,ou.owner_id,ou.id AS owner_unit_id,ra.current_balance,ra.minimum_balance,
                   CONCAT(p.name,' · ',u.unit_no) AS property_name
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id=ra.owner_unit_id AND ou.status='active' AND ou.asset_stage='OPERATING'
            JOIN units u ON u.id=ou.unit_id JOIN projects p ON p.id=u.project_id
            WHERE ra.id IN (#{sourceId},#{targetId}) AND ra.status='active'
            ORDER BY ra.id FOR UPDATE
            """)
    List<TransferAccountRow> lockTransferAccounts(@Param("sourceId") Long sourceId,
            @Param("targetId") Long targetId);

    @Insert("""
            INSERT INTO reserve_internal_transfers
              (transfer_no,source_reserve_account_id,target_reserve_account_id,amount,requested_date,reason,status,created_by)
            VALUES (#{transferNo},#{sourceReserveAccountId},#{targetReserveAccountId},#{amount},#{requestedDate},#{reason},'pending',#{createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTransfer(InternalTransferRecord record);

    @Select("SELECT id,source_reserve_account_id,target_reserve_account_id,amount,status,created_by FROM reserve_internal_transfers WHERE id=#{id} FOR UPDATE")
    InternalTransferRecord lockTransfer(@Param("id") Long id);

    @Update("UPDATE reserve_accounts SET current_balance=current_balance-#{amount} WHERE id=#{accountId} AND current_balance-#{amount}>=minimum_balance")
    int debitTransferSource(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount);

    @Update("UPDATE reserve_accounts SET current_balance=current_balance+#{amount} WHERE id=#{accountId}")
    int creditTransferTarget(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount);

    @Insert("""
            INSERT INTO reserve_transactions
              (reserve_account_id,internal_transfer_id,transaction_type,amount,occurred_at,balance_after,note,created_by)
            VALUES (#{accountId},#{transferId},#{transactionType},#{amount},NOW(),#{balanceAfter},#{note},#{actorId})
            """)
    int insertReserveTransaction(@Param("accountId") Long accountId, @Param("transferId") Long transferId,
            @Param("transactionType") String transactionType, @Param("amount") BigDecimal amount,
            @Param("balanceAfter") BigDecimal balanceAfter, @Param("note") String note,
            @Param("actorId") Long actorId);

    @Update("""
            UPDATE reserve_internal_transfers
            SET status=#{nextStatus},review_note=#{note},reviewed_by=#{actorId},reviewed_at=NOW()
            WHERE id=#{id} AND status='pending'
            """)
    int reviewTransfer(@Param("id") Long id, @Param("nextStatus") String nextStatus,
            @Param("note") String note, @Param("actorId") Long actorId);

    @Update("""
            UPDATE reserve_internal_transfers
            SET status='reversed',review_note=#{note},reversed_by=#{actorId},reversed_at=NOW()
            WHERE id=#{id} AND status='approved'
            """)
    int reverseTransfer(@Param("id") Long id, @Param("note") String note, @Param("actorId") Long actorId);

    @Select("""
            SELECT t.id,t.transfer_no,t.source_reserve_account_id,t.target_reserve_account_id,o.full_name AS owner_name,
                   CONCAT(sp.name,' · ',su.unit_no) AS source_property,CONCAT(tp.name,' · ',tu.unit_no) AS target_property,
                   t.amount,t.requested_date,t.reason,t.status,t.review_note,creator.display_name AS created_by_name,
                   reviewer.display_name AS reviewed_by_name,t.reviewed_at,t.reversed_at,t.created_at
            FROM reserve_internal_transfers t
            JOIN reserve_accounts sra ON sra.id=t.source_reserve_account_id
            JOIN owner_units sou ON sou.id=sra.owner_unit_id JOIN owners o ON o.id=sou.owner_id
            JOIN units su ON su.id=sou.unit_id JOIN projects sp ON sp.id=su.project_id
            JOIN reserve_accounts tra ON tra.id=t.target_reserve_account_id
            JOIN owner_units tou ON tou.id=tra.owner_unit_id JOIN units tu ON tu.id=tou.unit_id JOIN projects tp ON tp.id=tu.project_id
            LEFT JOIN users creator ON creator.id=t.created_by LEFT JOIN users reviewer ON reviewer.id=t.reviewed_by
            ORDER BY t.created_at DESC,t.id DESC LIMIT 200
            """)
    List<InternalTransferRow> findTransfers();

    @Select("SELECT COUNT(*) FROM property_bank_accounts pba JOIN reserve_accounts ra ON ra.owner_unit_id=pba.owner_unit_id WHERE pba.id=#{bankId} AND ra.id=#{accountId}")
    int countBankForAccount(@Param("accountId") Long accountId, @Param("bankId") Long bankId);

    @Insert("""
            INSERT INTO owner_remittance_settings
              (reserve_account_id,cycle,next_remittance_date,enabled,hold_enabled,hold_reason,hold_until,
               retained_amount,tax_retained_amount,default_bank_account_id,created_by,updated_by)
            VALUES (#{accountId},#{cycle},#{nextDate},#{enabled},#{holdEnabled},#{holdReason},#{holdUntil},
                    #{retainedAmount},#{taxRetainedAmount},#{bankId},#{actorId},#{actorId})
            ON DUPLICATE KEY UPDATE cycle=VALUES(cycle),next_remittance_date=VALUES(next_remittance_date),
              enabled=VALUES(enabled),hold_enabled=VALUES(hold_enabled),hold_reason=VALUES(hold_reason),
              hold_until=VALUES(hold_until),retained_amount=VALUES(retained_amount),
              tax_retained_amount=VALUES(tax_retained_amount),default_bank_account_id=VALUES(default_bank_account_id),
              updated_by=VALUES(updated_by)
            """)
    int upsertRemittanceSetting(@Param("actorId") Long actorId, @Param("accountId") Long accountId,
            @Param("cycle") String cycle, @Param("nextDate") LocalDate nextDate,
            @Param("enabled") boolean enabled, @Param("holdEnabled") boolean holdEnabled,
            @Param("holdReason") String holdReason, @Param("holdUntil") LocalDate holdUntil,
            @Param("retainedAmount") BigDecimal retainedAmount,
            @Param("taxRetainedAmount") BigDecimal taxRetainedAmount, @Param("bankId") Long bankId);

    @Select("""
            SELECT ors.id,ors.reserve_account_id,o.full_name AS owner_name,CONCAT(p.name,' · ',u.unit_no) AS property_name,
                   ors.cycle,ors.next_remittance_date,ors.enabled,ors.hold_enabled,ors.hold_reason,ors.hold_until,
                   ors.retained_amount,ors.tax_retained_amount,ors.default_bank_account_id,
                   pba.item_name AS bank_name,pba.account_no AS bank_account_no
            FROM owner_remittance_settings ors
            JOIN reserve_accounts ra ON ra.id=ors.reserve_account_id JOIN owner_units ou ON ou.id=ra.owner_unit_id
            JOIN owners o ON o.id=ou.owner_id JOIN units u ON u.id=ou.unit_id JOIN projects p ON p.id=u.project_id
            LEFT JOIN property_bank_accounts pba ON pba.id=ors.default_bank_account_id
            ORDER BY o.full_name,p.name,u.unit_no
            """)
    List<RemittanceSettingRow> findRemittanceSettings();

    @Select("""
            SELECT ors.reserve_account_id,ors.cycle,ors.retained_amount,ors.tax_retained_amount,
                   ors.default_bank_account_id AS bank_account_id,ra.current_balance,ra.minimum_balance
            FROM owner_remittance_settings ors
            JOIN reserve_accounts ra ON ra.id=ors.reserve_account_id AND ra.status='active'
            JOIN owner_units ou ON ou.id=ra.owner_unit_id AND ou.status='active' AND ou.asset_stage='OPERATING'
            JOIN property_bank_accounts pba ON pba.id=ors.default_bank_account_id AND pba.owner_unit_id=ou.id
            WHERE ors.enabled=1 AND ors.cycle<>'manual' AND ors.next_remittance_date IS NOT NULL
              AND ors.next_remittance_date<=#{scheduledDate}
              AND (ors.hold_enabled=0 OR (ors.hold_until IS NOT NULL AND ors.hold_until<#{scheduledDate}))
              AND NOT EXISTS (
                SELECT 1 FROM owner_remittance_items ori
                JOIN owner_remittance_batches orb ON orb.id=ori.batch_id
                WHERE ori.reserve_account_id=ors.reserve_account_id AND orb.status IN ('draft','submitted')
              )
            ORDER BY ors.reserve_account_id FOR UPDATE
            """)
    List<RemittanceCandidateRow> lockDueRemittanceCandidates(@Param("scheduledDate") LocalDate scheduledDate);

    @Insert("""
            INSERT INTO owner_remittance_batches (batch_no,scheduled_date,status,total_amount,item_count,note,created_by)
            VALUES (#{batchNo},#{scheduledDate},'draft',#{totalAmount},#{itemCount},#{note},#{createdBy})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRemittanceBatch(RemittanceBatchRecord record);

    @Insert("""
            INSERT INTO owner_remittance_items
              (batch_id,reserve_account_id,bank_account_id,amount,retained_amount,tax_retained_amount)
            VALUES (#{batchId},#{accountId},#{bankId},#{amount},#{retainedAmount},#{taxRetainedAmount})
            """)
    int insertRemittanceItem(@Param("batchId") Long batchId, @Param("accountId") Long accountId,
            @Param("bankId") Long bankId, @Param("amount") BigDecimal amount,
            @Param("retainedAmount") BigDecimal retainedAmount,
            @Param("taxRetainedAmount") BigDecimal taxRetainedAmount);

    @Select("SELECT id,scheduled_date,status,batch_no FROM owner_remittance_batches WHERE id=#{id} FOR UPDATE")
    RemittanceBatchRecord lockRemittanceBatch(@Param("id") Long id);

    @Select("SELECT id,reserve_account_id,bank_account_id,amount FROM owner_remittance_items WHERE batch_id=#{batchId} ORDER BY id FOR UPDATE")
    List<RemittanceSubmitRow> lockRemittanceItems(@Param("batchId") Long batchId);

    @Update("UPDATE owner_remittance_items SET finance_record_id=#{financeRecordId} WHERE id=#{itemId} AND finance_record_id IS NULL")
    int linkRemittanceFinance(@Param("itemId") Long itemId, @Param("financeRecordId") Long financeRecordId);

    @Insert("INSERT INTO owner_remittance_finance_links(remittance_item_id,finance_record_id) VALUES (#{itemId},#{financeRecordId})")
    int insertRemittanceFinanceLink(@Param("itemId") Long itemId,
            @Param("financeRecordId") Long financeRecordId);

    @Update("""
            UPDATE owner_remittance_batches SET status=#{status},reviewed_by=#{actorId},reviewed_at=NOW()
            WHERE id=#{id} AND status='draft'
            """)
    int reviewRemittanceBatch(@Param("id") Long id, @Param("status") String status,
            @Param("actorId") Long actorId);

    @Update("""
            UPDATE owner_remittance_settings SET next_remittance_date=
              CASE cycle WHEN 'monthly' THEN DATE_ADD(#{scheduledDate},INTERVAL 1 MONTH)
                         WHEN 'quarterly' THEN DATE_ADD(#{scheduledDate},INTERVAL 3 MONTH)
                         WHEN 'semiannual' THEN DATE_ADD(#{scheduledDate},INTERVAL 6 MONTH)
                         ELSE next_remittance_date END,
              updated_by=#{actorId}
            WHERE reserve_account_id=#{accountId}
            """)
    int advanceRemittanceDate(@Param("accountId") Long accountId,
            @Param("scheduledDate") LocalDate scheduledDate, @Param("actorId") Long actorId);

    @Select("""
            SELECT b.id,b.batch_no,b.scheduled_date,
                   CASE WHEN b.status IN ('submitted','completed','rejected')
                          AND SUM(CASE WHEN fr.confirmation_status='rejected' THEN 1 ELSE 0 END)>0
                        THEN 'rejected'
                        WHEN b.status IN ('submitted','completed','rejected') AND COUNT(link.id)>0
                          AND SUM(CASE WHEN fr.confirmation_status='confirmed' THEN 1 ELSE 0 END)=COUNT(link.id)
                        THEN 'completed' ELSE b.status END AS status,
                   b.total_amount,b.item_count,b.note,creator.display_name AS created_by_name,
                   reviewer.display_name AS reviewed_by_name,b.reviewed_at,b.created_at
            FROM owner_remittance_batches b
            LEFT JOIN owner_remittance_items i ON i.batch_id=b.id
            LEFT JOIN owner_remittance_finance_links link ON link.remittance_item_id=i.id
            LEFT JOIN finance_records fr ON fr.id=link.finance_record_id
            LEFT JOIN users creator ON creator.id=b.created_by LEFT JOIN users reviewer ON reviewer.id=b.reviewed_by
            GROUP BY b.id,b.batch_no,b.scheduled_date,b.status,b.total_amount,b.item_count,b.note,
                     creator.display_name,reviewer.display_name,b.reviewed_at,b.created_at
            ORDER BY b.created_at DESC,b.id DESC LIMIT 100
            """)
    List<RemittanceBatchRow> findRemittanceBatches();

    @Select("""
            SELECT i.id,i.batch_id,i.reserve_account_id,o.full_name AS owner_name,
                   CONCAT(p.name,' · ',u.unit_no) AS property_name,pba.item_name AS bank_name,
                   pba.account_no AS bank_account_no,i.amount,i.retained_amount,i.tax_retained_amount,
                   MIN(link.finance_record_id) AS finance_record_id,GROUP_CONCAT(fr.transaction_no ORDER BY fr.id SEPARATOR ', ') AS transaction_no,
                   CASE WHEN SUM(fr.payment_status='voided')>0 THEN 'voided'
                        WHEN COUNT(link.id)>0 AND SUM(fr.payment_status='paid')=COUNT(link.id) THEN 'paid' ELSE 'unpaid' END AS payment_status,
                   CASE WHEN SUM(fr.confirmation_status='rejected')>0 THEN 'rejected'
                        WHEN COUNT(link.id)>0 AND SUM(fr.confirmation_status='confirmed')=COUNT(link.id) THEN 'confirmed' ELSE 'pending' END AS confirmation_status
            FROM owner_remittance_items i
            JOIN reserve_accounts ra ON ra.id=i.reserve_account_id JOIN owner_units ou ON ou.id=ra.owner_unit_id
            JOIN owners o ON o.id=ou.owner_id JOIN units u ON u.id=ou.unit_id JOIN projects p ON p.id=u.project_id
            JOIN property_bank_accounts pba ON pba.id=i.bank_account_id
            LEFT JOIN owner_remittance_finance_links link ON link.remittance_item_id=i.id
            LEFT JOIN finance_records fr ON fr.id=link.finance_record_id
            WHERE i.batch_id=#{batchId}
            GROUP BY i.id,i.batch_id,i.reserve_account_id,o.full_name,p.name,u.unit_no,pba.item_name,pba.account_no,
                     i.amount,i.retained_amount,i.tax_retained_amount
            ORDER BY i.id
            """)
    List<RemittanceItemRow> findRemittanceItems(@Param("batchId") Long batchId);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,before_data,after_data)
            VALUES (#{actorId},#{action},#{entityType},#{entityId},JSON_OBJECT(),
                    JSON_OBJECT('status',#{status},'note',#{note}))
            """)
    int insertAudit(@Param("actorId") Long actorId, @Param("action") String action,
            @Param("entityType") String entityType, @Param("entityId") Long entityId,
            @Param("status") String status, @Param("note") String note);

    class AccountOptionRow {
        private Long id,ownerId,ownerUnitId,defaultBankAccountId; private String ownerName,projectName,unitNo,defaultBankName,defaultBankAccountNo; private BigDecimal currentBalance,minimumBalance;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;} public Long getOwnerUnitId(){return ownerUnitId;} public void setOwnerUnitId(Long v){ownerUnitId=v;} public Long getDefaultBankAccountId(){return defaultBankAccountId;} public void setDefaultBankAccountId(Long v){defaultBankAccountId=v;} public String getOwnerName(){return ownerName;} public void setOwnerName(String v){ownerName=v;} public String getProjectName(){return projectName;} public void setProjectName(String v){projectName=v;} public String getUnitNo(){return unitNo;} public void setUnitNo(String v){unitNo=v;} public String getDefaultBankName(){return defaultBankName;} public void setDefaultBankName(String v){defaultBankName=v;} public String getDefaultBankAccountNo(){return defaultBankAccountNo;} public void setDefaultBankAccountNo(String v){defaultBankAccountNo=v;} public BigDecimal getCurrentBalance(){return currentBalance;} public void setCurrentBalance(BigDecimal v){currentBalance=v;} public BigDecimal getMinimumBalance(){return minimumBalance;} public void setMinimumBalance(BigDecimal v){minimumBalance=v;}
    }
    class TransferAccountRow {
        private Long id,ownerId,ownerUnitId; private BigDecimal currentBalance,minimumBalance; private String propertyName;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getOwnerId(){return ownerId;} public void setOwnerId(Long v){ownerId=v;} public Long getOwnerUnitId(){return ownerUnitId;} public void setOwnerUnitId(Long v){ownerUnitId=v;} public BigDecimal getCurrentBalance(){return currentBalance;} public void setCurrentBalance(BigDecimal v){currentBalance=v;} public BigDecimal getMinimumBalance(){return minimumBalance;} public void setMinimumBalance(BigDecimal v){minimumBalance=v;} public String getPropertyName(){return propertyName;} public void setPropertyName(String v){propertyName=v;}
    }
    class InternalTransferRecord {
        private Long id,sourceReserveAccountId,targetReserveAccountId,createdBy; private String transferNo,reason,status; private BigDecimal amount; private LocalDate requestedDate;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getSourceReserveAccountId(){return sourceReserveAccountId;} public void setSourceReserveAccountId(Long v){sourceReserveAccountId=v;} public Long getTargetReserveAccountId(){return targetReserveAccountId;} public void setTargetReserveAccountId(Long v){targetReserveAccountId=v;} public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;} public String getTransferNo(){return transferNo;} public void setTransferNo(String v){transferNo=v;} public String getReason(){return reason;} public void setReason(String v){reason=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public LocalDate getRequestedDate(){return requestedDate;} public void setRequestedDate(LocalDate v){requestedDate=v;}
    }
    class InternalTransferRow {
        private Long id,sourceReserveAccountId,targetReserveAccountId; private String transferNo,ownerName,sourceProperty,targetProperty,reason,status,reviewNote,createdByName,reviewedByName; private BigDecimal amount; private LocalDate requestedDate; private LocalDateTime reviewedAt,reversedAt,createdAt;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getSourceReserveAccountId(){return sourceReserveAccountId;} public void setSourceReserveAccountId(Long v){sourceReserveAccountId=v;} public Long getTargetReserveAccountId(){return targetReserveAccountId;} public void setTargetReserveAccountId(Long v){targetReserveAccountId=v;} public String getTransferNo(){return transferNo;} public void setTransferNo(String v){transferNo=v;} public String getOwnerName(){return ownerName;} public void setOwnerName(String v){ownerName=v;} public String getSourceProperty(){return sourceProperty;} public void setSourceProperty(String v){sourceProperty=v;} public String getTargetProperty(){return targetProperty;} public void setTargetProperty(String v){targetProperty=v;} public String getReason(){return reason;} public void setReason(String v){reason=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getReviewNote(){return reviewNote;} public void setReviewNote(String v){reviewNote=v;} public String getCreatedByName(){return createdByName;} public void setCreatedByName(String v){createdByName=v;} public String getReviewedByName(){return reviewedByName;} public void setReviewedByName(String v){reviewedByName=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public LocalDate getRequestedDate(){return requestedDate;} public void setRequestedDate(LocalDate v){requestedDate=v;} public LocalDateTime getReviewedAt(){return reviewedAt;} public void setReviewedAt(LocalDateTime v){reviewedAt=v;} public LocalDateTime getReversedAt(){return reversedAt;} public void setReversedAt(LocalDateTime v){reversedAt=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    }
    class RemittanceSettingRow {
        private Long id,reserveAccountId,defaultBankAccountId; private String ownerName,propertyName,cycle,holdReason,bankName,bankAccountNo; private LocalDate nextRemittanceDate,holdUntil; private Boolean enabled,holdEnabled; private BigDecimal retainedAmount,taxRetainedAmount;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getReserveAccountId(){return reserveAccountId;} public void setReserveAccountId(Long v){reserveAccountId=v;} public Long getDefaultBankAccountId(){return defaultBankAccountId;} public void setDefaultBankAccountId(Long v){defaultBankAccountId=v;} public String getOwnerName(){return ownerName;} public void setOwnerName(String v){ownerName=v;} public String getPropertyName(){return propertyName;} public void setPropertyName(String v){propertyName=v;} public String getCycle(){return cycle;} public void setCycle(String v){cycle=v;} public String getHoldReason(){return holdReason;} public void setHoldReason(String v){holdReason=v;} public String getBankName(){return bankName;} public void setBankName(String v){bankName=v;} public String getBankAccountNo(){return bankAccountNo;} public void setBankAccountNo(String v){bankAccountNo=v;} public LocalDate getNextRemittanceDate(){return nextRemittanceDate;} public void setNextRemittanceDate(LocalDate v){nextRemittanceDate=v;} public LocalDate getHoldUntil(){return holdUntil;} public void setHoldUntil(LocalDate v){holdUntil=v;} public Boolean getEnabled(){return enabled;} public void setEnabled(Boolean v){enabled=v;} public Boolean getHoldEnabled(){return holdEnabled;} public void setHoldEnabled(Boolean v){holdEnabled=v;} public BigDecimal getRetainedAmount(){return retainedAmount;} public void setRetainedAmount(BigDecimal v){retainedAmount=v;} public BigDecimal getTaxRetainedAmount(){return taxRetainedAmount;} public void setTaxRetainedAmount(BigDecimal v){taxRetainedAmount=v;}
    }
    class RemittanceCandidateRow {
        private Long reserveAccountId,bankAccountId; private String cycle; private BigDecimal retainedAmount,taxRetainedAmount,currentBalance,minimumBalance;
        public Long getReserveAccountId(){return reserveAccountId;} public void setReserveAccountId(Long v){reserveAccountId=v;} public Long getBankAccountId(){return bankAccountId;} public void setBankAccountId(Long v){bankAccountId=v;} public String getCycle(){return cycle;} public void setCycle(String v){cycle=v;} public BigDecimal getRetainedAmount(){return retainedAmount;} public void setRetainedAmount(BigDecimal v){retainedAmount=v;} public BigDecimal getTaxRetainedAmount(){return taxRetainedAmount;} public void setTaxRetainedAmount(BigDecimal v){taxRetainedAmount=v;} public BigDecimal getCurrentBalance(){return currentBalance;} public void setCurrentBalance(BigDecimal v){currentBalance=v;} public BigDecimal getMinimumBalance(){return minimumBalance;} public void setMinimumBalance(BigDecimal v){minimumBalance=v;}
    }
    class RemittanceBatchRecord {
        private Long id,createdBy; private String batchNo,status,note; private LocalDate scheduledDate; private BigDecimal totalAmount; private Integer itemCount;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getCreatedBy(){return createdBy;} public void setCreatedBy(Long v){createdBy=v;} public String getBatchNo(){return batchNo;} public void setBatchNo(String v){batchNo=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getNote(){return note;} public void setNote(String v){note=v;} public LocalDate getScheduledDate(){return scheduledDate;} public void setScheduledDate(LocalDate v){scheduledDate=v;} public BigDecimal getTotalAmount(){return totalAmount;} public void setTotalAmount(BigDecimal v){totalAmount=v;} public Integer getItemCount(){return itemCount;} public void setItemCount(Integer v){itemCount=v;}
    }
    class RemittanceSubmitRow {
        private Long id,reserveAccountId,bankAccountId; private BigDecimal amount;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getReserveAccountId(){return reserveAccountId;} public void setReserveAccountId(Long v){reserveAccountId=v;} public Long getBankAccountId(){return bankAccountId;} public void setBankAccountId(Long v){bankAccountId=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    }
    class RemittanceBatchRow extends RemittanceBatchRecord {
        private String createdByName,reviewedByName; private LocalDateTime reviewedAt,createdAt;
        public String getCreatedByName(){return createdByName;} public void setCreatedByName(String v){createdByName=v;} public String getReviewedByName(){return reviewedByName;} public void setReviewedByName(String v){reviewedByName=v;} public LocalDateTime getReviewedAt(){return reviewedAt;} public void setReviewedAt(LocalDateTime v){reviewedAt=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    }
    class RemittanceItemRow {
        private Long id,batchId,reserveAccountId,financeRecordId; private String ownerName,propertyName,bankName,bankAccountNo,transactionNo,paymentStatus,confirmationStatus; private BigDecimal amount,retainedAmount,taxRetainedAmount;
        public Long getId(){return id;} public void setId(Long v){id=v;} public Long getBatchId(){return batchId;} public void setBatchId(Long v){batchId=v;} public Long getReserveAccountId(){return reserveAccountId;} public void setReserveAccountId(Long v){reserveAccountId=v;} public Long getFinanceRecordId(){return financeRecordId;} public void setFinanceRecordId(Long v){financeRecordId=v;} public String getOwnerName(){return ownerName;} public void setOwnerName(String v){ownerName=v;} public String getPropertyName(){return propertyName;} public void setPropertyName(String v){propertyName=v;} public String getBankName(){return bankName;} public void setBankName(String v){bankName=v;} public String getBankAccountNo(){return bankAccountNo;} public void setBankAccountNo(String v){bankAccountNo=v;} public String getTransactionNo(){return transactionNo;} public void setTransactionNo(String v){transactionNo=v;} public String getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(String v){paymentStatus=v;} public String getConfirmationStatus(){return confirmationStatus;} public void setConfirmationStatus(String v){confirmationStatus=v;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;} public BigDecimal getRetainedAmount(){return retainedAmount;} public void setRetainedAmount(BigDecimal v){retainedAmount=v;} public BigDecimal getTaxRetainedAmount(){return taxRetainedAmount;} public void setTaxRetainedAmount(BigDecimal v){taxRetainedAmount=v;}
    }
}
