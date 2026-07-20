USE ccps_property_management;
SET NAMES utf8mb4;
START TRANSACTION;

SET @verify_owner = (SELECT id FROM owners WHERE full_name = 'Tan Wei Ming' LIMIT 1);
SET @verify_unit = (SELECT u.id FROM units u JOIN projects p ON p.id = u.project_id WHERE p.name = 'Bangsar South Suites' AND u.unit_no = 'G-21-01' LIMIT 1);

INSERT INTO finance_records
  (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date,
   payment_status, confirmation_status, sync_status)
VALUES
  ('VERIFY-RESERVE-POLICY', 'cashflow', @verify_unit, @verify_owner, 1000.00, 'MYR', CURRENT_DATE,
   'unpaid', 'pending', 'not_synced');
SET @verify_finance = LAST_INSERT_ID();
INSERT INTO cashflow_entries
  (finance_record_id, unit_id, owner_id, direction, category, description, occurred_on, attachment_status)
VALUES
  (@verify_finance, @verify_unit, @verify_owner, 'expense', 'maintenance', 'Reserve policy runtime verification', CURRENT_DATE, 'missing');
SELECT 'before_confirmation' AS checkpoint, reserve_account_id, attachment_status
  FROM cashflow_entries WHERE finance_record_id = @verify_finance;

UPDATE finance_records
   SET payment_status = 'paid', confirmation_status = 'confirmed'
 WHERE id = @verify_finance;
SELECT 'after_confirmation' AS checkpoint, fr.payment_method, fr.payment_status,
       fr.confirmation_status, ce.reserve_account_id, ce.attachment_status,
       rt.amount AS reserve_debit
  FROM finance_records fr
  JOIN cashflow_entries ce ON ce.finance_record_id = fr.id
  LEFT JOIN reserve_transactions rt
    ON rt.finance_record_id = fr.id AND rt.transaction_type = 'debit'
 WHERE fr.id = @verify_finance;

ROLLBACK;
