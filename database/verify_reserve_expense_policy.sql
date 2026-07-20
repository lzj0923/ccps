USE ccps_property_management;
SET NAMES utf8mb4;

SELECT
  p.name AS project_name,
  u.unit_no,
  fr.transaction_no,
  fr.amount,
  fr.payment_method,
  fr.payment_status,
  fr.confirmation_status,
  ce.attachment_status,
  ra.current_balance,
  rt.amount AS reserve_debit,
  rt.balance_after
FROM cashflow_entries ce
JOIN finance_records fr ON fr.id = ce.finance_record_id
JOIN units u ON u.id = ce.unit_id
JOIN projects p ON p.id = u.project_id
LEFT JOIN reserve_accounts ra ON ra.id = ce.reserve_account_id
LEFT JOIN reserve_transactions rt
  ON rt.finance_record_id = ce.finance_record_id
 AND rt.transaction_type = 'debit'
WHERE ce.direction = 'expense'
ORDER BY ce.occurred_on, ce.id;
