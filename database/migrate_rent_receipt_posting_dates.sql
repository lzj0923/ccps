USE ccps_property_management;
SET NAMES utf8mb4;

-- finance_records.transaction_date remains the business posting date.
-- receipt_date records when the money actually reached the account.
SET @receipt_date_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'finance_records' AND column_name = 'receipt_date'
);
SET @receipt_date_sql := IF(
  @receipt_date_exists = 0,
  'ALTER TABLE finance_records ADD COLUMN receipt_date DATE NULL COMMENT ''Actual cash receipt date used for accounting reconciliation'' AFTER transaction_date',
  'SELECT 1'
);
PREPARE receipt_date_statement FROM @receipt_date_sql;
EXECUTE receipt_date_statement;
DEALLOCATE PREPARE receipt_date_statement;

UPDATE finance_records SET receipt_date = transaction_date WHERE receipt_date IS NULL;

SET @receipt_date_index_exists := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'finance_records' AND index_name = 'idx_finance_records_receipt_date'
);
SET @receipt_date_index_sql := IF(
  @receipt_date_index_exists = 0,
  'ALTER TABLE finance_records ADD KEY idx_finance_records_receipt_date (receipt_date, record_type, confirmation_status)',
  'SELECT 1'
);
PREPARE receipt_date_index_statement FROM @receipt_date_index_sql;
EXECUTE receipt_date_index_statement;
DEALLOCATE PREPARE receipt_date_index_statement;

-- Store the amount assigned to the linked rental month separately from any
-- incomplete future-month prepayment kept in the same finance receipt.
SET @allocated_amount_exists := (
  SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = 'rent_payments' AND column_name = 'allocated_amount'
);
SET @allocated_amount_sql := IF(
  @allocated_amount_exists = 0,
  'ALTER TABLE rent_payments ADD COLUMN allocated_amount DECIMAL(18,2) NULL COMMENT ''Amount recognized for the linked rental month'' AFTER finance_record_id',
  'SELECT 1'
);
PREPARE allocated_amount_statement FROM @allocated_amount_sql;
EXECUTE allocated_amount_statement;
DEALLOCATE PREPARE allocated_amount_statement;

UPDATE rent_payments rp
JOIN finance_records fr ON fr.id = rp.finance_record_id
SET rp.allocated_amount = fr.amount
WHERE rp.allocated_amount IS NULL;
ALTER TABLE rent_payments MODIFY COLUMN allocated_amount DECIMAL(18,2) NOT NULL
  COMMENT 'Amount recognized for the linked rental month';
