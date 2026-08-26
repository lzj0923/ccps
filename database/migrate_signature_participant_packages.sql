-- 多人电子签署：保存每份文件的完整签署人配置，支持自动顺序邀请与重新发起时编辑。
CREATE TABLE IF NOT EXISTS electronic_signature_participants (
  root_document_id BIGINT UNSIGNED NOT NULL,
  document_kind VARCHAR(64) NOT NULL,
  signer_role VARCHAR(32) NOT NULL,
  signing_order INT NOT NULL,
  signer_name VARCHAR(190) NOT NULL,
  signer_email VARCHAR(190) NOT NULL,
  expires_in_days INT NOT NULL DEFAULT 7,
  updated_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (root_document_id, signer_role),
  UNIQUE KEY uk_e_signature_participant_order (root_document_id, signing_order),
  CONSTRAINT fk_e_signature_participant_document FOREIGN KEY (root_document_id) REFERENCES documents (id),
  CONSTRAINT fk_e_signature_participant_user FOREIGN KEY (updated_by) REFERENCES users (id)
) ENGINE=InnoDB;
