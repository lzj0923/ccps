USE ccps_property_management;

SELECT asset_stage, COUNT(*) AS property_count
FROM owner_units
WHERE status = 'active'
GROUP BY asset_stage
ORDER BY asset_stage;

SELECT service_type, COUNT(*) AS active_service_count
FROM owner_unit_services
WHERE status = 'active'
GROUP BY service_type
ORDER BY service_type;

SELECT 'operating_with_active_payment_plan' AS conflict, COUNT(*) AS conflict_count
FROM owner_units ou
JOIN purchase_contracts pc ON pc.owner_unit_id = ou.id
JOIN payment_plans pp ON pp.purchase_contract_id = pc.id
WHERE ou.asset_stage = 'OPERATING'
  AND pp.status = 'active'
UNION ALL
SELECT 'pre_handover_with_active_service', COUNT(*)
FROM owner_units ou
JOIN owner_unit_services ous ON ous.owner_unit_id = ou.id
WHERE ou.asset_stage = 'PRE_HANDOVER'
  AND ous.status = 'active'
UNION ALL
SELECT 'pre_handover_with_active_reserve', COUNT(*)
FROM owner_units ou
JOIN reserve_accounts ra ON ra.owner_unit_id = ou.id
WHERE ou.asset_stage = 'PRE_HANDOVER'
  AND ra.status = 'active';
