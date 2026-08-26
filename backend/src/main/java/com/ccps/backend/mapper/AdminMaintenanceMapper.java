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
import com.ccps.backend.dto.AdminMaintenanceOptionsResponse.UnitOption;
import com.ccps.backend.dto.AdminMaintenanceOptionsResponse.VendorOption;
import com.ccps.backend.dto.AdminPropertyMaintenanceResponse;
import com.ccps.backend.dto.AdminRecycleBinItem;

@Mapper
public interface AdminMaintenanceMapper {
    @Select("""
            SELECT u.id AS unit_id, ou.id AS owner_unit_id, o.id AS owner_id, o.full_name AS owner_name,
                   (SELECT t.full_name FROM leases l JOIN tenants t ON t.id = l.tenant_id
                    WHERE l.unit_id = u.id AND l.status = 'active'
                    ORDER BY l.end_date DESC, l.id DESC LIMIT 1) AS tenant_name,
                   p.name AS project_name, u.unit_no, ra.id AS reserve_account_id,
                   COALESCE(ra.current_balance, 0) AS reserve_balance,
                   NOT EXISTS(SELECT 1 FROM owner_unit_services ous
                              WHERE ous.owner_unit_id = ou.id
                                AND ous.service_type = 'RENTAL'
                                AND ous.status = 'ended') AS direct_payment_allowed
            FROM owner_units ou
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            JOIN units u ON u.id = ou.unit_id
            JOIN projects p ON p.id = u.project_id
            LEFT JOIN reserve_accounts ra ON ra.owner_unit_id = ou.id AND ra.status = 'active'
            WHERE ou.status = 'active' AND ou.asset_stage = 'OPERATING' AND ou.is_primary = 1
            ORDER BY p.name, u.unit_no
            """)
    List<UnitOption> findUnitOptions();

    @Select("SELECT id, name, contact_name, phone FROM vendors WHERE status = 'active' ORDER BY name")
    List<VendorOption> findVendorOptions();

    @Select("""
            SELECT ou.id AS owner_unit_id, ou.owner_id, ou.unit_id
            FROM owner_units ou
            WHERE ou.id = #{ownerUnitId} AND ou.owner_id = #{ownerId} AND ou.status = 'active'
            LIMIT 1
            """)
    PropertyContext findPropertyContext(@Param("ownerId") Long ownerId,
            @Param("ownerUnitId") Long ownerUnitId);

    @Select("""
            SELECT mwo.id, mwo.work_order_no, mwo.vendor_id, v.name AS vendor_name,
                   mwo.category, mwo.title, mwo.description, mwo.requested_at,
                   mwo.completed_at, mwo.status, mwo.estimated_amount, mwo.actual_amount,
                   mwo.cashflow_entry_id,
                   (SELECT COUNT(*) FROM document_links dl JOIN documents d ON d.id = dl.document_id
                    WHERE dl.entity_type = 'work_order' AND dl.entity_id = mwo.id
                      AND d.status = 'active') AS attachment_count,
                   mwo.updated_at,
                   CASE WHEN mwo.cashflow_entry_id IS NULL
                              OR COALESCE(fr.confirmation_status, 'pending') <> 'confirmed'
                        THEN TRUE ELSE FALSE END AS editable
            FROM maintenance_work_orders mwo
            LEFT JOIN vendors v ON v.id = mwo.vendor_id
            LEFT JOIN cashflow_entries ce ON ce.id = mwo.cashflow_entry_id
            LEFT JOIN finance_records fr ON fr.id = ce.finance_record_id
            WHERE mwo.unit_id = #{unitId} AND mwo.status <> 'cancelled'
            ORDER BY mwo.requested_at DESC, mwo.id DESC
            """)
    List<AdminPropertyMaintenanceResponse> findPropertyMaintenance(@Param("unitId") Long unitId);

    @Select("""
            SELECT mwo.id, mwo.work_order_no, mwo.vendor_id, v.name AS vendor_name,
                   mwo.category, mwo.title, mwo.description, mwo.requested_at,
                   mwo.completed_at, mwo.status, mwo.estimated_amount, mwo.actual_amount,
                   mwo.cashflow_entry_id,
                   (SELECT COUNT(*) FROM document_links dl JOIN documents d ON d.id = dl.document_id
                    WHERE dl.entity_type = 'work_order' AND dl.entity_id = mwo.id
                      AND d.status = 'active') AS attachment_count,
                   mwo.updated_at,
                   CASE WHEN mwo.cashflow_entry_id IS NULL
                              OR COALESCE(fr.confirmation_status, 'pending') <> 'confirmed'
                        THEN TRUE ELSE FALSE END AS editable
            FROM maintenance_work_orders mwo
            LEFT JOIN vendors v ON v.id = mwo.vendor_id
            LEFT JOIN cashflow_entries ce ON ce.id = mwo.cashflow_entry_id
            LEFT JOIN finance_records fr ON fr.id = ce.finance_record_id
            WHERE mwo.id = #{workOrderId} AND mwo.unit_id = #{unitId}
            LIMIT 1
            """)
    AdminPropertyMaintenanceResponse findPropertyMaintenanceById(@Param("unitId") Long unitId,
            @Param("workOrderId") Long workOrderId);

    @Update("""
            UPDATE maintenance_work_orders
            SET vendor_id = #{vendorId}, category = #{category}, title = #{title},
                description = #{description}, requested_at = #{requestedAt},
                estimated_amount = #{estimatedAmount}, status = #{status}, payer_name = #{payerName},
                bank_name = #{bankName}, payment_account_no = #{paymentAccountNo},
                fee_account_type = #{feeAccountKey}, fee_account_no = #{feeAccountNo}
            WHERE id = #{workOrderId} AND unit_id = #{unitId}
              AND status <> 'cancelled'
            """)
    int updatePropertyMaintenance(@Param("unitId") Long unitId,
            @Param("workOrderId") Long workOrderId, @Param("vendorId") Long vendorId,
            @Param("category") String category, @Param("title") String title,
            @Param("description") String description, @Param("requestedAt") LocalDateTime requestedAt,
            @Param("estimatedAmount") BigDecimal estimatedAmount, @Param("status") String status,
            @Param("payerName") String payerName, @Param("bankName") String bankName,
            @Param("paymentAccountNo") String paymentAccountNo,
            @Param("feeAccountKey") String feeAccountKey, @Param("feeAccountNo") String feeAccountNo);

    @Update("""
            UPDATE maintenance_work_orders
            SET status = 'cancelled'
            WHERE id = #{workOrderId} AND unit_id = #{unitId} AND status <> 'cancelled'
            """)
    int cancelPropertyMaintenance(@Param("unitId") Long unitId,
            @Param("workOrderId") Long workOrderId);

    @Insert("""
            INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by)
            VALUES (#{workOrderId}, #{status}, NOW(), #{note}, #{actorId})
            """)
    int insertPropertyHistory(@Param("workOrderId") Long workOrderId, @Param("status") String status,
            @Param("note") String note, @Param("actorId") Long actorId);

    @Select("""
            SELECT u.id AS unit_id, o.id AS owner_id, ou.id AS owner_unit_id,
                   ra.id AS reserve_account_id, COALESCE(ra.current_balance, 0) AS reserve_balance,
                   NOT EXISTS(SELECT 1 FROM owner_unit_services ous
                              WHERE ous.owner_unit_id = ou.id
                                AND ous.service_type = 'RENTAL'
                                AND ous.status = 'ended') AS direct_payment_allowed
            FROM units u
            JOIN owner_units ou ON ou.unit_id = u.id AND ou.status = 'active'
              AND ou.asset_stage = 'OPERATING' AND ou.is_primary = 1
            JOIN owners o ON o.id = ou.owner_id AND o.status = 'active'
            LEFT JOIN reserve_accounts ra ON ra.owner_unit_id = ou.id AND ra.status = 'active'
            WHERE u.id = #{unitId}
            LIMIT 1 FOR UPDATE
            """)
    UnitContext lockUnitContext(@Param("unitId") Long unitId);

    @Select("""
            SELECT ce.id, ce.finance_record_id, ce.unit_id, ce.owner_id,
                   fr.transaction_no, fr.record_type, fr.payment_method, fr.payment_status,
                   fr.confirmation_status, fr.sync_status, mwo.id AS work_order_id,
                   reserve_debit.reserve_account_id,
                   COALESCE(reserve_debit.amount, 0) AS reserve_debit_amount
            FROM cashflow_entries ce
            JOIN finance_records fr ON fr.id = ce.finance_record_id
            LEFT JOIN maintenance_work_orders mwo ON mwo.cashflow_entry_id = ce.id
            LEFT JOIN (
              SELECT finance_record_id, MAX(reserve_account_id) AS reserve_account_id,
                     SUM(amount) AS amount
              FROM reserve_transactions
              WHERE transaction_type = 'debit'
              GROUP BY finance_record_id
            ) reserve_debit ON reserve_debit.finance_record_id = fr.id
            WHERE ce.id = #{cashflowId} AND ce.direction = 'expense'
            LIMIT 1 FOR UPDATE
            """)
    ExpenseContext lockExpense(@Param("cashflowId") Long cashflowId);

    @Insert("""
            INSERT INTO admin_recycle_bin
              (entity_type, entity_id, finance_record_id, work_order_id,
               previous_payment_status, previous_confirmation_status, previous_sync_status,
               previous_payment_method, previous_reserve_account_id, previous_reserve_amount,
               previous_maintenance_status, deleted_by, expires_at)
            VALUES
              (#{entityType}, #{entityId}, #{financeRecordId}, #{workOrderId},
               #{previousPaymentStatus}, #{previousConfirmationStatus}, #{previousSyncStatus},
               #{previousPaymentMethod}, #{previousReserveAccountId}, #{previousReserveAmount},
               #{previousMaintenanceStatus}, #{deletedBy}, DATE_ADD(NOW(), INTERVAL #{expiresInDays} DAY))
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertRecycleBin(RecycleBinCreate record);

    @Select("""
            SELECT rb.id, rb.entity_type, rb.entity_id,
                   COALESCE(fr.transaction_no, mwo.work_order_no) AS reference_no,
                   COALESCE(mwo.title, ce.description, fr.transaction_no) AS title,
                   p.name AS project_name, u.unit_no,
                   COALESCE(fr.amount, mwo.actual_amount, mwo.estimated_amount, 0) AS amount,
                   COALESCE(rb.previous_payment_status, rb.previous_maintenance_status) AS status,
                   rb.deleted_at, rb.expires_at
            FROM admin_recycle_bin rb
            LEFT JOIN finance_records fr ON fr.id = rb.finance_record_id
            LEFT JOIN cashflow_entries ce ON ce.finance_record_id = fr.id
            LEFT JOIN maintenance_work_orders mwo ON mwo.id = rb.work_order_id
            LEFT JOIN units u ON u.id = COALESCE(fr.unit_id, mwo.unit_id)
            LEFT JOIN projects p ON p.id = u.project_id
            WHERE rb.restored_at IS NULL AND rb.expires_at > NOW()
            ORDER BY rb.deleted_at DESC, rb.id DESC
            """)
    List<AdminRecycleBinItem> findActiveRecycleBin();

    @Select("""
            SELECT id, entity_type, entity_id, finance_record_id, work_order_id,
                   previous_payment_status, previous_confirmation_status, previous_sync_status,
                   previous_payment_method, previous_reserve_account_id, previous_reserve_amount,
                   previous_maintenance_status, expires_at, restored_at
            FROM admin_recycle_bin
            WHERE id = #{id} AND restored_at IS NULL AND expires_at > NOW()
            LIMIT 1 FOR UPDATE
            """)
    RecycleBinContext lockRecycleBin(@Param("id") Long id);

    @Update("""
            UPDATE finance_records
            SET payment_status = #{paymentStatus}, confirmation_status = #{confirmationStatus},
                sync_status = #{syncStatus}, payment_method = #{paymentMethod},
                confirmed_by = NULL, confirmed_at = NULL
            WHERE id = #{financeRecordId} AND payment_status = 'voided'
            """)
    int restoreExpenseFinance(@Param("financeRecordId") Long financeRecordId,
            @Param("paymentStatus") String paymentStatus, @Param("confirmationStatus") String confirmationStatus,
            @Param("syncStatus") String syncStatus, @Param("paymentMethod") String paymentMethod);

    @Update("UPDATE cashflow_entries SET reserve_account_id = #{reserveAccountId} WHERE id = #{cashflowId}")
    int restoreExpenseCashflow(@Param("cashflowId") Long cashflowId, @Param("reserveAccountId") Long reserveAccountId);

    @Update("UPDATE maintenance_work_orders SET status = #{status} WHERE id = #{workOrderId} AND status = 'cancelled'")
    int restoreMaintenance(@Param("workOrderId") Long workOrderId, @Param("status") String status);

    @Update("UPDATE maintenance_work_orders SET status = 'submitted', completed_at = NULL WHERE id = #{workOrderId} AND status = 'completed'")
    int resetRejectedMaintenanceWorkOrder(@Param("workOrderId") Long workOrderId);

    @Update("UPDATE admin_recycle_bin SET restored_by = #{actorId}, restored_at = NOW() WHERE id = #{id} AND restored_at IS NULL")
    int markRecycleBinRestored(@Param("id") Long id, @Param("actorId") Long actorId);

    @Delete("DELETE FROM admin_recycle_bin WHERE id = #{id} AND restored_at IS NULL")
    int purgeRecycleBinItem(@Param("id") Long id);

    @Delete("DELETE FROM admin_recycle_bin WHERE restored_at IS NULL AND expires_at <= NOW()")
    int purgeExpiredRecycleBin();

    @Update("""
            UPDATE finance_records
            SET record_type = 'property_expense', amount = #{amount}, transaction_date = #{occurredOn},
                payment_method = #{paymentMethod}, payment_status = #{paymentStatus},
                confirmation_status = #{confirmationStatus}, sync_status = 'not_synced',
                confirmed_by = CASE WHEN #{confirmationStatus} = 'confirmed' THEN #{actorId} ELSE NULL END,
                confirmed_at = CASE WHEN #{confirmationStatus} = 'confirmed' THEN NOW() ELSE NULL END
            WHERE id = #{financeRecordId} AND payment_status <> 'voided'
            """)
    int updateExpenseFinance(@Param("financeRecordId") Long financeRecordId,
            @Param("amount") BigDecimal amount, @Param("occurredOn") LocalDate occurredOn,
            @Param("paymentMethod") String paymentMethod, @Param("paymentStatus") String paymentStatus,
            @Param("confirmationStatus") String confirmationStatus, @Param("actorId") Long actorId);

    @Update("""
            UPDATE cashflow_entries
            SET category = #{category}, description = #{description}, occurred_on = #{occurredOn},
                reserve_account_id = #{reserveAccountId}, attachment_status = 'not_required'
            WHERE id = #{cashflowId} AND direction = 'expense'
            """)
    int updateExpenseCashflow(@Param("cashflowId") Long cashflowId,
            @Param("category") String category, @Param("description") String description,
            @Param("occurredOn") LocalDate occurredOn, @Param("reserveAccountId") Long reserveAccountId);

    @Update("UPDATE reserve_accounts SET current_balance = current_balance + #{amount} WHERE id = #{reserveAccountId}")
    int restoreExpenseReserve(@Param("reserveAccountId") Long reserveAccountId, @Param("amount") BigDecimal amount);

    @Update("UPDATE reserve_accounts SET current_balance = current_balance - #{amount} WHERE id = #{reserveAccountId}")
    int debitExpenseReserve(@Param("reserveAccountId") Long reserveAccountId, @Param("amount") BigDecimal amount);

    @Select("SELECT current_balance FROM reserve_accounts WHERE id = #{reserveAccountId}")
    BigDecimal findExpenseReserveBalance(@Param("reserveAccountId") Long reserveAccountId);

    @Update("UPDATE reserve_transactions SET transaction_type = 'reversed', note = CONCAT(COALESCE(note, ''), '（记录修改/作废回冲）') WHERE finance_record_id = #{financeRecordId} AND transaction_type = 'debit'")
    int reverseExpenseReserveDebits(@Param("financeRecordId") Long financeRecordId);

    @Insert("""
            INSERT INTO reserve_transactions
              (reserve_account_id, finance_record_id, transaction_type, amount,
               occurred_at, balance_after, note, created_by)
            VALUES
              (#{reserveAccountId}, #{financeRecordId}, #{transactionType}, #{amount},
               NOW(), #{balanceAfter}, #{note}, #{actorId})
            """)
    int insertExpenseReserveTransaction(@Param("reserveAccountId") Long reserveAccountId,
            @Param("financeRecordId") Long financeRecordId, @Param("transactionType") String transactionType,
            @Param("amount") BigDecimal amount, @Param("balanceAfter") BigDecimal balanceAfter,
            @Param("note") String note, @Param("actorId") Long actorId);

    @Update("""
            UPDATE finance_records
            SET payment_status = 'voided', confirmation_status = 'rejected',
                sync_status = 'not_synced', confirmed_by = NULL, confirmed_at = NULL
            WHERE id = #{financeRecordId} AND payment_status <> 'voided'
            """)
    int voidExpenseFinance(@Param("financeRecordId") Long financeRecordId);

    @Update("UPDATE cashflow_entries SET reserve_account_id = NULL WHERE id = #{cashflowId}")
    int clearExpenseReserve(@Param("cashflowId") Long cashflowId);

    @Select("""
            SELECT mwo.id, mwo.status, mwo.unit_id, COALESCE(mwo.owner_id, ou.owner_id) AS owner_id, mwo.vendor_id,
                   mwo.cashflow_entry_id, ce.finance_record_id,
                   fr.payment_status, fr.confirmation_status, fr.sync_status,
                   mwo.payer_name, mwo.bank_name, mwo.payment_account_no,
                   mwo.fee_account_type AS fee_account_key, mwo.fee_account_no,
                   ou.id AS owner_unit_id, ra.id AS reserve_account_id,
                   COALESCE(ra.current_balance, 0) AS reserve_balance,
                   NOT EXISTS(SELECT 1 FROM owner_unit_services ous
                              WHERE ous.owner_unit_id = ou.id
                                AND ous.service_type = 'RENTAL'
                                AND ous.status = 'ended') AS direct_payment_allowed,
                   COALESCE((SELECT SUM(rt.amount) FROM reserve_transactions rt
                     WHERE rt.transaction_type = 'debit'
                       AND (rt.maintenance_work_order_id = mwo.id
                            OR rt.finance_record_id = ce.finance_record_id)), 0)
                     AS reserve_deducted_amount
            FROM maintenance_work_orders mwo
            LEFT JOIN cashflow_entries ce ON ce.id = mwo.cashflow_entry_id
            LEFT JOIN finance_records fr ON fr.id = ce.finance_record_id
            LEFT JOIN owner_units ou ON ou.unit_id = mwo.unit_id AND ou.status = 'active' AND ou.is_primary = 1
            LEFT JOIN reserve_accounts ra ON ra.owner_unit_id = ou.id AND ra.status = 'active'
            WHERE mwo.id = #{workOrderId}
            LIMIT 1
            FOR UPDATE
            """)
    CompletionContext lockContext(@Param("workOrderId") Long workOrderId);

    @Update("""
            UPDATE finance_records
            SET confirmation_status = 'pending', payment_status = 'unpaid',
                sync_status = 'not_synced', sync_batch_id = NULL,
                confirmed_by = NULL, confirmed_at = NULL
            WHERE id = #{financeRecordId} AND record_type = 'property_expense'
              AND confirmation_status = 'rejected' AND payment_status = 'voided'
            """)
    int resubmitRejectedMaintenanceFinance(@Param("financeRecordId") Long financeRecordId);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, before_data, after_data)
            VALUES (#{actorId}, 'resubmit_maintenance_finance', 'maintenance_work_order', #{workOrderId},
                    JSON_OBJECT('confirmationStatus', 'rejected', 'paymentStatus', 'voided'),
                    JSON_OBJECT('confirmationStatus', 'pending', 'paymentStatus', 'unpaid', 'note', #{note}))
            """)
    int insertResubmitFinanceAudit(@Param("actorId") Long actorId,
            @Param("workOrderId") Long workOrderId, @Param("note") String note);

    @Select("""
            SELECT COUNT(*) FROM document_links dl
            JOIN documents d ON d.id = dl.document_id
            WHERE dl.entity_type = 'work_order' AND dl.entity_id = #{workOrderId}
              AND dl.relation_type = #{relationType}
              AND d.document_type = 'maintenance_attachment' AND d.status = 'active'
            """)
    int countPhotos(@Param("workOrderId") Long workOrderId, @Param("relationType") String relationType);

    @Insert("""
            INSERT INTO finance_records
              (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date,
               payment_method, payment_status, confirmation_status, confirmed_by, confirmed_at,
               sync_status, created_by)
            VALUES
              (#{transactionNo}, 'property_expense', #{unitId}, #{ownerId}, #{amount}, 'MYR', CURRENT_DATE,
               #{paymentMethod}, CASE WHEN #{confirmationStatus} = 'confirmed' THEN 'paid' ELSE 'unpaid' END, #{confirmationStatus},
               CASE WHEN #{confirmationStatus} = 'confirmed' THEN #{actorId} ELSE NULL END,
               CASE WHEN #{confirmationStatus} = 'confirmed' THEN NOW() ELSE NULL END,
               'not_synced', #{actorId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertFinance(NewFinance finance);

    @Update("""
            UPDATE finance_records
            SET amount = #{amount}, transaction_date = CURRENT_DATE, payment_method = #{paymentMethod},
                payment_status = CASE WHEN #{paymentMethod} = 'reserve_account' THEN 'paid' ELSE 'unpaid' END,
                confirmation_status = #{confirmationStatus},
                confirmed_by = CASE WHEN #{confirmationStatus} = 'confirmed' THEN #{actorId} ELSE NULL END,
                confirmed_at = CASE WHEN #{confirmationStatus} = 'confirmed' THEN NOW() ELSE NULL END,
                sync_status = 'not_synced'
            WHERE id = #{financeRecordId}
            """)
    int updateFinance(@Param("financeRecordId") Long financeRecordId, @Param("amount") BigDecimal amount,
            @Param("paymentMethod") String paymentMethod,
            @Param("confirmationStatus") String confirmationStatus, @Param("actorId") Long actorId);

    @Insert("""
            INSERT INTO cashflow_entries
              (finance_record_id, unit_id, owner_id, vendor_id, direction, category, description,
               occurred_on, reserve_account_id, attachment_status)
            VALUES
              (#{financeRecordId}, #{unitId}, #{ownerId}, #{vendorId}, 'expense', 'maintenance',
               #{description}, CURRENT_DATE, #{reserveAccountId}, 'complete')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertCashflow(NewCashflow cashflow);

    @Update("""
            UPDATE cashflow_entries
            SET reserve_account_id = #{reserveAccountId}, attachment_status = 'complete',
                description = #{description}, occurred_on = CURRENT_DATE
            WHERE id = #{cashflowEntryId}
            """)
    int updateCashflow(@Param("cashflowEntryId") Long cashflowEntryId,
            @Param("reserveAccountId") Long reserveAccountId, @Param("description") String description);

    @Update("""
            UPDATE reserve_transactions
            SET maintenance_work_order_id = #{workOrderId}
            WHERE finance_record_id = #{financeRecordId} AND transaction_type = 'debit'
              AND maintenance_work_order_id IS NULL
            """)
    int linkReserveDebitToWorkOrder(@Param("financeRecordId") Long financeRecordId,
            @Param("workOrderId") Long workOrderId);

    @Select("""
            SELECT COALESCE(SUM(amount), 0)
            FROM reserve_transactions
            WHERE transaction_type = 'debit'
              AND (finance_record_id = #{financeRecordId} OR maintenance_work_order_id = #{workOrderId})
            """)
    BigDecimal findReserveDebitAmount(@Param("financeRecordId") Long financeRecordId,
            @Param("workOrderId") Long workOrderId);

    @Update("""
            UPDATE maintenance_work_orders
            SET cashflow_entry_id = #{cashflowEntryId}, actual_amount = #{amount},
                completed_at = NOW(), status = 'completed'
            WHERE id = #{workOrderId}
            """)
    int completeWorkOrder(@Param("workOrderId") Long workOrderId,
            @Param("cashflowEntryId") Long cashflowEntryId, @Param("amount") BigDecimal amount);

    @Insert("""
            INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by)
            VALUES (#{workOrderId}, 'completed', NOW(), #{note}, #{actorId})
            """)
    int insertHistory(@Param("workOrderId") Long workOrderId, @Param("note") String note,
            @Param("actorId") Long actorId);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, after_data)
            VALUES (#{actorId}, 'complete_maintenance', 'maintenance_work_order', #{workOrderId},
                    JSON_OBJECT('amount', #{amount}, 'settlementMethod', #{settlementMethod}))
            """)
    int insertAudit(@Param("actorId") Long actorId, @Param("workOrderId") Long workOrderId,
            @Param("amount") BigDecimal amount, @Param("settlementMethod") String settlementMethod);

    @Insert("""
            INSERT INTO finance_records
              (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date,
               payment_method, payment_status, confirmation_status, confirmed_by, confirmed_at,
               sync_status, created_by)
            VALUES
              (#{transactionNo}, 'property_expense', #{unitId}, #{ownerId}, #{amount}, 'MYR', #{occurredOn},
               #{paymentMethod}, #{paymentStatus}, #{confirmationStatus},
               CASE WHEN #{confirmationStatus} = 'confirmed' THEN #{actorId} ELSE NULL END,
               CASE WHEN #{confirmationStatus} = 'confirmed' THEN NOW() ELSE NULL END,
               'not_synced', #{actorId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertExpenseFinance(NewExpenseFinance finance);

    @Insert("""
            INSERT INTO payment_receipts (finance_record_id, payer_name, bank_reference, fee_account_type, fee_account_no)
            VALUES (#{financeRecordId}, NULLIF(#{payerName}, ''),
                    NULLIF(CONCAT_WS(' | ', NULLIF(#{bankName}, ''), NULLIF(#{paymentAccountNo}, '')), ''),
                    NULLIF(#{feeAccountKey}, ''), NULLIF(#{feeAccountNo}, ''))
            ON DUPLICATE KEY UPDATE payer_name = VALUES(payer_name), bank_reference = VALUES(bank_reference),
                fee_account_type = COALESCE(VALUES(fee_account_type), fee_account_type),
                fee_account_no = COALESCE(VALUES(fee_account_no), fee_account_no)
            """)
    int upsertPaymentReceipt(@Param("financeRecordId") Long financeRecordId,
            @Param("payerName") String payerName, @Param("bankName") String bankName,
            @Param("paymentAccountNo") String paymentAccountNo,
            @Param("feeAccountKey") String feeAccountKey, @Param("feeAccountNo") String feeAccountNo);

    @Insert("""
            INSERT INTO cashflow_entries
              (finance_record_id, unit_id, owner_id, direction, category, description,
               occurred_on, reserve_account_id, attachment_status)
            VALUES
              (#{financeRecordId}, #{unitId}, #{ownerId}, 'expense', #{category}, #{description},
               #{occurredOn}, #{reserveAccountId}, 'not_required')
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertExpenseCashflow(NewExpenseCashflow cashflow);

    @Insert("""
            INSERT INTO maintenance_work_orders
               (work_order_no, unit_id, owner_id, vendor_id, category, title, description,
                requested_at, status, estimated_amount, payer_name, bank_name, payment_account_no,
                fee_account_type, fee_account_no, created_by)
            VALUES
               (#{workOrderNo}, #{unitId}, #{ownerId}, #{vendorId}, #{category}, #{title},
                #{description}, #{requestedAt}, 'submitted', #{estimatedAmount}, #{payerName}, #{bankName},
                #{paymentAccountNo}, #{feeAccountKey}, #{feeAccountNo}, #{actorId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertWorkOrder(NewWorkOrder workOrder);

    @Insert("""
            INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by)
            VALUES (#{workOrderId}, 'submitted', #{occurredAt}, '管理員建立維修工單', #{actorId})
            """)
    int insertSubmittedHistory(@Param("workOrderId") Long workOrderId,
            @Param("occurredAt") LocalDateTime occurredAt, @Param("actorId") Long actorId);

    @Insert("""
            INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, after_data)
            VALUES (#{actorId}, #{action}, #{entityType}, #{entityId}, #{afterData})
            """)
    int insertCreateAudit(@Param("actorId") Long actorId, @Param("action") String action,
            @Param("entityType") String entityType, @Param("entityId") Long entityId,
            @Param("afterData") String afterData);

    class PropertyContext {
        private Long ownerUnitId; private Long ownerId; private Long unitId;
        public Long getOwnerUnitId() { return ownerUnitId; } public void setOwnerUnitId(Long value) { ownerUnitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
    }

    class UnitContext {
        private Long unitId; private Long ownerId; private Long ownerUnitId;
        private Long reserveAccountId; private BigDecimal reserveBalance; private boolean directPaymentAllowed;
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public Long getOwnerUnitId() { return ownerUnitId; } public void setOwnerUnitId(Long value) { ownerUnitId = value; }
        public Long getReserveAccountId() { return reserveAccountId; } public void setReserveAccountId(Long value) { reserveAccountId = value; }
        public BigDecimal getReserveBalance() { return reserveBalance; } public void setReserveBalance(BigDecimal value) { reserveBalance = value; }
        public boolean isDirectPaymentAllowed() { return directPaymentAllowed; } public void setDirectPaymentAllowed(boolean value) { directPaymentAllowed = value; }
    }

    class ExpenseContext {
        private Long id; private Long financeRecordId; private Long unitId; private Long ownerId;
        private Long workOrderId; private Long reserveAccountId; private BigDecimal reserveDebitAmount;
        private String transactionNo; private String recordType; private String paymentMethod; private String paymentStatus;
        private String confirmationStatus; private String syncStatus;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public Long getFinanceRecordId() { return financeRecordId; } public void setFinanceRecordId(Long value) { financeRecordId = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public Long getWorkOrderId() { return workOrderId; } public void setWorkOrderId(Long value) { workOrderId = value; }
        public Long getReserveAccountId() { return reserveAccountId; } public void setReserveAccountId(Long value) { reserveAccountId = value; }
        public BigDecimal getReserveDebitAmount() { return reserveDebitAmount; } public void setReserveDebitAmount(BigDecimal value) { reserveDebitAmount = value; }
        public String getTransactionNo() { return transactionNo; } public void setTransactionNo(String value) { transactionNo = value; }
        public String getRecordType() { return recordType; } public void setRecordType(String value) { recordType = value; }
        public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String value) { paymentMethod = value; }
        public String getPaymentStatus() { return paymentStatus; } public void setPaymentStatus(String value) { paymentStatus = value; }
        public String getConfirmationStatus() { return confirmationStatus; } public void setConfirmationStatus(String value) { confirmationStatus = value; }
        public String getSyncStatus() { return syncStatus; } public void setSyncStatus(String value) { syncStatus = value; }
    }

    class RecycleBinCreate {
        private Long id; private String entityType; private Long entityId; private Long financeRecordId; private Long workOrderId;
        private String previousPaymentStatus; private String previousConfirmationStatus; private String previousSyncStatus;
        private String previousPaymentMethod; private Long previousReserveAccountId; private BigDecimal previousReserveAmount = BigDecimal.ZERO;
        private String previousMaintenanceStatus; private Long deletedBy; private int expiresInDays = 30;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public String getEntityType() { return entityType; } public void setEntityType(String value) { entityType = value; }
        public Long getEntityId() { return entityId; } public void setEntityId(Long value) { entityId = value; }
        public Long getFinanceRecordId() { return financeRecordId; } public void setFinanceRecordId(Long value) { financeRecordId = value; }
        public Long getWorkOrderId() { return workOrderId; } public void setWorkOrderId(Long value) { workOrderId = value; }
        public String getPreviousPaymentStatus() { return previousPaymentStatus; } public void setPreviousPaymentStatus(String value) { previousPaymentStatus = value; }
        public String getPreviousConfirmationStatus() { return previousConfirmationStatus; } public void setPreviousConfirmationStatus(String value) { previousConfirmationStatus = value; }
        public String getPreviousSyncStatus() { return previousSyncStatus; } public void setPreviousSyncStatus(String value) { previousSyncStatus = value; }
        public String getPreviousPaymentMethod() { return previousPaymentMethod; } public void setPreviousPaymentMethod(String value) { previousPaymentMethod = value; }
        public Long getPreviousReserveAccountId() { return previousReserveAccountId; } public void setPreviousReserveAccountId(Long value) { previousReserveAccountId = value; }
        public BigDecimal getPreviousReserveAmount() { return previousReserveAmount; } public void setPreviousReserveAmount(BigDecimal value) { previousReserveAmount = value; }
        public String getPreviousMaintenanceStatus() { return previousMaintenanceStatus; } public void setPreviousMaintenanceStatus(String value) { previousMaintenanceStatus = value; }
        public Long getDeletedBy() { return deletedBy; } public void setDeletedBy(Long value) { deletedBy = value; }
        public int getExpiresInDays() { return expiresInDays; } public void setExpiresInDays(int value) { expiresInDays = value; }
    }

    class RecycleBinContext {
        private Long id; private String entityType; private Long entityId; private Long financeRecordId; private Long workOrderId;
        private String previousPaymentStatus; private String previousConfirmationStatus; private String previousSyncStatus;
        private String previousPaymentMethod; private Long previousReserveAccountId; private BigDecimal previousReserveAmount;
        private String previousMaintenanceStatus; private LocalDateTime expiresAt; private LocalDateTime restoredAt;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public String getEntityType() { return entityType; } public void setEntityType(String value) { entityType = value; }
        public Long getEntityId() { return entityId; } public void setEntityId(Long value) { entityId = value; }
        public Long getFinanceRecordId() { return financeRecordId; } public void setFinanceRecordId(Long value) { financeRecordId = value; }
        public Long getWorkOrderId() { return workOrderId; } public void setWorkOrderId(Long value) { workOrderId = value; }
        public String getPreviousPaymentStatus() { return previousPaymentStatus; } public void setPreviousPaymentStatus(String value) { previousPaymentStatus = value; }
        public String getPreviousConfirmationStatus() { return previousConfirmationStatus; } public void setPreviousConfirmationStatus(String value) { previousConfirmationStatus = value; }
        public String getPreviousSyncStatus() { return previousSyncStatus; } public void setPreviousSyncStatus(String value) { previousSyncStatus = value; }
        public String getPreviousPaymentMethod() { return previousPaymentMethod; } public void setPreviousPaymentMethod(String value) { previousPaymentMethod = value; }
        public Long getPreviousReserveAccountId() { return previousReserveAccountId; } public void setPreviousReserveAccountId(Long value) { previousReserveAccountId = value; }
        public BigDecimal getPreviousReserveAmount() { return previousReserveAmount; } public void setPreviousReserveAmount(BigDecimal value) { previousReserveAmount = value; }
        public String getPreviousMaintenanceStatus() { return previousMaintenanceStatus; } public void setPreviousMaintenanceStatus(String value) { previousMaintenanceStatus = value; }
    }

    class NewExpenseFinance {
        private Long id; private String transactionNo; private Long unitId; private Long ownerId;
        private BigDecimal amount; private LocalDate occurredOn; private String paymentMethod;
        private String paymentStatus; private String confirmationStatus; private Long actorId;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public String getTransactionNo() { return transactionNo; } public void setTransactionNo(String value) { transactionNo = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal value) { amount = value; }
        public LocalDate getOccurredOn() { return occurredOn; } public void setOccurredOn(LocalDate value) { occurredOn = value; }
        public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String value) { paymentMethod = value; }
        public String getPaymentStatus() { return paymentStatus; } public void setPaymentStatus(String value) { paymentStatus = value; }
        public String getConfirmationStatus() { return confirmationStatus; } public void setConfirmationStatus(String value) { confirmationStatus = value; }
        public Long getActorId() { return actorId; } public void setActorId(Long value) { actorId = value; }
    }

    class NewExpenseCashflow {
        private Long id; private Long financeRecordId; private Long unitId; private Long ownerId;
        private String category; private String description; private LocalDate occurredOn; private Long reserveAccountId;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public Long getFinanceRecordId() { return financeRecordId; } public void setFinanceRecordId(Long value) { financeRecordId = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public String getCategory() { return category; } public void setCategory(String value) { category = value; }
        public String getDescription() { return description; } public void setDescription(String value) { description = value; }
        public LocalDate getOccurredOn() { return occurredOn; } public void setOccurredOn(LocalDate value) { occurredOn = value; }
        public Long getReserveAccountId() { return reserveAccountId; } public void setReserveAccountId(Long value) { reserveAccountId = value; }
    }

    class NewWorkOrder {
        private Long id; private String workOrderNo; private Long unitId; private Long ownerId; private Long vendorId;
        private String category; private String title; private String description; private LocalDateTime requestedAt;
        private BigDecimal estimatedAmount; private Long actorId; private String payerName; private String bankName;
        private String paymentAccountNo; private String feeAccountKey; private String feeAccountNo;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public String getWorkOrderNo() { return workOrderNo; } public void setWorkOrderNo(String value) { workOrderNo = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public Long getVendorId() { return vendorId; } public void setVendorId(Long value) { vendorId = value; }
        public String getCategory() { return category; } public void setCategory(String value) { category = value; }
        public String getTitle() { return title; } public void setTitle(String value) { title = value; }
        public String getDescription() { return description; } public void setDescription(String value) { description = value; }
        public LocalDateTime getRequestedAt() { return requestedAt; } public void setRequestedAt(LocalDateTime value) { requestedAt = value; }
        public BigDecimal getEstimatedAmount() { return estimatedAmount; } public void setEstimatedAmount(BigDecimal value) { estimatedAmount = value; }
        public Long getActorId() { return actorId; } public void setActorId(Long value) { actorId = value; }
        public String getPayerName() { return payerName; } public void setPayerName(String value) { payerName = value; }
        public String getBankName() { return bankName; } public void setBankName(String value) { bankName = value; }
        public String getPaymentAccountNo() { return paymentAccountNo; } public void setPaymentAccountNo(String value) { paymentAccountNo = value; }
        public String getFeeAccountKey() { return feeAccountKey; } public void setFeeAccountKey(String value) { feeAccountKey = value; }
        public String getFeeAccountNo() { return feeAccountNo; } public void setFeeAccountNo(String value) { feeAccountNo = value; }
    }

    class CompletionContext {
        private Long id; private String status; private Long unitId; private Long ownerId; private Long vendorId;
        private Long cashflowEntryId; private Long financeRecordId; private Long ownerUnitId;
        private String paymentStatus; private String confirmationStatus; private String syncStatus;
        private Long reserveAccountId; private BigDecimal reserveBalance; private BigDecimal reserveDeductedAmount; private boolean directPaymentAllowed;
        private String payerName; private String bankName; private String paymentAccountNo; private String feeAccountKey; private String feeAccountNo;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public String getStatus() { return status; } public void setStatus(String value) { status = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public Long getVendorId() { return vendorId; } public void setVendorId(Long value) { vendorId = value; }
        public Long getCashflowEntryId() { return cashflowEntryId; } public void setCashflowEntryId(Long value) { cashflowEntryId = value; }
        public Long getFinanceRecordId() { return financeRecordId; } public void setFinanceRecordId(Long value) { financeRecordId = value; }
        public String getPaymentStatus() { return paymentStatus; } public void setPaymentStatus(String value) { paymentStatus = value; }
        public String getConfirmationStatus() { return confirmationStatus; } public void setConfirmationStatus(String value) { confirmationStatus = value; }
        public String getSyncStatus() { return syncStatus; } public void setSyncStatus(String value) { syncStatus = value; }
        public Long getOwnerUnitId() { return ownerUnitId; } public void setOwnerUnitId(Long value) { ownerUnitId = value; }
        public Long getReserveAccountId() { return reserveAccountId; } public void setReserveAccountId(Long value) { reserveAccountId = value; }
        public BigDecimal getReserveBalance() { return reserveBalance; } public void setReserveBalance(BigDecimal value) { reserveBalance = value; }
        public BigDecimal getReserveDeductedAmount() { return reserveDeductedAmount; } public void setReserveDeductedAmount(BigDecimal value) { reserveDeductedAmount = value; }
        public boolean isDirectPaymentAllowed() { return directPaymentAllowed; } public void setDirectPaymentAllowed(boolean value) { directPaymentAllowed = value; }
        public String getPayerName() { return payerName; } public void setPayerName(String value) { payerName = value; }
        public String getBankName() { return bankName; } public void setBankName(String value) { bankName = value; }
        public String getPaymentAccountNo() { return paymentAccountNo; } public void setPaymentAccountNo(String value) { paymentAccountNo = value; }
        public String getFeeAccountKey() { return feeAccountKey; } public void setFeeAccountKey(String value) { feeAccountKey = value; }
        public String getFeeAccountNo() { return feeAccountNo; } public void setFeeAccountNo(String value) { feeAccountNo = value; }
    }

    class NewFinance {
        private Long id; private String transactionNo; private Long unitId; private Long ownerId;
        private BigDecimal amount; private String paymentMethod; private String confirmationStatus; private Long actorId;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public String getTransactionNo() { return transactionNo; } public void setTransactionNo(String value) { transactionNo = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal value) { amount = value; }
        public String getPaymentMethod() { return paymentMethod; } public void setPaymentMethod(String value) { paymentMethod = value; }
        public String getConfirmationStatus() { return confirmationStatus; } public void setConfirmationStatus(String value) { confirmationStatus = value; }
        public Long getActorId() { return actorId; } public void setActorId(Long value) { actorId = value; }
    }

    class NewCashflow {
        private Long id; private Long financeRecordId; private Long unitId; private Long ownerId;
        private Long vendorId; private String description; private Long reserveAccountId;
        public Long getId() { return id; } public void setId(Long value) { id = value; }
        public Long getFinanceRecordId() { return financeRecordId; } public void setFinanceRecordId(Long value) { financeRecordId = value; }
        public Long getUnitId() { return unitId; } public void setUnitId(Long value) { unitId = value; }
        public Long getOwnerId() { return ownerId; } public void setOwnerId(Long value) { ownerId = value; }
        public Long getVendorId() { return vendorId; } public void setVendorId(Long value) { vendorId = value; }
        public String getDescription() { return description; } public void setDescription(String value) { description = value; }
        public Long getReserveAccountId() { return reserveAccountId; } public void setReserveAccountId(Long value) { reserveAccountId = value; }
    }
}
