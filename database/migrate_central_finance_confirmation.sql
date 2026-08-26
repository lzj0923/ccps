-- Central finance confirmation keeps the source module's proposed date while
-- finance_records.transaction_date becomes the finance-authoritative posting date.
-- MySQL 5.7 does not support ALTER TABLE ... ADD COLUMN IF NOT EXISTS.
SET @requested_transaction_date_exists = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'finance_records'
    AND COLUMN_NAME = 'requested_transaction_date'
);
SET @add_requested_transaction_date_sql = IF(
  @requested_transaction_date_exists = 0,
  'ALTER TABLE finance_records ADD COLUMN requested_transaction_date DATE NULL COMMENT ''Business-proposed date retained when finance determines the final date'' AFTER currency',
  'SELECT 1'
);
PREPARE add_requested_transaction_date_stmt FROM @add_requested_transaction_date_sql;
EXECUTE add_requested_transaction_date_stmt;
DEALLOCATE PREPARE add_requested_transaction_date_stmt;

UPDATE finance_records
SET requested_transaction_date = transaction_date
WHERE requested_transaction_date IS NULL;
