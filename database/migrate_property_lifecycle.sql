-- Add property holding lifecycle and independently selectable operating services.
-- Compatible with MySQL 5.7 and safe to run repeatedly.
USE ccps_property_management;
SET NAMES utf8mb4;

SET @has_asset_stage = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'owner_units' AND COLUMN_NAME = 'asset_stage'
);
SET @sql = IF(@has_asset_stage = 0,
  'ALTER TABLE owner_units ADD COLUMN asset_stage ENUM(''PRE_HANDOVER'', ''OPERATING'', ''DISPOSED'') NOT NULL DEFAULT ''PRE_HANDOVER'' AFTER end_date',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_expected_handover = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'owner_units' AND COLUMN_NAME = 'expected_handover_date'
);
SET @sql = IF(@has_expected_handover = 0,
  'ALTER TABLE owner_units ADD COLUMN expected_handover_date DATE NULL AFTER asset_stage',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_actual_handover = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'owner_units' AND COLUMN_NAME = 'actual_handover_date'
);
SET @sql = IF(@has_actual_handover = 0,
  'ALTER TABLE owner_units ADD COLUMN actual_handover_date DATE NULL AFTER expected_handover_date',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_stage_index = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'owner_units' AND INDEX_NAME = 'idx_owner_units_stage'
);
SET @sql = IF(@has_stage_index = 0,
  'ALTER TABLE owner_units ADD INDEX idx_owner_units_stage (asset_stage, status)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS owner_unit_services (
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  service_type ENUM('RENTAL', 'RESALE', 'MANAGEMENT') NOT NULL,
  status ENUM('active', 'paused', 'ended') NOT NULL DEFAULT 'active',
  started_at DATE NULL,
  ended_at DATE NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (owner_unit_id, service_type),
  KEY idx_owner_unit_services_status (service_type, status),
  CONSTRAINT fk_owner_unit_services_holding FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id)
) ENGINE=InnoDB;

START TRANSACTION;

-- Operational evidence takes precedence when classifying legacy rows.
UPDATE owner_units ou
SET ou.asset_stage = 'OPERATING',
    ou.actual_handover_date = COALESCE(ou.actual_handover_date, ou.start_date, CURRENT_DATE)
WHERE ou.status = 'active'
  AND (
    EXISTS (SELECT 1 FROM reserve_accounts ra WHERE ra.owner_unit_id = ou.id AND ra.status = 'active')
    OR EXISTS (SELECT 1 FROM leases l WHERE l.unit_id = ou.unit_id AND l.status = 'active')
    OR EXISTS (SELECT 1 FROM maintenance_work_orders mwo WHERE mwo.unit_id = ou.unit_id)
    OR EXISTS (SELECT 1 FROM cashflow_entries ce WHERE ce.unit_id = ou.unit_id)
  );

UPDATE owner_units ou
LEFT JOIN purchase_contracts pc ON pc.owner_unit_id = ou.id AND pc.status <> 'cancelled'
SET ou.expected_handover_date = COALESCE(ou.expected_handover_date, pc.handover_date)
WHERE ou.asset_stage = 'PRE_HANDOVER';

INSERT INTO owner_unit_services (owner_unit_id, service_type, status, started_at)
SELECT ou.id, 'MANAGEMENT', 'active', COALESCE(ou.actual_handover_date, ou.start_date)
FROM owner_units ou
WHERE ou.asset_stage = 'OPERATING'
  AND (EXISTS (SELECT 1 FROM reserve_accounts ra WHERE ra.owner_unit_id = ou.id)
       OR EXISTS (SELECT 1 FROM maintenance_work_orders mwo WHERE mwo.unit_id = ou.unit_id))
ON DUPLICATE KEY UPDATE status = 'active', ended_at = NULL;

INSERT INTO owner_unit_services (owner_unit_id, service_type, status, started_at)
SELECT ou.id, 'RENTAL', 'active', COALESCE(ou.actual_handover_date, ou.start_date)
FROM owner_units ou
WHERE ou.asset_stage = 'OPERATING'
  AND EXISTS (SELECT 1 FROM leases l WHERE l.unit_id = ou.unit_id AND l.status = 'active')
ON DUPLICATE KEY UPDATE status = 'active', ended_at = NULL;

INSERT INTO reserve_accounts (owner_unit_id, minimum_balance, current_balance, status, low_balance_alert_enabled)
SELECT ou.id, 0, 0, 'active', 1
FROM owner_units ou
WHERE ou.asset_stage = 'OPERATING'
  AND NOT EXISTS (SELECT 1 FROM reserve_accounts ra WHERE ra.owner_unit_id = ou.id);

UPDATE payment_plans pp
JOIN purchase_contracts pc ON pc.id = pp.purchase_contract_id
JOIN owner_units ou ON ou.id = pc.owner_unit_id
SET pp.status = 'historical'
WHERE ou.asset_stage = 'OPERATING' AND pp.status = 'active';

UPDATE purchase_contracts pc
JOIN owner_units ou ON ou.id = pc.owner_unit_id
SET pc.status = 'completed',
    pc.handover_date = COALESCE(pc.handover_date, ou.actual_handover_date)
WHERE ou.asset_stage = 'OPERATING' AND pc.status = 'active';

COMMIT;

-- Keep the database read models aligned with the lifecycle boundary.
CREATE OR REPLACE VIEW v_unit_payment_progress AS
SELECT
  ou.owner_id,
  ou.unit_id,
  u.unit_no,
  p.name AS project_name,
  pc.purchase_price,
  COALESCE(SUM(pi.amount_due), 0) AS scheduled_amount,
  COALESCE(SUM(pi.amount_paid), 0) AS paid_amount,
  GREATEST(COALESCE(SUM(pi.amount_due), 0) - COALESCE(SUM(pi.amount_paid), 0), 0) AS unpaid_amount,
  MAX(pi.due_date) AS last_due_date
FROM owner_units ou
JOIN units u ON u.id = ou.unit_id
JOIN projects p ON p.id = u.project_id
LEFT JOIN purchase_contracts pc ON pc.owner_unit_id = ou.id AND pc.status = 'active'
LEFT JOIN payment_plans pp ON pp.purchase_contract_id = pc.id AND pp.status = 'active'
LEFT JOIN payment_installments pi ON pi.payment_plan_id = pp.id
WHERE ou.status = 'active'
  AND ou.asset_stage = 'PRE_HANDOVER'
GROUP BY ou.owner_id, ou.unit_id, u.unit_no, p.name, pc.purchase_price;

CREATE OR REPLACE VIEW v_rent_collection_status AS
SELECT
  ri.id AS rent_invoice_id,
  l.id AS lease_id,
  l.unit_id,
  l.tenant_id,
  ri.billing_month,
  ri.due_date,
  ri.amount_due,
  ri.amount_paid,
  GREATEST(ri.amount_due - ri.amount_paid, 0) AS unpaid_amount,
  CASE
    WHEN ri.amount_paid >= ri.amount_due THEN 'paid'
    WHEN ri.amount_paid > 0 THEN 'partial'
    WHEN CURRENT_DATE > GREATEST(ri.due_date, DATE_ADD(l.start_date, INTERVAL 7 DAY)) THEN 'overdue'
    ELSE 'unpaid'
  END AS calculated_status
FROM rent_invoices ri
JOIN leases l ON l.id = ri.lease_id
WHERE EXISTS (
  SELECT 1
  FROM owner_units ou
  JOIN owner_unit_services ous ON ous.owner_unit_id = ou.id
  WHERE ou.unit_id = l.unit_id
    AND ou.status = 'active'
    AND ou.asset_stage = 'OPERATING'
    AND ous.service_type = 'RENTAL'
    AND ous.status = 'active'
);

CREATE OR REPLACE VIEW v_reserve_balances AS
SELECT
  ra.id AS reserve_account_id,
  ou.owner_id,
  ou.unit_id,
  ra.minimum_balance,
  ra.current_balance,
  GREATEST(ra.minimum_balance - ra.current_balance, 0) AS shortage_amount,
  CASE WHEN ra.current_balance < ra.minimum_balance THEN 'low' ELSE 'normal' END AS calculated_status
FROM reserve_accounts ra
JOIN owner_units ou ON ou.id = ra.owner_unit_id
WHERE ra.status = 'active'
  AND ou.status = 'active'
  AND ou.asset_stage = 'OPERATING';
