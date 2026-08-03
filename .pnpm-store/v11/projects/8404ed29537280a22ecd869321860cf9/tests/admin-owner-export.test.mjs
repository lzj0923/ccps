import assert from 'node:assert/strict';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';

import * as XLSX from 'xlsx';
import { createServer } from 'vite';

test('admin owner export uses the filtered database owner-property rows', async () => {
  const outputDirectory = await mkdtemp(join(tmpdir(), 'ccps-owner-export-'));
  const previousDirectory = process.cwd();
  const vite = await createServer({ root: previousDirectory, server: { middlewareMode: true }, appType: 'custom' });
  process.chdir(outputDirectory);

  try {
    const { default: dashboardActions } = await vite.ssrLoadModule('/src/composables/dashboardActions.js');
    dashboardActions.methods.exportCsv.call({
      currentId: 'adminOwners',
      currentHeaders: ['業主姓名', '手機號', '操作'],
      filteredRows: [['STATIC OWNER', '+60 000']],
      adminOwnerExportHeaders: ['業主姓名', '手機號', '建案／項目', '單位編號'],
      adminOwnerExportRows: [
        ['DATABASE OWNER', '+60 123', 'Database Project', 'A-01'],
        ['DATABASE OWNER', '+60 123', 'Database Project', 'A-02']
      ],
      showToast() {}
    });

    const bytes = await readFile(join(outputDirectory, 'adminOwners-export.xlsx'));
    const workbook = XLSX.read(bytes);
    const rows = XLSX.utils.sheet_to_json(workbook.Sheets[workbook.SheetNames[0]], { header: 1 });

    assert.deepEqual(rows[0], ['業主姓名', '手機號', '建案／項目', '單位編號']);
    assert.equal(rows.length, 3);
    assert.equal(rows[1][0], 'DATABASE OWNER');
    assert.equal(rows[2][3], 'A-02');
    assert.equal(rows.flat().includes('STATIC OWNER'), false);
  } finally {
    process.chdir(previousDirectory);
    await vite.close();
    await rm(outputDirectory, { recursive: true, force: true });
  }
});
