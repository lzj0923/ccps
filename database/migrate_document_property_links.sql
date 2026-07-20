-- Backfill the property link for the seeded purchase contract so the owner
-- document list can show its exact unit instead of an unscoped fallback.
USE ccps_property_management;
SET NAMES utf8mb4;

INSERT INTO document_links (document_id, entity_type, entity_id, relation_type)
SELECT d.id, 'unit', u.id, 'property'
FROM documents d
JOIN units u ON u.unit_no = 'ADMIN-A-0301'
JOIN projects p ON p.id = u.project_id AND p.project_code = 'ADMIN-TEST-20260715-P01'
WHERE d.document_no = 'ADMIN-TEST-20260715-D01'
  AND NOT EXISTS (
    SELECT 1 FROM document_links dl
    WHERE dl.document_id = d.id
      AND dl.entity_type = 'unit'
      AND dl.entity_id = u.id
      AND dl.relation_type = 'property'
  );
