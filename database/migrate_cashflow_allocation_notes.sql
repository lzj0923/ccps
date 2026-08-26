USE ccps_property_management;
SET NAMES utf8mb4;

SET @allocation_note_column_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'cashflow_entries'
    AND column_name = 'allocation_note'
);
SET @allocation_note_column_sql := IF(
  @allocation_note_column_exists = 0,
  'ALTER TABLE cashflow_entries ADD COLUMN allocation_note VARCHAR(500) NULL COMMENT ''Informational finance note; does not split the bill or change balances'' AFTER description',
  'SELECT 1'
);
PREPARE allocation_note_column_statement FROM @allocation_note_column_sql;
EXECUTE allocation_note_column_statement;
DEALLOCATE PREPARE allocation_note_column_statement;

CREATE TABLE IF NOT EXISTS cashflow_note_defaults (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  unit_id BIGINT UNSIGNED NOT NULL,
  direction VARCHAR(10) NOT NULL,
  category VARCHAR(60) NOT NULL,
  note VARCHAR(500) NOT NULL,
  updated_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cashflow_note_default (unit_id, direction, category),
  CONSTRAINT fk_cashflow_note_default_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT fk_cashflow_note_default_user FOREIGN KEY (updated_by) REFERENCES users (id)
) ENGINE=InnoDB;

DROP TRIGGER IF EXISTS trg_cashflow_allocation_note_before_insert;
DELIMITER $$
CREATE TRIGGER trg_cashflow_allocation_note_before_insert
BEFORE INSERT ON cashflow_entries
FOR EACH ROW
BEGIN
  IF NEW.allocation_note IS NULL OR TRIM(NEW.allocation_note) = '' THEN
    SET NEW.allocation_note = (
      SELECT d.note
        FROM cashflow_note_defaults d
       WHERE d.unit_id = NEW.unit_id
         AND d.direction = NEW.direction
         AND d.category = NEW.category
       LIMIT 1
    );
  END IF;
END$$
DELIMITER ;
