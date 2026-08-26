-- CCPS 管理后台岗位权限与测试账号。可重复执行。
-- 五个账号初始密码均为：123456（上线前必须修改）。
USE ccps_property_management;
SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO roles (code, name) VALUES
('SUPER_ADMIN', '超级管理员'),
('FINANCE', '财务管理员'),
('BUSINESS', '业务管理员'),
('CUSTOMER_SERVICE', '客服管理员'),
('ADMINISTRATION', '行政管理员')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO permissions (code, name) VALUES
('BACKOFFICE_VIEW', '查看管理后台与智慧大屏'),
('BUSINESS_MANAGE', '管理资产与租赁业务'),
('FINANCE_MANAGE', '财务确认、退回与资金管理'),
('OPERATIONS_MANAGE', '客服跟进、提醒与维修经办'),
('SYSTEM_MANAGE', '账号、岗位、模板、审计与备份管理'),
('REPORT_MANAGE', '生成及导出报表')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 先清除五个岗位的旧权限，再按本次设计重新建立。
DELETE rp
FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
WHERE r.code IN ('SUPER_ADMIN','FINANCE','BUSINESS','CUSTOMER_SERVICE','ADMINISTRATION');

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN ('BACKOFFICE_VIEW','BUSINESS_MANAGE','FINANCE_MANAGE','OPERATIONS_MANAGE','SYSTEM_MANAGE','REPORT_MANAGE');
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'FINANCE'
  AND p.code IN ('BACKOFFICE_VIEW','FINANCE_MANAGE','REPORT_MANAGE');
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'BUSINESS'
  AND p.code IN ('BACKOFFICE_VIEW','BUSINESS_MANAGE','OPERATIONS_MANAGE','REPORT_MANAGE');
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'CUSTOMER_SERVICE'
  AND p.code IN ('BACKOFFICE_VIEW','OPERATIONS_MANAGE');
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRATION'
  AND p.code IN ('BACKOFFICE_VIEW','SYSTEM_MANAGE','REPORT_MANAGE');

INSERT INTO users (username, email, password_hash, display_name, phone, account_type, status)
SELECT 'admin', 'admin@example.com', '$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW', '超级管理员', NULL, 'ADMIN', 'active'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');
INSERT INTO users (username, email, password_hash, display_name, phone, account_type, status)
SELECT 'admin.finance', 'admin.finance@example.com', '$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW', '财务管理员', NULL, 'ADMIN', 'active'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin.finance');
INSERT INTO users (username, email, password_hash, display_name, phone, account_type, status)
SELECT 'admin.business', 'admin.business@example.com', '$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW', '业务管理员', NULL, 'ADMIN', 'active'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin.business');
INSERT INTO users (username, email, password_hash, display_name, phone, account_type, status)
SELECT 'admin.customer', 'admin.customer@example.com', '$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW', '客服管理员', NULL, 'ADMIN', 'active'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin.customer');
INSERT INTO users (username, email, password_hash, display_name, phone, account_type, status)
SELECT 'admin.administration', 'admin.administration@example.com', '$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW', '行政管理员', NULL, 'ADMIN', 'active'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin.administration');

UPDATE users SET account_type = 'ADMIN', status = 'active' WHERE username IN
('admin','admin.finance','admin.business','admin.customer','admin.administration');

-- 后台账号必须拥有 ADMIN 门户角色。
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.code = 'ADMIN'
WHERE u.account_type = 'ADMIN';

-- 指定五个岗位账号；同一账号只保留一个后台岗位。
DELETE ur FROM user_roles ur
JOIN users u ON u.id = ur.user_id
JOIN roles r ON r.id = ur.role_id
WHERE u.username IN ('admin','admin.finance','admin.business','admin.customer','admin.administration')
  AND r.code IN ('SUPER_ADMIN','FINANCE','BUSINESS','CUSTOMER_SERVICE','ADMINISTRATION');

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.code = CASE u.username
  WHEN 'admin' THEN 'SUPER_ADMIN'
  WHEN 'admin.finance' THEN 'FINANCE'
  WHEN 'admin.business' THEN 'BUSINESS'
  WHEN 'admin.customer' THEN 'CUSTOMER_SERVICE'
  WHEN 'admin.administration' THEN 'ADMINISTRATION'
END
WHERE u.username IN ('admin','admin.finance','admin.business','admin.customer','admin.administration');

-- 其他旧后台账号默认归行政岗位，避免被兼容逻辑当成超级管理员。
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u JOIN roles r ON r.code = 'ADMINISTRATION'
WHERE u.account_type = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM user_roles ur JOIN roles sr ON sr.id = ur.role_id
    WHERE ur.user_id = u.id AND sr.code IN
    ('SUPER_ADMIN','FINANCE','BUSINESS','CUSTOMER_SERVICE','ADMINISTRATION')
  );

COMMIT;
