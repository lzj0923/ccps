import test from 'node:test';
import assert from 'node:assert/strict';
import { reserveTransactionPositive } from '../src/utils/reserveTransaction.js';

test('migrated opening credit and negative adjustments preserve their direction', () => {
  assert.equal(reserveTransactionPositive('adjustment', '1000.00'), true);
  assert.equal(reserveTransactionPositive('adjustment', '-1000.00'), false);
  assert.equal(reserveTransactionPositive('adjustment', '0.00'), true);
});
test('unsigned debit and transfer amounts retain their business direction', () => {
  for (const type of ['debit', 'transfer_out', 'transfer_reverse_out']) {
    assert.equal(reserveTransactionPositive(type, '300.00'), false);
  }
  for (const type of ['topup', 'transfer_in', 'transfer_reverse_in']) {
    assert.equal(reserveTransactionPositive(type, '300.00'), true);
  }
});
