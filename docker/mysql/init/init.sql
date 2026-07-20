-- MySQL dump 10.13  Distrib 5.7.37, for Win64 (x86_64)
--
-- Host: localhost    Database: ccps_property_management
-- ------------------------------------------------------
-- Server version	5.7.37

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `audit_logs`
--

DROP TABLE IF EXISTS `audit_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `audit_logs` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `actor_user_id` bigint(20) unsigned DEFAULT NULL,
  `action` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity_id` bigint(20) unsigned DEFAULT NULL,
  `before_data` json DEFAULT NULL,
  `after_data` json DEFAULT NULL,
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_logs_entity` (`entity_type`,`entity_id`,`created_at`),
  KEY `idx_audit_logs_actor` (`actor_user_id`,`created_at`),
  CONSTRAINT `fk_audit_logs_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_logs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cashflow_entries`
--

DROP TABLE IF EXISTS `cashflow_entries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cashflow_entries` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `finance_record_id` bigint(20) unsigned NOT NULL,
  `unit_id` bigint(20) unsigned DEFAULT NULL,
  `owner_id` bigint(20) unsigned DEFAULT NULL,
  `tenant_id` bigint(20) unsigned DEFAULT NULL,
  `vendor_id` bigint(20) unsigned DEFAULT NULL,
  `direction` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'income / expense',
  `category` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'rent / maintenance / utilities / management / other',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `occurred_on` date NOT NULL,
  `reserve_account_id` bigint(20) unsigned DEFAULT NULL,
  `attachment_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'missing',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cashflow_finance` (`finance_record_id`),
  KEY `idx_cashflow_date_category` (`occurred_on`,`category`),
  KEY `idx_cashflow_unit` (`unit_id`,`occurred_on`),
  KEY `fk_cashflow_owner` (`owner_id`),
  KEY `fk_cashflow_tenant` (`tenant_id`),
  KEY `fk_cashflow_vendor` (`vendor_id`),
  KEY `fk_cashflow_reserve_account` (`reserve_account_id`),
  CONSTRAINT `fk_cashflow_finance` FOREIGN KEY (`finance_record_id`) REFERENCES `finance_records` (`id`),
  CONSTRAINT `fk_cashflow_owner` FOREIGN KEY (`owner_id`) REFERENCES `owners` (`id`),
  CONSTRAINT `fk_cashflow_reserve_account` FOREIGN KEY (`reserve_account_id`) REFERENCES `reserve_accounts` (`id`),
  CONSTRAINT `fk_cashflow_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`),
  CONSTRAINT `fk_cashflow_unit` FOREIGN KEY (`unit_id`) REFERENCES `units` (`id`),
  CONSTRAINT `fk_cashflow_vendor` FOREIGN KEY (`vendor_id`) REFERENCES `vendors` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cashflow_entries`
--

LOCK TABLES `cashflow_entries` WRITE;
/*!40000 ALTER TABLE `cashflow_entries` DISABLE KEYS */;
INSERT INTO `cashflow_entries` (`id`, `finance_record_id`, `unit_id`, `owner_id`, `tenant_id`, `vendor_id`, `direction`, `category`, `description`, `occurred_on`, `reserve_account_id`, `attachment_status`, `created_at`, `updated_at`) VALUES (1,7,5,3,NULL,NULL,'expense','utilities','2026年7月水費賬單','2026-07-15',1,'not_required','2026-07-16 10:24:25','2026-07-17 15:16:06'),(2,8,6,3,NULL,NULL,'expense','management','2026年7月管理費','2026-07-12',2,'not_required','2026-07-16 10:24:25','2026-07-17 15:16:06'),(3,9,6,3,NULL,2,'expense','maintenance','廚房水管漏水維修更換','2026-07-10',2,'not_required','2026-07-16 10:24:25','2026-07-17 15:17:58'),(4,10,5,3,NULL,2,'expense','maintenance','客廳冷氣不冷，檢查並加注冷媒','2026-07-09',NULL,'missing','2026-07-16 10:24:25','2026-07-17 15:25:36'),(5,11,7,3,NULL,NULL,'expense','other','門禁卡補辦費用（2張）','2026-07-07',3,'not_required','2026-07-16 10:24:25','2026-07-17 15:16:06'),(6,12,7,3,NULL,NULL,'expense','cleaning','公共區域清潔費','2026-07-05',3,'not_required','2026-07-16 10:24:25','2026-07-17 15:16:06'),(7,13,5,3,NULL,NULL,'expense','utilities','2026年6月電費賬單','2026-06-20',1,'not_required','2026-07-16 10:24:25','2026-07-17 15:16:06'),(8,17,12,1,5,NULL,'income','rent','Monthly rental received for KL Sentral Loft E-09-03','2026-07-03',NULL,'complete','2026-07-17 13:58:13','2026-07-17 13:58:13'),(9,18,13,1,6,NULL,'income','rent','Partial monthly rental received for Mont Kiara Verde F-16-06','2026-07-08',NULL,'complete','2026-07-17 13:58:13','2026-07-17 13:58:13'),(10,19,12,1,NULL,NULL,'expense','management','Monthly management fee and common-area charges','2026-07-06',5,'not_required','2026-07-17 13:58:13','2026-07-17 15:16:06'),(11,20,13,1,NULL,NULL,'expense','maintenance','Plumbing inspection and emergency repair deposit','2026-07-10',6,'not_required','2026-07-17 13:58:13','2026-07-17 15:16:06'),(12,21,14,1,NULL,NULL,'expense','management','Management fee and pre-sale property preparation','2026-07-07',7,'not_required','2026-07-17 13:58:13','2026-07-17 15:16:06');
/*!40000 ALTER TABLE `cashflow_entries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `document_links`
--

DROP TABLE IF EXISTS `document_links`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `document_links` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `document_id` bigint(20) unsigned NOT NULL,
  `entity_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'owner / tenant / unit / contract / lease / finance / cashflow / work_order / reserve_transaction',
  `entity_id` bigint(20) unsigned NOT NULL,
  `relation_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'attachment',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_document_links` (`document_id`,`entity_type`,`entity_id`,`relation_type`),
  KEY `idx_document_links_entity` (`entity_type`,`entity_id`),
  CONSTRAINT `fk_document_links_document` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document_links`
--

LOCK TABLES `document_links` WRITE;
/*!40000 ALTER TABLE `document_links` DISABLE KEYS */;
INSERT INTO `document_links` (`id`, `document_id`, `entity_type`, `entity_id`, `relation_type`, `created_at`) VALUES (1,1,'owner',3,'signature','2026-07-15 15:31:14'),(3,3,'work_order',1,'before_photo','2026-07-16 10:40:01'),(4,4,'work_order',2,'before_photo','2026-07-16 10:42:03'),(5,5,'work_order',1,'before_photo','2026-07-16 10:47:28'),(8,8,'finance',16,'reserve_topup_proof','2026-07-16 11:31:45'),(9,1,'unit',5,'property','2026-07-16 14:47:47'),(10,9,'finance',27,'payment_proof','2026-07-17 14:23:50'),(11,10,'finance',28,'reserve_topup_proof','2026-07-17 14:31:09'),(12,11,'work_order',5,'invoice','2026-07-17 14:53:39');
/*!40000 ALTER TABLE `document_links` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `documents`
--

DROP TABLE IF EXISTS `documents`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `documents` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `document_no` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `storage_key` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mime_type` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_size` bigint(20) unsigned DEFAULT NULL,
  `checksum_sha256` char(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `document_type` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'purchase_contract / payment_proof / lease / invoice / maintenance_attachment / identity',
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending_review',
  `expires_at` date DEFAULT NULL,
  `uploaded_by` bigint(20) unsigned DEFAULT NULL,
  `reviewed_by` bigint(20) unsigned DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `review_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_documents_no` (`document_no`),
  KEY `idx_documents_expiry` (`expires_at`,`status`),
  KEY `idx_documents_type_status` (`document_type`,`status`),
  KEY `fk_documents_uploader` (`uploaded_by`),
  KEY `fk_documents_reviewer` (`reviewed_by`),
  CONSTRAINT `fk_documents_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_documents_uploader` FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
INSERT INTO `documents` (`id`, `document_no`, `original_name`, `storage_key`, `mime_type`, `file_size`, `checksum_sha256`, `document_type`, `status`, `expires_at`, `uploaded_by`, `reviewed_by`, `reviewed_at`, `review_note`, `created_at`, `updated_at`) VALUES (1,'ADMIN-TEST-20260715-D01','Lakeview A-0301 purchase contract.pdf','test/admin/lakeview-a0301-contract.pdf','application/pdf',NULL,NULL,'purchase_contract','pending_signature',NULL,1,NULL,NULL,NULL,'2026-07-15 15:31:14','2026-07-15 15:31:14'),(3,'MNT-20260716104001-ED8A0C7C','ig_03a1961f2735210b016a407fdd3080819b9e1a7a8c4d405cfa.png','1/ed8a0c7c3706462fa22eaf6c18108836-0.png','image/png',2043709,'481020e3bc14a6d2da49edff87cce53338219daf3b355511a3cc019f1754c88d','maintenance_attachment','active',NULL,1,NULL,NULL,NULL,'2026-07-16 10:40:01','2026-07-16 10:40:01'),(4,'MNT-20260716104203-F2ADD39C','ig_03a1961f2735210b016a407fdd3080819b9e1a7a8c4d405cfa.png','2/f2add39c76254097b7927e808a84be99-0.png','image/png',2043709,'481020e3bc14a6d2da49edff87cce53338219daf3b355511a3cc019f1754c88d','maintenance_attachment','active',NULL,1,NULL,NULL,NULL,'2026-07-16 10:42:03','2026-07-16 10:42:03'),(5,'MNT-20260716104728-C1143230','ig_03a1961f2735210b016a407ff41df4819ba047542fbfbc70e9.png','1/c1143230c77b4b34b5f75d28fe055041-0.png','image/png',1782144,'142fbe741cf47f0d6ad68be72c5b07b15bc3faacd77ad0a268215992deeb58f5','maintenance_attachment','active',NULL,1,NULL,NULL,NULL,'2026-07-16 10:47:28','2026-07-16 10:47:28'),(8,'RTU-DOC-2C361DE37FFB-0','kitten_avatar_10.jpg','3/2c361de37ffb486e92ea77968e5ad68c-0.jpg','image/jpeg',38744,'8fbefde6cfca23f8b7d333b549368f64e9d146be3dc7120fa9ac3268581fec9d','reserve_topup_proof','pending_review',NULL,1,NULL,NULL,NULL,'2026-07-16 11:31:45','2026-07-16 11:31:45'),(9,'DOC-330107CE7DFA-0','kitten_avatar_10.jpg','7/330107ce7dfa498eb5552c829c07ad19-0.jpg','image/jpeg',38744,'8fbefde6cfca23f8b7d333b549368f64e9d146be3dc7120fa9ac3268581fec9d','payment_proof','pending_review',NULL,4,NULL,NULL,NULL,'2026-07-17 14:23:50','2026-07-17 14:23:50'),(10,'RTU-DOC-A5F6887A3547-0','kitten_avatar_10.jpg','6/a5f6887a354743ecb53640e2a0794852-0.jpg','image/jpeg',38744,'8fbefde6cfca23f8b7d333b549368f64e9d146be3dc7120fa9ac3268581fec9d','reserve_topup_proof','pending_review',NULL,4,NULL,NULL,NULL,'2026-07-17 14:31:09','2026-07-17 14:31:09'),(11,'MNT-20260717145339-A3786FF3','kitten_avatar_09.jpg','5/a3786ff3cfd94d149db687968296a979-0.jpg','image/jpeg',35616,'f94c99249b75e57825494b6541e3f3f57daf79786c383173cb40812b730bf57b','maintenance_attachment','active',NULL,4,NULL,NULL,NULL,'2026-07-17 14:53:39','2026-07-17 14:53:39');
/*!40000 ALTER TABLE `documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `finance_records`
--

DROP TABLE IF EXISTS `finance_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `finance_records` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `transaction_no` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `record_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'property_payment / rent_payment / cashflow / reserve_topup / reserve_debit',
  `unit_id` bigint(20) unsigned DEFAULT NULL,
  `owner_id` bigint(20) unsigned DEFAULT NULL,
  `tenant_id` bigint(20) unsigned DEFAULT NULL,
  `amount` decimal(18,2) NOT NULL,
  `currency` char(3) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MYR',
  `transaction_date` date NOT NULL,
  `payment_method` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'unpaid',
  `confirmation_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `confirmed_by` bigint(20) unsigned DEFAULT NULL,
  `confirmed_at` datetime DEFAULT NULL,
  `sync_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'not_synced',
  `sync_batch_id` bigint(20) unsigned DEFAULT NULL,
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_finance_records_no` (`transaction_no`),
  KEY `idx_finance_records_review` (`confirmation_status`,`transaction_date`),
  KEY `idx_finance_records_unit_date` (`unit_id`,`transaction_date`),
  KEY `idx_finance_records_sync` (`sync_status`,`sync_batch_id`),
  KEY `fk_finance_records_owner` (`owner_id`),
  KEY `fk_finance_records_tenant` (`tenant_id`),
  KEY `fk_finance_records_confirmed_by` (`confirmed_by`),
  KEY `fk_finance_records_sync_batch` (`sync_batch_id`),
  KEY `fk_finance_records_creator` (`created_by`),
  CONSTRAINT `fk_finance_records_confirmed_by` FOREIGN KEY (`confirmed_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_finance_records_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_finance_records_owner` FOREIGN KEY (`owner_id`) REFERENCES `owners` (`id`),
  CONSTRAINT `fk_finance_records_sync_batch` FOREIGN KEY (`sync_batch_id`) REFERENCES `sync_batches` (`id`),
  CONSTRAINT `fk_finance_records_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`),
  CONSTRAINT `fk_finance_records_unit` FOREIGN KEY (`unit_id`) REFERENCES `units` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `finance_records`
--

LOCK TABLES `finance_records` WRITE;
/*!40000 ALTER TABLE `finance_records` DISABLE KEYS */;
INSERT INTO `finance_records` (`id`, `transaction_no`, `record_type`, `unit_id`, `owner_id`, `tenant_id`, `amount`, `currency`, `transaction_date`, `payment_method`, `payment_status`, `confirmation_status`, `confirmed_by`, `confirmed_at`, `sync_status`, `sync_batch_id`, `created_by`, `created_at`, `updated_at`) VALUES (1,'TEST-20260715-F01','rent_payment',3,2,1,2800.00,'MYR','2026-06-03','bank_transfer','paid','confirmed',NULL,NULL,'not_synced',NULL,NULL,'2026-07-15 13:52:30','2026-07-15 13:52:30'),(2,'ADMIN-TEST-20260715-F01','rent_payment',6,3,3,3200.00,'MYR','2026-06-03','bank_transfer','paid','confirmed',NULL,NULL,'not_synced',NULL,NULL,'2026-07-15 14:09:59','2026-07-15 14:09:59'),(3,'ADMIN-TEST-20260715-F02','rent_payment',7,3,4,2100.00,'MYR','2026-07-01','online_payment','paid','confirmed',NULL,NULL,'not_synced',NULL,NULL,'2026-07-15 14:09:59','2026-07-15 14:09:59'),(4,'ADMIN-TEST-20260715-F03','property_payment',5,3,NULL,162500.00,'MYR','2026-07-08','bank_transfer','paid','pending',NULL,NULL,'not_synced',NULL,NULL,'2026-07-15 15:31:14','2026-07-15 15:31:14'),(5,'ADMIN-TEST-20260715-F04','property_payment',7,3,NULL,120000.00,'MYR','2026-07-10','online_payment','paid','pending',NULL,NULL,'not_synced',NULL,NULL,'2026-07-15 15:31:14','2026-07-15 15:31:14'),(6,'ADMIN-TEST-20260715-F05','rent_payment',6,3,3,3200.00,'MYR','2026-07-12','bank_transfer','paid','pending',NULL,NULL,'not_synced',NULL,NULL,'2026-07-15 15:31:14','2026-07-15 15:31:14'),(7,'ADMIN-TEST-20260716-E01','cashflow',5,3,NULL,286.40,'MYR','2026-07-15','reserve_account','paid','confirmed',NULL,'2026-07-17 15:16:06','not_synced',NULL,1,'2026-07-16 10:24:25','2026-07-17 15:16:06'),(8,'ADMIN-TEST-20260716-E02','cashflow',6,3,NULL,950.00,'MYR','2026-07-12','reserve_account','paid','confirmed',NULL,'2026-07-17 15:16:06','not_synced',NULL,1,'2026-07-16 10:24:25','2026-07-17 15:16:06'),(9,'ADMIN-TEST-20260716-E03','cashflow',6,3,NULL,480.00,'MYR','2026-07-10','reserve_account','paid','confirmed',NULL,'2026-07-17 15:17:58','not_synced',NULL,1,'2026-07-16 10:24:25','2026-07-17 15:17:58'),(10,'ADMIN-TEST-20260716-E04','cashflow',5,3,NULL,350.00,'MYR','2026-07-09',NULL,'unpaid','pending',NULL,NULL,'not_synced',NULL,1,'2026-07-16 10:24:25','2026-07-17 15:25:36'),(11,'ADMIN-TEST-20260716-E05','cashflow',7,3,NULL,60.00,'MYR','2026-07-07','reserve_account','paid','confirmed',NULL,'2026-07-17 15:16:06','not_synced',NULL,1,'2026-07-16 10:24:25','2026-07-17 15:16:06'),(12,'ADMIN-TEST-20260716-E06','cashflow',7,3,NULL,420.00,'MYR','2026-07-05','reserve_account','paid','confirmed',NULL,'2026-07-17 15:16:06','not_synced',NULL,1,'2026-07-16 10:24:25','2026-07-17 15:16:06'),(13,'ADMIN-TEST-20260716-E07','cashflow',5,3,NULL,612.30,'MYR','2026-06-20','reserve_account','paid','confirmed',NULL,'2026-07-17 15:16:06','not_synced',NULL,1,'2026-07-16 10:24:25','2026-07-17 15:16:06'),(16,'RTU-20260716113145-2C361DE3','reserve_topup',7,3,NULL,1.00,'MYR','2026-07-16','online_payment','paid','pending',NULL,NULL,'not_synced',NULL,1,'2026-07-16 11:31:45','2026-07-16 11:31:45'),(17,'OWNER-DEMO-F-RENT-01','rent_payment',12,1,5,2600.00,'MYR','2026-07-03','bank_transfer','paid','confirmed',NULL,NULL,'synced',NULL,NULL,'2026-07-17 13:58:13','2026-07-17 13:58:13'),(18,'OWNER-DEMO-F-RENT-02','rent_payment',13,1,6,1600.00,'MYR','2026-07-08','online_transfer','partial','pending',NULL,NULL,'not_synced',NULL,NULL,'2026-07-17 13:58:13','2026-07-17 13:58:13'),(19,'OWNER-DEMO-F-EXP-01','cashflow',12,1,NULL,480.00,'MYR','2026-07-06','reserve_account','paid','confirmed',NULL,'2026-07-17 15:16:06','synced',NULL,NULL,'2026-07-17 13:58:13','2026-07-17 15:16:06'),(20,'OWNER-DEMO-F-EXP-02','cashflow',13,1,NULL,1250.00,'MYR','2026-07-10','reserve_account','paid','confirmed',NULL,'2026-07-17 15:16:06','synced',NULL,NULL,'2026-07-17 13:58:13','2026-07-17 15:16:06'),(21,'OWNER-DEMO-F-EXP-03','cashflow',14,1,NULL,680.00,'MYR','2026-07-07','reserve_account','paid','confirmed',NULL,'2026-07-17 15:16:06','synced',NULL,NULL,'2026-07-17 13:58:13','2026-07-17 15:16:06'),(22,'OWNER-DEMO-F-PAY-01','property_payment',1,1,NULL,183750.00,'MYR','2026-06-02','bank_transfer','paid','confirmed',NULL,NULL,'synced',NULL,NULL,'2026-07-17 13:58:13','2026-07-17 13:58:13'),(23,'OWNER-DEMO-F-PAY-02','property_payment',10,1,NULL,45000.00,'MYR','2026-07-03','online_transfer','partial','pending',NULL,NULL,'not_synced',NULL,NULL,'2026-07-17 13:58:13','2026-07-17 13:58:13'),(27,'PP-20260717142350-330107CE','property_payment',9,1,NULL,276000.00,'MYR','2026-07-17','bank_transfer','pending','pending',NULL,NULL,'not_synced',NULL,4,'2026-07-17 14:23:50','2026-07-17 14:23:50'),(28,'RTU-20260717143109-A5F6887A','reserve_topup',13,1,NULL,1.00,'MYR','2026-07-17','bank_transfer','paid','pending',NULL,NULL,'not_synced',NULL,4,'2026-07-17 14:31:09','2026-07-17 14:31:09');
/*!40000 ALTER TABLE `finance_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `leases`
--

DROP TABLE IF EXISTS `leases`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `leases` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `unit_id` bigint(20) unsigned NOT NULL,
  `tenant_id` bigint(20) unsigned NOT NULL,
  `lease_no` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `monthly_rent` decimal(18,2) NOT NULL,
  `deposit_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `payment_day` tinyint(3) unsigned NOT NULL DEFAULT '1',
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `contract_document_id` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_leases_no` (`lease_no`),
  KEY `idx_leases_unit_status` (`unit_id`,`status`),
  KEY `idx_leases_tenant_status` (`tenant_id`,`status`),
  KEY `idx_leases_end_date` (`end_date`,`status`),
  KEY `fk_leases_document` (`contract_document_id`),
  CONSTRAINT `fk_leases_document` FOREIGN KEY (`contract_document_id`) REFERENCES `documents` (`id`),
  CONSTRAINT `fk_leases_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`),
  CONSTRAINT `fk_leases_unit` FOREIGN KEY (`unit_id`) REFERENCES `units` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `leases`
--

LOCK TABLES `leases` WRITE;
/*!40000 ALTER TABLE `leases` DISABLE KEYS */;
INSERT INTO `leases` (`id`, `unit_id`, `tenant_id`, `lease_no`, `start_date`, `end_date`, `monthly_rent`, `deposit_amount`, `payment_day`, `status`, `contract_document_id`, `created_at`, `updated_at`) VALUES (1,3,1,'TEST-20260715-L01','2026-01-01','2026-12-31',2800.00,5600.00,5,'active',NULL,'2026-07-15 13:52:30','2026-07-15 13:52:30'),(2,6,3,'ADMIN-TEST-20260715-L01','2026-03-01','2027-02-28',3200.00,6400.00,5,'active',NULL,'2026-07-15 14:09:59','2026-07-15 14:09:59'),(3,7,4,'ADMIN-TEST-20260715-L02','2026-04-01','2027-03-31',2100.00,4200.00,1,'active',NULL,'2026-07-15 14:09:59','2026-07-15 14:09:59'),(4,12,5,'OWNER-DEMO-L01','2026-03-01','2027-03-01',2600.00,5200.00,3,'active',NULL,'2026-07-17 13:58:13','2026-07-17 13:58:13'),(5,13,6,'OWNER-DEMO-L02','2025-08-31','2026-08-31',4200.00,8400.00,5,'active',NULL,'2026-07-17 13:58:13','2026-07-17 13:58:13');
/*!40000 ALTER TABLE `leases` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `maintenance_status_history`
--

DROP TABLE IF EXISTS `maintenance_status_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `maintenance_status_history` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `work_order_id` bigint(20) unsigned NOT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `occurred_at` datetime NOT NULL,
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `changed_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_maintenance_history_order_time` (`work_order_id`,`occurred_at`),
  KEY `fk_maintenance_history_user` (`changed_by`),
  CONSTRAINT `fk_maintenance_history_order` FOREIGN KEY (`work_order_id`) REFERENCES `maintenance_work_orders` (`id`),
  CONSTRAINT `fk_maintenance_history_user` FOREIGN KEY (`changed_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `maintenance_status_history`
--

LOCK TABLES `maintenance_status_history` WRITE;
/*!40000 ALTER TABLE `maintenance_status_history` DISABLE KEYS */;
INSERT INTO `maintenance_status_history` (`id`, `work_order_id`, `status`, `occurred_at`, `note`, `changed_by`, `created_at`) VALUES (1,1,'submitted','2026-07-10 10:30:00','業主提交報修',1,'2026-07-16 10:24:25'),(2,1,'assigned','2026-07-10 11:00:00','已指派 ADMIN Test Property Care',1,'2026-07-16 10:24:25'),(3,1,'in_progress','2026-07-11 09:30:00','維修員已開始處理',1,'2026-07-16 10:24:25'),(4,2,'submitted','2026-07-09 14:15:00','業主提交報修',1,'2026-07-16 10:24:25'),(5,4,'open','2026-07-11 13:58:13','Leakage report received from tenant.',1,'2026-07-17 13:58:13'),(6,4,'in_progress','2026-07-15 13:58:13','Vendor appointed and repair visit scheduled.',1,'2026-07-17 13:58:13'),(7,5,'open','2026-07-14 13:58:13','Work order raised for resale preparation.',1,'2026-07-17 13:58:13');
/*!40000 ALTER TABLE `maintenance_status_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `maintenance_work_orders`
--

DROP TABLE IF EXISTS `maintenance_work_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `maintenance_work_orders` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `work_order_no` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `unit_id` bigint(20) unsigned NOT NULL,
  `owner_id` bigint(20) unsigned DEFAULT NULL,
  `tenant_id` bigint(20) unsigned DEFAULT NULL,
  `vendor_id` bigint(20) unsigned DEFAULT NULL,
  `cashflow_entry_id` bigint(20) unsigned DEFAULT NULL,
  `category` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(180) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requested_at` datetime NOT NULL,
  `completed_at` datetime DEFAULT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'open',
  `estimated_amount` decimal(18,2) DEFAULT NULL,
  `actual_amount` decimal(18,2) DEFAULT NULL,
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_orders_no` (`work_order_no`),
  KEY `idx_work_orders_status_date` (`status`,`requested_at`),
  KEY `idx_work_orders_unit` (`unit_id`,`status`),
  KEY `fk_work_orders_owner` (`owner_id`),
  KEY `fk_work_orders_tenant` (`tenant_id`),
  KEY `fk_work_orders_vendor` (`vendor_id`),
  KEY `fk_work_orders_cashflow` (`cashflow_entry_id`),
  KEY `fk_work_orders_creator` (`created_by`),
  CONSTRAINT `fk_work_orders_cashflow` FOREIGN KEY (`cashflow_entry_id`) REFERENCES `cashflow_entries` (`id`),
  CONSTRAINT `fk_work_orders_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_work_orders_owner` FOREIGN KEY (`owner_id`) REFERENCES `owners` (`id`),
  CONSTRAINT `fk_work_orders_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`),
  CONSTRAINT `fk_work_orders_unit` FOREIGN KEY (`unit_id`) REFERENCES `units` (`id`),
  CONSTRAINT `fk_work_orders_vendor` FOREIGN KEY (`vendor_id`) REFERENCES `vendors` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `maintenance_work_orders`
--

LOCK TABLES `maintenance_work_orders` WRITE;
/*!40000 ALTER TABLE `maintenance_work_orders` DISABLE KEYS */;
INSERT INTO `maintenance_work_orders` (`id`, `work_order_no`, `unit_id`, `owner_id`, `tenant_id`, `vendor_id`, `cashflow_entry_id`, `category`, `title`, `description`, `requested_at`, `completed_at`, `status`, `estimated_amount`, `actual_amount`, `created_by`, `created_at`, `updated_at`) VALUES (1,'ADMIN-TEST-20260716-M01',6,3,NULL,2,3,'plumbing','廚房水管漏水維修','廚房水管接口持續滲水，需要更換接頭及密封件。','2026-07-10 10:30:00',NULL,'in_progress',500.00,480.00,1,'2026-07-16 10:24:25','2026-07-16 10:24:25'),(2,'ADMIN-TEST-20260716-M02',5,3,NULL,2,4,'air_conditioning','客廳冷氣檢查','客廳冷氣制冷不足，等待安排維修員上門。','2026-07-09 14:15:00',NULL,'submitted',350.00,NULL,1,'2026-07-16 10:24:25','2026-07-16 10:24:25'),(3,'OWNER-DEMO-M01',12,1,5,3,NULL,'air_conditioning','Living room air-conditioner servicing','Routine chemical cleaning and refrigerant pressure inspection.','2026-06-09 13:58:13','2026-06-12 13:58:13','completed',380.00,350.00,1,'2026-07-17 13:58:13','2026-07-17 13:58:13'),(4,'OWNER-DEMO-M02',13,1,6,4,11,'plumbing','Master bathroom concealed pipe leakage','Moisture detected behind the vanity wall. Pressure test and pipe replacement required.','2026-07-11 13:58:13',NULL,'in_progress',1850.00,NULL,1,'2026-07-17 13:58:13','2026-07-17 13:58:13'),(5,'OWNER-DEMO-M03',14,1,NULL,3,NULL,'painting','Touch-up painting before property viewing','Repair minor wall marks and repaint the living room feature wall before listing photography.','2026-07-14 13:58:13',NULL,'open',980.00,NULL,1,'2026-07-17 13:58:13','2026-07-17 13:58:13');
/*!40000 ALTER TABLE `maintenance_work_orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_deliveries`
--

DROP TABLE IF EXISTS `notification_deliveries`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notification_deliveries` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `notification_id` bigint(20) unsigned NOT NULL,
  `channel` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'in_app / email / line',
  `destination` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `attempt_count` tinyint(3) unsigned NOT NULL DEFAULT '0',
  `sent_at` datetime DEFAULT NULL,
  `failed_at` datetime DEFAULT NULL,
  `failure_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_deliveries_channel` (`notification_id`,`channel`),
  KEY `idx_notification_deliveries_status` (`status`,`sent_at`),
  CONSTRAINT `fk_notification_deliveries_notification` FOREIGN KEY (`notification_id`) REFERENCES `notifications` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_deliveries`
--

LOCK TABLES `notification_deliveries` WRITE;
/*!40000 ALTER TABLE `notification_deliveries` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification_deliveries` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_rules`
--

DROP TABLE IF EXISTS `notification_rules`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notification_rules` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'payment_due / rent_due / lease_expiry / reserve_low / document_expiry',
  `days_before` smallint(6) NOT NULL DEFAULT '0',
  `channels` json NOT NULL,
  `recipient_role` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_rules_code` (`code`),
  KEY `idx_notification_rules_event_enabled` (`event_type`,`enabled`),
  KEY `fk_notification_rules_creator` (`created_by`),
  CONSTRAINT `fk_notification_rules_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_rules`
--

LOCK TABLES `notification_rules` WRITE;
/*!40000 ALTER TABLE `notification_rules` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification_rules` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notification_subscriptions`
--

DROP TABLE IF EXISTS `notification_subscriptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notification_subscriptions` (
  `user_id` bigint(20) unsigned NOT NULL,
  `channel` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `destination` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '0',
  `verified_at` datetime DEFAULT NULL,
  `verification_code_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `verification_expires_at` datetime DEFAULT NULL,
  `last_verification_sent_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`,`channel`),
  KEY `idx_notification_subscriptions_delivery` (`channel`,`enabled`,`verified_at`),
  CONSTRAINT `fk_notification_subscriptions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notification_subscriptions`
--

LOCK TABLES `notification_subscriptions` WRITE;
/*!40000 ALTER TABLE `notification_subscriptions` DISABLE KEYS */;
/*!40000 ALTER TABLE `notification_subscriptions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `notifications` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `rule_id` bigint(20) unsigned DEFAULT NULL,
  `recipient_user_id` bigint(20) unsigned DEFAULT NULL,
  `recipient_owner_id` bigint(20) unsigned DEFAULT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `body` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `related_type` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `related_id` bigint(20) unsigned DEFAULT NULL,
  `priority` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'normal',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'unread',
  `read_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_notifications_recipient` (`recipient_user_id`,`status`,`created_at`),
  KEY `idx_notifications_owner` (`recipient_owner_id`,`status`,`created_at`),
  KEY `idx_notifications_related` (`related_type`,`related_id`),
  KEY `fk_notifications_rule` (`rule_id`),
  CONSTRAINT `fk_notifications_owner` FOREIGN KEY (`recipient_owner_id`) REFERENCES `owners` (`id`),
  CONSTRAINT `fk_notifications_rule` FOREIGN KEY (`rule_id`) REFERENCES `notification_rules` (`id`),
  CONSTRAINT `fk_notifications_user` FOREIGN KEY (`recipient_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` (`id`, `rule_id`, `recipient_user_id`, `recipient_owner_id`, `title`, `body`, `related_type`, `related_id`, `priority`, `status`, `read_at`, `created_at`) VALUES (1,NULL,6,3,'2026 年 7 月租金已入帳','Central Suites B-0602 租金 RM 2,100.00 已完成確認。',NULL,NULL,'normal','read','2026-07-16 14:00:23','2026-07-15 09:30:00'),(2,NULL,6,3,'房款即將到期','Central Suites B-0602 下一期房款將於 2026-07-20 到期。',NULL,NULL,'high','read','2026-07-16 14:00:21','2026-07-14 10:00:00'),(3,NULL,6,3,'預備金帳戶已更新','您名下 4 個單位的預備金餘額已完成同步。',NULL,NULL,'normal','read',NULL,'2026-07-12 16:20:00'),(4,NULL,4,1,'Meridian Park 房款即將到期','B-12-05 建築結構款 RM 276,000.00 將於 2026-07-29 到期。','owner_unit',7,'high','read','2026-07-17 14:10:24','2026-07-17 11:58:13'),(5,NULL,4,1,'Mont Kiara Verde 租金尚未收齊','F-16-06 本月租金應收 RM 4,200.00，目前已收 RM 1,600.00，尚欠 RM 2,600.00。','lease',5,'urgent','read','2026-07-17 14:10:22','2026-07-16 13:58:13'),(6,NULL,4,1,'預備金低於最低標準','Mont Kiara Verde F-16-06 預備金餘額 RM 1,850.00，低於最低標準 RM 4,000.00。','owner_unit',11,'high','read','2026-07-17 14:10:23','2026-07-15 13:58:13'),(7,NULL,4,1,'維修工單處理中','F-16-06 主浴室暗管滲漏已安排 AquaFix Plumbing Solutions 進場處理。','work_order',4,'normal','read',NULL,'2026-07-14 13:58:13');
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `owner_unit_services`
--

DROP TABLE IF EXISTS `owner_unit_services`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `owner_unit_services` (
  `owner_unit_id` bigint(20) unsigned NOT NULL,
  `service_type` enum('RENTAL','RESALE','MANAGEMENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('active','paused','ended') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `started_at` date DEFAULT NULL,
  `ended_at` date DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`owner_unit_id`,`service_type`),
  KEY `idx_owner_unit_services_status` (`service_type`,`status`),
  CONSTRAINT `fk_owner_unit_services_holding` FOREIGN KEY (`owner_unit_id`) REFERENCES `owner_units` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `owner_unit_services`
--

LOCK TABLES `owner_unit_services` WRITE;
/*!40000 ALTER TABLE `owner_unit_services` DISABLE KEYS */;
INSERT INTO `owner_unit_services` (`owner_unit_id`, `service_type`, `status`, `started_at`, `ended_at`, `created_at`, `updated_at`) VALUES (3,'MANAGEMENT','active','2026-01-01',NULL,'2026-07-17 12:11:03','2026-07-17 12:11:03'),(4,'RENTAL','active','2026-01-01',NULL,'2026-07-17 12:11:03','2026-07-17 12:11:03'),(4,'MANAGEMENT','active','2026-01-01',NULL,'2026-07-17 12:11:03','2026-07-17 12:11:03'),(5,'RENTAL','active','2026-02-01',NULL,'2026-07-17 12:11:03','2026-07-17 12:11:03'),(5,'MANAGEMENT','active','2026-02-01',NULL,'2026-07-17 12:11:03','2026-07-17 12:11:03'),(6,'MANAGEMENT','active','2026-02-01',NULL,'2026-07-17 12:11:03','2026-07-17 12:11:03'),(10,'RENTAL','active','2025-02-01',NULL,'2026-07-17 13:58:13','2026-07-17 13:58:13'),(10,'MANAGEMENT','active','2025-01-20',NULL,'2026-07-17 13:58:13','2026-07-17 13:58:13'),(11,'RENTAL','active','2024-05-01',NULL,'2026-07-17 13:58:13','2026-07-17 13:58:13'),(11,'MANAGEMENT','active','2024-04-15',NULL,'2026-07-17 13:58:13','2026-07-17 13:58:13'),(12,'RENTAL','ended','2026-07-17','2026-07-17','2026-07-17 14:21:26','2026-07-17 14:21:29'),(12,'RESALE','active','2026-06-02',NULL,'2026-07-17 13:58:13','2026-07-17 14:21:37'),(12,'MANAGEMENT','active','2023-01-10',NULL,'2026-07-17 13:58:13','2026-07-17 14:21:37');
/*!40000 ALTER TABLE `owner_unit_services` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `owner_units`
--

DROP TABLE IF EXISTS `owner_units`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `owner_units` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `owner_id` bigint(20) unsigned NOT NULL,
  `unit_id` bigint(20) unsigned NOT NULL,
  `ownership_percent` decimal(5,2) NOT NULL DEFAULT '100.00',
  `is_primary` tinyint(1) NOT NULL DEFAULT '1',
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `asset_stage` enum('PRE_HANDOVER','OPERATING','DISPOSED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PRE_HANDOVER',
  `expected_handover_date` date DEFAULT NULL,
  `actual_handover_date` date DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner_units_period` (`owner_id`,`unit_id`,`start_date`),
  KEY `idx_owner_units_unit` (`unit_id`,`status`),
  KEY `idx_owner_units_owner` (`owner_id`,`status`),
  KEY `idx_owner_units_stage` (`asset_stage`,`status`),
  CONSTRAINT `fk_owner_units_owner` FOREIGN KEY (`owner_id`) REFERENCES `owners` (`id`),
  CONSTRAINT `fk_owner_units_unit` FOREIGN KEY (`unit_id`) REFERENCES `units` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `owner_units`
--

LOCK TABLES `owner_units` WRITE;
/*!40000 ALTER TABLE `owner_units` DISABLE KEYS */;
INSERT INTO `owner_units` (`id`, `owner_id`, `unit_id`, `ownership_percent`, `is_primary`, `start_date`, `end_date`, `asset_stage`, `expected_handover_date`, `actual_handover_date`, `status`, `created_at`) VALUES (1,1,1,100.00,1,'2026-01-01',NULL,'PRE_HANDOVER','2027-05-13',NULL,'active','2026-07-15 13:52:30'),(2,2,2,100.00,1,'2026-01-01',NULL,'PRE_HANDOVER',NULL,NULL,'active','2026-07-15 13:52:30'),(3,3,5,100.00,1,'2026-01-01',NULL,'OPERATING',NULL,'2026-01-01','active','2026-07-15 14:09:59'),(4,3,6,100.00,1,'2026-01-01',NULL,'OPERATING',NULL,'2026-01-01','active','2026-07-15 14:09:59'),(5,3,7,100.00,1,'2026-02-01',NULL,'OPERATING',NULL,'2026-02-01','active','2026-07-15 14:09:59'),(6,3,8,100.00,1,'2026-02-01',NULL,'OPERATING',NULL,'2026-02-01','active','2026-07-15 14:09:59'),(7,1,9,100.00,1,'2025-11-15',NULL,'PRE_HANDOVER','2027-03-14',NULL,'active','2026-07-17 13:58:13'),(8,1,10,100.00,1,'2026-02-20',NULL,'PRE_HANDOVER','2027-06-12',NULL,'active','2026-07-17 13:58:13'),(9,1,11,100.00,1,'2025-08-08',NULL,'PRE_HANDOVER','2026-10-15',NULL,'active','2026-07-17 13:58:13'),(10,1,12,100.00,1,'2024-06-18',NULL,'OPERATING',NULL,'2025-01-20','active','2026-07-17 13:58:13'),(11,1,13,100.00,1,'2023-09-12',NULL,'OPERATING',NULL,'2024-04-15','active','2026-07-17 13:58:13'),(12,1,14,100.00,1,'2022-05-06',NULL,'OPERATING',NULL,'2023-01-10','active','2026-07-17 13:58:13');
/*!40000 ALTER TABLE `owner_units` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `owners`
--

DROP TABLE IF EXISTS `owners`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `owners` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned DEFAULT NULL,
  `full_name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `identity_no` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '敏感資料；應在應用層或欄位層加密，禁止寫入日誌',
  `phone` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(190) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owners_name` (`full_name`),
  KEY `idx_owners_phone` (`phone`),
  KEY `idx_owners_user` (`user_id`),
  CONSTRAINT `fk_owners_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `owners`
--

LOCK TABLES `owners` WRITE;
/*!40000 ALTER TABLE `owners` DISABLE KEYS */;
INSERT INTO `owners` (`id`, `user_id`, `full_name`, `identity_no`, `phone`, `email`, `status`, `created_at`, `updated_at`) VALUES (1,4,'Tan Wei Ming','900101-14-1234','+60123456789','weiming.tan@example.com','active','2026-07-15 13:52:30','2026-07-17 13:58:13'),(2,5,'TEST Owner Two',NULL,'+60110000002','test.owner2@example.com','active','2026-07-15 13:52:30','2026-07-17 11:34:26'),(3,6,'ADMIN Test Portfolio Owner',NULL,'+60110000999','admin.test.owner@example.com','active','2026-07-15 14:09:59','2026-07-17 11:34:26');
/*!40000 ALTER TABLE `owners` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_installments`
--

DROP TABLE IF EXISTS `payment_installments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `payment_installments` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `payment_plan_id` bigint(20) unsigned NOT NULL,
  `installment_no` smallint(5) unsigned NOT NULL,
  `milestone` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `due_date` date NOT NULL,
  `amount_due` decimal(18,2) NOT NULL,
  `amount_paid` decimal(18,2) NOT NULL DEFAULT '0.00',
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_installments_no` (`payment_plan_id`,`installment_no`),
  KEY `idx_payment_installments_due_status` (`due_date`,`status`),
  CONSTRAINT `fk_payment_installments_plan` FOREIGN KEY (`payment_plan_id`) REFERENCES `payment_plans` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_installments`
--

LOCK TABLES `payment_installments` WRITE;
/*!40000 ALTER TABLE `payment_installments` DISABLE KEYS */;
INSERT INTO `payment_installments` (`id`, `payment_plan_id`, `installment_no`, `milestone`, `due_date`, `amount_due`, `amount_paid`, `status`, `created_at`, `updated_at`) VALUES (1,1,1,'Deposit','2026-01-15',162500.00,162500.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(2,1,2,'Foundation','2026-04-15',162500.00,162500.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(3,1,3,'Structure','2026-09-15',162500.00,0.00,'pending','2026-07-15 15:31:14','2026-07-15 15:31:14'),(4,1,4,'Handover','2026-12-15',162500.00,0.00,'pending','2026-07-15 15:31:14','2026-07-15 15:31:14'),(5,2,1,'Deposit','2026-01-15',245000.00,245000.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(6,2,2,'Foundation','2026-03-15',245000.00,245000.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(7,2,3,'Structure','2026-05-15',245000.00,245000.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(8,2,4,'Handover','2026-07-01',245000.00,245000.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(9,3,1,'Deposit','2026-02-15',120000.00,120000.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(10,3,2,'Foundation','2026-07-20',120000.00,0.00,'pending','2026-07-15 15:31:14','2026-07-15 15:31:14'),(11,3,3,'Structure','2026-10-20',120000.00,0.00,'pending','2026-07-15 15:31:14','2026-07-15 15:31:14'),(12,3,4,'Handover','2027-01-20',120000.00,0.00,'pending','2026-07-15 15:31:14','2026-07-15 15:31:14'),(13,4,1,'Deposit','2026-02-15',177500.00,177500.00,'paid','2026-07-15 15:31:14','2026-07-15 15:31:14'),(14,4,2,'Foundation','2026-06-15',177500.00,0.00,'overdue','2026-07-15 15:31:14','2026-07-15 15:31:14'),(15,4,3,'Structure','2026-10-15',177500.00,0.00,'pending','2026-07-15 15:31:14','2026-07-15 15:31:14'),(16,4,4,'Handover','2027-02-15',177500.00,0.00,'pending','2026-07-15 15:31:14','2026-07-15 15:31:14'),(17,5,1,'Booking and SPA signing','2025-10-10',73500.00,73500.00,'paid','2026-07-17 13:58:13','2026-07-17 13:58:13'),(18,5,2,'Foundation completed','2026-01-28',147000.00,147000.00,'paid','2026-07-17 13:58:13','2026-07-17 13:58:13'),(19,5,3,'Structural frame completed','2026-06-02',183750.00,183750.00,'paid','2026-07-17 13:58:13','2026-07-17 13:58:13'),(20,5,4,'Internal works completed','2026-09-15',183750.00,0.00,'pending','2026-07-17 13:58:13','2026-07-17 13:58:13'),(21,5,5,'Vacant possession','2027-03-14',147000.00,0.00,'pending','2026-07-17 13:58:13','2026-07-17 13:58:13'),(22,6,1,'Booking and down payment','2025-11-19',184000.00,184000.00,'paid','2026-07-17 13:58:13','2026-07-17 13:58:13'),(23,6,2,'Foundation and podium','2026-04-08',276000.00,276000.00,'paid','2026-07-17 13:58:13','2026-07-17 13:58:13'),(24,6,3,'Building structure','2026-07-29',276000.00,0.00,'pending','2026-07-17 13:58:13','2026-07-17 13:58:13'),(25,6,4,'Handover balance','2027-02-07',184000.00,0.00,'pending','2026-07-17 13:58:13','2026-07-17 13:58:13'),(26,7,1,'Booking deposit','2026-02-27',58500.00,58500.00,'paid','2026-07-17 13:58:13','2026-07-17 13:58:13'),(27,7,2,'SPA progressive claim','2026-06-29',117000.00,45000.00,'overdue','2026-07-17 13:58:13','2026-07-17 13:58:13'),(28,7,3,'Structure and roofing','2026-11-04',175500.00,0.00,'pending','2026-07-17 13:58:13','2026-07-17 13:58:13'),(29,7,4,'Vacant possession','2027-05-13',234000.00,0.00,'pending','2026-07-17 13:58:13','2026-07-17 13:58:13'),(30,8,1,'Booking and SPA','2025-08-31',128000.00,128000.00,'paid','2026-07-17 13:58:13','2026-07-17 13:58:13'),(31,8,2,'Foundation works','2025-12-09',384000.00,384000.00,'paid','2026-07-17 13:58:13','2026-07-17 13:58:13'),(32,8,3,'Structure completed','2026-04-08',384000.00,384000.00,'paid','2026-07-17 13:58:13','2026-07-17 13:58:13'),(33,8,4,'Final payment before handover','2026-06-27',384000.00,384000.00,'paid','2026-07-17 13:58:13','2026-07-17 13:58:13');
/*!40000 ALTER TABLE `payment_installments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_plans`
--

DROP TABLE IF EXISTS `payment_plans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `payment_plans` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `purchase_contract_id` bigint(20) unsigned NOT NULL,
  `plan_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installment_count` smallint(5) unsigned NOT NULL DEFAULT '1',
  `total_amount` decimal(18,2) NOT NULL,
  `start_date` date DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_payment_plans_contract` (`purchase_contract_id`,`status`),
  CONSTRAINT `fk_payment_plans_contract` FOREIGN KEY (`purchase_contract_id`) REFERENCES `purchase_contracts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_plans`
--

LOCK TABLES `payment_plans` WRITE;
/*!40000 ALTER TABLE `payment_plans` DISABLE KEYS */;
INSERT INTO `payment_plans` (`id`, `purchase_contract_id`, `plan_name`, `installment_count`, `total_amount`, `start_date`, `status`, `created_at`) VALUES (1,1,'ADMIN Test Standard Plan',4,650000.00,'2026-01-15','historical','2026-07-15 15:31:14'),(2,2,'ADMIN Test Standard Plan',4,980000.00,'2026-01-15','historical','2026-07-15 15:31:14'),(3,3,'ADMIN Test Standard Plan',4,480000.00,'2026-02-15','historical','2026-07-15 15:31:14'),(4,4,'ADMIN Test Standard Plan',4,710000.00,'2026-02-15','historical','2026-07-15 15:31:14'),(5,5,'Progressive Construction Plan',5,735000.00,'2025-10-18','active','2026-07-17 13:58:13'),(6,6,'Standard Progressive Plan',4,920000.00,'2025-11-15','active','2026-07-17 13:58:13'),(7,7,'Affordable Home Progressive Plan',4,585000.00,'2026-02-20','active','2026-07-17 13:58:13'),(8,8,'Premium Residence Plan',4,1280000.00,'2025-08-08','active','2026-07-17 13:58:13');
/*!40000 ALTER TABLE `payment_plans` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_receipt_allocations`
--

DROP TABLE IF EXISTS `payment_receipt_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `payment_receipt_allocations` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `receipt_id` bigint(20) unsigned NOT NULL,
  `installment_id` bigint(20) unsigned NOT NULL,
  `allocated_amount` decimal(18,2) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_receipt_allocations` (`receipt_id`,`installment_id`),
  KEY `fk_receipt_allocations_installment` (`installment_id`),
  CONSTRAINT `fk_receipt_allocations_installment` FOREIGN KEY (`installment_id`) REFERENCES `payment_installments` (`id`),
  CONSTRAINT `fk_receipt_allocations_receipt` FOREIGN KEY (`receipt_id`) REFERENCES `payment_receipts` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_receipt_allocations`
--

LOCK TABLES `payment_receipt_allocations` WRITE;
/*!40000 ALTER TABLE `payment_receipt_allocations` DISABLE KEYS */;
INSERT INTO `payment_receipt_allocations` (`id`, `receipt_id`, `installment_id`, `allocated_amount`) VALUES (1,3,24,276000.00);
/*!40000 ALTER TABLE `payment_receipt_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `payment_receipts`
--

DROP TABLE IF EXISTS `payment_receipts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `payment_receipts` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `finance_record_id` bigint(20) unsigned NOT NULL,
  `receipt_no` varchar(60) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payer_name` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_reference` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `proof_document_id` bigint(20) unsigned DEFAULT NULL,
  `submission_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `review_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_receipts_finance` (`finance_record_id`),
  UNIQUE KEY `uk_payment_receipts_no` (`receipt_no`),
  KEY `fk_payment_receipts_document` (`proof_document_id`),
  CONSTRAINT `fk_payment_receipts_document` FOREIGN KEY (`proof_document_id`) REFERENCES `documents` (`id`),
  CONSTRAINT `fk_payment_receipts_finance` FOREIGN KEY (`finance_record_id`) REFERENCES `finance_records` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_receipts`
--

LOCK TABLES `payment_receipts` WRITE;
/*!40000 ALTER TABLE `payment_receipts` DISABLE KEYS */;
INSERT INTO `payment_receipts` (`id`, `finance_record_id`, `receipt_no`, `payer_name`, `bank_reference`, `proof_document_id`, `submission_note`, `review_note`, `created_at`) VALUES (2,16,'RTU-RCP-20260716113145-7FFB486E','1','1 | 1',8,NULL,NULL,'2026-07-16 11:31:45'),(3,27,'RCP-20260717142350-7DFA498E','Tan Wei Ming','Maybank Berhad | 1000000000',9,NULL,'10','2026-07-17 14:23:50'),(4,28,'RTU-RCP-20260717143109-354743EC','111','111 | 11',10,NULL,NULL,'2026-07-17 14:31:09');
/*!40000 ALTER TABLE `payment_receipts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `permissions`
--

DROP TABLE IF EXISTS `permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `permissions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permissions_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `permissions`
--

LOCK TABLES `permissions` WRITE;
/*!40000 ALTER TABLE `permissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `projects`
--

DROP TABLE IF EXISTS `projects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `projects` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `project_code` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `country_code` char(2) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MY',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_projects_code` (`project_code`),
  KEY `idx_projects_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `projects`
--

LOCK TABLES `projects` WRITE;
/*!40000 ALTER TABLE `projects` DISABLE KEYS */;
INSERT INTO `projects` (`id`, `project_code`, `name`, `address`, `city`, `country_code`, `status`, `created_at`, `updated_at`) VALUES (1,'TEST-20260715-P01','TEST Riverside Residence','100 Test River Road','Kuala Lumpur','MY','active','2026-07-15 13:52:30','2026-07-15 13:52:30'),(2,'TEST-20260715-P02','TEST Garden Suites','200 Test Garden Avenue','Petaling Jaya','MY','active','2026-07-15 13:52:30','2026-07-15 13:52:30'),(3,'ADMIN-TEST-20260715-P01','ADMIN Test Lakeview Towers','10 Admin Test Lakeview Road','Kuala Lumpur','MY','active','2026-07-15 14:09:59','2026-07-15 14:09:59'),(4,'ADMIN-TEST-20260715-P02','ADMIN Test Central Suites','20 Admin Test Central Street','Petaling Jaya','MY','active','2026-07-15 14:09:59','2026-07-15 14:09:59'),(5,'OWNER-DEMO-P01','Riverside Residence','8 Jalan Ampang Hilir','Kuala Lumpur','MY','active','2026-07-17 13:58:13','2026-07-17 13:58:13'),(6,'OWNER-DEMO-P02','Meridian Park','12 Jalan Teknokrat 6','Cyberjaya','MY','active','2026-07-17 13:58:13','2026-07-17 13:58:13'),(7,'OWNER-DEMO-P03','The Grove Suites','25 Jalan PJU 8/8','Petaling Jaya','MY','active','2026-07-17 13:58:13','2026-07-17 13:58:13'),(8,'OWNER-DEMO-P04','Marina Crest','3 Persiaran Bayan Indah','Penang','MY','active','2026-07-17 13:58:13','2026-07-17 13:58:13'),(9,'OWNER-DEMO-P05','KL Sentral Loft','18 Jalan Stesen Sentral 5','Kuala Lumpur','MY','active','2026-07-17 13:58:13','2026-07-17 13:58:13'),(10,'OWNER-DEMO-P06','Mont Kiara Verde','6 Jalan Kiara 3','Kuala Lumpur','MY','active','2026-07-17 13:58:13','2026-07-17 13:58:13'),(11,'OWNER-DEMO-P07','Bangsar South Suites','2 Jalan Kerinchi','Kuala Lumpur','MY','active','2026-07-17 13:58:13','2026-07-17 13:58:13');
/*!40000 ALTER TABLE `projects` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `properties`
--

DROP TABLE IF EXISTS `properties`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `properties` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `area` int(11) DEFAULT NULL,
  `bedrooms` int(11) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `price` decimal(38,2) DEFAULT NULL,
  `project_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` enum('AVAILABLE','OFFLINE','RESERVED','SOLD') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `properties`
--

LOCK TABLES `properties` WRITE;
/*!40000 ALTER TABLE `properties` DISABLE KEYS */;
INSERT INTO `properties` (`id`, `address`, `area`, `bedrooms`, `created_at`, `name`, `price`, `project_name`, `status`, `updated_at`) VALUES (1,'100 Test River Road, Kuala Lumpur',79,2,'2026-07-15 13:52:30.000000','TEST Riverside Residence A-0801',680000.00,'TEST Riverside Residence','AVAILABLE','2026-07-15 13:52:30.000000'),(2,'100 Test River Road, Kuala Lumpur',102,3,'2026-07-15 13:52:30.000000','TEST Riverside Residence A-1208',920000.00,'TEST Riverside Residence','RESERVED','2026-07-15 13:52:30.000000'),(3,'200 Test Garden Avenue, Petaling Jaya',81,2,'2026-07-15 13:52:30.000000','TEST Garden Suites B-1802',720000.00,'TEST Garden Suites','AVAILABLE','2026-07-15 13:52:30.000000'),(4,'10 Admin Test Lakeview Road, Kuala Lumpur',76,2,'2026-07-15 14:09:59.000000','ADMIN Test Lakeview A-0301',650000.00,'ADMIN Test Lakeview Towers','AVAILABLE','2026-07-15 14:09:59.000000'),(5,'10 Admin Test Lakeview Road, Kuala Lumpur',110,3,'2026-07-15 14:09:59.000000','ADMIN Test Lakeview A-0906',980000.00,'ADMIN Test Lakeview Towers','SOLD','2026-07-15 14:09:59.000000'),(6,'20 Admin Test Central Street, Petaling Jaya',48,1,'2026-07-15 14:09:59.000000','ADMIN Test Central B-0602',480000.00,'ADMIN Test Central Suites','RESERVED','2026-07-15 14:09:59.000000'),(7,'20 Admin Test Central Street, Petaling Jaya',84,2,'2026-07-15 14:09:59.000000','ADMIN Test Central B-1508',710000.00,'ADMIN Test Central Suites','AVAILABLE','2026-07-15 14:09:59.000000');
/*!40000 ALTER TABLE `properties` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `purchase_contracts`
--

DROP TABLE IF EXISTS `purchase_contracts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `purchase_contracts` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `owner_unit_id` bigint(20) unsigned NOT NULL,
  `contract_no` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `purchase_price` decimal(18,2) NOT NULL,
  `currency` char(3) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'MYR',
  `signed_date` date DEFAULT NULL,
  `handover_date` date DEFAULT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_purchase_contracts_no` (`contract_no`),
  KEY `idx_purchase_contracts_owner_unit` (`owner_unit_id`,`status`),
  CONSTRAINT `fk_purchase_contracts_owner_unit` FOREIGN KEY (`owner_unit_id`) REFERENCES `owner_units` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_contracts`
--

LOCK TABLES `purchase_contracts` WRITE;
/*!40000 ALTER TABLE `purchase_contracts` DISABLE KEYS */;
INSERT INTO `purchase_contracts` (`id`, `owner_unit_id`, `contract_no`, `purchase_price`, `currency`, `signed_date`, `handover_date`, `status`, `created_at`, `updated_at`) VALUES (1,3,'ADMIN-TEST-20260715-C01',650000.00,'MYR','2026-01-01','2026-01-01','completed','2026-07-15 15:31:14','2026-07-17 12:11:03'),(2,4,'ADMIN-TEST-20260715-C02',980000.00,'MYR','2026-01-01','2026-01-01','completed','2026-07-15 15:31:14','2026-07-17 12:11:03'),(3,5,'ADMIN-TEST-20260715-C03',480000.00,'MYR','2026-02-01','2026-02-01','completed','2026-07-15 15:31:14','2026-07-17 12:11:03'),(4,6,'ADMIN-TEST-20260715-C04',710000.00,'MYR','2026-02-01','2026-02-01','completed','2026-07-15 15:31:14','2026-07-17 12:11:03'),(5,1,'OWNER-DEMO-C01',735000.00,'MYR','2025-10-18','2027-05-13','active','2026-07-17 13:58:13','2026-07-17 13:58:13'),(6,7,'OWNER-DEMO-C02',920000.00,'MYR','2025-11-15','2027-03-14','active','2026-07-17 13:58:13','2026-07-17 13:58:13'),(7,8,'OWNER-DEMO-C03',585000.00,'MYR','2026-02-20','2027-06-12','active','2026-07-17 13:58:13','2026-07-17 13:58:13'),(8,9,'OWNER-DEMO-C04',1280000.00,'MYR','2025-08-08','2026-10-15','active','2026-07-17 13:58:13','2026-07-17 13:58:13'),(9,10,'OWNER-DEMO-C05',680000.00,'MYR','2024-06-18','2025-01-20','completed','2026-07-17 13:58:13','2026-07-17 13:58:13'),(10,11,'OWNER-DEMO-C06',1150000.00,'MYR','2023-09-12','2024-04-15','completed','2026-07-17 13:58:13','2026-07-17 13:58:13'),(11,12,'OWNER-DEMO-C07',890000.00,'MYR','2022-05-06','2023-01-10','completed','2026-07-17 13:58:13','2026-07-17 13:58:13');
/*!40000 ALTER TABLE `purchase_contracts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rent_invoices`
--

DROP TABLE IF EXISTS `rent_invoices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rent_invoices` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `lease_id` bigint(20) unsigned NOT NULL,
  `billing_month` date NOT NULL COMMENT '統一存該月份第一天',
  `due_date` date NOT NULL,
  `amount_due` decimal(18,2) NOT NULL,
  `amount_paid` decimal(18,2) NOT NULL DEFAULT '0.00',
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'unpaid',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rent_invoices_month` (`lease_id`,`billing_month`),
  KEY `idx_rent_invoices_due_status` (`due_date`,`status`),
  CONSTRAINT `fk_rent_invoices_lease` FOREIGN KEY (`lease_id`) REFERENCES `leases` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rent_invoices`
--

LOCK TABLES `rent_invoices` WRITE;
/*!40000 ALTER TABLE `rent_invoices` DISABLE KEYS */;
INSERT INTO `rent_invoices` (`id`, `lease_id`, `billing_month`, `due_date`, `amount_due`, `amount_paid`, `status`, `created_at`, `updated_at`) VALUES (1,1,'2026-06-01','2026-06-05',2800.00,2800.00,'paid','2026-07-15 13:52:30','2026-07-15 13:52:30'),(2,1,'2026-07-01','2026-07-05',2800.00,0.00,'overdue','2026-07-15 13:52:30','2026-07-15 13:52:30'),(3,2,'2026-06-01','2026-06-05',3200.00,3200.00,'paid','2026-07-15 14:09:59','2026-07-15 14:09:59'),(4,2,'2026-07-01','2026-07-05',3200.00,0.00,'overdue','2026-07-15 14:09:59','2026-07-15 14:09:59'),(5,3,'2026-07-01','2026-07-01',2100.00,2100.00,'paid','2026-07-15 14:09:59','2026-07-15 14:09:59'),(6,4,'2026-07-01','2026-07-03',2600.00,2600.00,'paid','2026-07-17 13:58:13','2026-07-17 13:58:13'),(7,5,'2026-07-01','2026-07-05',4200.00,1600.00,'partial','2026-07-17 13:58:13','2026-07-17 13:58:13');
/*!40000 ALTER TABLE `rent_invoices` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rent_payments`
--

DROP TABLE IF EXISTS `rent_payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rent_payments` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `rent_invoice_id` bigint(20) unsigned NOT NULL,
  `finance_record_id` bigint(20) unsigned NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rent_payments_finance` (`finance_record_id`),
  KEY `idx_rent_payments_invoice` (`rent_invoice_id`),
  CONSTRAINT `fk_rent_payments_finance` FOREIGN KEY (`finance_record_id`) REFERENCES `finance_records` (`id`),
  CONSTRAINT `fk_rent_payments_invoice` FOREIGN KEY (`rent_invoice_id`) REFERENCES `rent_invoices` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rent_payments`
--

LOCK TABLES `rent_payments` WRITE;
/*!40000 ALTER TABLE `rent_payments` DISABLE KEYS */;
INSERT INTO `rent_payments` (`id`, `rent_invoice_id`, `finance_record_id`, `created_at`) VALUES (1,1,1,'2026-07-15 13:52:30'),(2,3,2,'2026-07-15 17:47:26'),(3,5,3,'2026-07-15 17:47:26'),(4,4,6,'2026-07-15 17:47:26'),(5,6,17,'2026-07-17 13:58:13'),(6,7,18,'2026-07-17 13:58:13');
/*!40000 ALTER TABLE `rent_payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `report_definitions`
--

DROP TABLE IF EXISTS `report_definitions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `report_definitions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `report_code` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `report_type` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'property_payment / rent_collection / income_expense / maintenance / reserve / finance / sync',
  `default_format` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PDF',
  `default_filters` json DEFAULT NULL,
  `schedule_cron` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_definitions_code` (`report_code`),
  KEY `idx_report_definitions_type_enabled` (`report_type`,`enabled`),
  KEY `fk_report_definitions_creator` (`created_by`),
  CONSTRAINT `fk_report_definitions_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report_definitions`
--

LOCK TABLES `report_definitions` WRITE;
/*!40000 ALTER TABLE `report_definitions` DISABLE KEYS */;
/*!40000 ALTER TABLE `report_definitions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `report_runs`
--

DROP TABLE IF EXISTS `report_runs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `report_runs` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `report_definition_id` bigint(20) unsigned DEFAULT NULL,
  `report_name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `requested_by` bigint(20) unsigned DEFAULT NULL,
  `date_start` date NOT NULL,
  `date_end` date NOT NULL,
  `project_id` bigint(20) unsigned DEFAULT NULL,
  `output_format` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `filters` json DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'queued',
  `record_count` int(10) unsigned DEFAULT NULL,
  `storage_key` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `error_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `started_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_runs_status_time` (`status`,`created_at`),
  KEY `idx_report_runs_period` (`date_start`,`date_end`),
  KEY `fk_report_runs_definition` (`report_definition_id`),
  KEY `fk_report_runs_requester` (`requested_by`),
  KEY `fk_report_runs_project` (`project_id`),
  CONSTRAINT `fk_report_runs_definition` FOREIGN KEY (`report_definition_id`) REFERENCES `report_definitions` (`id`),
  CONSTRAINT `fk_report_runs_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`),
  CONSTRAINT `fk_report_runs_requester` FOREIGN KEY (`requested_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report_runs`
--

LOCK TABLES `report_runs` WRITE;
/*!40000 ALTER TABLE `report_runs` DISABLE KEYS */;
/*!40000 ALTER TABLE `report_runs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reserve_accounts`
--

DROP TABLE IF EXISTS `reserve_accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reserve_accounts` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `owner_unit_id` bigint(20) unsigned NOT NULL,
  `minimum_balance` decimal(18,2) NOT NULL DEFAULT '0.00',
  `current_balance` decimal(18,2) NOT NULL DEFAULT '0.00',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `low_balance_alert_enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reserve_accounts_owner_unit` (`owner_unit_id`),
  KEY `idx_reserve_accounts_balance` (`current_balance`,`minimum_balance`),
  CONSTRAINT `fk_reserve_accounts_owner_unit` FOREIGN KEY (`owner_unit_id`) REFERENCES `owner_units` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reserve_accounts`
--

LOCK TABLES `reserve_accounts` WRITE;
/*!40000 ALTER TABLE `reserve_accounts` DISABLE KEYS */;
INSERT INTO `reserve_accounts` (`id`, `owner_unit_id`, `minimum_balance`, `current_balance`, `status`, `low_balance_alert_enabled`, `created_at`, `updated_at`) VALUES (1,3,2000.00,3601.30,'active',1,'2026-07-15 15:31:14','2026-07-17 15:25:36'),(2,4,2500.00,2430.50,'active',1,'2026-07-15 15:31:14','2026-07-17 15:16:06'),(3,5,1500.00,1020.00,'active',1,'2026-07-15 15:31:14','2026-07-17 15:16:06'),(4,6,2000.00,3000.00,'active',1,'2026-07-15 15:31:14','2026-07-15 15:31:14'),(5,10,2500.00,6720.00,'active',1,'2026-07-17 13:58:13','2026-07-17 15:16:06'),(6,11,4000.00,600.00,'active',1,'2026-07-17 13:58:13','2026-07-17 15:16:06'),(7,12,3000.00,4920.00,'active',1,'2026-07-17 13:58:13','2026-07-17 15:16:06');
/*!40000 ALTER TABLE `reserve_accounts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reserve_transactions`
--

DROP TABLE IF EXISTS `reserve_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reserve_transactions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `reserve_account_id` bigint(20) unsigned NOT NULL,
  `finance_record_id` bigint(20) unsigned DEFAULT NULL,
  `maintenance_work_order_id` bigint(20) unsigned DEFAULT NULL,
  `transaction_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'topup / debit / adjustment',
  `amount` decimal(18,2) NOT NULL,
  `occurred_at` datetime NOT NULL,
  `balance_after` decimal(18,2) NOT NULL,
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reserve_transactions_account_date` (`reserve_account_id`,`occurred_at`),
  KEY `idx_reserve_transactions_type` (`transaction_type`,`occurred_at`),
  KEY `fk_reserve_transactions_finance` (`finance_record_id`),
  KEY `fk_reserve_transactions_work_order` (`maintenance_work_order_id`),
  KEY `fk_reserve_transactions_creator` (`created_by`),
  CONSTRAINT `fk_reserve_transactions_account` FOREIGN KEY (`reserve_account_id`) REFERENCES `reserve_accounts` (`id`),
  CONSTRAINT `fk_reserve_transactions_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_reserve_transactions_finance` FOREIGN KEY (`finance_record_id`) REFERENCES `finance_records` (`id`),
  CONSTRAINT `fk_reserve_transactions_work_order` FOREIGN KEY (`maintenance_work_order_id`) REFERENCES `maintenance_work_orders` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reserve_transactions`
--

LOCK TABLES `reserve_transactions` WRITE;
/*!40000 ALTER TABLE `reserve_transactions` DISABLE KEYS */;
INSERT INTO `reserve_transactions` (`id`, `reserve_account_id`, `finance_record_id`, `maintenance_work_order_id`, `transaction_type`, `amount`, `occurred_at`, `balance_after`, `note`, `created_by`, `created_at`) VALUES (1,2,9,1,'debit',480.00,'2026-07-12 09:00:00',3380.50,'廚房水管維修費由預備金扣除',1,'2026-07-16 10:24:25'),(2,1,NULL,NULL,'topup',4500.00,'2026-03-01 09:00:00',4500.00,'ADMIN opening reserve balance A-0301',1,'2026-07-16 11:19:24'),(3,2,NULL,NULL,'topup',3860.50,'2026-03-01 09:10:00',3860.50,'ADMIN opening reserve balance A-0906',1,'2026-07-16 11:19:24'),(4,3,NULL,NULL,'topup',1500.00,'2026-04-01 09:00:00',1500.00,'ADMIN opening reserve balance B-0602',1,'2026-07-16 11:19:24'),(5,4,NULL,NULL,'topup',3000.00,'2026-04-01 09:10:00',3000.00,'ADMIN opening reserve balance B-1508',1,'2026-07-16 11:19:24'),(6,5,NULL,NULL,'topup',8000.00,'2026-02-17 13:58:13',8000.00,'Opening reserve funding',1,'2026-07-17 13:58:13'),(7,5,NULL,NULL,'debit',800.00,'2026-06-27 13:58:13',7200.00,'Air-conditioning service and management charges',1,'2026-07-17 13:58:13'),(8,6,NULL,NULL,'topup',5500.00,'2025-12-19 13:58:13',5500.00,'Opening reserve funding',1,'2026-07-17 13:58:13'),(9,6,NULL,NULL,'debit',3650.00,'2026-07-05 13:58:13',1850.00,'Plumbing repair and sinking fund charges',1,'2026-07-17 13:58:13'),(10,7,NULL,NULL,'topup',6500.00,'2026-04-08 13:58:13',6500.00,'Opening reserve funding',1,'2026-07-17 13:58:13'),(11,7,NULL,NULL,'debit',900.00,'2026-07-02 13:58:13',5600.00,'Pre-sale cleaning and minor touch-up',1,'2026-07-17 13:58:13'),(12,1,13,NULL,'debit',612.30,'2026-06-20 00:00:00',3887.70,'支出由預備金自動扣除',NULL,'2026-07-17 15:16:06'),(13,3,12,NULL,'debit',420.00,'2026-07-05 00:00:00',1080.00,'支出由預備金自動扣除',NULL,'2026-07-17 15:16:06'),(14,5,19,NULL,'debit',480.00,'2026-07-06 00:00:00',6720.00,'支出由預備金自動扣除',NULL,'2026-07-17 15:16:06'),(15,3,11,NULL,'debit',60.00,'2026-07-07 00:00:00',1020.00,'支出由預備金自動扣除',NULL,'2026-07-17 15:16:06'),(16,7,21,NULL,'debit',680.00,'2026-07-07 00:00:00',4920.00,'支出由預備金自動扣除',NULL,'2026-07-17 15:16:06'),(18,6,20,NULL,'debit',1250.00,'2026-07-10 00:00:00',600.00,'支出由預備金自動扣除',NULL,'2026-07-17 15:16:06'),(19,2,8,NULL,'debit',950.00,'2026-07-12 00:00:00',2430.50,'支出由預備金自動扣除',NULL,'2026-07-17 15:16:06'),(20,1,7,NULL,'debit',286.40,'2026-07-15 00:00:00',3251.30,'支出由預備金自動扣除',NULL,'2026-07-17 15:16:06');
/*!40000 ALTER TABLE `reserve_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `role_permissions`
--

DROP TABLE IF EXISTS `role_permissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `role_permissions` (
  `role_id` bigint(20) unsigned NOT NULL,
  `permission_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`role_id`,`permission_id`),
  KEY `fk_role_permissions_permission` (`permission_id`),
  CONSTRAINT `fk_role_permissions_permission` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`),
  CONSTRAINT `fk_role_permissions_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `role_permissions`
--

LOCK TABLES `role_permissions` WRITE;
/*!40000 ALTER TABLE `role_permissions` DISABLE KEYS */;
/*!40000 ALTER TABLE `role_permissions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `roles` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_roles_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` (`id`, `code`, `name`) VALUES (1,'ADMIN','System Administrator'),(2,'OWNER','Property Owner');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sync_batch_items`
--

DROP TABLE IF EXISTS `sync_batch_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sync_batch_items` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `batch_id` bigint(20) unsigned NOT NULL,
  `entity_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity_id` bigint(20) unsigned DEFAULT NULL,
  `operation` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'upsert',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `external_id` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `error_message` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_payload` json DEFAULT NULL,
  `synced_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sync_items_batch_status` (`batch_id`,`status`),
  KEY `idx_sync_items_entity` (`entity_type`,`entity_id`),
  CONSTRAINT `fk_sync_items_batch` FOREIGN KEY (`batch_id`) REFERENCES `sync_batches` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sync_batch_items`
--

LOCK TABLES `sync_batch_items` WRITE;
/*!40000 ALTER TABLE `sync_batch_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `sync_batch_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sync_batches`
--

DROP TABLE IF EXISTS `sync_batches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sync_batches` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_module` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `trigger_mode` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'manual',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `total_count` int(10) unsigned NOT NULL DEFAULT '0',
  `success_count` int(10) unsigned NOT NULL DEFAULT '0',
  `failure_count` int(10) unsigned NOT NULL DEFAULT '0',
  `started_at` datetime DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sync_batches_no` (`batch_no`),
  KEY `idx_sync_batches_status_time` (`status`,`created_at`),
  KEY `fk_sync_batches_creator` (`created_by`),
  CONSTRAINT `fk_sync_batches_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sync_batches`
--

LOCK TABLES `sync_batches` WRITE;
/*!40000 ALTER TABLE `sync_batches` DISABLE KEYS */;
/*!40000 ALTER TABLE `sync_batches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenants`
--

DROP TABLE IF EXISTS `tenants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tenants` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) unsigned DEFAULT NULL,
  `full_name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `identity_no` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '敏感資料；應在應用層或欄位層加密，禁止寫入日誌',
  `phone` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(190) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tenants_name` (`full_name`),
  KEY `idx_tenants_phone` (`phone`),
  KEY `idx_tenants_user` (`user_id`),
  CONSTRAINT `fk_tenants_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenants`
--

LOCK TABLES `tenants` WRITE;
/*!40000 ALTER TABLE `tenants` DISABLE KEYS */;
INSERT INTO `tenants` (`id`, `user_id`, `full_name`, `identity_no`, `phone`, `email`, `status`, `created_at`, `updated_at`) VALUES (1,NULL,'TEST Tenant One',NULL,'+60110000011','test.tenant1@example.com','active','2026-07-15 13:52:30','2026-07-15 13:52:30'),(2,NULL,'TEST Tenant Two',NULL,'+60110000012','test.tenant2@example.com','active','2026-07-15 13:52:30','2026-07-15 13:52:30'),(3,NULL,'ADMIN Test Tenant One',NULL,'+60110000901','admin.test.tenant1@example.com','active','2026-07-15 14:09:59','2026-07-15 14:09:59'),(4,NULL,'ADMIN Test Tenant Two',NULL,'+60110000902','admin.test.tenant2@example.com','active','2026-07-15 14:09:59','2026-07-15 14:09:59'),(5,NULL,'Nur Aisyah Binti Rahman','920415-10-5188','+60176543210','aisyah.rahman@example.com','active','2026-07-17 13:58:13','2026-07-17 13:58:13'),(6,NULL,'Daniel Lee Jian Wei','880722-14-6023','+60129876543','daniel.lee@example.com','active','2026-07-17 13:58:13','2026-07-17 13:58:13');
/*!40000 ALTER TABLE `tenants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `units`
--

DROP TABLE IF EXISTS `units`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `units` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `project_id` bigint(20) unsigned NOT NULL,
  `building` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `floor_no` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `unit_no` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `unit_type` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `area_sqm` decimal(12,2) DEFAULT NULL,
  `bedroom_count` tinyint(3) unsigned DEFAULT NULL,
  `listing_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'available',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_units_project_unit` (`project_id`,`unit_no`),
  KEY `idx_units_project_status` (`project_id`,`listing_status`),
  CONSTRAINT `fk_units_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `units`
--

LOCK TABLES `units` WRITE;
/*!40000 ALTER TABLE `units` DISABLE KEYS */;
INSERT INTO `units` (`id`, `project_id`, `building`, `floor_no`, `unit_no`, `unit_type`, `area_sqm`, `bedroom_count`, `listing_status`, `created_at`, `updated_at`) VALUES (1,5,'A','08','A-08-01','2 Bedroom',82.50,2,'reserved','2026-07-15 13:52:30','2026-07-17 13:58:13'),(2,1,'A','12','A-1208','3BR',102.00,3,'reserved','2026-07-15 13:52:30','2026-07-15 13:52:30'),(3,2,'B','05','B-0503','1BR',52.00,1,'occupied','2026-07-15 13:52:30','2026-07-15 13:52:30'),(4,2,'B','18','B-1802','2BR',81.25,2,'available','2026-07-15 13:52:30','2026-07-15 13:52:30'),(5,3,'A','03','ADMIN-A-0301','2BR',75.50,2,'available','2026-07-15 14:09:59','2026-07-15 14:09:59'),(6,3,'A','09','ADMIN-A-0906','3BR',110.00,3,'occupied','2026-07-15 14:09:59','2026-07-15 14:09:59'),(7,4,'B','06','ADMIN-B-0602','1BR',48.00,1,'reserved','2026-07-15 14:09:59','2026-07-15 14:09:59'),(8,4,'B','15','ADMIN-B-1508','2BR',84.25,2,'available','2026-07-15 14:09:59','2026-07-15 14:09:59'),(9,6,'B','12','B-12-05','3 Bedroom',108.80,3,'reserved','2026-07-17 13:58:13','2026-07-17 13:58:13'),(10,7,'C','05','C-05-08','1+1 Bedroom',61.20,2,'reserved','2026-07-17 13:58:13','2026-07-17 13:58:13'),(11,8,'D','18','D-18-02','3 Bedroom Sea View',132.40,3,'sold','2026-07-17 13:58:13','2026-07-17 13:58:13'),(12,9,'E','09','E-09-03','1 Bedroom',52.60,1,'rented','2026-07-17 13:58:13','2026-07-17 13:58:13'),(13,10,'F','16','F-16-06','3 Bedroom',124.70,3,'rented','2026-07-17 13:58:13','2026-07-17 13:58:13'),(14,11,'G','21','G-21-01','2 Bedroom',88.30,2,'available','2026-07-17 13:58:13','2026-07-17 13:58:13'),(15,6,'1','1','1111','111',111111.00,2,'available','2026-07-17 15:30:02','2026-07-17 15:30:02');
/*!40000 ALTER TABLE `units` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `user_roles` (
  `user_id` bigint(20) unsigned NOT NULL,
  `role_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `fk_user_roles_role` (`role_id`),
  CONSTRAINT `fk_user_roles_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `fk_user_roles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` (`user_id`, `role_id`) VALUES (1,1),(2,1),(3,1),(4,2),(5,2),(6,2);
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(190) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `display_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `account_type` enum('ADMIN','OWNER') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OWNER' COMMENT 'Login portal type; admin and owner are mutually exclusive',
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `last_login_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_username` (`username`),
  UNIQUE KEY `uk_users_email` (`email`),
  KEY `idx_users_status` (`status`),
  KEY `idx_users_account_type_status` (`account_type`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` (`id`, `username`, `email`, `password_hash`, `display_name`, `phone`, `account_type`, `status`, `last_login_at`, `created_at`, `updated_at`) VALUES (1,'admin','admin@example.com','$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW','System Administrator',NULL,'ADMIN','active','2026-07-17 17:35:40','2026-07-15 14:01:33','2026-07-17 11:34:04'),(2,'admin.ops','admin.ops@example.com','$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW','Operations Administrator','+60110000101','ADMIN','active','2026-07-17 12:13:29','2026-07-17 11:34:26','2026-07-17 11:34:26'),(3,'admin.finance','admin.finance@example.com','$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW','Finance Administrator','+60110000102','ADMIN','active','2026-07-17 12:13:29','2026-07-17 11:34:26','2026-07-17 11:34:26'),(4,'owner','owner@example.com','$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW','Tan Wei Ming','+60123456789','OWNER','active','2026-07-17 17:35:19','2026-07-17 11:34:26','2026-07-17 13:58:13'),(5,'owner2','owner2@example.com','$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW','TEST Owner Two','+60110000002','OWNER','active','2026-07-17 12:13:29','2026-07-17 11:34:26','2026-07-17 11:34:26'),(6,'owner3','owner3@example.com','$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW','ADMIN Test Portfolio Owner','+60110000999','OWNER','active','2026-07-17 13:45:39','2026-07-17 11:34:26','2026-07-17 11:34:26');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary table structure for view `v_finance_review_queue`
--

DROP TABLE IF EXISTS `v_finance_review_queue`;
/*!50001 DROP VIEW IF EXISTS `v_finance_review_queue`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `v_finance_review_queue` AS SELECT 
 1 AS `id`,
 1 AS `transaction_no`,
 1 AS `record_type`,
 1 AS `unit_id`,
 1 AS `owner_id`,
 1 AS `tenant_id`,
 1 AS `amount`,
 1 AS `currency`,
 1 AS `transaction_date`,
 1 AS `payment_status`,
 1 AS `confirmation_status`,
 1 AS `sync_status`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `v_rent_collection_status`
--

DROP TABLE IF EXISTS `v_rent_collection_status`;
/*!50001 DROP VIEW IF EXISTS `v_rent_collection_status`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `v_rent_collection_status` AS SELECT 
 1 AS `rent_invoice_id`,
 1 AS `lease_id`,
 1 AS `unit_id`,
 1 AS `tenant_id`,
 1 AS `billing_month`,
 1 AS `due_date`,
 1 AS `amount_due`,
 1 AS `amount_paid`,
 1 AS `unpaid_amount`,
 1 AS `calculated_status`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `v_reserve_balances`
--

DROP TABLE IF EXISTS `v_reserve_balances`;
/*!50001 DROP VIEW IF EXISTS `v_reserve_balances`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `v_reserve_balances` AS SELECT 
 1 AS `reserve_account_id`,
 1 AS `owner_id`,
 1 AS `unit_id`,
 1 AS `minimum_balance`,
 1 AS `current_balance`,
 1 AS `shortage_amount`,
 1 AS `calculated_status`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `v_sync_batch_summary`
--

DROP TABLE IF EXISTS `v_sync_batch_summary`;
/*!50001 DROP VIEW IF EXISTS `v_sync_batch_summary`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `v_sync_batch_summary` AS SELECT 
 1 AS `id`,
 1 AS `batch_no`,
 1 AS `source_module`,
 1 AS `trigger_mode`,
 1 AS `status`,
 1 AS `total_count`,
 1 AS `success_count`,
 1 AS `failure_count`,
 1 AS `success_rate`,
 1 AS `started_at`,
 1 AS `completed_at`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary table structure for view `v_unit_payment_progress`
--

DROP TABLE IF EXISTS `v_unit_payment_progress`;
/*!50001 DROP VIEW IF EXISTS `v_unit_payment_progress`*/;
SET @saved_cs_client     = @@character_set_client;
SET character_set_client = utf8;
/*!50001 CREATE VIEW `v_unit_payment_progress` AS SELECT 
 1 AS `owner_id`,
 1 AS `unit_id`,
 1 AS `unit_no`,
 1 AS `project_name`,
 1 AS `purchase_price`,
 1 AS `scheduled_amount`,
 1 AS `paid_amount`,
 1 AS `unpaid_amount`,
 1 AS `last_due_date`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `vendors`
--

DROP TABLE IF EXISTS `vendors`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vendors` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vendor_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contact_name` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(190) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_vendors_code` (`vendor_code`),
  KEY `idx_vendors_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vendors`
--

LOCK TABLES `vendors` WRITE;
/*!40000 ALTER TABLE `vendors` DISABLE KEYS */;
INSERT INTO `vendors` (`id`, `vendor_code`, `name`, `contact_name`, `phone`, `email`, `status`, `created_at`, `updated_at`) VALUES (1,'TEST-20260715-V01','TEST Bright Maintenance','TEST Vendor Contact','+60110000021','test.vendor@example.com','active','2026-07-15 13:52:30','2026-07-15 13:52:30'),(2,'ADMIN-TEST-20260716-V01','ADMIN Test Property Care','Maintenance Desk','+60110000888','maintenance@example.com','active','2026-07-16 10:24:25','2026-07-16 10:24:25'),(3,'OWNER-DEMO-V01','UrbanCare Property Services','Service Desk','+60327881288','service@urbancare.example.com','active','2026-07-17 13:58:13','2026-07-17 13:58:13'),(4,'OWNER-DEMO-V02','AquaFix Plumbing Solutions','Jason Lim','+60163384722','jason@aquafix.example.com','active','2026-07-17 13:58:13','2026-07-17 13:58:13');
/*!40000 ALTER TABLE `vendors` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'ccps_property_management'
--

--
-- Dumping routines for database 'ccps_property_management'
--

--
-- Final view structure for view `v_finance_review_queue`
--

/*!50001 DROP VIEW IF EXISTS `v_finance_review_queue`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_finance_review_queue` AS select `fr`.`id` AS `id`,`fr`.`transaction_no` AS `transaction_no`,`fr`.`record_type` AS `record_type`,`fr`.`unit_id` AS `unit_id`,`fr`.`owner_id` AS `owner_id`,`fr`.`tenant_id` AS `tenant_id`,`fr`.`amount` AS `amount`,`fr`.`currency` AS `currency`,`fr`.`transaction_date` AS `transaction_date`,`fr`.`payment_status` AS `payment_status`,`fr`.`confirmation_status` AS `confirmation_status`,`fr`.`sync_status` AS `sync_status` from `finance_records` `fr` where ((`fr`.`confirmation_status` in ('pending','partial','rejected')) or (`fr`.`payment_status` in ('unpaid','partial','overdue'))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_rent_collection_status`
--

/*!50001 DROP VIEW IF EXISTS `v_rent_collection_status`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_rent_collection_status` AS select `ri`.`id` AS `rent_invoice_id`,`l`.`id` AS `lease_id`,`l`.`unit_id` AS `unit_id`,`l`.`tenant_id` AS `tenant_id`,`ri`.`billing_month` AS `billing_month`,`ri`.`due_date` AS `due_date`,`ri`.`amount_due` AS `amount_due`,`ri`.`amount_paid` AS `amount_paid`,greatest((`ri`.`amount_due` - `ri`.`amount_paid`),0) AS `unpaid_amount`,(case when (`ri`.`amount_paid` >= `ri`.`amount_due`) then 'paid' when (`ri`.`amount_paid` > 0) then 'partial' when (`ri`.`due_date` < curdate()) then 'overdue' else 'unpaid' end) AS `calculated_status` from (`rent_invoices` `ri` join `leases` `l` on((`l`.`id` = `ri`.`lease_id`))) where exists(select 1 from (`owner_units` `ou` join `owner_unit_services` `ous` on((`ous`.`owner_unit_id` = `ou`.`id`))) where ((`ou`.`unit_id` = `l`.`unit_id`) and (`ou`.`status` = 'active') and (`ou`.`asset_stage` = 'OPERATING') and (`ous`.`service_type` = 'RENTAL') and (`ous`.`status` = 'active'))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_reserve_balances`
--

/*!50001 DROP VIEW IF EXISTS `v_reserve_balances`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_reserve_balances` AS select `ra`.`id` AS `reserve_account_id`,`ou`.`owner_id` AS `owner_id`,`ou`.`unit_id` AS `unit_id`,`ra`.`minimum_balance` AS `minimum_balance`,`ra`.`current_balance` AS `current_balance`,greatest((`ra`.`minimum_balance` - `ra`.`current_balance`),0) AS `shortage_amount`,(case when (`ra`.`current_balance` < `ra`.`minimum_balance`) then 'low' else 'normal' end) AS `calculated_status` from (`reserve_accounts` `ra` join `owner_units` `ou` on((`ou`.`id` = `ra`.`owner_unit_id`))) where ((`ra`.`status` = 'active') and (`ou`.`status` = 'active') and (`ou`.`asset_stage` = 'OPERATING')) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_sync_batch_summary`
--

/*!50001 DROP VIEW IF EXISTS `v_sync_batch_summary`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_sync_batch_summary` AS select `sb`.`id` AS `id`,`sb`.`batch_no` AS `batch_no`,`sb`.`source_module` AS `source_module`,`sb`.`trigger_mode` AS `trigger_mode`,`sb`.`status` AS `status`,`sb`.`total_count` AS `total_count`,`sb`.`success_count` AS `success_count`,`sb`.`failure_count` AS `failure_count`,round(((`sb`.`success_count` / nullif(`sb`.`total_count`,0)) * 100),2) AS `success_rate`,`sb`.`started_at` AS `started_at`,`sb`.`completed_at` AS `completed_at` from `sync_batches` `sb` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `v_unit_payment_progress`
--

/*!50001 DROP VIEW IF EXISTS `v_unit_payment_progress`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_general_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `v_unit_payment_progress` AS select `ou`.`owner_id` AS `owner_id`,`ou`.`unit_id` AS `unit_id`,`u`.`unit_no` AS `unit_no`,`p`.`name` AS `project_name`,`pc`.`purchase_price` AS `purchase_price`,coalesce(sum(`pi`.`amount_due`),0) AS `scheduled_amount`,coalesce(sum(`pi`.`amount_paid`),0) AS `paid_amount`,greatest((coalesce(sum(`pi`.`amount_due`),0) - coalesce(sum(`pi`.`amount_paid`),0)),0) AS `unpaid_amount`,max(`pi`.`due_date`) AS `last_due_date` from (((((`owner_units` `ou` join `units` `u` on((`u`.`id` = `ou`.`unit_id`))) join `projects` `p` on((`p`.`id` = `u`.`project_id`))) left join `purchase_contracts` `pc` on(((`pc`.`owner_unit_id` = `ou`.`id`) and (`pc`.`status` = 'active')))) left join `payment_plans` `pp` on(((`pp`.`purchase_contract_id` = `pc`.`id`) and (`pp`.`status` = 'active')))) left join `payment_installments` `pi` on((`pi`.`payment_plan_id` = `pp`.`id`))) where ((`ou`.`status` = 'active') and (`ou`.`asset_stage` = 'PRE_HANDOVER')) group by `ou`.`owner_id`,`ou`.`unit_id`,`u`.`unit_no`,`p`.`name`,`pc`.`purchase_price` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-17 18:11:50
