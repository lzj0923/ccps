-- 租客账单费用改为财务确认后入账。
-- 历史 rent_invoice_items 不回填 finance_record_id，保持既有已入账语义。

ALTER TABLE rent_invoice_items
  ADD COLUMN finance_record_id BIGINT UNSIGNED NULL
    COMMENT '待财务确认的租客账单费用；历史记录为空即视为已确认'
    AFTER invoice_id,
  ADD UNIQUE KEY uk_invoice_items_finance (finance_record_id),
  ADD CONSTRAINT fk_invoice_items_finance
    FOREIGN KEY (finance_record_id) REFERENCES finance_records (id);
