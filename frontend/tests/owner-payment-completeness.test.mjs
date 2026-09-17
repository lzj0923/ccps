import test from 'node:test';
import assert from 'node:assert/strict';
import { hasOwnerPaymentRecords, hasCompleteOwnerPaymentRecords } from '../src/utils/ownerPortfolio.js';
test('无分期和零总价不能推导成已缴清或待缴100%', () => {
  assert.equal(hasOwnerPaymentRecords({ purchasePrice: 12000, paidAmount: 12000, totalInstallmentCount: 0 }), false);
  assert.equal(hasOwnerPaymentRecords({ purchasePrice: 0, totalInstallmentCount: 1 }), false);
  assert.equal(hasOwnerPaymentRecords({ purchasePrice: 12000, totalInstallmentCount: 2 }), true);
});
test('资产组合仅在全部房产都有分期依据时显示整体付款百分比', () => {
  const known = { purchasePrice: 12000, totalInstallmentCount: 1 };
  assert.equal(hasCompleteOwnerPaymentRecords([]), false);
  assert.equal(hasCompleteOwnerPaymentRecords([known]), true);
  assert.equal(hasCompleteOwnerPaymentRecords([known, { purchasePrice: 10000, totalInstallmentCount: 0 }]), false);
});
