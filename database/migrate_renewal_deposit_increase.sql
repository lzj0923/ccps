-- Allow one lease to have the original deposit collection plus renewal top-up collections.
SET @has_deposit_lease_index = (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'security_deposit_entries'
     AND index_name = 'idx_security_deposit_lease'
);
SET @add_deposit_lease_index_sql = IF(
  @has_deposit_lease_index = 0,
  'ALTER TABLE security_deposit_entries ADD INDEX idx_security_deposit_lease (lease_id)',
  'SELECT 1'
);
PREPARE add_deposit_lease_index_stmt FROM @add_deposit_lease_index_sql;
EXECUTE add_deposit_lease_index_stmt;
DEALLOCATE PREPARE add_deposit_lease_index_stmt;

SET @has_unique_deposit_lease_index = (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'security_deposit_entries'
     AND index_name = 'uk_security_deposit_lease'
);
SET @drop_unique_deposit_lease_index_sql = IF(
  @has_unique_deposit_lease_index > 0,
  'ALTER TABLE security_deposit_entries DROP INDEX uk_security_deposit_lease',
  'SELECT 1'
);
PREPARE drop_unique_deposit_lease_index_stmt FROM @drop_unique_deposit_lease_index_sql;
EXECUTE drop_unique_deposit_lease_index_stmt;
DEALLOCATE PREPARE drop_unique_deposit_lease_index_stmt;

-- Backfill renewals that were saved before the application started creating the top-up bill.
DROP TEMPORARY TABLE IF EXISTS renewal_deposit_backfill;
CREATE TEMPORARY TABLE renewal_deposit_backfill AS
SELECT l.id AS lease_id,l.lease_no,l.unit_id,l.tenant_id,
       latest_period.start_date AS transaction_date,
       ROUND(l.deposit_amount-COALESCE(SUM(CASE WHEN sde.status<>'rejected' THEN sde.amount ELSE 0 END),0),2) AS amount
  FROM leases l
  JOIN lease_periods latest_period ON latest_period.id=(
       SELECT lp.id FROM lease_periods lp WHERE lp.lease_id=l.id ORDER BY lp.period_no DESC,lp.id DESC LIMIT 1)
  LEFT JOIN security_deposit_entries sde ON sde.lease_id=l.id
 GROUP BY l.id,l.lease_no,l.unit_id,l.tenant_id,l.deposit_amount,latest_period.start_date
HAVING (SELECT COUNT(*) FROM lease_periods period_count WHERE period_count.lease_id=l.id)>1
   AND amount>0;

INSERT INTO finance_records
  (transaction_no,record_type,unit_id,owner_id,tenant_id,amount,currency,transaction_date,
   payment_method,payment_status,confirmation_status,sync_status,created_by)
SELECT CONCAT('DEPOSIT-RENEW-BACKFILL-',backfill.lease_id),'security_deposit',backfill.unit_id,
       (SELECT ou.owner_id FROM owner_units ou WHERE ou.unit_id=backfill.unit_id AND ou.status='active'
         ORDER BY ou.is_primary DESC,ou.id LIMIT 1),
       backfill.tenant_id,backfill.amount,'MYR',backfill.transaction_date,
       'internal_accrual','unpaid','pending','not_synced',NULL
  FROM renewal_deposit_backfill backfill
 WHERE NOT EXISTS (
       SELECT 1 FROM finance_records fr
        WHERE fr.transaction_no=CONCAT('DEPOSIT-RENEW-BACKFILL-',backfill.lease_id));

INSERT INTO security_deposit_entries (lease_id,finance_record_id,amount,status)
SELECT backfill.lease_id,fr.id,backfill.amount,'pending'
  FROM renewal_deposit_backfill backfill
  JOIN finance_records fr
    ON fr.transaction_no=CONCAT('DEPOSIT-RENEW-BACKFILL-',backfill.lease_id)
   AND fr.record_type='security_deposit'
 WHERE NOT EXISTS (
       SELECT 1 FROM security_deposit_entries sde WHERE sde.finance_record_id=fr.id);

INSERT INTO cashflow_entries
  (finance_record_id,unit_id,lease_id,owner_id,tenant_id,direction,category,description,occurred_on,attachment_status)
SELECT fr.id,backfill.unit_id,backfill.lease_id,fr.owner_id,backfill.tenant_id,'income','deposit',
       CONCAT('续约补收租客押金 · ',backfill.lease_no),backfill.transaction_date,'not_required'
  FROM renewal_deposit_backfill backfill
  JOIN finance_records fr
    ON fr.transaction_no=CONCAT('DEPOSIT-RENEW-BACKFILL-',backfill.lease_id)
   AND fr.record_type='security_deposit'
 WHERE NOT EXISTS (
       SELECT 1 FROM cashflow_entries ce WHERE ce.finance_record_id=fr.id AND ce.category='deposit');

DROP TEMPORARY TABLE renewal_deposit_backfill;
