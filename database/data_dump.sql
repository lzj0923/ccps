-- MySQL dump 10.13  Distrib 5.7.37, for Win64 (x86_64)
--
-- Host: localhost    Database: ccps_property_management
-- ------------------------------------------------------
-- Server version	5.7.37

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `cashflow_entries`
--

LOCK TABLES `cashflow_entries` WRITE;
/*!40000 ALTER TABLE `cashflow_entries` DISABLE KEYS */;
INSERT INTO `cashflow_entries` (`id`, `finance_record_id`, `unit_id`, `owner_id`, `tenant_id`, `vendor_id`, `direction`, `category`, `description`, `occurred_on`, `reserve_account_id`, `attachment_status`, `created_at`, `updated_at`) VALUES (1,7,5,3,NULL,NULL,'expense','utilities','2026年7月水費賬單','2026-07-15',NULL,'available','2026-07-16 10:24:25','2026-07-16 10:24:25'),(2,8,6,3,NULL,NULL,'expense','management','2026年7月管理費','2026-07-12',NULL,'available','2026-07-16 10:24:25','2026-07-16 10:24:25'),(3,9,6,3,NULL,2,'expense','maintenance','廚房水管漏水維修更換','2026-07-10',2,'available','2026-07-16 10:24:25','2026-07-16 10:24:25'),(4,10,5,3,NULL,2,'expense','maintenance','客廳冷氣不冷，檢查並加注冷媒','2026-07-09',NULL,'missing','2026-07-16 10:24:25','2026-07-16 10:24:25'),(5,11,7,3,NULL,NULL,'expense','other','門禁卡補辦費用（2張）','2026-07-07',NULL,'available','2026-07-16 10:24:25','2026-07-16 10:24:25'),(6,12,7,3,NULL,NULL,'expense','cleaning','公共區域清潔費','2026-07-05',NULL,'available','2026-07-16 10:24:25','2026-07-16 10:24:25'),(7,13,5,3,NULL,NULL,'expense','utilities','2026年6月電費賬單','2026-06-20',NULL,'available','2026-07-16 10:24:25','2026-07-16 10:24:25');
/*!40000 ALTER TABLE `cashflow_entries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `document_links`
--

LOCK TABLES `document_links` WRITE;
/*!40000 ALTER TABLE `document_links` DISABLE KEYS */;
INSERT INTO `document_links` (`id`, `document_id`, `entity_type`, `entity_id`, `relation_type`, `created_at`) VALUES (1,1,'owner',3,'signature','2026-07-15 15:31:14'),(3,3,'work_order',1,'before_photo','2026-07-16 10:40:01'),(4,4,'work_order',2,'before_photo','2026-07-16 10:42:03'),(5,5,'work_order',1,'before_photo','2026-07-16 10:47:28'),(8,8,'finance',16,'reserve_topup_proof','2026-07-16 11:31:45'),(9,1,'unit',5,'property','2026-07-16 14:47:47');
/*!40000 ALTER TABLE `document_links` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
INSERT INTO `documents` (`id`, `document_no`, `original_name`, `storage_key`, `mime_type`, `file_size`, `checksum_sha256`, `document_type`, `status`, `expires_at`, `uploaded_by`, `reviewed_by`, `reviewed_at`, `review_note`, `created_at`, `updated_at`) VALUES (1,'ADMIN-TEST-20260715-D01','Lakeview A-0301 purchase contract.pdf','test/admin/lakeview-a0301-contract.pdf','application/pdf',NULL,NULL,'purchase_contract','pending_signature',NULL,1,NULL,NULL,NULL,'2026-07-15 15:31:14','2026-07-15 15:31:14'),(3,'MNT-20260716104001-ED8A0C7C','ig_03a1961f2735210b016a407fdd3080819b9e1a7a8c4d405cfa.png','1/ed8a0c7c3706462fa22eaf6c18108836-0.png','image/png',2043709,'481020e3bc14a6d2da49edff87cce53338219daf3b355511a3cc019f1754c88d','maintenance_attachment','active',NULL,1,NULL,NULL,NULL,'2026-07-16 10:40:01','2026-07-16 10:40:01'),(4,'MNT-20260716104203-F2ADD39C','ig_03a1961f2735210b016a407fdd3080819b9e1a7a8c4d405cfa.png','2/f2add39c76254097b7927e808a84be99-0.png','image/png',2043709,'481020e3bc14a6d2da49edff87cce53338219daf3b355511a3cc019f1754c88d','maintenance_attachment','active',NULL,1,NULL,NULL,NULL,'2026-07-16 10:42:03','2026-07-16 10:42:03'),(5,'MNT-20260716104728-C1143230','ig_03a1961f2735210b016a407ff41df4819ba047542fbfbc70e9.png','1/c1143230c77b4b34b5f75d28fe055041-0.png','image/png',1782144,'142fbe741cf47f0d6ad68be72c5b07b15bc3faacd77ad0a268215992deeb58f5','maintenance_attachment','active',NULL,1,NULL,NULL,NULL,'2026-07-16 10:47:28','2026-07-16 10:47:28'),(8,'RTU-DOC-2C361DE37FFB-0','kitten_avatar_10.jpg','3/2c361de37ffb486e92ea77968e5ad68c-0.jpg','image/jpeg',38744,'8fbefde6cfca23f8b7d333b549368f64e9d146be3dc7120fa9ac3268581fec9d','reserve_topup_proof','pending_review',NULL,1,NULL,NULL,NULL,'2026-07-16 11:31:45','2026-07-16 11:31:45');
/*!40000 ALTER TABLE `documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `finance_records`
--

LOCK TABLES `finance_records` WRITE;
/*!40000 ALTER TABLE `finance_records` DISABLE KEYS */;
INSERT INTO `finance_records` (`id`, `transaction_no`, `record_type`, `unit_id`, `owner_id`, `tenant_id`, `amount`, `currency`, `transaction_date`, `payment_method`, `payment_status`, `confirmation_status`, `confirmed_by`, `confirmed_at`, `sync_status`, `sync_batch_id`, `created_by`, `created_at`, `updated_at`) VALUES (1,'TEST-20260715-F01','rent_payment',3,2,1,2800.00,'MYR','2026-06-03','bank_transfer','paid','confirmed',NULL,NULL,'not_synced',NULL,NULL,'2026-07-15 13:52:30','2026-07-15 13:52:30'),(2,'ADMIN-TEST-20260715-F01','rent_payment',6,3,3,3200.00,'MYR','2026-06-03','bank_transfer','paid','confirmed',NULL,NULL,'not_synced',NULL,NULL,'2026-07-15 14:09:59','2026-07-15 14:09:59'),(3,'ADMIN-TEST-20260715-F02','rent_payment',7,3,4,2100.00,'MYR','2026-07-01','online_payment','paid','confirmed',NULL,NULL,'not_synced',NULL,NULL,'2026-07-15 14:09:59','2026-07-15 14:09:59'),(4,'ADMIN-TEST-20260715-F03','property_payment',5,3,NULL,162500.00,'MYR','2026-07-08','bank_transfer','paid','pending',NULL,NULL,'not_synced',NULL,NULL,'2026-07-15 15:31:14','2026-07-15 15:31:14'),(5,'ADMIN-TEST-20260715-F04','property_payment',7,3,NULL,120000.00,'MYR','2026-07-10','online_payment','paid','pending',NULL,NULL,'not_synced',NULL,NULL,'2026-07-15 15:31:14','2026-07-15 15:31:14'),(6,'ADMIN-TEST-20260715-F05','rent_payment',6,3,3,3200.00,'MYR','2026-07-12','bank_transfer','paid','pending',NULL,NULL,'not_synced',NULL,NULL,'2026-07-15 15:31:14','2026-07-15 15:31:14'),(7,'ADMIN-TEST-20260716-E01','cashflow',5,3,NULL,286.40,'MYR','2026-07-15','bank_transfer','paid','confirmed',NULL,NULL,'not_synced',NULL,1,'2026-07-16 10:24:25','2026-07-16 10:24:25'),(8,'ADMIN-TEST-20260716-E02','cashflow',6,3,NULL,950.00,'MYR','2026-07-12','online_payment','paid','confirmed',NULL,NULL,'not_synced',NULL,1,'2026-07-16 10:24:25','2026-07-16 10:24:25'),(9,'ADMIN-TEST-20260716-E03','cashflow',6,3,NULL,480.00,'MYR','2026-07-10','reserve_account','paid','confirmed',NULL,NULL,'not_synced',NULL,1,'2026-07-16 10:24:25','2026-07-16 10:24:25'),(10,'ADMIN-TEST-20260716-E04','cashflow',5,3,NULL,350.00,'MYR','2026-07-09',NULL,'unpaid','pending',NULL,NULL,'not_synced',NULL,1,'2026-07-16 10:24:25','2026-07-16 10:24:25'),(11,'ADMIN-TEST-20260716-E05','cashflow',7,3,NULL,60.00,'MYR','2026-07-07','cash','paid','confirmed',NULL,NULL,'not_synced',NULL,1,'2026-07-16 10:24:25','2026-07-16 10:24:25'),(12,'ADMIN-TEST-20260716-E06','cashflow',7,3,NULL,420.00,'MYR','2026-07-05','online_payment','paid','confirmed',NULL,NULL,'not_synced',NULL,1,'2026-07-16 10:24:25','2026-07-16 10:24:25'),(13,'ADMIN-TEST-20260716-E07','cashflow',5,3,NULL,612.30,'MYR','2026-06-20','bank_transfer','paid','confirmed',NULL,NULL,'not_synced',NULL,1,'2026-07-16 10:24:25','2026-07-16 10:24:25'),(16,'RTU-20260716113145-2C361DE3','reserve_topup',7,3,NULL,1.00,'MYR','2026-07-16','online_payment','paid','pending',NULL,NULL,'not_synced',NULL,1,'2026-07-16 11:31:45','2026-07-16 11:31:45');
/*!40000 ALTER TABLE `finance_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `leases`
--

LOCK TABLES `leases` WRITE;
/*!40000 ALTER TABLE `leases` DISABLE KEYS */;
INSERT INTO `leases` (`id`, `unit_id`, `tenant_id`, `lease_no`, `start_date`, `end_date`, `monthly_rent`, `deposit_amount`, `payment_day`, `status`, `contract_document_id`, `created_at`, `updated_at`) VALUES (1,3,1,'TEST-20260715-L01','2026-01-01','2026-12-31',2800.00,5600.00,5,'active',NULL,'2026-07-15 13:52:30','2026-07-15 13:52:30'),(2,6,3,'ADMIN-TEST-20260715-L01','2026-03-01','2027-02-28',3200.00,6400.00,5,'active',NULL,'2026-07-15 14:09:59','2026-07-15 14:09:59'),(3,7,4,'ADMIN-TEST-20260715-L02','2026-04-01','2027-03-31',2100.00,4200.00,1,'active',NULL,'2026-07-15 14:09:59','2026-07-15 14:09:59');
/*!40000 ALTER TABLE `leases` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `maintenance_status_history`
--

LOCK TABLES `maintenance_status_history` WRITE;
/*!40000 ALTER TABLE `maintenance_status_history` DISABLE KEYS */;
INSERT INTO `maintenance_status_history` (`id`, `work_order_id`, `status`, `occurred_at`, `note`, `changed_by`, `created_at`) VALUES (1,1,'submitted','2026-07-10 10:30:00','業主提交報修',1,'2026-07-16 10:24:25'),(2,1,'assigned','2026-07-10 11:00:00','已指派 ADMIN Test Property Care',1,'2026-07-16 10:24:25'),(3,1,'in_progress','2026-07-11 09:30:00','維修員已開始處理',1,'2026-07-16 10:24:25'),(4,2,'submitted','2026-07-09 14:15:00','業主提交報修',1,'2026-07-16 10:24:25');
/*!40000 ALTER TABLE `maintenance_status_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `maintenance_work_orders`
--

LOCK TABLES `maintenance_work_orders` WRITE;
/*!40000 ALTER TABLE `maintenance_work_orders` DISABLE KEYS */;
INSERT INTO `maintenance_work_orders` (`id`, `work_order_no`, `unit_id`, `owner_id`, `tenant_id`, `vendor_id`, `cashflow_entry_id`, `category`, `title`, `description`, `requested_at`, `completed_at`, `status`, `estimated_amount`, `actual_amount`, `created_by`, `created_at`, `updated_at`) VALUES (1,'ADMIN-TEST-20260716-M01',6,3,NULL,2,3,'plumbing','廚房水管漏水維修','廚房水管接口持續滲水，需要更換接頭及密封件。','2026-07-10 10:30:00',NULL,'in_progress',500.00,480.00,1,'2026-07-16 10:24:25','2026-07-16 10:24:25'),(2,'ADMIN-TEST-20260716-M02',5,3,NULL,2,4,'air_conditioning','客廳冷氣檢查','客廳冷氣制冷不足，等待安排維修員上門。','2026-07-09 14:15:00',NULL,'submitted',350.00,NULL,1,'2026-07-16 10:24:25','2026-07-16 10:24:25');
/*!40000 ALTER TABLE `maintenance_work_orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `notification_deliveries`
--

LOCK TABLES `notification_deliveries` WRITE;
/*!40000 ALTER TABLE `notification_deliveries` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification_deliveries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `notification_rules`
--

LOCK TABLES `notification_rules` WRITE;
/*!40000 ALTER TABLE `notification_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` (`id`, `rule_id`, `recipient_user_id`, `recipient_owner_id`, `title`, `body`, `related_type`, `related_id`, `priority`, `status`, `read_at`, `created_at`) VALUES (1,NULL,1,3,'2026 年 7 月租金已入帳','Central Suites B-0602 租金 RM 2,100.00 已完成確認。',NULL,NULL,'normal','read','2026-07-16 14:00:23','2026-07-15 09:30:00'),(2,NULL,1,3,'房款即將到期','Central Suites B-0602 下一期房款將於 2026-07-20 到期。',NULL,NULL,'high','read','2026-07-16 14:00:21','2026-07-14 10:00:00'),(3,NULL,1,3,'預備金帳戶已更新','您名下 4 個單位的預備金餘額已完成同步。',NULL,NULL,'normal','read',NULL,'2026-07-12 16:20:00');
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `owner_units`
--

LOCK TABLES `owner_units` WRITE;
/*!40000 ALTER TABLE `owner_units` DISABLE KEYS */;
INSERT INTO `owner_units` (`id`, `owner_id`, `unit_id`, `ownership_percent`, `is_primary`, `start_date`, `end_date`, `status`, `created_at`) VALUES (1,1,1,100.00,1,'2026-01-01',NULL,'active','2026-07-15 13:52:30'),(2,2,2,100.00,1,'2026-01-01',NULL,'active','2026-07-15 13:52:30'),(3,3,5,100.00,1,'2026-01-01',NULL,'active','2026-07-15 14:09:59'),(4,3,6,100.00,1,'2026-01-01',NULL,'active','2026-07-15 14:09:59'),(5,3,7,100.00,1,'2026-02-01',NULL,'active','2026-07-15 14:09:59'),(6,3,8,100.00,1,'2026-02-01',NULL,'active','2026-07-15 14:09:59');
/*!40000 ALTER TABLE `owner_units` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `owners`
--

LOCK TABLES `owners` WRITE;
/*!40000 ALTER TABLE `owners` DISABLE KEYS */;
INSERT INTO `owners` (`id`, `user_id`, `full_name`, `identity_no`, `phone`, `email`, `status`, `created_at`, `updated_at`) VALUES (1,1,'TEST Owner One',NULL,'+60110000001','test.owner1@example.com','active','2026-07-15 13:52:30','2026-07-15 14:09:59'),(2,NULL,'TEST Owner Two',NULL,'+60110000002','test.owner2@example.com','active','2026-07-15 13:52:30','2026-07-15 13:52:30'),(3,1,'ADMIN Test Portfolio Owner',NULL,'+60110000999','admin.test.owner@example.com','active','2026-07-15 14:09:59','2026-07-15 14:09:59');
/*!40000 ALTER TABLE `owners` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `payment_installments`
--

LOCK TABLES `payment_installments` WRITE;
/*!40000 ALTER TABLE `payment_installments` DISABLE KEYS */;
INSERT INTO `payment_installments` (`id`, `payment_plan_id`, `installment_no`, `milestone`, `due_date`, `amount_due`, `amount_paid`, `status`, `created_at`, `updated_at`) VALUES (1,1,1,'Deposit','2026-01-15',162500.00,162500.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(2,1,2,'Foundation','2026-04-15',162500.00,162500.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(3,1,3,'Structure','2026-09-15',162500.00,0.00,'pending','2026-07-15 15:31:14','2026-07-15 15:31:14'),(4,1,4,'Handover','2026-12-15',162500.00,0.00,'pending','2026-07-15 15:31:14','2026-07-15 15:31:14'),(5,2,1,'Deposit','2026-01-15',245000.00,245000.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(6,2,2,'Foundation','2026-03-15',245000.00,245000.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(7,2,3,'Structure','2026-05-15',245000.00,245000.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(8,2,4,'Handover','2026-07-01',245000.00,245000.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(9,3,1,'Deposit','2026-02-15',120000.00,120000.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(10,3,2,'Foundation','2026-07-20',120000.00,0.00,'pending','2026-07-15 15:31:14','2026-07-15 15:31:14'),(11,3,3,'Structure','2026-10-20',120000.00,0.00,'pending','2026-07-15 15:31:14','2026-07-15 15:31:14'),(12,3,4,'Handover','2027-01-20',120000.00,0.00,'pending','2026-07-15 15:31:14','2026-07-15 15:31:14'),(13,4,1,'Deposit','2026-02-15',177500.00,177500.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(14,4,2,'Foundation','2026-06-15',177500.00,0.00,'overdue','2026-07-15 15:31:14','2026-07-15 15:31:14'),(15,4,3,'Structure','2026-10-15',177500.00,0.00,'pending','2026-07-15 15:31:14','2026-07-15 15:31:14'),(16,4,4,'Handover','2027-02-15',177500.00,0.00,'pending','2026-07-15 15:31:14','2026-07-15 15:31:14');
/*!40000 ALTER TABLE `payment_installments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `payment_plans`
--

LOCK TABLES `payment_plans` WRITE;
/*!40000 ALTER TABLE `payment_plans` DISABLE KEYS */;
INSERT INTO `payment_plans` (`id`, `purchase_contract_id`, `plan_name`, `installment_count`, `total_amount`, `start_date`, `status`, `created_at`) VALUES (1,1,'ADMIN Test Standard Plan',4,650000.00,'2026-01-15','active','2026-07-15 15:31:14'),(2,2,'ADMIN Test Standard Plan',4,980000.00,'2026-01-15','active','2026-07-15 15:31:14'),(3,3,'ADMIN Test Standard Plan',4,480000.00,'2026-02-15','active','2026-07-15 15:31:14'),(4,4,'ADMIN Test Standard Plan',4,710000.00,'2026-02-15','active','2026-07-15 15:31:14');
/*!40000 ALTER TABLE `payment_plans` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `payment_receipt_allocations`
--

LOCK TABLES `payment_receipt_allocations` WRITE;
/*!40000 ALTER TABLE `payment_receipt_allocations` DISABLE KEYS */;
/*!40000 ALTER TABLE `payment_receipt_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `payment_receipts`
--

LOCK TABLES `payment_receipts` WRITE;
/*!40000 ALTER TABLE `payment_receipts` DISABLE KEYS */;
INSERT INTO `payment_receipts` (`id`, `finance_record_id`, `receipt_no`, `payer_name`, `bank_reference`, `proof_document_id`, `submission_note`, `review_note`, `created_at`) VALUES (2,16,'RTU-RCP-20260716113145-7FFB486E','1','1 | 1',8,NULL,NULL,'2026-07-16 11:31:45');
/*!40000 ALTER TABLE `payment_receipts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `permissions`
--

LOCK TABLES `permissions` WRITE;
/*!40000 ALTER TABLE `permissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `projects`
--

LOCK TABLES `projects` WRITE;
/*!40000 ALTER TABLE `projects` DISABLE KEYS */;
INSERT INTO `projects` (`id`, `project_code`, `name`, `address`, `city`, `country_code`, `status`, `created_at`, `updated_at`) VALUES (1,'TEST-20260715-P01','TEST Riverside Residence','100 Test River Road','Kuala Lumpur','MY','active','2026-07-15 13:52:30','2026-07-15 13:52:30'),(2,'TEST-20260715-P02','TEST Garden Suites','200 Test Garden Avenue','Petaling Jaya','MY','active','2026-07-15 13:52:30','2026-07-15 13:52:30'),(3,'ADMIN-TEST-20260715-P01','ADMIN Test Lakeview Towers','10 Admin Test Lakeview Road','Kuala Lumpur','MY','active','2026-07-15 14:09:59','2026-07-15 14:09:59'),(4,'ADMIN-TEST-20260715-P02','ADMIN Test Central Suites','20 Admin Test Central Street','Petaling Jaya','MY','active','2026-07-15 14:09:59','2026-07-15 14:09:59');
/*!40000 ALTER TABLE `projects` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `properties`
--

LOCK TABLES `properties` WRITE;
/*!40000 ALTER TABLE `properties` DISABLE KEYS */;
INSERT INTO `properties` (`id`, `address`, `area`, `bedrooms`, `created_at`, `name`, `price`, `project_name`, `status`, `updated_at`) VALUES (1,'100 Test River Road, Kuala Lumpur',79,2,'2026-07-15 13:52:30.000000','TEST Riverside Residence A-0801',680000.00,'TEST Riverside Residence','AVAILABLE','2026-07-15 13:52:30.000000'),(2,'100 Test River Road, Kuala Lumpur',102,3,'2026-07-15 13:52:30.000000','TEST Riverside Residence A-1208',920000.00,'TEST Riverside Residence','RESERVED','2026-07-15 13:52:30.000000'),(3,'200 Test Garden Avenue, Petaling Jaya',81,2,'2026-07-15 13:52:30.000000','TEST Garden Suites B-1802',720000.00,'TEST Garden Suites','AVAILABLE','2026-07-15 13:52:30.000000'),(4,'10 Admin Test Lakeview Road, Kuala Lumpur',76,2,'2026-07-15 14:09:59.000000','ADMIN Test Lakeview A-0301',650000.00,'ADMIN Test Lakeview Towers','AVAILABLE','2026-07-15 14:09:59.000000'),(5,'10 Admin Test Lakeview Road, Kuala Lumpur',110,3,'2026-07-15 14:09:59.000000','ADMIN Test Lakeview A-0906',980000.00,'ADMIN Test Lakeview Towers','SOLD','2026-07-15 14:09:59.000000'),(6,'20 Admin Test Central Street, Petaling Jaya',48,1,'2026-07-15 14:09:59.000000','ADMIN Test Central B-0602',480000.00,'ADMIN Test Central Suites','RESERVED','2026-07-15 14:09:59.000000'),(7,'20 Admin Test Central Street, Petaling Jaya',84,2,'2026-07-15 14:09:59.000000','ADMIN Test Central B-1508',710000.00,'ADMIN Test Central Suites','AVAILABLE','2026-07-15 14:09:59.000000');
/*!40000 ALTER TABLE `properties` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `purchase_contracts`
--

LOCK TABLES `purchase_contracts` WRITE;
/*!40000 ALTER TABLE `purchase_contracts` DISABLE KEYS */;
INSERT INTO `purchase_contracts` (`id`, `owner_unit_id`, `contract_no`, `purchase_price`, `currency`, `signed_date`, `handover_date`, `status`, `created_at`, `updated_at`) VALUES (1,3,'ADMIN-TEST-20260715-C01',650000.00,'MYR','2026-01-01',NULL,'active','2026-07-15 15:31:14','2026-07-15 15:31:14'),(2,4,'ADMIN-TEST-20260715-C02',980000.00,'MYR','2026-01-01',NULL,'active','2026-07-15 15:31:14','2026-07-15 15:31:14'),(3,5,'ADMIN-TEST-20260715-C03',480000.00,'MYR','2026-02-01',NULL,'active','2026-07-15 15:31:14','2026-07-15 15:31:14'),(4,6,'ADMIN-TEST-20260715-C04',710000.00,'MYR','2026-02-01',NULL,'active','2026-07-15 15:31:14','2026-07-15 15:31:14');
/*!40000 ALTER TABLE `purchase_contracts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `rent_invoices`
--

LOCK TABLES `rent_invoices` WRITE;
/*!40000 ALTER TABLE `rent_invoices` DISABLE KEYS */;
INSERT INTO `rent_invoices` (`id`, `lease_id`, `billing_month`, `due_date`, `amount_due`, `amount_paid`, `status`, `created_at`, `updated_at`) VALUES (1,1,'2026-06-01','2026-06-05',2800.00,2800.00,'paid','2026-07-15 13:52:30','2026-07-15 13:52:30'),(2,1,'2026-07-01','2026-07-05',2800.00,0.00,'overdue','2026-07-15 13:52:30','2026-07-15 13:52:30'),(3,2,'2026-06-01','2026-06-05',3200.00,3200.00,'paid','2026-07-15 14:09:59','2026-07-15 14:09:59'),(4,2,'2026-07-01','2026-07-05',3200.00,0.00,'overdue','2026-07-15 14:09:59','2026-07-15 14:09:59'),(5,3,'2026-07-01','2026-07-01',2100.00,2100.00,'paid','2026-07-15 14:09:59','2026-07-15 14:09:59');
/*!40000 ALTER TABLE `rent_invoices` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `rent_payments`
--

LOCK TABLES `rent_payments` WRITE;
/*!40000 ALTER TABLE `rent_payments` DISABLE KEYS */;
INSERT INTO `rent_payments` (`id`, `rent_invoice_id`, `finance_record_id`, `created_at`) VALUES (1,1,1,'2026-07-15 13:52:30'),(2,3,2,'2026-07-15 17:47:26'),(3,5,3,'2026-07-15 17:47:26'),(4,4,6,'2026-07-15 17:47:26');
/*!40000 ALTER TABLE `rent_payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `report_definitions`
--

LOCK TABLES `report_definitions` WRITE;
/*!40000 ALTER TABLE `report_definitions` DISABLE KEYS */;
/*!40000 ALTER TABLE `report_definitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `report_runs`
--

LOCK TABLES `report_runs` WRITE;
/*!40000 ALTER TABLE `report_runs` DISABLE KEYS */;
/*!40000 ALTER TABLE `report_runs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `reserve_accounts`
--

LOCK TABLES `reserve_accounts` WRITE;
/*!40000 ALTER TABLE `reserve_accounts` DISABLE KEYS */;
INSERT INTO `reserve_accounts` (`id`, `owner_unit_id`, `minimum_balance`, `current_balance`, `status`, `low_balance_alert_enabled`, `created_at`, `updated_at`) VALUES (1,3,2000.00,4500.00,'active',1,'2026-07-15 15:31:14','2026-07-16 11:26:24'),(2,4,2500.00,3380.50,'active',1,'2026-07-15 15:31:14','2026-07-16 10:24:25'),(3,5,1500.00,1500.00,'active',1,'2026-07-15 15:31:14','2026-07-15 15:31:14'),(4,6,2000.00,3000.00,'active',1,'2026-07-15 15:31:14','2026-07-15 15:31:14');
/*!40000 ALTER TABLE `reserve_accounts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `reserve_transactions`
--

LOCK TABLES `reserve_transactions` WRITE;
/*!40000 ALTER TABLE `reserve_transactions` DISABLE KEYS */;
INSERT INTO `reserve_transactions` (`id`, `reserve_account_id`, `finance_record_id`, `maintenance_work_order_id`, `transaction_type`, `amount`, `occurred_at`, `balance_after`, `note`, `created_by`, `created_at`) VALUES (1,2,9,1,'debit',480.00,'2026-07-12 09:00:00',3380.50,'廚房水管維修費由預備金扣除',1,'2026-07-16 10:24:25'),(2,1,NULL,NULL,'topup',4500.00,'2026-03-01 09:00:00',4500.00,'ADMIN opening reserve balance A-0301',1,'2026-07-16 11:19:24'),(3,2,NULL,NULL,'topup',3860.50,'2026-03-01 09:10:00',3860.50,'ADMIN opening reserve balance A-0906',1,'2026-07-16 11:19:24'),(4,3,NULL,NULL,'topup',1500.00,'2026-04-01 09:00:00',1500.00,'ADMIN opening reserve balance B-0602',1,'2026-07-16 11:19:24'),(5,4,NULL,NULL,'topup',3000.00,'2026-04-01 09:10:00',3000.00,'ADMIN opening reserve balance B-1508',1,'2026-07-16 11:19:24');
/*!40000 ALTER TABLE `reserve_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `role_permissions`
--

LOCK TABLES `role_permissions` WRITE;
/*!40000 ALTER TABLE `role_permissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `role_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` (`id`, `code`, `name`) VALUES (1,'ADMIN','System Administrator');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `sync_batch_items`
--

LOCK TABLES `sync_batch_items` WRITE;
/*!40000 ALTER TABLE `sync_batch_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `sync_batch_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `sync_batches`
--

LOCK TABLES `sync_batches` WRITE;
/*!40000 ALTER TABLE `sync_batches` DISABLE KEYS */;
/*!40000 ALTER TABLE `sync_batches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `tenants`
--

LOCK TABLES `tenants` WRITE;
/*!40000 ALTER TABLE `tenants` DISABLE KEYS */;
INSERT INTO `tenants` (`id`, `user_id`, `full_name`, `identity_no`, `phone`, `email`, `status`, `created_at`, `updated_at`) VALUES (1,NULL,'TEST Tenant One',NULL,'+60110000011','test.tenant1@example.com','active','2026-07-15 13:52:30','2026-07-15 13:52:30'),(2,NULL,'TEST Tenant Two',NULL,'+60110000012','test.tenant2@example.com','active','2026-07-15 13:52:30','2026-07-15 13:52:30'),(3,NULL,'ADMIN Test Tenant One',NULL,'+60110000901','admin.test.tenant1@example.com','active','2026-07-15 14:09:59','2026-07-15 14:09:59'),(4,NULL,'ADMIN Test Tenant Two',NULL,'+60110000902','admin.test.tenant2@example.com','active','2026-07-15 14:09:59','2026-07-15 14:09:59');
/*!40000 ALTER TABLE `tenants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `units`
--

LOCK TABLES `units` WRITE;
/*!40000 ALTER TABLE `units` DISABLE KEYS */;
INSERT INTO `units` (`id`, `project_id`, `building`, `floor_no`, `unit_no`, `unit_type`, `area_sqm`, `bedroom_count`, `listing_status`, `created_at`, `updated_at`) VALUES (1,1,'A','08','A-0801','2BR',78.50,2,'available','2026-07-15 13:52:30','2026-07-15 13:52:30'),(2,1,'A','12','A-1208','3BR',102.00,3,'reserved','2026-07-15 13:52:30','2026-07-15 13:52:30'),(3,2,'B','05','B-0503','1BR',52.00,1,'occupied','2026-07-15 13:52:30','2026-07-15 13:52:30'),(4,2,'B','18','B-1802','2BR',81.25,2,'available','2026-07-15 13:52:30','2026-07-15 13:52:30'),(5,3,'A','03','ADMIN-A-0301','2BR',75.50,2,'available','2026-07-15 14:09:59','2026-07-15 14:09:59'),(6,3,'A','09','ADMIN-A-0906','3BR',110.00,3,'occupied','2026-07-15 14:09:59','2026-07-15 14:09:59'),(7,4,'B','06','ADMIN-B-0602','1BR',48.00,1,'reserved','2026-07-15 14:09:59','2026-07-15 14:09:59'),(8,4,'B','15','ADMIN-B-1508','2BR',84.25,2,'available','2026-07-15 14:09:59','2026-07-15 14:09:59');
/*!40000 ALTER TABLE `units` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES (1,1);
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` (`id`, `username`, `email`, `password_hash`, `display_name`, `phone`, `status`, `last_login_at`, `created_at`, `updated_at`) VALUES (1,'admin','admin@example.com','$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW','System Administrator',NULL,'active','2026-07-16 14:50:13','2026-07-15 14:01:33','2026-07-15 14:01:33');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `vendors`
--

LOCK TABLES `vendors` WRITE;
/*!40000 ALTER TABLE `vendors` DISABLE KEYS */;
INSERT INTO `vendors` (`id`, `vendor_code`, `name`, `contact_name`, `phone`, `email`, `status`, `created_at`, `updated_at`) VALUES (1,'TEST-20260715-V01','TEST Bright Maintenance','TEST Vendor Contact','+60110000021','test.vendor@example.com','active','2026-07-15 13:52:30','2026-07-15 13:52:30'),(2,'ADMIN-TEST-20260716-V01','ADMIN Test Property Care','Maintenance Desk','+60110000888','maintenance@example.com','active','2026-07-16 10:24:25','2026-07-16 10:24:25');
/*!40000 ALTER TABLE `vendors` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-16 18:18:38
