-- Self-hosted electronic signing for uploaded lease and rental-mandate PDF contracts.
-- Run once against an existing CCPS database.

CREATE TABLE IF NOT EXISTS electronic_signature_requests (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_document_id BIGINT UNSIGNED NOT NULL,
  entity_type VARCHAR(40) NOT NULL,
  entity_id BIGINT UNSIGNED NOT NULL,
  signer_name VARCHAR(190) NOT NULL,
  signer_email VARCHAR(190) NOT NULL,
  access_token_hash CHAR(64) NOT NULL,
  verification_code_hash VARCHAR(100) NOT NULL,
  verification_expires_at DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  expires_at DATETIME NOT NULL,
  requested_by BIGINT UNSIGNED NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  signed_at DATETIME NULL,
  signed_document_id BIGINT UNSIGNED NULL,
  source_checksum_sha256 CHAR(64) NULL,
  signature_hash CHAR(64) NULL,
  signer_ip VARCHAR(64) NULL,
  signer_user_agent VARCHAR(500) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_e_signature_token (access_token_hash),
  KEY idx_e_signature_entity (entity_type, entity_id, status),
  KEY idx_e_signature_expiry (status, expires_at),
  CONSTRAINT fk_e_signature_source_document FOREIGN KEY (source_document_id) REFERENCES documents (id),
  CONSTRAINT fk_e_signature_signed_document FOREIGN KEY (signed_document_id) REFERENCES documents (id),
  CONSTRAINT fk_e_signature_requester FOREIGN KEY (requested_by) REFERENCES users (id),
  CONSTRAINT chk_e_signature_status CHECK (status IN ('pending', 'signed', 'rejected', 'cancelled', 'expired'))
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS electronic_signature_events (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  signature_request_id BIGINT UNSIGNED NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  detail VARCHAR(500) NULL,
  remote_ip VARCHAR(64) NULL,
  user_agent VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_e_signature_events_request (signature_request_id, created_at),
  CONSTRAINT fk_e_signature_events_request FOREIGN KEY (signature_request_id) REFERENCES electronic_signature_requests (id)
) ENGINE=InnoDB;
