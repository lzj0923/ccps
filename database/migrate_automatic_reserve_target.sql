-- Add an automatic reserve target while preserving every existing non-zero manual setting.
-- Compatible with MySQL 5.7 and safe to run repeatedly.
USE ccps_property_management;
SET NAMES utf8mb4;

SET @has_minimum_balance_mode = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reserve_accounts'
    AND COLUMN_NAME = 'minimum_balance_mode'
);
SET @sql = IF(@has_minimum_balance_mode = 0,
  'ALTER TABLE reserve_accounts ADD COLUMN minimum_balance_mode VARCHAR(20) NOT NULL DEFAULT ''auto'' AFTER minimum_balance',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_calculated_minimum = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reserve_accounts'
    AND COLUMN_NAME = 'calculated_minimum_balance'
);
SET @sql = IF(@has_calculated_minimum = 0,
  'ALTER TABLE reserve_accounts ADD COLUMN calculated_minimum_balance DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER minimum_balance_mode',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_rent_buffer = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reserve_accounts'
    AND COLUMN_NAME = 'rent_buffer_amount'
);
SET @sql = IF(@has_rent_buffer = 0,
  'ALTER TABLE reserve_accounts ADD COLUMN rent_buffer_amount DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER calculated_minimum_balance',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_expense_average = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reserve_accounts'
    AND COLUMN_NAME = 'monthly_expense_average'
);
SET @sql = IF(@has_expense_average = 0,
  'ALTER TABLE reserve_accounts ADD COLUMN monthly_expense_average DECIMAL(18,2) NOT NULL DEFAULT 0 AFTER rent_buffer_amount',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_expense_months = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reserve_accounts'
    AND COLUMN_NAME = 'expense_buffer_months'
);
SET @sql = IF(@has_expense_months = 0,
  'ALTER TABLE reserve_accounts ADD COLUMN expense_buffer_months SMALLINT UNSIGNED NOT NULL DEFAULT 3 AFTER monthly_expense_average',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_calculated_at = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reserve_accounts'
    AND COLUMN_NAME = 'minimum_balance_calculated_at'
);
SET @sql = IF(@has_calculated_at = 0,
  'ALTER TABLE reserve_accounts ADD COLUMN minimum_balance_calculated_at DATETIME NULL AFTER expense_buffer_months',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Values entered before this feature existed are contractual/manual settings.
UPDATE reserve_accounts
SET minimum_balance_mode = CASE WHEN minimum_balance > 0 THEN 'manual' ELSE 'auto' END
WHERE calculated_minimum_balance = 0
  AND minimum_balance_calculated_at IS NULL;

-- Calculate the first recommendation immediately; later changes are refreshed by the scheduler.
DROP TEMPORARY TABLE IF EXISTS tmp_reserve_target_calculations;
CREATE TEMPORARY TABLE tmp_reserve_target_calculations (
  account_id BIGINT UNSIGNED NOT NULL PRIMARY KEY,
  rent_buffer DECIMAL(18,2) NOT NULL,
  expense_average DECIMAL(18,2) NOT NULL,
  calculated_target DECIMAL(18,2) NOT NULL DEFAULT 0
);

INSERT INTO tmp_reserve_target_calculations (account_id, rent_buffer, expense_average)
SELECT ra.id,
       COALESCE(
         (SELECT SUM(l.monthly_rent) FROM leases l
          WHERE l.unit_id = ou.unit_id AND l.status = 'active'
            AND CURRENT_DATE BETWEEN l.start_date AND l.end_date),
         (SELECT l.monthly_rent FROM leases l
          WHERE l.unit_id = ou.unit_id
          ORDER BY l.end_date DESC, l.id DESC LIMIT 1), 0
       ),
       ROUND(COALESCE((
         SELECT SUM(fr.amount)
         FROM cashflow_entries ce
         JOIN finance_records fr ON fr.id = ce.finance_record_id
         WHERE ce.unit_id = ou.unit_id AND ce.owner_id = ou.owner_id
           AND ce.direction = 'expense'
           AND ce.category IN ('management','utilities','maintenance','cleaning','service_fee','insurance','tax','other')
           AND ce.occurred_on >= DATE_SUB(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 11 MONTH)
           AND fr.confirmation_status <> 'rejected'
           AND fr.payment_status <> 'voided'
       ), 0) / GREATEST(1, LEAST(12,
         TIMESTAMPDIFF(MONTH, COALESCE(ou.actual_handover_date, CURRENT_DATE), CURRENT_DATE) + 1
       )), 2)
FROM reserve_accounts ra
JOIN owner_units ou ON ou.id = ra.owner_unit_id
  AND ou.status = 'active' AND ou.asset_stage = 'OPERATING'
WHERE ra.status = 'active';

UPDATE tmp_reserve_target_calculations
SET calculated_target = GREATEST(2000, rent_buffer + expense_average * 3);

UPDATE reserve_accounts ra
JOIN tmp_reserve_target_calculations calc ON calc.account_id = ra.id
SET ra.calculated_minimum_balance = calc.calculated_target,
    ra.rent_buffer_amount = calc.rent_buffer,
    ra.monthly_expense_average = calc.expense_average,
    ra.expense_buffer_months = 3,
    ra.minimum_balance_calculated_at = NOW(),
    ra.minimum_balance = CASE WHEN ra.minimum_balance_mode = 'auto'
                              THEN calc.calculated_target ELSE ra.minimum_balance END;

DROP TEMPORARY TABLE tmp_reserve_target_calculations;
