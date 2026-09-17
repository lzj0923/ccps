import { matchLocalizedSource } from './helpers/localizedSource.mjs';
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const collection = readFileSync(new URL('../src/components/AdminRentCollectionWorkspace.vue', import.meta.url), 'utf8');
const propertyDetail = readFileSync(new URL('../src/components/AdminPropertyDetailWorkspace.vue', import.meta.url), 'utf8');
const adminReserve = readFileSync(new URL('../src/components/AdminReserveWorkspace.vue', import.meta.url), 'utf8');
const ownerReserve = readFileSync(new URL('../src/components/ReserveDashboard.vue', import.meta.url), 'utf8');

test('rent confirmation captures receipt and posting dates separately', () => {
  matchLocalizedSource(collection, /form\.receivedDate/);
  matchLocalizedSource(collection, /form\.postingDate/);
  matchLocalizedSource(collection, /收款日期/);
  matchLocalizedSource(collection, /入账日期/);
  matchLocalizedSource(collection, /用于会计月度余额核对/);
  matchLocalizedSource(collection, /用于业主账单、前端余额及租期结算/);
});

test('lease payment maintenance preserves both dates', () => {
  matchLocalizedSource(propertyDetail, /rentalPaymentForm\.receivedDate/);
  matchLocalizedSource(propertyDetail, /rentalPaymentForm\.postingDate/);
  matchLocalizedSource(propertyDetail, /row\.receivedDate \|\| row\.paymentDate/);
  matchLocalizedSource(propertyDetail, /row\.postingDate \|\| row\.paymentDate/);
});

test('reserve workspaces show owner-statement and accounting balances', () => {
  for (const source of [adminReserve, ownerReserve]) {
    matchLocalizedSource(source, /业主账单余额/);
    matchLocalizedSource(source, /会计余额/);
    matchLocalizedSource(source, /accountingBalance/);
  }
});

test('monthly reserve reconciliation uses the receipt-date accounting balance', () => {
  matchLocalizedSource(adminReserve, /CCPS 系统总余额（按收款日期）/);
  matchLocalizedSource(adminReserve, /account\.accountingBalance \|\| 0/);
  assert.doesNotMatch(adminReserve, /reconciliationSystemBalance\(\) \{ return this\.accounts\.reduce\(\(sum, account\) => sum \+ Number\(account\.currentBalance/);
});

test('reserve account table keeps status and removes the redundant pending column', () => {
  matchLocalizedSource(adminReserve, /legacy\.t_45293595eae3/);
  assert.doesNotMatch(adminReserve, /<th>\{\{ \$t\('legacy\.t_ecfe4930d2c5'\) \}\}<\/th>/);
  assert.doesNotMatch(adminReserve, /<td><span v-if="account\.pendingTopupCount"/);
  matchLocalizedSource(adminReserve, /colspan="11"/);
});
