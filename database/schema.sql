-- CCPS Property Management
-- MySQL 5.7+ / utf8mb4 (MySQL 8.0 is recommended)
-- The frontend currently reads mock data from frontend/src/data/dashboardData.js.
-- This schema is the persistent model that should replace that mock adapter.

CREATE DATABASE IF NOT EXISTS ccps_property_management
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ccps_property_management;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP VIEW IF EXISTS v_sync_batch_summary;
DROP VIEW IF EXISTS v_finance_review_queue;
DROP VIEW IF EXISTS v_reserve_balances;
DROP VIEW IF EXISTS v_rent_collection_status;
DROP VIEW IF EXISTS v_unit_payment_progress;

DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS report_runs;
DROP TABLE IF EXISTS report_definitions;
DROP TABLE IF EXISTS notification_deliveries;
DROP TABLE IF EXISTS notification_subscriptions;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS notification_rules;
DROP TABLE IF EXISTS sync_batch_items;
DROP TABLE IF EXISTS sync_batches;
DROP TABLE IF EXISTS property_attachments;
DROP TABLE IF EXISTS property_handover_reports;
DROP TABLE IF EXISTS property_important_messages;
DROP TABLE IF EXISTS property_bank_accounts;
DROP TABLE IF EXISTS property_photos;
DROP TABLE IF EXISTS document_links;
DROP TABLE IF EXISTS electronic_signature_events;
DROP TABLE IF EXISTS electronic_signature_requests;
DROP TABLE IF EXISTS documents;
DROP TABLE IF EXISTS reserve_transactions;
DROP TABLE IF EXISTS reserve_accounts;
DROP TABLE IF EXISTS property_maintenance_records;
DROP TABLE IF EXISTS maintenance_status_history;
DROP TABLE IF EXISTS maintenance_work_orders;
DROP TABLE IF EXISTS cashflow_entries;
DROP TABLE IF EXISTS rent_payments;
DROP TABLE IF EXISTS rent_invoices;
DROP TABLE IF EXISTS leases;
DROP TABLE IF EXISTS payment_receipt_allocations;
DROP TABLE IF EXISTS payment_receipts;
DROP TABLE IF EXISTS payment_installments;
DROP TABLE IF EXISTS payment_plans;
DROP TABLE IF EXISTS purchase_contracts;
DROP TABLE IF EXISTS finance_records;
DROP TABLE IF EXISTS vendors;
DROP TABLE IF EXISTS property_handovers;
DROP TABLE IF EXISTS rental_mandate_status_history;
DROP TABLE IF EXISTS rental_mandates;
DROP TABLE IF EXISTS owner_unit_services;
DROP TABLE IF EXISTS property_contract_records;
DROP TABLE IF EXISTS property_expense_postings;
DROP TABLE IF EXISTS property_basic_profiles;
DROP TABLE IF EXISTS owner_units;
DROP TABLE IF EXISTS tenants;
DROP TABLE IF EXISTS owners;
DROP TABLE IF EXISTS units;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS role_permissions;
DROP TABLE IF EXISTS permissions;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. Identity and access
-- ============================================================

CREATE TABLE users (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username VARCHAR(80) NOT NULL,
  email VARCHAR(190) NULL,
  password_hash VARCHAR(255) NOT NULL,
  display_name VARCHAR(120) NOT NULL,
  phone VARCHAR(40) NULL,
  account_type ENUM('ADMIN', 'OWNER') NOT NULL DEFAULT 'OWNER' COMMENT '登入入口類型；管理端與業主端互斥',
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  last_login_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_username (username),
  UNIQUE KEY uk_users_email (email),
  KEY idx_users_account_type_status (account_type, status),
  KEY idx_users_status (status)
) ENGINE=InnoDB;

CREATE TABLE roles (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(80) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_roles_code (code)
) ENGINE=InnoDB;

CREATE TABLE permissions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code VARCHAR(100) NOT NULL,
  name VARCHAR(120) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_permissions_code (code)
) ENGINE=InnoDB;

CREATE TABLE user_roles (
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB;

CREATE TABLE role_permissions (
  role_id BIGINT UNSIGNED NOT NULL,
  permission_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id),
  CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id)
) ENGINE=InnoDB;

-- ============================================================
-- 2. Property master data
-- ============================================================

CREATE TABLE projects (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_code VARCHAR(40) NOT NULL,
  name VARCHAR(160) NOT NULL,
  address VARCHAR(255) NULL,
  city VARCHAR(100) NULL,
  country_code CHAR(2) NOT NULL DEFAULT 'MY',
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_projects_code (project_code),
  KEY idx_projects_status (status)
) ENGINE=InnoDB;

CREATE TABLE units (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id BIGINT UNSIGNED NOT NULL,
  building VARCHAR(80) NULL,
  floor_no VARCHAR(20) NULL,
  unit_no VARCHAR(40) NOT NULL,
  unit_type VARCHAR(80) NULL,
  area_sqm DECIMAL(12,2) NULL,
  bedroom_count TINYINT UNSIGNED NULL,
  listing_status VARCHAR(30) NOT NULL DEFAULT 'available',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_units_project_unit (project_id, unit_no),
  KEY idx_units_project_status (project_id, listing_status),
  CONSTRAINT fk_units_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB;

CREATE TABLE owners (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL,
  owner_no VARCHAR(30) NULL,
  full_name VARCHAR(160) NOT NULL,
  identity_no VARCHAR(120) NULL COMMENT '敏感資料；應在應用層或欄位層加密，禁止寫入日誌',
  phone VARCHAR(40) NULL,
  mobile_phone VARCHAR(40) NULL,
  home_phone VARCHAR(40) NULL,
  office_phone VARCHAR(40) NULL,
  passport_no VARCHAR(80) NULL,
  email VARCHAR(190) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_owners_name (full_name),
  KEY idx_owners_phone (phone),
  KEY idx_owners_user (user_id),
  CONSTRAINT fk_owners_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE tenants (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id BIGINT UNSIGNED NULL,
  full_name VARCHAR(160) NOT NULL,
  identity_no VARCHAR(120) NULL COMMENT '敏感資料；應在應用層或欄位層加密，禁止寫入日誌',
  phone VARCHAR(40) NULL,
  email VARCHAR(190) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tenants_name (full_name),
  KEY idx_tenants_phone (phone),
  KEY idx_tenants_user (user_id),
  CONSTRAINT fk_tenants_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE owner_units (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_id BIGINT UNSIGNED NOT NULL,
  unit_id BIGINT UNSIGNED NOT NULL,
  ownership_percent DECIMAL(5,2) NOT NULL DEFAULT 100.00,
  is_primary TINYINT(1) NOT NULL DEFAULT 1,
  start_date DATE NULL,
  end_date DATE NULL,
  asset_stage ENUM('PRE_HANDOVER', 'OPERATING', 'DISPOSED') NOT NULL DEFAULT 'PRE_HANDOVER',
  expected_handover_date DATE NULL,
  actual_handover_date DATE NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_owner_units_period (owner_id, unit_id, start_date),
  KEY idx_owner_units_unit (unit_id, status),
  KEY idx_owner_units_owner (owner_id, status),
  KEY idx_owner_units_stage (asset_stage, status),
  CONSTRAINT fk_owner_units_owner FOREIGN KEY (owner_id) REFERENCES owners (id),
  CONSTRAINT fk_owner_units_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT chk_owner_units_percent CHECK (ownership_percent > 0 AND ownership_percent <= 100)
) ENGINE=InnoDB;

CREATE TABLE owner_unit_services (
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  service_type ENUM('RENTAL', 'RESALE', 'MANAGEMENT') NOT NULL,
  status ENUM('active', 'paused', 'ended') NOT NULL DEFAULT 'active',
  started_at DATE NULL,
  ended_at DATE NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (owner_unit_id, service_type),
  KEY idx_owner_unit_services_status (service_type, status),
  CONSTRAINT fk_owner_unit_services_holding FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id)
) ENGINE=InnoDB;

CREATE TABLE property_basic_profiles (
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  profile_json JSON NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (owner_unit_id),
  CONSTRAINT fk_property_basic_profiles_owner_unit
    FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id)
) ENGINE=InnoDB;

CREATE TABLE rental_mandates (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  mandate_no VARCHAR(60) NOT NULL,
  mandate_type VARCHAR(30) NOT NULL DEFAULT 'management',
  start_date DATE NOT NULL,
  end_date DATE NULL,
  management_fee DECIMAL(18,2) NULL,
  commission_percent DECIMAL(5,2) NULL,
  responsible_user_id BIGINT UNSIGNED NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'draft',
  submitted_at DATETIME NULL,
  reviewed_by BIGINT UNSIGNED NULL,
  reviewed_at DATETIME NULL,
  review_note VARCHAR(500) NULL,
  termination_reason VARCHAR(500) NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rental_mandates_no (mandate_no),
  KEY idx_rental_mandates_unit_status (owner_unit_id, status),
  KEY idx_rental_mandates_responsible (responsible_user_id, status),
  CONSTRAINT fk_rental_mandates_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_rental_mandates_responsible FOREIGN KEY (responsible_user_id) REFERENCES users (id),
  CONSTRAINT fk_rental_mandates_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id),
  CONSTRAINT fk_rental_mandates_creator FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_rental_mandates_dates CHECK (end_date IS NULL OR end_date >= start_date),
  CONSTRAINT chk_rental_mandates_fee CHECK (management_fee IS NULL OR management_fee >= 0),
  CONSTRAINT chk_rental_mandates_commission CHECK (commission_percent IS NULL OR (commission_percent >= 0 AND commission_percent <= 100)),
  CONSTRAINT chk_rental_mandates_status CHECK (status IN ('draft', 'pending_review', 'active', 'suspended', 'terminated', 'expired'))
) ENGINE=InnoDB;

CREATE TABLE rental_mandate_status_history (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  mandate_id BIGINT UNSIGNED NOT NULL,
  from_status VARCHAR(30) NULL,
  to_status VARCHAR(30) NOT NULL,
  reason VARCHAR(500) NULL,
  changed_by BIGINT UNSIGNED NULL,
  changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_rental_mandate_history_mandate (mandate_id, changed_at),
  CONSTRAINT fk_rental_mandate_history_mandate FOREIGN KEY (mandate_id) REFERENCES rental_mandates (id),
  CONSTRAINT fk_rental_mandate_history_user FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE property_handovers (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  mandate_id BIGINT UNSIGNED NOT NULL,
  handover_date DATE NOT NULL,
  condition_summary VARCHAR(2000) NULL,
  key_count INT UNSIGNED NOT NULL DEFAULT 0,
  access_card_count INT UNSIGNED NOT NULL DEFAULT 0,
  water_meter VARCHAR(120) NULL,
  electricity_meter VARCHAR(120) NULL,
  inventory JSON NULL,
  received_by VARCHAR(160) NULL,
  notes VARCHAR(1000) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'draft',
  completed_by BIGINT UNSIGNED NULL,
  completed_at DATETIME NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_property_handovers_mandate (mandate_id),
  KEY idx_property_handovers_status (status, handover_date),
  CONSTRAINT fk_property_handovers_mandate FOREIGN KEY (mandate_id) REFERENCES rental_mandates (id),
  CONSTRAINT fk_property_handovers_completed_by FOREIGN KEY (completed_by) REFERENCES users (id),
  CONSTRAINT fk_property_handovers_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_property_handovers_status CHECK (status IN ('draft','completed'))
) ENGINE=InnoDB;

CREATE TABLE vendors (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  vendor_code VARCHAR(50) NOT NULL,
  name VARCHAR(160) NOT NULL,
  contact_name VARCHAR(120) NULL,
  phone VARCHAR(40) NULL,
  email VARCHAR(190) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_vendors_code (vendor_code),
  KEY idx_vendors_name (name)
) ENGINE=InnoDB;

-- ============================================================
-- 3. Finance and property payment progress
-- ============================================================

CREATE TABLE sync_batches (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  batch_no VARCHAR(60) NOT NULL,
  source_module VARCHAR(80) NOT NULL,
  trigger_mode VARCHAR(20) NOT NULL DEFAULT 'manual',
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  total_count INT UNSIGNED NOT NULL DEFAULT 0,
  success_count INT UNSIGNED NOT NULL DEFAULT 0,
  failure_count INT UNSIGNED NOT NULL DEFAULT 0,
  started_at DATETIME NULL,
  completed_at DATETIME NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sync_batches_no (batch_no),
  KEY idx_sync_batches_status_time (status, created_at),
  CONSTRAINT fk_sync_batches_creator FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE finance_records (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  transaction_no VARCHAR(60) NOT NULL,
  record_type VARCHAR(40) NOT NULL COMMENT 'property_payment / rent_payment / cashflow / reserve_topup / reserve_debit',
  unit_id BIGINT UNSIGNED NULL,
  owner_id BIGINT UNSIGNED NULL,
  tenant_id BIGINT UNSIGNED NULL,
  amount DECIMAL(18,2) NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'MYR',
  transaction_date DATE NOT NULL,
  payment_method VARCHAR(40) NULL,
  payment_status VARCHAR(30) NOT NULL DEFAULT 'unpaid',
  confirmation_status VARCHAR(30) NOT NULL DEFAULT 'pending',
  confirmed_by BIGINT UNSIGNED NULL,
  confirmed_at DATETIME NULL,
  sync_status VARCHAR(30) NOT NULL DEFAULT 'not_synced',
  sync_batch_id BIGINT UNSIGNED NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_finance_records_no (transaction_no),
  KEY idx_finance_records_review (confirmation_status, transaction_date),
  KEY idx_finance_records_unit_date (unit_id, transaction_date),
  KEY idx_finance_records_sync (sync_status, sync_batch_id),
  CONSTRAINT fk_finance_records_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT fk_finance_records_owner FOREIGN KEY (owner_id) REFERENCES owners (id),
  CONSTRAINT fk_finance_records_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
  CONSTRAINT fk_finance_records_confirmed_by FOREIGN KEY (confirmed_by) REFERENCES users (id),
  CONSTRAINT fk_finance_records_sync_batch FOREIGN KEY (sync_batch_id) REFERENCES sync_batches (id),
  CONSTRAINT fk_finance_records_creator FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_finance_records_amount CHECK (amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE property_expense_postings (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  charge_key VARCHAR(120) NOT NULL,
  charge_name VARCHAR(180) NOT NULL,
  period_key VARCHAR(20) NOT NULL,
  finance_record_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_property_expense_period (owner_unit_id, charge_key, period_key),
  KEY idx_property_expense_finance (finance_record_id),
  CONSTRAINT fk_property_expense_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_property_expense_finance FOREIGN KEY (finance_record_id) REFERENCES finance_records (id)
) ENGINE=InnoDB;

CREATE TABLE property_contract_records (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  lease_id BIGINT UNSIGNED NULL,
  contract_type VARCHAR(40) NOT NULL,
  contract_no VARCHAR(80) NOT NULL,
  signed_date DATE NULL,
  valid_from DATE NULL,
  valid_to DATE NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  notes VARCHAR(1000) NULL,
  original_name VARCHAR(255) NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  mime_type VARCHAR(120) NOT NULL,
  file_size BIGINT UNSIGNED NOT NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_property_contract_records_no (owner_unit_id, contract_no),
  UNIQUE KEY uk_property_contract_records_lease (lease_id),
  KEY idx_property_contract_records_type (owner_unit_id, contract_type, status),
  CONSTRAINT fk_property_contract_records_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_property_contract_records_creator FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_property_contract_records_dates CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to >= valid_from),
  CONSTRAINT chk_property_contract_records_status CHECK (status IN ('draft', 'active', 'completed', 'cancelled'))
) ENGINE=InnoDB;

CREATE TABLE purchase_contracts (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  contract_no VARCHAR(60) NOT NULL,
  purchase_price DECIMAL(18,2) NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'MYR',
  signed_date DATE NULL,
  handover_date DATE NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'active',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_purchase_contracts_no (contract_no),
  KEY idx_purchase_contracts_owner_unit (owner_unit_id, status),
  CONSTRAINT fk_purchase_contracts_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT chk_purchase_contracts_price CHECK (purchase_price >= 0)
) ENGINE=InnoDB;

CREATE TABLE payment_plans (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  purchase_contract_id BIGINT UNSIGNED NOT NULL,
  plan_name VARCHAR(120) NOT NULL,
  installment_count SMALLINT UNSIGNED NOT NULL DEFAULT 1,
  total_amount DECIMAL(18,2) NOT NULL,
  start_date DATE NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_payment_plans_contract (purchase_contract_id, status),
  CONSTRAINT fk_payment_plans_contract FOREIGN KEY (purchase_contract_id) REFERENCES purchase_contracts (id),
  CONSTRAINT chk_payment_plans_amount CHECK (total_amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE payment_installments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  payment_plan_id BIGINT UNSIGNED NOT NULL,
  installment_no SMALLINT UNSIGNED NOT NULL,
  milestone VARCHAR(160) NULL,
  due_date DATE NOT NULL,
  amount_due DECIMAL(18,2) NOT NULL,
  amount_paid DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(30) NOT NULL DEFAULT 'pending',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_installments_no (payment_plan_id, installment_no),
  KEY idx_payment_installments_due_status (due_date, status),
  CONSTRAINT fk_payment_installments_plan FOREIGN KEY (payment_plan_id) REFERENCES payment_plans (id),
  CONSTRAINT chk_payment_installments_amount CHECK (amount_due >= 0 AND amount_paid >= 0)
) ENGINE=InnoDB;

CREATE TABLE payment_receipts (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  finance_record_id BIGINT UNSIGNED NOT NULL,
  receipt_no VARCHAR(60) NULL,
  payer_name VARCHAR(160) NULL,
  bank_reference VARCHAR(120) NULL,
  proof_document_id BIGINT UNSIGNED NULL,
  submission_note VARCHAR(500) NULL,
  review_note VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_payment_receipts_finance (finance_record_id),
  UNIQUE KEY uk_payment_receipts_no (receipt_no),
  CONSTRAINT fk_payment_receipts_finance FOREIGN KEY (finance_record_id) REFERENCES finance_records (id)
) ENGINE=InnoDB;

CREATE TABLE payment_receipt_allocations (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  receipt_id BIGINT UNSIGNED NOT NULL,
  installment_id BIGINT UNSIGNED NOT NULL,
  allocated_amount DECIMAL(18,2) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_receipt_allocations (receipt_id, installment_id),
  CONSTRAINT fk_receipt_allocations_receipt FOREIGN KEY (receipt_id) REFERENCES payment_receipts (id),
  CONSTRAINT fk_receipt_allocations_installment FOREIGN KEY (installment_id) REFERENCES payment_installments (id),
  CONSTRAINT chk_receipt_allocations_amount CHECK (allocated_amount > 0)
) ENGINE=InnoDB;

-- ============================================================
-- 4. Tenancy and rent collection
-- ============================================================

CREATE TABLE leases (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  unit_id BIGINT UNSIGNED NOT NULL,
  tenant_id BIGINT UNSIGNED NOT NULL,
  rental_mandate_id BIGINT UNSIGNED NULL,
  lease_no VARCHAR(60) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  monthly_rent DECIMAL(18,2) NOT NULL,
  deposit_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  payment_day TINYINT UNSIGNED NOT NULL DEFAULT 1,
  rent_calculation_method VARCHAR(30) NOT NULL DEFAULT 'daily_prorated' COMMENT '按當月實際承租天數折算',
  status VARCHAR(30) NOT NULL DEFAULT 'active',
  contract_document_id BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_leases_no (lease_no),
  KEY idx_leases_rental_mandate (rental_mandate_id, status),
  KEY idx_leases_unit_status (unit_id, status),
  KEY idx_leases_tenant_status (tenant_id, status),
  KEY idx_leases_end_date (end_date, status),
  CONSTRAINT fk_leases_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT fk_leases_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
  CONSTRAINT fk_leases_rental_mandate FOREIGN KEY (rental_mandate_id) REFERENCES rental_mandates (id),
  CONSTRAINT chk_leases_dates CHECK (end_date >= start_date),
  CONSTRAINT chk_leases_payment_day CHECK (payment_day BETWEEN 1 AND 31)
) ENGINE=InnoDB;

ALTER TABLE property_contract_records
  ADD CONSTRAINT fk_property_contract_records_lease
  FOREIGN KEY (lease_id) REFERENCES leases (id);

CREATE TABLE rent_invoices (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  lease_id BIGINT UNSIGNED NOT NULL,
  billing_month DATE NOT NULL COMMENT '統一存該月份第一天',
  due_date DATE NOT NULL,
  amount_due DECIMAL(18,2) NOT NULL,
  amount_paid DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(30) NOT NULL DEFAULT 'unpaid',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rent_invoices_month (lease_id, billing_month),
  KEY idx_rent_invoices_due_status (due_date, status),
  CONSTRAINT fk_rent_invoices_lease FOREIGN KEY (lease_id) REFERENCES leases (id),
  CONSTRAINT chk_rent_invoices_amount CHECK (amount_due >= 0 AND amount_paid >= 0)
) ENGINE=InnoDB;

CREATE TABLE rent_invoice_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  invoice_id BIGINT UNSIGNED NOT NULL,
  charge_type VARCHAR(40) NOT NULL COMMENT 'management / utilities / maintenance / other',
  description VARCHAR(255) NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  payer VARCHAR(20) NOT NULL DEFAULT 'tenant' COMMENT 'tenant / owner / agency',
  source_type VARCHAR(40) NULL,
  source_id BIGINT UNSIGNED NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_invoice_items_invoice (invoice_id),
  KEY idx_invoice_items_source (source_type, source_id),
  CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES rent_invoices (id),
  CONSTRAINT fk_invoice_items_creator FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_invoice_items_amount CHECK (amount > 0),
  CONSTRAINT chk_invoice_items_payer CHECK (payer IN ('tenant', 'owner', 'agency'))
) ENGINE=InnoDB;

CREATE TABLE rent_payments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rent_invoice_id BIGINT UNSIGNED NOT NULL,
  finance_record_id BIGINT UNSIGNED NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_rent_payments_finance (finance_record_id),
  KEY idx_rent_payments_invoice (rent_invoice_id),
  CONSTRAINT fk_rent_payments_invoice FOREIGN KEY (rent_invoice_id) REFERENCES rent_invoices (id),
  CONSTRAINT fk_rent_payments_finance FOREIGN KEY (finance_record_id) REFERENCES finance_records (id)
) ENGINE=InnoDB;

-- ============================================================
-- 5. Income, expenses and maintenance
-- ============================================================

CREATE TABLE cashflow_entries (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  finance_record_id BIGINT UNSIGNED NOT NULL,
  unit_id BIGINT UNSIGNED NULL,
  lease_id BIGINT UNSIGNED NULL,
  owner_id BIGINT UNSIGNED NULL,
  tenant_id BIGINT UNSIGNED NULL,
  vendor_id BIGINT UNSIGNED NULL,
  direction VARCHAR(10) NOT NULL COMMENT 'income / expense',
  category VARCHAR(60) NOT NULL COMMENT 'rent / maintenance / utilities / management / other',
  description VARCHAR(500) NOT NULL,
  occurred_on DATE NOT NULL,
  reserve_account_id BIGINT UNSIGNED NULL,
  attachment_status VARCHAR(30) NOT NULL DEFAULT 'missing',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_cashflow_finance (finance_record_id),
  KEY idx_cashflow_date_category (occurred_on, category),
  KEY idx_cashflow_unit (unit_id, occurred_on),
  CONSTRAINT fk_cashflow_finance FOREIGN KEY (finance_record_id) REFERENCES finance_records (id),
  CONSTRAINT fk_cashflow_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT fk_cashflow_lease FOREIGN KEY (lease_id) REFERENCES leases (id),
  CONSTRAINT fk_cashflow_owner FOREIGN KEY (owner_id) REFERENCES owners (id),
  CONSTRAINT fk_cashflow_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
  CONSTRAINT fk_cashflow_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
  CONSTRAINT chk_cashflow_direction CHECK (direction IN ('income', 'expense'))
) ENGINE=InnoDB;

CREATE TABLE maintenance_work_orders (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  work_order_no VARCHAR(60) NOT NULL,
  unit_id BIGINT UNSIGNED NOT NULL,
  lease_id BIGINT UNSIGNED NULL,
  owner_id BIGINT UNSIGNED NULL,
  tenant_id BIGINT UNSIGNED NULL,
  vendor_id BIGINT UNSIGNED NULL,
  cashflow_entry_id BIGINT UNSIGNED NULL,
  category VARCHAR(60) NOT NULL,
  title VARCHAR(180) NOT NULL,
  description VARCHAR(1000) NULL,
  requested_at DATETIME NOT NULL,
  completed_at DATETIME NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'open',
  estimated_amount DECIMAL(18,2) NULL,
  actual_amount DECIMAL(18,2) NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_work_orders_no (work_order_no),
  KEY idx_work_orders_status_date (status, requested_at),
  KEY idx_work_orders_unit (unit_id, status),
  CONSTRAINT fk_work_orders_unit FOREIGN KEY (unit_id) REFERENCES units (id),
  CONSTRAINT fk_work_orders_lease FOREIGN KEY (lease_id) REFERENCES leases (id),
  CONSTRAINT fk_work_orders_owner FOREIGN KEY (owner_id) REFERENCES owners (id),
  CONSTRAINT fk_work_orders_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id),
  CONSTRAINT fk_work_orders_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
  CONSTRAINT fk_work_orders_cashflow FOREIGN KEY (cashflow_entry_id) REFERENCES cashflow_entries (id),
  CONSTRAINT fk_work_orders_creator FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE maintenance_status_history (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  work_order_id BIGINT UNSIGNED NOT NULL,
  status VARCHAR(30) NOT NULL,
  occurred_at DATETIME NOT NULL,
  note VARCHAR(500) NULL,
  changed_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_maintenance_history_order_time (work_order_id, occurred_at),
  CONSTRAINT fk_maintenance_history_order FOREIGN KEY (work_order_id) REFERENCES maintenance_work_orders (id),
  CONSTRAINT fk_maintenance_history_user FOREIGN KEY (changed_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE property_maintenance_records (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  work_order_id BIGINT UNSIGNED NULL,
  record_no VARCHAR(60) NOT NULL,
  category VARCHAR(60) NOT NULL,
  title VARCHAR(180) NOT NULL,
  maintenance_date DATE NOT NULL,
  duration_minutes INT UNSIGNED NOT NULL DEFAULT 0,
  details VARCHAR(1000) NULL,
  result_summary VARCHAR(500) NULL,
  next_maintenance_date DATE NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_property_maintenance_no (record_no),
  UNIQUE KEY uk_property_maintenance_work_order (work_order_id),
  KEY idx_property_maintenance_unit_date (owner_unit_id, maintenance_date),
  CONSTRAINT fk_property_maintenance_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_property_maintenance_work_order FOREIGN KEY (work_order_id) REFERENCES maintenance_work_orders (id),
  CONSTRAINT fk_property_maintenance_creator FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB;

-- ============================================================
-- 6. Reserve money
-- ============================================================

CREATE TABLE reserve_accounts (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  minimum_balance DECIMAL(18,2) NOT NULL DEFAULT 0,
  current_balance DECIMAL(18,2) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'active',
  low_balance_alert_enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reserve_accounts_owner_unit (owner_unit_id),
  KEY idx_reserve_accounts_balance (current_balance, minimum_balance),
  CONSTRAINT fk_reserve_accounts_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT chk_reserve_accounts_balance CHECK (minimum_balance >= 0 AND current_balance >= 0)
) ENGINE=InnoDB;

ALTER TABLE cashflow_entries
  ADD CONSTRAINT fk_cashflow_reserve_account
  FOREIGN KEY (reserve_account_id) REFERENCES reserve_accounts (id);

CREATE TABLE reserve_transactions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  reserve_account_id BIGINT UNSIGNED NOT NULL,
  finance_record_id BIGINT UNSIGNED NULL,
  maintenance_work_order_id BIGINT UNSIGNED NULL,
  transaction_type VARCHAR(20) NOT NULL COMMENT 'topup / debit / adjustment',
  amount DECIMAL(18,2) NOT NULL,
  occurred_at DATETIME NOT NULL,
  balance_after DECIMAL(18,2) NOT NULL,
  note VARCHAR(500) NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_reserve_transactions_account_date (reserve_account_id, occurred_at),
  KEY idx_reserve_transactions_type (transaction_type, occurred_at),
  CONSTRAINT fk_reserve_transactions_account FOREIGN KEY (reserve_account_id) REFERENCES reserve_accounts (id),
  CONSTRAINT fk_reserve_transactions_finance FOREIGN KEY (finance_record_id) REFERENCES finance_records (id),
  CONSTRAINT fk_reserve_transactions_work_order FOREIGN KEY (maintenance_work_order_id) REFERENCES maintenance_work_orders (id),
  CONSTRAINT fk_reserve_transactions_creator FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT chk_reserve_transactions_amount CHECK (amount > 0 AND balance_after >= 0)
) ENGINE=InnoDB;

-- ============================================================
-- 7. Documents and attachments
-- ============================================================

CREATE TABLE documents (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  document_no VARCHAR(60) NULL,
  original_name VARCHAR(255) NOT NULL,
  storage_key VARCHAR(500) NOT NULL,
  mime_type VARCHAR(120) NULL,
  file_size BIGINT UNSIGNED NULL,
  checksum_sha256 CHAR(64) NULL,
  document_type VARCHAR(60) NOT NULL COMMENT 'purchase_contract / payment_proof / lease / invoice / maintenance_attachment / identity',
  status VARCHAR(30) NOT NULL DEFAULT 'pending_review',
  expires_at DATE NULL,
  uploaded_by BIGINT UNSIGNED NULL,
  reviewed_by BIGINT UNSIGNED NULL,
  reviewed_at DATETIME NULL,
  review_note VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_documents_no (document_no),
  KEY idx_documents_expiry (expires_at, status),
  KEY idx_documents_type_status (document_type, status),
  CONSTRAINT fk_documents_uploader FOREIGN KEY (uploaded_by) REFERENCES users (id),
  CONSTRAINT fk_documents_reviewer FOREIGN KEY (reviewed_by) REFERENCES users (id)
) ENGINE=InnoDB;

-- These two references point to the document table declared below.
ALTER TABLE payment_receipts
  ADD CONSTRAINT fk_payment_receipts_document
  FOREIGN KEY (proof_document_id) REFERENCES documents (id);

ALTER TABLE leases
  ADD CONSTRAINT fk_leases_document
  FOREIGN KEY (contract_document_id) REFERENCES documents (id);

CREATE TABLE document_links (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  document_id BIGINT UNSIGNED NOT NULL,
  entity_type VARCHAR(40) NOT NULL COMMENT 'owner / tenant / unit / contract / lease / finance / cashflow / work_order / reserve_transaction',
  entity_id BIGINT UNSIGNED NOT NULL,
  relation_type VARCHAR(40) NOT NULL DEFAULT 'attachment',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_document_links (document_id, entity_type, entity_id, relation_type),
  KEY idx_document_links_entity (entity_type, entity_id),
  CONSTRAINT fk_document_links_document FOREIGN KEY (document_id) REFERENCES documents (id)
) ENGINE=InnoDB;

CREATE TABLE electronic_signature_requests (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_document_id BIGINT UNSIGNED NOT NULL,
  entity_type VARCHAR(40) NOT NULL,
  entity_id BIGINT UNSIGNED NOT NULL,
  signer_name VARCHAR(190) NOT NULL,
  signer_email VARCHAR(190) NOT NULL,
  access_token_hash CHAR(64) NOT NULL,
  verification_code_hash VARCHAR(100) NOT NULL,
  verification_expires_at DATETIME NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  expires_at DATETIME NOT NULL,
  requested_by BIGINT UNSIGNED NULL,
  requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  signed_at DATETIME NULL,
  signed_document_id BIGINT UNSIGNED NULL,
  source_checksum_sha256 CHAR(64) NULL,
  signature_hash CHAR(64) NULL,
  signer_ip VARCHAR(64) NULL,
  signer_user_agent VARCHAR(500) NULL,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_e_signature_token (access_token_hash),
  KEY idx_e_signature_entity (entity_type, entity_id, status),
  KEY idx_e_signature_expiry (status, expires_at),
  CONSTRAINT fk_e_signature_source_document FOREIGN KEY (source_document_id) REFERENCES documents (id),
  CONSTRAINT fk_e_signature_signed_document FOREIGN KEY (signed_document_id) REFERENCES documents (id),
  CONSTRAINT fk_e_signature_requester FOREIGN KEY (requested_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE electronic_signature_events (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  signature_request_id BIGINT UNSIGNED NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  detail VARCHAR(500) NULL,
  remote_ip VARCHAR(64) NULL,
  user_agent VARCHAR(500) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_e_signature_events_request (signature_request_id, created_at),
  CONSTRAINT fk_e_signature_events_request FOREIGN KEY (signature_request_id) REFERENCES electronic_signature_requests (id)
) ENGINE=InnoDB;

CREATE TABLE property_photos (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  lease_id BIGINT UNSIGNED NULL,
  rental_stage VARCHAR(20) NULL COMMENT 'before / after',
  document_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(120) NOT NULL,
  category VARCHAR(30) NOT NULL DEFAULT 'interior',
  description VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  is_cover TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_property_photos_document (document_id),
  KEY idx_property_photos_lease_stage (lease_id, rental_stage),
  KEY idx_property_photos_order (owner_unit_id, sort_order, id),
  CONSTRAINT fk_property_photos_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_property_photos_lease FOREIGN KEY (lease_id) REFERENCES leases (id),
  CONSTRAINT fk_property_photos_document FOREIGN KEY (document_id) REFERENCES documents (id)
) ENGINE=InnoDB;

CREATE TABLE property_attachments (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  document_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(160) NOT NULL,
  remarks VARCHAR(1000) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_property_attachments_document (document_id),
  KEY idx_property_attachments_owner_unit (owner_unit_id, enabled, created_at),
  CONSTRAINT fk_property_attachments_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_property_attachments_document FOREIGN KEY (document_id) REFERENCES documents (id),
  CONSTRAINT fk_property_attachments_creator FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE property_handover_reports (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  document_id BIGINT UNSIGNED NULL,
  title VARCHAR(160) NOT NULL,
  report_date DATE NOT NULL,
  tracking_start_date DATE NULL,
  tracking_end_date DATE NULL,
  remarks VARCHAR(1000) NULL,
  content_json MEDIUMTEXT NULL,
  completed TINYINT(1) NOT NULL DEFAULT 0,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_property_handover_reports_document (document_id),
  KEY idx_property_handover_reports_owner_unit (owner_unit_id, completed, report_date),
  CONSTRAINT fk_property_handover_reports_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_property_handover_reports_document FOREIGN KEY (document_id) REFERENCES documents (id),
  CONSTRAINT fk_property_handover_reports_creator FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE property_handover_checklist_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  category VARCHAR(120) NOT NULL,
  item_name VARCHAR(255) NOT NULL,
  default_quantity VARCHAR(80) NULL,
  notes VARCHAR(500) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_property_handover_checklist_owner (owner_unit_id, enabled, category, sort_order),
  CONSTRAINT fk_property_handover_checklist_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id)
) ENGINE=InnoDB;

-- ============================================================
-- 8. Automatic reminders and notification center
-- ============================================================

CREATE TABLE notification_rules (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code VARCHAR(80) NOT NULL,
  name VARCHAR(160) NOT NULL,
  event_type VARCHAR(60) NOT NULL COMMENT 'payment_due / rent_due / lease_expiry / reserve_low / document_expiry',
  days_before SMALLINT NOT NULL DEFAULT 0,
  channels JSON NOT NULL,
  recipient_role VARCHAR(50) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_rules_code (code),
  KEY idx_notification_rules_event_enabled (event_type, enabled),
  CONSTRAINT fk_notification_rules_creator FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE notifications (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  rule_id BIGINT UNSIGNED NULL,
  recipient_user_id BIGINT UNSIGNED NULL,
  recipient_owner_id BIGINT UNSIGNED NULL,
  title VARCHAR(200) NOT NULL,
  body VARCHAR(1000) NOT NULL,
  related_type VARCHAR(40) NULL,
  related_id BIGINT UNSIGNED NULL,
  priority VARCHAR(20) NOT NULL DEFAULT 'normal',
  status VARCHAR(20) NOT NULL DEFAULT 'unread',
  read_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notifications_recipient (recipient_user_id, status, created_at),
  KEY idx_notifications_owner (recipient_owner_id, status, created_at),
  KEY idx_notifications_related (related_type, related_id),
  CONSTRAINT fk_notifications_rule FOREIGN KEY (rule_id) REFERENCES notification_rules (id),
  CONSTRAINT fk_notifications_user FOREIGN KEY (recipient_user_id) REFERENCES users (id),
  CONSTRAINT fk_notifications_owner FOREIGN KEY (recipient_owner_id) REFERENCES owners (id)
) ENGINE=InnoDB;

CREATE TABLE notification_deliveries (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  notification_id BIGINT UNSIGNED NOT NULL,
  channel VARCHAR(20) NOT NULL COMMENT 'in_app / email / line',
  destination VARCHAR(255) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  attempt_count TINYINT UNSIGNED NOT NULL DEFAULT 0,
  sent_at DATETIME NULL,
  failed_at DATETIME NULL,
  failure_reason VARCHAR(500) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_deliveries_channel (notification_id, channel),
  KEY idx_notification_deliveries_status (status, sent_at),
  CONSTRAINT fk_notification_deliveries_notification FOREIGN KEY (notification_id) REFERENCES notifications (id)
) ENGINE=InnoDB;

CREATE TABLE notification_subscriptions (
  user_id BIGINT UNSIGNED NOT NULL,
  channel VARCHAR(20) NOT NULL,
  destination VARCHAR(255) NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 0,
  verified_at DATETIME NULL,
  verification_code_hash VARCHAR(255) NULL,
  verification_expires_at DATETIME NULL,
  last_verification_sent_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, channel),
  KEY idx_notification_subscriptions_delivery (channel, enabled, verified_at),
  CONSTRAINT fk_notification_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- ============================================================
-- 9. SQL Account synchronization
-- ============================================================

CREATE TABLE sync_batch_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  batch_id BIGINT UNSIGNED NOT NULL,
  entity_type VARCHAR(40) NOT NULL,
  entity_id BIGINT UNSIGNED NULL,
  operation VARCHAR(20) NOT NULL DEFAULT 'upsert',
  status VARCHAR(20) NOT NULL DEFAULT 'pending',
  external_id VARCHAR(120) NULL,
  error_message VARCHAR(1000) NULL,
  source_payload JSON NULL,
  synced_at DATETIME NULL,
  PRIMARY KEY (id),
  KEY idx_sync_items_batch_status (batch_id, status),
  KEY idx_sync_items_entity (entity_type, entity_id),
  CONSTRAINT fk_sync_items_batch FOREIGN KEY (batch_id) REFERENCES sync_batches (id)
) ENGINE=InnoDB;

-- ============================================================
-- 10. Reports, exports and audit
-- ============================================================

CREATE TABLE report_definitions (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  report_code VARCHAR(60) NOT NULL,
  name VARCHAR(160) NOT NULL,
  report_type VARCHAR(60) NOT NULL COMMENT 'property_payment / rent_collection / income_expense / maintenance / reserve / finance / sync',
  default_format VARCHAR(10) NOT NULL DEFAULT 'PDF',
  default_filters JSON NULL,
  schedule_cron VARCHAR(120) NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_report_definitions_code (report_code),
  KEY idx_report_definitions_type_enabled (report_type, enabled),
  CONSTRAINT fk_report_definitions_creator FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE report_runs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  report_definition_id BIGINT UNSIGNED NULL,
  report_name VARCHAR(160) NOT NULL,
  requested_by BIGINT UNSIGNED NULL,
  date_start DATE NOT NULL,
  date_end DATE NOT NULL,
  project_id BIGINT UNSIGNED NULL,
  output_format VARCHAR(10) NOT NULL,
  filters JSON NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'queued',
  record_count INT UNSIGNED NULL,
  storage_key VARCHAR(500) NULL,
  error_message VARCHAR(1000) NULL,
  started_at DATETIME NULL,
  completed_at DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_report_runs_status_time (status, created_at),
  KEY idx_report_runs_period (date_start, date_end),
  CONSTRAINT fk_report_runs_definition FOREIGN KEY (report_definition_id) REFERENCES report_definitions (id),
  CONSTRAINT fk_report_runs_requester FOREIGN KEY (requested_by) REFERENCES users (id),
  CONSTRAINT fk_report_runs_project FOREIGN KEY (project_id) REFERENCES projects (id),
  CONSTRAINT chk_report_runs_period CHECK (date_end >= date_start)
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  actor_user_id BIGINT UNSIGNED NULL,
  action VARCHAR(80) NOT NULL,
  entity_type VARCHAR(50) NOT NULL,
  entity_id BIGINT UNSIGNED NULL,
  before_data JSON NULL,
  after_data JSON NULL,
  ip_address VARCHAR(45) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_audit_logs_entity (entity_type, entity_id, created_at),
  KEY idx_audit_logs_actor (actor_user_id, created_at),
  CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- ============================================================
-- 11. Read models for dashboard pages
-- ============================================================

CREATE OR REPLACE VIEW v_unit_payment_progress AS
SELECT
  ou.owner_id,
  ou.unit_id,
  u.unit_no,
  p.name AS project_name,
  pc.purchase_price,
  COALESCE(SUM(pi.amount_due), 0) AS scheduled_amount,
  COALESCE(SUM(pi.amount_paid), 0) AS paid_amount,
  GREATEST(COALESCE(SUM(pi.amount_due), 0) - COALESCE(SUM(pi.amount_paid), 0), 0) AS unpaid_amount,
  MAX(pi.due_date) AS last_due_date
FROM owner_units ou
JOIN units u ON u.id = ou.unit_id
JOIN projects p ON p.id = u.project_id
LEFT JOIN purchase_contracts pc ON pc.owner_unit_id = ou.id AND pc.status = 'active'
LEFT JOIN payment_plans pp ON pp.purchase_contract_id = pc.id AND pp.status = 'active'
LEFT JOIN payment_installments pi ON pi.payment_plan_id = pp.id
WHERE ou.status = 'active'
  AND ou.asset_stage = 'PRE_HANDOVER'
GROUP BY ou.owner_id, ou.unit_id, u.unit_no, p.name, pc.purchase_price;

CREATE OR REPLACE VIEW v_rent_collection_status AS
SELECT
  ri.id AS rent_invoice_id,
  l.id AS lease_id,
  l.unit_id,
  l.tenant_id,
  ri.billing_month,
  ri.due_date,
  ri.amount_due,
  ri.amount_paid,
  GREATEST(ri.amount_due - ri.amount_paid, 0) AS unpaid_amount,
  CASE
    WHEN ri.amount_paid >= ri.amount_due THEN 'paid'
    WHEN ri.amount_paid > 0 THEN 'partial'
    WHEN CURRENT_DATE > GREATEST(ri.due_date, DATE_ADD(l.start_date, INTERVAL 7 DAY)) THEN 'overdue'
    ELSE 'unpaid'
  END AS calculated_status
FROM rent_invoices ri
JOIN leases l ON l.id = ri.lease_id
WHERE EXISTS (
  SELECT 1
  FROM owner_units ou
  JOIN owner_unit_services ous ON ous.owner_unit_id = ou.id
  WHERE ou.unit_id = l.unit_id
    AND ou.status = 'active'
    AND ou.asset_stage = 'OPERATING'
    AND ous.service_type = 'RENTAL'
    AND ous.status = 'active'
);

CREATE TABLE property_important_messages (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  subject VARCHAR(200) NOT NULL,
  content VARCHAR(2000) NULL,
  announcement_start_date DATE NOT NULL,
  announcement_end_date DATE NULL,
  importance VARCHAR(20) NOT NULL DEFAULT 'normal',
  is_read TINYINT(1) NOT NULL DEFAULT 0,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_property_important_messages_unit_date (owner_unit_id, announcement_start_date, announcement_end_date),
  KEY idx_property_important_messages_flags (owner_unit_id, importance, is_read),
  CONSTRAINT fk_property_important_messages_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_property_important_messages_creator FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE property_bank_accounts (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  owner_unit_id BIGINT UNSIGNED NOT NULL,
  item_name VARCHAR(120) NOT NULL,
  payment_name VARCHAR(160) NOT NULL,
  account_no VARCHAR(120) NOT NULL,
  remarks VARCHAR(1000) NULL,
  created_by BIGINT UNSIGNED NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_property_bank_accounts_owner_unit (owner_unit_id, id),
  CONSTRAINT fk_property_bank_accounts_owner_unit FOREIGN KEY (owner_unit_id) REFERENCES owner_units (id),
  CONSTRAINT fk_property_bank_accounts_creator FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE OR REPLACE VIEW v_reserve_balances AS
SELECT
  ra.id AS reserve_account_id,
  ou.owner_id,
  ou.unit_id,
  ra.minimum_balance,
  ra.current_balance,
  GREATEST(ra.minimum_balance - ra.current_balance, 0) AS shortage_amount,
  CASE WHEN ra.current_balance < ra.minimum_balance THEN 'low' ELSE 'normal' END AS calculated_status
FROM reserve_accounts ra
JOIN owner_units ou ON ou.id = ra.owner_unit_id
WHERE ra.status = 'active'
  AND ou.status = 'active'
  AND ou.asset_stage = 'OPERATING';

CREATE OR REPLACE VIEW v_finance_review_queue AS
SELECT
  fr.id,
  fr.transaction_no,
  fr.record_type,
  fr.unit_id,
  fr.owner_id,
  fr.tenant_id,
  fr.amount,
  fr.currency,
  fr.transaction_date,
  fr.payment_status,
  fr.confirmation_status,
  fr.sync_status
FROM finance_records fr
WHERE fr.confirmation_status IN ('pending', 'partial', 'rejected')
   OR fr.payment_status IN ('unpaid', 'partial', 'overdue');

CREATE OR REPLACE VIEW v_sync_batch_summary AS
SELECT
  sb.id,
  sb.batch_no,
  sb.source_module,
  sb.trigger_mode,
  sb.status,
  sb.total_count,
  sb.success_count,
  sb.failure_count,
  ROUND(sb.success_count / NULLIF(sb.total_count, 0) * 100, 2) AS success_rate,
  sb.started_at,
  sb.completed_at
FROM sync_batches sb;
