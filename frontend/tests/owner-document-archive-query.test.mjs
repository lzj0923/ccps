import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { DatabaseSync } from 'node:sqlite';

// Execute the mapper's actual portable SELECTs against isolated records, never the customer DB.
const java = readFileSync(new URL('../../backend/src/main/java/com/ccps/backend/mapper/OwnerDocumentMapper.java', import.meta.url), 'utf8');
function expand(expression) {
  return [...expression.matchAll(/"""([\s\S]*?)"""|\b([A-Z][A-Z_]+)\b/g)].map(match => {
    if (match[1] !== undefined) return match[1];
    const constant = java.match(new RegExp(`String ${match[2]}\\s*=([\\s\\S]*?);`));
    assert.ok(constant, `Missing SQL constant ${match[2]}`);
    return expand(constant[1]);
  }).join('');
}
function query(method) {
  const annotations = [...java.matchAll(/@Select\(([\s\S]*?)\)\s+(?:List<DocumentRow>|DocumentFile)\s+(\w+)\(/g)];
  const annotation = annotations.find(item => item[2] === method);
  assert.ok(annotation, `Missing mapper method ${method}`);
  return expand(annotation[1]).replace(/#\{(\w+)\}/g, '$$$1');
}
function fixture() {
  const db = new DatabaseSync(':memory:');
  db.exec(`
    CREATE TABLE documents(id INTEGER, document_no TEXT, original_name TEXT, document_type TEXT, status TEXT, mime_type TEXT, file_size INTEGER, storage_key TEXT, expires_at TEXT, created_at TEXT, updated_at TEXT, uploaded_by INTEGER);
    CREATE TABLE users(id INTEGER, display_name TEXT);
    CREATE TABLE projects(id INTEGER, name TEXT, city TEXT);
    CREATE TABLE units(id INTEGER, project_id INTEGER, unit_no TEXT);
    CREATE TABLE owners(id INTEGER, user_id INTEGER, status TEXT);
    CREATE TABLE owner_units(id INTEGER, owner_id INTEGER, unit_id INTEGER, status TEXT, is_primary INTEGER);
    CREATE TABLE document_links(document_id INTEGER, entity_type TEXT, entity_id INTEGER);
    CREATE TABLE maintenance_work_orders(id INTEGER, unit_id INTEGER);
    CREATE TABLE finance_records(id INTEGER, unit_id INTEGER, owner_id INTEGER);
    CREATE TABLE cashflow_entries(id INTEGER, unit_id INTEGER, owner_id INTEGER);
    CREATE TABLE leases(id INTEGER, unit_id INTEGER, rental_mandate_id INTEGER);
    CREATE TABLE lease_periods(id INTEGER, lease_id INTEGER);
    CREATE TABLE rental_mandates(id INTEGER, owner_unit_id INTEGER);
    CREATE TABLE electronic_signature_requests(source_document_id INTEGER, signed_document_id INTEGER, status TEXT);
    CREATE TABLE property_photos(document_id INTEGER, owner_unit_id INTEGER);
    CREATE TABLE property_attachments(document_id INTEGER, owner_unit_id INTEGER, enabled INTEGER);
    CREATE TABLE property_handover_reports(document_id INTEGER, owner_unit_id INTEGER);
    CREATE TABLE property_contract_records(id INTEGER, owner_unit_id INTEGER, original_name TEXT, storage_key TEXT, mime_type TEXT, file_size INTEGER, status TEXT, contract_type TEXT, contract_no TEXT, valid_to TEXT, created_at TEXT, updated_at TEXT, created_by INTEGER);
    INSERT INTO users VALUES(99,'Staff');
    INSERT INTO owners VALUES(1,42,'active'),(2,43,'active'),(3,44,'inactive');
    INSERT INTO owner_units VALUES(11,1,101,'active',1),(12,2,102,'active',1),(13,3,103,'active',1),(14,1,104,'inactive',1);
    INSERT INTO projects VALUES(1,'Project A','KL'),(2,'Project B','KL');
    INSERT INTO units VALUES(101,1,'102'),(102,2,'102'),(103,1,'103'),(104,1,'104');
    INSERT INTO documents(id,original_name,document_type,status,storage_key,uploaded_by) VALUES
      (1,'photo.jpg','property_photo','approved','11/photo.jpg',99),
      (2,'property.pdf','property_attachment','approved','11/property.pdf',99),
      (3,'handover.pdf','handover_repair_attachment','approved','11/handover.pdf',99),
      (4,'foreign.jpg','property_photo','approved','12/foreign.jpg',99),
      (5,'disabled.pdf','property_attachment','approved','11/disabled.pdf',99),
      (6,'inactive-owner.jpg','property_photo','approved','13/inactive.jpg',99),
      (7,'inactive-unit.jpg','property_photo','approved','14/inactive.jpg',99),
      (8,'voided.jpg','property_photo','voided','11/voided.jpg',99),
      (9,'legacy.pdf','invoice','approved','legacy.pdf',99);
    INSERT INTO property_photos VALUES(1,11),(4,12),(6,13),(7,14),(8,11);
    INSERT INTO property_attachments VALUES(2,11,1),(5,11,0);
    INSERT INTO property_handover_reports VALUES(3,11);
    INSERT INTO document_links VALUES(9,'unit',101);
    INSERT INTO property_contract_records VALUES(1,11,'mandate.pdf','11/mandate.pdf','application/pdf',10,'signed','M_MANAGEMENT','M-1',NULL,'2026-01-01','2026-01-01',99),
      (2,12,'foreign.pdf','12/foreign.pdf','application/pdf',10,'signed','M_MANAGEMENT','M-2',NULL,'2026-01-01','2026-01-01',99),
      (3,11,'draft.pdf','11/draft.pdf','application/pdf',10,'draft','M_MANAGEMENT','M-3',NULL,'2026-01-01','2026-01-01',99);
  `);
  return db;
}

test('照片、房产通用附件和交接附件不需要额外 document_links 也能列出，并保留房产归属', () => {
  const db = fixture();
  try {
    const rows = db.prepare(query('findDocuments')).all({userId:42});
    assert.deepEqual(rows.map(row=>row.id).sort(),[1,2,3,9]);
    for (const row of rows) assert.equal(row.project_name,'Project A');
  } finally { db.close(); }
});
test('列表与下载使用同样的权限：自有归档可下载，其他业主、停用和作废资料不可下载', () => {
  const db = fixture();
  try {
    const stmt=db.prepare(query('findDocumentFile'));
    for(const documentId of [1,2,3,9]) assert.equal(stmt.get({userId:42,documentId})?.id,documentId);
    for(const documentId of [4,5,6,7,8]) assert.equal(stmt.get({userId:42,documentId}),undefined);
  } finally { db.close(); }
});
test('独立归档的已签合同可查可下载，不泄露其他业主合同或管理员草稿', () => {
  const db = fixture();
  try {
    assert.deepEqual(db.prepare(query('findContractDocuments')).all({userId:42}).map(row=>row.id),[1]);
    const stmt=db.prepare(query('findContractFile'));
    assert.equal(stmt.get({userId:42,documentId:1})?.id,1);
    assert.equal(stmt.get({userId:42,documentId:2}),undefined);
    assert.equal(stmt.get({userId:42,documentId:3}),undefined);
  } finally { db.close(); }
});
