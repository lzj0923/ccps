import { matchLocalizedSource } from './helpers/localizedSource.mjs';
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const detailSource = readFileSync(new URL('../src/components/AdminPropertyDetailWorkspace.vue', import.meta.url), 'utf8');
const ownerSource = readFileSync(new URL('../src/components/AdminOwnersWorkspace.vue', import.meta.url), 'utf8');

test('property basic profile exposes seven payment account number fields and persists them', () => {
  for (const field of ['electricity', 'water', 'sewerage', 'gas', 'withholdingTax']) {
    matchLocalizedSource(detailSource, new RegExp(`paymentAccountNumbers[.[]['"]?${field}`));
  }
  matchLocalizedSource(detailSource, /电费账户号码/);
  matchLocalizedSource(detailSource, /水费账户号码/);
  matchLocalizedSource(detailSource, /排污费账户号码/);
  matchLocalizedSource(detailSource, /瓦斯费账户号码/);
  matchLocalizedSource(detailSource, /预扣税账户号码/);
  matchLocalizedSource(detailSource, /地税账户号码/);
  matchLocalizedSource(detailSource, /门牌税账户号码/);
  matchLocalizedSource(detailSource, /accountField:'landTax'/);
  matchLocalizedSource(detailSource, /accountField:'assessmentTax'/);
  matchLocalizedSource(detailSource, /profilePayload[\s\S]*paymentAccountNumbers/);
});

test('property export includes the seven payment account number columns', () => {
  matchLocalizedSource(ownerSource, /电费账户号码/);
  matchLocalizedSource(ownerSource, /水费账户号码/);
  matchLocalizedSource(ownerSource, /排污费账户号码/);
  matchLocalizedSource(ownerSource, /瓦斯费账户号码/);
  matchLocalizedSource(ownerSource, /预扣税账户号码/);
  matchLocalizedSource(ownerSource, /地税账户号码/);
  matchLocalizedSource(ownerSource, /门牌税账户号码/);
  matchLocalizedSource(ownerSource, /paymentAccountNumbers/);
});
