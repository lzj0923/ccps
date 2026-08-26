CREATE TABLE IF NOT EXISTS system_financial_notification_actions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  action_type VARCHAR(40) NOT NULL COMMENT 'building_payment / reserve',
  related_id BIGINT UNSIGNED NOT NULL,
  stage VARCHAR(40) NOT NULL,
  period_key VARCHAR(20) NOT NULL,
  scheduled_date DATE NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'sent',
  notification_id BIGINT UNSIGNED NULL,
  title VARCHAR(200) NOT NULL,
  body VARCHAR(1000) NOT NULL,
  sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_system_financial_notice (action_type, related_id, stage, period_key),
  KEY idx_system_financial_notice_schedule (scheduled_date, status),
  KEY idx_system_financial_notice_notification (notification_id),
  CONSTRAINT fk_system_financial_notice_notification
    FOREIGN KEY (notification_id) REFERENCES notifications (id)
) ENGINE=InnoDB;
