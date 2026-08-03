import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const pageSource = readFileSync(new URL('../src/pages/AdminPage.vue', import.meta.url), 'utf8');
const financeSource = readFileSync(new URL('../src/components/AdminFinanceWorkspace.vue', import.meta.url), 'utf8');

test('finance renders the shared toolbar below its type navigation', () => {
  assert.match(
    pageSource,
    /<ModuleToolbar[^>]+currentId !== 'adminFinance'/,
    'AdminPage should not render the shared toolbar above the finance workspace',
  );

  const navigationEnd = financeSource.indexOf('</div>', financeSource.indexOf('class="admin-finance-navigation"'));
  const toolbar = financeSource.indexOf('<ModuleToolbar');
  assert.ok(toolbar > navigationEnd, 'finance toolbar should render after the finance type navigation');
});
