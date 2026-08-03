-- Keep one current draft/source and one current signed version per contract.
-- Historical rows are retained for audit; older files are marked voided/superseded.

-- Keep only the newest signing request that is still pending for each contract.
CREATE TEMPORARY TABLE ccps_latest_pending_signature AS
SELECT entity_type, entity_id, MAX(id) AS keep_id
FROM electronic_signature_requests
WHERE status = 'pending'
GROUP BY entity_type, entity_id;

UPDATE electronic_signature_requests r
LEFT JOIN ccps_latest_pending_signature k
  ON k.entity_type = r.entity_type AND k.entity_id = r.entity_id
SET r.status = 'cancelled', r.updated_at = CURRENT_TIMESTAMP
WHERE r.status = 'pending' AND (k.keep_id IS NULL OR r.id <> k.keep_id);

DROP TEMPORARY TABLE ccps_latest_pending_signature;

-- Keep the newest signed PDF for each contract and retain older signed PDFs as history.
CREATE TEMPORARY TABLE ccps_latest_signed_signature AS
SELECT entity_type, entity_id, MAX(id) AS keep_id
FROM electronic_signature_requests
WHERE status = 'signed' AND signed_document_id IS NOT NULL
GROUP BY entity_type, entity_id;

UPDATE documents d
JOIN electronic_signature_requests r ON r.signed_document_id = d.id
JOIN ccps_latest_signed_signature k
  ON k.entity_type = r.entity_type AND k.entity_id = r.entity_id
SET d.status = 'voided', d.updated_at = CURRENT_TIMESTAMP
WHERE r.status = 'signed' AND r.id <> k.keep_id
  AND d.status NOT IN ('voided', 'superseded');

DROP TEMPORARY TABLE ccps_latest_signed_signature;
