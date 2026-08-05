-- Link lease deposits to finance confirmation without changing rent invoices.
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

INSERT INTO finance_records
  (transaction_no,record_type,unit_id,owner_id,tenant_id,amount,currency,transaction_date,
   payment_method,payment_status,confirmation_status,sync_status,created_by)
SELECT CONCAT('DEPOSIT-',l.lease_no),'security_deposit',l.unit_id,ou.owner_id,l.tenant_id,l.deposit_amount,'MYR',l.start_date,
       'internal_accrual','unpaid','pending','not_synced',NULL
FROM leases l
JOIN owner_units ou ON ou.id=(SELECT ou2.id FROM owner_units ou2 WHERE ou2.unit_id=l.unit_id AND ou2.status='active' ORDER BY ou2.is_primary DESC,ou2.id LIMIT 1)
WHERE l.deposit_amount>0
  AND NOT EXISTS (SELECT 1 FROM security_deposit_entries sde WHERE sde.lease_id=l.id)
  AND NOT EXISTS (SELECT 1 FROM finance_records fr WHERE fr.transaction_no=CONCAT('DEPOSIT-',l.lease_no));

INSERT INTO security_deposit_entries (lease_id,finance_record_id,amount,status)
SELECT l.id,fr.id,l.deposit_amount,'pending'
FROM leases l
JOIN finance_records fr ON fr.transaction_no=CONCAT('DEPOSIT-',l.lease_no) AND fr.record_type='security_deposit'
WHERE l.deposit_amount>0
  AND NOT EXISTS (SELECT 1 FROM security_deposit_entries sde WHERE sde.lease_id=l.id);

INSERT INTO cashflow_entries
  (finance_record_id,unit_id,lease_id,owner_id,tenant_id,direction,category,description,occurred_on,attachment_status)
SELECT fr.id,l.unit_id,l.id,fr.owner_id,l.tenant_id,'income','deposit',CONCAT('租客押金 · ',l.lease_no),l.start_date,'not_required'
FROM leases l
JOIN finance_records fr ON fr.transaction_no=CONCAT('DEPOSIT-',l.lease_no) AND fr.record_type='security_deposit'
JOIN security_deposit_entries sde ON sde.finance_record_id=fr.id
WHERE NOT EXISTS (SELECT 1 FROM cashflow_entries ce WHERE ce.finance_record_id=fr.id);
