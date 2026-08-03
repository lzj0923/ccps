import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';

const utilityPath = path.resolve('frontend/src/utils/generatedFilesByStage.js');

test('filters generated files to the selected stage and current rental cycle', async () => {
  assert.ok(fs.existsSync(utilityPath), 'stage file filter utility should exist');
  const { filterGeneratedFilesByStage } = await import('../src/utils/generatedFilesByStage.js');
  const files = [
    { key: 'old-mandate', kind: 'mandate', mandateId: 8 },
    { key: 'current-mandate', kind: 'mandate', mandateId: 42 },
    { key: 'current-handover', kind: 'handover', mandateId: 42 },
    { key: 'old-handover', kind: 'handover', mandateId: 8 },
    { key: 'current-otr', kind: 'contract', leaseId: 88 },
    { key: 'old-otr', kind: 'contract', leaseId: 77 },
    { key: 'current-receipt', kind: 'receipt', leaseId: 88 },
  ];

  assert.deepEqual(
    filterGeneratedFilesByStage(files, 'mandateAuthorization', { mandateId: 42, leaseId: 88 }).map(file => file.key),
    ['current-mandate'],
  );
  assert.deepEqual(
    filterGeneratedFilesByStage(files, 'leasingSigning', { mandateId: 42, leaseId: 88 }).map(file => file.key),
    ['current-otr'],
  );
  assert.deepEqual(
    filterGeneratedFilesByStage(files, 'moveInCollection', { mandateId: 42, leaseId: 88 }).map(file => file.key),
    ['current-handover'],
  );
});
