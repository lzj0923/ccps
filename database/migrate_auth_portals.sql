-- Split CCPS login accounts into two mutually exclusive portal types.
-- Compatible with MySQL 5.7 and safe to run repeatedly.
USE ccps_property_management;
SET NAMES utf8mb4;

-- MySQL 5.7 does not support ADD COLUMN IF NOT EXISTS, so use information_schema.
SET @has_account_type = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'account_type'
);
SET @add_account_type_sql = IF(
  @has_account_type = 0,
  'ALTER TABLE users ADD COLUMN account_type ENUM(''ADMIN'', ''OWNER'') NOT NULL DEFAULT ''OWNER'' COMMENT ''Login portal type; admin and owner are mutually exclusive'' AFTER phone',
  'SELECT 1'
);
PREPARE add_account_type_stmt FROM @add_account_type_sql;
EXECUTE add_account_type_stmt;
DEALLOCATE PREPARE add_account_type_stmt;

SET @has_account_type_index = (
  SELECT COUNT(*)
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users'
    AND INDEX_NAME = 'idx_users_account_type_status'
);
SET @add_account_type_index_sql = IF(
  @has_account_type_index = 0,
  'ALTER TABLE users ADD INDEX idx_users_account_type_status (account_type, status)',
  'SELECT 1'
);
PREPARE add_account_type_index_stmt FROM @add_account_type_index_sql;
EXECUTE add_account_type_index_stmt;
DEALLOCATE PREPARE add_account_type_index_stmt;

START TRANSACTION;

INSERT INTO roles (code, name)
SELECT 'ADMIN', 'System Administrator'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ADMIN');
INSERT INTO roles (code, name)
SELECT 'OWNER', 'Property Owner'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'OWNER');

-- Existing ADMIN role assignments take precedence during migration.
UPDATE users u
SET u.account_type = CASE
  WHEN EXISTS (
    SELECT 1
    FROM user_roles ur
    JOIN roles r ON r.id = ur.role_id
    WHERE ur.user_id = u.id AND UPPER(r.code) = 'ADMIN'
  ) THEN 'ADMIN'
  ELSE 'OWNER'
END;

-- A portal account gets exactly one portal role matching account_type.
DELETE ur
FROM user_roles ur
JOIN users u ON u.id = ur.user_id
JOIN roles r ON r.id = ur.role_id
WHERE (u.account_type = 'ADMIN' AND UPPER(r.code) = 'OWNER')
   OR (u.account_type = 'OWNER' AND UPPER(r.code) = 'ADMIN');

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON UPPER(r.code) = u.account_type;

-- Admin identities cannot be used as owner portal identities.
UPDATE owners o
JOIN users u ON u.id = o.user_id
SET o.user_id = NULL
WHERE u.account_type = 'ADMIN';

COMMIT;
