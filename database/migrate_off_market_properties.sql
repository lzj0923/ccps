SET @schema_name = DATABASE();

-- This state belongs to the rental listing, not to the property asset.
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='management_status')=1
  AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='rental_listing_status')=0,
  'ALTER TABLE units CHANGE COLUMN management_status rental_listing_status VARCHAR(20) NOT NULL DEFAULT ''listed'' COMMENT ''listed / off_market''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='rental_listing_status')=0,
  'ALTER TABLE units ADD COLUMN rental_listing_status VARCHAR(20) NOT NULL DEFAULT ''listed'' COMMENT ''listed / off_market'' AFTER listing_status', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
UPDATE units SET rental_listing_status='listed' WHERE rental_listing_status='active';

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='off_market_reason_code')=1
  AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='rental_off_market_reason_code')=0,
  'ALTER TABLE units CHANGE COLUMN off_market_reason_code rental_off_market_reason_code VARCHAR(40) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='rental_off_market_reason_code')=0,
  'ALTER TABLE units ADD COLUMN rental_off_market_reason_code VARCHAR(40) NULL AFTER rental_listing_status', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='off_market_note')=1
  AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='rental_off_market_note')=0,
  'ALTER TABLE units CHANGE COLUMN off_market_note rental_off_market_note VARCHAR(500) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='rental_off_market_note')=0,
  'ALTER TABLE units ADD COLUMN rental_off_market_note VARCHAR(500) NULL AFTER rental_off_market_reason_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='off_market_at')=1
  AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='rental_off_market_at')=0,
  'ALTER TABLE units CHANGE COLUMN off_market_at rental_off_market_at DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='rental_off_market_at')=0,
  'ALTER TABLE units ADD COLUMN rental_off_market_at DATETIME NULL AFTER rental_off_market_note', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='off_market_by')=1
  AND (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='rental_off_market_by')=0,
  'ALTER TABLE units CHANGE COLUMN off_market_by rental_off_market_by BIGINT UNSIGNED NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=@schema_name AND table_name='units' AND column_name='rental_off_market_by')=0,
  'ALTER TABLE units ADD COLUMN rental_off_market_by BIGINT UNSIGNED NULL AFTER rental_off_market_at', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='units' AND index_name='idx_units_management_status')=1,
  'ALTER TABLE units DROP INDEX idx_units_management_status', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='units' AND index_name='idx_units_rental_listing_status')=0,
  'ALTER TABLE units ADD KEY idx_units_rental_listing_status (rental_listing_status, rental_off_market_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=@schema_name AND table_name='unit_management_status_history')=1
  AND (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=@schema_name AND table_name='unit_rental_listing_status_history')=0,
  'RENAME TABLE unit_management_status_history TO unit_rental_listing_status_history', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS unit_rental_listing_status_history (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  unit_id BIGINT UNSIGNED NOT NULL,
  action VARCHAR(20) NOT NULL COMMENT 'off_market / relisted',
  reason_code VARCHAR(40) NULL,
  note VARCHAR(500) NULL,
  changed_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_unit_rental_listing_history (unit_id, created_at),
  CONSTRAINT fk_unit_rental_listing_history_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT fk_unit_rental_listing_history_actor FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB;
