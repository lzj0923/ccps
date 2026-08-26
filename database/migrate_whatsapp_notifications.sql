USE ccps_property_management;

CREATE TABLE IF NOT EXISTS tenant_whatsapp_subscriptions (
  tenant_id BIGINT UNSIGNED NOT NULL,
  destination VARCHAR(40) NOT NULL COMMENT 'E.164 digits without plus sign',
  enabled TINYINT(1) NOT NULL DEFAULT 0,
  opted_in_at DATETIME NULL,
  opted_out_at DATETIME NULL,
  opt_in_source VARCHAR(120) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (tenant_id),
  KEY idx_tenant_whatsapp_enabled (enabled, updated_at),
  CONSTRAINT fk_tenant_whatsapp_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS whatsapp_delivery_attempts (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  delivery_id BIGINT UNSIGNED NOT NULL,
  attempt_number SMALLINT UNSIGNED NOT NULL,
  provider_message_id VARCHAR(191) NULL,
  provider_wa_id VARCHAR(40) NULL,
  template_name VARCHAR(160) NOT NULL,
  template_language VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'sending',
  status_at DATETIME NULL,
  delivered_at DATETIME NULL,
  read_at DATETIME NULL,
  meta_error_code INT NULL,
  meta_error_subcode INT NULL,
  meta_error_details VARCHAR(500) NULL,
  fbtrace_id VARCHAR(120) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_whatsapp_delivery_attempt (delivery_id, attempt_number),
  UNIQUE KEY uk_whatsapp_provider_message (provider_message_id),
  KEY idx_whatsapp_attempt_status (status, status_at),
  CONSTRAINT fk_whatsapp_attempt_delivery FOREIGN KEY (delivery_id) REFERENCES notification_deliveries (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
