START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS duplicate_maintenance_reserve_debits;
CREATE TEMPORARY TABLE duplicate_maintenance_reserve_debits AS
SELECT DISTINCT
       manual.id AS transaction_id,
       manual.reserve_account_id,
       manual.finance_record_id,
       manual.maintenance_work_order_id,
       manual.amount
FROM reserve_transactions manual
JOIN reserve_transactions automatic
  ON automatic.finance_record_id = manual.finance_record_id
 AND automatic.id <> manual.id
 AND automatic.transaction_type = 'debit'
 AND automatic.amount = manual.amount
 AND automatic.note = '支出由預備金自動扣除'
JOIN maintenance_work_orders work_order
  ON work_order.id = manual.maintenance_work_order_id
 AND work_order.status = 'completed'
WHERE manual.transaction_type = 'debit'
  AND manual.note = CONCAT('維修工單 ', manual.maintenance_work_order_id, ' 完成扣款')
  AND manual.amount = work_order.actual_amount;

UPDATE reserve_accounts account
JOIN (
  SELECT reserve_account_id, SUM(amount) AS refund_amount
  FROM duplicate_maintenance_reserve_debits
  GROUP BY reserve_account_id
) duplicate_debit ON duplicate_debit.reserve_account_id = account.id
SET account.current_balance = account.current_balance + duplicate_debit.refund_amount;

UPDATE reserve_transactions transaction_row
JOIN duplicate_maintenance_reserve_debits duplicate_debit
  ON duplicate_debit.transaction_id = transaction_row.id
JOIN reserve_accounts account
  ON account.id = transaction_row.reserve_account_id
SET transaction_row.transaction_type = 'adjustment',
    transaction_row.balance_after = account.current_balance,
    transaction_row.note = CONCAT('重複維修扣款回沖 · 原記錄：', transaction_row.note);

INSERT INTO audit_logs
  (actor_user_id, action, entity_type, entity_id, before_data, after_data)
SELECT NULL,
       'reverse_duplicate_maintenance_reserve_debit',
       'maintenance_work_order',
       maintenance_work_order_id,
       JSON_OBJECT('duplicateTransactionId', transaction_id, 'amount', amount),
       JSON_OBJECT('transactionType', 'adjustment', 'reserveBalanceRestored', amount)
FROM duplicate_maintenance_reserve_debits;

COMMIT;

SELECT transaction_id, reserve_account_id, finance_record_id,
       maintenance_work_order_id, amount
FROM duplicate_maintenance_reserve_debits
ORDER BY transaction_id;
