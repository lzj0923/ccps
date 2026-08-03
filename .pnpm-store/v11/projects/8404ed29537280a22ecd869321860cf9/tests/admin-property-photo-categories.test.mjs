import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const root = new URL('../', import.meta.url);
const detailSource = fs.readFileSync(new URL('src/components/AdminPropertyDetailWorkspace.vue', root), 'utf8');
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
