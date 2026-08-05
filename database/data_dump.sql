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
) ENGINE=InnoDB AUTO_INCREMENT=281 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `audit_logs`
--

LOCK TABLES `audit_logs` WRITE;
/*!40000 ALTER TABLE `audit_logs` DISABLE KEYS */;
INSERT INTO `audit_logs` VALUES (243,1,'create','rental_mandate',21,NULL,'{\"status\": \"draft\"}',NULL,'2026-08-02 09:35:24'),(244,1,'start_electronic_signature','rental_mandate',21,NULL,'{\"signerName\": \"吕志杰\", \"signatureRequestId\": 26}',NULL,'2026-08-02 09:35:34'),(245,NULL,'complete_electronic_signature','rental_mandate',21,NULL,'{\"signerName\": \"吕志杰\", \"signatureRequestId\": 26}',NULL,'2026-08-02 09:36:23'),(246,1,'status_change','rental_mandate',21,'{\"status\": \"draft\"}','{\"status\": \"pending_review\"}',NULL,'2026-08-02 09:44:01'),(247,1,'status_change','rental_mandate',21,'{\"status\": \"pending_review\"}','{\"status\": \"active\"}',NULL,'2026-08-02 09:44:06'),(248,1,'upload_lease_contract','lease',13,'{\"contractDocumentId\": null}','{\"fileName\": \"tenancy-agreement.pdf\", \"contractDocumentId\": 74}',NULL,'2026-08-02 09:45:20'),(249,1,'start_electronic_signature','lease',13,NULL,'{\"signerName\": \"吕志杰\", \"signatureRequestId\": 27}',NULL,'2026-08-02 09:45:21'),(250,NULL,'complete_electronic_signature','lease',13,NULL,'{\"signerName\": \"吕志杰\", \"signatureRequestId\": 27}',NULL,'2026-08-02 09:46:31'),(251,1,'complete_handover','property_handover',5,NULL,'{\"status\": \"completed\", \"mandateId\": 21}',NULL,'2026-08-02 09:47:49'),(252,1,'confirm_property_expense','finance_record',56,'{\"confirmationStatus\": \"pending\"}','{\"note\": \"費用資料核對正確\", \"confirmationStatus\": \"confirmed\"}',NULL,'2026-08-02 09:56:26'),(253,1,'confirm_property_expense','finance_record',55,'{\"confirmationStatus\": \"pending\"}','{\"note\": \"費用資料核對正確\", \"confirmationStatus\": \"confirmed\"}',NULL,'2026-08-02 09:56:29'),(254,1,'confirm_property_expense','finance_record',54,'{\"confirmationStatus\": \"pending\"}','{\"note\": \"費用資料核對正確\", \"confirmationStatus\": \"confirmed\"}',NULL,'2026-08-02 09:56:32'),(255,1,'update_reserve_settings','reserve_account',17,'{\"minimumBalance\": 0.00, \"lowBalanceAlertEnabled\": 1}','{\"minimumBalance\": 1000.00, \"lowBalanceAlertEnabled\": 1}',NULL,'2026-08-02 11:53:18'),(256,1,'confirm_reserve_topup','finance_record',57,'{\"confirmationStatus\": \"pending\"}','{\"note\": \"銀行入賬與充值憑證核對一致\", \"confirmationStatus\": \"confirmed\"}',NULL,'2026-08-02 11:54:09'),(257,1,'create','rental_mandate',22,NULL,'{\"status\": \"draft\"}',NULL,'2026-08-04 16:32:51'),(258,1,'status_change','rental_mandate',22,'{\"status\": \"draft\"}','{\"status\": \"pending_review\"}',NULL,'2026-08-04 16:32:53'),(259,1,'upload_lease_contract','lease',14,'{\"contractDocumentId\": null}','{\"fileName\": \"tenancy-agreement.pdf\", \"contractDocumentId\": 77}',NULL,'2026-08-04 16:34:22'),(260,1,'start_electronic_signature','rental_mandate',22,NULL,'{\"signerName\": \"吕志杰\", \"signatureRequestId\": 28}',NULL,'2026-08-04 16:36:26'),(261,NULL,'complete_electronic_signature','rental_mandate',22,NULL,'{\"signerName\": \"吕志杰\", \"signatureRequestId\": 28}',NULL,'2026-08-04 16:37:41'),(262,1,'status_change','rental_mandate',22,'{\"status\": \"pending_review\"}','{\"status\": \"active\"}',NULL,'2026-08-04 16:38:05'),(263,1,'replace_lease_contract','lease',14,'{\"contractDocumentId\": 77}','{\"fileName\": \"tenancy-agreement.pdf\", \"contractDocumentId\": 82}',NULL,'2026-08-04 16:40:10'),(264,1,'start_electronic_signature','lease',14,NULL,'{\"signerName\": \"吕志杰\", \"signatureRequestId\": 29}',NULL,'2026-08-04 16:40:11'),(265,NULL,'complete_electronic_signature','lease',14,NULL,'{\"signerName\": \"吕志杰\", \"signatureRequestId\": 29}',NULL,'2026-08-04 16:41:15'),(266,1,'upload_lease_contract','lease',15,'{\"contractDocumentId\": null}','{\"fileName\": \"tenancy-agreement.pdf\", \"contractDocumentId\": 84}',NULL,'2026-08-04 16:42:21'),(267,1,'start_electronic_signature','lease',15,NULL,'{\"signerName\": \"吕志杰\", \"signatureRequestId\": 30}',NULL,'2026-08-04 16:42:22'),(268,1,'replace_lease_contract','lease',15,'{\"contractDocumentId\": 84}','{\"fileName\": \"tenancy-agreement.pdf\", \"contractDocumentId\": 85}',NULL,'2026-08-04 16:43:20'),(269,1,'start_electronic_signature','lease',15,NULL,'{\"signerName\": \"吕志杰\", \"signatureRequestId\": 31}',NULL,'2026-08-04 16:43:21'),(270,1,'create_expense','cashflow_entry',48,NULL,'{\"transactionNo\": \"EXP-20260804164514-804EAC\"}',NULL,'2026-08-04 16:45:14'),(271,1,'create_maintenance','maintenance_work_order',9,NULL,'{\"workOrderNo\": \"MWO-20260804164526-C7A2FF\"}',NULL,'2026-08-04 16:45:26'),(272,1,'complete_maintenance','maintenance_work_order',9,NULL,'{\"amount\": 1, \"settlementMethod\": \"direct_payment\"}',NULL,'2026-08-04 16:45:53'),(273,1,'confirm_rent_collection','finance_record',63,'{\"invoiceId\": 21, \"amountPaid\": 0.00}','{\"note\": \"管理員確認租金收款；預收租金 RM 3.23\", \"invoiceId\": 21, \"amountPaid\": 96.77, \"receivedAmount\": 96.77}',NULL,'2026-08-04 16:46:45'),(274,1,'confirm_property_payment','finance_record',64,'{\"confirmationStatus\": \"pending\"}','{\"note\": \"银行入账与付款凭证核对一致\", \"confirmationStatus\": \"confirmed\"}',NULL,'2026-08-04 17:02:03'),(275,1,'create','rental_mandate',23,NULL,'{\"status\": \"draft\"}',NULL,'2026-08-04 17:33:32'),(276,1,'status_change','rental_mandate',23,'{\"status\": \"draft\"}','{\"status\": \"pending_review\"}',NULL,'2026-08-04 17:33:34'),(277,1,'start_electronic_signature','rental_mandate',23,NULL,'{\"signerName\": \"吕志杰\", \"signatureRequestId\": 32}',NULL,'2026-08-04 17:34:06'),(278,1,'replace_lease_contract','lease',15,'{\"contractDocumentId\": 85}','{\"fileName\": \"tenancy-agreement.pdf\", \"contractDocumentId\": 90}',NULL,'2026-08-04 18:45:42'),(279,1,'start_electronic_signature','lease',15,NULL,'{\"signerName\": \"吕志杰\", \"signatureRequestId\": 33}',NULL,'2026-08-04 18:45:43'),(280,NULL,'complete_electronic_signature','lease',15,NULL,'{\"signerName\": \"吕志杰\", \"signatureRequestId\": 33}',NULL,'2026-08-04 18:46:17');
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
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cashflow_entries`
--

LOCK TABLES `cashflow_entries` WRITE;
/*!40000 ALTER TABLE `cashflow_entries` DISABLE KEYS */;
INSERT INTO `cashflow_entries` VALUES (40,52,23,NULL,8,NULL,NULL,'expense','management','大樓管理費（帳期 2026-08）','2026-08-05',NULL,'not_required','2026-08-02 09:54:30','2026-08-05 08:56:17'),(41,53,23,NULL,8,NULL,NULL,'expense','service_fee','租客招募（帳期 ONCE）','2026-08-05',NULL,'not_required','2026-08-02 09:54:30','2026-08-05 08:56:17'),(42,54,23,NULL,8,NULL,NULL,'expense','insurance','火險（帳期 2026）','2026-08-02',NULL,'not_required','2026-08-02 09:54:30','2026-08-02 09:54:30'),(43,55,23,NULL,8,NULL,NULL,'expense','tax','地稅（帳期 2026）','2026-08-02',NULL,'not_required','2026-08-02 09:54:30','2026-08-02 09:54:30'),(44,56,23,NULL,8,NULL,NULL,'expense','service_fee','租客招募2（帳期 ONCE）','2026-08-02',NULL,'not_required','2026-08-02 09:55:34','2026-08-02 09:55:34'),(45,58,23,NULL,8,NULL,NULL,'expense','insurance','火險（帳期 2026-08）','2026-08-05',NULL,'not_required','2026-08-02 17:20:54','2026-08-05 08:56:17'),(46,59,23,NULL,8,NULL,NULL,'expense','tax','地稅（帳期 2026-08）','2026-08-05',NULL,'not_required','2026-08-02 17:20:54','2026-08-05 08:56:17'),(47,60,24,NULL,8,NULL,NULL,'expense','management','大樓管理費（帳期 2026-08）','2026-08-05',NULL,'not_required','2026-08-04 16:42:14','2026-08-05 08:56:17'),(48,61,23,NULL,8,NULL,NULL,'expense','utilities','1','2026-08-04',17,'not_required','2026-08-04 16:45:14','2026-08-04 16:45:14'),(49,62,24,NULL,8,NULL,NULL,'expense','maintenance','1','2026-08-04',NULL,'complete','2026-08-04 16:45:53','2026-08-04 16:45:53'),(50,63,23,NULL,8,8,NULL,'income','rent','租金收款及預收 · LEASE-20260802-EA043D536A20','2026-08-04',NULL,'missing','2026-08-04 16:46:45','2026-08-04 16:46:45');
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
) ENGINE=InnoDB AUTO_INCREMENT=78 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document_links`
--

LOCK TABLES `document_links` WRITE;
/*!40000 ALTER TABLE `document_links` DISABLE KEYS */;
INSERT INTO `document_links` VALUES (60,72,'rental_mandate',21,'authorization_draft','2026-08-02 09:35:28'),(61,73,'rental_mandate',21,'signed_contract','2026-08-02 09:36:23'),(62,74,'lease',13,'contract','2026-08-02 09:45:20'),(63,75,'lease',13,'signed_contract','2026-08-02 09:46:31'),(64,76,'finance',57,'reserve_topup_proof','2026-08-02 11:53:55'),(65,77,'lease',14,'contract','2026-08-04 16:34:22'),(66,80,'rental_mandate',22,'authorization_draft','2026-08-04 16:36:08'),(67,81,'rental_mandate',22,'signed_contract','2026-08-04 16:37:41'),(68,82,'lease',14,'contract','2026-08-04 16:40:10'),(69,83,'lease',14,'signed_contract','2026-08-04 16:41:15'),(70,84,'lease',15,'contract','2026-08-04 16:42:21'),(71,85,'lease',15,'contract','2026-08-04 16:43:20'),(72,86,'work_order',9,'before_photo','2026-08-04 16:45:53'),(73,87,'work_order',9,'after_photo','2026-08-04 16:45:53'),(74,88,'finance',64,'payment_proof','2026-08-04 17:01:47'),(76,90,'lease',15,'contract','2026-08-04 18:45:42'),(77,91,'lease',15,'signed_contract','2026-08-04 18:46:17');
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
) ENGINE=InnoDB AUTO_INCREMENT=92 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `documents`
--

LOCK TABLES `documents` WRITE;
/*!40000 ALTER TABLE `documents` DISABLE KEYS */;
INSERT INTO `documents` VALUES (70,'PHOTO-20260802093456-357E3659','kitten_avatar_05.jpg','22/ee93761f25934b9fb8d0c554b8002eb8.jpg','image/jpeg',85671,NULL,'property_photo','approved',NULL,1,NULL,NULL,NULL,'2026-08-02 09:34:56','2026-08-02 09:34:56'),(71,'PHOTO-20260802093457-36AC18A0','kitten_avatar_05.jpg','22/74d89415632e40d5b1588fa000d5486a.jpg','image/jpeg',85671,NULL,'property_photo','approved',NULL,1,NULL,NULL,NULL,'2026-08-02 09:34:57','2026-08-02 09:34:57'),(72,'RM-DOC-BF067AD741','authorization.pdf','21/bf067ad741454df3bc9035cb27cc597a.pdf','application/pdf',312612,'54434e757f8e026040737e60324a56abf1e2dc339ec2bb5bd3352692ae7eb5ae','authorization_draft','pending_review',NULL,1,NULL,NULL,NULL,'2026-08-02 09:35:28','2026-08-02 09:35:28'),(73,'SIGNED-26-20260802093623','authorization-已簽署.pdf','26/signed-contract.pdf','application/pdf',319164,'5644b535baeb9bbbf1b8cdd5cb17e8312cc960ddc513cfd0d4a9c19e3294be62','signed_contract','approved',NULL,NULL,NULL,'2026-08-02 09:36:23',NULL,'2026-08-02 09:36:23','2026-08-02 09:36:23'),(74,'LEASE-DOC-20260802094520-537D4F7D','tenancy-agreement.pdf','13/537d4f7dc4354b10b2b43629c8bc3094.pdf','application/pdf',1305148,'6b91b2249ab678882d958c89ff3c7e62b727043691475e5f08f443fcd3a29dca','lease','approved',NULL,1,1,'2026-08-02 09:45:20',NULL,'2026-08-02 09:45:20','2026-08-02 09:45:20'),(75,'SIGNED-27-20260802094631','tenancy-agreement-已簽署.pdf','27/signed-contract.pdf','application/pdf',1308576,'9dce1b3c3c21c186e596a3fce9333eaf55e92a0da88c5dabe31afe5a198f2a22','signed_contract','approved',NULL,NULL,NULL,'2026-08-02 09:46:31',NULL,'2026-08-02 09:46:31','2026-08-02 09:46:31'),(76,'RTU-DOC-AA458AB59114-0','kitten_avatar_09.jpg','17/aa458ab591144703b8b0fbcdaf02e055-0.jpg','image/jpeg',35616,'f94c99249b75e57825494b6541e3f3f57daf79786c383173cb40812b730bf57b','reserve_topup_proof','active',NULL,10,NULL,NULL,NULL,'2026-08-02 11:53:55','2026-08-02 11:54:09'),(77,'LEASE-DOC-20260804163422-1E0252D7','tenancy-agreement.pdf','14/1e0252d752da4865961d265192103a6b.pdf','application/pdf',1537506,'d71e92eb8dccb3ff5619f735a0f46a0531336ee3566e8e38a25b7a301d560a1e','lease','superseded',NULL,1,1,'2026-08-04 16:34:22',NULL,'2026-08-04 16:34:22','2026-08-04 16:40:10'),(78,'PHOTO-20260804163533-5103F259','kitten_avatar_09.jpg','23/6eafba5f4d594982a84e25e6a6f19470.jpg','image/jpeg',35616,NULL,'property_photo','approved',NULL,1,NULL,NULL,NULL,'2026-08-04 16:35:33','2026-08-04 16:35:33'),(79,'PHOTO-20260804163535-55CE8438','kitten_avatar_09.jpg','23/7d7ff37e90e84ffdb20de092597730bb.jpg','image/jpeg',35616,NULL,'property_photo','approved',NULL,1,NULL,NULL,NULL,'2026-08-04 16:35:35','2026-08-04 16:35:35'),(80,'RM-DOC-A7C9E35864','authorization.pdf','22/a7c9e35864b64490b12d2deb4d2cab00.pdf','application/pdf',312607,'fa8a264a89c197bf76a054537d92062266356716101d16f941a979d5c76b06a2','authorization_draft','pending_review',NULL,1,NULL,NULL,NULL,'2026-08-04 16:36:08','2026-08-04 16:36:08'),(81,'SIGNED-28-20260804163741','authorization-已簽署.pdf','28/signed-contract.pdf','application/pdf',317381,'8ac2a4c6187b6681291563b09b585756e8ae4049a2efc340d311dc05f7212e7a','signed_contract','approved',NULL,NULL,NULL,'2026-08-04 16:37:41',NULL,'2026-08-04 16:37:41','2026-08-04 16:37:41'),(82,'LEASE-DOC-20260804164010-73BE4487','tenancy-agreement.pdf','14/73be44875ef047ca909ec6a0a9820b0e.pdf','application/pdf',1305088,'f467addf5b16295e01c0b674fc9e40287e0abf4619f58ce514c914da0e37086a','lease','approved',NULL,1,1,'2026-08-04 16:40:10',NULL,'2026-08-04 16:40:10','2026-08-04 16:40:10'),(83,'SIGNED-29-20260804164115','tenancy-agreement-已簽署.pdf','29/signed-contract.pdf','application/pdf',1310555,'99a5d15e9355d728520f4ef61e8f3091c653fa6088ea2596e43ea19a4fa25f5d','signed_contract','approved',NULL,NULL,NULL,'2026-08-04 16:41:15',NULL,'2026-08-04 16:41:15','2026-08-04 16:41:15'),(84,'LEASE-DOC-20260804164221-C313277F','tenancy-agreement.pdf','15/c313277fa3144dcb9e2811d3a23dff06.pdf','application/pdf',1305004,'ce7e9d914531f07672381e9ba23a3ed5d32671383cfe6ac2898bfd1ecc856d00','lease','superseded',NULL,1,1,'2026-08-04 16:42:21',NULL,'2026-08-04 16:42:21','2026-08-04 16:43:20'),(85,'LEASE-DOC-20260804164320-549FB6C3','tenancy-agreement.pdf','15/549fb6c3a59c41f6828f0f11f3def327.pdf','application/pdf',1305004,'3d9cfb79365baed3f5799ac0e6c8b3ce0c85c55bc12272a9e2ee24737880c867','lease','superseded',NULL,1,1,'2026-08-04 16:43:20',NULL,'2026-08-04 16:43:20','2026-08-04 18:45:42'),(86,'MNT-20260804164553-FAB9033B','kitten_avatar_09.jpg','9/fab9033bb4014262bd93b216e3456855-0.jpg','image/jpeg',35616,'f94c99249b75e57825494b6541e3f3f57daf79786c383173cb40812b730bf57b','maintenance_attachment','active',NULL,1,NULL,NULL,NULL,'2026-08-04 16:45:53','2026-08-04 16:45:53'),(87,'MNT-20260804164553-5B3A7753','kitten_avatar_09.jpg','9/5b3a775359d74a16bed0cd1e8564c11b-0.jpg','image/jpeg',35616,'f94c99249b75e57825494b6541e3f3f57daf79786c383173cb40812b730bf57b','maintenance_attachment','active',NULL,1,NULL,NULL,NULL,'2026-08-04 16:45:53','2026-08-04 16:45:53'),(88,'DOC-7DB8F02077F3-0','kitten_avatar_08.jpg','24/7db8f02077f34a05a5e9874f81004964-0.jpg','image/jpeg',40894,'384af0fdcde60e7760b45307657e343de6c2452db21f2416dd1a92e4ddb24c9a','payment_proof','approved',NULL,10,1,'2026-08-04 17:02:03','银行入账与付款凭证核对一致','2026-08-04 17:01:47','2026-08-04 17:02:03'),(90,'LEASE-DOC-20260804184542-7B5BE003','tenancy-agreement.pdf','15/7b5be0032fde4022a921f4bdbc1b05b1.pdf','application/pdf',1305004,'59f9dd1bc446a51da2b033c21b5d713bdf93614510c37b84ffe2e25a649bd5ae','lease','approved',NULL,1,1,'2026-08-04 18:45:42',NULL,'2026-08-04 18:45:42','2026-08-04 18:45:42'),(91,'SIGNED-33-20260804184617','tenancy-agreement-已簽署.pdf','33/signed-contract.pdf','application/pdf',1308523,'ad1dec7ef9c48e590914f69c881b6aae4e5a476c4bad4375bf7ac6d5dbd30d35','signed_contract','approved',NULL,NULL,NULL,'2026-08-04 18:46:17',NULL,'2026-08-04 18:46:17','2026-08-04 18:46:17');
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
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `electronic_signature_events`
--

LOCK TABLES `electronic_signature_events` WRITE;
/*!40000 ALTER TABLE `electronic_signature_events` DISABLE KEYS */;
INSERT INTO `electronic_signature_events` VALUES (38,26,'requested','Signing link created',NULL,NULL,'2026-08-02 09:35:34'),(39,26,'verification_resent','Verification code re-sent','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0','2026-08-02 09:35:57'),(40,26,'signed','Contract signed','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0','2026-08-02 09:36:23'),(41,27,'requested','Signing link created',NULL,NULL,'2026-08-02 09:45:21'),(42,27,'signed','Contract signed','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0','2026-08-02 09:46:31'),(43,28,'requested','Signing link created',NULL,NULL,'2026-08-04 16:36:26'),(44,28,'signed','Contract signed','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36','2026-08-04 16:37:41'),(45,29,'requested','Signing link created',NULL,NULL,'2026-08-04 16:40:11'),(46,29,'signed','Contract signed','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36','2026-08-04 16:41:15'),(47,30,'requested','Signing link created',NULL,NULL,'2026-08-04 16:42:22'),(48,31,'requested','Signing link created',NULL,NULL,'2026-08-04 16:43:21'),(50,33,'requested','Signing link created',NULL,NULL,'2026-08-04 18:45:43'),(51,33,'signed','Contract signed','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36','2026-08-04 18:46:17');
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
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `electronic_signature_requests`
--

LOCK TABLES `electronic_signature_requests` WRITE;
/*!40000 ALTER TABLE `electronic_signature_requests` DISABLE KEYS */;
INSERT INTO `electronic_signature_requests` VALUES (26,72,'rental_mandate',21,'吕志杰','1270673745@qq.com','6807872f85662a37852bcbe0aae0c6654a9ef9407468c72f4ca13047c67b8205','$2a$10$EDFnCADQO3KeCFjICeXJGO386jH4nFhtwaRnMU35Z1IobVeo2El2W','2026-08-02 09:45:57','signed','2026-08-09 09:35:33',1,'2026-08-02 09:35:33','2026-08-02 09:36:24',73,'54434e757f8e026040737e60324a56abf1e2dc339ec2bb5bd3352692ae7eb5ae','9d70c22e6fc8cbf2443690097f1f1a6039b508716c5631792945665e2b77c6d4','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0','2026-08-02 09:36:23'),(27,74,'lease',13,'吕志杰','1270673745@qq.com','20a0bfae3a58799ac39ef31ae4a8237b011682672f70af818ff3a676210438bb','$2a$10$C27Z4jSCyhDOlS/oHYym.OREOdsCOUI5hozAvysWQJbOA0cZ6lk8e','2026-08-02 09:55:21','signed','2026-08-09 09:45:21',1,'2026-08-02 09:45:20','2026-08-02 09:46:31',75,'6b91b2249ab678882d958c89ff3c7e62b727043691475e5f08f443fcd3a29dca','085a36b6d05eabbfc7667b03a086875cf2b064e44fc0e40af84e74e6ac9b6f63','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0','2026-08-02 09:46:31'),(28,80,'rental_mandate',22,'吕志杰','1270673745@qq.com','baa0b19d5fd84422232d690fdb894a8d00044141abcbbc33563f800fc4c38575','$2a$10$gdwS9puZx5SafjS7woZgqeTvGb7MzooWW.nS0.Qv1vHAPwB7ymeL2','2026-08-04 16:46:25','signed','2026-08-11 16:36:25',1,'2026-08-04 16:36:25','2026-08-04 16:37:42',81,'fa8a264a89c197bf76a054537d92062266356716101d16f941a979d5c76b06a2','6e8d8301d40cc567027fdf03a370bd2e479ef2e3aa3b8167c4c57e8f33e6ed75','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36','2026-08-04 16:37:41'),(29,82,'lease',14,'吕志杰','1270673745@qq.com','05cf08d1fe141994747c0edac539b61f74a51d1f92d774dd2655e4774359d585','$2a$10$Ly4zbz4XcrBI4R8NNAXImuHe03XnpDTUjxoBZHe.AwSji5H6FoxP6','2026-08-04 16:50:11','signed','2026-08-11 16:40:11',1,'2026-08-04 16:40:10','2026-08-04 16:41:15',83,'f467addf5b16295e01c0b674fc9e40287e0abf4619f58ce514c914da0e37086a','f52bc6cfcefe88f4d92901b9b2274d9562eca589167ef4734440776656a1b207','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36','2026-08-04 16:41:15'),(30,84,'lease',15,'吕志杰','1270673745@qq.com','c754586fc6a9968c45c451bf3652519946e1be8ca3eed14dded78e8776fe5ec1','$2a$10$OxxD7hVx4/i4fsqAELtttO0VqmKxW/sBtu9oBYEciTmmu2uvb1PYe','2026-08-04 16:52:21','cancelled','2026-08-11 16:42:21',1,'2026-08-04 16:42:21',NULL,NULL,'ce7e9d914531f07672381e9ba23a3ed5d32671383cfe6ac2898bfd1ecc856d00',NULL,NULL,NULL,'2026-08-04 16:43:20'),(31,85,'lease',15,'吕志杰','1270673745@qq.com','f2201fdb83fc468ce44093e44e6c3c5d1f3c4bd67a08f42a67ffa6ad7c60bc02','$2a$10$ZhTz/lIvl4s3Jko/6B2VPONM.6NfQHutIn3ypdSWvA/TL0HAccYUm','2026-08-04 16:53:20','cancelled','2026-08-11 16:43:20',1,'2026-08-04 16:43:20',NULL,NULL,'3d9cfb79365baed3f5799ac0e6c8b3ce0c85c55bc12272a9e2ee24737880c867',NULL,NULL,NULL,'2026-08-04 18:45:42'),(33,90,'lease',15,'吕志杰','1270673745@qq.com','7e65edd9059d325830d0bc80a9ce7c5e334d52e66442a57069430350db894eed','$2a$10$mOmQJm.AbTIF8jE9et2kAu5AsuMA0WZTvLCp1ZXhZkCDgLlGRoruC','2026-08-04 18:55:42','signed','2026-08-11 18:45:42',1,'2026-08-04 18:45:42','2026-08-04 18:46:17',91,'59f9dd1bc446a51da2b033c21b5d713bdf93614510c37b84ffe2e25a649bd5ae','ad4a97fed85e058b2d43bcd06f29f88242de37f836302388b0ad4ecb27040de2','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36','2026-08-04 18:46:17');
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
) ENGINE=InnoDB AUTO_INCREMENT=65 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `finance_records`
--

LOCK TABLES `finance_records` WRITE;
/*!40000 ALTER TABLE `finance_records` DISABLE KEYS */;
INSERT INTO `finance_records` VALUES (52,'AUTO-EXP-20260802-AF595965','property_expense',23,8,NULL,20.00,'MYR','2026-08-05','internal_accrual','unpaid','pending',NULL,NULL,'not_synced',NULL,1,'2026-08-02 09:54:30','2026-08-05 08:56:17'),(53,'AUTO-EXP-20260802-3AC957B5','property_expense',23,8,NULL,100.00,'MYR','2026-08-05','internal_accrual','unpaid','pending',NULL,NULL,'not_synced',NULL,1,'2026-08-02 09:54:30','2026-08-05 08:56:17'),(54,'AUTO-EXP-20260802-4438016E','property_expense',23,8,NULL,20.00,'MYR','2026-08-02','internal_accrual','unpaid','confirmed',1,'2026-08-02 09:56:32','pending',NULL,1,'2026-08-02 09:54:30','2026-08-02 09:56:32'),(55,'AUTO-EXP-20260802-4E9A2B89','property_expense',23,8,NULL,20.00,'MYR','2026-08-02','internal_accrual','unpaid','confirmed',1,'2026-08-02 09:56:29','pending',NULL,1,'2026-08-02 09:54:30','2026-08-02 09:56:29'),(56,'AUTO-EXP-20260802-6E0D7D0B','property_expense',23,8,NULL,101.00,'MYR','2026-08-02','internal_accrual','unpaid','confirmed',1,'2026-08-02 09:56:26','pending',NULL,1,'2026-08-02 09:55:34','2026-08-02 09:56:26'),(57,'RTU-20260802115355-AA458AB5','reserve_topup',23,8,NULL,1000.00,'MYR','2026-08-02','online_payment','paid','confirmed',1,'2026-08-02 11:54:09','pending',NULL,10,'2026-08-02 11:53:55','2026-08-02 11:54:09'),(58,'AUTO-EXP-20260802-1502BE71','property_expense',23,8,NULL,20.00,'MYR','2026-08-05','internal_accrual','unpaid','pending',NULL,NULL,'not_synced',NULL,1,'2026-08-02 17:20:54','2026-08-05 08:56:17'),(59,'AUTO-EXP-20260802-4FA67417','property_expense',23,8,NULL,20.00,'MYR','2026-08-05','internal_accrual','unpaid','pending',NULL,NULL,'not_synced',NULL,1,'2026-08-02 17:20:54','2026-08-05 08:56:17'),(60,'AUTO-EXP-20260804-FF37F2F7','property_expense',24,8,NULL,1.00,'MYR','2026-08-05','internal_accrual','unpaid','pending',NULL,NULL,'not_synced',NULL,1,'2026-08-04 16:42:14','2026-08-05 08:56:17'),(61,'EXP-20260804164514-804EAC','cashflow',23,8,NULL,1.00,'MYR','2026-08-04','reserve_account','paid','confirmed',1,'2026-08-04 16:45:14','not_synced',NULL,1,'2026-08-04 16:45:14','2026-08-04 16:45:14'),(62,'EXP-MNT-20260804164553-9','cashflow',24,8,NULL,1.00,'MYR','2026-08-04','direct_payment','paid','not_required',NULL,NULL,'not_synced',NULL,1,'2026-08-04 16:45:53','2026-08-04 16:45:53'),(63,'RENT-ADM-20260804164645-EE29E808','rent_payment',23,8,8,100.00,'MYR','2026-08-04','cash','paid','confirmed',1,'2026-08-04 16:46:45','pending',NULL,1,'2026-08-04 16:46:45','2026-08-04 16:46:45'),(64,'PP-20260804170147-7DB8F020','property_payment',25,8,NULL,3333.34,'MYR','2026-08-04','bank_transfer','paid','confirmed',1,'2026-08-04 17:02:03','pending',NULL,10,'2026-08-04 17:01:47','2026-08-04 17:02:03');
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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lease_rent_credit_allocations`
--

LOCK TABLES `lease_rent_credit_allocations` WRITE;
/*!40000 ALTER TABLE `lease_rent_credit_allocations` DISABLE KEYS */;
INSERT INTO `lease_rent_credit_allocations` VALUES (1,2,24,3.23,'automatic',1,'2026-08-04 16:46:45');
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lease_rent_credits`
--

LOCK TABLES `lease_rent_credits` WRITE;
/*!40000 ALTER TABLE `lease_rent_credits` DISABLE KEYS */;
INSERT INTO `lease_rent_credits` VALUES (2,13,63,3.23,3.23,0.00,'allocated',1,'2026-08-04 16:46:45','2026-08-04 16:46:45');
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
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `leases`
--

LOCK TABLES `leases` WRITE;
/*!40000 ALTER TABLE `leases` DISABLE KEYS */;
INSERT INTO `leases` VALUES (13,23,8,21,'LEASE-20260802-EA043D536A20','2026-08-02','2026-09-02',100.00,1.00,1,'daily_prorated','active',74,'2026-08-02 09:45:07','2026-08-02 09:45:20'),(14,23,8,21,'LEASE-20260804-186C4BF4581B4','2026-09-03','2026-12-01',100.00,1.00,1,'daily_prorated','active',82,'2026-08-04 09:37:36','2026-08-04 16:40:10'),(15,24,8,22,'LEASE-20260804-19DC810EF8F90','2026-08-04','2026-08-09',11000.00,1000.00,1,'daily_prorated','active',90,'2026-08-04 16:39:19','2026-08-04 18:45:42');
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
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `maintenance_status_history`
--

LOCK TABLES `maintenance_status_history` WRITE;
/*!40000 ALTER TABLE `maintenance_status_history` DISABLE KEYS */;
INSERT INTO `maintenance_status_history` VALUES (13,9,'submitted','2026-08-04 16:45:00','管理員建立維修工單',1,'2026-08-04 16:45:26'),(14,9,'completed','2026-08-04 16:45:53','1',1,'2026-08-04 16:45:53');
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
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `maintenance_work_orders`
--

LOCK TABLES `maintenance_work_orders` WRITE;
/*!40000 ALTER TABLE `maintenance_work_orders` DISABLE KEYS */;
INSERT INTO `maintenance_work_orders` VALUES (9,'MWO-20260804164526-C7A2FF',24,NULL,8,NULL,NULL,49,'plumbing','1','','2026-08-04 16:45:00','2026-08-04 16:45:53','completed',NULL,1.00,1,'2026-08-04 16:45:26','2026-08-04 16:45:53');
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
) ENGINE=InnoDB AUTO_INCREMENT=31 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
INSERT INTO `notifications` VALUES (29,NULL,10,8,'預備金充值已確認','测试建案1号 101 預備金已充值 RM 1000.00。','finance_record',57,'normal','read','2026-08-04 17:48:12','2026-08-02 11:54:09'),(30,NULL,10,8,'房款已確認','测试建案1号 1 第 1 期已確認收款 RM 3333.34。','finance_record',64,'normal','read','2026-08-04 17:48:12','2026-08-04 17:02:03');
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
INSERT INTO `owner_unit_services` VALUES (22,'RENTAL','active','2026-08-02',NULL,'2026-08-02 09:34:09','2026-08-04 16:35:10'),(22,'RESALE','active','2026-08-02',NULL,'2026-08-02 09:34:09','2026-08-04 16:35:10'),(22,'MANAGEMENT','active','2026-08-02',NULL,'2026-08-02 09:34:09','2026-08-04 16:35:10'),(23,'RENTAL','active','2026-08-04',NULL,'2026-08-04 16:30:25','2026-08-04 16:42:14'),(23,'RESALE','active','2026-08-04',NULL,'2026-08-04 16:30:25','2026-08-04 16:42:14');
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
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `owner_units`
--

LOCK TABLES `owner_units` WRITE;
/*!40000 ALTER TABLE `owner_units` DISABLE KEYS */;
INSERT INTO `owner_units` VALUES (22,8,23,100.00,1,'2026-08-02',NULL,'OPERATING',NULL,'2026-08-02','active','2026-08-02 09:34:09'),(23,8,24,100.00,1,'2026-08-04',NULL,'OPERATING',NULL,'2026-08-04','active','2026-08-04 16:30:25'),(24,8,25,100.00,1,'2026-08-04',NULL,'OPERATING','2026-08-04','2026-08-04','active','2026-08-04 16:58:56');
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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `owners`
--

LOCK TABLES `owners` WRITE;
/*!40000 ALTER TABLE `owners` DISABLE KEYS */;
INSERT INTO `owners` VALUES (8,10,'001','吕志杰','523722200209235815','18981712596','18981712596','18981712596','18981712596',NULL,'1270673745@qq.com','active','2026-08-02 09:31:39','2026-08-02 09:31:39');
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
) ENGINE=InnoDB AUTO_INCREMENT=60 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_installments`
--

LOCK TABLES `payment_installments` WRITE;
/*!40000 ALTER TABLE `payment_installments` DISABLE KEYS */;
INSERT INTO `payment_installments` VALUES (57,11,1,NULL,'2026-09-04',3333.34,3333.34,'paid','2026-08-04 16:59:42','2026-08-04 17:02:03'),(58,11,2,NULL,'2026-10-04',3333.33,3333.33,'paid','2026-08-04 16:59:42','2026-08-04 17:32:45'),(59,11,3,NULL,'2026-11-04',3333.33,3333.33,'paid','2026-08-04 16:59:42','2026-08-04 17:32:45');
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
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_plans`
--

LOCK TABLES `payment_plans` WRITE;
/*!40000 ALTER TABLE `payment_plans` DISABLE KEYS */;
INSERT INTO `payment_plans` VALUES (11,22,'付款计划',3,10000.00,'2026-08-04','historical','2026-08-04 16:59:42');
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_receipt_allocations`
--

LOCK TABLES `payment_receipt_allocations` WRITE;
/*!40000 ALTER TABLE `payment_receipt_allocations` DISABLE KEYS */;
INSERT INTO `payment_receipt_allocations` VALUES (5,16,57,3333.34);
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
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `payment_receipts`
--

LOCK TABLES `payment_receipts` WRITE;
/*!40000 ALTER TABLE `payment_receipts` DISABLE KEYS */;
INSERT INTO `payment_receipts` VALUES (14,57,'RTU-RCP-20260802115355-91144703','吕志杰','1000 | 1100101',76,NULL,'銀行入賬與充值憑證核對一致','2026-08-02 11:53:55'),(15,63,'RENT-RCP-20260804164645-EE29E808','lzj',NULL,NULL,'管理員確認租金收款','管理員確認租金收款','2026-08-04 16:46:45'),(16,64,'RCP-20260804170147-77F34A05','吕志杰','马来亚银行有限公司 | 1',88,NULL,'银行入账与付款凭证核对一致','2026-08-04 17:01:47');
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
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `projects`
--

LOCK TABLES `projects` WRITE;
/*!40000 ALTER TABLE `projects` DISABLE KEYS */;
INSERT INTO `projects` VALUES (14,'TEST1','测试建案1号','测试建案1号','吉隆坡','MY','active','2026-08-02 09:30:29','2026-08-02 09:30:29');
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
INSERT INTO `property_basic_profiles` VALUES (22,'{\"notes\": \"\", \"dealNo\": \"\", \"address\": \"测试建案1号\", \"areaUnit\": \"sqm\", \"bankCode\": \"\", \"bankName\": \"\", \"currency\": \"MYR\", \"landTaxFee\": 20, \"countryCode\": \"MY\", \"landTaxMonth\": 8, \"serviceFlags\": {\"rental\": true, \"resale\": true, \"handover\": false, \"management\": true, \"renovation\": false}, \"bankAccountNo\": \"\", \"bankBranchCode\": \"\", \"bankAccountName\": \"\", \"remittanceCycle\": \"monthly\", \"salesServiceFee\": 0, \"assessmentTaxFee\": 0, \"fireInsuranceFee\": 20, \"operatingReserve\": 0, \"generalServiceFee\": 0, \"rentalServiceFees\": [{\"id\": \"65e33ced-b02d-4ae0-9d53-16211c8c34b5\", \"name\": \"租客招募\", \"amount\": 100}, {\"id\": \"3f62deae-3973-4c5a-8e67-ee8060ffc3ea\", \"name\": \"租客招募2\", \"amount\": 101}], \"administrativeArea\": \"吉隆坡\", \"assessmentTaxMonth\": 8, \"fireInsuranceMonth\": 8, \"managementFeeAmount\": 0, \"managementFeeMonths\": [], \"managementFeePercent\": 0, \"rentalServiceEnabled\": true, \"buildingManagementFee\": 20}','2026-08-02 09:34:27','2026-08-04 16:35:10'),(23,'{\"landTaxFee\": 0, \"landTaxMonth\": 8, \"salesServiceFee\": 0, \"assessmentTaxFee\": 0, \"fireInsuranceFee\": 0, \"generalServiceFee\": 0, \"rentalServiceFees\": [], \"assessmentTaxMonth\": 8, \"fireInsuranceMonth\": 8, \"landTaxBillingMode\": \"months\", \"managementFeeAmount\": 0, \"managementFeeMonths\": [10, 11, 12], \"landTaxBillingMonths\": [8], \"buildingManagementFee\": 1, \"salesServiceBillingMode\": \"once\", \"assessmentTaxBillingMode\": \"months\", \"fireInsuranceBillingMode\": \"months\", \"managementFeeBillingMode\": \"months\", \"generalServiceBillingMode\": \"once\", \"salesServiceBillingMonths\": [8], \"assessmentTaxBillingMonths\": [8], \"fireInsuranceBillingMonths\": [8], \"managementFeeBillingMonths\": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12], \"generalServiceBillingMonths\": [8], \"buildingManagementBillingMode\": \"months\", \"buildingManagementBillingMonths\": [10, 11, 12]}','2026-08-04 16:35:35','2026-08-04 16:42:14'),(24,'{}','2026-08-04 17:32:45','2026-08-04 17:32:45');
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
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_contract_records`
--

LOCK TABLES `property_contract_records` WRITE;
/*!40000 ALTER TABLE `property_contract_records` DISABLE KEYS */;
INSERT INTO `property_contract_records` VALUES (12,22,13,'O_LEASE_RESERVATION','OTR-13','2026-08-02','2026-08-02','2026-09-02','completed','Generated from the rental workbench','otr.pdf','22/20fe5e64617e41da8f307d85fb8bf022.pdf','application/pdf',345113,1,'2026-08-02 09:45:12','2026-08-02 09:45:12'),(13,23,15,'O_LEASE_RESERVATION','OTR-15','2026-08-04','2026-08-04','2026-08-09','completed','Generated from the rental workbench','otr.pdf','23/a0df6cfb9a3a4bdcafd95d6ea7f8450b.pdf','application/pdf',345111,1,'2026-08-04 16:39:32','2026-08-04 16:39:32'),(14,22,14,'O_LEASE_RESERVATION','OTR-14','2026-08-04','2026-09-03','2026-12-01','completed','Generated from the rental workbench','otr.pdf','22/f6bcdbcb7e2243018c0bff168ccd2e4c.pdf','application/pdf',345113,1,'2026-08-04 16:39:54','2026-08-04 16:39:54');
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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_expense_postings`
--

LOCK TABLES `property_expense_postings` WRITE;
/*!40000 ALTER TABLE `property_expense_postings` DISABLE KEYS */;
INSERT INTO `property_expense_postings` VALUES (1,22,'building-management','大樓管理費','2026-08',52,'2026-08-02 09:54:30','2026-08-02 09:54:30'),(2,22,'rental-service-65e33ced-b02d-4ae0-9d53-16211c8c34b5','2','ONCE',53,'2026-08-02 09:54:30','2026-08-02 09:54:30'),(3,22,'fire-insurance','火險','2026',54,'2026-08-02 09:54:30','2026-08-02 09:54:30'),(4,22,'land-tax','地稅','2026',55,'2026-08-02 09:54:30','2026-08-02 09:54:30'),(5,22,'rental-service-3f62deae-3973-4c5a-8e67-ee8060ffc3ea','租客招募2','ONCE',56,'2026-08-02 09:55:34','2026-08-02 09:55:34'),(6,22,'fire-insurance','火險','2026-08',58,'2026-08-02 17:20:54','2026-08-02 17:20:54'),(7,22,'land-tax','地稅','2026-08',59,'2026-08-02 17:20:54','2026-08-02 17:20:54'),(8,23,'building-management','大樓管理費','2026-08',60,'2026-08-04 16:42:14','2026-08-04 16:42:14');
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
) ENGINE=InnoDB AUTO_INCREMENT=2237 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_handover_checklist_items`
--

LOCK TABLES `property_handover_checklist_items` WRITE;
/*!40000 ALTER TABLE `property_handover_checklist_items` DISABLE KEYS */;
INSERT INTO `property_handover_checklist_items` VALUES (1979,22,'鑰匙 Keys','鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)',NULL,NULL,0,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1980,22,'鑰匙 Keys','大門鑰匙 / Main Door Entrance Key',NULL,NULL,1,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1981,22,'鑰匙 Keys','主臥室鑰匙 / Master Bedroom Door Key',NULL,NULL,2,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1982,22,'鑰匙 Keys','臥室2鑰匙 / Bedroom 2 Door Key',NULL,NULL,3,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1983,22,'鑰匙 Keys','臥室3鑰匙 / Bedroom 3 Door Key',NULL,NULL,4,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1984,22,'鑰匙 Keys','臥室4鑰匙 / Bedroom 4 Door Key',NULL,NULL,5,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1985,22,'鑰匙 Keys','書房鑰匙 / Study Room Door Key',NULL,NULL,6,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1986,22,'鑰匙 Keys','儲存室鑰匙 / Store Room Key',NULL,NULL,7,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1987,22,'鑰匙 Keys','陽台鑰匙 / Balcony Key',NULL,NULL,8,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1988,22,'鑰匙 Keys','廚房鑰匙 / Kitchen Door Key',NULL,NULL,9,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1989,22,'鑰匙 Keys','後門鑰匙 / Yard Key',NULL,NULL,10,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1990,22,'鑰匙 Keys','信箱鑰匙 / Mailbox Key',NULL,NULL,11,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1991,22,'鑰匙 Keys','傭人房鑰匙 / Maid Room Door Key',NULL,NULL,12,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1992,22,'門禁卡 Access Card','大門通行卡 / Main Door Access Card',NULL,NULL,13,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1993,22,'門禁卡 Access Card','停車場通行卡 / Parking Access Card',NULL,NULL,14,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1994,22,'門禁卡 Access Card','電梯通行卡 / Lift Access Card',NULL,NULL,15,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1995,22,'門禁卡 Access Card','電梯與停車場通行卡 / Lift with Parking Access Card',NULL,NULL,16,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1996,22,'門禁卡 Access Card','設施通行卡 / Facility Access Card',NULL,NULL,17,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1997,22,'遙控器 Remote Control','空調遙控 / Air Con Remote',NULL,NULL,18,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1998,22,'遙控器 Remote Control','電視遙控 / TV Remote',NULL,NULL,19,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(1999,22,'遙控器 Remote Control','風扇遙控 / Fan Remote',NULL,NULL,20,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2000,22,'遙控器 Remote Control','電燈遙控 / Light Remote',NULL,NULL,21,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2001,22,'客廳 Living Room','窗簾/卷簾 / Curtain/Roller Blind',NULL,NULL,22,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2002,22,'客廳 Living Room','空調 / Air Cond',NULL,NULL,23,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2003,22,'客廳 Living Room','風扇 / Ceiling Fan',NULL,NULL,24,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2004,22,'客廳 Living Room','沙發 / Sofa',NULL,NULL,25,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2005,22,'客廳 Living Room','咖啡桌 / Coffee Table',NULL,NULL,26,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2006,22,'客廳 Living Room','地毯 / Rug',NULL,NULL,27,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2007,22,'客廳 Living Room','電視 / TV',NULL,NULL,28,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2008,22,'客廳 Living Room','電視櫃 / TV Cabinet',NULL,NULL,29,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2009,22,'客廳 Living Room','站立式燈 / Stand Lamp',NULL,NULL,30,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2010,22,'客廳 Living Room','擺架 / Shelf',NULL,NULL,31,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2011,22,'客廳 Living Room','畫 / Wall Painting',NULL,NULL,32,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2012,22,'客廳 Living Room','時鐘 / Clock',NULL,NULL,33,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2013,22,'客廳 Living Room','沙發床/床褥 / Sofa Bed/Mattress',NULL,NULL,34,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2014,22,'客廳 Living Room','書架 / Bookshelf',NULL,NULL,35,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2015,22,'客廳 Living Room','鞋架 / Shoes Rack',NULL,NULL,36,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2016,22,'客廳 Living Room','吊燈 / Pendant Lamp',NULL,NULL,37,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2017,22,'客廳 Living Room','凳子 / Stool',NULL,NULL,38,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2018,22,'飯廳 Dining Room','吊燈 / Pendant Lamp',NULL,NULL,39,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2019,22,'飯廳 Dining Room','餐桌 / Dining Table',NULL,NULL,40,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2020,22,'飯廳 Dining Room','餐椅 / Dining Chair',NULL,NULL,41,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2021,22,'飯廳 Dining Room','畫 / Wall Painting',NULL,NULL,42,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2022,22,'飯廳 Dining Room','電視 / TV',NULL,NULL,43,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2023,22,'飯廳 Dining Room','電視櫃 / TV Cabinet',NULL,NULL,44,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2024,22,'飯廳 Dining Room','鞋架 / Shoes Rack',NULL,NULL,45,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2025,22,'廚房 Kitchen','櫥櫃 / Kitchen Cabinet',NULL,NULL,46,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2026,22,'廚房 Kitchen','洗衣機 / Washer Machine',NULL,NULL,47,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2027,22,'廚房 Kitchen','烘乾機 / Dryer Machine',NULL,NULL,48,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2028,22,'廚房 Kitchen','2合1洗衣烘乾機 / 2in1 Washer Dryer Machine',NULL,NULL,49,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2029,22,'廚房 Kitchen','冰箱 / Refrigerator',NULL,NULL,50,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2030,22,'廚房 Kitchen','微波爐 / Microwave',NULL,NULL,51,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2031,22,'廚房 Kitchen','烤箱 / Oven',NULL,NULL,52,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2032,22,'廚房 Kitchen','電子爐 / Electric Stove',NULL,NULL,53,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2033,22,'廚房 Kitchen','煤氣爐 / Gas Stove',NULL,NULL,54,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2034,22,'廚房 Kitchen','抽油煙機 / Gas Exhaustion',NULL,NULL,55,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2035,22,'廚房 Kitchen','洗碗機 / Dishwasher',NULL,NULL,56,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2036,22,'廚房 Kitchen','熱水壺 / Kettle',NULL,NULL,57,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2037,22,'廚房 Kitchen','濾水器 / Water Filter',NULL,NULL,58,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2038,22,'廚房 Kitchen','窗簾/卷簾 / Curtain/Roller Blind',NULL,NULL,59,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2039,22,'主臥室 Master Bedroom','空調 / Air Cond',NULL,NULL,60,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2040,22,'主臥室 Master Bedroom','窗簾/卷簾 / Curtain/Roller Blind',NULL,NULL,61,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2041,22,'主臥室 Master Bedroom','風扇 / Fan',NULL,NULL,62,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2042,22,'主臥室 Master Bedroom','衣櫃 / Wardrobe',NULL,NULL,63,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2043,22,'主臥室 Master Bedroom','床頭櫃 / Bedside Table',NULL,NULL,64,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2044,22,'主臥室 Master Bedroom','床架 / Bedframe',NULL,NULL,65,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2045,22,'主臥室 Master Bedroom','床褥 / Mattress',NULL,NULL,66,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2046,22,'主臥室 Master Bedroom','梳妝台 / Dressing Table',NULL,NULL,67,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2047,22,'主臥室 Master Bedroom','梳妝台椅子 / Dressing Chair',NULL,NULL,68,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2048,22,'主臥室 Master Bedroom','畫 / Wall Painting',NULL,NULL,69,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2049,22,'主臥室 Master Bedroom','地毯 / Rug',NULL,NULL,70,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2050,22,'主臥室 Master Bedroom','桌燈 / Table Lamp',NULL,NULL,71,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2051,22,'主臥室 Master Bedroom','衣架 / Hanger',NULL,NULL,72,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2052,22,'主臥室 Master Bedroom','書桌 / Study Table',NULL,NULL,73,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2053,22,'主臥室 Master Bedroom','書椅 / Study Chair',NULL,NULL,74,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2054,22,'主臥室 Master Bedroom','吊燈 / Pendant Lamp',NULL,NULL,75,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2055,22,'主臥室 Master Bedroom','擺架 / Shelf',NULL,NULL,76,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2056,22,'主浴室 Master Bathroom','浴缸 / Bathtub',NULL,NULL,77,1,'2026-08-02 09:34:12','2026-08-02 10:52:56'),(2057,22,'主浴室 Master Bathroom','鏡子 / Mirror',NULL,NULL,78,1,'2026-08-02 09:34:12','2026-08-02 10:52:56'),(2058,22,'主浴室 Master Bathroom','花灑 / Shower Hose',NULL,NULL,79,1,'2026-08-02 09:34:12','2026-08-02 10:52:56'),(2059,22,'主浴室 Master Bathroom','洗手盆 / Basin',NULL,NULL,80,1,'2026-08-02 09:34:12','2026-08-02 10:52:56'),(2060,22,'主浴室 Master Bathroom','馬桶 / Toilet Bowl',NULL,NULL,81,1,'2026-08-02 09:34:12','2026-08-02 10:52:56'),(2061,22,'主浴室 Master Bathroom','浴室噴槍 / Bidet Spray',NULL,NULL,82,1,'2026-08-02 09:34:12','2026-08-02 10:52:56'),(2062,22,'主浴室 Master Bathroom','衛生紙架 / Toilet Paper Holder',NULL,NULL,83,1,'2026-08-02 09:34:12','2026-08-02 10:52:56'),(2063,22,'主浴室 Master Bathroom','窗簾/卷簾 / Curtain/Roller Blind',NULL,NULL,84,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2064,22,'主浴室 Master Bathroom','熱水器 / Water Heater',NULL,NULL,85,1,'2026-08-02 09:34:12','2026-08-02 09:34:12'),(2065,23,'鑰匙 Keys','鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)',NULL,NULL,0,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2066,23,'鑰匙 Keys','大門鑰匙 / Main Door Entrance Key',NULL,NULL,1,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2067,23,'鑰匙 Keys','主臥室鑰匙 / Master Bedroom Door Key',NULL,NULL,2,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2068,23,'鑰匙 Keys','臥室2鑰匙 / Bedroom 2 Door Key',NULL,NULL,3,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2069,23,'鑰匙 Keys','臥室3鑰匙 / Bedroom 3 Door Key',NULL,NULL,4,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2070,23,'鑰匙 Keys','臥室4鑰匙 / Bedroom 4 Door Key',NULL,NULL,5,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2071,23,'鑰匙 Keys','書房鑰匙 / Study Room Door Key',NULL,NULL,6,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2072,23,'鑰匙 Keys','儲存室鑰匙 / Store Room Key',NULL,NULL,7,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2073,23,'鑰匙 Keys','陽台鑰匙 / Balcony Key',NULL,NULL,8,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2074,23,'鑰匙 Keys','廚房鑰匙 / Kitchen Door Key',NULL,NULL,9,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2075,23,'鑰匙 Keys','後門鑰匙 / Yard Key',NULL,NULL,10,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2076,23,'鑰匙 Keys','信箱鑰匙 / Mailbox Key',NULL,NULL,11,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2077,23,'鑰匙 Keys','傭人房鑰匙 / Maid Room Door Key',NULL,NULL,12,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2078,23,'門禁卡 Access Card','大門通行卡 / Main Door Access Card',NULL,NULL,13,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2079,23,'門禁卡 Access Card','停車場通行卡 / Parking Access Card',NULL,NULL,14,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2080,23,'門禁卡 Access Card','電梯通行卡 / Lift Access Card',NULL,NULL,15,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2081,23,'門禁卡 Access Card','電梯與停車場通行卡 / Lift with Parking Access Card',NULL,NULL,16,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2082,23,'門禁卡 Access Card','設施通行卡 / Facility Access Card',NULL,NULL,17,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2083,23,'遙控器 Remote Control','空調遙控 / Air Con Remote',NULL,NULL,18,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2084,23,'遙控器 Remote Control','電視遙控 / TV Remote',NULL,NULL,19,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2085,23,'遙控器 Remote Control','風扇遙控 / Fan Remote',NULL,NULL,20,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2086,23,'遙控器 Remote Control','電燈遙控 / Light Remote',NULL,NULL,21,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2087,23,'客廳 Living Room','窗簾/卷簾 / Curtain/Roller Blind',NULL,NULL,22,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2088,23,'客廳 Living Room','空調 / Air Cond',NULL,NULL,23,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2089,23,'客廳 Living Room','風扇 / Ceiling Fan',NULL,NULL,24,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2090,23,'客廳 Living Room','沙發 / Sofa',NULL,NULL,25,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2091,23,'客廳 Living Room','咖啡桌 / Coffee Table',NULL,NULL,26,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2092,23,'客廳 Living Room','地毯 / Rug',NULL,NULL,27,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2093,23,'客廳 Living Room','電視 / TV',NULL,NULL,28,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2094,23,'客廳 Living Room','電視櫃 / TV Cabinet',NULL,NULL,29,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2095,23,'客廳 Living Room','站立式燈 / Stand Lamp',NULL,NULL,30,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2096,23,'客廳 Living Room','擺架 / Shelf',NULL,NULL,31,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2097,23,'客廳 Living Room','畫 / Wall Painting',NULL,NULL,32,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2098,23,'客廳 Living Room','時鐘 / Clock',NULL,NULL,33,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2099,23,'客廳 Living Room','沙發床/床褥 / Sofa Bed/Mattress',NULL,NULL,34,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2100,23,'客廳 Living Room','書架 / Bookshelf',NULL,NULL,35,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2101,23,'客廳 Living Room','鞋架 / Shoes Rack',NULL,NULL,36,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2102,23,'客廳 Living Room','吊燈 / Pendant Lamp',NULL,NULL,37,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2103,23,'客廳 Living Room','凳子 / Stool',NULL,NULL,38,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2104,23,'飯廳 Dining Room','吊燈 / Pendant Lamp',NULL,NULL,39,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2105,23,'飯廳 Dining Room','餐桌 / Dining Table',NULL,NULL,40,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2106,23,'飯廳 Dining Room','餐椅 / Dining Chair',NULL,NULL,41,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2107,23,'飯廳 Dining Room','畫 / Wall Painting',NULL,NULL,42,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2108,23,'飯廳 Dining Room','電視 / TV',NULL,NULL,43,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2109,23,'飯廳 Dining Room','電視櫃 / TV Cabinet',NULL,NULL,44,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2110,23,'飯廳 Dining Room','鞋架 / Shoes Rack',NULL,NULL,45,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2111,23,'廚房 Kitchen','櫥櫃 / Kitchen Cabinet',NULL,NULL,46,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2112,23,'廚房 Kitchen','洗衣機 / Washer Machine',NULL,NULL,47,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2113,23,'廚房 Kitchen','烘乾機 / Dryer Machine',NULL,NULL,48,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2114,23,'廚房 Kitchen','2合1洗衣烘乾機 / 2in1 Washer Dryer Machine',NULL,NULL,49,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2115,23,'廚房 Kitchen','冰箱 / Refrigerator',NULL,NULL,50,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2116,23,'廚房 Kitchen','微波爐 / Microwave',NULL,NULL,51,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2117,23,'廚房 Kitchen','烤箱 / Oven',NULL,NULL,52,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2118,23,'廚房 Kitchen','電子爐 / Electric Stove',NULL,NULL,53,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2119,23,'廚房 Kitchen','煤氣爐 / Gas Stove',NULL,NULL,54,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2120,23,'廚房 Kitchen','抽油煙機 / Gas Exhaustion',NULL,NULL,55,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2121,23,'廚房 Kitchen','洗碗機 / Dishwasher',NULL,NULL,56,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2122,23,'廚房 Kitchen','熱水壺 / Kettle',NULL,NULL,57,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2123,23,'廚房 Kitchen','濾水器 / Water Filter',NULL,NULL,58,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2124,23,'廚房 Kitchen','窗簾/卷簾 / Curtain/Roller Blind',NULL,NULL,59,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2125,23,'主臥室 Master Bedroom','空調 / Air Cond',NULL,NULL,60,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2126,23,'主臥室 Master Bedroom','窗簾/卷簾 / Curtain/Roller Blind',NULL,NULL,61,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2127,23,'主臥室 Master Bedroom','風扇 / Fan',NULL,NULL,62,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2128,23,'主臥室 Master Bedroom','衣櫃 / Wardrobe',NULL,NULL,63,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2129,23,'主臥室 Master Bedroom','床頭櫃 / Bedside Table',NULL,NULL,64,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2130,23,'主臥室 Master Bedroom','床架 / Bedframe',NULL,NULL,65,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2131,23,'主臥室 Master Bedroom','床褥 / Mattress',NULL,NULL,66,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2132,23,'主臥室 Master Bedroom','梳妝台 / Dressing Table',NULL,NULL,67,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2133,23,'主臥室 Master Bedroom','梳妝台椅子 / Dressing Chair',NULL,NULL,68,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2134,23,'主臥室 Master Bedroom','畫 / Wall Painting',NULL,NULL,69,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2135,23,'主臥室 Master Bedroom','地毯 / Rug',NULL,NULL,70,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2136,23,'主臥室 Master Bedroom','桌燈 / Table Lamp',NULL,NULL,71,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2137,23,'主臥室 Master Bedroom','衣架 / Hanger',NULL,NULL,72,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2138,23,'主臥室 Master Bedroom','書桌 / Study Table',NULL,NULL,73,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2139,23,'主臥室 Master Bedroom','書椅 / Study Chair',NULL,NULL,74,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2140,23,'主臥室 Master Bedroom','吊燈 / Pendant Lamp',NULL,NULL,75,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2141,23,'主臥室 Master Bedroom','擺架 / Shelf',NULL,NULL,76,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2142,23,'主浴室 Master Bathroom','浴缸 / Bathtub',NULL,NULL,77,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2143,23,'主浴室 Master Bathroom','鏡子 / Mirror',NULL,NULL,78,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2144,23,'主浴室 Master Bathroom','花灑 / Shower Hose',NULL,NULL,79,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2145,23,'主浴室 Master Bathroom','洗手盆 / Basin',NULL,NULL,80,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2146,23,'主浴室 Master Bathroom','馬桶 / Toilet Bowl',NULL,NULL,81,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2147,23,'主浴室 Master Bathroom','浴室噴槍 / Bidet Spray',NULL,NULL,82,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2148,23,'主浴室 Master Bathroom','衛生紙架 / Toilet Paper Holder',NULL,NULL,83,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2149,23,'主浴室 Master Bathroom','窗簾/卷簾 / Curtain/Roller Blind',NULL,NULL,84,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2150,23,'主浴室 Master Bathroom','熱水器 / Water Heater',NULL,NULL,85,1,'2026-08-04 16:30:34','2026-08-04 16:30:34'),(2151,24,'鑰匙 Keys','鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)',NULL,NULL,0,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2152,24,'鑰匙 Keys','大門鑰匙 / Main Door Entrance Key',NULL,NULL,1,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2153,24,'鑰匙 Keys','主臥室鑰匙 / Master Bedroom Door Key',NULL,NULL,2,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2154,24,'鑰匙 Keys','臥室2鑰匙 / Bedroom 2 Door Key',NULL,NULL,3,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2155,24,'鑰匙 Keys','臥室3鑰匙 / Bedroom 3 Door Key',NULL,NULL,4,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2156,24,'鑰匙 Keys','臥室4鑰匙 / Bedroom 4 Door Key',NULL,NULL,5,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2157,24,'鑰匙 Keys','書房鑰匙 / Study Room Door Key',NULL,NULL,6,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2158,24,'鑰匙 Keys','儲存室鑰匙 / Store Room Key',NULL,NULL,7,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2159,24,'鑰匙 Keys','陽台鑰匙 / Balcony Key',NULL,NULL,8,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2160,24,'鑰匙 Keys','廚房鑰匙 / Kitchen Door Key',NULL,NULL,9,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2161,24,'鑰匙 Keys','後門鑰匙 / Yard Key',NULL,NULL,10,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2162,24,'鑰匙 Keys','信箱鑰匙 / Mailbox Key',NULL,NULL,11,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2163,24,'鑰匙 Keys','傭人房鑰匙 / Maid Room Door Key',NULL,NULL,12,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2164,24,'門禁卡 Access Card','大門通行卡 / Main Door Access Card',NULL,NULL,13,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2165,24,'門禁卡 Access Card','停車場通行卡 / Parking Access Card',NULL,NULL,14,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2166,24,'門禁卡 Access Card','電梯通行卡 / Lift Access Card',NULL,NULL,15,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2167,24,'門禁卡 Access Card','電梯與停車場通行卡 / Lift with Parking Access Card',NULL,NULL,16,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2168,24,'門禁卡 Access Card','設施通行卡 / Facility Access Card',NULL,NULL,17,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2169,24,'遙控器 Remote Control','空調遙控 / Air Con Remote',NULL,NULL,18,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2170,24,'遙控器 Remote Control','電視遙控 / TV Remote',NULL,NULL,19,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2171,24,'遙控器 Remote Control','風扇遙控 / Fan Remote',NULL,NULL,20,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2172,24,'遙控器 Remote Control','電燈遙控 / Light Remote',NULL,NULL,21,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2173,24,'客廳 Living Room','窗簾/卷簾 / Curtain/Roller Blind',NULL,NULL,22,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2174,24,'客廳 Living Room','空調 / Air Cond',NULL,NULL,23,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2175,24,'客廳 Living Room','風扇 / Ceiling Fan',NULL,NULL,24,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2176,24,'客廳 Living Room','沙發 / Sofa',NULL,NULL,25,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2177,24,'客廳 Living Room','咖啡桌 / Coffee Table',NULL,NULL,26,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2178,24,'客廳 Living Room','地毯 / Rug',NULL,NULL,27,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2179,24,'客廳 Living Room','電視 / TV',NULL,NULL,28,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2180,24,'客廳 Living Room','電視櫃 / TV Cabinet',NULL,NULL,29,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2181,24,'客廳 Living Room','站立式燈 / Stand Lamp',NULL,NULL,30,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2182,24,'客廳 Living Room','擺架 / Shelf',NULL,NULL,31,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2183,24,'客廳 Living Room','畫 / Wall Painting',NULL,NULL,32,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2184,24,'客廳 Living Room','時鐘 / Clock',NULL,NULL,33,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2185,24,'客廳 Living Room','沙發床/床褥 / Sofa Bed/Mattress',NULL,NULL,34,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2186,24,'客廳 Living Room','書架 / Bookshelf',NULL,NULL,35,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2187,24,'客廳 Living Room','鞋架 / Shoes Rack',NULL,NULL,36,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2188,24,'客廳 Living Room','吊燈 / Pendant Lamp',NULL,NULL,37,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2189,24,'客廳 Living Room','凳子 / Stool',NULL,NULL,38,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2190,24,'飯廳 Dining Room','吊燈 / Pendant Lamp',NULL,NULL,39,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2191,24,'飯廳 Dining Room','餐桌 / Dining Table',NULL,NULL,40,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2192,24,'飯廳 Dining Room','餐椅 / Dining Chair',NULL,NULL,41,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2193,24,'飯廳 Dining Room','畫 / Wall Painting',NULL,NULL,42,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2194,24,'飯廳 Dining Room','電視 / TV',NULL,NULL,43,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2195,24,'飯廳 Dining Room','電視櫃 / TV Cabinet',NULL,NULL,44,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2196,24,'飯廳 Dining Room','鞋架 / Shoes Rack',NULL,NULL,45,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2197,24,'廚房 Kitchen','櫥櫃 / Kitchen Cabinet',NULL,NULL,46,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2198,24,'廚房 Kitchen','洗衣機 / Washer Machine',NULL,NULL,47,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2199,24,'廚房 Kitchen','烘乾機 / Dryer Machine',NULL,NULL,48,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2200,24,'廚房 Kitchen','2合1洗衣烘乾機 / 2in1 Washer Dryer Machine',NULL,NULL,49,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2201,24,'廚房 Kitchen','冰箱 / Refrigerator',NULL,NULL,50,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2202,24,'廚房 Kitchen','微波爐 / Microwave',NULL,NULL,51,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2203,24,'廚房 Kitchen','烤箱 / Oven',NULL,NULL,52,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2204,24,'廚房 Kitchen','電子爐 / Electric Stove',NULL,NULL,53,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2205,24,'廚房 Kitchen','煤氣爐 / Gas Stove',NULL,NULL,54,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2206,24,'廚房 Kitchen','抽油煙機 / Gas Exhaustion',NULL,NULL,55,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2207,24,'廚房 Kitchen','洗碗機 / Dishwasher',NULL,NULL,56,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2208,24,'廚房 Kitchen','熱水壺 / Kettle',NULL,NULL,57,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2209,24,'廚房 Kitchen','濾水器 / Water Filter',NULL,NULL,58,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2210,24,'廚房 Kitchen','窗簾/卷簾 / Curtain/Roller Blind',NULL,NULL,59,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2211,24,'主臥室 Master Bedroom','空調 / Air Cond',NULL,NULL,60,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2212,24,'主臥室 Master Bedroom','窗簾/卷簾 / Curtain/Roller Blind',NULL,NULL,61,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2213,24,'主臥室 Master Bedroom','風扇 / Fan',NULL,NULL,62,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2214,24,'主臥室 Master Bedroom','衣櫃 / Wardrobe',NULL,NULL,63,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2215,24,'主臥室 Master Bedroom','床頭櫃 / Bedside Table',NULL,NULL,64,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2216,24,'主臥室 Master Bedroom','床架 / Bedframe',NULL,NULL,65,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2217,24,'主臥室 Master Bedroom','床褥 / Mattress',NULL,NULL,66,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2218,24,'主臥室 Master Bedroom','梳妝台 / Dressing Table',NULL,NULL,67,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2219,24,'主臥室 Master Bedroom','梳妝台椅子 / Dressing Chair',NULL,NULL,68,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2220,24,'主臥室 Master Bedroom','畫 / Wall Painting',NULL,NULL,69,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2221,24,'主臥室 Master Bedroom','地毯 / Rug',NULL,NULL,70,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2222,24,'主臥室 Master Bedroom','桌燈 / Table Lamp',NULL,NULL,71,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2223,24,'主臥室 Master Bedroom','衣架 / Hanger',NULL,NULL,72,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2224,24,'主臥室 Master Bedroom','書桌 / Study Table',NULL,NULL,73,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2225,24,'主臥室 Master Bedroom','書椅 / Study Chair',NULL,NULL,74,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2226,24,'主臥室 Master Bedroom','吊燈 / Pendant Lamp',NULL,NULL,75,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2227,24,'主臥室 Master Bedroom','擺架 / Shelf',NULL,NULL,76,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2228,24,'主浴室 Master Bathroom','浴缸 / Bathtub',NULL,NULL,77,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2229,24,'主浴室 Master Bathroom','鏡子 / Mirror',NULL,NULL,78,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2230,24,'主浴室 Master Bathroom','花灑 / Shower Hose',NULL,NULL,79,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2231,24,'主浴室 Master Bathroom','洗手盆 / Basin',NULL,NULL,80,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2232,24,'主浴室 Master Bathroom','馬桶 / Toilet Bowl',NULL,NULL,81,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2233,24,'主浴室 Master Bathroom','浴室噴槍 / Bidet Spray',NULL,NULL,82,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2234,24,'主浴室 Master Bathroom','衛生紙架 / Toilet Paper Holder',NULL,NULL,83,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2235,24,'主浴室 Master Bathroom','窗簾/卷簾 / Curtain/Roller Blind',NULL,NULL,84,1,'2026-08-04 16:59:52','2026-08-04 16:59:52'),(2236,24,'主浴室 Master Bathroom','熱水器 / Water Heater',NULL,NULL,85,1,'2026-08-04 16:59:52','2026-08-04 16:59:52');
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
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_handover_reports`
--

LOCK TABLES `property_handover_reports` WRITE;
/*!40000 ALTER TABLE `property_handover_reports` DISABLE KEYS */;
INSERT INTO `property_handover_reports` VALUES (19,22,NULL,'测试建案1号 101交接報告','2026-08-02',NULL,NULL,NULL,'{\"unitType\":\"3\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',1,1,'2026-08-02 09:34:24','2026-08-02 10:52:56'),(20,22,NULL,'测试建案1号 101交接报告','2026-08-02',NULL,NULL,NULL,'{\"mandateId\":null,\"leaseId\":null,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785634495955},\"unitType\":\"3\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[{\"section\":\"鑰匙、通行卡和遙控器照片 / Keys, Access Card & Remote Control\",\"caption\":\"kitten_avatar_05.jpg\",\"storageKey\":\"22/f24f26386ac749e48b63fbc22f119c70.jpg\",\"originalName\":\"kitten_avatar_05.jpg\"}]}',0,1,'2026-08-02 09:34:56','2026-08-02 09:34:56'),(21,22,NULL,'测试建案1号 101交接报告','2026-08-02',NULL,NULL,NULL,'{\"mandateId\":21,\"leaseId\":null,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785634856508},\"unitType\":\"3\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[{\"section\":\"鑰匙、通行卡和遙控器照片 / Keys, Access Card & Remote Control\",\"caption\":\"kitten_avatar_05.jpg\",\"storageKey\":\"22/af510a9863e74b2289372c14b6898ef0.jpg\",\"originalName\":\"kitten_avatar_05.jpg\"},{\"section\":\"鑰匙、通行卡和遙控器照片 / Keys, Access Card & Remote Control\",\"caption\":\"kitten_avatar_05.jpg\",\"storageKey\":\"22/3afaef985eb64ea0b915ea9c390009bf.jpg\",\"originalName\":\"kitten_avatar_05.jpg\"}]}',0,1,'2026-08-02 09:40:56','2026-08-02 09:40:56'),(22,22,NULL,'测试建案1号 101交接报告','2026-08-02',NULL,NULL,NULL,'{\"mandateId\":21,\"leaseId\":14,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785832489785},\"unitType\":\"3\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 16:34:49','2026-08-04 16:34:49'),(23,23,NULL,'测试建案1号 2交接报告','2026-08-04',NULL,NULL,NULL,'{\"mandateId\":22,\"leaseId\":null,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785832532930},\"unitType\":\"2\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[{\"section\":\"鑰匙、通行卡和遙控器照片 / Keys, Access Card & Remote Control\",\"caption\":\"kitten_avatar_09.jpg\",\"storageKey\":\"23/e04879afeb4946bb94fd96a5b8660e68.jpg\",\"originalName\":\"kitten_avatar_09.jpg\"}]}',0,1,'2026-08-04 16:35:33','2026-08-04 16:35:33'),(24,24,NULL,'测试建案1号 1交接报告','2026-08-04',NULL,NULL,NULL,'{\"mandateId\":null,\"leaseId\":null,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785835358417},\"unitType\":\"1\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 17:22:38','2026-08-04 17:22:38'),(25,23,NULL,'测试建案1号 2交接报告','2026-08-04',NULL,NULL,NULL,'{\"mandateId\":22,\"leaseId\":15,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785839784916},\"unitType\":\"2\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 18:36:25','2026-08-04 18:36:25'),(26,23,NULL,'测试建案1号 2交接报告','2026-08-04',NULL,NULL,NULL,'{\"mandateId\":22,\"leaseId\":15,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785839830561},\"unitType\":\"2\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[{\"section\":\"鑰匙、通行卡和遙控器照片 / Keys, Access Card & Remote Control\",\"caption\":\"kitten_avatar_09.jpg\",\"storageKey\":\"23/03998a8834834cfa83f9337c987a5731.jpg\",\"originalName\":\"kitten_avatar_09.jpg\"},{\"section\":\"鑰匙、通行卡和遙控器照片 / Keys, Access Card & Remote Control\",\"caption\":\"kitten_avatar_09.jpg\",\"storageKey\":\"23/b04d5a9b426046a1b90981290267742b.jpg\",\"originalName\":\"kitten_avatar_09.jpg\"}]}',0,1,'2026-08-04 18:37:10','2026-08-04 18:37:10'),(27,24,NULL,'测试建案1号 1交接报告','2026-08-04',NULL,NULL,NULL,'{\"mandateId\":23,\"leaseId\":null,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785839976143},\"unitType\":\"1\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 18:39:36','2026-08-04 18:39:36'),(28,24,NULL,'测试建案1号 1交接报告','2026-08-04',NULL,NULL,NULL,'{\"mandateId\":23,\"leaseId\":null,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785840265453},\"unitType\":\"1\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 18:44:25','2026-08-04 18:44:25'),(29,22,NULL,'测试建案1号 101交接报告','2026-08-02',NULL,NULL,NULL,'{\"mandateId\":21,\"leaseId\":14,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785841971205},\"unitType\":\"3\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 19:12:51','2026-08-04 19:12:51'),(30,22,NULL,'测试建案1号 101交接报告','2026-08-02',NULL,NULL,NULL,'{\"mandateId\":21,\"leaseId\":14,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785841972816},\"unitType\":\"3\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 19:12:52','2026-08-04 19:12:52'),(31,22,NULL,'测试建案1号 101交接报告','2026-08-02',NULL,NULL,NULL,'{\"mandateId\":21,\"leaseId\":14,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785841973934},\"unitType\":\"3\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 19:12:54','2026-08-04 19:12:54'),(32,22,NULL,'测试建案1号 101交接报告','2026-08-02',NULL,NULL,NULL,'{\"mandateId\":21,\"leaseId\":14,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785841974692},\"unitType\":\"3\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 19:12:54','2026-08-04 19:12:54'),(33,22,NULL,'测试建案1号 101交接报告','2026-08-02',NULL,NULL,NULL,'{\"mandateId\":21,\"leaseId\":14,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785841975393},\"unitType\":\"3\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 19:12:55','2026-08-04 19:12:55'),(34,22,NULL,'测试建案1号 101交接报告','2026-08-02',NULL,NULL,NULL,'{\"mandateId\":21,\"leaseId\":14,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785841976009},\"unitType\":\"3\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 19:12:56','2026-08-04 19:12:56'),(35,22,NULL,'测试建案1号 101交接报告','2026-08-02',NULL,NULL,NULL,'{\"mandateId\":21,\"leaseId\":14,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785841976697},\"unitType\":\"3\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 19:12:56','2026-08-04 19:12:56'),(36,23,NULL,'测试建案1号 2交接报告','2026-08-04',NULL,NULL,NULL,'{\"mandateId\":22,\"leaseId\":15,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785841982185},\"unitType\":\"2\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 19:13:02','2026-08-04 19:13:02'),(37,23,NULL,'测试建案1号 2交接报告','2026-08-04',NULL,NULL,NULL,'{\"mandateId\":22,\"leaseId\":15,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785841982917},\"unitType\":\"2\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 19:13:03','2026-08-04 19:13:03'),(38,23,NULL,'测试建案1号 2交接报告','2026-08-04',NULL,NULL,NULL,'{\"mandateId\":22,\"leaseId\":15,\"handoverType\":{\"isTrusted\":true,\"_vts\":1785841988108},\"unitType\":\"2\",\"handoverFrom\":\"吕志杰\",\"handoverTo\":\"\",\"sections\":[{\"key\":\"主浴室 Master Bathroom\",\"title\":\"主浴室 Master Bathroom\",\"items\":[{\"name\":\"浴缸 / Bathtub\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鏡子 / Mirror\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"花灑 / Shower Hose\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗手盆 / Basin\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"馬桶 / Toilet Bowl\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"浴室噴槍 / Bidet Spray\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衛生紙架 / Toilet Paper Holder\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水器 / Water Heater\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"主臥室 Master Bedroom\",\"title\":\"主臥室 Master Bedroom\",\"items\":[{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣櫃 / Wardrobe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床頭櫃 / Bedside Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床架 / Bedframe\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"床褥 / Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台 / Dressing Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"梳妝台椅子 / Dressing Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"桌燈 / Table Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"衣架 / Hanger\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書桌 / Study Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書椅 / Study Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"客廳 Living Room\",\"title\":\"客廳 Living Room\",\"items\":[{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"空調 / Air Cond\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇 / Ceiling Fan\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發 / Sofa\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"咖啡桌 / Coffee Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"地毯 / Rug\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"站立式燈 / Stand Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"擺架 / Shelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"時鐘 / Clock\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"沙發床/床褥 / Sofa Bed/Mattress\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書架 / Bookshelf\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"凳子 / Stool\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"廚房 Kitchen\",\"title\":\"廚房 Kitchen\",\"items\":[{\"name\":\"櫥櫃 / Kitchen Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗衣機 / Washer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烘乾機 / Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"2合1洗衣烘乾機 / 2in1 Washer Dryer Machine\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"冰箱 / Refrigerator\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"微波爐 / Microwave\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"烤箱 / Oven\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電子爐 / Electric Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"煤氣爐 / Gas Stove\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"抽油煙機 / Gas Exhaustion\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"洗碗機 / Dishwasher\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"熱水壺 / Kettle\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"濾水器 / Water Filter\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"窗簾/卷簾 / Curtain/Roller Blind\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"遙控器 Remote Control\",\"title\":\"遙控器 Remote Control\",\"items\":[{\"name\":\"空調遙控 / Air Con Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視遙控 / TV Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"風扇遙控 / Fan Remote\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電燈遙控 / Light Remote\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"鑰匙 Keys\",\"title\":\"鑰匙 Keys\",\"items\":[{\"name\":\"鐵架門鑰匙（外部鐵架門） / Grill Door Key (External Metal Grill Door)\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"大門鑰匙 / Main Door Entrance Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"主臥室鑰匙 / Master Bedroom Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室2鑰匙 / Bedroom 2 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室3鑰匙 / Bedroom 3 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"臥室4鑰匙 / Bedroom 4 Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"書房鑰匙 / Study Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"儲存室鑰匙 / Store Room Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"陽台鑰匙 / Balcony Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"廚房鑰匙 / Kitchen Door Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"後門鑰匙 / Yard Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"信箱鑰匙 / Mailbox Key\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"傭人房鑰匙 / Maid Room Door Key\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"門禁卡 Access Card\",\"title\":\"門禁卡 Access Card\",\"items\":[{\"name\":\"大門通行卡 / Main Door Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"停車場通行卡 / Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯通行卡 / Lift Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電梯與停車場通行卡 / Lift with Parking Access Card\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"設施通行卡 / Facility Access Card\",\"quantity\":\"\",\"remarks\":\"\"}]},{\"key\":\"飯廳 Dining Room\",\"title\":\"飯廳 Dining Room\",\"items\":[{\"name\":\"吊燈 / Pendant Lamp\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐桌 / Dining Table\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"餐椅 / Dining Chair\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"畫 / Wall Painting\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視 / TV\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"電視櫃 / TV Cabinet\",\"quantity\":\"\",\"remarks\":\"\"},{\"name\":\"鞋架 / Shoes Rack\",\"quantity\":\"\",\"remarks\":\"\"}]}],\"tenantIssues\":[],\"ownerIssues\":[],\"remarks\":\"\",\"photos\":[]}',0,1,'2026-08-04 19:13:08','2026-08-04 19:13:08');
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_handovers`
--

LOCK TABLES `property_handovers` WRITE;
/*!40000 ALTER TABLE `property_handovers` DISABLE KEYS */;
INSERT INTO `property_handovers` VALUES (5,21,'2026-08-02','好',1,1,'1','1','{}','1','','completed',1,'2026-08-02 09:47:49',1,'2026-08-02 09:47:49','2026-08-02 09:47:49');
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_maintenance_records`
--

LOCK TABLES `property_maintenance_records` WRITE;
/*!40000 ALTER TABLE `property_maintenance_records` DISABLE KEYS */;
INSERT INTO `property_maintenance_records` VALUES (5,23,9,'PMR-MWO-20260804164526-C7A2FF','plumbing','1','2026-08-04',0,'','1',NULL,1,'2026-08-04 16:45:53','2026-08-04 16:45:53');
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
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `property_photos`
--

LOCK TABLES `property_photos` WRITE;
/*!40000 ALTER TABLE `property_photos` DISABLE KEYS */;
INSERT INTO `property_photos` VALUES (11,22,NULL,NULL,70,'kitten_avatar_05.jpg','other','交接模块：鑰匙、通行卡和遙控器照片 / Keys, Access Card & Remote Control',0,0,'2026-08-02 09:34:56','2026-08-02 09:34:57'),(12,22,NULL,NULL,71,'kitten_avatar_05.jpg','other','交接模块：鑰匙、通行卡和遙控器照片 / Keys, Access Card & Remote Control',0,1,'2026-08-02 09:34:57','2026-08-02 09:34:57'),(13,23,NULL,NULL,78,'kitten_avatar_09.jpg','other','交接模块：鑰匙、通行卡和遙控器照片 / Keys, Access Card & Remote Control',0,0,'2026-08-04 16:35:33','2026-08-04 16:35:35'),(14,23,NULL,NULL,79,'kitten_avatar_09.jpg','other','交接模块：鑰匙、通行卡和遙控器照片 / Keys, Access Card & Remote Control',0,1,'2026-08-04 16:35:35','2026-08-04 16:35:35');
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
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `purchase_contracts`
--

LOCK TABLES `purchase_contracts` WRITE;
/*!40000 ALTER TABLE `purchase_contracts` DISABLE KEYS */;
INSERT INTO `purchase_contracts` VALUES (20,22,'AUTO-8-23-1785634449709',10000000.00,'MYR','2026-08-02','2026-08-02','completed','2026-08-02 09:34:09','2026-08-02 09:34:09'),(21,23,'AUTO-8-24-1785832225617',0.00,'MYR','2026-08-04','2026-08-04','completed','2026-08-04 16:30:25','2026-08-04 16:30:25'),(22,24,'AUTO-8-25-1785833936743',10000.00,'MYR','2026-08-04','2026-08-04','completed','2026-08-04 16:58:56','2026-08-04 17:32:45');
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
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rent_invoices`
--

LOCK TABLES `rent_invoices` WRITE;
/*!40000 ALTER TABLE `rent_invoices` DISABLE KEYS */;
INSERT INTO `rent_invoices` VALUES (21,13,'2026-08-01','2026-08-01',96.77,96.77,'paid','2026-08-02 09:45:07','2026-08-04 16:46:45'),(23,15,'2026-08-01','2026-08-01',2129.03,0.00,'unpaid','2026-08-04 16:39:19','2026-08-04 16:39:19'),(24,13,'2026-09-01','2026-09-01',6.67,3.23,'partial','2026-08-04 16:46:45','2026-08-04 16:46:45');
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
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rent_payments`
--

LOCK TABLES `rent_payments` WRITE;
/*!40000 ALTER TABLE `rent_payments` DISABLE KEYS */;
INSERT INTO `rent_payments` VALUES (12,21,63,'2026-08-04 16:46:45');
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
) ENGINE=InnoDB AUTO_INCREMENT=128 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rental_mandate_status_history`
--

LOCK TABLES `rental_mandate_status_history` WRITE;
/*!40000 ALTER TABLE `rental_mandate_status_history` DISABLE KEYS */;
INSERT INTO `rental_mandate_status_history` VALUES (120,21,NULL,'draft','建立租管委託',1,'2026-08-02 09:35:24'),(121,21,'draft','pending_review','提交審核',1,'2026-08-02 09:44:01'),(122,21,'pending_review','active','',1,'2026-08-02 09:44:06'),(123,22,NULL,'draft','建立租管委託',1,'2026-08-04 16:32:51'),(124,22,'draft','pending_review','提交審核',1,'2026-08-04 16:32:53'),(125,22,'pending_review','active','',1,'2026-08-04 16:38:05');
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
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rental_mandates`
--

LOCK TABLES `rental_mandates` WRITE;
/*!40000 ALTER TABLE `rental_mandates` DISABLE KEYS */;
INSERT INTO `rental_mandates` VALUES (21,22,'RM-20260802-471BF8','management','2026-08-02','2027-01-01',1.00,10.00,NULL,'active','2026-08-02 09:44:01',1,'2026-08-02 09:44:07','',NULL,1,'2026-08-02 09:35:24','2026-08-02 09:44:06'),(22,23,'RM-20260804-2A8A04','management','2026-08-04','2026-08-09',1.00,1.00,NULL,'active','2026-08-04 16:32:53',1,'2026-08-04 16:38:06','',NULL,1,'2026-08-04 16:32:51','2026-08-04 16:38:05');
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
) ENGINE=InnoDB AUTO_INCREMENT=1208 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report_definitions`
--

LOCK TABLES `report_definitions` WRITE;
/*!40000 ALTER TABLE `report_definitions` DISABLE KEYS */;
INSERT INTO `report_definitions` VALUES (856,'PROPERTY_PAYMENT','房款收款與未收款','property_payment','XLSX','{}',NULL,1,NULL,'2026-08-02 09:59:37','2026-08-02 09:59:37'),(857,'RENT_COLLECTION','租金收款進度','rent_collection','XLSX','{}',NULL,1,NULL,'2026-08-02 09:59:37','2026-08-02 09:59:37'),(858,'INCOME_EXPENSE','收入與支出明細','income_expense','XLSX','{}',NULL,1,NULL,'2026-08-02 09:59:37','2026-08-02 09:59:37'),(859,'MAINTENANCE','維修費用統計','maintenance','PDF','{}',NULL,1,NULL,'2026-08-02 09:59:37','2026-08-02 09:59:37'),(860,'RESERVE','預備金餘額及流水','reserve','XLSX','{}',NULL,1,NULL,'2026-08-02 09:59:37','2026-08-02 09:59:37'),(861,'RESERVE_REFUND','業主預備金返還清單','reserve_refund','XLSX','{}',NULL,1,NULL,'2026-08-02 09:59:37','2026-08-02 09:59:37'),(862,'FINANCE','財務確認記錄','finance','XLSX','{}',NULL,1,NULL,'2026-08-02 09:59:37','2026-08-02 09:59:37'),(863,'SYNC','SQL Account 匯出結果','sync','PDF','{}',NULL,1,NULL,'2026-08-02 09:59:37','2026-08-02 09:59:37');
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
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reserve_accounts`
--

LOCK TABLES `reserve_accounts` WRITE;
/*!40000 ALTER TABLE `reserve_accounts` DISABLE KEYS */;
INSERT INTO `reserve_accounts` VALUES (17,22,1000.00,999.00,'active',1,'2026-08-02 09:34:09','2026-08-04 16:45:14'),(18,23,0.00,0.00,'active',1,'2026-08-04 16:30:25','2026-08-04 16:30:25'),(19,24,0.00,0.00,'active',1,'2026-08-04 17:32:45','2026-08-04 17:32:45');
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
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reserve_transactions`
--

LOCK TABLES `reserve_transactions` WRITE;
/*!40000 ALTER TABLE `reserve_transactions` DISABLE KEYS */;
INSERT INTO `reserve_transactions` VALUES (41,17,57,NULL,'topup',1000.00,'2026-08-02 11:54:10',1000.00,'銀行入賬與充值憑證核對一致',1,'2026-08-02 11:54:09'),(42,17,61,NULL,'debit',1.00,'2026-08-04 16:45:14',999.00,'支出由預備金自動扣除',NULL,'2026-08-04 16:45:14');
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
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenants`
--

LOCK TABLES `tenants` WRITE;
/*!40000 ALTER TABLE `tenants` DISABLE KEYS */;
INSERT INTO `tenants` VALUES (8,NULL,'lzj','5138222','18981712596','1270673745@qq.com','active','2026-08-02 09:44:37','2026-08-02 09:44:37');
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
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `units`
--

LOCK TABLES `units` WRITE;
/*!40000 ALTER TABLE `units` DISABLE KEYS */;
INSERT INTO `units` VALUES (23,14,'1','1','101','3',100.00,1,'rented','2026-08-02 09:34:09','2026-08-02 09:45:07'),(24,14,'2','22','2','2',2.00,2,'rented','2026-08-04 16:30:25','2026-08-04 16:39:19'),(25,14,'1','1','1','1',1.00,1,'available','2026-08-04 16:58:56','2026-08-04 16:58:56');
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
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','admin@example.com','$2b$10$.0fx.vY6sOyPgBOhtfD0nOtgYMKuWfgD5mvS/4YbENnQCxqFjY1VW','System Administrator',NULL,'ADMIN','active','2026-08-04 18:41:48','2026-07-15 14:01:33','2026-08-02 09:26:26'),(10,'18981712596','1270673745@qq.com','$2a$10$t7f520HQJ8ESKbwL9fB3yeqp6dGd00K.kcwUZURNED1qsq07xkmOy','吕志杰','18981712596','OWNER','active','2026-08-04 19:54:33','2026-08-02 09:31:39','2026-08-02 09:31:39');
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

-- Dump completed on 2026-08-05  9:56:05
