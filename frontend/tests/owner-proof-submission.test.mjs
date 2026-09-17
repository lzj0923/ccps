import test from 'node:test';
import assert from 'node:assert/strict';
import { ownerProofPayload, validateOwnerProof } from '../src/utils/ownerProofSubmission.js';
const form = { targetId: '11', amount: '50.00', paymentDate: '2026-09-07', bankName: 'Bank', reference: 'REF', payerName: 'Owner', note: '' };
const files = [{ type: 'application/pdf', size: 2048 }];
test('valid proof accepts attachment but never accepts finance status from client', () => {
  assert.equal(validateOwnerProof(form, files, '2026-09-07'), '');
  assert.deepEqual(ownerProofPayload({ ...form, confirmationStatus: 'confirmed', ownerId: 99 }, 'reserve'), { reserveAccountId: '11', amount: '50.00', paymentDate: '2026-09-07', paymentMethod: 'bank_transfer', bankName: 'Bank', reference: 'REF', payerName: 'Owner', note: '' });
  assert.equal(ownerProofPayload(form, 'payment').installmentId, '11');
});
test('invalid amount, future date, missing target and oversized files are rejected', () => {
  for (const amount of ['0', '-1', '1.001', 'NaN', '1000001', '1e3']) assert.equal(validateOwnerProof({ ...form, amount }, files, '2026-09-07'), 'amount');
  assert.equal(validateOwnerProof(form, files, '2026-09-06'), 'date');
  assert.equal(validateOwnerProof({ ...form, targetId: '' }, files, '2026-09-07'), 'target');
  assert.equal(validateOwnerProof(form, files, '2026-09-07', 10), 'amount');
  for (const invalidFiles of [[], Array(5).fill(files[0]), [{ type: 'text/html', size: 10 }], [{ type: 'image/png', size: 11 * 1024 * 1024 }]]) assert.equal(validateOwnerProof(form, invalidFiles, '2026-09-07'), 'files');
});
