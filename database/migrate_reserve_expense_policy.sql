-- Automatically fund confirmed property expenses from the owner's reserve account.
-- An explicitly selected reserve payment may produce a negative balance; the
-- negative amount represents the owner's outstanding replenishment obligation.
USE ccps_property_management;
SET NAMES utf8mb4;

DROP TRIGGER IF EXISTS trg_cashflow_expense_reserve_before;
DROP TRIGGER IF EXISTS trg_cashflow_expense_reserve_after;
DROP TRIGGER IF EXISTS trg_finance_expense_reserve_before_update;
DROP TRIGGER IF EXISTS trg_finance_expense_reserve_after_update;
DROP TRIGGER IF EXISTS trg_work_order_reserve_link_after_insert;
DROP TRIGGER IF EXISTS trg_work_order_reserve_link_after_update;

DELIMITER $$

CREATE TRIGGER trg_cashflow_expense_reserve_before
BEFORE INSERT ON cashflow_entries
FOR EACH ROW
BEGIN
  DECLARE v_amount DECIMAL(18,2);
  DECLARE v_reserve_id BIGINT UNSIGNED;
  DECLARE v_balance DECIMAL(18,2);
  DECLARE v_payment_status VARCHAR(30);
  DECLARE v_confirmation_status VARCHAR(30);

  IF NEW.direction = 'expense' THEN
    SET v_amount = NULL;
    SET v_reserve_id = NULL;
    SET v_balance = NULL;
    SELECT fr.amount, fr.payment_status, fr.confirmation_status
      INTO v_amount, v_payment_status, v_confirmation_status
      FROM finance_records fr
     WHERE fr.id = NEW.finance_record_id;
    IF v_payment_status = 'paid' AND v_confirmation_status = 'confirmed' AND NEW.reserve_account_id IS NOT NULL THEN
      SELECT ra.current_balance
        INTO v_balance
        FROM reserve_accounts ra
       WHERE ra.id = NEW.reserve_account_id
       FOR UPDATE;
      IF v_amount IS NULL OR v_balance IS NULL THEN
        SET NEW.reserve_account_id = NULL;
      ELSE
        SET NEW.attachment_status = 'not_required';
      END IF;
    END IF;
    IF v_payment_status = 'paid' AND v_confirmation_status = 'confirmed' AND NEW.reserve_account_id IS NULL THEN
      SET v_balance = NULL;
    SELECT ra.id, ra.current_balance
      INTO v_reserve_id, v_balance
      FROM reserve_accounts ra
      JOIN owner_units ou ON ou.id = ra.owner_unit_id
     WHERE ou.unit_id = NEW.unit_id
       AND (NEW.owner_id IS NULL OR ou.owner_id = NEW.owner_id)
       AND ou.status = 'active'
       AND ou.asset_stage = 'OPERATING'
       AND ra.status = 'active'
     ORDER BY ra.id
     LIMIT 1
     FOR UPDATE;
      IF v_reserve_id IS NOT NULL AND v_amount IS NOT NULL AND v_balance >= v_amount THEN
        SET NEW.reserve_account_id = v_reserve_id;
        SET NEW.attachment_status = 'not_required';
      END IF;
    ELSEIF v_payment_status <> 'paid' OR v_confirmation_status <> 'confirmed' THEN
      SET NEW.reserve_account_id = NULL;
    END IF;
  END IF;
END$$

CREATE TRIGGER trg_cashflow_expense_reserve_after
AFTER INSERT ON cashflow_entries
FOR EACH ROW
BEGIN
  DECLARE v_amount DECIMAL(18,2);
  DECLARE v_balance DECIMAL(18,2);
  DECLARE v_after DECIMAL(18,2);
  DECLARE v_payment_status VARCHAR(30);
  DECLARE v_confirmation_status VARCHAR(30);

  IF NEW.direction = 'expense' AND NEW.reserve_account_id IS NOT NULL
     AND NOT EXISTS (
       SELECT 1 FROM reserve_transactions rt
        WHERE rt.finance_record_id = NEW.finance_record_id
          AND rt.transaction_type = 'debit'
     ) THEN
    SELECT fr.amount, fr.payment_status, fr.confirmation_status
      INTO v_amount, v_payment_status, v_confirmation_status
      FROM finance_records fr
     WHERE fr.id = NEW.finance_record_id;
    SELECT ra.current_balance
      INTO v_balance
      FROM reserve_accounts ra
     WHERE ra.id = NEW.reserve_account_id
     FOR UPDATE;
    IF v_payment_status = 'paid' AND v_confirmation_status = 'confirmed'
       AND v_amount IS NOT NULL AND v_balance IS NOT NULL THEN
      SET v_after = v_balance - v_amount;
      UPDATE reserve_accounts
         SET current_balance = v_after
       WHERE id = NEW.reserve_account_id;
      INSERT INTO reserve_transactions
        (reserve_account_id, finance_record_id, transaction_type, amount,
         occurred_at, balance_after, note, created_by)
      VALUES
        (NEW.reserve_account_id, NEW.finance_record_id, 'debit', v_amount,
         NOW(), v_after, '支出由預備金自動扣除', NULL);
      UPDATE finance_records
         SET payment_method = 'reserve_account',
             payment_status = 'paid',
             confirmation_status = 'confirmed',
             confirmed_at = COALESCE(confirmed_at, NOW())
       WHERE id = NEW.finance_record_id;
    END IF;
  END IF;
END$$

CREATE TRIGGER trg_finance_expense_reserve_before_update
BEFORE UPDATE ON finance_records
FOR EACH ROW
BEGIN
  DECLARE v_cashflow_id BIGINT UNSIGNED;
  DECLARE v_unit_id BIGINT UNSIGNED;
  DECLARE v_owner_id BIGINT UNSIGNED;
  DECLARE v_reserve_id BIGINT UNSIGNED;
  DECLARE v_balance DECIMAL(18,2);

  IF NEW.record_type = 'cashflow'
     AND NEW.payment_status = 'paid'
     AND NEW.confirmation_status = 'confirmed'
     AND (OLD.payment_status <> NEW.payment_status
          OR OLD.confirmation_status <> NEW.confirmation_status
          OR OLD.amount <> NEW.amount)
     AND NOT EXISTS (SELECT 1 FROM reserve_transactions rt WHERE rt.finance_record_id = NEW.id AND rt.transaction_type = 'debit') THEN
    SET v_cashflow_id = NULL;
    SET v_unit_id = NULL;
    SET v_owner_id = NULL;
    SET v_reserve_id = NULL;
    SET v_balance = NULL;
    SELECT ce.id, ce.unit_id, ce.owner_id
      INTO v_cashflow_id, v_unit_id, v_owner_id
      FROM cashflow_entries ce
     WHERE ce.finance_record_id = NEW.id
     LIMIT 1;
    SELECT ra.id, ra.current_balance
      INTO v_reserve_id, v_balance
      FROM reserve_accounts ra
      JOIN owner_units ou ON ou.id = ra.owner_unit_id
     WHERE ou.unit_id = v_unit_id
       AND (v_owner_id IS NULL OR ou.owner_id = v_owner_id)
       AND ou.status = 'active'
       AND ou.asset_stage = 'OPERATING'
       AND ra.status = 'active'
     ORDER BY ra.id
     LIMIT 1
     FOR UPDATE;
    IF v_reserve_id IS NOT NULL AND v_balance >= NEW.amount THEN
      SET NEW.payment_method = 'reserve_account';
    END IF;
  END IF;
END$$

CREATE TRIGGER trg_finance_expense_reserve_after_update
AFTER UPDATE ON finance_records
FOR EACH ROW
BEGIN
  DECLARE v_cashflow_id BIGINT UNSIGNED;
  DECLARE v_unit_id BIGINT UNSIGNED;
  DECLARE v_owner_id BIGINT UNSIGNED;
  DECLARE v_reserve_id BIGINT UNSIGNED;
  DECLARE v_balance DECIMAL(18,2);
  DECLARE v_after DECIMAL(18,2);

  IF NEW.record_type = 'cashflow'
     AND NEW.payment_method = 'reserve_account'
     AND NEW.payment_status = 'paid'
     AND NEW.confirmation_status = 'confirmed'
     AND NOT EXISTS (SELECT 1 FROM reserve_transactions rt WHERE rt.finance_record_id = NEW.id AND rt.transaction_type = 'debit') THEN
    SET v_cashflow_id = NULL;
    SET v_unit_id = NULL;
    SET v_owner_id = NULL;
    SET v_reserve_id = NULL;
    SET v_balance = NULL;
    SELECT ce.id, ce.unit_id, ce.owner_id
      INTO v_cashflow_id, v_unit_id, v_owner_id
      FROM cashflow_entries ce
     WHERE ce.finance_record_id = NEW.id
     LIMIT 1;
    SELECT ra.id, ra.current_balance
      INTO v_reserve_id, v_balance
      FROM reserve_accounts ra
      JOIN owner_units ou ON ou.id = ra.owner_unit_id
     WHERE ou.unit_id = v_unit_id
       AND (v_owner_id IS NULL OR ou.owner_id = v_owner_id)
       AND ou.status = 'active'
       AND ou.asset_stage = 'OPERATING'
       AND ra.status = 'active'
     ORDER BY ra.id
     LIMIT 1
     FOR UPDATE;
    IF v_reserve_id IS NOT NULL THEN
      SET v_after = v_balance - NEW.amount;
      UPDATE cashflow_entries
         SET reserve_account_id = v_reserve_id,
             attachment_status = 'not_required'
       WHERE id = v_cashflow_id;
      UPDATE reserve_accounts
         SET current_balance = v_after
       WHERE id = v_reserve_id;
      INSERT INTO reserve_transactions
        (reserve_account_id, finance_record_id, transaction_type, amount,
         occurred_at, balance_after, note, created_by)
      VALUES
        (v_reserve_id, NEW.id, 'debit', NEW.amount, COALESCE(NEW.transaction_date, CURRENT_DATE),
         v_after, '支出由預備金自動扣除', NULL);
    END IF;
  END IF;
END$$

CREATE TRIGGER trg_work_order_reserve_link_after_insert
AFTER INSERT ON maintenance_work_orders
FOR EACH ROW
BEGIN
  IF NEW.cashflow_entry_id IS NOT NULL THEN
    UPDATE reserve_transactions rt
    JOIN cashflow_entries ce ON ce.finance_record_id = rt.finance_record_id
    SET rt.maintenance_work_order_id = NEW.id
    WHERE ce.id = NEW.cashflow_entry_id
      AND rt.transaction_type = 'debit'
      AND rt.maintenance_work_order_id IS NULL;
  END IF;
END$$

CREATE TRIGGER trg_work_order_reserve_link_after_update
AFTER UPDATE ON maintenance_work_orders
FOR EACH ROW
BEGIN
  IF NEW.cashflow_entry_id IS NOT NULL
     AND (OLD.cashflow_entry_id IS NULL OR OLD.cashflow_entry_id <> NEW.cashflow_entry_id) THEN
    UPDATE reserve_transactions rt
    JOIN cashflow_entries ce ON ce.finance_record_id = rt.finance_record_id
    SET rt.maintenance_work_order_id = NEW.id
    WHERE ce.id = NEW.cashflow_entry_id
      AND rt.transaction_type = 'debit'
      AND rt.maintenance_work_order_id IS NULL;
  END IF;
END$$

CREATE PROCEDURE ccps_allocate_existing_expense_reserves()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_cashflow_id BIGINT UNSIGNED;
  DECLARE v_finance_id BIGINT UNSIGNED;
  DECLARE v_unit_id BIGINT UNSIGNED;
  DECLARE v_owner_id BIGINT UNSIGNED;
  DECLARE v_amount DECIMAL(18,2);
  DECLARE v_payment_method VARCHAR(30);
  DECLARE v_reserve_id BIGINT UNSIGNED;
  DECLARE v_balance DECIMAL(18,2);
  DECLARE v_after DECIMAL(18,2);

  DECLARE expense_cursor CURSOR FOR
    SELECT ce.id, ce.finance_record_id, ce.unit_id, ce.owner_id, fr.amount, fr.payment_method
      FROM cashflow_entries ce
      JOIN finance_records fr ON fr.id = ce.finance_record_id
     WHERE ce.direction = 'expense'
       AND fr.payment_status = 'paid'
       AND fr.confirmation_status = 'confirmed'
       AND NOT EXISTS (
         SELECT 1 FROM reserve_transactions rt
          WHERE rt.finance_record_id = ce.finance_record_id
            AND rt.transaction_type = 'debit'
       )
     ORDER BY ce.occurred_on, ce.id;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN expense_cursor;
  expense_loop: LOOP
    FETCH expense_cursor INTO v_cashflow_id, v_finance_id, v_unit_id, v_owner_id, v_amount, v_payment_method;
    IF done THEN
      LEAVE expense_loop;
    END IF;
    SET v_reserve_id = NULL;
    SET v_balance = NULL;
    SELECT ra.id, ra.current_balance
      INTO v_reserve_id, v_balance
      FROM reserve_accounts ra
      JOIN owner_units ou ON ou.id = ra.owner_unit_id
     WHERE ou.unit_id = v_unit_id
       AND (v_owner_id IS NULL OR ou.owner_id = v_owner_id)
       AND ou.status = 'active'
       AND ou.asset_stage = 'OPERATING'
       AND ra.status = 'active'
     ORDER BY ra.id
     LIMIT 1;
    -- Explicit reserve-funded expenses must still be deducted when the account
    -- is insufficient.  The negative balance is the owner's replenishment due.
    -- For older records without an explicit reserve payment method, keep the
    -- conservative auto-allocation rule and only use an available balance.
    IF v_reserve_id IS NOT NULL
       AND (v_payment_method = 'reserve_account' OR v_balance >= v_amount) THEN
      SET v_after = v_balance - v_amount;
      UPDATE cashflow_entries
         SET reserve_account_id = v_reserve_id,
             attachment_status = 'not_required'
       WHERE id = v_cashflow_id;
      UPDATE reserve_accounts
         SET current_balance = v_after
       WHERE id = v_reserve_id;
      INSERT INTO reserve_transactions
        (reserve_account_id, finance_record_id, transaction_type, amount,
         occurred_at, balance_after, note, created_by)
      SELECT v_reserve_id, v_finance_id, 'debit', v_amount,
             COALESCE(fr.transaction_date, CURRENT_DATE), v_after,
             '支出由預備金自動扣除', NULL
        FROM finance_records fr
       WHERE fr.id = v_finance_id;
      UPDATE finance_records
         SET payment_method = 'reserve_account',
             payment_status = 'paid',
             confirmation_status = 'confirmed',
             confirmed_at = COALESCE(confirmed_at, NOW())
       WHERE id = v_finance_id;
    END IF;
  END LOOP;
  CLOSE expense_cursor;
END$$

DELIMITER ;

CALL ccps_allocate_existing_expense_reserves();
DROP PROCEDURE ccps_allocate_existing_expense_reserves;

-- Undo any earlier automatic debit that was created before financial confirmation.
UPDATE reserve_accounts ra
JOIN reserve_transactions rt ON rt.reserve_account_id = ra.id
JOIN finance_records fr ON fr.id = rt.finance_record_id
SET ra.current_balance = ra.current_balance + rt.amount
WHERE rt.transaction_type = 'debit'
  AND (fr.payment_status <> 'paid' OR fr.confirmation_status <> 'confirmed');

UPDATE cashflow_entries ce
JOIN finance_records fr ON fr.id = ce.finance_record_id
JOIN reserve_transactions rt
  ON rt.finance_record_id = fr.id
 AND rt.transaction_type = 'debit'
SET ce.reserve_account_id = NULL,
    ce.attachment_status = 'missing'
WHERE fr.payment_status <> 'paid' OR fr.confirmation_status <> 'confirmed';

DELETE rt
  FROM reserve_transactions rt
  JOIN finance_records fr ON fr.id = rt.finance_record_id
 WHERE rt.transaction_type = 'debit'
   AND (fr.payment_status <> 'paid' OR fr.confirmation_status <> 'confirmed');

-- Normalize older reserve-funded rows created before this policy was installed.
UPDATE cashflow_entries ce
JOIN reserve_transactions rt
  ON rt.finance_record_id = ce.finance_record_id
 AND rt.transaction_type = 'debit'
SET ce.attachment_status = 'not_required'
WHERE ce.direction = 'expense';

UPDATE finance_records fr
JOIN reserve_transactions rt
  ON rt.finance_record_id = fr.id
 AND rt.transaction_type = 'debit'
SET fr.payment_method = 'reserve_account',
    fr.payment_status = 'paid',
    fr.confirmation_status = 'confirmed',
    fr.confirmed_at = COALESCE(fr.confirmed_at, NOW())
WHERE fr.record_type = 'cashflow';
