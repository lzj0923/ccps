SET @has_lease_id=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='property_photos' AND COLUMN_NAME='lease_id');
SET @sql=IF(@has_lease_id=0,'ALTER TABLE property_photos ADD COLUMN lease_id BIGINT UNSIGNED NULL AFTER owner_unit_id','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Rental photos are lease evidence and must never become the property cover.
UPDATE property_photos SET is_cover=0 WHERE lease_id IS NOT NULL;

-- Restore one ordinary cover only where ordinary property photos exist without one.
UPDATE property_photos candidate
JOIN (
  SELECT pp.owner_unit_id, MIN(pp.id) AS first_photo_id
  FROM property_photos pp
  WHERE pp.lease_id IS NULL
    AND NOT EXISTS (
      SELECT 1 FROM property_photos cover_photo
      WHERE cover_photo.owner_unit_id=pp.owner_unit_id
        AND cover_photo.lease_id IS NULL AND cover_photo.is_cover=1
    )
  GROUP BY pp.owner_unit_id
) missing_cover ON missing_cover.first_photo_id=candidate.id
SET candidate.is_cover=1;

SET @has_rental_stage=(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='property_photos' AND COLUMN_NAME='rental_stage');
SET @sql=IF(@has_rental_stage=0,'ALTER TABLE property_photos ADD COLUMN rental_stage VARCHAR(20) NULL COMMENT ''before / after'' AFTER lease_id','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx=(SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='property_photos' AND INDEX_NAME='idx_property_photos_lease_stage');
SET @sql=IF(@has_idx=0,'ALTER TABLE property_photos ADD KEY idx_property_photos_lease_stage (lease_id,rental_stage)','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_fk=(SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='property_photos' AND CONSTRAINT_NAME='fk_property_photos_lease');
SET @sql=IF(@has_fk=0,'ALTER TABLE property_photos ADD CONSTRAINT fk_property_photos_lease FOREIGN KEY (lease_id) REFERENCES leases(id)','SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
