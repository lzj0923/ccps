USE ccps_property_management;

SET @notes_column_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'property_handover_checklist_items'
    AND column_name = 'notes'
);
SET @notes_sql := IF(
  @notes_column_exists = 0,
  'ALTER TABLE property_handover_checklist_items ADD COLUMN notes VARCHAR(500) NULL AFTER default_quantity',
  'SELECT 1'
);
PREPARE notes_statement FROM @notes_sql;
EXECUTE notes_statement;
DEALLOCATE PREPARE notes_statement;
