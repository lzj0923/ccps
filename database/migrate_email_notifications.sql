USE ccps_property_management;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS notification_subscriptions (
  user_id BIGINT UNSIGNED NOT NULL,
  channel VARCHAR(20) NOT NULL,
  destination VARCHAR(255) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 0,
  verified_at DATETIME NULL,
  verification_code_hash VARCHAR(255) NULL,
  verification_expires_at DATETIME NULL,
  last_verification_sent_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, channel),
  KEY idx_notification_subscriptions_delivery (channel, enabled, verified_at),
  CONSTRAINT fk_notification_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;
