import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = relativePath => readFileSync(new URL(relativePath, import.meta.url), 'utf8');
const workspace = read('../src/components/AdminReminderWorkspace.vue');
const request = read('../../backend/src/main/java/com/ccps/backend/dto/AdminReminderRuleRequest.java');
const service = read('../../backend/src/main/java/com/ccps/backend/service/AdminReminderService.java');

test('新增提醒规则提供 WhatsApp 通知方式', () => {
  assert.match(workspace, /value: 'whatsapp', label: 'WhatsApp'/);
  assert.match(request, /in_app\|email\|line\|whatsapp/);
  assert.match(service, /CHANNELS = Set\.of\([^;]*"whatsapp"/s);
  assert.match(service, /channels\.contains\("whatsapp"\)/);
});

test('新增提醒规则默认建立租客 WhatsApp 逾期提醒', () => {
  assert.ok(workspace.includes("eventType: 'rent_due', daysBefore: 0, channels: ['whatsapp'], recipientRole: 'tenant'"));
  assert.match(workspace, /租金到期／逾期/);
  assert.match(workspace, /到期当天及已逾期账单/);
});
