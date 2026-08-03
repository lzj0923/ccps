USE ccps_property_management;

CREATE TABLE IF NOT EXISTS property_handover_checklist_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  category VARCHAR(120) NOT NULL,
  item_name VARCHAR(255) NOT NULL,
  default_quantity VARCHAR(80) NULL,
  notes VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_property_handover_checklist_owner (owner_unit_id, enabled, category, sort_order),
  CONSTRAINT fk_property_handover_checklist_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id)
) ENGINE=InnoDB;
