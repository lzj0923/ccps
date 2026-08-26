-- 合租支持：房产下增加可独立出租的空间，租约改为绑定出租空间。
-- 可重复执行；现有租约统一迁移到每套房产的“整套房产”空间。

SET @schema_name = DATABASE();

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA=@schema_name AND TABLE_NAME='units' AND COLUMN_NAME='rental_mode'),
  'SELECT 1',
  "ALTER TABLE units ADD COLUMN rental_mode VARCHAR(20) NOT NULL DEFAULT 'whole_unit' AFTER bedroom_count"
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS rental_spaces (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  unit_id BIGINT UNSIGNED NOT NULL,
  space_code VARCHAR(40) NOT NULL,
  space_name VARCHAR(100) NOT NULL,
  space_type VARCHAR(20) NOT NULL COMMENT 'whole_unit / room',
  capacity SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  area_sqm DECIMAL(12,2) NULL,
  recommended_rent DECIMAL(18,2) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rental_spaces_unit_code (unit_id, space_code),
  KEY idx_rental_spaces_unit_status (unit_id, status, space_type),
  CONSTRAINT fk_rental_spaces_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT chk_rental_spaces_type CHECK (space_type IN ('whole_unit','room'))
) ENGINE=InnoDB;

INSERT INTO rental_spaces (unit_id, space_code, space_name, space_type, capacity, status, sort_order)
SELECT u.id, 'WHOLE', '整套房产', 'whole_unit', 1, 'active', 0
FROM units u
WHERE NOT EXISTS (
  SELECT 1 FROM rental_spaces rs WHERE rs.unit_id=u.id AND rs.space_type='whole_unit'
);

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA=@schema_name AND TABLE_NAME='leases' AND COLUMN_NAME='rental_space_id'),
  'SELECT 1',
  'ALTER TABLE leases ADD COLUMN rental_space_id BIGINT UNSIGNED NULL AFTER unit_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE leases l
JOIN rental_spaces rs ON rs.unit_id=l.unit_id AND rs.space_type='whole_unit'
SET l.rental_space_id=rs.id
WHERE l.rental_space_id IS NULL;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA=@schema_name AND TABLE_NAME='leases' AND INDEX_NAME='idx_leases_space_status'),
  'SELECT 1',
  'ALTER TABLE leases ADD KEY idx_leases_space_status (rental_space_id, status, start_date, end_date)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.REFERENTIAL_CONSTRAINTS
         WHERE CONSTRAINT_SCHEMA=@schema_name AND TABLE_NAME='leases' AND CONSTRAINT_NAME='fk_leases_rental_space'),
  'SELECT 1',
  'ALTER TABLE leases ADD CONSTRAINT fk_leases_rental_space FOREIGN KEY (rental_space_id) REFERENCES rental_spaces (id)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA=@schema_name AND TABLE_NAME='leases' AND COLUMN_NAME='rental_space_id' AND IS_NULLABLE='NO'),
  'SELECT 1',
  'ALTER TABLE leases MODIFY COLUMN rental_space_id BIGINT UNSIGNED NOT NULL'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
