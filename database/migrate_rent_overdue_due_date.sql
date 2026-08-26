-- 租金逾期必须始终以账单到期日为基准，催缴动作不得重置逾期天数。
USE ccps_property_management;

CREATE OR REPLACE VIEW v_rent_collection_status AS
SELECT
  ri.id AS rent_invoice_id,
  l.id AS lease_id,
  l.unit_id,
  l.tenant_id,
  ri.billing_month,
  ri.due_date,
  ri.amount_due,
  ri.amount_paid,
  GREATEST(ri.amount_due - ri.amount_paid, 0) AS unpaid_amount,
  CASE
    WHEN ri.amount_paid >= ri.amount_due THEN 'paid'
    WHEN ri.amount_paid > 0 THEN 'partial'
    WHEN CURRENT_DATE > ri.due_date THEN 'overdue'
    ELSE 'unpaid'
  END AS calculated_status
FROM rent_invoices ri
JOIN leases l ON l.id = ri.lease_id
WHERE EXISTS (
  SELECT 1
  FROM owner_units ou
  JOIN owner_unit_services ous ON ous.owner_unit_id = ou.id
  WHERE ou.unit_id = l.unit_id
    AND ou.status = 'active'
    AND ou.asset_stage = 'OPERATING'
    AND ous.service_type = 'RENTAL'
    AND ous.status = 'active'
);
