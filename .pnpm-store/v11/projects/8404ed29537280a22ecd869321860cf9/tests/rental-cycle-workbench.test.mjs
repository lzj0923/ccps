import test from 'node:test';
import assert from 'node:assert/strict';
import { buildRentalWorkbench, selectCurrentRentalMandate } from '../src/utils/rentalCycleWorkbench.js';

const property = {
  ownerId: 7,
  ownerUnitId: 11,
  assetStage: 'OPERATING',
  actualHandoverDate: '2026-01-01',
  services: ['RENTAL'],
};

const currentMandate = {
  id: 42,
  ownerUnitId: 11,
  startDate: '2026-07-01',
  status: 'pending_review',
};

test('ignores terminated mandate records when selecting the current rental cycle', () => {
  const oldMandate = { id: 9, ownerUnitId: 11, startDate: '2025-01-01', status: 'terminated' };
  assert.equal(selectCurrentRentalMandate({ property, mandates: [oldMandate] }), null);
  assert.equal(selectCurrentRentalMandate({ property, mandates: [oldMandate, currentMandate] }).id, 42);
});

test('blocks the mandate stage until the current owner authorization is signed', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [currentMandate, { id: 8, ownerUnitId: 11, status: 'expired' }],
    workspace: { profile: { id: 1 }, photos: [{ id: 2 }], handoverChecklist: [{ id: 3 }], handovers: [{ id: 76, contentJson: JSON.stringify({ mandateId: 42 }) }] },
    documents: [
      { mandateId: 42, relationType: 'authorization_draft' },
      { mandateId: 8, relationType: 'signed_contract' },
    ],
  });

  assert.deepEqual(workbench.stages.map(stage => stage.key), [
    'preparation', 'mandateAuthorization', 'leasingSigning', 'moveInCollection',
  ]);
  assert.equal(workbench.stages[1].status, 'blocked');
  assert.equal(workbench.stages[1].blockingReasonKey, 'authorization_signing_required');
  assert.equal(workbench.stages[1].primaryAction.key, 'view_signing_status');
  assert.equal(workbench.currentTask.key, 'mandateAuthorization');
});

test('recognizes the backend signed authorization link', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [currentMandate],
    documents: [{ mandateId: 42, relationType: 'authorization', documentType: 'signed_contract' }],
  });

  assert.notEqual(workbench.stages[1].status, 'blocked');
  assert.notEqual(workbench.stages[1].blockingReasonKey, 'authorization_signing_required');
});

test('does not mark an active mandate complete when its signed authorization is missing', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [{ ...currentMandate, status: 'active' }],
    documents: [{ mandateId: 42, relationType: 'authorization_draft' }],
  });

  assert.equal(workbench.stages[1].status, 'blocked');
  assert.deepEqual(workbench.stages[1].missingItems, ['authorization_signature']);
});

test('does not mark preparation complete from property photos without a report for the current rental', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [currentMandate],
    workspace: { profile: { id: 1 }, photos: [{ id: 2 }], handoverChecklist: [{ id: 3 }] },
  });

  assert.equal(workbench.stages[0].status, 'in_progress');
  assert.deepEqual(workbench.stages[0].missingItems, ['handover_report']);
  assert.equal(workbench.stages[0].blockingReasonKey, 'handover_report_required');
});

test('accepts a newly generated unlinked report created during the current mandate', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [{ ...currentMandate, createdAt: '2026-07-29T09:00:00' }],
    workspace: {
      profile: { id: 1 },
      photos: [{ id: 2 }],
      handoverChecklist: [{ id: 3 }],
      handovers: [{ id: 78, createdAt: '2026-07-30T09:00:00', contentJson: JSON.stringify({}) }],
    },
  });

  assert.equal(workbench.stages[0].status, 'completed');
});

test('completes the current rental cycle after the signed lease move-in handover', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [{ ...currentMandate, status: 'active' }],
    documents: [
      { mandateId: 42, relationType: 'authorization_draft' },
      { mandateId: 42, relationType: 'signed_contract' },
    ],
    workspace: {
      profile: { id: 1 },
      photos: [{ id: 2 }],
      handoverChecklist: [{ id: 3 }],
      handovers: [{ id: 77, completed: false, contentJson: JSON.stringify({ mandateId: 42 }) }],
      handover: { status: 'completed', mandateId: 42 },
      leases: [{ id: 88, rentalMandateId: 42, tenantId: 31, status: 'active' }],
      contracts: [
        { leaseId: 88, contractType: 'O_LEASE_RESERVATION', status: 'completed' },
        { leaseId: 88, contractType: 'L_LEASE', status: 'completed' },
      ],
    },
  });

  assert.ok(workbench.stages.every(stage => stage.status === 'completed'));
  assert.ok(workbench.stages.every(stage => stage.missingItems.length === 0));
  assert.equal(workbench.currentTask, null);
  assert.equal(workbench.includesDailyOperations, true);
});

test('does not let an unpaid first invoice block a completed rental workflow', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [{ ...currentMandate, status: 'active' }],
    documents: [
      { mandateId: 42, relationType: 'authorization_draft' },
      { mandateId: 42, relationType: 'signed_contract' },
    ],
    workspace: {
      profile: { id: 1 },
      photos: [{ id: 2 }],
      handoverChecklist: [{ id: 3 }],
      handovers: [{ id: 77, completed: true, contentJson: JSON.stringify({ mandateId: 42 }) }],
      leases: [{ id: 88, rentalMandateId: 42, tenantId: 31, status: 'active' }],
      contracts: [
        { leaseId: 88, contractType: 'O_LEASE_RESERVATION', status: 'completed' },
        { leaseId: 88, contractType: 'L_LEASE', status: 'completed' },
      ],
    },
    invoices: [{ invoiceId: 91, leaseId: 88, amountDue: 100, amountPaid: 0 }],
    payments: [],
  });

  assert.equal(workbench.stages[3].status, 'completed');
  assert.deepEqual(workbench.stages[3].missingItems, []);
  assert.equal(workbench.currentTask, null);
  assert.equal(workbench.firstInvoice.invoiceId, 91);
});

test('keeps a transferred lease waiting for its own move-out handover report', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [{ ...currentMandate, status: 'active' }],
    workspace: {
      leases: [{ id: 88, rentalMandateId: 42, tenantId: 31, status: 'transferred', endDate: '2026-07-30' }],
      handovers: [{
        id: 77,
        completed: true,
        contentJson: JSON.stringify({ mandateId: 42, handoverType: 'move_in' }),
      }],
    },
  });

  assert.equal(workbench.currentLease, null);
  assert.equal(workbench.leaseClosure.leaseId, 88);
  assert.equal(workbench.leaseClosure.reason, 'transfer');
  assert.equal(workbench.leaseClosure.status, 'awaiting_handover_report');
  assert.deepEqual(workbench.leaseClosure.missingItems, ['lease_end_handover_report']);
});

test('keeps the previous lease closure visible when a transferred replacement lease is active', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [{ ...currentMandate, status: 'active' }],
    workspace: {
      leases: [
        { id: 88, rentalMandateId: 42, tenantId: 31, status: 'transferred', endDate: '2026-07-30', tenantName: '旧租客' },
        { id: 99, rentalMandateId: 42, tenantId: 32, status: 'active', startDate: '2026-07-31', tenantName: '新租客' },
      ],
    },
  });

  assert.equal(workbench.currentLease.id, 99);
  assert.equal(workbench.leaseClosure.leaseId, 88);
  assert.equal(workbench.leaseClosure.tenantName, '旧租客');
  assert.equal(workbench.leaseClosure.status, 'awaiting_handover_report');
});

test('marks the closed lease complete only when its own move-out report exists', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [{ ...currentMandate, status: 'active' }],
    workspace: {
      leases: [{ id: 88, rentalMandateId: 42, status: 'terminated', endDate: '2026-07-30' }],
      handovers: [{ id: 101, completed: true, contentJson: JSON.stringify({ leaseId: 88, handoverType: 'move_out' }) }],
    },
  });

  assert.equal(workbench.leaseClosure.status, 'completed');
  assert.deepEqual(workbench.leaseClosure.missingItems, []);
});

test('keeps the latest closed lease available after its mandate expires', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [{ id: 42, ownerUnitId: 11, startDate: '2025-07-01', endDate: '2026-07-29', status: 'expired' }],
    workspace: {
      leases: [{ id: 88, rentalMandateId: 42, status: 'terminated', endDate: '2026-07-29' }],
    },
  });

  assert.equal(workbench.currentMandate, null);
  assert.equal(workbench.leaseClosure.leaseId, 88);
  assert.equal(workbench.leaseClosure.status, 'awaiting_handover_report');
});

test('completes move-in collection from the lease-scoped invoice API without requiring a proof file', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [{ ...currentMandate, status: 'active' }],
    documents: [
      { mandateId: 42, relationType: 'authorization_draft' },
      { mandateId: 42, relationType: 'signed_contract' },
    ],
    workspace: {
      profile: { id: 1 },
      photos: [{ id: 2 }],
      handoverChecklist: [{ id: 3 }],
      handovers: [{ id: 77, completed: true, contentJson: JSON.stringify({ mandateId: 42 }) }],
      leases: [{ id: 88, rentalMandateId: 42, tenantId: 31, status: 'active' }],
      contracts: [
        { leaseId: 88, contractType: 'O_LEASE_RESERVATION', status: 'completed' },
        { leaseId: 88, contractType: 'L_LEASE', status: 'completed' },
      ],
    },
    invoices: [{ invoiceId: 91, leaseId: 88, amountDue: 100, amountPaid: 100 }],
    payments: [{ invoiceId: 91, leaseId: 88, confirmationStatus: 'confirmed' }],
  });

  assert.equal(workbench.firstInvoice.invoiceId, 91);
  assert.equal(workbench.stages[3].status, 'completed');
  assert.deepEqual(workbench.stages[3].missingItems, []);
});

test('does not complete leasing before the current lease contract is signed', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [{ ...currentMandate, status: 'active' }],
    documents: [{ mandateId: 42, relationType: 'signed_contract' }],
    workspace: {
      leases: [{ id: 88, rentalMandateId: 42, tenantId: 31, status: 'active' }],
      contracts: [{ leaseId: 88, contractType: 'O_LEASE_RESERVATION', status: 'completed' }],
    },
  });

  assert.equal(workbench.stages[2].status, 'in_progress');
  assert.deepEqual(workbench.stages[2].missingItems, ['lease_contract_signature']);
  assert.equal(workbench.stages[3].status, 'pending');
});

test('keeps completed rental preparation editable', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [],
    workspace: { profile: { id: 1 }, photos: [{ id: 2 }], handoverChecklist: [{ id: 3 }] },
  });

  assert.equal(workbench.stages[0].status, 'completed');
  assert.equal(workbench.stages[0].primaryAction.key, 'complete_property_data');
});
