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
  root_document_id BIGINT UNSIGNED NULL,
  entity_type VARCHAR(40) NOT NULL,
  entity_id BIGINT UNSIGNED NOT NULL,
  document_kind VARCHAR(64) NULL,
  signer_role VARCHAR(32) NOT NULL DEFAULT 'signer',
  signing_order INT NOT NULL DEFAULT 1,
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
  KEY idx_e_signature_package (root_document_id, signing_order, status),
  KEY idx_e_signature_expiry (status, expires_at),
  CONSTRAINT fk_e_signature_source_document FOREIGN KEY (source_document_id) REFERENCES documents (id),
  CONSTRAINT fk_e_signature_root_document FOREIGN KEY (root_document_id) REFERENCES documents (id),
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

CREATE TABLE IF NOT EXISTS security_deposit_entries (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  lease_id BIGINT UNSIGNED NOT NULL,
  finance_record_id BIGINT UNSIGNED NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_security_deposit_lease (lease_id),
  UNIQUE KEY uk_security_deposit_finance (finance_record_id),
  KEY idx_security_deposit_status (status, created_at),
  CONSTRAINT fk_security_deposit_lease FOREIGN KEY (lease_id) REFERENCES leases (id),
  CONSTRAINT fk_security_deposit_finance FOREIGN KEY (finance_record_id) REFERENCES finance_records (id),
  CONSTRAINT chk_security_deposit_amount CHECK (amount > 0)
) ENGINE=InnoDB;

-- Tenant security-deposit ledger. Day-to-day advances and repayments stay in
-- this ledger and are intentionally not posted to the owner's account. Only
-- move-out settlement records create an owner finance entry.
CREATE TABLE IF NOT EXISTS tenant_deposit_transactions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  lease_id BIGINT UNSIGNED NOT NULL,
  tenant_id BIGINT UNSIGNED NOT NULL,
  unit_id BIGINT UNSIGNED NOT NULL,
  finance_record_id BIGINT UNSIGNED NULL,
  transaction_type VARCHAR(40) NOT NULL COMMENT 'collection / tenant_advance / tenant_repayment / rent_deduction / refund / forfeiture / adjustment_credit / adjustment_debit',
  direction VARCHAR(10) NOT NULL COMMENT 'credit / debit',
  amount DECIMAL(18,2) NOT NULL,
  occurred_on DATE NOT NULL,
  description VARCHAR(500) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'posted',
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_tenant_deposit_finance_type (finance_record_id, transaction_type),
  KEY idx_tenant_deposit_tenant_date (tenant_id, occurred_on, id),
  KEY idx_tenant_deposit_lease_date (lease_id, occurred_on, id),
  CONSTRAINT fk_tenant_deposit_lease FOREIGN KEY (lease_id) REFERENCES leases (id),
  CONSTRAINT fk_tenant_deposit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
  CONSTRAINT fk_tenant_deposit_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT fk_tenant_deposit_finance FOREIGN KEY (finance_record_id) REFERENCES finance_records (id),
  CONSTRAINT fk_tenant_deposit_creator FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_tenant_deposit_amount CHECK (amount > 0),
  CONSTRAINT chk_tenant_deposit_direction CHECK (direction IN ('credit','debit')),
  CONSTRAINT chk_tenant_deposit_status CHECK (status IN ('pending','posted','cancelled'))
) ENGINE=InnoDB;

INSERT INTO tenant_deposit_transactions
  (lease_id,tenant_id,unit_id,finance_record_id,transaction_type,direction,amount,occurred_on,description,status,created_by)
SELECT sde.lease_id,l.tenant_id,l.unit_id,sde.finance_record_id,'collection','credit',sde.amount,
       fr.transaction_date,CONCAT('租客押金 · ',l.lease_no),'posted',fr.confirmed_by
FROM security_deposit_entries sde
JOIN leases l ON l.id=sde.lease_id
JOIN finance_records fr ON fr.id=sde.finance_record_id
WHERE sde.status='confirmed' AND fr.confirmation_status='confirmed'
  AND NOT EXISTS (SELECT 1 FROM tenant_deposit_transactions tdt
                  WHERE tdt.finance_record_id=sde.finance_record_id AND tdt.transaction_type='collection');

-- A lease keeps one identity across consecutive renewals. Each period preserves
-- the dates, rent terms and contract that were effective for that renewal.
CREATE TABLE IF NOT EXISTS lease_periods (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  lease_id BIGINT UNSIGNED NOT NULL,
  period_no INT UNSIGNED NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  monthly_rent DECIMAL(18,2) NOT NULL,
  deposit_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  payment_day TINYINT UNSIGNED NOT NULL DEFAULT 1,
  rent_calculation_method VARCHAR(30) NOT NULL DEFAULT 'daily_prorated',
  contract_document_id BIGINT UNSIGNED NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_lease_period_no (lease_id, period_no),
  KEY idx_lease_period_dates (lease_id, start_date, end_date),
  CONSTRAINT fk_lease_period_lease FOREIGN KEY (lease_id) REFERENCES leases (id),
  CONSTRAINT fk_lease_period_document FOREIGN KEY (contract_document_id) REFERENCES documents (id),
  CONSTRAINT fk_lease_period_creator FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_lease_period_dates CHECK (end_date >= start_date),
  CONSTRAINT chk_lease_period_payment_day CHECK (payment_day BETWEEN 1 AND 31)
) ENGINE=InnoDB;

INSERT INTO lease_periods
  (lease_id,period_no,start_date,end_date,monthly_rent,deposit_amount,payment_day,
   rent_calculation_method,contract_document_id)
SELECT l.id,1,l.start_date,l.end_date,l.monthly_rent,l.deposit_amount,l.payment_day,
       l.rent_calculation_method,l.contract_document_id
FROM leases l
WHERE NOT EXISTS (SELECT 1 FROM lease_periods lp WHERE lp.lease_id=l.id);

CREATE TABLE IF NOT EXISTS reserve_reconciliations (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reconciliation_month DATE NOT NULL,
  system_balance DECIMAL(18,2) NOT NULL,
  finance_balance DECIMAL(18,2) NOT NULL,
  difference_amount DECIMAL(18,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  note VARCHAR(500) NULL,
  confirmed_by BIGINT UNSIGNED NULL,
  confirmed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reserve_reconciliation_month (reconciliation_month),
  CONSTRAINT fk_reserve_reconciliation_user FOREIGN KEY (confirmed_by) REFERENCES users (id),
  CONSTRAINT chk_reserve_reconciliation_status CHECK (status IN ('pending','confirmed'))
) ENGINE=InnoDB;

-- Rental mandates are effective immediately. Migrate records that were left in
-- the retired review workflow and restore the corresponding rental service.
UPDATE rental_mandates
SET status = 'active',
    submitted_at = NULL,
    reviewed_by = NULL,
    reviewed_at = NULL,
    review_note = NULL
WHERE status IN ('draft', 'pending_review');

-- Repair expenses confirmed by finance before payment-state synchronization was fixed.
UPDATE finance_records
SET payment_status = 'paid'
WHERE record_type = 'property_expense'
  AND confirmation_status = 'confirmed'
  AND payment_status = 'unpaid';

INSERT INTO owner_unit_services (owner_unit_id, service_type, status, started_at, ended_at)
SELECT owner_unit_id, 'RENTAL', 'active', start_date, NULL
FROM rental_mandates
WHERE status = 'active'
ON DUPLICATE KEY UPDATE status = 'active', ended_at = NULL;

-- A terminated rental service must not leave company-paid expenses waiting for payout.
-- Keep the finance row for audit history, but remove it from active expense and finance queues.
UPDATE finance_records fr
JOIN owner_units ou ON ou.owner_id = fr.owner_id AND ou.unit_id = fr.unit_id
JOIN owner_unit_services ous ON ous.owner_unit_id = ou.id
  AND ous.service_type = 'RENTAL' AND ous.status = 'ended'
SET fr.payment_status = 'voided',
    fr.confirmation_status = 'rejected',
    fr.sync_status = 'not_synced',
    fr.sync_batch_id = NULL,
    fr.confirmed_by = NULL,
    fr.confirmed_at = CURRENT_TIMESTAMP
WHERE fr.record_type = 'property_expense'
  AND fr.payment_method = 'direct_payment'
  AND fr.payment_status = 'unpaid'
  AND fr.confirmation_status = 'pending';
