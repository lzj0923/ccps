import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const deposit = readFileSync(new URL('../src/components/AdminDepositWorkspace.vue', import.meta.url), 'utf8');
const adminPage = readFileSync(new URL('../src/pages/AdminPage.vue', import.meta.url), 'utf8');
const router = readFileSync(new URL('../src/router.js', import.meta.url), 'utf8');
const viewModel = readFileSync(new URL('../src/composables/dashboardViewModel.js', import.meta.url), 'utf8');
const modules = readFileSync(new URL('../src/data/dashboardData.js', import.meta.url), 'utf8');
const i18n = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8');
const process = readFileSync(new URL('../src/components/AdminPropertyProcessWorkspace.vue', import.meta.url), 'utf8');
const tenantDirectory = readFileSync(new URL('../src/components/AdminTenantDirectoryWorkspace.vue', import.meta.url), 'utf8');

test('exposes deposit management as its own rental module', () => {
  assert.match(router, /path: '\/admin\/deposits'.*moduleId: 'adminDeposits'/);
  assert.match(viewModel, /adminRentalSigning', 'adminDeposits', 'adminTenantDirectory/);
  assert.match(modules, /id: "adminDeposits".*name: "押金管理"/);
  assert.match(i18n, /adminDeposits: \['押金管理', '押金管理', 'Deposit Management'\]/);
  assert.match(adminPage, /AdminDepositWorkspace v-else-if="currentId === 'adminDeposits'"/);
  assert.match(adminPage, /import AdminDepositWorkspace/);
});

test('manages lease deposits and transactions inside the standalone workspace', () => {
  assert.match(deposit, /fetchAdminDepositAccounts/);
  assert.match(deposit, /fetchAdminDepositAccount/);
  assert.match(deposit, /createAdminTenantDepositTransaction/);
  assert.match(deposit, /confirmAdminFinanceReview/);
  assert.match(deposit, /押金账单/);
  assert.match(deposit, /增加押金/);
  assert.match(deposit, /代付租客费用/);
  assert.match(deposit, /登记租客还款/);
  assert.match(deposit, /押金余款返还/);
  assert.match(deposit, /押金余款没收/);
  assert.match(deposit, /业主预备金/);
  assert.match(deposit, /不会进入业主账户报表/);
});

test('keeps deposit page styles independent from Vue scoped metadata', () => {
  assert.doesNotMatch(deposit, /<style\s+scoped>/);
  assert.match(deposit, /<style>\s*@scope \(\.deposit-page\)\s*{\s*:scope\s*{/);
});

test('removes deposit management from the system rental workflow', () => {
  assert.doesNotMatch(process, /key:\s*'depositManagement'/);
  assert.doesNotMatch(process, /operationsTab === 'deposit'/);
  assert.doesNotMatch(process, /openOperationsCenter\('deposit'\)/);
  assert.doesNotMatch(tenantDirectory, /createAdminTenantDepositTransaction/);
  assert.doesNotMatch(tenantDirectory, /openDepositDialog/);
});

test('shows the current deposit balance in the tenant directory', () => {
  assert.match(tenantDirectory, /tenantDirectory\.depositBalance/);
  assert.match(tenantDirectory, /money\(row\.currentDepositBalance\)/);
  assert.doesNotMatch(tenantDirectory, /money\(row\.currentDepositAmount\)/);
});
