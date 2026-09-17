import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { parse, compileTemplate } from '@vue/compiler-sfc';
import * as phone from '../src/utils/tenantPhone.js';

const source = readFileSync(new URL('../src/components/AdminTenantDirectoryWorkspace.vue', import.meta.url), 'utf8');
const { descriptor } = parse(source);
function fixture(fail = false) {
  const calls = [];
  const write = async (...args) => { calls.push(args); if (fail) throw new Error('save failed'); return { id: 7 }; };
  const bindings = { ...phone, createAdminTenant: write, updateAdminTenant: write };
  for (const name of ['ChevronLeft', 'ChevronRight', 'PauseCircle', 'Pencil', 'PlayCircle', 'Plus', 'Search', 'Trash2', 'UserRound', 'X']) bindings[name] = {};
  const script = descriptor.script.content.replace(/^import .*;\r?$/gm, '').replace('export default', 'return');
  const component = new Function(...Object.keys(bindings), script)(...Object.values(bindings));
  const page = { ...component.data(), ...component.methods, $t: key => key, load: async () => {}, page: { showToast() {} } };
  Object.defineProperty(page, 'selectedPhoneCountry', { get: () => phone.tenantPhoneCountry(page.form.phoneCountry) });
  return { page, calls };
}

test('新增租客默认开启且无需逐项确认，单次保存提交通知偏好', async () => {
  const { page, calls } = fixture();
  page.openCreate();
  assert.equal(page.form.whatsappEnabled, true);
  assert.equal('whatsappConsentConfirmed' in page.form, false);
  page.form.fullName = '测试租客';
  page.form.phoneCountry = 'CN';
  page.form.phoneNational = '13800138000';
  await page.save();
  assert.equal(calls.length, 1);
  assert.equal(calls[0][0].whatsappEnabled, true);
  assert.equal(calls[0][0].phone, '+8613800138000');
  assert.equal(page.dialogOpen, false);
});

test('编辑已关闭通知的租客不会默认重开', async () => {
  const { page, calls } = fixture();
  page.openEdit({ tenantId: 7, fullName: '退订租客', phone: '+8613800138000', whatsappEnabled: false });
  assert.equal(page.form.whatsappEnabled, false);
  await page.save();
  assert.equal(calls.length, 1);
  assert.equal(calls[0][0], 7);
  assert.equal(calls[0][1].whatsappEnabled, null, '普通编辑保留服务端退订/自动暂停状态');
});

test('只有主动切换才提交启用或退订；避免旧表单重开并发退订', async () => {
  const { page, calls } = fixture();
  page.openEdit({ tenantId: 7, phone: '+8613800138000', whatsappEnabled: true });
  await page.save();
  assert.equal(calls[0][1].whatsappEnabled, null);
  page.form.whatsappEnabled = false;
  await page.save();
  assert.equal(calls[1][1].whatsappEnabled, false);
  page.openEdit({ tenantId: 7, phone: '+8613800138000', whatsappEnabled: false });
  page.form.whatsappEnabled = true;
  await page.save();
  assert.equal(calls[2][1].whatsappEnabled, true);
});

test('无手机号码允许保存资料，由后端暂停实际发送；非法号码仍拦截', async () => {
  const { page, calls } = fixture();
  page.openCreate();
  await page.save();
  assert.equal(calls.length, 1);
  assert.equal(calls[0][0].phone, '');
  page.form.phoneNational = 'not-a-number';
  await page.save();
  assert.equal(calls.length, 1);
  assert.ok(page.phoneError);
});

test('保存失败保留表单，不再分两次请求产生半保存状态', async () => {
  const { page, calls } = fixture(true);
  page.openCreate();
  await page.save();
  assert.equal(calls.length, 1);
  assert.equal(page.dialogOpen, true);
  assert.equal(page.saving, false);
  assert.doesNotMatch(source, /updateAdminTenantWhatsAppSubscription|whatsappConsentConfirmed/);
});

test('保留有文字标签的通知开关，统一授权只展示说明；Vue模板可编译', () => {
  assert.match(descriptor.template.content, /v-model="form.whatsappEnabled" type="checkbox"/);
  assert.match(descriptor.template.content, /<p v-if="form.whatsappEnabled" class="whatsapp-consent">/);
  const result = compileTemplate({ source: descriptor.template.content, filename: 'AdminTenantDirectoryWorkspace.vue', id: 'tenant-whatsapp-test' });
  assert.deepEqual(result.errors, []);
});
