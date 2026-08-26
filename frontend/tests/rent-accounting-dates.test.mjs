import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const collection = readFileSync(new URL('../src/components/AdminRentCollectionWorkspace.vue', import.meta.url), 'utf8');
const propertyDetail = readFileSync(new URL('../src/components/AdminPropertyDetailWorkspace.vue', import.meta.url), 'utf8');
const adminReserve = readFileSync(new URL('../src/components/AdminReserveWorkspace.vue', import.meta.url), 'utf8');
const ownerReserve = readFileSync(new URL('../src/components/ReserveDashboard.vue', import.meta.url), 'utf8');

test('rent confirmation captures receipt and posting dates separately', () => {
  assert.match(collection, /form\.receivedDate/);
  assert.match(collection, /form\.postingDate/);
  assert.match(collection, /收款日期/);
  assert.match(collection, /入账日期/);
  assert.match(collection, /用于会计月度余额核对/);
  assert.match(collection, /用于业主账单、前端余额及租期结算/);
});

test('lease payment maintenance preserves both dates', () => {
  assert.match(propertyDetail, /rentalPaymentForm\.receivedDate/);
  assert.match(propertyDetail, /rentalPaymentForm\.postingDate/);
  assert.match(propertyDetail, /row\.receivedDate \|\| row\.paymentDate/);
  assert.match(propertyDetail, /row\.postingDate \|\| row\.paymentDate/);
});

test('reserve workspaces show owner-statement and accounting balances', () => {
  for (const source of [adminReserve, ownerReserve]) {
    assert.match(source, /业主账单余额/);
    assert.match(source, /会计余额/);
    assert.match(source, /accountingBalance/);
  }
});

test('monthly reserve reconciliation uses the posting-date system balance', () => {
  assert.match(adminReserve, /CCPS 系统总余额（按入账日期）/);
  assert.match(adminReserve, /account\.currentBalance \|\| 0/);
  assert.doesNotMatch(adminReserve, /reconciliationSystemBalance\(\) \{ return this\.accounts\.reduce\(\(sum, account\) => sum \+ Number\(account\.accountingBalance/);
});

test('reserve account table keeps status and removes the redundant pending column', () => {
  assert.match(adminReserve, /legacy\.t_45293595eae3/);
  assert.doesNotMatch(adminReserve, /<th>\{\{ \$t\('legacy\.t_ecfe4930d2c5'\) \}\}<\/th>/);
  assert.doesNotMatch(adminReserve, /<td><span v-if="account\.pendingTopupCount"/);
  assert.match(adminReserve, /colspan="11"/);
});
