-- Backfill system owner numbers and enforce uniqueness. Safe to run repeatedly.
USE ccps_property_management;
SET NAMES utf8mb4;

UPDATE owners
SET owner_no = LPAD(id, 6, '0')
WHERE owner_no IS NULL OR TRIM(owner_no) = '';

SET @has_owner_no_index = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'owners' AND INDEX_NAME = 'uk_owners_owner_no'
);
SET @add_owner_no_index_sql = IF(
  @has_owner_no_index = 0,
  'ALTER TABLE owners ADD UNIQUE KEY uk_owners_owner_no (owner_no)',
  'SELECT 1'
);
PREPARE add_owner_no_index_stmt FROM @add_owner_no_index_sql;
EXECUTE add_owner_no_index_stmt;
DEALLOCATE PREPARE add_owner_no_index_stmt;
