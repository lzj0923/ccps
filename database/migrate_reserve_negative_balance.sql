-- Allow reserve accounts to remain auditable when approved payments exceed the available reserve.
-- Run once on an existing CCPS database before deploying this release.
ALTER TABLE reserve_transactions DROP CHECK chk_reserve_transactions_amount;
ALTER TABLE reserve_transactions
  ADD CONSTRAINT chk_reserve_transactions_amount CHECK (amount > 0);

ALTER TABLE reserve_accounts DROP CHECK chk_reserve_accounts_balance;
ALTER TABLE reserve_accounts
  ADD CONSTRAINT chk_reserve_accounts_balance CHECK (minimum_balance >= 0);
