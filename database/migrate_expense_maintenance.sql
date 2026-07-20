USE ccps_property_management;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS maintenance_status_history (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  work_order_id BIGINT UNSIGNED NOT NULL,
  status VARCHAR(30) NOT NULL,
  occurred_at DATETIME NOT NULL,
  note VARCHAR(500) NULL,
  changed_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_maintenance_history_order_time (work_order_id, occurred_at),
  CONSTRAINT fk_maintenance_history_order FOREIGN KEY (work_order_id) REFERENCES maintenance_work_orders (id),
  CONSTRAINT fk_maintenance_history_user FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB;
