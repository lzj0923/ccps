import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const detailSource = readFileSync(new URL('../src/components/AdminPropertyDetailWorkspace.vue', import.meta.url), 'utf8');
const ownerSource = readFileSync(new URL('../src/components/AdminOwnersWorkspace.vue', import.meta.url), 'utf8');

test('property basic profile exposes seven payment account number fields and persists them', () => {
  for (const field of ['electricity', 'water', 'sewerage', 'gas', 'withholdingTax']) {
    assert.match(detailSource, new RegExp(`paymentAccountNumbers[.[]['"]?${field}`));
  }
  assert.match(detailSource, /电费账户号码/);
  assert.match(detailSource, /水费账户号码/);
  assert.match(detailSource, /排污费账户号码/);
  assert.match(detailSource, /瓦斯费账户号码/);
  assert.match(detailSource, /预扣税账户号码/);
  assert.match(detailSource, /地税账户号码/);
  assert.match(detailSource, /门牌税账户号码/);
  assert.match(detailSource, /accountField:'landTax'/);
  assert.match(detailSource, /accountField:'assessmentTax'/);
  assert.match(detailSource, /profilePayload[\s\S]*paymentAccountNumbers/);
});

test('property export includes the seven payment account number columns', () => {
  assert.match(ownerSource, /电费账户号码/);
  assert.match(ownerSource, /水费账户号码/);
  assert.match(ownerSource, /排污费账户号码/);
  assert.match(ownerSource, /瓦斯费账户号码/);
  assert.match(ownerSource, /预扣税账户号码/);
  assert.match(ownerSource, /地税账户号码/);
  assert.match(ownerSource, /门牌税账户号码/);
  assert.match(ownerSource, /paymentAccountNumbers/);
});
