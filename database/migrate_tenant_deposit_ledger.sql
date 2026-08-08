CREATE TABLE IF NOT EXISTS tenant_deposit_transactions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  lease_id BIGINT UNSIGNED NOT NULL,
  tenant_id BIGINT UNSIGNED NOT NULL,
  unit_id BIGINT UNSIGNED NOT NULL,
  finance_record_id BIGINT UNSIGNED NULL,
  transaction_type VARCHAR(40) NOT NULL,
  direction VARCHAR(10) NOT NULL,
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
