CREATE TABLE IF NOT EXISTS property_important_messages (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  subject VARCHAR(200) NOT NULL,
  content VARCHAR(2000) NULL,
  announcement_start_date DATE NOT NULL,
  announcement_end_date DATE NULL,
  importance VARCHAR(20) NOT NULL DEFAULT 'normal',
  is_read TINYINT(1) NOT NULL DEFAULT 0,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_property_important_messages_unit_date (owner_unit_id, announcement_start_date, announcement_end_date),
  KEY idx_property_important_messages_flags (owner_unit_id, importance, is_read),
  CONSTRAINT fk_property_important_messages_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_property_important_messages_creator FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
