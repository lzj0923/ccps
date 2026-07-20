USE ccps_property_management;
SET NAMES utf8mb4;

-- Older databases do not have the submission note used by payment and reserve proofs.
SET @has_submission_note = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'payment_receipts'
    AND COLUMN_NAME = 'submission_note'
);
SET @submission_note_sql = IF(
  @has_submission_note = 0,
  'ALTER TABLE payment_receipts ADD COLUMN submission_note VARCHAR(500) NULL AFTER proof_document_id',
  'SELECT 1'
);
PREPARE submission_note_statement FROM @submission_note_sql;
EXECUTE submission_note_statement;
DEALLOCATE PREPARE submission_note_statement;
