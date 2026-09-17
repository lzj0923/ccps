import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');

const processWorkspace = read('../src/components/AdminPropertyProcessWorkspace.vue');
const collectionWorkspace = read('../src/components/AdminRentCollectionWorkspace.vue');
const dashboardState = read('../src/composables/dashboardState.js');
const controller = read('../../backend/src/main/java/com/ccps/backend/controller/AdminTenancyController.java');
const service = read('../../backend/src/main/java/com/ccps/backend/service/AdminTenancyService.java');
const mapper = read('../../backend/src/main/java/com/ccps/backend/mapper/AdminTenancyMapper.java');

test('daily operations passes the current invoice to finance confirmation', () => {
  assert.match(dashboardState, /adminFinanceTargetInvoiceId:\s*null/);
  assert.match(processWorkspace, /goToCentralFinance\('rent',\s*this\.operationsInvoice\?\.(?:invoiceId|id)/);
  assert.match(processWorkspace, /adminFinanceTargetInvoiceId\s*=\s*targetInvoiceId/);
});

test('rent confirmation loads, selects, and opens the requested invoice', () => {
  assert.match(collectionWorkspace, /targetInvoiceId\(\)\s*\{\s*return this\.page\.adminFinanceTargetInvoiceId/);
  assert.match(collectionWorkspace, /invoiceId:\s*this\.targetInvoiceId/);
  assert.match(collectionWorkspace, /this\.selectedId\s*=\s*targetRow\.invoiceId/);
  assert.match(collectionWorkspace, /this\.page\.adminFinanceTargetInvoiceId\s*=\s*null/);
  assert.match(collectionWorkspace, /this\.\$nextTick\(this\.openConfirm\)/);
});

test('rent collection API supports an exact invoice filter', () => {
  assert.match(controller, /@RequestParam\(required = false\) Long invoiceId/);
  assert.match(service, /findRentCollections\([^)]*Long invoiceId/s);
  assert.match(mapper, /<if test=\\"invoiceId != null\\">AND ri\.id=#\{invoiceId\}<\/if>/);
});
