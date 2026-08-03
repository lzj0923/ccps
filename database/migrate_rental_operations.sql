-- Rental operations: split monthly invoices into auditable charge items and
-- keep maintenance/cashflow records scoped to the lease that generated them.
ALTER TABLE cashflow_entries
  ADD COLUMN lease_id BIGINT UNSIGNED NULL AFTER unit_id,
  ADD KEY idx_cashflow_lease (lease_id),
  ADD CONSTRAINT fk_cashflow_lease FOREIGN KEY (lease_id) REFERENCES leases (id);

ALTER TABLE maintenance_work_orders
  ADD COLUMN lease_id BIGINT UNSIGNED NULL AFTER unit_id,
  ADD KEY idx_work_orders_lease (lease_id),
  ADD CONSTRAINT fk_work_orders_lease FOREIGN KEY (lease_id) REFERENCES leases (id);

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
