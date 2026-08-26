SET @reserve_account_remarks_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'reserve_accounts'
    AND column_name = 'remarks'
);

SET @reserve_account_remarks_sql := IF(
  @reserve_account_remarks_exists = 0,
  'ALTER TABLE reserve_accounts ADD COLUMN remarks VARCHAR(500) NULL COMMENT ''Account-level reserve remarks'' AFTER low_balance_alert_enabled',
  'SELECT 1'
);

PREPARE reserve_account_remarks_statement FROM @reserve_account_remarks_sql;
EXECUTE reserve_account_remarks_statement;
DEALLOCATE PREPARE reserve_account_remarks_statement;
