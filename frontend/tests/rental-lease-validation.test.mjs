import test from 'node:test';
import assert from 'node:assert/strict';

import { rentalLeaseTermError } from '../src/utils/rentalLeaseValidation.js';

test('rejects a lease that extends beyond the current rental mandate', () => {
  assert.equal(
    rentalLeaseTermError({
      startDate: '2026-07-30',
      endDate: '2027-07-31',
      mandateStartDate: '2026-07-30',
      mandateEndDate: '2026-07-31',
    }),
    'lease_end_after_mandate',
  );
});

test('accepts a lease fully covered by the current rental mandate', () => {
  assert.equal(
    rentalLeaseTermError({
      startDate: '2026-07-30',
      endDate: '2026-07-31',
      mandateStartDate: '2026-07-30',
      mandateEndDate: '2026-07-31',
    }),
    '',
  );
});
