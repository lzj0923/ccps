ALTER TABLE property_bank_accounts
  ADD COLUMN bank_address VARCHAR(500) NULL AFTER account_no,
  ADD COLUMN branch_code VARCHAR(80) NULL AFTER bank_address,
  ADD COLUMN swift_code VARCHAR(80) NULL AFTER branch_code;
