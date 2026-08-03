-- Backfill monthly rent invoices that were skipped when a lease was entered late.
-- Safe to run repeatedly: uk_rent_invoices_month (lease_id, billing_month)
-- and INSERT IGNORE preserve every existing invoice and payment state.

INSERT IGNORE INTO rent_invoices
    (lease_id, billing_month, due_date, amount_due, amount_paid, status)
WITH RECURSIVE lease_months AS (
    SELECT
        l.id AS lease_id,
        DATE_SUB(l.start_date, INTERVAL (DAYOFMONTH(l.start_date) - 1) DAY) AS billing_month,
        LEAST(
            DATE_SUB(l.end_date, INTERVAL (DAYOFMONTH(l.end_date) - 1) DAY),
            DATE_SUB(CURRENT_DATE, INTERVAL (DAYOFMONTH(CURRENT_DATE) - 1) DAY)
        ) AS last_billing_month,
        l.payment_day,
        l.monthly_rent,
        l.start_date,
        l.end_date,
        l.rent_calculation_method
    FROM leases l
    WHERE l.status = 'active'
      AND l.start_date <= CURRENT_DATE

    UNION ALL

    SELECT
        lease_id,
        DATE_ADD(billing_month, INTERVAL 1 MONTH),
        last_billing_month,
        payment_day,
        monthly_rent,
        start_date,
        end_date,
        rent_calculation_method
    FROM lease_months
    WHERE billing_month < last_billing_month
)
SELECT
    lease_id,
    billing_month,
    DATE_ADD(
        billing_month,
        INTERVAL (LEAST(payment_day, DAY(LAST_DAY(billing_month))) - 1) DAY
    ) AS due_date,
    CASE WHEN rent_calculation_method = 'daily_prorated'
      THEN ROUND(monthly_rent / DAY(LAST_DAY(billing_month)) *
        (DATEDIFF(LEAST(end_date, LAST_DAY(billing_month)), GREATEST(start_date, billing_month)) + 1), 2)
      ELSE monthly_rent END,
    0,
    'unpaid'
FROM lease_months
WHERE billing_month <= last_billing_month;
