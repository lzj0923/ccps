import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const workspace = readFileSync(new URL('../src/components/AdminMaintenanceWorkspace.vue', import.meta.url), 'utf8');
const api = readFileSync(new URL('../src/services/propertyApi.js', import.meta.url), 'utf8');
const optionsDto = readFileSync(new URL('../../backend/src/main/java/com/ccps/backend/dto/AdminMaintenanceOptionsResponse.java', import.meta.url), 'utf8');
const mapper = readFileSync(new URL('../../backend/src/main/java/com/ccps/backend/mapper/AdminMaintenanceMapper.java', import.meta.url), 'utf8');
const expenseDto = readFileSync(new URL('../../backend/src/main/java/com/ccps/backend/dto/AdminExpenseCreateRequest.java', import.meta.url), 'utf8');
const maintenanceDto = readFileSync(new URL('../../backend/src/main/java/com/ccps/backend/dto/AdminMaintenanceCreateRequest.java', import.meta.url), 'utf8');
const migration = readFileSync(new URL('../../database/migrate_payment_report_links.sql', import.meta.url), 'utf8');

test('expense form chooses one existing or new account profile after payer type', () => {
  for (const model of ['payerType', 'accountProfileChoice', 'feeAccountChoice']) assert.match(workspace, new RegExp(`v-model="expenseForm\\.${model}"`));
  assert.match(workspace, /value="owner"[^>]*>业主/);
  assert.match(workspace, /value="management"[^>]*>管理层/);
  assert.match(workspace, /value="other"[^>]*>其他/);
  assert.match(workspace, /value="__new__"[^>]*>新建账户/);
  assert.doesNotMatch(workspace, /自己输入/);
  for (const obsoleteModel of ['payerNameChoice', 'bankNameChoice', 'paymentAccountChoice']) assert.doesNotMatch(workspace, new RegExp(`expenseForm\\.${obsoleteModel}`));
  assert.match(workspace, /<section class="expense-linked-fields expense-bank-fields wide">[\s\S]*?<strong>银行信息<\/strong>[\s\S]*?<\/section>\s*<section class="expense-linked-fields expense-fee-account-fields wide">[\s\S]*?<strong>费用账户号码<\/strong>/);
  assert.doesNotMatch(workspace, /v-if="expenseForm\.accountProfileChoice">费用账户号码/);
  assert.match(workspace, /value="__new__"[^>]*>新建费用账户/);
  assert.match(workspace, /feeAccountChoice === '__new__'[\s\S]*费用账户类型[\s\S]*新费用账户号码/);
  assert.match(workspace, /expense-account-summary/);
  const payerTypeHandler = workspace.match(/onExpensePayerTypeChange\(\) \{([\s\S]*?)\n    \},/)?.[1] || '';
  const bankChoiceHandler = workspace.match(/applyExpenseAccountProfileChoice\(\) \{([\s\S]*?)\n    \},/)?.[1] || '';
  assert.doesNotMatch(payerTypeHandler, /feeAccount/);
  assert.doesNotMatch(bankChoiceHandler, /feeAccount/);
});

test('maintenance form separates bank and fee accounts and syncs new profiles', () => {
  for (const model of ['payerType', 'accountProfileChoice', 'feeAccountChoice']) assert.match(workspace, new RegExp(`v-model="maintenanceCreateForm\\.${model}"`));
  assert.match(workspace, /<section class="expense-linked-fields maintenance-bank-fields wide">[\s\S]*?<strong>银行信息<\/strong>[\s\S]*?<\/section>\s*<section class="expense-linked-fields maintenance-fee-account-fields wide">[\s\S]*?<strong>费用账户号码<\/strong>/);
  assert.doesNotMatch(workspace, /<label>付款方<input v-model\.trim="maintenanceCreateForm\.payerName"/);
  assert.match(workspace, /async loadMaintenanceMasterData\(/);
  assert.match(workspace, /validateMaintenanceLinkedFields\(/);
  assert.match(workspace, /async syncMaintenanceMasterData\(/);
  assert.match(workspace, /await this\.syncMaintenanceMasterData\(\)/);
});

test('new owner and management accounts use their property-detail field sets', () => {
  assert.match(workspace, /payerType === 'owner'[\s\S]*ownerBankForm\.itemName[\s\S]*ownerBankForm\.paymentName[\s\S]*ownerBankForm\.accountNo[\s\S]*ownerBankForm\.branchCode[\s\S]*ownerBankForm\.bankAddress[\s\S]*ownerBankForm\.swiftCode[\s\S]*ownerBankForm\.transferLimit[\s\S]*ownerBankForm\.overseasBank[\s\S]*ownerBankForm\.overseasTransferFee[\s\S]*ownerBankForm\.remarks/);
  assert.match(workspace, /payerType === 'management'[\s\S]*managementBankForm\.managementName[\s\S]*managementBankForm\.purpose[\s\S]*managementBankForm\.bankName[\s\S]*managementBankForm\.accountNo[\s\S]*managementBankForm\.accountName[\s\S]*managementBankForm\.branchOrSwift[\s\S]*managementBankForm\.remarks/);
});

test('expense form loads property master data and writes custom owner or management data back', () => {
  for (const apiName of ['fetchAdminPropertyBasicProfile', 'saveAdminPropertyBasicProfile', 'fetchAdminPropertyBankAccounts', 'createAdminPropertyBankAccount']) {
    assert.match(api, new RegExp(`export function ${apiName}`));
    assert.match(workspace, new RegExp(apiName));
  }
  assert.match(workspace, /async loadExpenseMasterData\(/);
  assert.match(workspace, /async syncExpenseMasterData\(/);
  assert.match(workspace, /paymentAccountNumbers/);
  assert.match(workspace, /\[form\.feeAccountKey\]: form\.feeAccountNo\.trim\(\)/);
  assert.match(workspace, /managementBankAccounts/);
  assert.match(workspace, /expenseCreatesMasterAccount/);
  assert.match(workspace, /payerType !== 'other'/);
});

test('maintenance unit options expose ownerUnitId for linked property APIs', () => {
  assert.match(optionsDto, /Long unitId, Long ownerUnitId, Long ownerId, String ownerName, String tenantName/);
  assert.match(mapper, /ou\.id AS owner_unit_id/);
  assert.match(mapper, /AS tenant_name/);
});

test('selected fee accounts persist on expenses and maintenance snapshots', () => {
  for (const dto of [expenseDto, maintenanceDto]) {
    assert.match(dto, /String feeAccountKey/);
    assert.match(dto, /String feeAccountNo/);
  }
  assert.match(mapper, /INSERT INTO payment_receipts[\s\S]*fee_account_type, fee_account_no/);
  assert.match(mapper, /INSERT INTO maintenance_work_orders[\s\S]*fee_account_type, fee_account_no/);
  assert.match(migration, /ALTER TABLE payment_receipts[\s\S]*fee_account_type[\s\S]*fee_account_no/);
  assert.match(migration, /ALTER TABLE maintenance_work_orders[\s\S]*payer_name[\s\S]*payment_account_no[\s\S]*fee_account_type[\s\S]*fee_account_no/);
  assert.match(workspace, /openExpenseEdit\(row\)[\s\S]*feeAccountKey: row\.feeAccountKey[\s\S]*feeAccountNo: row\.feeAccountNo/);
  assert.match(workspace, /openMaintenanceEdit\(row\)[\s\S]*feeAccountKey: row\.feeAccountKey[\s\S]*feeAccountNo: row\.feeAccountNo/);
});
