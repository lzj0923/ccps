import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const root = new URL('../', import.meta.url);
const detailSource = fs.readFileSync(new URL('src/components/AdminPropertyDetailWorkspace.vue', root), 'utf8');
const signingSource = fs.readFileSync(new URL('src/components/AdminRentalSigningWorkspace.vue', root), 'utf8');
const i18nSource = fs.readFileSync(new URL('src/i18n/index.js', root), 'utf8');
const serviceSource = fs.readFileSync(new URL('../backend/src/main/java/com/ccps/backend/service/AdminPropertyPhotoService.java', root), 'utf8');

const handoverCategories = [
  'handover_keys',
  'handover_living_room',
  'handover_dining_room',
  'handover_kitchen',
  'handover_master_bedroom',
  'handover_master_bathroom',
  'handover_defect_deposit',
  'handover_defect_owner_repair',
];

test('property photo category picker includes handover report sections', () => {
  for (const category of handoverCategories) assert.match(detailSource, new RegExp(`value:'${category}'`));
});

test('backend accepts the handover report photo categories', () => {
  for (const category of handoverCategories) assert.match(serviceSource, new RegExp(`"${category}"`));
});

test('property photo editor does not expose photo titles or versions', () => {
  assert.doesNotMatch(detailSource, /photoForm\.title/);
  assert.doesNotMatch(detailSource, /rentalPhotoForm\.title|photo\.title/);
  assert.doesNotMatch(detailSource, /photoTitleOptional/);
  assert.doesNotMatch(detailSource, /photo-version-bar|selectedPhotoVersion|photoVersions/);
  assert.doesNotMatch(i18nSource, /photoTitleOptional|photoVersionHint/);
  assert.match(serviceSource, /resolveTitle\(title, stored\.originalName\(\)\)/);
  assert.doesNotMatch(serviceSource, /Photo title is required/);
});

test('property photo editor hides manual order and cover controls', () => {
  assert.doesNotMatch(detailSource, /v-model\.number="photoForm\.sortOrder"/);
  assert.doesNotMatch(detailSource, /v-model="photoForm\.cover"/);
  assert.doesNotMatch(detailSource, /photo-cover-badge/);
  assert.doesNotMatch(detailSource, /legacy\.t_3912a2763f7a/);
});

test('tenancy document photo upload does not depend on a photo title', () => {
  const uploadBlock = signingSource.match(/for \(const photo of \[\.\.\.this\.leasePhotoFiles\]\) \{[\s\S]*?\n\s*\}/)?.[0] || '';
  assert.match(uploadBlock, /createAdminPropertyPhoto/);
  assert.doesNotMatch(uploadBlock, /title\s*:/);
});
