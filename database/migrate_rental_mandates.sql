-- R02 vertical slice: rental mandate and auditable status history.
-- Run once against an existing CCPS database before using /api/admin/rental-mandates.

CREATE TABLE IF NOT EXISTS rental_mandates (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  mandate_no VARCHAR(60) NOT NULL,
  mandate_type VARCHAR(30) NOT NULL DEFAULT 'management',
  start_date DATE NOT NULL,
  end_date DATE NULL,
  management_fee DECIMAL(18,2) NULL,
  commission_percent DECIMAL(5,2) NULL,
  responsible_user_id BIGINT UNSIGNED NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'draft',
  submitted_at DATETIME NULL,
  reviewed_by BIGINT UNSIGNED NULL,
  reviewed_at DATETIME NULL,
  review_note VARCHAR(500) NULL,
  termination_reason VARCHAR(500) NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rental_mandates_no (mandate_no),
  KEY idx_rental_mandates_unit_status (owner_unit_id, status),
  KEY idx_rental_mandates_responsible (responsible_user_id, status),
  CONSTRAINT fk_rental_mandates_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_rental_mandates_responsible FOREIGN KEY (responsible_user_id) REFERENCES users (id),
  CONSTRAINT fk_rental_mandates_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id),
  CONSTRAINT fk_rental_mandates_creator FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_rental_mandates_dates CHECK (end_date IS NULL OR end_date >= start_date),
  CONSTRAINT chk_rental_mandates_fee CHECK (management_fee IS NULL OR management_fee >= 0),
  CONSTRAINT chk_rental_mandates_commission CHECK (commission_percent IS NULL OR (commission_percent >= 0 AND commission_percent <= 100)),
  CONSTRAINT chk_rental_mandates_status CHECK (status IN ('draft', 'pending_review', 'active', 'suspended', 'terminated'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rental_mandate_status_history (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  mandate_id BIGINT UNSIGNED NOT NULL,
  from_status VARCHAR(30) NULL,
  to_status VARCHAR(30) NOT NULL,
  reason VARCHAR(500) NULL,
  changed_by BIGINT UNSIGNED NULL,
  changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_rental_mandate_history_mandate (mandate_id, changed_at),
  CONSTRAINT fk_rental_mandate_history_mandate FOREIGN KEY (mandate_id) REFERENCES rental_mandates (id),
  CONSTRAINT fk_rental_mandate_history_user FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB;
