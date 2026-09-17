import test from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';

const root = new URL('../../', import.meta.url);
const backendTemplateDir = new URL('backend/src/main/resources/contract-templates/', root);
const controller = readFileSync(new URL('backend/src/main/java/com/ccps/backend/controller/AdminContractTemplateController.java', root), 'utf8');
const tenancy = readFileSync(new URL('frontend/src/components/AdminTenancyWorkspace.vue', root), 'utf8');
const mandate = readFileSync(new URL('frontend/src/components/AdminRentalMandateWorkspace.vue', root), 'utf8');
const signing = readFileSync(new URL('frontend/src/components/AdminRentalSigningWorkspace.vue', root), 'utf8');

test('OTR and authorization templates are available to the existing generation routes', () => {
  assert.equal(existsSync(new URL('letter-offer-to-rent.pdf', backendTemplateDir)), true);
  assert.equal(existsSync(new URL('letter-of-appointment-to-rent.pdf', backendTemplateDir)), true);
  assert.match(controller, /case "otr", "offer-to-rent", "letter-offer-to-rent"/);
  assert.match(controller, /case "authorization", "appointment", "letter-of-appointment-to-rent"/);
});

test('the generation entry points remain in their owning modules', () => {
  assert.match(signing, /generateAdminContractTemplate\('otr'/);
  assert.match(tenancy, /openTemplateGenerator\('otr'\)/);
  assert.doesNotMatch(mandate, /openAuthorizationGenerator\(item\)/);
  assert.match(mandate, /generateAdminContractTemplate\('authorization'/);
});
