import test from 'node:test';
import assert from 'node:assert/strict';

import { rentalFileLifecycle } from '../src/utils/rentalFileStatus.js';

test('labels original signing documents as generated even when their workflow is signed', () => {
  assert.equal(rentalFileLifecycle({
    kind: 'mandate', documentType: 'management_authorization_draft', status: 'signed',
  }), 'generated');
});

test('labels the current signed result as signed', () => {
  assert.equal(rentalFileLifecycle({
    kind: 'mandate', documentType: 'signed_contract', status: 'approved',
  }), 'signed');
  assert.equal(rentalFileLifecycle({
    kind: 'contract', contractType: 'L_LEASE', status: 'completed', signedFile: true,
  }), 'signed');
});

test('labels replaced signed results as archived', () => {
  assert.equal(rentalFileLifecycle({
    kind: 'mandate', documentType: 'signed_contract', status: 'superseded',
  }), 'archived');
});
