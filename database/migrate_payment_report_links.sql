USE ccps_property_management;
SET NAMES utf8mb4;

ALTER TABLE payment_receipts
  ADD COLUMN fee_account_type VARCHAR(40) NULL AFTER bank_reference,
  ADD COLUMN fee_account_no VARCHAR(120) NULL AFTER fee_account_type;

ALTER TABLE maintenance_work_orders
  ADD COLUMN payer_name VARCHAR(160) NULL AFTER actual_amount,
  ADD COLUMN bank_name VARCHAR(120) NULL AFTER payer_name,
  ADD COLUMN payment_account_no VARCHAR(120) NULL AFTER bank_name,
  ADD COLUMN fee_account_type VARCHAR(40) NULL AFTER payment_account_no,
  ADD COLUMN fee_account_no VARCHAR(120) NULL AFTER fee_account_type;
