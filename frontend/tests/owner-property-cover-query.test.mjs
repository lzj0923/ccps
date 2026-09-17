import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { DatabaseSync } from 'node:sqlite';
test('actual cover query chooses latest owned archive cover and excludes invalid files', () => {
  const java = readFileSync(new URL('../../backend/src/main/java/com/ccps/backend/mapper/OwnerDashboardMapper.java', import.meta.url), 'utf8');
  const select = java.match(/\((SELECT pp\.document_id[\s\S]+?LIMIT 1)\) AS cover_document_id/)[1].replace('ou.id', '$ownerUnitId');
  const db = new DatabaseSync(':memory:');
  try {
    db.exec(`CREATE TABLE property_photos(id INTEGER, document_id INTEGER, owner_unit_id INTEGER, lease_id INTEGER, version_month TEXT, is_cover INTEGER, sort_order INTEGER);
      CREATE TABLE documents(id INTEGER, mime_type TEXT, status TEXT);
      INSERT INTO documents VALUES(1,'image/jpeg','approved'),(2,'image/jpeg','approved'),(3,'image/jpeg','approved'),(4,'image/jpeg','voided'),(5,'application/pdf','approved'),(6,'image/jpeg','approved');
      INSERT INTO property_photos VALUES(1,1,11,NULL,'2026-08',1,0),(2,2,11,NULL,'2026-09',0,1),(3,3,11,NULL,'2026-09',1,0),(4,4,11,NULL,'2026-10',1,0),(5,5,11,NULL,'2026-10',1,0),(6,6,12,NULL,'2026-10',1,0);`);
    assert.equal(db.prepare(select).get({ownerUnitId:11}).document_id, 3);
    assert.equal(db.prepare(select).get({ownerUnitId:12}).document_id, 6);
    assert.equal(db.prepare(select).get({ownerUnitId:99}), undefined);
  } finally { db.close(); }
});
