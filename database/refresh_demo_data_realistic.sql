-- Replace the original TEST / ADMIN Test seed labels with realistic Malaysian demo data.
-- Business amounts, dates, statuses and foreign-key relationships are intentionally preserved.
USE ccps_property_management;
SET NAMES utf8mb4;
START TRANSACTION;

UPDATE tenants
SET full_name = 'Farah Nadia Binti Ismail',
    identity_no = '950218-10-4316',
    phone = '+60127864531',
    email = 'farah.nadia@ccps-demo.my'
WHERE email = 'test.tenant1@example.com';

UPDATE tenants
SET full_name = 'Lee Jia Hui',
    identity_no = '920907-14-6824',
    phone = '+60173345219',
    email = 'jia.hui.lee@ccps-demo.my'
WHERE email = 'test.tenant2@example.com';

UPDATE tenants
SET full_name = 'Alicia Tan Pei Wen',
    identity_no = '940326-10-5732',
    phone = '+60123881756',
    email = 'alicia.tan@ccps-demo.my'
WHERE email = 'admin.test.tenant1@example.com';

UPDATE tenants
SET full_name = 'Muhammad Hafiz Bin Roslan',
    identity_no = '910814-10-6247',
    phone = '+60176559028',
    email = 'hafiz.roslan@ccps-demo.my'
WHERE email = 'admin.test.tenant2@example.com';

UPDATE owners
SET full_name = 'Lim Chee Keong',
    identity_no = '780315-10-5521',
    phone = '+60123346882',
    email = 'cheekeong.lim@ccps-demo.my'
WHERE email = 'test.owner2@example.com';

UPDATE owners
SET full_name = 'Michelle Wong Siew Ling',
    identity_no = '850611-10-3188',
    phone = '+60122917603',
    email = 'michelle.wong@ccps-demo.my'
WHERE email = 'admin.test.owner@example.com';

UPDATE users
SET display_name = 'Lim Chee Keong',
    email = 'cheekeong.lim@ccps-demo.my',
    phone = '+60123346882'
WHERE username = 'owner2';

UPDATE users
SET display_name = 'Michelle Wong Siew Ling',
    email = 'michelle.wong@ccps-demo.my',
    phone = '+60122917603'
WHERE username = 'owner3';

UPDATE projects
SET project_code = 'VHR-KL-2026',
    name = 'Vista Harmoni Residences',
    address = '18 Jalan Tun Razak',
    city = 'Kuala Lumpur'
WHERE project_code = 'TEST-20260715-P01';

UPDATE projects
SET project_code = 'SMS-PJ-2026',
    name = 'Seri Mutiara Suites',
    address = '6 Jalan SS 2/72',
    city = 'Petaling Jaya'
WHERE project_code = 'TEST-20260715-P02';

UPDATE projects
SET project_code = 'AMK-KL-2026',
    name = 'Aria Mont Kiara',
    address = '15 Jalan Kiara 3',
    city = 'Kuala Lumpur'
WHERE project_code = 'ADMIN-TEST-20260715-P01';

UPDATE projects
SET project_code = 'SGR-SB-2026',
    name = 'Sunway Geo Residences',
    address = 'Jalan Lagoon Selatan, Bandar Sunway',
    city = 'Subang Jaya'
WHERE project_code = 'ADMIN-TEST-20260715-P02';

UPDATE units SET unit_no = 'A-08-01', unit_type = '2 Bedroom' WHERE id = 1 AND unit_no = 'A-0801';
UPDATE units SET unit_no = 'A-12-08', unit_type = '3 Bedroom' WHERE unit_no = 'A-1208';
UPDATE units SET unit_no = 'B-05-03', unit_type = '1 Bedroom' WHERE unit_no = 'B-0503';
UPDATE units SET unit_no = 'B-18-02', unit_type = '2 Bedroom' WHERE unit_no = 'B-1802';
UPDATE units SET unit_no = 'A-03-01', unit_type = '2 Bedroom' WHERE unit_no = 'ADMIN-A-0301';
UPDATE units SET unit_no = 'A-09-06', unit_type = '3 Bedroom' WHERE unit_no = 'ADMIN-A-0906';
UPDATE units SET unit_no = 'B-06-02', unit_type = '1 Bedroom' WHERE unit_no = 'ADMIN-B-0602';
UPDATE units SET unit_no = 'B-15-08', unit_type = '2 Bedroom' WHERE unit_no = 'ADMIN-B-1508';

UPDATE leases SET lease_no = 'LS-2026-SMS-B0503' WHERE lease_no = 'TEST-20260715-L01';
UPDATE leases SET lease_no = 'LS-2026-AMK-A0906' WHERE lease_no = 'ADMIN-TEST-20260715-L01';
UPDATE leases SET lease_no = 'LS-2026-SGR-B0602' WHERE lease_no = 'ADMIN-TEST-20260715-L02';

UPDATE finance_records SET transaction_no = 'RC-20260603-SMS-B0503' WHERE transaction_no = 'TEST-20260715-F01';
UPDATE finance_records SET transaction_no = 'RC-20260603-AMK-A0906' WHERE transaction_no = 'ADMIN-TEST-20260715-F01';
UPDATE finance_records SET transaction_no = 'RC-20260701-SGR-B0602' WHERE transaction_no = 'ADMIN-TEST-20260715-F02';
UPDATE finance_records SET transaction_no = 'PP-20260708-AMK-A0301' WHERE transaction_no = 'ADMIN-TEST-20260715-F03';
UPDATE finance_records SET transaction_no = 'PP-20260710-SGR-B0602' WHERE transaction_no = 'ADMIN-TEST-20260715-F04';
UPDATE finance_records SET transaction_no = 'RC-20260712-AMK-A0906' WHERE transaction_no = 'ADMIN-TEST-20260715-F05';
UPDATE finance_records SET transaction_no = REPLACE(transaction_no, 'ADMIN-TEST-20260716-E', 'EXP-202607-')
WHERE transaction_no LIKE 'ADMIN-TEST-20260716-E%';

UPDATE purchase_contracts SET contract_no = 'SPA-2026-AMK-A0301' WHERE contract_no = 'ADMIN-TEST-20260715-C01';
UPDATE purchase_contracts SET contract_no = 'SPA-2026-AMK-A0906' WHERE contract_no = 'ADMIN-TEST-20260715-C02';
UPDATE purchase_contracts SET contract_no = 'SPA-2026-SGR-B0602' WHERE contract_no = 'ADMIN-TEST-20260715-C03';
UPDATE purchase_contracts SET contract_no = 'SPA-2026-SGR-B1508' WHERE contract_no = 'ADMIN-TEST-20260715-C04';

UPDATE vendors
SET vendor_code = 'VND-BRIGHTCARE-01',
    name = 'BrightCare Facilities Sdn. Bhd.',
    contact_name = 'Nurul Huda',
    phone = '+60380621888',
    email = 'service@brightcare-demo.my'
WHERE vendor_code = 'TEST-20260715-V01';

UPDATE vendors
SET vendor_code = 'VND-PROFIX-01',
    name = 'ProFix Property Services Sdn. Bhd.',
    contact_name = 'Jason Goh',
    phone = '+60362013888',
    email = 'support@profix-demo.my'
WHERE vendor_code = 'ADMIN-TEST-20260716-V01';

UPDATE maintenance_work_orders SET work_order_no = 'MWO-20260710-AMK-A0906'
WHERE work_order_no = 'ADMIN-TEST-20260716-M01';
UPDATE maintenance_work_orders SET work_order_no = 'MWO-20260709-AMK-A0301'
WHERE work_order_no = 'ADMIN-TEST-20260716-M02';

UPDATE maintenance_status_history
SET note = '已指派 ProFix Property Services Sdn. Bhd.'
WHERE note = '已指派 ADMIN Test Property Care';

UPDATE reserve_transactions SET note = '期初預備金充值 · A-03-01' WHERE note = 'ADMIN opening reserve balance A-0301';
UPDATE reserve_transactions SET note = '期初預備金充值 · A-09-06' WHERE note = 'ADMIN opening reserve balance A-0906';
UPDATE reserve_transactions SET note = '期初預備金充值 · B-06-02' WHERE note = 'ADMIN opening reserve balance B-0602';
UPDATE reserve_transactions SET note = '期初預備金充值 · B-15-08' WHERE note = 'ADMIN opening reserve balance B-1508';

UPDATE notifications
SET body = REPLACE(REPLACE(body, 'Central Suites', 'Sunway Geo Residences'), 'B-0602', 'B-06-02')
WHERE body LIKE '%Central Suites%' OR body LIKE '%B-0602%';

UPDATE documents
SET document_no = 'DOC-SPA-2026-AMK-A0301',
    original_name = 'Aria Mont Kiara A-03-01 SPA.pdf',
    storage_key = 'demo/contracts/aria-mont-kiara-a0301-spa.pdf'
WHERE document_no = 'ADMIN-TEST-20260715-D01';

UPDATE properties
SET name = REPLACE(REPLACE(name, 'TEST Riverside Residence', 'Vista Harmoni Residences'), 'A-0801', 'A-08-01'),
    project_name = 'Vista Harmoni Residences',
    address = '18 Jalan Tun Razak, Kuala Lumpur'
WHERE project_name = 'TEST Riverside Residence';

UPDATE properties
SET name = REPLACE(REPLACE(name, 'TEST Garden Suites', 'Seri Mutiara Suites'), 'B-1802', 'B-18-02'),
    project_name = 'Seri Mutiara Suites',
    address = '6 Jalan SS 2/72, Petaling Jaya'
WHERE project_name = 'TEST Garden Suites';

UPDATE properties
SET name = REPLACE(REPLACE(REPLACE(name, 'ADMIN Test Lakeview', 'Aria Mont Kiara'), 'A-0301', 'A-03-01'), 'A-0906', 'A-09-06'),
    project_name = 'Aria Mont Kiara',
    address = '15 Jalan Kiara 3, Kuala Lumpur'
WHERE project_name = 'ADMIN Test Lakeview Towers';

UPDATE properties
SET name = REPLACE(REPLACE(REPLACE(name, 'ADMIN Test Central', 'Sunway Geo Residences'), 'B-0602', 'B-06-02'), 'B-1508', 'B-15-08'),
    project_name = 'Sunway Geo Residences',
    address = 'Jalan Lagoon Selatan, Bandar Sunway, Subang Jaya'
WHERE project_name = 'ADMIN Test Central Suites';

-- Clean up placeholder records entered during manual UI testing on 2026-07-19.
UPDATE tenants
SET full_name = 'Chan Pui San',
    identity_no = '960522-07-5814',
    phone = '+60174486290',
    email = 'puisan.chan@ccps-demo.my'
WHERE id = 7 AND identity_no = '10101';

UPDATE owners
SET full_name = 'Ong Wei Kiat',
    identity_no = '870924-10-4763',
    phone = '+60122863917',
    email = 'weikiat.ong@ccps-demo.my'
WHERE id = 7 AND identity_no = '1115';

UPDATE users
SET username = 'ong.weikiat',
    display_name = 'Ong Wei Kiat',
    email = 'weikiat.ong@ccps-demo.my',
    phone = '+60122863917'
WHERE id = 9 AND username = '12121';

UPDATE projects
SET project_code = 'PSQ-KL-2026',
    name = 'Pavilion Square',
    address = '1 Jalan Yap Kwan Seng',
    city = 'Kuala Lumpur'
WHERE id = 12 AND name = '測試';

UPDATE projects
SET project_code = 'EHR-PG-2026',
    name = 'Eco Horizon Residences',
    address = 'Persiaran Eco Horizon, Batu Kawan',
    city = 'Penang'
WHERE id = 13 AND name = 'lzj';

UPDATE units
SET building = 'B', floor_no = '22', unit_no = 'B-22-08', unit_type = '2 Bedroom',
    area_sqm = 89.60, bedroom_count = 2
WHERE id = 15 AND unit_no = '1111';

UPDATE units
SET building = 'A', floor_no = '10', unit_no = 'A-10-01', unit_type = '2 Bedroom',
    area_sqm = 86.40, bedroom_count = 2
WHERE id = 16 AND unit_no = '101';

UPDATE units
SET building = 'A', floor_no = '10', unit_no = 'A-10-02', unit_type = '3 Bedroom',
    area_sqm = 112.80, bedroom_count = 3
WHERE id = 17 AND unit_no = '102';

UPDATE units
SET building = 'A', floor_no = '20', unit_no = 'A-20-02', unit_type = '3 Bedroom',
    area_sqm = 118.20, bedroom_count = 3
WHERE id = 18 AND unit_no = '202';

UPDATE leases
SET lease_no = 'LS-2026-PSQ-A1002',
    monthly_rent = 3000.00,
    deposit_amount = 6000.00,
    payment_day = 5
WHERE id = 6 AND lease_no = 'LEASE-20260719-AC3A467423CC';

UPDATE rent_invoices
SET due_date = '2026-07-05', amount_due = 3000.00
WHERE lease_id = 6 AND billing_month = '2026-07-01' AND amount_paid = 0.00;

UPDATE projects SET project_code = 'RSR-KL-2026' WHERE project_code = 'OWNER-DEMO-P01';
UPDATE projects SET project_code = 'MDP-CYB-2026' WHERE project_code = 'OWNER-DEMO-P02';
UPDATE projects SET project_code = 'TGS-PJ-2026' WHERE project_code = 'OWNER-DEMO-P03';
UPDATE projects SET project_code = 'MCR-PG-2026' WHERE project_code = 'OWNER-DEMO-P04';
UPDATE projects SET project_code = 'KSL-KL-2026' WHERE project_code = 'OWNER-DEMO-P05';
UPDATE projects SET project_code = 'MKV-KL-2026' WHERE project_code = 'OWNER-DEMO-P06';
UPDATE projects SET project_code = 'BSS-KL-2026' WHERE project_code = 'OWNER-DEMO-P07';

UPDATE leases SET lease_no = 'LS-2026-KSL-E0903' WHERE lease_no = 'OWNER-DEMO-L01';
UPDATE leases SET lease_no = 'LS-2025-MKV-F1606' WHERE lease_no = 'OWNER-DEMO-L02';

-- Traditional Chinese display data for the administrator-facing interface.
UPDATE tenants SET full_name = '陳慧敏' WHERE id = 1;
UPDATE tenants SET full_name = '李佳慧' WHERE id = 2;
UPDATE tenants SET full_name = '陳佩雯' WHERE id = 3;
UPDATE tenants SET full_name = '莫哈末哈菲茲' WHERE id = 4;
UPDATE tenants SET full_name = '努爾艾莎' WHERE id = 5;
UPDATE tenants SET full_name = '李建偉' WHERE id = 6;

-- This is a user-created record, not seed data. Restore its original identity fields.
UPDATE tenants
SET full_name = '呂志傑',
    identity_no = '10101',
    phone = '18981712555',
    email = '1270673765423@qq.com'
WHERE id = 7;

UPDATE owners SET full_name = '陳偉明' WHERE id = 1;
UPDATE owners SET full_name = '林志強' WHERE id = 2;
UPDATE owners SET full_name = '黃秀玲' WHERE id = 3;
UPDATE owners
SET full_name = '呂小布', identity_no = '1115', phone = '12121', email = '127070@qq.com'
WHERE id = 7;

UPDATE users SET display_name = '陳偉明' WHERE id = 4;
UPDATE users SET display_name = '林志強' WHERE id = 5;
UPDATE users SET display_name = '黃秀玲' WHERE id = 6;
UPDATE users
SET username = '12121', display_name = '呂小布', email = '127070@qq.com', phone = '12121'
WHERE id = 9;

UPDATE projects SET name = '和景公寓', address = '敦拉薩路 18 號', city = '吉隆坡' WHERE project_code = 'VHR-KL-2026';
UPDATE projects SET name = '珍珠苑', address = 'SS 2/72 路 6 號', city = '八打靈再也' WHERE project_code = 'SMS-PJ-2026';
UPDATE projects SET name = '滿家樂雅居', address = '滿家樂 3 路 15 號', city = '吉隆坡' WHERE project_code = 'AMK-KL-2026';
UPDATE projects SET name = '雙威麗景公寓', address = '雙威鎮南湖路', city = '梳邦再也' WHERE project_code = 'SGR-SB-2026';
UPDATE projects SET name = '河畔公寓', address = '安邦希里路 8 號', city = '吉隆坡' WHERE project_code = 'RSR-KL-2026';
UPDATE projects SET name = '子午線花園', address = '科技城 6 路 12 號', city = '賽城' WHERE project_code = 'MDP-CYB-2026';
UPDATE projects SET name = '森林雅苑', address = '白沙羅 PJU 8/8 路 25 號', city = '八打靈再也' WHERE project_code = 'TGS-PJ-2026';
UPDATE projects SET name = '濱海峰景', address = '峇央英達大道 3 號', city = '檳城' WHERE project_code = 'MCR-PG-2026';
UPDATE projects SET name = '吉隆坡中環閣', address = '中環車站路 5 號 18 號', city = '吉隆坡' WHERE project_code = 'KSL-KL-2026';
UPDATE projects SET name = '滿家樂翠景', address = '滿家樂 3 路 6 號', city = '吉隆坡' WHERE project_code = 'MKV-KL-2026';
UPDATE projects SET name = '孟沙南城公寓', address = '克靈芝路 2 號', city = '吉隆坡' WHERE project_code = 'BSS-KL-2026';
UPDATE projects SET name = '柏威年廣場', address = '葉觀盛路 1 號', city = '吉隆坡' WHERE project_code = 'PSQ-KL-2026';
UPDATE projects SET name = '綠境住宅', address = '峇都交灣綠境大道', city = '檳城' WHERE project_code = 'EHR-PG-2026';

UPDATE units SET unit_type = '一房' WHERE unit_type = '1 Bedroom';
UPDATE units SET unit_type = '兩房' WHERE unit_type = '2 Bedroom';
UPDATE units SET unit_type = '三房' WHERE unit_type = '3 Bedroom';
UPDATE units SET unit_type = '一房加書房' WHERE unit_type = '1+1 Bedroom';
UPDATE units SET unit_type = '三房海景單位' WHERE unit_type = '3 Bedroom Sea View';

UPDATE vendors
SET name = '明亮物業設施管理有限公司', contact_name = '努魯胡達'
WHERE vendor_code = 'VND-BRIGHTCARE-01';
UPDATE vendors
SET name = '安居物業維修有限公司', contact_name = '吳志成'
WHERE vendor_code = 'VND-PROFIX-01';

UPDATE maintenance_status_history
SET note = '已指派安居物業維修有限公司'
WHERE note = '已指派 ProFix Property Services Sdn. Bhd.';

UPDATE maintenance_work_orders
SET work_order_no = 'MWO-20260609-KSL-E0903',
    title = '客廳冷氣例行保養',
    description = '安排技師清洗濾網、檢查冷媒與室外機運轉狀況。'
WHERE work_order_no = 'OWNER-DEMO-M01';

UPDATE maintenance_work_orders
SET work_order_no = 'MWO-20260711-MKV-F1606',
    title = '主臥浴室暗管漏水',
    description = '主臥浴室牆面出現滲水痕跡，需要檢測暗管並安排修復。'
WHERE work_order_no = 'OWNER-DEMO-M02';

UPDATE maintenance_work_orders
SET work_order_no = 'MWO-20260714-BSS-G2101',
    title = '看房前牆面補漆',
    description = '出租看房前修補客廳與走道牆面刮痕並完成局部補漆。',
    status = 'submitted'
WHERE work_order_no = 'OWNER-DEMO-M03';

UPDATE notifications SET body = REPLACE(body, 'Sunway Geo Residences', '雙威麗景公寓')
WHERE body LIKE '%Sunway Geo Residences%';

UPDATE documents
SET original_name = '滿家樂雅居 A-03-01 買賣合約.pdf'
WHERE document_no = 'DOC-SPA-2026-AMK-A0301';

UPDATE properties SET name = REPLACE(name, 'Vista Harmoni Residences', '和景公寓'), project_name = '和景公寓', address = '敦拉薩路 18 號，吉隆坡' WHERE project_name = 'Vista Harmoni Residences';
UPDATE properties SET name = REPLACE(name, 'Seri Mutiara Suites', '珍珠苑'), project_name = '珍珠苑', address = 'SS 2/72 路 6 號，八打靈再也' WHERE project_name = 'Seri Mutiara Suites';
UPDATE properties SET name = REPLACE(name, 'Aria Mont Kiara', '滿家樂雅居'), project_name = '滿家樂雅居', address = '滿家樂 3 路 15 號，吉隆坡' WHERE project_name = 'Aria Mont Kiara';
UPDATE properties SET name = REPLACE(name, 'Sunway Geo Residences', '雙威麗景公寓'), project_name = '雙威麗景公寓', address = '雙威鎮南湖路，梳邦再也' WHERE project_name = 'Sunway Geo Residences';

COMMIT;
