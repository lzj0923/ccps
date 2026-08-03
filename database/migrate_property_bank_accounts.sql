CREATE TABLE IF NOT EXISTS property_bank_accounts (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  item_name VARCHAR(120) NOT NULL,
  payment_name VARCHAR(160) NOT NULL,
  account_no VARCHAR(120) NOT NULL,
  remarks VARCHAR(1000) NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_property_bank_accounts_owner_unit (owner_unit_id, id),
  CONSTRAINT fk_property_bank_accounts_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_property_bank_accounts_creator FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO property_bank_accounts (owner_unit_id,item_name,payment_name,account_no,remarks)
SELECT p.owner_unit_id,
       COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(p.profile_json,'$.bankName')),''),'銀行帳戶'),
       COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(p.profile_json,'$.bankAccountName')),''),'未設定付款名稱'),
       JSON_UNQUOTE(JSON_EXTRACT(p.profile_json,'$.bankAccountNo')),
       '由房產基本資料自動匯入'
FROM property_basic_profiles p
WHERE NULLIF(JSON_UNQUOTE(JSON_EXTRACT(p.profile_json,'$.bankAccountNo')),'') IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM property_bank_accounts a
    WHERE a.owner_unit_id=p.owner_unit_id
      AND a.account_no=JSON_UNQUOTE(JSON_EXTRACT(p.profile_json,'$.bankAccountNo'))
  );
