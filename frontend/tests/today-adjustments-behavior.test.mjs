import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { parse } from '@babel/parser';

// Execute the component methods themselves, rather than matching implementation text.
function member(file, group, name, globals = {}) {
  const source = readFileSync(new URL('../src/components/' + file + '.vue', import.meta.url), 'utf8').match(/<script[^>]*>([\s\S]*?)<\/script>/)[1];
  const ast = parse(source, { sourceType: 'module' });
  const component = ast.program.body.find(node => node.type === 'ExportDefaultDeclaration').declaration;
  const container = component.properties.find(node => node.key.name === group).value;
  const method = container.properties.find(node => node.key.name === name);
  assert.ok(method, group + '.' + name);
  return new Function(...Object.keys(globals), 'return ({' + source.slice(method.start, method.end) + '})[' + JSON.stringify(name) + ']')(...Object.values(globals));
}
const processFile = 'AdminPropertyProcessWorkspace';
const detailFile = 'AdminPropertyDetailWorkspace';
const maintenanceFile = 'AdminMaintenanceWorkspace';

test('property workflow edit reaches basic editor with the selected property', async () => {
  let url;
  const open = member(processFile, 'methods', 'openTarget', { navigate: value => { url = value; } });
  open.call({ selectedProperty: { ownerUnitId: 21, unitId: 8 }, rentalWorkbench: {} }, { type: 'property', tab: 'basic', action: 'edit' });
  const parsed = new URL(url, 'https://test.invalid');
  assert.equal(parsed.pathname, '/admin/properties/8');
  let edits = 0;
  const apply = member(detailFile, 'methods', 'applyProcessRoute', { window: { location: { search: parsed.search } } });
  await apply.call({ menu: [{ key: 'basic' }], selectMenu: async () => {}, startBasicEdit: () => { edits++; } });
  assert.equal(edits, 1);
});

test('missing explicit lease never falls back to a different active lease', async () => {
  let opened = 0;
  const apply = member(detailFile, 'methods', 'applyProcessRoute', { window: { location: { search: '?tab=rentalManagement&rentalTab=lease&leaseId=77' } } });
  await apply.call({ $t: value => value, menu: [], leaseOptions: [{ leaseId: 1, status: 'active' }], openRentalWorkspace: async () => { opened++; } });
  assert.equal(opened, 0);
});

test('mandate edit fetches the exact ID and opens its populated editor', async () => {
  const item = { id: 500, ownerUnitId: 21, status: 'active' };
  const calls = [];
  const apply = member('AdminRentalMandateWorkspace', 'methods', 'applyProcessRoute', {
    window: { location: { search: '?workflow=edit&ownerUnitId=21&mandateId=500' } },
    fetchAdminRentalMandate: async id => { calls.push(id); return item; }
  });
  await apply.call({ select: async row => calls.push(row), openEdit: async row => calls.push(row), $t: x => x, $emit: () => assert.fail('unexpected error') });
  assert.deepEqual(calls, ['500', item, item]);
});

test('mandate shortcut rejects a record belonging to another property', async () => {
  let edited = false; let message = '';
  const apply = member('AdminRentalMandateWorkspace', 'methods', 'applyProcessRoute', {
    window: { location: { search: '?workflow=edit&ownerUnitId=21&mandateId=500' } },
    fetchAdminRentalMandate: async () => ({ id: 500, ownerUnitId: 22 })
  });
  await apply.call({ openEdit: () => { edited = true; }, $t: x => x, $emit: (_, value) => { message = value; } });
  assert.equal(edited, false);
  assert.equal(message, 'rentalMandates.targetMissing');
});

test('monthly unit search uses historical choices, not new-expense options', () => {
  const filter = member(maintenanceFile, 'computed', 'monthlyFilteredUnits');
  const archived = { unitId: 9, unitNo: '08-12', projectName: 'Archived' };
  assert.deepEqual(filter.call({ monthlyUnitSearch: '08-12', monthlyUnits: [archived], options: { units: [] } }), [archived]);
});

test('new expense and maintenance forms keep the operational unit loader', async () => {
  for (const name of ['openExpenseCreate', 'openMaintenanceCreate']) {
    let loads = 0;
    const state = {
      page: {}, $refs: {}, emptyExpenseForm: () => ({}), emptyMaintenanceForm: () => ({}),
      resetNewExpenseAccountForms: () => {}, resetExpenseMasterData: () => {},
      loadOptions: async () => { loads++; }
    };
    await member(maintenanceFile, 'methods', name).call(state);
    assert.equal(loads, 1, name);
    assert.equal(state[name === 'openExpenseCreate' ? 'expenseCreateError' : 'maintenanceCreateError'], '');
    assert.equal(state.monthlyUnitsLoading, undefined);
  }
});

test('switching units ignores late responses and sends the selected month', async () => {
  const pending = []; const calls = [];
  const select = member(maintenanceFile, 'methods', 'selectMonthlyUnit', {
    fetchAdminPropertyCashflows: (...args) => { calls.push(args); return new Promise(resolve => pending.push(resolve)); }
  });
  const state = { monthlyCashflowRequestSerial: 0, monthlyMonth: '2026-08', $t: x => x };
  const a = select.call(state, { unitId: 1, ownerId: 11, ownerUnitId: 21 });
  const b = select.call(state, { unitId: 2, ownerId: 12, ownerUnitId: 22 });
  pending[1]([{ id: 200 }]); await b;
  pending[0]([{ id: 100 }]); await a;
  assert.deepEqual(state.monthlyCashflows, [{ id: 200 }]);
  assert.deepEqual(calls, [[11, 21, '2026-08'], [12, 22, '2026-08']]);
  assert.equal(state.monthlyCashflowLoading, false);
});

test('invalid unit selection cancels an earlier request', async () => {
  let finish;
  const select = member(maintenanceFile, 'methods', 'selectMonthlyUnit', {
    fetchAdminPropertyCashflows: () => new Promise(resolve => { finish = resolve; })
  });
  const state = { monthlyCashflowRequestSerial: 0, $t: x => x };
  const old = select.call(state, { unitId: 1, ownerId: 11, ownerUnitId: 21 });
  await select.call(state, { unitId: 2 });
  finish([{ id: 100 }]); await old;
  assert.deepEqual(state.monthlyCashflows, []);
  assert.equal(state.monthlyCashflowError, 'finance.unitLinkMissing');
  assert.equal(state.monthlyCashflowLoading, false);
});

test('monthly totals deduplicate transactions and exclude pending and refundable deposits', () => {
  const row = (id, amount, extra = {}) => ({ id, financeRecordId: id, occurredOn: '2026-08-15', direction: 'income', category: 'rent', confirmationStatus: 'confirmed', amount, ...extra });
  const state = { monthlyMonth: '2026-08', monthlyCashflows: [
    row(1, 1000), row(1, 1000), row(2, 200, { direction: 'expense', category: 'maintenance' }),
    row(3, 3000, { category: 'deposit' }), row(4, 1000, { occurredOn: '2026-09-01' }), row(5, 500, { confirmationStatus: 'pending' })
  ] };
  for (const name of ['monthlyCashflowRows', 'monthlyConfirmedCashflowRows', 'monthlyIncome', 'monthlyExpense', 'monthlyNet']) state[name] = member(maintenanceFile, 'computed', name).call(state);
  assert.equal(state.monthlyCashflowRows.length, 4);
  assert.equal(state.monthlyIncome, 1000);
  assert.equal(state.monthlyExpense, 200);
  assert.equal(state.monthlyNet, 800);
});
