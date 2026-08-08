-- 附件签约：为同一文件的多角色连续签署增加分组与顺序字段。
-- 执行后重启后端；历史签署记录会自动按原文件回填为普通签署人。

ALTER TABLE electronic_signature_requests
  ADD COLUMN root_document_id BIGINT UNSIGNED NULL AFTER source_document_id,
  ADD COLUMN document_kind VARCHAR(64) NULL AFTER entity_id,
  ADD COLUMN signer_role VARCHAR(32) NOT NULL DEFAULT 'signer' AFTER document_kind,
  ADD COLUMN signing_order INT NOT NULL DEFAULT 1 AFTER signer_role,
  ADD KEY idx_e_signature_package (root_document_id, signing_order, status),
  ADD CONSTRAINT fk_e_signature_root_document FOREIGN KEY (root_document_id) REFERENCES documents (id);

UPDATE electronic_signature_requests
SET root_document_id=source_document_id
WHERE root_document_id IS NULL;
