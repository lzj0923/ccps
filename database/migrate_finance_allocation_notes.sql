-- Give every finance record a general informational finance note.
-- The note never changes balances and never splits a bill.

SET @finance_allocation_note_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'finance_records'
    AND column_name = 'allocation_note'
);
SET @finance_allocation_note_sql := IF(
  @finance_allocation_note_exists = 0,
  'ALTER TABLE finance_records ADD COLUMN allocation_note VARCHAR(500) NULL COMMENT ''Informational finance note; does not split the bill or change balances'' AFTER payment_method',
  'SELECT 1'
);
PREPARE finance_allocation_note_statement FROM @finance_allocation_note_sql;
EXECUTE finance_allocation_note_statement;
DEALLOCATE PREPARE finance_allocation_note_statement;

UPDATE finance_records fr
JOIN cashflow_entries ce ON ce.finance_record_id = fr.id
SET fr.allocation_note = ce.allocation_note
WHERE (fr.allocation_note IS NULL OR TRIM(fr.allocation_note) = '')
  AND ce.allocation_note IS NOT NULL
  AND TRIM(ce.allocation_note) <> '';

CREATE TABLE IF NOT EXISTS finance_allocation_note_defaults (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  unit_id BIGINT UNSIGNED NOT NULL,
  record_type VARCHAR(40) NOT NULL,
  note VARCHAR(500) NOT NULL,
  created_by BIGINT UNSIGNED NULL,
  updated_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_finance_allocation_note_default (unit_id,record_type),
  CONSTRAINT fk_finance_allocation_note_default_unit FOREIGN KEY (unit_id) REFERENCES units(id),
  CONSTRAINT fk_finance_allocation_note_default_creator FOREIGN KEY (created_by) REFERENCES users(id),
  CONSTRAINT fk_finance_allocation_note_default_updater FOREIGN KEY (updated_by) REFERENCES users(id)
) ENGINE=InnoDB;

DROP TRIGGER IF EXISTS trg_finance_allocation_note_before_insert;
DELIMITER $$
CREATE TRIGGER trg_finance_allocation_note_before_insert
BEFORE INSERT ON finance_records
FOR EACH ROW
BEGIN
  IF NEW.unit_id IS NOT NULL AND (NEW.allocation_note IS NULL OR TRIM(NEW.allocation_note) = '') THEN
    SET NEW.allocation_note = (
      SELECT d.note FROM finance_allocation_note_defaults d
      WHERE d.unit_id=NEW.unit_id AND d.record_type=NEW.record_type LIMIT 1
    );
  END IF;
END$$
DELIMITER ;
