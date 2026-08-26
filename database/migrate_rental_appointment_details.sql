SET @rental_appointment_details_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'rental_mandates'
    AND column_name = 'rental_appointment_details'
);

SET @rental_appointment_details_sql := IF(
  @rental_appointment_details_exists = 0,
  'ALTER TABLE rental_mandates ADD COLUMN rental_appointment_details JSON NULL AFTER termination_reason',
  'SELECT 1'
);

PREPARE rental_appointment_details_statement FROM @rental_appointment_details_sql;
EXECUTE rental_appointment_details_statement;
DEALLOCATE PREPARE rental_appointment_details_statement;
