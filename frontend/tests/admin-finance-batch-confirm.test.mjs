import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = file => readFileSync(new URL(`../src/${file}`, import.meta.url), 'utf8');

test('all finance confirmation workspaces expose batch confirmation', () => {
  const property = read('components/AdminFinanceWorkspace.vue');
  const expense = read('components/AdminExpenseFinanceWorkspace.vue');
  const reserve = read('components/AdminReserveFinanceWorkspace.vue');
  const rent = read('components/AdminRentCollectionWorkspace.vue');

  assert.match(property, /batchConfirmAdminFinanceReviews/);
  assert.match(reserve, /batchConfirmAdminFinanceReviews/);
  assert.match(expense, /selectedIds[\s\S]*batchConfirmAdminFinanceReviews/);
  assert.match(rent, /selectedIds[\s\S]*batchConfirmAdminRentCollections/);
});

test('rent batch confirmation is connected to the backend endpoint', () => {
  const api = read('services/propertyApi.js');
  const toolbar = read('components/ModuleToolbar.vue');
  const rent = read('components/AdminRentCollectionWorkspace.vue');
  assert.match(api, /batchConfirmAdminRentCollections/);
  assert.match(api, /finance\/rent\/invoices\/batch-confirm/);
  assert.doesNotMatch(toolbar, /adminFinanceMode === 'rent' \|\| this\.page\.adminFinanceViewMode/);
  assert.match(rent, /adminFinanceBatchNonce[\s\S]*openBatchConfirm/);
});

test('rent list primary confirmation action opens the one-click batch dialog', () => {
  const rent = read('components/AdminRentCollectionWorkspace.vue');
  assert.match(rent, /rent-list-batch-trigger[^>]*@click="openBatchConfirm"/);
  assert.doesNotMatch(rent, /class="primary-btn"[^>]*@click="openConfirm"/);
  assert.match(rent, /selected\.length\?selected:this\.rows\.filter/);
});
