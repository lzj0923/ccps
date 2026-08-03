USE ccps_property_management;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS property_basic_profiles (
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  profile_json JSON NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (owner_unit_id),
  CONSTRAINT fk_property_basic_profiles_owner_unit
    FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id)
) ENGINE=InnoDB;
