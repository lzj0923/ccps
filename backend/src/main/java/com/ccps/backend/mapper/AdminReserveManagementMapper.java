package com.ccps.backend.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Options;

import com.ccps.backend.dto.AdminReserveManagementResponse.Account;
import com.ccps.backend.dto.AdminReserveManagementResponse.Transaction;
import com.ccps.backend.dto.AdminReserveManagementResponse.RefundBankAccount;

@Mapper
public interface AdminReserveManagementMapper {
    @Select("""
            SELECT COALESCE(SUM(current_balance),0) + COALESCE((SELECT SUM(rp.allocated_amount)
                     FROM rent_payments rp JOIN finance_records fr ON fr.id=rp.finance_record_id
                     JOIN rent_invoices ri ON ri.id=rp.rent_invoice_id JOIN leases l ON l.id=ri.lease_id
                     JOIN owner_units xou ON xou.unit_id=l.unit_id AND xou.status='active' AND xou.asset_stage='OPERATING'
                     JOIN reserve_accounts xra ON xra.owner_unit_id=xou.id AND xra.status='active'
                    WHERE fr.record_type='rent_payment' AND fr.payment_status='paid'
                      AND fr.confirmation_status='confirmed' AND fr.transaction_date<=CURRENT_DATE),0) AS total_balance,
                   COALESCE(SUM(current_balance),0) + COALESCE((SELECT SUM(fr.amount)
                     FROM finance_records fr
                     JOIN owner_units xou ON xou.owner_id=fr.owner_id AND xou.unit_id=fr.unit_id
                       AND xou.status='active' AND xou.asset_stage='OPERATING'
                     JOIN reserve_accounts xra ON xra.owner_unit_id=xou.id AND xra.status='active'
                    WHERE fr.record_type='rent_payment' AND fr.payment_status='paid'
                      AND fr.confirmation_status='confirmed'
                      AND COALESCE(fr.receipt_date,fr.transaction_date)<=CURRENT_DATE
                      AND EXISTS (SELECT 1 FROM rent_payments rp WHERE rp.finance_record_id=fr.id)),0) AS accounting_balance,
                   COALESCE(SUM(minimum_balance),0) AS minimum_balance,
                   COUNT(*) AS account_count,
                   COALESCE(SUM(current_balance < minimum_balance),0) AS low_balance_count,
                   COALESCE((SELECT SUM(amount) FROM reserve_transactions
                     WHERE transaction_type='topup' AND occurred_at >= DATE_FORMAT(CURRENT_DATE,'%Y-%m-01')),0) AS monthly_topups,
                   COALESCE((SELECT SUM(amount) FROM reserve_transactions
                     WHERE transaction_type='debit' AND occurred_at >= DATE_FORMAT(CURRENT_DATE,'%Y-%m-01')),0) AS monthly_debits,
                   COALESCE((SELECT COUNT(*) FROM finance_records WHERE record_type='reserve_topup' AND confirmation_status='pending'),0) AS pending_topup_count,
                   COALESCE((SELECT SUM(amount) FROM finance_records WHERE record_type='reserve_topup' AND confirmation_status='pending'),0) AS pending_topup_amount
            FROM reserve_accounts WHERE status='active'
            """)
    SummaryRow findSummary();

    @Select("""
            SELECT ra.id, o.id AS owner_id, o.full_name AS owner_name, p.id AS project_id, p.name AS project_name,
                   u.id AS unit_id, u.unit_no,
                   (SELECT l.start_date FROM leases l WHERE l.unit_id=u.id AND l.status='active' ORDER BY l.end_date DESC, l.id DESC LIMIT 1) AS lease_start_date,
                   (SELECT l.end_date FROM leases l WHERE l.unit_id=u.id AND l.status='active' ORDER BY l.end_date DESC, l.id DESC LIMIT 1) AS lease_end_date,
                   ra.remarks, ra.minimum_balance,
                   ra.current_balance + COALESCE(posted_rent.amount,0) AS current_balance,
                   ra.current_balance + COALESCE(received_rent.amount,0) AS accounting_balance,
                   ra.minimum_balance_mode, ra.calculated_minimum_balance, ra.rent_buffer_amount,
                   ra.monthly_expense_average, ra.expense_buffer_months, ra.minimum_balance_calculated_at,
                   GREATEST(ra.minimum_balance-(ra.current_balance + COALESCE(posted_rent.amount,0)),0) AS shortage_amount,
                   COALESCE(SUM(CASE WHEN rt.transaction_type='topup' THEN rt.amount ELSE 0 END),0) AS total_topups,
                   COALESCE(SUM(CASE WHEN rt.transaction_type='debit' THEN rt.amount ELSE 0 END),0) AS total_debits,
                   MAX(rt.occurred_at) AS last_movement_at, ra.low_balance_alert_enabled,
                   CASE WHEN ra.current_balance + COALESCE(posted_rent.amount,0) < ra.minimum_balance THEN 'low' ELSE 'normal' END AS balance_status,
                   (SELECT COUNT(*) FROM finance_records fr WHERE fr.record_type='reserve_topup'
                     AND fr.confirmation_status='pending' AND fr.owner_id=o.id AND fr.unit_id=u.id) AS pending_topup_count,
                   COALESCE((SELECT SUM(fr.amount) FROM finance_records fr WHERE fr.record_type='reserve_topup'
                     AND fr.confirmation_status='pending' AND fr.owner_id=o.id AND fr.unit_id=u.id),0) AS pending_topup_amount
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id=ra.owner_unit_id AND ou.status='active' AND ou.asset_stage='OPERATING'
            JOIN owners o ON o.id=ou.owner_id AND o.status='active'
            JOIN units u ON u.id=ou.unit_id
            JOIN projects p ON p.id=u.project_id
            LEFT JOIN reserve_transactions rt ON rt.reserve_account_id=ra.id
            LEFT JOIN (SELECT fr.owner_id,fr.unit_id,SUM(rp.allocated_amount) AS amount
                         FROM rent_payments rp JOIN finance_records fr ON fr.id=rp.finance_record_id
                        WHERE fr.record_type='rent_payment' AND fr.payment_status='paid'
                          AND fr.confirmation_status='confirmed' AND fr.transaction_date<=CURRENT_DATE
                        GROUP BY fr.owner_id,fr.unit_id) posted_rent ON posted_rent.owner_id=o.id AND posted_rent.unit_id=u.id
            LEFT JOIN (SELECT fr.owner_id,fr.unit_id,SUM(fr.amount) AS amount
                         FROM finance_records fr
                        WHERE fr.record_type='rent_payment' AND fr.payment_status='paid'
                          AND fr.confirmation_status='confirmed'
                          AND COALESCE(fr.receipt_date,fr.transaction_date)<=CURRENT_DATE
                          AND EXISTS (SELECT 1 FROM rent_payments rp WHERE rp.finance_record_id=fr.id)
                        GROUP BY fr.owner_id,fr.unit_id) received_rent ON received_rent.owner_id=o.id AND received_rent.unit_id=u.id
            WHERE ra.status='active'
            GROUP BY ra.id,o.id,o.full_name,p.id,p.name,u.id,u.unit_no,ra.remarks,ra.minimum_balance,ra.current_balance,
                     ra.minimum_balance_mode,ra.calculated_minimum_balance,ra.rent_buffer_amount,
                     ra.monthly_expense_average,ra.expense_buffer_months,ra.minimum_balance_calculated_at,
                     ra.low_balance_alert_enabled,posted_rent.amount,received_rent.amount
            ORDER BY (ra.current_balance + COALESCE(posted_rent.amount,0) < ra.minimum_balance) DESC,
                     GREATEST(ra.minimum_balance-(ra.current_balance + COALESCE(posted_rent.amount,0)),0) DESC,p.name,u.unit_no
            """)
    List<Account> findAccounts();

    @Select("""
            SELECT rt.id, rt.reserve_account_id, rt.finance_record_id,
                   rt.maintenance_work_order_id AS work_order_id, rt.transaction_type,
                   rt.amount, rt.balance_after, rt.note, rt.occurred_at,
                   fr.transaction_no, mwo.work_order_no, creator.display_name AS created_by_name
            FROM reserve_transactions rt
            LEFT JOIN finance_records fr ON fr.id=rt.finance_record_id
            LEFT JOIN maintenance_work_orders mwo ON mwo.id=rt.maintenance_work_order_id
            LEFT JOIN users creator ON creator.id=rt.created_by
            ORDER BY rt.occurred_at DESC, rt.id DESC
            LIMIT 300
            """)
    List<Transaction> findTransactions();

    @Select("""
            SELECT pba.id,ra.id AS reserve_account_id,pba.item_name,pba.payment_name,pba.account_no,
                   pba.transfer_limit,pba.is_overseas_bank AS overseas_bank,
                   pba.overseas_transfer_fee
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id=ra.owner_unit_id AND ou.status='active'
            JOIN property_bank_accounts pba ON pba.owner_unit_id=ou.id
            WHERE ra.status='active'
            ORDER BY ra.id,pba.id DESC
            """)
    List<RefundBankAccount> findRefundBankAccounts();

    @Select("SELECT DISTINCT p.name FROM reserve_accounts ra JOIN owner_units ou ON ou.id=ra.owner_unit_id JOIN units u ON u.id=ou.unit_id JOIN projects p ON p.id=u.project_id WHERE ra.status='active' AND ou.status='active' ORDER BY p.name")
    List<String> findProjects();

    @Select("SELECT id, minimum_balance, minimum_balance_mode, calculated_minimum_balance, low_balance_alert_enabled, remarks FROM reserve_accounts WHERE id=#{accountId} AND status='active'")
    SettingsRow findSettings(@Param("accountId") Long accountId);

    @Update("""
            UPDATE reserve_accounts
            SET minimum_balance_mode=#{mode},
                minimum_balance=CASE WHEN #{mode}='auto' THEN calculated_minimum_balance ELSE #{minimumBalance} END,
                 low_balance_alert_enabled=#{alertEnabled},
                 remarks=#{remarks}
            WHERE id=#{accountId} AND status='active'
            """)
    int updateSettings(@Param("accountId") Long accountId, @Param("mode") String mode,
            @Param("minimumBalance") BigDecimal minimumBalance, @Param("alertEnabled") boolean alertEnabled,
            @Param("remarks") String remarks);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, before_data, after_data)
            VALUES (#{actorId}, 'update_reserve_settings', 'reserve_account', #{accountId},
               JSON_OBJECT('minimumBalance',#{beforeMinimum},'mode',#{beforeMode},'lowBalanceAlertEnabled',#{beforeAlert},'remarks',#{beforeRemarks}),
               JSON_OBJECT('minimumBalance',#{afterMinimum},'mode',#{afterMode},'lowBalanceAlertEnabled',#{afterAlert},'remarks',#{afterRemarks}))
            """)
    int insertSettingsAudit(@Param("actorId") Long actorId, @Param("accountId") Long accountId,
            @Param("beforeMinimum") BigDecimal beforeMinimum, @Param("beforeMode") String beforeMode,
            @Param("beforeAlert") boolean beforeAlert, @Param("afterMinimum") BigDecimal afterMinimum,
            @Param("afterMode") String afterMode, @Param("afterAlert") boolean afterAlert,
            @Param("beforeRemarks") String beforeRemarks, @Param("afterRemarks") String afterRemarks);

    @Select("""
            SELECT ra.id, ra.current_balance, o.id AS owner_id, o.user_id, o.full_name AS owner_name,
                   u.id AS unit_id, u.unit_no, p.name AS project_name
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id=ra.owner_unit_id AND ou.status='active' AND ou.asset_stage='OPERATING'
            JOIN owners o ON o.id=ou.owner_id AND o.status='active'
            JOIN units u ON u.id=ou.unit_id JOIN projects p ON p.id=u.project_id
            WHERE ra.id=#{accountId} AND ra.status='active' FOR UPDATE
            """)
    DirectTopupContext findDirectTopupContext(@Param("accountId") Long accountId);

    @Select("""
            SELECT ra.id AS reserve_account_id,ra.current_balance,o.id AS owner_id,o.user_id,
                   o.full_name AS owner_name,u.id AS unit_id,u.unit_no,p.name AS project_name,
                   pba.id AS bank_account_id,pba.item_name AS bank_name,pba.payment_name,
                   pba.account_no,pba.transfer_limit,pba.is_overseas_bank AS overseas_bank,
                   pba.overseas_transfer_fee
            FROM reserve_accounts ra
            JOIN owner_units ou ON ou.id=ra.owner_unit_id AND ou.status='active' AND ou.asset_stage='OPERATING'
            JOIN owners o ON o.id=ou.owner_id AND o.status='active'
            JOIN units u ON u.id=ou.unit_id JOIN projects p ON p.id=u.project_id
            JOIN property_bank_accounts pba ON pba.owner_unit_id=ou.id
            WHERE ra.id=#{accountId} AND pba.id=#{bankAccountId} AND ra.status='active'
            FOR UPDATE
            """)
    RefundBankContext findRefundBankContext(@Param("accountId") Long accountId,
            @Param("bankAccountId") Long bankAccountId);

    @Insert("""
            INSERT INTO finance_records
              (transaction_no,record_type,unit_id,owner_id,amount,currency,transaction_date,payment_method,
               payment_status,confirmation_status,sync_status,created_by)
            VALUES
              (#{transactionNo},'reserve_topup',#{unitId},#{ownerId},#{amount},'MYR',#{paymentDate},#{paymentMethod},
               'paid','pending','not_synced',#{actorId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertDirectTopupFinance(DirectTopupRecord record);

    @Insert("INSERT INTO finance_records (transaction_no,record_type,unit_id,owner_id,amount,currency,transaction_date,payment_method,payment_status,confirmation_status,sync_status,created_by) VALUES (#{transactionNo},'reserve_refund',#{unitId},#{ownerId},#{amount},'MYR',#{paymentDate},#{paymentMethod},'unpaid','pending','not_synced',#{actorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertReserveRefundFinance(DirectTopupRecord record);

    @Insert("INSERT INTO cashflow_entries (finance_record_id,unit_id,owner_id,direction,category,description,occurred_on,attachment_status) VALUES (#{financeRecordId},#{unitId},#{ownerId},'expense','other',#{note},#{occurredOn},'missing')")
    int insertReserveRefundCashflow(@Param("financeRecordId") Long financeRecordId,@Param("unitId") Long unitId,@Param("ownerId") Long ownerId,@Param("note") String note,@Param("occurredOn") LocalDate occurredOn);

    @Insert("INSERT INTO finance_records (transaction_no,record_type,unit_id,owner_id,amount,currency,transaction_date,payment_method,payment_status,confirmation_status,sync_status,created_by) VALUES (#{transactionNo},'cashflow',#{unitId},#{ownerId},#{amount},'MYR',#{paymentDate},'bank_transfer','unpaid','pending','not_synced',#{actorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertOverseasFeeFinance(DirectTopupRecord record);

    @Insert("INSERT INTO cashflow_entries (finance_record_id,unit_id,owner_id,direction,category,description,occurred_on,attachment_status) VALUES (#{financeRecordId},#{unitId},#{ownerId},'expense','service_fee',#{description},#{occurredOn},'not_required')")
    int insertOverseasFeeCashflow(@Param("financeRecordId") Long financeRecordId,@Param("unitId") Long unitId,
            @Param("ownerId") Long ownerId,@Param("description") String description,@Param("occurredOn") LocalDate occurredOn);

    @Insert("""
            INSERT INTO reserve_refund_transfers
              (batch_reference,reserve_account_id,bank_account_id,finance_record_id,fee_finance_record_id,
               installment_no,installment_count,scheduled_date,principal_amount,fee_amount,status,created_by)
            VALUES
              (#{batchReference},#{accountId},#{bankAccountId},#{financeRecordId},#{feeFinanceRecordId},
               #{installmentNo},#{installmentCount},#{scheduledDate},#{principalAmount},#{feeAmount},'pending',#{actorId})
            """)
    int insertRefundTransfer(@Param("batchReference") String batchReference,@Param("accountId") Long accountId,
            @Param("bankAccountId") Long bankAccountId,@Param("financeRecordId") Long financeRecordId,
            @Param("feeFinanceRecordId") Long feeFinanceRecordId,@Param("installmentNo") int installmentNo,
            @Param("installmentCount") int installmentCount,@Param("scheduledDate") LocalDate scheduledDate,
            @Param("principalAmount") BigDecimal principalAmount,@Param("feeAmount") BigDecimal feeAmount,
            @Param("actorId") Long actorId);

    @Insert("""
            INSERT INTO payment_receipts
              (finance_record_id,receipt_no,payer_name,bank_reference,submission_note,review_note)
            VALUES (#{financeRecordId},#{receiptNo},#{payerName},#{bankReference},#{note},NULL)
            """)
    int insertDirectTopupReceipt(@Param("financeRecordId") Long financeRecordId,
            @Param("receiptNo") String receiptNo, @Param("payerName") String payerName,
            @Param("bankReference") String bankReference, @Param("note") String note);

    @Insert("""
            INSERT INTO reserve_transactions
              (reserve_account_id,finance_record_id,transaction_type,amount,occurred_at,balance_after,note,created_by)
            VALUES (#{accountId},#{financeRecordId},'topup',#{amount},#{occurredAt},#{balanceAfter},#{note},#{actorId})
            """)
    int insertDirectTopupTransaction(@Param("accountId") Long accountId,
            @Param("financeRecordId") Long financeRecordId, @Param("amount") BigDecimal amount,
            @Param("occurredAt") LocalDateTime occurredAt, @Param("balanceAfter") BigDecimal balanceAfter,
            @Param("note") String note, @Param("actorId") Long actorId);

    @Update("UPDATE reserve_accounts SET current_balance=#{balanceAfter} WHERE id=#{accountId} AND status='active'")
    int updateDirectTopupBalance(@Param("accountId") Long accountId, @Param("balanceAfter") BigDecimal balanceAfter);

    @Insert("""
            INSERT INTO notifications
              (recipient_user_id,recipient_owner_id,title,body,related_type,related_id,priority,status)
            VALUES (#{userId},#{ownerId},'預備金已充值',#{body},'finance_record',#{financeRecordId},'normal','unread')
            """)
    int insertDirectTopupNotification(@Param("userId") Long userId, @Param("ownerId") Long ownerId,
            @Param("financeRecordId") Long financeRecordId, @Param("body") String body);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,after_data)
            VALUES (#{actorId},'admin_direct_reserve_topup','finance_record',#{financeRecordId},
              JSON_OBJECT('reserveAccountId',#{accountId},'amount',#{amount},'projectedBalanceAfter',#{projectedBalanceAfter},
                'confirmationStatus','pending','note',#{note}))
            """)
    int insertDirectTopupAudit(@Param("actorId") Long actorId, @Param("financeRecordId") Long financeRecordId,
            @Param("accountId") Long accountId, @Param("amount") BigDecimal amount,
            @Param("projectedBalanceAfter") BigDecimal projectedBalanceAfter, @Param("note") String note);

    @Insert("INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,after_data) VALUES (#{actorId},'create_reserve_refund','finance_record',#{financeRecordId},JSON_OBJECT('reserveAccountId',#{accountId},'amount',#{amount},'note',#{note}))")
    int insertReserveRefundAudit(@Param("actorId") Long actorId,@Param("financeRecordId") Long financeRecordId,@Param("accountId") Long accountId,@Param("amount") BigDecimal amount,@Param("note") String note);

    @Select("SELECT rr.id,rr.reconciliation_month,rr.system_balance,rr.finance_balance,rr.difference_amount,rr.status,rr.note,u.display_name AS confirmed_by_name,rr.confirmed_at FROM reserve_reconciliations rr LEFT JOIN users u ON u.id=rr.confirmed_by ORDER BY rr.reconciliation_month DESC LIMIT 24")
    List<ReconciliationRow> findReconciliations();

    @Select("""
            SELECT COALESCE(SUM(ra.current_balance),0) + COALESCE((SELECT SUM(fr.amount)
              FROM finance_records fr
              JOIN owner_units ou ON ou.owner_id=fr.owner_id AND ou.unit_id=fr.unit_id
                AND ou.status='active' AND ou.asset_stage='OPERATING'
              JOIN reserve_accounts xra ON xra.owner_unit_id=ou.id AND xra.status='active'
             WHERE fr.record_type='rent_payment' AND fr.payment_status='paid'
               AND fr.confirmation_status='confirmed'
               AND COALESCE(fr.receipt_date,fr.transaction_date)<=CURRENT_DATE
               AND EXISTS (SELECT 1 FROM rent_payments rp WHERE rp.finance_record_id=fr.id)),0)
            FROM reserve_accounts ra WHERE ra.status='active'
            """)
    BigDecimal findReceivedTotalBalance();

    @Insert("INSERT INTO reserve_reconciliations (reconciliation_month,system_balance,finance_balance,difference_amount,status,note,confirmed_by,confirmed_at) VALUES (#{month},#{systemBalance},#{financeBalance},#{difference},#{status},#{note},#{actorId},CASE WHEN #{status}='confirmed' THEN CURRENT_TIMESTAMP ELSE NULL END) ON DUPLICATE KEY UPDATE system_balance=VALUES(system_balance),finance_balance=VALUES(finance_balance),difference_amount=VALUES(difference_amount),status=VALUES(status),note=VALUES(note),confirmed_by=VALUES(confirmed_by),confirmed_at=CASE WHEN VALUES(status)='confirmed' THEN CURRENT_TIMESTAMP ELSE NULL END")
    int saveReconciliation(@Param("month") LocalDate month,@Param("systemBalance") BigDecimal systemBalance,
            @Param("financeBalance") BigDecimal financeBalance,@Param("difference") BigDecimal difference,
            @Param("status") String status,@Param("note") String note,@Param("actorId") Long actorId);

    @Insert("INSERT INTO audit_logs (actor_user_id,action,entity_type,entity_id,after_data) VALUES (#{actorId},'save_reserve_reconciliation','reserve_reconciliation',NULL,JSON_OBJECT('month',#{month},'systemBalance',#{systemBalance},'financeBalance',#{financeBalance},'difference',#{difference},'status',#{status},'note',#{note}))")
    int insertReconciliationAudit(@Param("actorId") Long actorId,@Param("month") LocalDate month,
            @Param("systemBalance") BigDecimal systemBalance,@Param("financeBalance") BigDecimal financeBalance,
            @Param("difference") BigDecimal difference,@Param("status") String status,@Param("note") String note);

    class SummaryRow {
        private BigDecimal totalBalance, accountingBalance, minimumBalance, monthlyTopups, monthlyDebits, pendingTopupAmount;
        private Long accountCount, lowBalanceCount, pendingTopupCount;
        public BigDecimal getTotalBalance(){return totalBalance;} public void setTotalBalance(BigDecimal v){totalBalance=v;}
        public BigDecimal getAccountingBalance(){return accountingBalance;} public void setAccountingBalance(BigDecimal v){accountingBalance=v;}
        public BigDecimal getMinimumBalance(){return minimumBalance;} public void setMinimumBalance(BigDecimal v){minimumBalance=v;}
        public BigDecimal getMonthlyTopups(){return monthlyTopups;} public void setMonthlyTopups(BigDecimal v){monthlyTopups=v;}
        public BigDecimal getMonthlyDebits(){return monthlyDebits;} public void setMonthlyDebits(BigDecimal v){monthlyDebits=v;}
        public BigDecimal getPendingTopupAmount(){return pendingTopupAmount;} public void setPendingTopupAmount(BigDecimal v){pendingTopupAmount=v;}
        public Long getAccountCount(){return accountCount;} public void setAccountCount(Long v){accountCount=v;}
        public Long getLowBalanceCount(){return lowBalanceCount;} public void setLowBalanceCount(Long v){lowBalanceCount=v;}
        public Long getPendingTopupCount(){return pendingTopupCount;} public void setPendingTopupCount(Long v){pendingTopupCount=v;}
    }

    class SettingsRow {
        private Long id;
        private BigDecimal minimumBalance, calculatedMinimumBalance;
        private String minimumBalanceMode, remarks;
        private Boolean lowBalanceAlertEnabled;
        public Long getId(){return id;} public void setId(Long value){id=value;}
        public BigDecimal getMinimumBalance(){return minimumBalance;} public void setMinimumBalance(BigDecimal value){minimumBalance=value;}
        public BigDecimal getCalculatedMinimumBalance(){return calculatedMinimumBalance;} public void setCalculatedMinimumBalance(BigDecimal value){calculatedMinimumBalance=value;}
        public String getMinimumBalanceMode(){return minimumBalanceMode;} public void setMinimumBalanceMode(String value){minimumBalanceMode=value;}
        public Boolean getLowBalanceAlertEnabled(){return lowBalanceAlertEnabled;} public void setLowBalanceAlertEnabled(Boolean value){lowBalanceAlertEnabled=value;}
        public String getRemarks(){return remarks;} public void setRemarks(String value){remarks=value;}
    }

    class DirectTopupContext {
        private Long id, ownerId, userId, unitId;
        private BigDecimal currentBalance;
        private String ownerName, unitNo, projectName;
        public Long getId(){return id;} public void setId(Long value){id=value;}
        public Long getOwnerId(){return ownerId;} public void setOwnerId(Long value){ownerId=value;}
        public Long getUserId(){return userId;} public void setUserId(Long value){userId=value;}
        public Long getUnitId(){return unitId;} public void setUnitId(Long value){unitId=value;}
        public BigDecimal getCurrentBalance(){return currentBalance;} public void setCurrentBalance(BigDecimal value){currentBalance=value;}
        public String getOwnerName(){return ownerName;} public void setOwnerName(String value){ownerName=value;}
        public String getUnitNo(){return unitNo;} public void setUnitNo(String value){unitNo=value;}
        public String getProjectName(){return projectName;} public void setProjectName(String value){projectName=value;}
    }

    class RefundBankContext {
        private Long reserveAccountId, ownerId, userId, unitId, bankAccountId;
        private BigDecimal currentBalance, transferLimit, overseasTransferFee;
        private String ownerName, unitNo, projectName, bankName, paymentName, accountNo;
        private Boolean overseasBank;
        public Long getReserveAccountId(){return reserveAccountId;} public void setReserveAccountId(Long value){reserveAccountId=value;}
        public Long getOwnerId(){return ownerId;} public void setOwnerId(Long value){ownerId=value;}
        public Long getUserId(){return userId;} public void setUserId(Long value){userId=value;}
        public Long getUnitId(){return unitId;} public void setUnitId(Long value){unitId=value;}
        public Long getBankAccountId(){return bankAccountId;} public void setBankAccountId(Long value){bankAccountId=value;}
        public BigDecimal getCurrentBalance(){return currentBalance;} public void setCurrentBalance(BigDecimal value){currentBalance=value;}
        public BigDecimal getTransferLimit(){return transferLimit;} public void setTransferLimit(BigDecimal value){transferLimit=value;}
        public BigDecimal getOverseasTransferFee(){return overseasTransferFee;} public void setOverseasTransferFee(BigDecimal value){overseasTransferFee=value;}
        public String getOwnerName(){return ownerName;} public void setOwnerName(String value){ownerName=value;}
        public String getUnitNo(){return unitNo;} public void setUnitNo(String value){unitNo=value;}
        public String getProjectName(){return projectName;} public void setProjectName(String value){projectName=value;}
        public String getBankName(){return bankName;} public void setBankName(String value){bankName=value;}
        public String getPaymentName(){return paymentName;} public void setPaymentName(String value){paymentName=value;}
        public String getAccountNo(){return accountNo;} public void setAccountNo(String value){accountNo=value;}
        public Boolean getOverseasBank(){return overseasBank;} public void setOverseasBank(Boolean value){overseasBank=value;}
    }

    class DirectTopupRecord {
        private Long id, unitId, ownerId, actorId;
        private String transactionNo, paymentMethod;
        private BigDecimal amount;
        private java.time.LocalDate paymentDate;
        public Long getId(){return id;} public void setId(Long value){id=value;}
        public Long getUnitId(){return unitId;} public void setUnitId(Long value){unitId=value;}
        public Long getOwnerId(){return ownerId;} public void setOwnerId(Long value){ownerId=value;}
        public Long getActorId(){return actorId;} public void setActorId(Long value){actorId=value;}
        public String getTransactionNo(){return transactionNo;} public void setTransactionNo(String value){transactionNo=value;}
        public String getPaymentMethod(){return paymentMethod;} public void setPaymentMethod(String value){paymentMethod=value;}
        public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal value){amount=value;}
        public java.time.LocalDate getPaymentDate(){return paymentDate;} public void setPaymentDate(java.time.LocalDate value){paymentDate=value;}
    }

    class ReconciliationRow {
        private Long id; private LocalDate reconciliationMonth; private BigDecimal systemBalance,financeBalance,differenceAmount;
        private String status,note,confirmedByName; private LocalDateTime confirmedAt;
        public Long getId(){return id;} public void setId(Long v){id=v;}
        public LocalDate getReconciliationMonth(){return reconciliationMonth;} public void setReconciliationMonth(LocalDate v){reconciliationMonth=v;}
        public BigDecimal getSystemBalance(){return systemBalance;} public void setSystemBalance(BigDecimal v){systemBalance=v;}
        public BigDecimal getFinanceBalance(){return financeBalance;} public void setFinanceBalance(BigDecimal v){financeBalance=v;}
        public BigDecimal getDifferenceAmount(){return differenceAmount;} public void setDifferenceAmount(BigDecimal v){differenceAmount=v;}
        public String getStatus(){return status;} public void setStatus(String v){status=v;}
        public String getNote(){return note;} public void setNote(String v){note=v;}
        public String getConfirmedByName(){return confirmedByName;} public void setConfirmedByName(String v){confirmedByName=v;}
        public LocalDateTime getConfirmedAt(){return confirmedAt;} public void setConfirmedAt(LocalDateTime v){confirmedAt=v;}
    }
}
