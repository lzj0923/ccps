CREATE TABLE IF NOT EXISTS property_contract_records (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  contract_type VARCHAR(40) NOT NULL,
  contract_no VARCHAR(80) NOT NULL,
  signed_date DATE NULL,
  valid_from DATE NULL,
  valid_to DATE NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  notes VARCHAR(1000) NULL,
  original_name VARCHAR(255) NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  mime_type VARCHAR(120) NOT NULL,
  file_size BIGINT UNSIGNED NOT NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_property_contract_records_no (owner_unit_id, contract_no),
  KEY idx_property_contract_records_type (owner_unit_id, contract_type, status),
  CONSTRAINT fk_property_contract_records_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_property_contract_records_creator FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_property_contract_records_dates CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to >= valid_from),
  CONSTRAINT chk_property_contract_records_status CHECK (status IN ('draft', 'active', 'completed', 'cancelled'))
) ENGINE=InnoDB;

ALTER TABLE property_contract_records
  ADD COLUMN lease_id BIGINT UNSIGNED NULL AFTER owner_unit_id,
  ADD UNIQUE KEY uk_property_contract_records_lease (lease_id),
  ADD CONSTRAINT fk_property_contract_records_lease FOREIGN KEY (lease_id) REFERENCES leases (id);
