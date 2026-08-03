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
