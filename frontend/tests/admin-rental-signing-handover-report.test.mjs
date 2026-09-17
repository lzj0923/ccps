import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminRentalSigningWorkspace.vue', import.meta.url), 'utf8');

test('附件签约提供独立的交接报告生成入口', () => {
  assert.match(source, /key: 'handoverReport'/);
  assert.match(source, /task\.key === 'handoverReport'/);
  assert.match(source, /handoverReportFormOpen/);
  assert.match(source, /createAdminPropertyHandoverReport/);
});

test('交接报告可以选择清单、已有物件照片并批量新增照片', () => {
  assert.match(source, /v-model="handoverSelectedChecklistIds"/);
  assert.match(source, /v-model="handoverSelectedPhotoIds"/);
  assert.match(source, /type="file"[^>]*multiple[^>]*@change="setHandoverPhotoFiles"/);
  assert.match(source, /fetchAdminPropertyHandoverChecklist/);
  assert.match(source, /fetchAdminPropertyPhotos/);
  assert.match(source, /syncHandoverReportChecklist/);
});

test('生成后刷新附件并定位到本周期附件列表', () => {
  assert.match(source, /ref="generatedFilesSection"/);
  assert.match(source, /await this\.loadSigningWorkspace\(\);[\s\S]*?revealGeneratedFiles\(\)/);
  assert.match(source, /revealGeneratedFiles\(\)[\s\S]*?scrollIntoView/);
});

test('入住交接报告明确关联当前租约和入住类型', () => {
  assert.match(source, /leaseId:\s*this\.currentLease\?\.id \|\| this\.currentLease\?\.leaseId \|\| null/);
  assert.match(source, /handoverType:\s*'move_in'/);
  assert.match(source, /!property\?\.ownerUnitId \|\| !this\.currentLease \|\| this\.selectedCycleIsHistorical/);
  assert.match(source, /key: 'handoverReport'[\s\S]*?disabled: noLease \|\| readOnly/);
});

test('合同与已完成交接报告都阻止缺少清单、人员、房型或照片的生成', () => {
  assert.match(source, /leaseSelectedChecklistIds\.length/);
  assert.match(source, /leaseSelectedPhotoIds\.length \+ this\.leasePhotoFiles\.length/);
  assert.match(source, /v-model\.trim="handoverReportForm\.unitType"[^>]*required/);
  assert.match(source, /v-model\.trim="handoverReportForm\.handoverFrom"[^>]*required/);
  assert.match(source, /v-model\.trim="handoverReportForm\.handoverTo"[^>]*required/);
  assert.match(source, /handoverSelectedChecklistIds\.length/);
  assert.match(source, /handoverSelectedPhotoIds\.length \+ this\.handoverPhotoFiles\.length/);
});
