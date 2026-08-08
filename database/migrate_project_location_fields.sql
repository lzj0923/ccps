-- Add a structured state field while retaining city/district and detailed address.
-- Compatible with MySQL 5.7 and safe to run repeatedly.
USE ccps_property_management;
SET NAMES utf8mb4;

SET @has_project_state = (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'projects' AND COLUMN_NAME = 'state_name'
);
SET @add_project_state_sql = IF(
  @has_project_state = 0,
  'ALTER TABLE projects ADD COLUMN state_name VARCHAR(100) NULL AFTER address',
  'SELECT 1'
);
PREPARE add_project_state_stmt FROM @add_project_state_sql;
EXECUTE add_project_state_stmt;
DEALLOCATE PREPARE add_project_state_stmt;
