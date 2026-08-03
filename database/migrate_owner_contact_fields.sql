ALTER TABLE owners
  ADD COLUMN owner_no VARCHAR(30) NULL AFTER user_id,
  ADD COLUMN mobile_phone VARCHAR(40) NULL AFTER phone,
  ADD COLUMN home_phone VARCHAR(40) NULL AFTER mobile_phone,
  ADD COLUMN office_phone VARCHAR(40) NULL AFTER home_phone,
  ADD COLUMN passport_no VARCHAR(80) NULL AFTER office_phone;

UPDATE owners
SET mobile_phone = COALESCE(mobile_phone, phone)
WHERE mobile_phone IS NULL AND phone IS NOT NULL;
