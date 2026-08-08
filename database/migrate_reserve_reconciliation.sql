CREATE TABLE IF NOT EXISTS reserve_reconciliations (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reconciliation_month DATE NOT NULL COMMENT '统一存该月份第一天',
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
