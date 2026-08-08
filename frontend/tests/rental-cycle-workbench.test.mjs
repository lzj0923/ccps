import test from 'node:test';
import assert from 'node:assert/strict';
import { buildRentalWorkbench, reportBelongsToMandate, selectCurrentRentalMandate } from '../src/utils/rentalCycleWorkbench.js';

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
  status: 'active',
};

test('ignores terminated mandate records when selecting the current rental cycle', () => {
  const oldMandate = { id: 9, ownerUnitId: 11, startDate: '2025-01-01', status: 'terminated' };
  assert.equal(selectCurrentRentalMandate({ property, mandates: [oldMandate] }), null);
  assert.equal(selectCurrentRentalMandate({ property, mandates: [oldMandate, currentMandate] }).id, 42);
});

test('keeps legacy handover reports inside their rental cycle date boundary', () => {
  const oldMandate = { id: 9, createdAt: '2025-01-01T00:00:00' };
  const nextMandate = { id: 42, createdAt: '2026-01-01T00:00:00' };
  assert.equal(reportBelongsToMandate({ createdAt: '2025-06-01T00:00:00', contentJson: '{}' }, oldMandate, nextMandate), true);
  assert.equal(reportBelongsToMandate({ createdAt: '2026-02-01T00:00:00', contentJson: '{}' }, oldMandate, nextMandate), false);
  assert.equal(reportBelongsToMandate({ createdAt: '2026-02-01T00:00:00', mandateId: 9 }, oldMandate, nextMandate), true);
});

test('keeps authorization files outside the system rental workflow', () => {
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
    'propertySetup', 'mandateAuthorization', 'leasingSigning', 'moveInCollection', 'rentalOperations', 'leaseClosure',
  ]);
  assert.equal(workbench.stages[1].status, 'completed');
  assert.equal(workbench.stages[1].blockingReasonKey, null);
  assert.deepEqual(workbench.stages[1].missingItems, []);
  assert.equal(workbench.stages[1].primaryAction, null);
  assert.equal(workbench.stages.some(stage => stage.primaryAction?.key === 'review_mandate'), false);
  assert.equal(workbench.currentTask.key, 'leasingSigning');
});

test('still exposes signed authorization state to the separate signing workspace', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [currentMandate],
    documents: [{ mandateId: 42, relationType: 'authorization', documentType: 'signed_contract' }],
  });

  assert.equal(workbench.authorizationSigning.status, 'signed');
  assert.equal(workbench.stages[1].blockingReasonKey, null);
});

test('marks an active mandate complete without requiring authorization files', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [{ ...currentMandate, status: 'active' }],
    documents: [{ mandateId: 42, relationType: 'authorization_draft' }],
  });

  assert.equal(workbench.stages[1].status, 'completed');
  assert.deepEqual(workbench.stages[1].missingItems, []);
});

test('does not add a mandate review requirement', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [currentMandate],
    workspace: { profile: { id: 1 }, photos: [{ id: 2 }], handoverChecklist: [{ id: 3 }] },
  });

  assert.equal(workbench.stages[1].status, 'completed');
  assert.deepEqual(workbench.stages[1].missingItems, []);
  assert.equal(workbench.stages[1].blockingReasonKey, null);
});

test('does not treat a generated report file as system handover completion', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [{ ...currentMandate, status: 'active', createdAt: '2026-07-29T09:00:00' }],
    workspace: {
      profile: { id: 1 },
      photos: [{ id: 2 }],
      handoverChecklist: [{ id: 3 }],
      handovers: [{ id: 78, createdAt: '2026-07-30T09:00:00', contentJson: JSON.stringify({}) }],
      leases: [{ id: 88, rentalMandateId: 42, tenantId: 31, status: 'active' }],
    },
  });

  assert.equal(workbench.stages[3].status, 'in_progress');
  assert.deepEqual(workbench.stages[3].missingItems, ['handover']);
});

test('enters rental operations after the system move-in handover', () => {
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

  assert.ok(workbench.stages.slice(0, 4).every(stage => stage.status === 'completed'));
  assert.equal(workbench.stages[4].status, 'in_progress');
  assert.equal(workbench.stages[4].primaryAction.key, 'open_operations_center');
  assert.equal(workbench.stages[5].status, 'pending');
  assert.equal(workbench.currentTask.key, 'rentalOperations');
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
      handover: { status: 'completed', mandateId: 42 },
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
  assert.equal(workbench.currentTask.key, 'rentalOperations');
  assert.equal(workbench.firstInvoice.invoiceId, 91);
});

test('does not let a missing move-out report block a transferred lease closure', () => {
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
  assert.equal(workbench.leaseClosure.status, 'completed');
  assert.equal(workbench.leaseClosure.handoverReportReady, false);
  assert.deepEqual(workbench.leaseClosure.missingItems, []);
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
  assert.equal(workbench.leaseClosure.status, 'completed');
  assert.equal(workbench.leaseClosure.handoverReportReady, false);
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
  assert.equal(workbench.leaseClosure.handoverReportReady, true);
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
  assert.equal(workbench.leaseClosure.status, 'completed');
  assert.equal(workbench.leaseClosure.handoverReportReady, false);
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
      handover: { status: 'completed', mandateId: 42 },
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

test('does not let OTR or lease contract files block the system rental workflow', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [{ ...currentMandate, status: 'active' }],
    documents: [{ mandateId: 42, relationType: 'signed_contract' }],
    workspace: {
      leases: [{ id: 88, rentalMandateId: 42, tenantId: 31, status: 'active' }],
      contracts: [{ leaseId: 88, contractType: 'O_LEASE_RESERVATION', status: 'completed' }],
    },
  });

  assert.equal(workbench.stages[2].status, 'completed');
  assert.deepEqual(workbench.stages[2].missingItems, []);
  assert.equal(workbench.stages[3].status, 'in_progress');
  assert.deepEqual(workbench.stages[3].missingItems, ['handover']);
});

test('starts the system workflow by creating a rental mandate', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [],
    workspace: { profile: { id: 1 }, photos: [{ id: 2 }], handoverChecklist: [{ id: 3 }] },
  });

  assert.equal(workbench.stages[0].status, 'completed');
  assert.equal(workbench.stages[1].status, 'in_progress');
  assert.deepEqual(workbench.stages[1].missingItems, ['rental_mandate']);
  assert.equal(workbench.stages[1].primaryAction.key, 'create_mandate');
});

test('starts a pre-handover property with property setup', () => {
  const workbench = buildRentalWorkbench({ property: { ...property, assetStage: 'PRE_HANDOVER' } });

  assert.equal(workbench.stages[0].key, 'propertySetup');
  assert.equal(workbench.stages[0].status, 'in_progress');
  assert.equal(workbench.stages[0].primaryAction.key, 'complete_handover');
  assert.equal(workbench.currentTask.key, 'propertySetup');
});

test('completes the full system journey after the lease is closed', () => {
  const workbench = buildRentalWorkbench({
    property,
    mandates: [{ ...currentMandate, status: 'active' }],
    workspace: {
      leases: [{ id: 88, rentalMandateId: 42, tenantId: 31, status: 'terminated', endDate: '2026-07-30' }],
    },
  });

  assert.ok(workbench.stages.every(stage => stage.status === 'completed'));
  assert.equal(workbench.currentTask, null);
});
