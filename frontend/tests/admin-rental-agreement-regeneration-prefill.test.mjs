import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../src/components/AdminRentalSigningWorkspace.vue', import.meta.url), 'utf8').replace(/\r\n/g, '\n');

test('重新生成协议时重新读取系统资料，而不是清空表单', () => {
  const regenerateTask = source.match(/async regenerateTask\(task\) \{[\s\S]*?\n    \},\n    async editTask/)?.[0] || '';
  assert.ok(regenerateTask, '未找到 regenerateTask 方法');

  for (const opener of [
    'openPmaForm',
    'openRentalAppointmentForm',
    'openAuthorizationForm',
    'openTerminationLetterForm',
    'openRentalRemittanceForm',
    'openOtrForm',
    'openLeaseForm',
  ]) {
    assert.match(regenerateTask, new RegExp(`${opener}\\(false\\)`), `${opener} 应读取系统资料`);
    assert.doesNotMatch(regenerateTask, new RegExp(`${opener}\\(true\\)`), `${opener} 不应清空系统资料`);
  }
});

test('租赁委任书以系统主数据覆盖历史协议中的同名字段', () => {
  const openForm = source.match(/async openRentalAppointmentForm\(reset = false\) \{[\s\S]*?\n    \},\n    closeRentalAppointmentForm/)?.[0] || '';
  assert.ok(openForm, '未找到 openRentalAppointmentForm 方法');
  assert.match(openForm, /const savedFields = saved\?\.fields \|\| saved \|\| \{\}/);
  assert.ok(
    openForm.indexOf('...savedFields') < openForm.indexOf('landlordName: fields.landlordName'),
    '历史协议专属字段应先回填，业主等系统主数据随后覆盖',
  );
});

test('协议公共字段优先读取工作台中的业主主档', () => {
  assert.match(source, /const owner = this\.pmaOwnerProfile \|\| this\.workspace\?\.owner \|\| \{\}/);
  assert.match(source, /landlordName: owner\.fullName \|\| mandate\.ownerName \|\| property\.ownerName/);
  assert.match(source, /ownerEmail: owner\.email \|\| mandate\.ownerEmail \|\| property\.ownerEmail/);
});
