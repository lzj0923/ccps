USE ccps_property_management;

ALTER TABLE owners
  ADD COLUMN mailing_address VARCHAR(500) NULL AFTER email;
