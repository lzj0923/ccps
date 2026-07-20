-- Test data owned by the admin user. Safe to re-run.
USE ccps_property_management;
SET NAMES utf8mb4;
START TRANSACTION;

SET @admin_id = (SELECT id FROM users WHERE username = 'admin' LIMIT 1);
SET @portfolio_owner_user_id = (SELECT id FROM users WHERE username = 'owner3' LIMIT 1);

INSERT INTO roles (code, name)
SELECT 'ADMIN', 'System Administrator'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ADMIN');
SET @admin_role_id = (SELECT id FROM roles WHERE code = 'ADMIN' LIMIT 1);
INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (@admin_id, @admin_role_id);

INSERT INTO owners (user_id, full_name, email, phone, status)
SELECT @portfolio_owner_user_id, 'ADMIN Test Portfolio Owner', 'admin.test.owner@example.com', '+60110000999', 'active'
WHERE NOT EXISTS (SELECT 1 FROM owners WHERE email = 'admin.test.owner@example.com');
UPDATE owners
SET user_id = @portfolio_owner_user_id
WHERE email = 'admin.test.owner@example.com' AND @portfolio_owner_user_id IS NOT NULL;
SET @admin_owner_id = (SELECT id FROM owners WHERE email = 'admin.test.owner@example.com' LIMIT 1);

INSERT INTO projects (project_code, name, address, city, country_code, status)
SELECT 'ADMIN-TEST-20260715-P01', 'ADMIN Test Lakeview Towers', '10 Admin Test Lakeview Road', 'Kuala Lumpur', 'MY', 'active'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE project_code = 'ADMIN-TEST-20260715-P01');
INSERT INTO projects (project_code, name, address, city, country_code, status)
SELECT 'ADMIN-TEST-20260715-P02', 'ADMIN Test Central Suites', '20 Admin Test Central Street', 'Petaling Jaya', 'MY', 'active'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE project_code = 'ADMIN-TEST-20260715-P02');
SET @admin_project_1 = (SELECT id FROM projects WHERE project_code = 'ADMIN-TEST-20260715-P01' LIMIT 1);
SET @admin_project_2 = (SELECT id FROM projects WHERE project_code = 'ADMIN-TEST-20260715-P02' LIMIT 1);

INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
SELECT @admin_project_1, 'A', '03', 'ADMIN-A-0301', '2BR', 75.50, 2, 'available'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE project_id = @admin_project_1 AND unit_no = 'ADMIN-A-0301');
INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
SELECT @admin_project_1, 'A', '09', 'ADMIN-A-0906', '3BR', 110.00, 3, 'occupied'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE project_id = @admin_project_1 AND unit_no = 'ADMIN-A-0906');
INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
SELECT @admin_project_2, 'B', '06', 'ADMIN-B-0602', '1BR', 48.00, 1, 'reserved'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE project_id = @admin_project_2 AND unit_no = 'ADMIN-B-0602');
INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
SELECT @admin_project_2, 'B', '15', 'ADMIN-B-1508', '2BR', 84.25, 2, 'available'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE project_id = @admin_project_2 AND unit_no = 'ADMIN-B-1508');
SET @admin_unit_1 = (SELECT id FROM units WHERE project_id = @admin_project_1 AND unit_no = 'ADMIN-A-0301' LIMIT 1);
SET @admin_unit_2 = (SELECT id FROM units WHERE project_id = @admin_project_1 AND unit_no = 'ADMIN-A-0906' LIMIT 1);
SET @admin_unit_3 = (SELECT id FROM units WHERE project_id = @admin_project_2 AND unit_no = 'ADMIN-B-0602' LIMIT 1);
SET @admin_unit_4 = (SELECT id FROM units WHERE project_id = @admin_project_2 AND unit_no = 'ADMIN-B-1508' LIMIT 1);

INSERT INTO owner_units (owner_id, unit_id, ownership_percent, is_primary, start_date, status)
SELECT @admin_owner_id, @admin_unit_1, 100.00, 1, '2026-01-01', 'active'
WHERE NOT EXISTS (SELECT 1 FROM owner_units WHERE owner_id = @admin_owner_id AND unit_id = @admin_unit_1);
INSERT INTO owner_units (owner_id, unit_id, ownership_percent, is_primary, start_date, status)
SELECT @admin_owner_id, @admin_unit_2, 100.00, 1, '2026-01-01', 'active'
WHERE NOT EXISTS (SELECT 1 FROM owner_units WHERE owner_id = @admin_owner_id AND unit_id = @admin_unit_2);
INSERT INTO owner_units (owner_id, unit_id, ownership_percent, is_primary, start_date, status)
SELECT @admin_owner_id, @admin_unit_3, 100.00, 1, '2026-02-01', 'active'
WHERE NOT EXISTS (SELECT 1 FROM owner_units WHERE owner_id = @admin_owner_id AND unit_id = @admin_unit_3);
INSERT INTO owner_units (owner_id, unit_id, ownership_percent, is_primary, start_date, status)
SELECT @admin_owner_id, @admin_unit_4, 100.00, 1, '2026-02-01', 'active'
WHERE NOT EXISTS (SELECT 1 FROM owner_units WHERE owner_id = @admin_owner_id AND unit_id = @admin_unit_4);
SET @admin_owner_unit_1 = (SELECT id FROM owner_units WHERE owner_id = @admin_owner_id AND unit_id = @admin_unit_1 AND status = 'active' LIMIT 1);
SET @admin_owner_unit_2 = (SELECT id FROM owner_units WHERE owner_id = @admin_owner_id AND unit_id = @admin_unit_2 AND status = 'active' LIMIT 1);
SET @admin_owner_unit_3 = (SELECT id FROM owner_units WHERE owner_id = @admin_owner_id AND unit_id = @admin_unit_3 AND status = 'active' LIMIT 1);
SET @admin_owner_unit_4 = (SELECT id FROM owner_units WHERE owner_id = @admin_owner_id AND unit_id = @admin_unit_4 AND status = 'active' LIMIT 1);

INSERT INTO tenants (full_name, phone, email, status)
SELECT 'ADMIN Test Tenant One', '+60110000901', 'admin.test.tenant1@example.com', 'active'
WHERE NOT EXISTS (SELECT 1 FROM tenants WHERE email = 'admin.test.tenant1@example.com');
INSERT INTO tenants (full_name, phone, email, status)
SELECT 'ADMIN Test Tenant Two', '+60110000902', 'admin.test.tenant2@example.com', 'active'
WHERE NOT EXISTS (SELECT 1 FROM tenants WHERE email = 'admin.test.tenant2@example.com');
SET @admin_tenant_1 = (SELECT id FROM tenants WHERE email = 'admin.test.tenant1@example.com' LIMIT 1);
SET @admin_tenant_2 = (SELECT id FROM tenants WHERE email = 'admin.test.tenant2@example.com' LIMIT 1);

INSERT INTO leases (unit_id, tenant_id, lease_no, start_date, end_date, monthly_rent, deposit_amount, payment_day, status)
SELECT @admin_unit_2, @admin_tenant_1, 'ADMIN-TEST-20260715-L01', '2026-03-01', '2027-02-28', 3200.00, 6400.00, 5, 'active'
WHERE NOT EXISTS (SELECT 1 FROM leases WHERE lease_no = 'ADMIN-TEST-20260715-L01');
INSERT INTO leases (unit_id, tenant_id, lease_no, start_date, end_date, monthly_rent, deposit_amount, payment_day, status)
SELECT @admin_unit_3, @admin_tenant_2, 'ADMIN-TEST-20260715-L02', '2026-04-01', '2027-03-31', 2100.00, 4200.00, 1, 'active'
WHERE NOT EXISTS (SELECT 1 FROM leases WHERE lease_no = 'ADMIN-TEST-20260715-L02');
SET @admin_lease_1 = (SELECT id FROM leases WHERE lease_no = 'ADMIN-TEST-20260715-L01' LIMIT 1);
SET @admin_lease_2 = (SELECT id FROM leases WHERE lease_no = 'ADMIN-TEST-20260715-L02' LIMIT 1);

INSERT INTO rent_invoices (lease_id, billing_month, due_date, amount_due, amount_paid, status)
SELECT @admin_lease_1, '2026-06-01', '2026-06-05', 3200.00, 3200.00, 'paid'
WHERE NOT EXISTS (SELECT 1 FROM rent_invoices WHERE lease_id = @admin_lease_1 AND billing_month = '2026-06-01');
INSERT INTO rent_invoices (lease_id, billing_month, due_date, amount_due, amount_paid, status)
SELECT @admin_lease_1, '2026-07-01', '2026-07-05', 3200.00, 0.00, 'overdue'
WHERE NOT EXISTS (SELECT 1 FROM rent_invoices WHERE lease_id = @admin_lease_1 AND billing_month = '2026-07-01');
INSERT INTO rent_invoices (lease_id, billing_month, due_date, amount_due, amount_paid, status)
SELECT @admin_lease_2, '2026-07-01', '2026-07-01', 2100.00, 2100.00, 'paid'
WHERE NOT EXISTS (SELECT 1 FROM rent_invoices WHERE lease_id = @admin_lease_2 AND billing_month = '2026-07-01');

INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, tenant_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status)
SELECT 'ADMIN-TEST-20260715-F01', 'rent_payment', @admin_unit_2, @admin_owner_id, @admin_tenant_1, 3200.00, 'MYR', '2026-06-03', 'bank_transfer', 'paid', 'confirmed', 'not_synced'
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260715-F01');
INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, tenant_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status)
SELECT 'ADMIN-TEST-20260715-F02', 'rent_payment', @admin_unit_3, @admin_owner_id, @admin_tenant_2, 2100.00, 'MYR', '2026-07-01', 'online_payment', 'paid', 'confirmed', 'not_synced'
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260715-F02');

INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status)
SELECT 'ADMIN-TEST-20260715-F03', 'property_payment', @admin_unit_1, @admin_owner_id, 162500.00, 'MYR', '2026-07-08', 'bank_transfer', 'paid', 'pending', 'not_synced'
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260715-F03');
INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status)
SELECT 'ADMIN-TEST-20260715-F04', 'property_payment', @admin_unit_3, @admin_owner_id, 120000.00, 'MYR', '2026-07-10', 'online_payment', 'paid', 'pending', 'not_synced'
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260715-F04');
INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, tenant_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status)
SELECT 'ADMIN-TEST-20260715-F05', 'rent_payment', @admin_unit_2, @admin_owner_id, @admin_tenant_1, 3200.00, 'MYR', '2026-07-12', 'bank_transfer', 'paid', 'pending', 'not_synced'
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260715-F05');

SET @admin_invoice_1_june = (SELECT id FROM rent_invoices WHERE lease_id = @admin_lease_1 AND billing_month = '2026-06-01' LIMIT 1);
SET @admin_invoice_1_july = (SELECT id FROM rent_invoices WHERE lease_id = @admin_lease_1 AND billing_month = '2026-07-01' LIMIT 1);
SET @admin_invoice_2_july = (SELECT id FROM rent_invoices WHERE lease_id = @admin_lease_2 AND billing_month = '2026-07-01' LIMIT 1);
SET @admin_finance_1 = (SELECT id FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260715-F01' LIMIT 1);
SET @admin_finance_2 = (SELECT id FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260715-F02' LIMIT 1);
SET @admin_finance_5 = (SELECT id FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260715-F05' LIMIT 1);

INSERT INTO rent_payments (rent_invoice_id, finance_record_id)
SELECT @admin_invoice_1_june, @admin_finance_1
WHERE NOT EXISTS (SELECT 1 FROM rent_payments WHERE finance_record_id = @admin_finance_1);
INSERT INTO rent_payments (rent_invoice_id, finance_record_id)
SELECT @admin_invoice_2_july, @admin_finance_2
WHERE NOT EXISTS (SELECT 1 FROM rent_payments WHERE finance_record_id = @admin_finance_2);
INSERT INTO rent_payments (rent_invoice_id, finance_record_id)
SELECT @admin_invoice_1_july, @admin_finance_5
WHERE NOT EXISTS (SELECT 1 FROM rent_payments WHERE finance_record_id = @admin_finance_5);

INSERT INTO purchase_contracts (owner_unit_id, contract_no, purchase_price, currency, signed_date, status)
SELECT @admin_owner_unit_1, 'ADMIN-TEST-20260715-C01', 650000.00, 'MYR', '2026-01-01', 'active'
WHERE NOT EXISTS (SELECT 1 FROM purchase_contracts WHERE contract_no = 'ADMIN-TEST-20260715-C01');
INSERT INTO purchase_contracts (owner_unit_id, contract_no, purchase_price, currency, signed_date, status)
SELECT @admin_owner_unit_2, 'ADMIN-TEST-20260715-C02', 980000.00, 'MYR', '2026-01-01', 'active'
WHERE NOT EXISTS (SELECT 1 FROM purchase_contracts WHERE contract_no = 'ADMIN-TEST-20260715-C02');
INSERT INTO purchase_contracts (owner_unit_id, contract_no, purchase_price, currency, signed_date, status)
SELECT @admin_owner_unit_3, 'ADMIN-TEST-20260715-C03', 480000.00, 'MYR', '2026-02-01', 'active'
WHERE NOT EXISTS (SELECT 1 FROM purchase_contracts WHERE contract_no = 'ADMIN-TEST-20260715-C03');
INSERT INTO purchase_contracts (owner_unit_id, contract_no, purchase_price, currency, signed_date, status)
SELECT @admin_owner_unit_4, 'ADMIN-TEST-20260715-C04', 710000.00, 'MYR', '2026-02-01', 'active'
WHERE NOT EXISTS (SELECT 1 FROM purchase_contracts WHERE contract_no = 'ADMIN-TEST-20260715-C04');

SET @admin_contract_1 = (SELECT id FROM purchase_contracts WHERE contract_no = 'ADMIN-TEST-20260715-C01' LIMIT 1);
SET @admin_contract_2 = (SELECT id FROM purchase_contracts WHERE contract_no = 'ADMIN-TEST-20260715-C02' LIMIT 1);
SET @admin_contract_3 = (SELECT id FROM purchase_contracts WHERE contract_no = 'ADMIN-TEST-20260715-C03' LIMIT 1);
SET @admin_contract_4 = (SELECT id FROM purchase_contracts WHERE contract_no = 'ADMIN-TEST-20260715-C04' LIMIT 1);

INSERT INTO payment_plans (purchase_contract_id, plan_name, installment_count, total_amount, start_date, status)
SELECT @admin_contract_1, 'ADMIN Test Standard Plan', 4, 650000.00, '2026-01-15', 'active'
WHERE NOT EXISTS (SELECT 1 FROM payment_plans WHERE purchase_contract_id = @admin_contract_1 AND status = 'active');
INSERT INTO payment_plans (purchase_contract_id, plan_name, installment_count, total_amount, start_date, status)
SELECT @admin_contract_2, 'ADMIN Test Standard Plan', 4, 980000.00, '2026-01-15', 'active'
WHERE NOT EXISTS (SELECT 1 FROM payment_plans WHERE purchase_contract_id = @admin_contract_2 AND status = 'active');
INSERT INTO payment_plans (purchase_contract_id, plan_name, installment_count, total_amount, start_date, status)
SELECT @admin_contract_3, 'ADMIN Test Standard Plan', 4, 480000.00, '2026-02-15', 'active'
WHERE NOT EXISTS (SELECT 1 FROM payment_plans WHERE purchase_contract_id = @admin_contract_3 AND status = 'active');
INSERT INTO payment_plans (purchase_contract_id, plan_name, installment_count, total_amount, start_date, status)
SELECT @admin_contract_4, 'ADMIN Test Standard Plan', 4, 710000.00, '2026-02-15', 'active'
WHERE NOT EXISTS (SELECT 1 FROM payment_plans WHERE purchase_contract_id = @admin_contract_4 AND status = 'active');

SET @admin_plan_1 = (SELECT id FROM payment_plans WHERE purchase_contract_id = @admin_contract_1 AND status = 'active' LIMIT 1);
SET @admin_plan_2 = (SELECT id FROM payment_plans WHERE purchase_contract_id = @admin_contract_2 AND status = 'active' LIMIT 1);
SET @admin_plan_3 = (SELECT id FROM payment_plans WHERE purchase_contract_id = @admin_contract_3 AND status = 'active' LIMIT 1);
SET @admin_plan_4 = (SELECT id FROM payment_plans WHERE purchase_contract_id = @admin_contract_4 AND status = 'active' LIMIT 1);

INSERT IGNORE INTO payment_installments (payment_plan_id, installment_no, milestone, due_date, amount_due, amount_paid, status) VALUES
(@admin_plan_1, 1, 'Deposit', '2026-01-15', 162500.00, 162500.00, 'paid'),
(@admin_plan_1, 2, 'Foundation', '2026-04-15', 162500.00, 162500.00, 'paid'),
(@admin_plan_1, 3, 'Structure', '2026-09-15', 162500.00, 0.00, 'pending'),
(@admin_plan_1, 4, 'Handover', '2026-12-15', 162500.00, 0.00, 'pending'),
(@admin_plan_2, 1, 'Deposit', '2026-01-15', 245000.00, 245000.00, 'paid'),
(@admin_plan_2, 2, 'Foundation', '2026-03-15', 245000.00, 245000.00, 'paid'),
(@admin_plan_2, 3, 'Structure', '2026-05-15', 245000.00, 245000.00, 'paid'),
(@admin_plan_2, 4, 'Handover', '2026-07-01', 245000.00, 245000.00, 'paid'),
(@admin_plan_3, 1, 'Deposit', '2026-02-15', 120000.00, 120000.00, 'paid'),
(@admin_plan_3, 2, 'Foundation', '2026-07-20', 120000.00, 0.00, 'pending'),
(@admin_plan_3, 3, 'Structure', '2026-10-20', 120000.00, 0.00, 'pending'),
(@admin_plan_3, 4, 'Handover', '2027-01-20', 120000.00, 0.00, 'pending'),
(@admin_plan_4, 1, 'Deposit', '2026-02-15', 177500.00, 177500.00, 'paid'),
(@admin_plan_4, 2, 'Foundation', '2026-06-15', 177500.00, 0.00, 'overdue'),
(@admin_plan_4, 3, 'Structure', '2026-10-15', 177500.00, 0.00, 'pending'),
(@admin_plan_4, 4, 'Handover', '2027-02-15', 177500.00, 0.00, 'pending');

INSERT INTO reserve_accounts (owner_unit_id, minimum_balance, current_balance, status)
SELECT @admin_owner_unit_1, 2000.00, 4500.00, 'active'
WHERE NOT EXISTS (SELECT 1 FROM reserve_accounts WHERE owner_unit_id = @admin_owner_unit_1);
INSERT INTO reserve_accounts (owner_unit_id, minimum_balance, current_balance, status)
SELECT @admin_owner_unit_2, 2500.00, 3860.50, 'active'
WHERE NOT EXISTS (SELECT 1 FROM reserve_accounts WHERE owner_unit_id = @admin_owner_unit_2);
INSERT INTO reserve_accounts (owner_unit_id, minimum_balance, current_balance, status)
SELECT @admin_owner_unit_3, 1500.00, 1500.00, 'active'
WHERE NOT EXISTS (SELECT 1 FROM reserve_accounts WHERE owner_unit_id = @admin_owner_unit_3);
INSERT INTO reserve_accounts (owner_unit_id, minimum_balance, current_balance, status)
SELECT @admin_owner_unit_4, 2000.00, 3000.00, 'active'
WHERE NOT EXISTS (SELECT 1 FROM reserve_accounts WHERE owner_unit_id = @admin_owner_unit_4);

SET @admin_reserve_account_1 = (SELECT id FROM reserve_accounts WHERE owner_unit_id = @admin_owner_unit_1 LIMIT 1);
SET @admin_reserve_account_2 = (SELECT id FROM reserve_accounts WHERE owner_unit_id = @admin_owner_unit_2 LIMIT 1);
SET @admin_reserve_account_3 = (SELECT id FROM reserve_accounts WHERE owner_unit_id = @admin_owner_unit_3 LIMIT 1);
SET @admin_reserve_account_4 = (SELECT id FROM reserve_accounts WHERE owner_unit_id = @admin_owner_unit_4 LIMIT 1);

INSERT INTO reserve_transactions (reserve_account_id, transaction_type, amount, occurred_at, balance_after, note, created_by)
SELECT @admin_reserve_account_1, 'topup', 4500.00, '2026-03-01 09:00:00', 4500.00, 'ADMIN opening reserve balance A-0301', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM reserve_transactions WHERE reserve_account_id = @admin_reserve_account_1 AND note = 'ADMIN opening reserve balance A-0301');
INSERT INTO reserve_transactions (reserve_account_id, transaction_type, amount, occurred_at, balance_after, note, created_by)
SELECT @admin_reserve_account_2, 'topup', 3860.50, '2026-03-01 09:10:00', 3860.50, 'ADMIN opening reserve balance A-0906', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM reserve_transactions WHERE reserve_account_id = @admin_reserve_account_2 AND note = 'ADMIN opening reserve balance A-0906');
INSERT INTO reserve_transactions (reserve_account_id, transaction_type, amount, occurred_at, balance_after, note, created_by)
SELECT @admin_reserve_account_3, 'topup', 1500.00, '2026-04-01 09:00:00', 1500.00, 'ADMIN opening reserve balance B-0602', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM reserve_transactions WHERE reserve_account_id = @admin_reserve_account_3 AND note = 'ADMIN opening reserve balance B-0602');
INSERT INTO reserve_transactions (reserve_account_id, transaction_type, amount, occurred_at, balance_after, note, created_by)
SELECT @admin_reserve_account_4, 'topup', 3000.00, '2026-04-01 09:10:00', 3000.00, 'ADMIN opening reserve balance B-1508', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM reserve_transactions WHERE reserve_account_id = @admin_reserve_account_4 AND note = 'ADMIN opening reserve balance B-1508');

-- Owner expense and maintenance dashboard records.
INSERT INTO vendors (vendor_code, name, contact_name, phone, email, status)
SELECT 'ADMIN-TEST-20260716-V01', 'ADMIN Test Property Care', 'Maintenance Desk', '+60110000888', 'maintenance@example.com', 'active'
WHERE NOT EXISTS (SELECT 1 FROM vendors WHERE vendor_code = 'ADMIN-TEST-20260716-V01');
SET @admin_vendor_1 = (SELECT id FROM vendors WHERE vendor_code = 'ADMIN-TEST-20260716-V01' LIMIT 1);

INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status, created_by)
SELECT 'ADMIN-TEST-20260716-E01', 'cashflow', @admin_unit_1, @admin_owner_id, 286.40, 'MYR', '2026-07-15', 'reserve_account', 'paid', 'confirmed', 'not_synced', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260716-E01');
INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status, created_by)
SELECT 'ADMIN-TEST-20260716-E02', 'cashflow', @admin_unit_2, @admin_owner_id, 950.00, 'MYR', '2026-07-12', 'reserve_account', 'paid', 'confirmed', 'not_synced', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260716-E02');
INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status, created_by)
SELECT 'ADMIN-TEST-20260716-E03', 'cashflow', @admin_unit_2, @admin_owner_id, 480.00, 'MYR', '2026-07-10', 'reserve_account', 'paid', 'confirmed', 'not_synced', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260716-E03');
INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status, created_by)
SELECT 'ADMIN-TEST-20260716-E04', 'cashflow', @admin_unit_1, @admin_owner_id, 350.00, 'MYR', '2026-07-09', NULL, 'unpaid', 'pending', 'not_synced', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260716-E04');
INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status, created_by)
SELECT 'ADMIN-TEST-20260716-E05', 'cashflow', @admin_unit_3, @admin_owner_id, 60.00, 'MYR', '2026-07-07', 'reserve_account', 'paid', 'confirmed', 'not_synced', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260716-E05');
INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status, created_by)
SELECT 'ADMIN-TEST-20260716-E06', 'cashflow', @admin_unit_3, @admin_owner_id, 420.00, 'MYR', '2026-07-05', 'reserve_account', 'paid', 'confirmed', 'not_synced', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260716-E06');
INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status, created_by)
SELECT 'ADMIN-TEST-20260716-E07', 'cashflow', @admin_unit_1, @admin_owner_id, 612.30, 'MYR', '2026-06-20', 'reserve_account', 'paid', 'confirmed', 'not_synced', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260716-E07');

SET @admin_expense_finance_1 = (SELECT id FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260716-E01' LIMIT 1);
SET @admin_expense_finance_2 = (SELECT id FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260716-E02' LIMIT 1);
SET @admin_expense_finance_3 = (SELECT id FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260716-E03' LIMIT 1);
SET @admin_expense_finance_4 = (SELECT id FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260716-E04' LIMIT 1);
SET @admin_expense_finance_5 = (SELECT id FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260716-E05' LIMIT 1);
SET @admin_expense_finance_6 = (SELECT id FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260716-E06' LIMIT 1);
SET @admin_expense_finance_7 = (SELECT id FROM finance_records WHERE transaction_no = 'ADMIN-TEST-20260716-E07' LIMIT 1);
SET @admin_reserve_account_2 = (SELECT id FROM reserve_accounts WHERE owner_unit_id = @admin_owner_unit_2 LIMIT 1);

INSERT INTO cashflow_entries (finance_record_id, unit_id, owner_id, direction, category, description, occurred_on, attachment_status)
SELECT @admin_expense_finance_1, @admin_unit_1, @admin_owner_id, 'expense', 'utilities', '2026年7月水費賬單', '2026-07-15', 'not_required'
WHERE NOT EXISTS (SELECT 1 FROM cashflow_entries WHERE finance_record_id = @admin_expense_finance_1);
INSERT INTO cashflow_entries (finance_record_id, unit_id, owner_id, direction, category, description, occurred_on, attachment_status)
SELECT @admin_expense_finance_2, @admin_unit_2, @admin_owner_id, 'expense', 'management', '2026年7月管理費', '2026-07-12', 'not_required'
WHERE NOT EXISTS (SELECT 1 FROM cashflow_entries WHERE finance_record_id = @admin_expense_finance_2);
INSERT INTO cashflow_entries (finance_record_id, unit_id, owner_id, vendor_id, direction, category, description, occurred_on, reserve_account_id, attachment_status)
SELECT @admin_expense_finance_3, @admin_unit_2, @admin_owner_id, @admin_vendor_1, 'expense', 'maintenance', '廚房水管漏水維修更換', '2026-07-10', @admin_reserve_account_2, 'not_required'
WHERE NOT EXISTS (SELECT 1 FROM cashflow_entries WHERE finance_record_id = @admin_expense_finance_3);
INSERT INTO cashflow_entries (finance_record_id, unit_id, owner_id, vendor_id, direction, category, description, occurred_on, attachment_status)
SELECT @admin_expense_finance_4, @admin_unit_1, @admin_owner_id, @admin_vendor_1, 'expense', 'maintenance', '客廳冷氣不冷，檢查並加注冷媒', '2026-07-09', 'missing'
WHERE NOT EXISTS (SELECT 1 FROM cashflow_entries WHERE finance_record_id = @admin_expense_finance_4);
INSERT INTO cashflow_entries (finance_record_id, unit_id, owner_id, direction, category, description, occurred_on, attachment_status)
SELECT @admin_expense_finance_5, @admin_unit_3, @admin_owner_id, 'expense', 'other', '門禁卡補辦費用（2張）', '2026-07-07', 'not_required'
WHERE NOT EXISTS (SELECT 1 FROM cashflow_entries WHERE finance_record_id = @admin_expense_finance_5);
INSERT INTO cashflow_entries (finance_record_id, unit_id, owner_id, direction, category, description, occurred_on, attachment_status)
SELECT @admin_expense_finance_6, @admin_unit_3, @admin_owner_id, 'expense', 'cleaning', '公共區域清潔費', '2026-07-05', 'not_required'
WHERE NOT EXISTS (SELECT 1 FROM cashflow_entries WHERE finance_record_id = @admin_expense_finance_6);
INSERT INTO cashflow_entries (finance_record_id, unit_id, owner_id, direction, category, description, occurred_on, attachment_status)
SELECT @admin_expense_finance_7, @admin_unit_1, @admin_owner_id, 'expense', 'utilities', '2026年6月電費賬單', '2026-06-20', 'not_required'
WHERE NOT EXISTS (SELECT 1 FROM cashflow_entries WHERE finance_record_id = @admin_expense_finance_7);

SET @admin_cashflow_3 = (SELECT id FROM cashflow_entries WHERE finance_record_id = @admin_expense_finance_3 LIMIT 1);
SET @admin_cashflow_4 = (SELECT id FROM cashflow_entries WHERE finance_record_id = @admin_expense_finance_4 LIMIT 1);
INSERT INTO maintenance_work_orders (work_order_no, unit_id, owner_id, vendor_id, cashflow_entry_id, category, title, description, requested_at, status, estimated_amount, actual_amount, created_by)
SELECT 'ADMIN-TEST-20260716-M01', @admin_unit_2, @admin_owner_id, @admin_vendor_1, @admin_cashflow_3, 'plumbing', '廚房水管漏水維修', '廚房水管接口持續滲水，需要更換接頭及密封件。', '2026-07-10 10:30:00', 'in_progress', 500.00, 480.00, @admin_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_work_orders WHERE work_order_no = 'ADMIN-TEST-20260716-M01');
INSERT INTO maintenance_work_orders (work_order_no, unit_id, owner_id, vendor_id, cashflow_entry_id, category, title, description, requested_at, status, estimated_amount, created_by)
SELECT 'ADMIN-TEST-20260716-M02', @admin_unit_1, @admin_owner_id, @admin_vendor_1, @admin_cashflow_4, 'air_conditioning', '客廳冷氣檢查', '客廳冷氣制冷不足，等待安排維修員上門。', '2026-07-09 14:15:00', 'submitted', 350.00, @admin_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_work_orders WHERE work_order_no = 'ADMIN-TEST-20260716-M02');
SET @admin_work_order_1 = (SELECT id FROM maintenance_work_orders WHERE work_order_no = 'ADMIN-TEST-20260716-M01' LIMIT 1);
SET @admin_work_order_2 = (SELECT id FROM maintenance_work_orders WHERE work_order_no = 'ADMIN-TEST-20260716-M02' LIMIT 1);

INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by)
SELECT @admin_work_order_1, 'submitted', '2026-07-10 10:30:00', '業主提交報修', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_status_history WHERE work_order_id = @admin_work_order_1 AND status = 'submitted');
INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by)
SELECT @admin_work_order_1, 'assigned', '2026-07-10 11:00:00', '已指派 ADMIN Test Property Care', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_status_history WHERE work_order_id = @admin_work_order_1 AND status = 'assigned');
INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by)
SELECT @admin_work_order_1, 'in_progress', '2026-07-11 09:30:00', '維修員已開始處理', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_status_history WHERE work_order_id = @admin_work_order_1 AND status = 'in_progress');
INSERT INTO maintenance_status_history (work_order_id, status, occurred_at, note, changed_by)
SELECT @admin_work_order_2, 'submitted', '2026-07-09 14:15:00', '業主提交報修', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM maintenance_status_history WHERE work_order_id = @admin_work_order_2 AND status = 'submitted');

SET @admin_reserve_debit_exists = (SELECT COUNT(*) FROM reserve_transactions WHERE finance_record_id = @admin_expense_finance_3 AND transaction_type = 'debit');
INSERT INTO reserve_transactions (reserve_account_id, finance_record_id, maintenance_work_order_id, transaction_type, amount, occurred_at, balance_after, note, created_by)
SELECT @admin_reserve_account_2, @admin_expense_finance_3, @admin_work_order_1, 'debit', 480.00, '2026-07-12 09:00:00', GREATEST(current_balance - 480.00, 0), '廚房水管維修費由預備金扣除', @admin_id
FROM reserve_accounts
WHERE id = @admin_reserve_account_2 AND @admin_reserve_debit_exists = 0 AND current_balance >= 480.00;
UPDATE reserve_accounts
SET current_balance = current_balance - 480.00
WHERE id = @admin_reserve_account_2 AND @admin_reserve_debit_exists = 0 AND current_balance >= 480.00;

INSERT INTO notifications (recipient_user_id, recipient_owner_id, title, body, priority, status, created_at)
SELECT @portfolio_owner_user_id, @admin_owner_id, '2026 年 7 月租金已入帳', 'Central Suites B-0602 租金 RM 2,100.00 已完成確認。', 'normal', 'unread', '2026-07-15 09:30:00'
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_user_id = @portfolio_owner_user_id AND title = '2026 年 7 月租金已入帳');
INSERT INTO notifications (recipient_user_id, recipient_owner_id, title, body, priority, status, created_at)
SELECT @portfolio_owner_user_id, @admin_owner_id, '房款即將到期', 'Central Suites B-0602 下一期房款將於 2026-07-20 到期。', 'high', 'unread', '2026-07-14 10:00:00'
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_user_id = @portfolio_owner_user_id AND title = '房款即將到期');
INSERT INTO notifications (recipient_user_id, recipient_owner_id, title, body, priority, status, created_at)
SELECT @portfolio_owner_user_id, @admin_owner_id, '預備金帳戶已更新', '您名下 4 個單位的預備金餘額已完成同步。', 'normal', 'read', '2026-07-12 16:20:00'
WHERE NOT EXISTS (SELECT 1 FROM notifications WHERE recipient_user_id = @portfolio_owner_user_id AND title = '預備金帳戶已更新');

INSERT INTO documents (document_no, original_name, storage_key, mime_type, document_type, status, uploaded_by)
SELECT 'ADMIN-TEST-20260715-D01', 'Lakeview A-0301 purchase contract.pdf', 'test/admin/lakeview-a0301-contract.pdf', 'application/pdf', 'purchase_contract', 'pending_signature', @admin_id
WHERE NOT EXISTS (SELECT 1 FROM documents WHERE document_no = 'ADMIN-TEST-20260715-D01');
SET @admin_document_1 = (SELECT id FROM documents WHERE document_no = 'ADMIN-TEST-20260715-D01' LIMIT 1);
INSERT INTO document_links (document_id, entity_type, entity_id, relation_type)
SELECT @admin_document_1, 'owner', @admin_owner_id, 'signature'
WHERE NOT EXISTS (SELECT 1 FROM document_links WHERE document_id = @admin_document_1 AND entity_type = 'owner' AND entity_id = @admin_owner_id AND relation_type = 'signature');
INSERT INTO document_links (document_id, entity_type, entity_id, relation_type)
SELECT @admin_document_1, 'unit', @admin_unit_1, 'property'
WHERE NOT EXISTS (SELECT 1 FROM document_links WHERE document_id = @admin_document_1 AND entity_type = 'unit' AND entity_id = @admin_unit_1 AND relation_type = 'property');

INSERT INTO properties (name, project_name, address, price, area, bedrooms, status, created_at, updated_at)
SELECT 'ADMIN Test Lakeview A-0301', 'ADMIN Test Lakeview Towers', '10 Admin Test Lakeview Road, Kuala Lumpur', 650000.00, 76, 2, 'AVAILABLE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE name = 'ADMIN Test Lakeview A-0301');
INSERT INTO properties (name, project_name, address, price, area, bedrooms, status, created_at, updated_at)
SELECT 'ADMIN Test Lakeview A-0906', 'ADMIN Test Lakeview Towers', '10 Admin Test Lakeview Road, Kuala Lumpur', 980000.00, 110, 3, 'SOLD', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE name = 'ADMIN Test Lakeview A-0906');
INSERT INTO properties (name, project_name, address, price, area, bedrooms, status, created_at, updated_at)
SELECT 'ADMIN Test Central B-0602', 'ADMIN Test Central Suites', '20 Admin Test Central Street, Petaling Jaya', 480000.00, 48, 1, 'RESERVED', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE name = 'ADMIN Test Central B-0602');
INSERT INTO properties (name, project_name, address, price, area, bedrooms, status, created_at, updated_at)
SELECT 'ADMIN Test Central B-1508', 'ADMIN Test Central Suites', '20 Admin Test Central Street, Petaling Jaya', 710000.00, 84, 2, 'AVAILABLE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE name = 'ADMIN Test Central B-1508');

UPDATE owner_units
SET asset_stage = 'OPERATING',
    actual_handover_date = COALESCE(actual_handover_date, start_date)
WHERE owner_id = @admin_owner_id AND status = 'active';

INSERT INTO owner_unit_services (owner_unit_id, service_type, status, started_at)
SELECT id, 'MANAGEMENT', 'active', COALESCE(actual_handover_date, start_date)
FROM owner_units
WHERE owner_id = @admin_owner_id AND asset_stage = 'OPERATING'
ON DUPLICATE KEY UPDATE status = 'active', ended_at = NULL;

UPDATE payment_plans pp
JOIN purchase_contracts pc ON pc.id = pp.purchase_contract_id
JOIN owner_units ou ON ou.id = pc.owner_unit_id
SET pp.status = 'historical'
WHERE ou.owner_id = @admin_owner_id AND pp.status = 'active';

UPDATE purchase_contracts pc
JOIN owner_units ou ON ou.id = pc.owner_unit_id
SET pc.status = 'completed', pc.handover_date = COALESCE(pc.handover_date, ou.actual_handover_date)
WHERE ou.owner_id = @admin_owner_id AND pc.status = 'active';

COMMIT;
