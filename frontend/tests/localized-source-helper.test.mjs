import assert from 'node:assert/strict';
import test from 'node:test';
import { matchLocalizedSource } from './helpers/localizedSource.mjs';

test('source label assertions resolve real catalogue values and preserve structural failures', () => {
  matchLocalizedSource('<b>{{ $t(\'legacy.t_d613d88cb2d8\') }}</b>', /<b>PDF<\/b>/);
  assert.throws(() => matchLocalizedSource('<i>{{ $t(\'legacy.t_d613d88cb2d8\') }}</i>', /<b>PDF<\/b>/));
  assert.throws(() => matchLocalizedSource('<b>{{ $t(\'missing.pdf\') }}</b>', /<b>PDF<\/b>/));
});
