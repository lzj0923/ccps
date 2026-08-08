import assert from 'node:assert/strict';
import fs from 'node:fs';

const toolbar = fs.readFileSync('frontend/src/components/ModuleToolbar.vue', 'utf8');
const workspace = fs.readFileSync('frontend/src/components/AdminBuildingPaymentWorkspace.vue', 'utf8');
const state = fs.readFileSync('frontend/src/composables/dashboardState.js', 'utf8');

assert.match(toolbar, /showBuildingPropertyAction/);
assert.match(toolbar, /building\.addPreHandoverProperty/);
assert.match(state, /adminBuildingPropertyCreateNonce: 0/);
assert.match(workspace, /fetchAdminOwners\(\), fetchAdminPropertyProjects\(\)/);
assert.match(workspace, /createAdminOwnerProperty\(Number\(form\.ownerId\)/);
assert.match(workspace, /assetStage: 'PRE_HANDOVER'/);
assert.match(workspace, /actualHandoverDate: null, services: \[\]/);
assert.doesNotMatch(workspace, /v-model="preHandoverPropertyForm\.assetStage"/);

console.log('PASS: 建筑与房款可新增固定为未交房的房产');
