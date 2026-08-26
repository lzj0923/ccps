CREATE TABLE IF NOT EXISTS rent_collection_workflows (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  invoice_id BIGINT UNSIGNED NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT 'active / on_hold / resolved',
  hold_reason VARCHAR(500) NULL,
  held_by BIGINT UNSIGNED NULL,
  held_at DATETIME NULL,
  resolved_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rent_collection_workflow_invoice (invoice_id),
  KEY idx_rent_collection_workflow_status (status, updated_at),
  CONSTRAINT fk_rent_collection_workflow_invoice FOREIGN KEY (invoice_id) REFERENCES rent_invoices (id),
  CONSTRAINT fk_rent_collection_workflow_holder FOREIGN KEY (held_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS rent_collection_actions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  invoice_id BIGINT UNSIGNED NOT NULL,
  stage VARCHAR(32) NOT NULL COMMENT 'first_reminder / second_reminder / final_reminder / termination_notice',
  threshold_days SMALLINT UNSIGNED NOT NULL,
  scheduled_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'sent' COMMENT 'sent / cancelled / failed',
  title VARCHAR(200) NOT NULL,
  body VARCHAR(1000) NOT NULL,
  notification_id BIGINT UNSIGNED NULL,
  acted_by BIGINT UNSIGNED NULL,
  acted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rent_collection_action_stage (invoice_id, stage),
  KEY idx_rent_collection_action_date (scheduled_date, status),
  CONSTRAINT fk_rent_collection_action_invoice FOREIGN KEY (invoice_id) REFERENCES rent_invoices (id),
  CONSTRAINT fk_rent_collection_action_notification FOREIGN KEY (notification_id) REFERENCES notifications (id),
  CONSTRAINT fk_rent_collection_action_actor FOREIGN KEY (acted_by) REFERENCES users (id)
) ENGINE=InnoDB;
