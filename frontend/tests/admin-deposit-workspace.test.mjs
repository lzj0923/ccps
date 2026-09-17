import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const deposit = readFileSync(new URL('../src/components/AdminDepositWorkspace.vue', import.meta.url), 'utf8');
const adminPage = readFileSync(new URL('../src/pages/AdminPage.vue', import.meta.url), 'utf8');
const router = readFileSync(new URL('../src/router.js', import.meta.url), 'utf8');
const viewModel = readFileSync(new URL('../src/composables/dashboardViewModel.js', import.meta.url), 'utf8');
const modules = readFileSync(new URL('../src/data/dashboardData.js', import.meta.url), 'utf8');
const i18n = readFileSync(new URL('../src/i18n/index.js', import.meta.url), 'utf8');
const legacyI18n = readFileSync(new URL('../src/i18n/legacy.generated.js', import.meta.url), 'utf8');
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

test('manages lease deposits locally and routes pending reviews to centralized finance', () => {
  assert.match(deposit, /fetchAdminDepositAccounts/);
  assert.match(deposit, /fetchAdminDepositAccount/);
  assert.match(deposit, /createAdminTenantDepositTransaction/);
  assert.match(deposit, /adminFinanceMode = 'tenant_deposit'/);
  assert.match(deposit, /adminFinanceViewMode = 'pending'/);
  assert.doesNotMatch(deposit, /confirmAdminFinanceReview/);
  assert.match(legacyI18n, /押金账单/);
  assert.match(legacyI18n, /增加押金/);
  assert.match(legacyI18n, /代付租客费用/);
  assert.match(legacyI18n, /登记租客还款/);
  assert.match(legacyI18n, /押金余款返还/);
  assert.match(legacyI18n, /押金余款没收/);
  assert.match(legacyI18n, /业主预备金/);
  assert.match(legacyI18n, /不会进入业主账户报表/);
});

test('supports selecting and safely deleting manual deposit account entries', () => {
  assert.match(deposit, /deleteAdminTenantDepositTransactions/);
  assert.match(deposit, /startDeleteMode/);
  assert.match(deposit, /v-model="selectedTransactionIds"/);
  assert.match(i18n, /depositDeleteSelectAll: \['全选可删除记录'/);
  assert.match(i18n, /depositDeleteConfirm: \['确定删除'/);
  assert.match(deposit, /\['adjustment_credit', 'adjustment_debit', 'tenant_advance', 'tenant_repayment'\]/);
  assert.match(i18n, /depositDeleteLocked: \['系统／财务流水，不可删除'/);
  assert.match(deposit, /window\.confirm/);
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
