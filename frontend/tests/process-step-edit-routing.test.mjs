import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { parse } from '@babel/parser';

function member(file, group, name, globals = {}) {
  const source = readFileSync(new URL(`../src/components/${file}.vue`, import.meta.url), 'utf8').match(/<script[^>]*>([\s\S]*?)<\/script>/)[1];
  const component = parse(source, { sourceType: 'module' }).program.body.find(n => n.type === 'ExportDefaultDeclaration').declaration;
  const method = component.properties.find(n => n.key.name === group).value.properties.find(n => n.key.name === name);
  assert.ok(method, `${file}.${name}`);
  return new Function(...Object.keys(globals), `return ({${source.slice(method.start, method.end)}})[${JSON.stringify(name)}]`)(...Object.values(globals));
}
const file = 'AdminPropertyProcessWorkspace';
const edit = member(file, 'methods', 'editSelectedJourneyStep');
const enabled = member(file, 'computed', 'canEditSelectedJourneyStep');
const targets = {
  propertySetup: { type: 'property', tab: 'basic', action: 'edit' },
  mandateAuthorization: { type: 'rentalMandate', action: 'edit' },
  tenantSetup: { type: 'tenantDirectory', action: 'edit', tenantId: 18, keyword: 'Selected tenant' },
  leaseSetup: { type: 'property', tab: 'rentalManagement', rentalTab: 'lease', leaseId: 90 },
  billingOperations: 'billing', maintenanceOperations: 'maintenance', leaseClosure: 'lease',
};
for (const [key, expected] of Object.entries(targets)) {
  test(`${key}: edit uses the selected step and exact current record`, () => {
    const calls = [];
    const state = {
      selectedProperty: { unitId: 8, ownerUnitId: 21 }, selectedJourneyStep: { key, status: 'completed' },
      journeyCurrentStep: { key: 'tenantSetup' }, currentTenant: null,
      rentalWorkbench: { currentMandate: { id: 50 }, currentLease: { id: 90, tenantId: 18, tenantName: 'Selected tenant' } },
      openTarget: target => calls.push(target), openOperationsCenter: tab => calls.push(tab),
    };
    state.canEditSelectedJourneyStep = enabled.call(state);
    assert.equal(state.canEditSelectedJourneyStep, true);
    edit.call(state);
    assert.deepEqual(calls, [expected]);
  });
}
test('missing records disable edit without navigating to an unrelated record', () => {
  for (const key of Object.keys(targets).filter(key => key !== 'propertySetup')) {
    const state = { selectedProperty: { unitId: 8 }, selectedJourneyStep: { key }, rentalWorkbench: {}, currentTenant: null,
      openTarget: () => assert.fail(key), openOperationsCenter: () => assert.fail(key) };
    state.canEditSelectedJourneyStep = enabled.call(state);
    assert.equal(state.canEditSelectedJourneyStep, false, key);
    edit.call(state);
  }
});
test('closed lease opens its historical lease editor, never active operations', () => {
  let target;
  const state = { selectedProperty: { unitId: 8 }, selectedJourneyStep: { key: 'leaseClosure' },
    rentalWorkbench: { leaseClosure: { leaseId: 77 } },
    openTarget: value => { target = value; }, openOperationsCenter: () => assert.fail('closed lease must not use current operations') };
  state.canEditSelectedJourneyStep = enabled.call(state);
  edit.call(state);
  assert.equal(target.leaseId, 77);
  assert.equal(target.rentalTab, 'lease');
});
test('navigation preserves distinct physical unit, ownership, mandate and lease IDs', () => {
  const expectedPaths = { property: '/admin/properties/8', rentalMandate: '/admin/rental-mandates', tenantDirectory: '/admin/tenant-directory' };
  for (const [type, expected] of Object.entries(expectedPaths)) {
    let url;
    member(file, 'methods', 'openTarget', { navigate: value => { url = value; } }).call({
      selectedProperty: { unitId: 8, ownerUnitId: 21 }, rentalWorkbench: { currentMandate: { id: 50 }, currentLease: { id: 90 } },
    }, { type, action: 'edit', tenantId: 18, leaseId: 77 });
    const parsed = new URL(url, 'https://test.invalid');
    assert.equal(parsed.pathname, expected);
    for (const [key, value] of Object.entries({ ownerUnitId: '21', unitId: '8', mandateId: '50', leaseId: '77', tenantId: '18', action: 'edit', workflow: 'edit' })) {
      assert.equal(parsed.searchParams.get(key), value);
    }
  }
});
test('operations editors load the selected lease before retrieving workspace data', async () => {
  for (const tab of ['billing', 'maintenance', 'lease']) {
    const calls = [];
    const state = { rentalWorkbench: { currentLease: { leaseId: 90 } } };
    for (const name of ['loadOperationsWorkspace', 'loadOperationsOptions', 'loadOperationsExpenses', 'loadOperationsExpenseReviews']) {
      state[name] = async () => { assert.equal(state.operationsSelectedLeaseId, 90); calls.push(name); };
    }
    await member(file, 'methods', 'openOperationsCenter').call(state, tab);
    assert.equal(state.operationsCenterOpen, true);
    assert.equal(state.operationsTab, tab);
    assert.equal(calls.length, 4);
  }
});
test('lease editor opens the explicitly requested lease even when another active lease is first', async () => {
  const leases = [{ leaseId: 1, status: 'active' }, { leaseId: 77, status: 'expired' }];
  let opened;
  const state = { menu: [], leaseOptions: leases, openRentalWorkspace: async lease => { opened = lease; } };
  await member('AdminPropertyDetailWorkspace', 'methods', 'applyProcessRoute', {
    window: { location: { search: '?tab=rentalManagement&rentalTab=lease&leaseId=77' } },
  }).call(state);
  assert.equal(opened, leases[1]);
  assert.equal(state.rentalActiveTab, 'lease');
});
