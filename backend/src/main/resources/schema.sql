CREATE TABLE IF NOT EXISTS property_expense_postings (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  charge_key VARCHAR(120) NOT NULL,
  charge_name VARCHAR(180) NOT NULL,
  period_key VARCHAR(20) NOT NULL,
  finance_record_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_property_expense_period (owner_unit_id, charge_key, period_key),
  KEY idx_property_expense_finance (finance_record_id),
  CONSTRAINT fk_property_expense_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_property_expense_finance FOREIGN KEY (finance_record_id) REFERENCES finance_records (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS electronic_signature_requests (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_document_id BIGINT UNSIGNED NOT NULL,
  entity_type VARCHAR(40) NOT NULL,
  entity_id BIGINT UNSIGNED NOT NULL,
  signer_name VARCHAR(190) NOT NULL,
  signer_email VARCHAR(190) NOT NULL,
  access_token_hash CHAR(64) NOT NULL,
  verification_code_hash VARCHAR(100) NOT NULL,
  verification_expires_at DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  expires_at DATETIME NOT NULL,
  requested_by BIGINT UNSIGNED NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  signed_at DATETIME NULL,
  signed_document_id BIGINT UNSIGNED NULL,
  source_checksum_sha256 CHAR(64) NULL,
  signature_hash CHAR(64) NULL,
  signer_ip VARCHAR(64) NULL,
  signer_user_agent VARCHAR(500) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_e_signature_token (access_token_hash),
  KEY idx_e_signature_entity (entity_type, entity_id, status),
  KEY idx_e_signature_expiry (status, expires_at),
  CONSTRAINT fk_e_signature_source_document FOREIGN KEY (source_document_id) REFERENCES documents (id),
  CONSTRAINT fk_e_signature_signed_document FOREIGN KEY (signed_document_id) REFERENCES documents (id),
  CONSTRAINT fk_e_signature_requester FOREIGN KEY (requested_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS electronic_signature_events (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  signature_request_id BIGINT UNSIGNED NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  detail VARCHAR(500) NULL,
  remote_ip VARCHAR(64) NULL,
  user_agent VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_e_signature_events_request (signature_request_id, created_at),
  CONSTRAINT fk_e_signature_events_request FOREIGN KEY (signature_request_id) REFERENCES electronic_signature_requests (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS property_handover_checklist_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  category VARCHAR(120) NOT NULL,
  item_name VARCHAR(255) NOT NULL,
  default_quantity VARCHAR(80) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_property_handover_checklist_owner (owner_unit_id, enabled, category, sort_order),
  CONSTRAINT fk_property_handover_checklist_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rent_invoice_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  invoice_id BIGINT UNSIGNED NOT NULL,
  charge_type VARCHAR(40) NOT NULL COMMENT 'management / utilities / maintenance / other',
  description VARCHAR(255) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  payer VARCHAR(20) NOT NULL DEFAULT 'tenant' COMMENT 'tenant / owner / agency',
  source_type VARCHAR(40) NULL COMMENT 'manual / work_order / utility',
  source_id BIGINT UNSIGNED NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_invoice_items_invoice (invoice_id),
  KEY idx_invoice_items_source (source_type, source_id),
  CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES rent_invoices (id),
  CONSTRAINT fk_invoice_items_creator FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_invoice_items_amount CHECK (amount > 0),
  CONSTRAINT chk_invoice_items_payer CHECK (payer IN ('tenant', 'owner', 'agency'))
) ENGINE=InnoDB;

-- A rent credit is cash that has already been received but has not yet been
-- applied to a monthly rent invoice.  It is deliberately separate from a
-- security deposit and from the invoice amount itself.
CREATE TABLE IF NOT EXISTS lease_rent_credits (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  lease_id BIGINT UNSIGNED NOT NULL,
  finance_record_id BIGINT UNSIGNED NOT NULL,
  received_amount DECIMAL(18,2) NOT NULL,
  allocated_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  remaining_amount DECIMAL(18,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'available',
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_lease_rent_credit_finance (finance_record_id),
  KEY idx_lease_rent_credit_available (lease_id, status, created_at),
  CONSTRAINT fk_lease_rent_credit_lease FOREIGN KEY (lease_id) REFERENCES leases (id),
  CONSTRAINT fk_lease_rent_credit_finance FOREIGN KEY (finance_record_id) REFERENCES finance_records (id),
  CONSTRAINT fk_lease_rent_credit_creator FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_lease_rent_credit_amount CHECK (received_amount > 0 AND allocated_amount >= 0 AND remaining_amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS lease_rent_credit_allocations (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rent_credit_id BIGINT UNSIGNED NOT NULL,
  rent_invoice_id BIGINT UNSIGNED NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  allocation_type VARCHAR(30) NOT NULL DEFAULT 'automatic',
  allocated_by BIGINT UNSIGNED NULL,
  allocated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_lease_credit_invoice (rent_credit_id, rent_invoice_id),
  KEY idx_lease_credit_allocation_invoice (rent_invoice_id),
  CONSTRAINT fk_lease_credit_allocation_credit FOREIGN KEY (rent_credit_id) REFERENCES lease_rent_credits (id),
  CONSTRAINT fk_lease_credit_allocation_invoice FOREIGN KEY (rent_invoice_id) REFERENCES rent_invoices (id),
  CONSTRAINT fk_lease_credit_allocation_actor FOREIGN KEY (allocated_by) REFERENCES users (id),
  CONSTRAINT chk_lease_credit_allocation_amount CHECK (amount > 0)
) ENGINE=InnoDB;
