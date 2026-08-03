-- 統一採用「月租 / 當月天數 * 當月實際承租天數」，起訖日均計入。
-- 此腳本可重複執行；只有金額確實改變時才新增審計記錄。
START TRANSACTION;

ALTER TABLE leases
  MODIFY COLUMN rent_calculation_method VARCHAR(30) NOT NULL DEFAULT 'daily_prorated'
    COMMENT '按當月實際承租天數折算';

INSERT INTO audit_logs (actor_user_id, action, entity_type, entity_id, before_data, after_data)
SELECT
  NULL,
  'recalculate_daily_prorated_rent',
  'rent_invoice',
  calculated.invoice_id,
  JSON_OBJECT(
    'amountDue', calculated.old_amount_due,
    'calculationMethod', calculated.old_calculation_method
  ),
  JSON_OBJECT(
    'amountDue', calculated.new_amount_due,
    'calculationMethod', 'daily_prorated',
    'activeDays', calculated.active_days,
    'daysInMonth', calculated.days_in_month
  )
FROM (
  SELECT
    ri.id AS invoice_id,
    ri.amount_due AS old_amount_due,
    l.rent_calculation_method AS old_calculation_method,
    DAY(LAST_DAY(ri.billing_month)) AS days_in_month,
    GREATEST(
      DATEDIFF(
        LEAST(l.end_date, LAST_DAY(ri.billing_month)),
        GREATEST(l.start_date, ri.billing_month)
      ) + 1,
      0
    ) AS active_days,
    ROUND(
      l.monthly_rent / DAY(LAST_DAY(ri.billing_month)) *
      GREATEST(
        DATEDIFF(
          LEAST(l.end_date, LAST_DAY(ri.billing_month)),
          GREATEST(l.start_date, ri.billing_month)
        ) + 1,
        0
      ),
      2
    ) AS new_amount_due
  FROM rent_invoices ri
  JOIN leases l ON l.id = ri.lease_id
) calculated
WHERE calculated.old_amount_due <> calculated.new_amount_due;

UPDATE leases
SET rent_calculation_method = 'daily_prorated'
WHERE rent_calculation_method <> 'daily_prorated';

UPDATE rent_invoices ri
JOIN leases l ON l.id = ri.lease_id
SET ri.amount_due = ROUND(
  l.monthly_rent / DAY(LAST_DAY(ri.billing_month)) *
  GREATEST(
    DATEDIFF(
      LEAST(l.end_date, LAST_DAY(ri.billing_month)),
      GREATEST(l.start_date, ri.billing_month)
    ) + 1,
    0
  ),
  2
);

COMMIT;
