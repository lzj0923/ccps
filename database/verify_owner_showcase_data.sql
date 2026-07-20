USE ccps_property_management;
SET NAMES utf8mb4;

SET @owner_user_id = (SELECT id FROM users WHERE username = 'owner' AND account_type = 'OWNER' LIMIT 1);
SET @owner_id = (SELECT id FROM owners WHERE user_id = @owner_user_id LIMIT 1);

SELECT 'OWNER_PROFILE' AS section, u.username, u.display_name, o.full_name, o.phone, o.email
FROM users u
JOIN owners o ON o.user_id = u.id
WHERE u.id = @owner_user_id;

SELECT 'PROPERTY_STAGE_COUNTS' AS section, ou.asset_stage, COUNT(*) AS property_count
FROM owner_units ou
WHERE ou.owner_id = @owner_id AND ou.status = 'active'
GROUP BY ou.asset_stage
ORDER BY ou.asset_stage;

SELECT 'PRE_HANDOVER_PAYMENT_STATES' AS section, p.name AS project_name, un.unit_no,
       pc.purchase_price,
       SUM(pi.amount_paid) AS paid_amount,
       pc.purchase_price - SUM(pi.amount_paid) AS unpaid_amount,
       MIN(CASE WHEN pi.amount_paid < pi.amount_due THEN pi.due_date END) AS next_due_date,
       CASE
         WHEN MAX(pi.amount_paid < pi.amount_due AND pi.due_date < CURRENT_DATE) = 1 THEN 'overdue'
         WHEN MAX(pi.amount_paid < pi.amount_due AND pi.due_date <= DATE_ADD(CURRENT_DATE, INTERVAL 30 DAY)) = 1 THEN 'due_soon'
         WHEN MAX(pi.amount_paid < pi.amount_due) = 1 THEN 'paying'
         ELSE 'paid'
       END AS payment_state
FROM owner_units ou
JOIN units un ON un.id = ou.unit_id
JOIN projects p ON p.id = un.project_id
JOIN purchase_contracts pc ON pc.owner_unit_id = ou.id
JOIN payment_plans pp ON pp.purchase_contract_id = pc.id
JOIN payment_installments pi ON pi.payment_plan_id = pp.id
WHERE ou.owner_id = @owner_id AND ou.asset_stage = 'PRE_HANDOVER' AND ou.status = 'active'
GROUP BY ou.id, p.name, un.unit_no, pc.purchase_price
ORDER BY un.unit_no;

SELECT 'OPERATING_PROPERTIES' AS section, p.name AS project_name, un.unit_no,
       service_data.services,
       COALESCE(t.full_name, 'No active tenant') AS tenant_name,
       COALESCE(l.monthly_rent, 0) AS monthly_rent,
       COALESCE(ri.amount_paid, 0) AS rent_paid,
       COALESCE(ri.amount_due - ri.amount_paid, 0) AS rent_outstanding,
       ra.minimum_balance, ra.current_balance,
       COALESCE(cashflow_data.current_month_income, 0) AS current_month_income,
       COALESCE(cashflow_data.current_month_expense, 0) AS current_month_expense,
       COALESCE(maintenance_data.maintenance_count, 0) AS maintenance_count
FROM owner_units ou
JOIN units un ON un.id = ou.unit_id
JOIN projects p ON p.id = un.project_id
LEFT JOIN (
  SELECT owner_unit_id, GROUP_CONCAT(service_type ORDER BY service_type SEPARATOR '+') AS services
  FROM owner_unit_services
  WHERE status = 'active'
  GROUP BY owner_unit_id
) service_data ON service_data.owner_unit_id = ou.id
LEFT JOIN leases l ON l.unit_id = un.id AND l.status = 'active'
LEFT JOIN tenants t ON t.id = l.tenant_id
LEFT JOIN rent_invoices ri ON ri.lease_id = l.id AND ri.billing_month = DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
LEFT JOIN reserve_accounts ra ON ra.owner_unit_id = ou.id AND ra.status = 'active'
LEFT JOIN (
  SELECT ce.unit_id,
         SUM(CASE WHEN ce.direction = 'income' THEN fr.amount ELSE 0 END) AS current_month_income,
         SUM(CASE WHEN ce.direction = 'expense' THEN fr.amount ELSE 0 END) AS current_month_expense
  FROM cashflow_entries ce
  JOIN finance_records fr ON fr.id = ce.finance_record_id
  WHERE ce.occurred_on >= DATE_FORMAT(CURRENT_DATE, '%Y-%m-01')
    AND ce.occurred_on < DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 1 MONTH)
  GROUP BY ce.unit_id
) cashflow_data ON cashflow_data.unit_id = un.id
LEFT JOIN (
  SELECT unit_id, COUNT(*) AS maintenance_count
  FROM maintenance_work_orders
  WHERE work_order_no LIKE 'OWNER-DEMO-M%'
  GROUP BY unit_id
) maintenance_data ON maintenance_data.unit_id = un.id
WHERE ou.owner_id = @owner_id AND ou.asset_stage = 'OPERATING' AND ou.status = 'active'
ORDER BY un.unit_no;

SELECT 'NOTIFICATIONS' AS section, priority, status, title
FROM notifications
WHERE recipient_owner_id = @owner_id AND title IN (
  'Meridian Park 房款即將到期',
  'Mont Kiara Verde 租金尚未收齊',
  '預備金低於最低標準',
  '維修工單處理中'
)
ORDER BY created_at DESC;
