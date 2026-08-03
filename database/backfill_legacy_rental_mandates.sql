-- Backfill legacy active leases that predate rental mandate enforcement.
-- The lease dates are used as the minimum defensible mandate window.
-- Attachments are optional: an active mandate covering the lease term is sufficient.
START TRANSACTION;

-- Some legacy leases have finance ownership data but no owner_units row. Restore only
-- the minimum active holding needed by the mandate, using the latest recorded owner.
INSERT INTO owner_units (
  owner_id, unit_id, ownership_percent, is_primary, start_date,
  asset_stage, actual_handover_date, status
)
SELECT (
         SELECT fr.owner_id
         FROM finance_records fr
         WHERE fr.unit_id = l.unit_id AND fr.owner_id IS NOT NULL
         ORDER BY fr.transaction_date DESC, fr.id DESC
         LIMIT 1
       ),
       l.unit_id,
       100,
       1,
       l.start_date,
       'OPERATING',
       l.start_date,
       'active'
FROM leases l
WHERE l.status = 'active'
  AND NOT EXISTS (
    SELECT 1 FROM owner_units ou
    WHERE ou.unit_id = l.unit_id AND ou.status = 'active'
  )
  AND EXISTS (
    SELECT 1 FROM finance_records fr
    WHERE fr.unit_id = l.unit_id AND fr.owner_id IS NOT NULL
  );

INSERT INTO owner_unit_services (owner_unit_id, service_type, status, started_at)
SELECT ou.id, 'RENTAL', 'active', l.start_date
FROM leases l
JOIN owner_units ou ON ou.unit_id = l.unit_id AND ou.status = 'active'
WHERE l.status = 'active'
ON DUPLICATE KEY UPDATE status = 'active', ended_at = NULL;

INSERT INTO rental_mandates (
  owner_unit_id, mandate_no, mandate_type, start_date, end_date,
  management_fee, commission_percent, status, submitted_at,
  reviewed_by, reviewed_at, review_note, created_by
)
SELECT ou.id,
       CONCAT('LEGACY-', l.lease_no),
       'legacy',
       l.start_date,
       l.end_date,
       0,
       0,
       'active',
       CURRENT_TIMESTAMP,
       (SELECT id FROM users WHERE account_type = 'ADMIN' ORDER BY id LIMIT 1),
       CURRENT_TIMESTAMP,
       '由既有有效租約回填出租委託',
       (SELECT id FROM users WHERE account_type = 'ADMIN' ORDER BY id LIMIT 1)
FROM leases l
JOIN owner_units ou ON ou.unit_id = l.unit_id AND ou.status = 'active'
WHERE l.status = 'active'
  AND NOT EXISTS (
    SELECT 1 FROM rental_mandates rm
    WHERE rm.owner_unit_id = ou.id
      AND rm.status = 'active'
      AND rm.start_date <= l.start_date
      AND (rm.end_date IS NULL OR rm.end_date >= l.end_date)
  )
  AND NOT EXISTS (
    SELECT 1 FROM rental_mandates existing WHERE existing.mandate_no = CONCAT('LEGACY-', l.lease_no)
  );

INSERT INTO rental_mandate_status_history (mandate_id, from_status, to_status, reason, changed_by)
SELECT rm.id, NULL, 'active', '由既有有效租約回填', rm.created_by
FROM rental_mandates rm
WHERE rm.mandate_type = 'legacy'
  AND NOT EXISTS (
    SELECT 1 FROM rental_mandate_status_history h WHERE h.mandate_id = rm.id AND h.to_status = 'active'
  );

COMMIT;
