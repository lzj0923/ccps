import assert from 'node:assert/strict';
import fs from 'node:fs';
import { buildRentalWorkbench } from '../frontend/src/utils/rentalCycleWorkbench.js';

const baseInput = {
  property: { ownerUnitId: 21 },
  mandates: [{ id: 7, ownerUnitId: 21, mandateNo: 'RM-7', status: 'draft', startDate: '2026-01-01' }],
  workspace: {},
};

const notStartedDocuments = [{ id: 31, mandateId: 7, relationType: 'authorization_draft' }];
const notStarted = buildRentalWorkbench({
  ...baseInput,
  documents: notStartedDocuments,
});
assert.equal(notStarted.authorizationSigning.status, 'not_started');
assert.equal(notStarted.stages[0].primaryAction.key, 'start_authorization_signing');

const pending = buildRentalWorkbench({
  ...baseInput,
  documents: [{
    id: 31,
    mandateId: 7,
    relationType: 'authorization_draft',
    signatureStatus: 'pending',
    signatureSignerName: '吕志杰',
    signatureSignerEmail: 'owner@example.com',
    signatureExpiresAt: '2026-08-14T12:00:00',
  }],
});
assert.equal(pending.authorizationSigning.status, 'pending');
assert.equal(pending.stages[0].primaryAction.key, 'view_signing_status');
assert.equal(pending.authorizationSigning.signerEmail, 'owner@example.com');

const component = fs.readFileSync('frontend/src/components/AdminPropertyProcessWorkspace.vue', 'utf8');
const mapper = fs.readFileSync('backend/src/main/java/com/ccps/backend/mapper/AdminRentalMandateDocumentMapper.java', 'utf8');
assert.match(component, /activeAction === 'view_signing_status'[\s\S]*rentalWorkbench\.authorizationSigning\.signerName/);
assert.match(component, /v-if="activeAction !== 'view_signing_status'" type="submit"/);
assert.match(mapper, /signature_request\.status AS signature_status/);
assert.match(mapper, /signature_request\.signer_email AS signature_signer_email/);

console.log('PASS: 授权签约流程能区分未发起与已发起待签署');
