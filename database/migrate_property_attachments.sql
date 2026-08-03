USE ccps_property_management;

CREATE TABLE IF NOT EXISTS property_attachments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  document_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(160) NOT NULL,
  remarks VARCHAR(1000) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_property_attachments_document (document_id),
  KEY idx_property_attachments_owner_unit (owner_unit_id, enabled, created_at),
  CONSTRAINT fk_property_attachments_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_property_attachments_document FOREIGN KEY (document_id) REFERENCES documents (id),
  CONSTRAINT fk_property_attachments_creator FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB;
