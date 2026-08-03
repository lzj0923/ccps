INSERT INTO cashflow_entries
  (finance_record_id,unit_id,owner_id,tenant_id,direction,category,description,occurred_on,attachment_status)
SELECT fr.id,fr.unit_id,fr.owner_id,fr.tenant_id,'income','rent',CONCAT('租金收款 · ',l.lease_no),fr.transaction_date,
       CASE WHEN EXISTS (SELECT 1 FROM document_links dl WHERE dl.entity_type='finance' AND dl.entity_id=fr.id AND dl.relation_type='payment_proof') THEN 'attached' ELSE 'missing' END
FROM finance_records fr
JOIN rent_payments rp ON rp.finance_record_id=fr.id
JOIN rent_invoices ri ON ri.id=rp.rent_invoice_id
JOIN leases l ON l.id=ri.lease_id
WHERE fr.payment_status<>'voided'
  AND NOT EXISTS (SELECT 1 FROM cashflow_entries ce WHERE ce.finance_record_id=fr.id);
