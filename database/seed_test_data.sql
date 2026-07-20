-- CCPS test data seed. Safe to re-run: all rows are identified by TEST-20260715 markers.
USE ccps_property_management;
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO projects (project_code, name, address, city, country_code, status)
SELECT 'TEST-20260715-P01', 'TEST Riverside Residence', '100 Test River Road', 'Kuala Lumpur', 'MY', 'active'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE project_code = 'TEST-20260715-P01');
INSERT INTO projects (project_code, name, address, city, country_code, status)
SELECT 'TEST-20260715-P02', 'TEST Garden Suites', '200 Test Garden Avenue', 'Petaling Jaya', 'MY', 'active'
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE project_code = 'TEST-20260715-P02');

SET @p1 = (SELECT id FROM projects WHERE project_code = 'TEST-20260715-P01');
SET @p2 = (SELECT id FROM projects WHERE project_code = 'TEST-20260715-P02');

INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
SELECT @p1, 'A', '08', 'A-0801', '2BR', 78.50, 2, 'available'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE project_id = @p1 AND unit_no = 'A-0801');
INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
SELECT @p1, 'A', '12', 'A-1208', '3BR', 102.00, 3, 'reserved'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE project_id = @p1 AND unit_no = 'A-1208');
INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
SELECT @p2, 'B', '05', 'B-0503', '1BR', 52.00, 1, 'occupied'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE project_id = @p2 AND unit_no = 'B-0503');
INSERT INTO units (project_id, building, floor_no, unit_no, unit_type, area_sqm, bedroom_count, listing_status)
SELECT @p2, 'B', '18', 'B-1802', '2BR', 81.25, 2, 'available'
WHERE NOT EXISTS (SELECT 1 FROM units WHERE project_id = @p2 AND unit_no = 'B-1802');

SET @u1 = (SELECT id FROM units WHERE project_id = @p1 AND unit_no = 'A-0801');
SET @u2 = (SELECT id FROM units WHERE project_id = @p1 AND unit_no = 'A-1208');
SET @u3 = (SELECT id FROM units WHERE project_id = @p2 AND unit_no = 'B-0503');
SET @u4 = (SELECT id FROM units WHERE project_id = @p2 AND unit_no = 'B-1802');

INSERT INTO owners (full_name, phone, email, status)
SELECT 'TEST Owner One', '+60110000001', 'test.owner1@example.com', 'active'
WHERE NOT EXISTS (SELECT 1 FROM owners WHERE email = 'test.owner1@example.com');
INSERT INTO owners (full_name, phone, email, status)
SELECT 'TEST Owner Two', '+60110000002', 'test.owner2@example.com', 'active'
WHERE NOT EXISTS (SELECT 1 FROM owners WHERE email = 'test.owner2@example.com');
SET @o1 = (SELECT id FROM owners WHERE email = 'test.owner1@example.com');
SET @o2 = (SELECT id FROM owners WHERE email = 'test.owner2@example.com');

UPDATE owners
SET user_id = (SELECT id FROM users WHERE username = 'owner' LIMIT 1)
WHERE id = @o1
  AND EXISTS (SELECT 1 FROM users WHERE username = 'owner');
UPDATE owners
SET user_id = (SELECT id FROM users WHERE username = 'owner2' LIMIT 1)
WHERE id = @o2
  AND EXISTS (SELECT 1 FROM users WHERE username = 'owner2');

INSERT INTO owner_units (owner_id, unit_id, ownership_percent, is_primary, start_date, status)
SELECT @o1, @u1, 100.00, 1, '2026-01-01', 'active'
WHERE NOT EXISTS (SELECT 1 FROM owner_units WHERE owner_id = @o1 AND unit_id = @u1);
INSERT INTO owner_units (owner_id, unit_id, ownership_percent, is_primary, start_date, status)
SELECT @o2, @u2, 100.00, 1, '2026-01-01', 'active'
WHERE NOT EXISTS (SELECT 1 FROM owner_units WHERE owner_id = @o2 AND unit_id = @u2);

INSERT INTO tenants (full_name, phone, email, status)
SELECT 'TEST Tenant One', '+60110000011', 'test.tenant1@example.com', 'active'
WHERE NOT EXISTS (SELECT 1 FROM tenants WHERE email = 'test.tenant1@example.com');
INSERT INTO tenants (full_name, phone, email, status)
SELECT 'TEST Tenant Two', '+60110000012', 'test.tenant2@example.com', 'active'
WHERE NOT EXISTS (SELECT 1 FROM tenants WHERE email = 'test.tenant2@example.com');
SET @t1 = (SELECT id FROM tenants WHERE email = 'test.tenant1@example.com');
SET @t2 = (SELECT id FROM tenants WHERE email = 'test.tenant2@example.com');

INSERT INTO vendors (vendor_code, name, contact_name, phone, email, status)
SELECT 'TEST-20260715-V01', 'TEST Bright Maintenance', 'TEST Vendor Contact', '+60110000021', 'test.vendor@example.com', 'active'
WHERE NOT EXISTS (SELECT 1 FROM vendors WHERE vendor_code = 'TEST-20260715-V01');

INSERT INTO leases (unit_id, tenant_id, lease_no, start_date, end_date, monthly_rent, deposit_amount, payment_day, status)
SELECT @u3, @t1, 'TEST-20260715-L01', '2026-01-01', '2026-12-31', 2800.00, 5600.00, 5, 'active'
WHERE NOT EXISTS (SELECT 1 FROM leases WHERE lease_no = 'TEST-20260715-L01');
SET @l1 = (SELECT id FROM leases WHERE lease_no = 'TEST-20260715-L01');

INSERT INTO rent_invoices (lease_id, billing_month, due_date, amount_due, amount_paid, status)
SELECT @l1, '2026-06-01', '2026-06-05', 2800.00, 2800.00, 'paid'
WHERE NOT EXISTS (SELECT 1 FROM rent_invoices WHERE lease_id = @l1 AND billing_month = '2026-06-01');
INSERT INTO rent_invoices (lease_id, billing_month, due_date, amount_due, amount_paid, status)
SELECT @l1, '2026-07-01', '2026-07-05', 2800.00, 0.00, 'overdue'
WHERE NOT EXISTS (SELECT 1 FROM rent_invoices WHERE lease_id = @l1 AND billing_month = '2026-07-01');
SET @ri1 = (SELECT id FROM rent_invoices WHERE lease_id = @l1 AND billing_month = '2026-06-01');

INSERT INTO finance_records (transaction_no, record_type, unit_id, owner_id, tenant_id, amount, currency, transaction_date, payment_method, payment_status, confirmation_status, sync_status)
SELECT 'TEST-20260715-F01', 'rent_payment', @u3, @o2, @t1, 2800.00, 'MYR', '2026-06-03', 'bank_transfer', 'paid', 'confirmed', 'not_synced'
WHERE NOT EXISTS (SELECT 1 FROM finance_records WHERE transaction_no = 'TEST-20260715-F01');
SET @f1 = (SELECT id FROM finance_records WHERE transaction_no = 'TEST-20260715-F01');
INSERT INTO rent_payments (rent_invoice_id, finance_record_id)
SELECT @ri1, @f1
WHERE NOT EXISTS (SELECT 1 FROM rent_payments WHERE finance_record_id = @f1);

INSERT INTO properties (name, project_name, address, price, area, bedrooms, status, created_at, updated_at)
SELECT 'TEST Riverside Residence A-0801', 'TEST Riverside Residence', '100 Test River Road, Kuala Lumpur', 680000.00, 79, 2, 'AVAILABLE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE name = 'TEST Riverside Residence A-0801');
INSERT INTO properties (name, project_name, address, price, area, bedrooms, status, created_at, updated_at)
SELECT 'TEST Riverside Residence A-1208', 'TEST Riverside Residence', '100 Test River Road, Kuala Lumpur', 920000.00, 102, 3, 'RESERVED', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE name = 'TEST Riverside Residence A-1208');
INSERT INTO properties (name, project_name, address, price, area, bedrooms, status, created_at, updated_at)
SELECT 'TEST Garden Suites B-1802', 'TEST Garden Suites', '200 Test Garden Avenue, Petaling Jaya', 720000.00, 81, 2, 'AVAILABLE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM properties WHERE name = 'TEST Garden Suites B-1802');

COMMIT;
