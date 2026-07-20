-- Comprehensive, realistic showcase data for owner / 123456.
-- Covers pre-handover payment states and operating rental/management/resale states.
-- Safe to re-run: stable business keys are used for every record.
USE ccps_property_management;
SET NAMES utf8mb4;
START TRANSACTION;

SET @owner_user_id = (SELECT id FROM users WHERE username = 'owner' AND account_type = 'OWNER' LIMIT 1);
SET @owner_id = (SELECT id FROM owners WHERE user_id = @owner_user_id LIMIT 1);
SET @admin_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1);

UPDATE users
SET display_name = 'Tan Wei Ming', phone = '+60123456789'
WHERE id = @owner_user_id;

UPDATE owners
SET full_name = 'Tan Wei Ming', identity_no = '900101-14-1234',
    phone = '+60123456789', email = 'weiming.tan@example.com', status = 'active'
WHERE id = @owner_id;

-- Projects and units ---------------------------------------------------------
INSERT INTO projects (project_code, name, address, city, country_code, status)
SELECT 'OWNER-DEMO-P01', 'Riverside Residence', '8 Jalan Ampang Hilir', 'Kuala Lumpur', 'MY', 'active'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE project_code = 'OWNER-DEMO-P01');
INSERT INTO projects (project_code, name, address, city, country_code, status)
SELECT 'OWNER-DEMO-P02', 'Meridian Park', '12 Jalan Teknokrat 6', 'Cyberjaya', 'MY', 'active'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE project_code = 'OWNER-DEMO-P02');
INSERT INTO projects (project_code, name, address, city, country_code, status)
SELECT 'OWNER-DEMO-P03', 'The Grove Suites', '25 Jalan PJU 8/8', 'Petaling Jaya', 'MY', 'active'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE project_code = 'OWNER-DEMO-P03');
INSERT INTO projects (project_code, name, address, city, country_code, status)
SELECT 'OWNER-DEMO-P04', 'Marina Crest', '3 Persiaran Bayan Indah', 'Penang', 'MY', 'active'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE project_code = 'OWNER-DEMO-P04');
INSERT INTO projects (project_code, name, address, city, country_code, status)
SELECT 'OWNER-DEMO-P05', 'KL Sentral Loft', '18 Jalan Stesen Sentral 5', 'Kuala Lumpur', 'MY', 'active'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE project_code = 'OWNER-DEMO-P05');
INSERT INTO projects (project_code, name, address, city, country_code, status)
SELECT 'OWNER-DEMO-P06', 'Mont Kiara Verde', '6 Jalan Kiara 3', 'Kuala Lumpur', 'MY', 'active'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE project_code = 'OWNER-DEMO-P06');
INSERT INTO projects (project_code, name, address, city, country_code, status)
SELECT 'OWNER-DEMO-P07', 'Bangsar South Suites', '2 Jalan Kerinchi', 'Kuala Lumpur', 'MY', 'active'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE project_code = 'OWNER-DEMO-P07');

SET @p1 = (SELECT id FROM projects WHERE project_code = 'OWNER-DEMO-P01');
SET @p2 = (SELECT id FROM projects WHERE project_code = 'OWNER-DEMO-P02');
SET @p3 = (SELECT id FROM projects WHERE project_code = 'OWNER-DEMO-P03');
SET @p4 = (SELECT id FROM projects WHERE project_code = 'OWNER-DEMO-P04');
SET @p5 = (SELECT id FROM projects WHERE project_code = 'OWNER-DEMO-P05');
SET @p6 = (SELECT id FROM projects WHERE project_code = 'OWNER-DEMO-P06');
SET @p7 = (SELECT id FROM projects WHERE project_code = 'OWNER-DEMO-P07');

-- Reuse the original owner unit as the first scenario, but replace its zero-value project metadata.
SET @original_owner_unit = (SELECT id FROM owner_units WHERE owner_id = @owner_id ORDER BY id LIMIT 1);
SET @original_unit = (SELECT unit_id FROM owner_units WHERE id = @original_owner_unit);
UPDATE units SET project_id = @p1, building = 'A', floor_no = '08', unit_no = 'A-08-01',
                 unit_type = '2 Bedroom', area_sqm = 82.50, bedroom_count = 2, listing_status = 'reserved'
WHERE id = @original_unit;

INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
SELECT @p2, 'B', '12', 'B-12-05', '3 Bedroom', 108.80, 3, 'reserved'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE project_id = @p2 AND unit_no = 'B-12-05');
INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
SELECT @p3, 'C', '05', 'C-05-08', '1+1 Bedroom', 61.20, 2, 'reserved'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE project_id = @p3 AND unit_no = 'C-05-08');
INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
SELECT @p4, 'D', '18', 'D-18-02', '3 Bedroom Sea View', 132.40, 3, 'sold'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE project_id = @p4 AND unit_no = 'D-18-02');
INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
SELECT @p5, 'E', '09', 'E-09-03', '1 Bedroom', 52.60, 1, 'rented'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE project_id = @p5 AND unit_no = 'E-09-03');
INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
SELECT @p6, 'F', '16', 'F-16-06', '3 Bedroom', 124.70, 3, 'rented'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE project_id = @p6 AND unit_no = 'F-16-06');
INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
SELECT @p7, 'G', '21', 'G-21-01', '2 Bedroom', 88.30, 2, 'available'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE project_id = @p7 AND unit_no = 'G-21-01');

SET @u1 = @original_unit;
SET @u2 = (SELECT id FROM units WHERE project_id = @p2 AND unit_no = 'B-12-05');
SET @u3 = (SELECT id FROM units WHERE project_id = @p3 AND unit_no = 'C-05-08');
SET @u4 = (SELECT id FROM units WHERE project_id = @p4 AND unit_no = 'D-18-02');
SET @u5 = (SELECT id FROM units WHERE project_id = @p5 AND unit_no = 'E-09-03');
SET @u6 = (SELECT id FROM units WHERE project_id = @p6 AND unit_no = 'F-16-06');
SET @u7 = (SELECT id FROM units WHERE project_id = @p7 AND unit_no = 'G-21-01');

INSERT INTO owner_units (owner_id, unit_id, ownership_percent, is_primary, start_date, asset_stage, expected_handover_date, status)
SELECT @owner_id, @u2, 100.00, 1, '2025-11-15', 'PRE_HANDOVER', DATE_ADD(CURRENT_DATE, INTERVAL 240 DAY), 'active'
WHERE NOT EXISTS (SELECT 1 FROM owner_units WHERE owner_id = @owner_id AND unit_id = @u2 AND status = 'active');
INSERT INTO owner_units (owner_id, unit_id, ownership_percent, is_primary, start_date, asset_stage, expected_handover_date, status)
SELECT @owner_id, @u3, 100.00, 1, '2026-02-20', 'PRE_HANDOVER', DATE_ADD(CURRENT_DATE, INTERVAL 330 DAY), 'active'
WHERE NOT EXISTS (SELECT 1 FROM owner_units WHERE owner_id = @owner_id AND unit_id = @u3 AND status = 'active');
INSERT INTO owner_units (owner_id, unit_id, ownership_percent, is_primary, start_date, asset_stage, expected_handover_date, status)
SELECT @owner_id, @u4, 100.00, 1, '2025-08-08', 'PRE_HANDOVER', DATE_ADD(CURRENT_DATE, INTERVAL 90 DAY), 'active'
WHERE NOT EXISTS (SELECT 1 FROM owner_units WHERE owner_id = @owner_id AND unit_id = @u4 AND status = 'active');
INSERT INTO owner_units (owner_id, unit_id, ownership_percent, is_primary, start_date, asset_stage, actual_handover_date, status)
SELECT @owner_id, @u5, 100.00, 1, '2024-06-18', 'OPERATING', '2025-01-20', 'active'
WHERE NOT EXISTS (SELECT 1 FROM owner_units WHERE owner_id = @owner_id AND unit_id = @u5 AND status = 'active');
INSERT INTO owner_units (owner_id, unit_id, ownership_percent, is_primary, start_date, asset_stage, actual_handover_date, status)
SELECT @owner_id, @u6, 100.00, 1, '2023-09-12', 'OPERATING', '2024-04-15', 'active'
WHERE NOT EXISTS (SELECT 1 FROM owner_units WHERE owner_id = @owner_id AND unit_id = @u6 AND status = 'active');
INSERT INTO owner_units (owner_id, unit_id, ownership_percent, is_primary, start_date, asset_stage, actual_handover_date, status)
SELECT @owner_id, @u7, 100.00, 1, '2022-05-06', 'OPERATING', '2023-01-10', 'active'
WHERE NOT EXISTS (SELECT 1 FROM owner_units WHERE owner_id = @owner_id AND unit_id = @u7 AND status = 'active');

SET @ou1 = (SELECT id FROM owner_units WHERE owner_id = @owner_id AND unit_id = @u1 AND status = 'active' LIMIT 1);
SET @ou2 = (SELECT id FROM owner_units WHERE owner_id = @owner_id AND unit_id = @u2 AND status = 'active' LIMIT 1);
SET @ou3 = (SELECT id FROM owner_units WHERE owner_id = @owner_id AND unit_id = @u3 AND status = 'active' LIMIT 1);
SET @ou4 = (SELECT id FROM owner_units WHERE owner_id = @owner_id AND unit_id = @u4 AND status = 'active' LIMIT 1);
SET @ou5 = (SELECT id FROM owner_units WHERE owner_id = @owner_id AND unit_id = @u5 AND status = 'active' LIMIT 1);
SET @ou6 = (SELECT id FROM owner_units WHERE owner_id = @owner_id AND unit_id = @u6 AND status = 'active' LIMIT 1);
SET @ou7 = (SELECT id FROM owner_units WHERE owner_id = @owner_id AND unit_id = @u7 AND status = 'active' LIMIT 1);

UPDATE owner_units SET asset_stage = 'PRE_HANDOVER', expected_handover_date = DATE_ADD(CURRENT_DATE, INTERVAL 300 DAY), actual_handover_date = NULL WHERE id = @ou1;
UPDATE owner_units SET asset_stage = 'PRE_HANDOVER', expected_handover_date = DATE_ADD(CURRENT_DATE, INTERVAL 240 DAY), actual_handover_date = NULL WHERE id = @ou2;
UPDATE owner_units SET asset_stage = 'PRE_HANDOVER', expected_handover_date = DATE_ADD(CURRENT_DATE, INTERVAL 330 DAY), actual_handover_date = NULL WHERE id = @ou3;
UPDATE owner_units SET asset_stage = 'PRE_HANDOVER', expected_handover_date = DATE_ADD(CURRENT_DATE, INTERVAL 90 DAY), actual_handover_date = NULL WHERE id = @ou4;
UPDATE owner_units SET asset_stage = 'OPERATING', actual_handover_date = '2025-01-20' WHERE id = @ou5;
UPDATE owner_units SET asset_stage = 'OPERATING', actual_handover_date = '2024-04-15' WHERE id = @ou6;
UPDATE owner_units SET asset_stage = 'OPERATING', actual_handover_date = '2023-01-10' WHERE id = @ou7;

-- Purchase contracts and four distinct pre-handover payment states ---------
INSERT INTO purchase_contracts (owner_unit_id, contract_no, purchase_price, currency, signed_date, handover_date, status)
SELECT @ou1, 'OWNER-DEMO-C01', 735000.00, 'MYR', '2025-10-18', DATE_ADD(CURRENT_DATE, INTERVAL 300 DAY), 'active'
WHERE NOT EXISTS (SELECT 1 FROM purchase_contracts WHERE contract_no = 'OWNER-DEMO-C01');
INSERT INTO purchase_contracts (owner_unit_id, contract_no, purchase_price, currency, signed_date, handover_date, status)
SELECT @ou2, 'OWNER-DEMO-C02', 920000.00, 'MYR', '2025-11-15', DATE_ADD(CURRENT_DATE, INTERVAL 240 DAY), 'active'
WHERE NOT EXISTS (SELECT 1 FROM purchase_contracts WHERE contract_no = 'OWNER-DEMO-C02');
INSERT INTO purchase_contracts (owner_unit_id, contract_no, purchase_price, currency, signed_date, handover_date, status)
SELECT @ou3, 'OWNER-DEMO-C03', 585000.00, 'MYR', '2026-02-20', DATE_ADD(CURRENT_DATE, INTERVAL 330 DAY), 'active'
WHERE NOT EXISTS (SELECT 1 FROM purchase_contracts WHERE contract_no = 'OWNER-DEMO-C03');
INSERT INTO purchase_contracts (owner_unit_id, contract_no, purchase_price, currency, signed_date, handover_date, status)
SELECT @ou4, 'OWNER-DEMO-C04', 1280000.00, 'MYR', '2025-08-08', DATE_ADD(CURRENT_DATE, INTERVAL 90 DAY), 'active'
WHERE NOT EXISTS (SELECT 1 FROM purchase_contracts WHERE contract_no = 'OWNER-DEMO-C04');
INSERT INTO purchase_contracts (owner_unit_id, contract_no, purchase_price, currency, signed_date, handover_date, status)
SELECT @ou5, 'OWNER-DEMO-C05', 680000.00, 'MYR', '2024-06-18', '2025-01-20', 'completed'
WHERE NOT EXISTS (SELECT 1 FROM purchase_contracts WHERE contract_no = 'OWNER-DEMO-C05');
INSERT INTO purchase_contracts (owner_unit_id, contract_no, purchase_price, currency, signed_date, handover_date, status)
SELECT @ou6, 'OWNER-DEMO-C06', 1150000.00, 'MYR', '2023-09-12', '2024-04-15', 'completed'
WHERE NOT EXISTS (SELECT 1 FROM purchase_contracts WHERE contract_no = 'OWNER-DEMO-C06');
INSERT INTO purchase_contracts (owner_unit_id, contract_no, purchase_price, currency, signed_date, handover_date, status)
SELECT @ou7, 'OWNER-DEMO-C07', 890000.00, 'MYR', '2022-05-06', '2023-01-10', 'completed'
WHERE NOT EXISTS (SELECT 1 FROM purchase_contracts WHERE contract_no = 'OWNER-DEMO-C07');

SET @c1 = (SELECT id FROM purchase_contracts WHERE contract_no = 'OWNER-DEMO-C01');
SET @c2 = (SELECT id FROM purchase_contracts WHERE contract_no = 'OWNER-DEMO-C02');
SET @c3 = (SELECT id FROM purchase_contracts WHERE contract_no = 'OWNER-DEMO-C03');
SET @c4 = (SELECT id FROM purchase_contracts WHERE contract_no = 'OWNER-DEMO-C04');

UPDATE purchase_contracts SET purchase_price = 735000.00, handover_date = DATE_ADD(CURRENT_DATE, INTERVAL 300 DAY), status = 'active' WHERE id = @c1;
UPDATE purchase_contracts SET purchase_price = 920000.00, handover_date = DATE_ADD(CURRENT_DATE, INTERVAL 240 DAY), status = 'active' WHERE id = @c2;
UPDATE purchase_contracts SET purchase_price = 585000.00, handover_date = DATE_ADD(CURRENT_DATE, INTERVAL 330 DAY), status = 'active' WHERE id = @c3;
UPDATE purchase_contracts SET purchase_price = 1280000.00, handover_date = DATE_ADD(CURRENT_DATE, INTERVAL 90 DAY), status = 'active' WHERE id = @c4;

INSERT INTO payment_plans (purchase_contract_id, plan_name, installment_count, total_amount, start_date, status)
SELECT @c1, 'Progressive Construction Plan', 5, 735000.00, '2025-10-18', 'active'
WHERE NOT EXISTS (SELECT 1 FROM payment_plans WHERE purchase_contract_id = @c1 AND status = 'active');
INSERT INTO payment_plans (purchase_contract_id, plan_name, installment_count, total_amount, start_date, status)
SELECT @c2, 'Standard Progressive Plan', 4, 920000.00, '2025-11-15', 'active'
WHERE NOT EXISTS (SELECT 1 FROM payment_plans WHERE purchase_contract_id = @c2 AND status = 'active');
INSERT INTO payment_plans (purchase_contract_id, plan_name, installment_count, total_amount, start_date, status)
SELECT @c3, 'Affordable Home Progressive Plan', 4, 585000.00, '2026-02-20', 'active'
WHERE NOT EXISTS (SELECT 1 FROM payment_plans WHERE purchase_contract_id = @c3 AND status = 'active');
INSERT INTO payment_plans (purchase_contract_id, plan_name, installment_count, total_amount, start_date, status)
SELECT @c4, 'Premium Residence Plan', 4, 1280000.00, '2025-08-08', 'active'
WHERE NOT EXISTS (SELECT 1 FROM payment_plans WHERE purchase_contract_id = @c4 AND status = 'active');

SET @plan1 = (SELECT id FROM payment_plans WHERE purchase_contract_id = @c1 AND status = 'active' LIMIT 1);
SET @plan2 = (SELECT id FROM payment_plans WHERE purchase_contract_id = @c2 AND status = 'active' LIMIT 1);
SET @plan3 = (SELECT id FROM payment_plans WHERE purchase_contract_id = @c3 AND status = 'active' LIMIT 1);
SET @plan4 = (SELECT id FROM payment_plans WHERE purchase_contract_id = @c4 AND status = 'active' LIMIT 1);

INSERT INTO payment_installments (payment_plan_id, installment_no, milestone, due_date, amount_due, amount_paid, status) VALUES
(@plan1, 1, 'Booking and SPA signing', DATE_SUB(CURRENT_DATE, INTERVAL 280 DAY), 73500.00, 73500.00, 'paid'),
(@plan1, 2, 'Foundation completed', DATE_SUB(CURRENT_DATE, INTERVAL 170 DAY), 147000.00, 147000.00, 'paid'),
(@plan1, 3, 'Structural frame completed', DATE_SUB(CURRENT_DATE, INTERVAL 45 DAY), 183750.00, 183750.00, 'paid'),
(@plan1, 4, 'Internal works completed', DATE_ADD(CURRENT_DATE, INTERVAL 60 DAY), 183750.00, 0.00, 'pending'),
(@plan1, 5, 'Vacant possession', DATE_ADD(CURRENT_DATE, INTERVAL 240 DAY), 147000.00, 0.00, 'pending'),
(@plan2, 1, 'Booking and down payment', DATE_SUB(CURRENT_DATE, INTERVAL 240 DAY), 184000.00, 184000.00, 'paid'),
(@plan2, 2, 'Foundation and podium', DATE_SUB(CURRENT_DATE, INTERVAL 100 DAY), 276000.00, 276000.00, 'paid'),
(@plan2, 3, 'Building structure', DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), 276000.00, 0.00, 'pending'),
(@plan2, 4, 'Handover balance', DATE_ADD(CURRENT_DATE, INTERVAL 205 DAY), 184000.00, 0.00, 'pending'),
(@plan3, 1, 'Booking deposit', DATE_SUB(CURRENT_DATE, INTERVAL 140 DAY), 58500.00, 58500.00, 'paid'),
(@plan3, 2, 'SPA progressive claim', DATE_SUB(CURRENT_DATE, INTERVAL 18 DAY), 117000.00, 45000.00, 'overdue'),
(@plan3, 3, 'Structure and roofing', DATE_ADD(CURRENT_DATE, INTERVAL 110 DAY), 175500.00, 0.00, 'pending'),
(@plan3, 4, 'Vacant possession', DATE_ADD(CURRENT_DATE, INTERVAL 300 DAY), 234000.00, 0.00, 'pending'),
(@plan4, 1, 'Booking and SPA', DATE_SUB(CURRENT_DATE, INTERVAL 320 DAY), 128000.00, 128000.00, 'paid'),
(@plan4, 2, 'Foundation works', DATE_SUB(CURRENT_DATE, INTERVAL 220 DAY), 384000.00, 384000.00, 'paid'),
(@plan4, 3, 'Structure completed', DATE_SUB(CURRENT_DATE, INTERVAL 100 DAY), 384000.00, 384000.00, 'paid'),
(@plan4, 4, 'Final payment before handover', DATE_SUB(CURRENT_DATE, INTERVAL 20 DAY), 384000.00, 384000.00, 'paid')
ON DUPLICATE KEY UPDATE milestone = VALUES(milestone), due_date = VALUES(due_date),
  amount_due = VALUES(amount_due), amount_paid = VALUES(amount_paid), status = VALUES(status);

-- Operating services --------------------------------------------------------
INSERT INTO owner_unit_services (owner_unit_id, service_type, status, started_at) VALUES
(@ou5, 'RENTAL', 'active', '2025-02-01'),
(@ou5, 'MANAGEMENT', 'active', '2025-01-20'),
(@ou6, 'RENTAL', 'active', '2024-05-01'),
(@ou6, 'MANAGEMENT', 'active', '2024-04-15'),
(@ou7, 'RESALE', 'active', DATE_SUB(CURRENT_DATE, INTERVAL 45 DAY)),
(@ou7, 'MANAGEMENT', 'active', '2023-01-10')
ON DUPLICATE KEY UPDATE status = 'active', ended_at = NULL, started_at = VALUES(started_at);

INSERT INTO tenants (full_name, identity_no, phone, email, status)
SELECT 'Nur Aisyah Binti Rahman', '920415-10-5188', '+60176543210', 'aisyah.rahman@example.com', 'active'
WHERE NOT EXISTS (SELECT 1 FROM tenants WHERE email = 'aisyah.rahman@example.com');
INSERT INTO tenants (full_name, identity_no, phone, email, status)
SELECT 'Daniel Lee Jian Wei', '880722-14-6023', '+60129876543', 'daniel.lee@example.com', 'active'
WHERE NOT EXISTS (SELECT 1 FROM tenants WHERE email = 'daniel.lee@example.com');
SET @tenant1 = (SELECT id FROM tenants WHERE email = 'aisyah.rahman@example.com');
SET @tenant2 = (SELECT id FROM tenants WHERE email = 'daniel.lee@example.com');

INSERT INTO leases (unit_id, tenant_id, lease_no, start_date, end_date, monthly_rent, deposit_amount, payment_day, status)
SELECT @u5, @tenant1, 'OWNER-DEMO-L01', DATE_SUB(CURRENT_DATE, INTERVAL 138 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 227 DAY), 2600.00, 5200.00, 3, 'active'
WHERE NOT EXISTS (SELECT 1 FROM leases WHERE lease_no = 'OWNER-DEMO-L01');
INSERT INTO leases (unit_id, tenant_id, lease_no, start_date, end_date, monthly_rent, deposit_amount, payment_day, status)
SELECT @u6, @tenant2, 'OWNER-DEMO-L02', DATE_SUB(CURRENT_DATE, INTERVAL 320 DAY), DATE_ADD(CURRENT_DATE, INTERVAL 45 DAY), 4200.00, 8400.00, 5, 'active'
WHERE NOT EXISTS (SELECT 1 FROM leases WHERE lease_no = 'OWNER-DEMO-L02');
SET @lease1 = (SELECT id FROM leases WHERE lease_no = 'OWNER-DEMO-L01');
SET @lease2 = (SELECT id FROM leases WHERE lease_no = 'OWNER-DEMO-L02');
UPDATE leases SET end_date = DATE_ADD(CURRENT_DATE, INTERVAL 227 DAY), monthly_rent = 2600.00, deposit_amount = 5200.00, status = 'active' WHERE id = @lease1;
UPDATE leases SET end_date = DATE_ADD(CURRENT_DATE, INTERVAL 45 DAY), monthly_rent = 4200.00, deposit_amount = 8400.00, status = 'active' WHERE id = @lease2;

INSERT INTO rent_invoices (lease_id, billing_month, due_date, amount_due, amount_paid, status) VALUES
(@lease1, DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 2 DAY), 2600.00, 2600.00, 'paid'),
(@lease2, DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 4 DAY), 4200.00, 1600.00, 'partial')
ON DUPLICATE KEY UPDATE due_date = VALUES(due_date), amount_due = VALUES(amount_due), amount_paid = VALUES(amount_paid), status = VALUES(status);
SET @invoice1 = (SELECT id FROM rent_invoices WHERE lease_id = @lease1 AND billing_month = DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'));
SET @invoice2 = (SELECT id FROM rent_invoices WHERE lease_id = @lease2 AND billing_month = DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'));

INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, tenant_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status)
SELECT 'OWNER-DEMO-F-RENT-01', 'rent_payment', @u5, @owner_id, @tenant1, 2600.00, 'MYR', DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 2 DAY), 'bank_transfer', 'paid', 'confirmed', 'synced'
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'OWNER-DEMO-F-RENT-01');
INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, tenant_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status)
SELECT 'OWNER-DEMO-F-RENT-02', 'rent_payment', @u6, @owner_id, @tenant2, 1600.00, 'MYR', DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 7 DAY), 'online_transfer', 'partial', 'pending', 'not_synced'
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'OWNER-DEMO-F-RENT-02');
SET @rent_finance1 = (SELECT id FROM finance_records WHERE transaction_no = 'OWNER-DEMO-F-RENT-01');
SET @rent_finance2 = (SELECT id FROM finance_records WHERE transaction_no = 'OWNER-DEMO-F-RENT-02');
UPDATE finance_records SET amount = 2600.00, transaction_date = DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 2 DAY), payment_status = 'paid', confirmation_status = 'confirmed' WHERE id = @rent_finance1;
UPDATE finance_records SET amount = 1600.00, transaction_date = DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 7 DAY), payment_status = 'partial', confirmation_status = 'pending' WHERE id = @rent_finance2;
INSERT IGNORE INTO rent_payments (rent_invoice_id, finance_record_id) VALUES (@invoice1, @rent_finance1), (@invoice2, @rent_finance2);

-- Reserve balances: healthy, low and healthy-resale scenarios.
INSERT INTO reserve_accounts (owner_unit_id, minimum_balance, current_balance, status, low_balance_alert_enabled) VALUES
(@ou5, 2500.00, 7200.00, 'active', 1),
(@ou6, 4000.00, 1850.00, 'active', 1),
(@ou7, 3000.00, 5600.00, 'active', 1)
ON DUPLICATE KEY UPDATE minimum_balance = VALUES(minimum_balance), current_balance = VALUES(current_balance), status = 'active', low_balance_alert_enabled = 1;
SET @ra5 = (SELECT id FROM reserve_accounts WHERE owner_unit_id = @ou5);
SET @ra6 = (SELECT id FROM reserve_accounts WHERE owner_unit_id = @ou6);
SET @ra7 = (SELECT id FROM reserve_accounts WHERE owner_unit_id = @ou7);

INSERT INTO reserve_transactions (reserve_account_id, transaction_type, amount, occurred_at, balance_after, note, created_by)
SELECT @ra5, 'topup', 8000.00, DATE_SUB(NOW(), INTERVAL 150 DAY), 8000.00, 'Opening reserve funding', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM reserve_transactions WHERE reserve_account_id = @ra5 AND note = 'Opening reserve funding');
INSERT INTO reserve_transactions (reserve_account_id, transaction_type, amount, occurred_at, balance_after, note, created_by)
SELECT @ra5, 'debit', 800.00, DATE_SUB(NOW(), INTERVAL 20 DAY), 7200.00, 'Air-conditioning service and management charges', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM reserve_transactions WHERE reserve_account_id = @ra5 AND note = 'Air-conditioning service and management charges');
INSERT INTO reserve_transactions (reserve_account_id, transaction_type, amount, occurred_at, balance_after, note, created_by)
SELECT @ra6, 'topup', 5500.00, DATE_SUB(NOW(), INTERVAL 210 DAY), 5500.00, 'Opening reserve funding', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM reserve_transactions WHERE reserve_account_id = @ra6 AND note = 'Opening reserve funding');
INSERT INTO reserve_transactions (reserve_account_id, transaction_type, amount, occurred_at, balance_after, note, created_by)
SELECT @ra6, 'debit', 3650.00, DATE_SUB(NOW(), INTERVAL 12 DAY), 1850.00, 'Plumbing repair and sinking fund charges', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM reserve_transactions WHERE reserve_account_id = @ra6 AND note = 'Plumbing repair and sinking fund charges');
INSERT INTO reserve_transactions (reserve_account_id, transaction_type, amount, occurred_at, balance_after, note, created_by)
SELECT @ra7, 'topup', 6500.00, DATE_SUB(NOW(), INTERVAL 100 DAY), 6500.00, 'Opening reserve funding', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM reserve_transactions WHERE reserve_account_id = @ra7 AND note = 'Opening reserve funding');
INSERT INTO reserve_transactions (reserve_account_id, transaction_type, amount, occurred_at, balance_after, note, created_by)
SELECT @ra7, 'debit', 900.00, DATE_SUB(NOW(), INTERVAL 15 DAY), 5600.00, 'Pre-sale cleaning and minor touch-up', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM reserve_transactions WHERE reserve_account_id = @ra7 AND note = 'Pre-sale cleaning and minor touch-up');

-- Monthly income, expenses and maintenance ---------------------------------
INSERT INTO vendors (vendor_code, name, contact_name, phone, email, status)
SELECT 'OWNER-DEMO-V01', 'UrbanCare Property Services', 'Service Desk', '+60327881288', 'service@urbancare.example.com', 'active'
WHERE NOT EXISTS (SELECT 1 FROM vendors WHERE vendor_code = 'OWNER-DEMO-V01');
INSERT INTO vendors (vendor_code, name, contact_name, phone, email, status)
SELECT 'OWNER-DEMO-V02', 'AquaFix Plumbing Solutions', 'Jason Lim', '+60163384722', 'jason@aquafix.example.com', 'active'
WHERE NOT EXISTS (SELECT 1 FROM vendors WHERE vendor_code = 'OWNER-DEMO-V02');
SET @vendor1 = (SELECT id FROM vendors WHERE vendor_code = 'OWNER-DEMO-V01');
SET @vendor2 = (SELECT id FROM vendors WHERE vendor_code = 'OWNER-DEMO-V02');

INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status) VALUES
('OWNER-DEMO-F-EXP-01', 'cashflow', @u5, @owner_id, 480.00, 'MYR', DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 5 DAY), 'reserve_account', 'paid', 'confirmed', 'synced'),
('OWNER-DEMO-F-EXP-02', 'cashflow', @u6, @owner_id, 1250.00, 'MYR', DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 9 DAY), 'reserve_account', 'paid', 'confirmed', 'synced'),
('OWNER-DEMO-F-EXP-03', 'cashflow', @u7, @owner_id, 680.00, 'MYR', DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 6 DAY), 'reserve_account', 'paid', 'confirmed', 'synced')
ON DUPLICATE KEY UPDATE amount = VALUES(amount), transaction_date = VALUES(transaction_date), payment_method = VALUES(payment_method), payment_status = VALUES(payment_status), confirmation_status = VALUES(confirmation_status);
SET @expense_finance1 = (SELECT id FROM finance_records WHERE transaction_no = 'OWNER-DEMO-F-EXP-01');
SET @expense_finance2 = (SELECT id FROM finance_records WHERE transaction_no = 'OWNER-DEMO-F-EXP-02');
SET @expense_finance3 = (SELECT id FROM finance_records WHERE transaction_no = 'OWNER-DEMO-F-EXP-03');

INSERT INTO cashflow_entries (finance_record_id, unit_id, owner_id, tenant_id, direction, category, description, occurred_on, reserve_account_id, attachment_status) VALUES
(@rent_finance1, @u5, @owner_id, @tenant1, 'income', 'rent', 'Monthly rental received for KL Sentral Loft E-09-03', DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 2 DAY), NULL, 'complete'),
(@rent_finance2, @u6, @owner_id, @tenant2, 'income', 'rent', 'Partial monthly rental received for Mont Kiara Verde F-16-06', DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 7 DAY), NULL, 'complete'),
(@expense_finance1, @u5, @owner_id, NULL, 'expense', 'management', 'Monthly management fee and common-area charges', DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 5 DAY), @ra5, 'not_required'),
(@expense_finance2, @u6, @owner_id, NULL, 'expense', 'maintenance', 'Plumbing inspection and emergency repair deposit', DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 9 DAY), @ra6, 'not_required'),
(@expense_finance3, @u7, @owner_id, NULL, 'expense', 'management', 'Management fee and pre-sale property preparation', DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 6 DAY), @ra7, 'not_required')
ON DUPLICATE KEY UPDATE description = VALUES(description), occurred_on = VALUES(occurred_on), reserve_account_id = VALUES(reserve_account_id), attachment_status = VALUES(attachment_status);
SET @expense_cashflow2 = (SELECT id FROM cashflow_entries WHERE finance_record_id = @expense_finance2);

INSERT INTO maintenance_work_orders (work_order_no, unit_id, owner_id, tenant_id, vendor_id, cashflow_entry_id, category, title, description, requested_at, completed_at, status, estimated_amount, actual_amount, created_by)
SELECT 'OWNER-DEMO-M01', @u5, @owner_id, @tenant1, @vendor1, NULL, 'air_conditioning', 'Living room air-conditioner servicing', 'Routine chemical cleaning and refrigerant pressure inspection.', DATE_SUB(NOW(), INTERVAL 38 DAY), DATE_SUB(NOW(), INTERVAL 35 DAY), 'completed', 380.00, 350.00, @admin_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_work_orders WHERE work_order_no = 'OWNER-DEMO-M01');
INSERT INTO maintenance_work_orders (work_order_no, unit_id, owner_id, tenant_id, vendor_id, cashflow_entry_id, category, title, description, requested_at, completed_at, status, estimated_amount, actual_amount, created_by)
SELECT 'OWNER-DEMO-M02', @u6, @owner_id, @tenant2, @vendor2, @expense_cashflow2, 'plumbing', 'Master bathroom concealed pipe leakage', 'Moisture detected behind the vanity wall. Pressure test and pipe replacement required.', DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, 'in_progress', 1850.00, NULL, @admin_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_work_orders WHERE work_order_no = 'OWNER-DEMO-M02');
INSERT INTO maintenance_work_orders (work_order_no, unit_id, owner_id, tenant_id, vendor_id, cashflow_entry_id, category, title, description, requested_at, completed_at, status, estimated_amount, actual_amount, created_by)
SELECT 'OWNER-DEMO-M03', @u7, @owner_id, NULL, @vendor1, NULL, 'painting', 'Touch-up painting before property viewing', 'Repair minor wall marks and repaint the living room feature wall before listing photography.', DATE_SUB(NOW(), INTERVAL 3 DAY), NULL, 'open', 980.00, NULL, @admin_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_work_orders WHERE work_order_no = 'OWNER-DEMO-M03');
SET @m2 = (SELECT id FROM maintenance_work_orders WHERE work_order_no = 'OWNER-DEMO-M02');
SET @m3 = (SELECT id FROM maintenance_work_orders WHERE work_order_no = 'OWNER-DEMO-M03');
INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by)
SELECT @m2, 'open', DATE_SUB(NOW(), INTERVAL 6 DAY), 'Leakage report received from tenant.', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_status_history WHERE work_order_id = @m2 AND status = 'open');
INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by)
SELECT @m2, 'in_progress', DATE_SUB(NOW(), INTERVAL 2 DAY), 'Vendor appointed and repair visit scheduled.', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_status_history WHERE work_order_id = @m2 AND status = 'in_progress');
INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by)
SELECT @m3, 'open', DATE_SUB(NOW(), INTERVAL 3 DAY), 'Work order raised for resale preparation.', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_status_history WHERE work_order_id = @m3 AND status = 'open');

-- Current confirmed expenses are funded by reserves; no owner payment proof is required.
INSERT INTO reserve_transactions
  (reserve_account_id, finance_record_id, transaction_type, amount, occurred_at,
   balance_after, note, created_by)
SELECT @ra5, @expense_finance1, 'debit', 480.00,
       DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 5 DAY), 6720.00,
       'Monthly management fee由預備金自動扣除', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM reserve_transactions WHERE finance_record_id = @expense_finance1 AND transaction_type = 'debit');
INSERT INTO reserve_transactions
  (reserve_account_id, finance_record_id, maintenance_work_order_id, transaction_type, amount,
   occurred_at, balance_after, note, created_by)
SELECT @ra6, @expense_finance2, @m2, 'debit', 1250.00,
       DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 9 DAY), 600.00,
       'Plumbing repair由預備金自動扣除', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM reserve_transactions WHERE finance_record_id = @expense_finance2 AND transaction_type = 'debit');
INSERT INTO reserve_transactions
  (reserve_account_id, finance_record_id, transaction_type, amount, occurred_at,
   balance_after, note, created_by)
SELECT @ra7, @expense_finance3, 'debit', 680.00,
       DATE_ADD(DATE_FORMAT(CURRENT_DATE, '%Y-%m-01'), INTERVAL 6 DAY), 4920.00,
       'Management fee由預備金自動扣除', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM reserve_transactions WHERE finance_record_id = @expense_finance3 AND transaction_type = 'debit');
UPDATE reserve_accounts SET current_balance = 6720.00 WHERE id = @ra5;
UPDATE reserve_accounts SET current_balance = 600.00 WHERE id = @ra6;
UPDATE reserve_accounts SET current_balance = 4920.00 WHERE id = @ra7;

-- Payment proof coverage and owner notifications ----------------------------
INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status)
SELECT 'OWNER-DEMO-F-PAY-01', 'property_payment', @u1, @owner_id, 183750.00, 'MYR', DATE_SUB(CURRENT_DATE, INTERVAL 45 DAY), 'bank_transfer', 'paid', 'confirmed', 'synced'
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'OWNER-DEMO-F-PAY-01');
INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status)
SELECT 'OWNER-DEMO-F-PAY-02', 'property_payment', @u3, @owner_id, 45000.00, 'MYR', DATE_SUB(CURRENT_DATE, INTERVAL 14 DAY), 'online_transfer', 'partial', 'pending', 'not_synced'
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'OWNER-DEMO-F-PAY-02');

INSERT INTO notifications (recipient_user_id, recipient_owner_id, title, body, related_type, related_id, priority, status, created_at)
SELECT @owner_user_id, @owner_id, 'Meridian Park 房款即將到期', CONCAT('B-12-05 建築結構款 RM 276,000.00 將於 ', DATE_FORMAT(DATE_ADD(CURRENT_DATE, INTERVAL 12 DAY), '%Y-%m-%d'), ' 到期。'), 'owner_unit', @ou2, 'high', 'unread', DATE_SUB(NOW(), INTERVAL 2 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_owner_id = @owner_id AND title = 'Meridian Park 房款即將到期');
INSERT INTO notifications (recipient_user_id, recipient_owner_id, title, body, related_type, related_id, priority, status, created_at)
SELECT @owner_user_id, @owner_id, 'Mont Kiara Verde 租金尚未收齊', 'F-16-06 本月租金應收 RM 4,200.00，目前已收 RM 1,600.00，尚欠 RM 2,600.00。', 'lease', @lease2, 'urgent', 'unread', DATE_SUB(NOW(), INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_owner_id = @owner_id AND title = 'Mont Kiara Verde 租金尚未收齊');
INSERT INTO notifications (recipient_user_id, recipient_owner_id, title, body, related_type, related_id, priority, status, created_at)
SELECT @owner_user_id, @owner_id, '預備金低於最低標準', 'Mont Kiara Verde F-16-06 預備金餘額 RM 1,850.00，低於最低標準 RM 4,000.00。', 'owner_unit', @ou6, 'high', 'unread', DATE_SUB(NOW(), INTERVAL 2 DAY)
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_owner_id = @owner_id AND title = '預備金低於最低標準');
INSERT INTO notifications (recipient_user_id, recipient_owner_id, title, body, related_type, related_id, priority, status, created_at)
SELECT @owner_user_id, @owner_id, '維修工單處理中', 'F-16-06 主浴室暗管滲漏已安排 AquaFix Plumbing Solutions 進場處理。', 'work_order', @m2, 'normal', 'read', DATE_SUB(NOW(), INTERVAL 3 DAY)
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_owner_id = @owner_id AND title = '維修工單處理中');

COMMIT;
