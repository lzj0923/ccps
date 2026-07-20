-- Test login accounts for both independent CCPS portals. Safe to re-run.
-- Every account below uses password: 123456
USE ccps_property_management;
SET NAMES utf8mb4;
START TRANSACTION;

-- Admin portal test users.
INSERT INTO users (username, email, password_hash, display_name, phone, account_type, status)
SELECT 'admin', 'admin@example.com', '$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW', 'System Administrator', NULL, 'ADMIN', 'active'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin' OR email = 'admin@example.com');
INSERT INTO users (username, email, password_hash, display_name, phone, account_type, status)
SELECT 'admin.ops', 'admin.ops@example.com', '$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW', 'Operations Administrator', '+60110000101', 'ADMIN', 'active'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin.ops' OR email = 'admin.ops@example.com');
INSERT INTO users (username, email, password_hash, display_name, phone, account_type, status)
SELECT 'admin.finance', 'admin.finance@example.com', '$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW', 'Finance Administrator', '+60110000102', 'ADMIN', 'active'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin.finance' OR email = 'admin.finance@example.com');

-- Owner portal test users.
INSERT INTO users (username, email, password_hash, display_name, phone, account_type, status)
SELECT 'owner', 'owner@example.com', '$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW', 'TEST Owner One', '+60110000001', 'OWNER', 'active'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'owner' OR email = 'owner@example.com');
INSERT INTO users (username, email, password_hash, display_name, phone, account_type, status)
SELECT 'owner2', 'owner2@example.com', '$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW', 'TEST Owner Two', '+60110000002', 'OWNER', 'active'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'owner2' OR email = 'owner2@example.com');
INSERT INTO users (username, email, password_hash, display_name, phone, account_type, status)
SELECT 'owner3', 'owner3@example.com', '$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW', 'ADMIN Test Portfolio Owner', '+60110000999', 'OWNER', 'active'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'owner3' OR email = 'owner3@example.com');

-- Repair existing rows when this seed is re-run after an older seed version.
UPDATE users SET account_type = 'ADMIN' WHERE username IN ('admin', 'admin.ops', 'admin.finance');
UPDATE users SET account_type = 'OWNER' WHERE username IN ('owner', 'owner2', 'owner3');

INSERT INTO roles (code, name)
SELECT 'ADMIN', 'System Administrator'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ADMIN');
INSERT INTO roles (code, name)
SELECT 'OWNER', 'Property Owner'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'OWNER');

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.code = 'ADMIN'
WHERE u.username IN ('admin', 'admin.ops', 'admin.finance');
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.code = 'OWNER'
WHERE u.username IN ('owner', 'owner2', 'owner3');

DELETE ur
FROM user_roles ur
JOIN users u ON u.id = ur.user_id
JOIN roles r ON r.id = ur.role_id
WHERE (u.account_type = 'ADMIN' AND r.code = 'OWNER')
   OR (u.account_type = 'OWNER' AND r.code = 'ADMIN');

SET @owner_user_id = (SELECT id FROM users WHERE username = 'owner' LIMIT 1);
SET @owner2_user_id = (SELECT id FROM users WHERE username = 'owner2' LIMIT 1);
SET @owner3_user_id = (SELECT id FROM users WHERE username = 'owner3' LIMIT 1);

UPDATE owners SET user_id = @owner_user_id WHERE email = 'test.owner1@example.com';
UPDATE owners SET user_id = @owner2_user_id WHERE email = 'test.owner2@example.com';
UPDATE owners SET user_id = @owner3_user_id WHERE email = 'admin.test.owner@example.com';

UPDATE notifications n
JOIN owners o ON o.id = n.recipient_owner_id
SET n.recipient_user_id = @owner3_user_id
WHERE o.email = 'admin.test.owner@example.com' AND @owner3_user_id IS NOT NULL;

COMMIT;
