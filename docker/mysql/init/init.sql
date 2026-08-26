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
) ENGINE=InnoDB AUTO_INCREMENT=243 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `lease_id` bigint(20) unsigned DEFAULT NULL,
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
  KEY `idx_cashflow_lease` (`lease_id`),
  CONSTRAINT `fk_cashflow_finance` FOREIGN KEY (`finance_record_id`) REFERENCES `finance_records` (`id`),
  CONSTRAINT `fk_cashflow_lease` FOREIGN KEY (`lease_id`) REFERENCES `leases` (`id`),
  CONSTRAINT `fk_cashflow_owner` FOREIGN KEY (`owner_id`) REFERENCES `owners` (`id`),
  CONSTRAINT `fk_cashflow_reserve_account` FOREIGN KEY (`reserve_account_id`) REFERENCES `reserve_accounts` (`id`),
  CONSTRAINT `fk_cashflow_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`),
  CONSTRAINT `fk_cashflow_unit` FOREIGN KEY (`unit_id`) REFERENCES `units` (`id`),
  CONSTRAINT `fk_cashflow_vendor` FOREIGN KEY (`vendor_id`) REFERENCES `vendors` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=40 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cashflow_entries`
--

LOCK TABLES `cashflow_entries` WRITE;
/*!40000 ALTER TABLE `cashflow_entries` DISABLE KEYS */;
/*!40000 ALTER TABLE `cashflow_entries` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER trg_cashflow_expense_reserve_before
BEFORE INSERT ON cashflow_entries
FOR EACH ROW
BEGIN
  DECLARE v_amount DECIMAL(18,2);
  DECLARE v_reserve_id BIGINT UNSIGNED;
  DECLARE v_balance DECIMAL(18,2);
  DECLARE v_payment_status VARCHAR(30);
  DECLARE v_confirmation_status VARCHAR(30);

  IF NEW.direction = 'expense' THEN
    SET v_amount = NULL;
    SET v_reserve_id = NULL;
    SET v_balance = NULL;
    SELECT fr.amount, fr.payment_status, fr.confirmation_status
      INTO v_amount, v_payment_status, v_confirmation_status
      FROM finance_records fr
     WHERE fr.id = NEW.finance_record_id;
    IF v_payment_status = 'paid' AND v_confirmation_status = 'confirmed' AND NEW.reserve_account_id IS NOT NULL THEN
      SELECT ra.current_balance
        INTO v_balance
        FROM reserve_accounts ra
       WHERE ra.id = NEW.reserve_account_id
       FOR UPDATE;
      IF v_amount IS NULL OR v_balance IS NULL OR v_balance < v_amount THEN
        SET NEW.reserve_account_id = NULL;
      ELSE
        SET NEW.attachment_status = 'not_required';
      END IF;
    END IF;
    IF v_payment_status = 'paid' AND v_confirmation_status = 'confirmed' AND NEW.reserve_account_id IS NULL THEN
      SET v_balance = NULL;
    SELECT ra.id, ra.current_balance
      INTO v_reserve_id, v_balance
      FROM reserve_accounts ra
      JOIN owner_units ou ON ou.id = ra.owner_unit_id
     WHERE ou.unit_id = NEW.unit_id
       AND (NEW.owner_id IS NULL OR ou.owner_id = NEW.owner_id)
       AND ou.status = 'active'
       AND ou.asset_stage = 'OPERATING'
       AND ra.status = 'active'
     ORDER BY ra.id
     LIMIT 1
     FOR UPDATE;
      IF v_reserve_id IS NOT NULL AND v_amount IS NOT NULL AND v_balance >= v_amount THEN
        SET NEW.reserve_account_id = v_reserve_id;
        SET NEW.attachment_status = 'not_required';
      END IF;
    ELSEIF v_payment_status <> 'paid' OR v_confirmation_status <> 'confirmed' THEN
      SET NEW.reserve_account_id = NULL;
    END IF;
  END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER trg_cashflow_expense_reserve_after
AFTER INSERT ON cashflow_entries
FOR EACH ROW
BEGIN
  DECLARE v_amount DECIMAL(18,2);
  DECLARE v_balance DECIMAL(18,2);
  DECLARE v_after DECIMAL(18,2);
  DECLARE v_payment_status VARCHAR(30);
  DECLARE v_confirmation_status VARCHAR(30);

  IF NEW.direction = 'expense' AND NEW.reserve_account_id IS NOT NULL
     AND NOT EXISTS (
       SELECT 1 FROM reserve_transactions rt
        WHERE rt.finance_record_id = NEW.finance_record_id
          AND rt.transaction_type = 'debit'
     ) THEN
    SELECT fr.amount, fr.payment_status, fr.confirmation_status
      INTO v_amount, v_payment_status, v_confirmation_status
      FROM finance_records fr
     WHERE fr.id = NEW.finance_record_id;
    SELECT ra.current_balance
      INTO v_balance
      FROM reserve_accounts ra
     WHERE ra.id = NEW.reserve_account_id
     FOR UPDATE;
    IF v_payment_status = 'paid' AND v_confirmation_status = 'confirmed'
       AND v_amount IS NOT NULL AND v_balance >= v_amount THEN
      SET v_after = v_balance - v_amount;
      UPDATE reserve_accounts
         SET current_balance = v_after
       WHERE id = NEW.reserve_account_id;
      INSERT INTO reserve_transactions
        (reserve_account_id, finance_record_id, transaction_type, amount,
         occurred_at, balance_after, note, created_by)
      VALUES
        (NEW.reserve_account_id, NEW.finance_record_id, 'debit', v_amount,
         NOW(), v_after, '支出由預備金自動扣除', NULL);
      UPDATE finance_records
         SET payment_method = 'reserve_account',
             payment_status = 'paid',
             confirmation_status = 'confirmed',
             confirmed_at = COALESCE(confirmed_at, NOW())
       WHERE id = NEW.finance_record_id;
    END IF;
  END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

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
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document_links`
--

LOCK TABLES `document_links` WRITE;
/*!40000 ALTER TABLE `document_links` DISABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=70 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
/*!40000 ALTER TABLE `documents` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `electronic_signature_events`
--

DROP TABLE IF EXISTS `electronic_signature_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `electronic_signature_events` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `signature_request_id` bigint(20) unsigned NOT NULL,
  `event_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `detail` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remote_ip` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_e_signature_events_request` (`signature_request_id`,`created_at`),
  CONSTRAINT `fk_e_signature_events_request` FOREIGN KEY (`signature_request_id`) REFERENCES `electronic_signature_requests` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `electronic_signature_events`
--

LOCK TABLES `electronic_signature_events` WRITE;
/*!40000 ALTER TABLE `electronic_signature_events` DISABLE KEYS */;
/*!40000 ALTER TABLE `electronic_signature_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `electronic_signature_requests`
--

DROP TABLE IF EXISTS `electronic_signature_requests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `electronic_signature_requests` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `source_document_id` bigint(20) unsigned NOT NULL,
  `entity_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity_id` bigint(20) unsigned NOT NULL,
  `signer_name` varchar(190) COLLATE utf8mb4_unicode_ci NOT NULL,
  `signer_email` varchar(190) COLLATE utf8mb4_unicode_ci NOT NULL,
  `access_token_hash` char(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `verification_code_hash` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `verification_expires_at` datetime NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'pending',
  `expires_at` datetime NOT NULL,
  `requested_by` bigint(20) unsigned DEFAULT NULL,
  `requested_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `signed_at` datetime DEFAULT NULL,
  `signed_document_id` bigint(20) unsigned DEFAULT NULL,
  `source_checksum_sha256` char(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signature_hash` char(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signer_ip` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signer_user_agent` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_e_signature_token` (`access_token_hash`),
  KEY `idx_e_signature_entity` (`entity_type`,`entity_id`,`status`),
  KEY `idx_e_signature_expiry` (`status`,`expires_at`),
  KEY `fk_e_signature_source_document` (`source_document_id`),
  KEY `fk_e_signature_signed_document` (`signed_document_id`),
  KEY `fk_e_signature_requester` (`requested_by`),
  CONSTRAINT `fk_e_signature_requester` FOREIGN KEY (`requested_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_e_signature_signed_document` FOREIGN KEY (`signed_document_id`) REFERENCES `documents` (`id`),
  CONSTRAINT `fk_e_signature_source_document` FOREIGN KEY (`source_document_id`) REFERENCES `documents` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `electronic_signature_requests`
--

LOCK TABLES `electronic_signature_requests` WRITE;
/*!40000 ALTER TABLE `electronic_signature_requests` DISABLE KEYS */;
/*!40000 ALTER TABLE `electronic_signature_requests` ENABLE KEYS */;
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
  `receipt_date` date DEFAULT NULL COMMENT 'Actual cash receipt date used for accounting reconciliation',
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
  KEY `idx_finance_records_receipt_date` (`receipt_date`,`record_type`,`confirmation_status`),
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
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `finance_records`
--

LOCK TABLES `finance_records` WRITE;
/*!40000 ALTER TABLE `finance_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `finance_records` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER trg_finance_expense_reserve_before_update
BEFORE UPDATE ON finance_records
FOR EACH ROW
BEGIN
  DECLARE v_cashflow_id BIGINT UNSIGNED;
  DECLARE v_unit_id BIGINT UNSIGNED;
  DECLARE v_owner_id BIGINT UNSIGNED;
  DECLARE v_reserve_id BIGINT UNSIGNED;
  DECLARE v_balance DECIMAL(18,2);

  IF NEW.record_type = 'cashflow'
     AND NEW.payment_status = 'paid'
     AND NEW.confirmation_status = 'confirmed'
     AND (OLD.payment_status <> NEW.payment_status
          OR OLD.confirmation_status <> NEW.confirmation_status
          OR OLD.amount <> NEW.amount)
     AND NOT EXISTS (SELECT 1 FROM reserve_transactions rt WHERE rt.finance_record_id = NEW.id AND rt.transaction_type = 'debit') THEN
    SET v_cashflow_id = NULL;
    SET v_unit_id = NULL;
    SET v_owner_id = NULL;
    SET v_reserve_id = NULL;
    SET v_balance = NULL;
    SELECT ce.id, ce.unit_id, ce.owner_id
      INTO v_cashflow_id, v_unit_id, v_owner_id
      FROM cashflow_entries ce
     WHERE ce.finance_record_id = NEW.id
     LIMIT 1;
    SELECT ra.id, ra.current_balance
      INTO v_reserve_id, v_balance
      FROM reserve_accounts ra
      JOIN owner_units ou ON ou.id = ra.owner_unit_id
     WHERE ou.unit_id = v_unit_id
       AND (v_owner_id IS NULL OR ou.owner_id = v_owner_id)
       AND ou.status = 'active'
       AND ou.asset_stage = 'OPERATING'
       AND ra.status = 'active'
     ORDER BY ra.id
     LIMIT 1
     FOR UPDATE;
    IF v_reserve_id IS NOT NULL AND v_balance >= NEW.amount THEN
      SET NEW.payment_method = 'reserve_account';
    END IF;
  END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER trg_finance_expense_reserve_after_update
AFTER UPDATE ON finance_records
FOR EACH ROW
BEGIN
  DECLARE v_cashflow_id BIGINT UNSIGNED;
  DECLARE v_unit_id BIGINT UNSIGNED;
  DECLARE v_owner_id BIGINT UNSIGNED;
  DECLARE v_reserve_id BIGINT UNSIGNED;
  DECLARE v_balance DECIMAL(18,2);
  DECLARE v_after DECIMAL(18,2);

  IF NEW.record_type = 'cashflow'
     AND NEW.payment_method = 'reserve_account'
     AND NEW.payment_status = 'paid'
     AND NEW.confirmation_status = 'confirmed'
     AND NOT EXISTS (SELECT 1 FROM reserve_transactions rt WHERE rt.finance_record_id = NEW.id AND rt.transaction_type = 'debit') THEN
    SET v_cashflow_id = NULL;
    SET v_unit_id = NULL;
    SET v_owner_id = NULL;
    SET v_reserve_id = NULL;
    SET v_balance = NULL;
    SELECT ce.id, ce.unit_id, ce.owner_id
      INTO v_cashflow_id, v_unit_id, v_owner_id
      FROM cashflow_entries ce
     WHERE ce.finance_record_id = NEW.id
     LIMIT 1;
    SELECT ra.id, ra.current_balance
      INTO v_reserve_id, v_balance
      FROM reserve_accounts ra
      JOIN owner_units ou ON ou.id = ra.owner_unit_id
     WHERE ou.unit_id = v_unit_id
       AND (v_owner_id IS NULL OR ou.owner_id = v_owner_id)
       AND ou.status = 'active'
       AND ou.asset_stage = 'OPERATING'
       AND ra.status = 'active'
     ORDER BY ra.id
     LIMIT 1
     FOR UPDATE;
    IF v_reserve_id IS NOT NULL AND v_balance >= NEW.amount THEN
      SET v_after = v_balance - NEW.amount;
      UPDATE cashflow_entries
         SET reserve_account_id = v_reserve_id,
             attachment_status = 'not_required'
       WHERE id = v_cashflow_id;
      UPDATE reserve_accounts
         SET current_balance = v_after
       WHERE id = v_reserve_id;
      INSERT INTO reserve_transactions
        (reserve_account_id, finance_record_id, transaction_type, amount,
         occurred_at, balance_after, note, created_by)
      VALUES
        (v_reserve_id, NEW.id, 'debit', NEW.amount, COALESCE(NEW.transaction_date, CURRENT_DATE),
         v_after, '支出由預備金自動扣除', NULL);
    END IF;
  END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `lease_rent_credit_allocations`
--

DROP TABLE IF EXISTS `lease_rent_credit_allocations`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `lease_rent_credit_allocations` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `rent_credit_id` bigint(20) unsigned NOT NULL,
  `rent_invoice_id` bigint(20) unsigned NOT NULL,
  `amount` decimal(18,2) NOT NULL,
  `allocation_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'automatic',
  `allocated_by` bigint(20) unsigned DEFAULT NULL,
  `allocated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lease_credit_invoice` (`rent_credit_id`,`rent_invoice_id`),
  KEY `idx_lease_credit_allocation_invoice` (`rent_invoice_id`),
  KEY `fk_lease_credit_allocation_actor` (`allocated_by`),
  CONSTRAINT `fk_lease_credit_allocation_actor` FOREIGN KEY (`allocated_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_lease_credit_allocation_credit` FOREIGN KEY (`rent_credit_id`) REFERENCES `lease_rent_credits` (`id`),
  CONSTRAINT `fk_lease_credit_allocation_invoice` FOREIGN KEY (`rent_invoice_id`) REFERENCES `rent_invoices` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lease_rent_credit_allocations`
--

LOCK TABLES `lease_rent_credit_allocations` WRITE;
/*!40000 ALTER TABLE `lease_rent_credit_allocations` DISABLE KEYS */;
/*!40000 ALTER TABLE `lease_rent_credit_allocations` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lease_rent_credits`
--

DROP TABLE IF EXISTS `lease_rent_credits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `lease_rent_credits` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `lease_id` bigint(20) unsigned NOT NULL,
  `finance_record_id` bigint(20) unsigned NOT NULL,
  `received_amount` decimal(18,2) NOT NULL,
  `allocated_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `remaining_amount` decimal(18,2) NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'available',
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_lease_rent_credit_finance` (`finance_record_id`),
  KEY `idx_lease_rent_credit_available` (`lease_id`,`status`,`created_at`),
  KEY `fk_lease_rent_credit_creator` (`created_by`),
  CONSTRAINT `fk_lease_rent_credit_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_lease_rent_credit_finance` FOREIGN KEY (`finance_record_id`) REFERENCES `finance_records` (`id`),
  CONSTRAINT `fk_lease_rent_credit_lease` FOREIGN KEY (`lease_id`) REFERENCES `leases` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lease_rent_credits`
--

LOCK TABLES `lease_rent_credits` WRITE;
/*!40000 ALTER TABLE `lease_rent_credits` DISABLE KEYS */;
/*!40000 ALTER TABLE `lease_rent_credits` ENABLE KEYS */;
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
  `rental_mandate_id` bigint(20) unsigned DEFAULT NULL,
  `lease_no` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `monthly_rent` decimal(18,2) NOT NULL,
  `deposit_amount` decimal(18,2) NOT NULL DEFAULT '0.00',
  `payment_day` tinyint(3) unsigned NOT NULL DEFAULT '1',
  `rent_calculation_method` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'daily_prorated' COMMENT '按當月實際承租天數折算',
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
  KEY `idx_leases_rental_mandate` (`rental_mandate_id`,`status`),
  CONSTRAINT `fk_leases_document` FOREIGN KEY (`contract_document_id`) REFERENCES `documents` (`id`),
  CONSTRAINT `fk_leases_rental_mandate` FOREIGN KEY (`rental_mandate_id`) REFERENCES `rental_mandates` (`id`),
  CONSTRAINT `fk_leases_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`),
  CONSTRAINT `fk_leases_unit` FOREIGN KEY (`unit_id`) REFERENCES `units` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `leases`
--

LOCK TABLES `leases` WRITE;
/*!40000 ALTER TABLE `leases` DISABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `maintenance_status_history`
--

LOCK TABLES `maintenance_status_history` WRITE;
/*!40000 ALTER TABLE `maintenance_status_history` DISABLE KEYS */;
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
  `lease_id` bigint(20) unsigned DEFAULT NULL,
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
  `payer_name` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bank_name` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_account_no` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fee_account_type` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fee_account_no` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
  KEY `idx_work_orders_lease` (`lease_id`),
  CONSTRAINT `fk_work_orders_cashflow` FOREIGN KEY (`cashflow_entry_id`) REFERENCES `cashflow_entries` (`id`),
  CONSTRAINT `fk_work_orders_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_work_orders_lease` FOREIGN KEY (`lease_id`) REFERENCES `leases` (`id`),
  CONSTRAINT `fk_work_orders_owner` FOREIGN KEY (`owner_id`) REFERENCES `owners` (`id`),
  CONSTRAINT `fk_work_orders_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`),
  CONSTRAINT `fk_work_orders_unit` FOREIGN KEY (`unit_id`) REFERENCES `units` (`id`),
  CONSTRAINT `fk_work_orders_vendor` FOREIGN KEY (`vendor_id`) REFERENCES `vendors` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `maintenance_work_orders`
--

LOCK TABLES `maintenance_work_orders` WRITE;
/*!40000 ALTER TABLE `maintenance_work_orders` DISABLE KEYS */;
/*!40000 ALTER TABLE `maintenance_work_orders` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER trg_work_order_reserve_link_after_insert
AFTER INSERT ON maintenance_work_orders
FOR EACH ROW
BEGIN
  IF NEW.cashflow_entry_id IS NOT NULL THEN
    UPDATE reserve_transactions rt
    JOIN cashflow_entries ce ON ce.finance_record_id = rt.finance_record_id
    SET rt.maintenance_work_order_id = NEW.id
    WHERE ce.id = NEW.cashflow_entry_id
      AND rt.transaction_type = 'debit'
      AND rt.maintenance_work_order_id IS NULL;
  END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER trg_work_order_reserve_link_after_update
AFTER UPDATE ON maintenance_work_orders
FOR EACH ROW
BEGIN
  IF NEW.cashflow_entry_id IS NOT NULL
     AND (OLD.cashflow_entry_id IS NULL OR OLD.cashflow_entry_id <> NEW.cashflow_entry_id) THEN
    UPDATE reserve_transactions rt
    JOIN cashflow_entries ce ON ce.finance_record_id = rt.finance_record_id
    SET rt.maintenance_work_order_id = NEW.id
    WHERE ce.id = NEW.cashflow_entry_id
      AND rt.transaction_type = 'debit'
      AND rt.maintenance_work_order_id IS NULL;
  END IF;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

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
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `owner_units`
--

LOCK TABLES `owner_units` WRITE;
/*!40000 ALTER TABLE `owner_units` DISABLE KEYS */;
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
  `owner_no` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `full_name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `identity_no` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '敏感資料；應在應用層或欄位層加密，禁止寫入日誌',
  `phone` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mobile_phone` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `home_phone` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `office_phone` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `passport_no` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(190) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_owners_name` (`full_name`),
  KEY `idx_owners_phone` (`phone`),
  KEY `idx_owners_user` (`user_id`),
  CONSTRAINT `fk_owners_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `owners`
--

LOCK TABLES `owners` WRITE;
/*!40000 ALTER TABLE `owners` DISABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_installments`
--

LOCK TABLES `payment_installments` WRITE;
/*!40000 ALTER TABLE `payment_installments` DISABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_plans`
--

LOCK TABLES `payment_plans` WRITE;
/*!40000 ALTER TABLE `payment_plans` DISABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_receipt_allocations`
--

LOCK TABLES `payment_receipt_allocations` WRITE;
/*!40000 ALTER TABLE `payment_receipt_allocations` DISABLE KEYS */;
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
  `fee_account_type` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fee_account_no` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
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
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_receipts`
--

LOCK TABLES `payment_receipts` WRITE;
/*!40000 ALTER TABLE `payment_receipts` DISABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `projects`
--

LOCK TABLES `projects` WRITE;
/*!40000 ALTER TABLE `projects` DISABLE KEYS */;
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
/*!40000 ALTER TABLE `properties` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_attachments`
--

DROP TABLE IF EXISTS `property_attachments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `property_attachments` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `owner_unit_id` bigint(20) unsigned NOT NULL,
  `document_id` bigint(20) unsigned NOT NULL,
  `title` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `remarks` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_property_attachments_document` (`document_id`),
  KEY `idx_property_attachments_owner_unit` (`owner_unit_id`,`enabled`,`created_at`),
  KEY `fk_property_attachments_creator` (`created_by`),
  CONSTRAINT `fk_property_attachments_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_property_attachments_document` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`),
  CONSTRAINT `fk_property_attachments_owner_unit` FOREIGN KEY (`owner_unit_id`) REFERENCES `owner_units` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_attachments`
--

LOCK TABLES `property_attachments` WRITE;
/*!40000 ALTER TABLE `property_attachments` DISABLE KEYS */;
/*!40000 ALTER TABLE `property_attachments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_bank_accounts`
--

DROP TABLE IF EXISTS `property_bank_accounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `property_bank_accounts` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `owner_unit_id` bigint(20) unsigned NOT NULL,
  `item_name` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `payment_name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_no` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `remarks` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_property_bank_accounts_owner_unit` (`owner_unit_id`,`id`),
  KEY `fk_property_bank_accounts_creator` (`created_by`),
  CONSTRAINT `fk_property_bank_accounts_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_property_bank_accounts_owner_unit` FOREIGN KEY (`owner_unit_id`) REFERENCES `owner_units` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_bank_accounts`
--

LOCK TABLES `property_bank_accounts` WRITE;
/*!40000 ALTER TABLE `property_bank_accounts` DISABLE KEYS */;
/*!40000 ALTER TABLE `property_bank_accounts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_basic_profiles`
--

DROP TABLE IF EXISTS `property_basic_profiles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `property_basic_profiles` (
  `owner_unit_id` bigint(20) unsigned NOT NULL,
  `profile_json` json NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`owner_unit_id`),
  CONSTRAINT `fk_property_basic_profiles_owner_unit` FOREIGN KEY (`owner_unit_id`) REFERENCES `owner_units` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_basic_profiles`
--

LOCK TABLES `property_basic_profiles` WRITE;
/*!40000 ALTER TABLE `property_basic_profiles` DISABLE KEYS */;
/*!40000 ALTER TABLE `property_basic_profiles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_contract_records`
--

DROP TABLE IF EXISTS `property_contract_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `property_contract_records` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `owner_unit_id` bigint(20) unsigned NOT NULL,
  `lease_id` bigint(20) unsigned DEFAULT NULL,
  `contract_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contract_no` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  `signed_date` date DEFAULT NULL,
  `valid_from` date DEFAULT NULL,
  `valid_to` date DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'active',
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `storage_key` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mime_type` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_size` bigint(20) unsigned NOT NULL,
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_property_contract_records_no` (`owner_unit_id`,`contract_no`),
  UNIQUE KEY `uk_property_contract_records_lease` (`lease_id`),
  KEY `idx_property_contract_records_type` (`owner_unit_id`,`contract_type`,`status`),
  KEY `fk_property_contract_records_creator` (`created_by`),
  CONSTRAINT `fk_property_contract_records_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_property_contract_records_lease` FOREIGN KEY (`lease_id`) REFERENCES `leases` (`id`),
  CONSTRAINT `fk_property_contract_records_owner_unit` FOREIGN KEY (`owner_unit_id`) REFERENCES `owner_units` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_contract_records`
--

LOCK TABLES `property_contract_records` WRITE;
/*!40000 ALTER TABLE `property_contract_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `property_contract_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_expense_postings`
--

DROP TABLE IF EXISTS `property_expense_postings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `property_expense_postings` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `owner_unit_id` bigint(20) unsigned NOT NULL,
  `charge_key` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `charge_name` varchar(180) COLLATE utf8mb4_unicode_ci NOT NULL,
  `period_key` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `finance_record_id` bigint(20) unsigned NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_property_expense_period` (`owner_unit_id`,`charge_key`,`period_key`),
  KEY `idx_property_expense_finance` (`finance_record_id`),
  CONSTRAINT `fk_property_expense_finance` FOREIGN KEY (`finance_record_id`) REFERENCES `finance_records` (`id`),
  CONSTRAINT `fk_property_expense_owner_unit` FOREIGN KEY (`owner_unit_id`) REFERENCES `owner_units` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_expense_postings`
--

LOCK TABLES `property_expense_postings` WRITE;
/*!40000 ALTER TABLE `property_expense_postings` DISABLE KEYS */;
/*!40000 ALTER TABLE `property_expense_postings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_handover_checklist_items`
--

DROP TABLE IF EXISTS `property_handover_checklist_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `property_handover_checklist_items` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `owner_unit_id` bigint(20) unsigned NOT NULL,
  `category` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `default_quantity` varchar(80) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notes` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int(11) NOT NULL DEFAULT '0',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_property_handover_checklist_owner` (`owner_unit_id`,`enabled`,`category`,`sort_order`),
  CONSTRAINT `fk_property_handover_checklist_owner_unit` FOREIGN KEY (`owner_unit_id`) REFERENCES `owner_units` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1979 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_handover_checklist_items`
--

LOCK TABLES `property_handover_checklist_items` WRITE;
/*!40000 ALTER TABLE `property_handover_checklist_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `property_handover_checklist_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_handover_reports`
--

DROP TABLE IF EXISTS `property_handover_reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `property_handover_reports` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `owner_unit_id` bigint(20) unsigned NOT NULL,
  `document_id` bigint(20) unsigned DEFAULT NULL,
  `title` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `report_date` date NOT NULL,
  `tracking_start_date` date DEFAULT NULL,
  `tracking_end_date` date DEFAULT NULL,
  `remarks` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `content_json` mediumtext COLLATE utf8mb4_unicode_ci,
  `completed` tinyint(1) NOT NULL DEFAULT '0',
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_property_handover_reports_document` (`document_id`),
  KEY `idx_property_handover_reports_owner_unit` (`owner_unit_id`,`completed`,`report_date`),
  KEY `fk_property_handover_reports_creator` (`created_by`),
  CONSTRAINT `fk_property_handover_reports_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_property_handover_reports_document` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`),
  CONSTRAINT `fk_property_handover_reports_owner_unit` FOREIGN KEY (`owner_unit_id`) REFERENCES `owner_units` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_handover_reports`
--

LOCK TABLES `property_handover_reports` WRITE;
/*!40000 ALTER TABLE `property_handover_reports` DISABLE KEYS */;
/*!40000 ALTER TABLE `property_handover_reports` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_handovers`
--

DROP TABLE IF EXISTS `property_handovers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `property_handovers` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `mandate_id` bigint(20) unsigned NOT NULL,
  `handover_date` date NOT NULL,
  `condition_summary` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `key_count` int(10) unsigned NOT NULL DEFAULT '0',
  `access_card_count` int(10) unsigned NOT NULL DEFAULT '0',
  `water_meter` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `electricity_meter` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `inventory` json DEFAULT NULL,
  `received_by` varchar(160) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft',
  `completed_by` bigint(20) unsigned DEFAULT NULL,
  `completed_at` datetime DEFAULT NULL,
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_property_handovers_mandate` (`mandate_id`),
  KEY `idx_property_handovers_status` (`status`,`handover_date`),
  KEY `fk_property_handovers_completed_by` (`completed_by`),
  KEY `fk_property_handovers_created_by` (`created_by`),
  CONSTRAINT `fk_property_handovers_completed_by` FOREIGN KEY (`completed_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_property_handovers_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_property_handovers_mandate` FOREIGN KEY (`mandate_id`) REFERENCES `rental_mandates` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_handovers`
--

LOCK TABLES `property_handovers` WRITE;
/*!40000 ALTER TABLE `property_handovers` DISABLE KEYS */;
/*!40000 ALTER TABLE `property_handovers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_important_messages`
--

DROP TABLE IF EXISTS `property_important_messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `property_important_messages` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `owner_unit_id` bigint(20) unsigned NOT NULL,
  `subject` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `announcement_start_date` date NOT NULL,
  `announcement_end_date` date DEFAULT NULL,
  `importance` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'normal',
  `is_read` tinyint(1) NOT NULL DEFAULT '0',
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_property_important_messages_unit_date` (`owner_unit_id`,`announcement_start_date`,`announcement_end_date`),
  KEY `idx_property_important_messages_flags` (`owner_unit_id`,`importance`,`is_read`),
  KEY `fk_property_important_messages_creator` (`created_by`),
  CONSTRAINT `fk_property_important_messages_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_property_important_messages_owner_unit` FOREIGN KEY (`owner_unit_id`) REFERENCES `owner_units` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_important_messages`
--

LOCK TABLES `property_important_messages` WRITE;
/*!40000 ALTER TABLE `property_important_messages` DISABLE KEYS */;
/*!40000 ALTER TABLE `property_important_messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_maintenance_records`
--

DROP TABLE IF EXISTS `property_maintenance_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `property_maintenance_records` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `owner_unit_id` bigint(20) unsigned NOT NULL,
  `work_order_id` bigint(20) unsigned DEFAULT NULL,
  `record_no` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(180) COLLATE utf8mb4_unicode_ci NOT NULL,
  `maintenance_date` date NOT NULL,
  `duration_minutes` int(10) unsigned NOT NULL DEFAULT '0',
  `details` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `result_summary` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `next_maintenance_date` date DEFAULT NULL,
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_property_maintenance_no` (`record_no`),
  UNIQUE KEY `uk_property_maintenance_work_order` (`work_order_id`),
  KEY `idx_property_maintenance_unit_date` (`owner_unit_id`,`maintenance_date`),
  KEY `fk_property_maintenance_creator` (`created_by`),
  CONSTRAINT `fk_property_maintenance_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_property_maintenance_owner_unit` FOREIGN KEY (`owner_unit_id`) REFERENCES `owner_units` (`id`),
  CONSTRAINT `fk_property_maintenance_work_order` FOREIGN KEY (`work_order_id`) REFERENCES `maintenance_work_orders` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_maintenance_records`
--

LOCK TABLES `property_maintenance_records` WRITE;
/*!40000 ALTER TABLE `property_maintenance_records` DISABLE KEYS */;
/*!40000 ALTER TABLE `property_maintenance_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `property_photos`
--

DROP TABLE IF EXISTS `property_photos`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `property_photos` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `owner_unit_id` bigint(20) unsigned NOT NULL,
  `lease_id` bigint(20) unsigned DEFAULT NULL,
  `rental_stage` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'before / after',
  `document_id` bigint(20) unsigned NOT NULL,
  `title` varchar(120) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'interior',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int(11) NOT NULL DEFAULT '0',
  `is_cover` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_property_photos_document` (`document_id`),
  KEY `idx_property_photos_order` (`owner_unit_id`,`sort_order`,`id`),
  KEY `idx_property_photos_lease_stage` (`lease_id`,`rental_stage`),
  CONSTRAINT `fk_property_photos_document` FOREIGN KEY (`document_id`) REFERENCES `documents` (`id`),
  CONSTRAINT `fk_property_photos_lease` FOREIGN KEY (`lease_id`) REFERENCES `leases` (`id`),
  CONSTRAINT `fk_property_photos_owner_unit` FOREIGN KEY (`owner_unit_id`) REFERENCES `owner_units` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_photos`
--

LOCK TABLES `property_photos` WRITE;
/*!40000 ALTER TABLE `property_photos` DISABLE KEYS */;
/*!40000 ALTER TABLE `property_photos` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_contracts`
--

LOCK TABLES `purchase_contracts` WRITE;
/*!40000 ALTER TABLE `purchase_contracts` DISABLE KEYS */;
/*!40000 ALTER TABLE `purchase_contracts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rent_invoice_items`
--

DROP TABLE IF EXISTS `rent_invoice_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rent_invoice_items` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint(20) unsigned NOT NULL,
  `charge_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'management / utilities / maintenance / other',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `amount` decimal(18,2) NOT NULL,
  `payer` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'tenant' COMMENT 'tenant / owner / agency',
  `source_type` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'manual / work_order / utility',
  `source_id` bigint(20) unsigned DEFAULT NULL,
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_invoice_items_invoice` (`invoice_id`),
  KEY `idx_invoice_items_source` (`source_type`,`source_id`),
  KEY `fk_invoice_items_creator` (`created_by`),
  CONSTRAINT `fk_invoice_items_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_invoice_items_invoice` FOREIGN KEY (`invoice_id`) REFERENCES `rent_invoices` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rent_invoice_items`
--

LOCK TABLES `rent_invoice_items` WRITE;
/*!40000 ALTER TABLE `rent_invoice_items` DISABLE KEYS */;
/*!40000 ALTER TABLE `rent_invoice_items` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rent_invoices`
--

LOCK TABLES `rent_invoices` WRITE;
/*!40000 ALTER TABLE `rent_invoices` DISABLE KEYS */;
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
  `allocated_amount` decimal(18,2) NOT NULL DEFAULT '0.00' COMMENT 'Amount recognized for the linked rental month',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rent_payments_finance` (`finance_record_id`),
  KEY `idx_rent_payments_invoice` (`rent_invoice_id`),
  CONSTRAINT `fk_rent_payments_finance` FOREIGN KEY (`finance_record_id`) REFERENCES `finance_records` (`id`),
  CONSTRAINT `fk_rent_payments_invoice` FOREIGN KEY (`rent_invoice_id`) REFERENCES `rent_invoices` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rent_payments`
--

LOCK TABLES `rent_payments` WRITE;
/*!40000 ALTER TABLE `rent_payments` DISABLE KEYS */;
/*!40000 ALTER TABLE `rent_payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rental_mandate_status_history`
--

DROP TABLE IF EXISTS `rental_mandate_status_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rental_mandate_status_history` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `mandate_id` bigint(20) unsigned NOT NULL,
  `from_status` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `to_status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `changed_by` bigint(20) unsigned DEFAULT NULL,
  `changed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rental_mandate_history_mandate` (`mandate_id`,`changed_at`),
  KEY `fk_rental_mandate_history_user` (`changed_by`),
  CONSTRAINT `fk_rental_mandate_history_mandate` FOREIGN KEY (`mandate_id`) REFERENCES `rental_mandates` (`id`),
  CONSTRAINT `fk_rental_mandate_history_user` FOREIGN KEY (`changed_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=120 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rental_mandate_status_history`
--

LOCK TABLES `rental_mandate_status_history` WRITE;
/*!40000 ALTER TABLE `rental_mandate_status_history` DISABLE KEYS */;
/*!40000 ALTER TABLE `rental_mandate_status_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rental_mandates`
--

DROP TABLE IF EXISTS `rental_mandates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `rental_mandates` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `owner_unit_id` bigint(20) unsigned NOT NULL,
  `mandate_no` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mandate_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'management',
  `start_date` date NOT NULL,
  `end_date` date DEFAULT NULL,
  `management_fee` decimal(18,2) DEFAULT NULL,
  `commission_percent` decimal(5,2) DEFAULT NULL,
  `responsible_user_id` bigint(20) unsigned DEFAULT NULL,
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'draft',
  `submitted_at` datetime DEFAULT NULL,
  `reviewed_by` bigint(20) unsigned DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `review_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `termination_reason` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` bigint(20) unsigned DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rental_mandates_no` (`mandate_no`),
  KEY `idx_rental_mandates_unit_status` (`owner_unit_id`,`status`),
  KEY `idx_rental_mandates_responsible` (`responsible_user_id`,`status`),
  KEY `fk_rental_mandates_reviewer` (`reviewed_by`),
  KEY `fk_rental_mandates_creator` (`created_by`),
  CONSTRAINT `fk_rental_mandates_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_rental_mandates_owner_unit` FOREIGN KEY (`owner_unit_id`) REFERENCES `owner_units` (`id`),
  CONSTRAINT `fk_rental_mandates_responsible` FOREIGN KEY (`responsible_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_rental_mandates_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rental_mandates`
--

LOCK TABLES `rental_mandates` WRITE;
/*!40000 ALTER TABLE `rental_mandates` DISABLE KEYS */;
/*!40000 ALTER TABLE `rental_mandates` ENABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=856 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reserve_accounts`
--

LOCK TABLES `reserve_accounts` WRITE;
/*!40000 ALTER TABLE `reserve_accounts` DISABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reserve_transactions`
--

LOCK TABLES `reserve_transactions` WRITE;
/*!40000 ALTER TABLE `reserve_transactions` DISABLE KEYS */;
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
INSERT INTO `roles` VALUES (1,'ADMIN','System Administrator'),(2,'OWNER','Property Owner');
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
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenants`
--

LOCK TABLES `tenants` WRITE;
/*!40000 ALTER TABLE `tenants` DISABLE KEYS */;
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
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `units`
--

LOCK TABLES `units` WRITE;
/*!40000 ALTER TABLE `units` DISABLE KEYS */;
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
INSERT INTO `user_roles` VALUES (1,1);
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
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','admin@example.com','$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW','System Administrator',NULL,'ADMIN','active',NULL,'2026-07-15 14:01:33','2026-08-02 09:26:26');
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
/*!50001 VIEW `v_rent_collection_status` AS select `ri`.`id` AS `rent_invoice_id`,`l`.`id` AS `lease_id`,`l`.`unit_id` AS `unit_id`,`l`.`tenant_id` AS `tenant_id`,`ri`.`billing_month` AS `billing_month`,`ri`.`due_date` AS `due_date`,`ri`.`amount_due` AS `amount_due`,`ri`.`amount_paid` AS `amount_paid`,greatest((`ri`.`amount_due` - `ri`.`amount_paid`),0) AS `unpaid_amount`,(case when (`ri`.`amount_paid` >= `ri`.`amount_due`) then 'paid' when (`ri`.`amount_paid` > 0) then 'partial' when (curdate() > greatest(`ri`.`due_date`,(date_add(`l`.`start_date`,interval 7 day)))) then 'overdue' else 'unpaid' end) AS `calculated_status` from (`rent_invoices` `ri` join `leases` `l` on((`l`.`id` = `ri`.`lease_id`))) where exists(select 1 from (`owner_units` `ou` join `owner_unit_services` `ous` on((`ous`.`owner_unit_id` = `ou`.`id`))) where ((`ou`.`unit_id` = `l`.`unit_id`) and (`ou`.`status` = 'active') and (`ou`.`asset_stage` = 'OPERATING') and (`ous`.`service_type` = 'RENTAL') and (`ous`.`status` = 'active'))) */;
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
CREATE TABLE IF NOT EXISTS `tenant_whatsapp_subscriptions` (
  `tenant_id` bigint(20) unsigned NOT NULL,
  `destination` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '0',
  `opted_in_at` datetime DEFAULT NULL,
  `opted_out_at` datetime DEFAULT NULL,
  `opt_in_source` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`tenant_id`),
  KEY `idx_tenant_whatsapp_enabled` (`enabled`,`updated_at`),
  CONSTRAINT `fk_tenant_whatsapp_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `tenants` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `whatsapp_delivery_attempts` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `delivery_id` bigint(20) unsigned NOT NULL,
  `attempt_number` smallint(5) unsigned NOT NULL,
  `provider_message_id` varchar(191) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider_wa_id` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `template_name` varchar(160) COLLATE utf8mb4_unicode_ci NOT NULL,
  `template_language` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'sending',
  `status_at` datetime DEFAULT NULL,
  `delivered_at` datetime DEFAULT NULL,
  `read_at` datetime DEFAULT NULL,
  `meta_error_code` int(11) DEFAULT NULL,
  `meta_error_subcode` int(11) DEFAULT NULL,
  `meta_error_details` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `fbtrace_id` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_whatsapp_delivery_attempt` (`delivery_id`,`attempt_number`),
  UNIQUE KEY `uk_whatsapp_provider_message` (`provider_message_id`),
  KEY `idx_whatsapp_attempt_status` (`status`,`status_at`),
  CONSTRAINT `fk_whatsapp_attempt_delivery` FOREIGN KEY (`delivery_id`) REFERENCES `notification_deliveries` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-08-02  9:27:18
