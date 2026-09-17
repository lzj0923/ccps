import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

// Execute the real App lifecycle with only external dependencies replaced.
async function loadApp(native, fetchSession) {
  const vue = await readFile(new URL('../src/App.vue', import.meta.url), 'utf8');
  const source = vue.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^import .*;\r?$/gm, '').replace('export default', 'return');
  const bindings = {
    OwnerSystem: {}, AdminSystem: {}, LoginPage: {}, SignaturePage: {},
    dashboardState: {}, dashboardViewModel: {}, dashboardActions: {},
    isNativeApp: native, fetchSession, logout: async () => {},
    navigate: () => {}, resolveRoute: () => ({ mode: 'owner', portal: 'owner' }),
    routeForModule: () => '/owner', interceptRoute: () => null, userMode: () => 'owner',
    canAccessAdminModule: () => true, canAccessAdminProcessTarget: () => true,
    defaultAdminModuleId: () => 'adminDashboard',
    window: { addEventListener() {}, removeEventListener() {}, localStorage: { removeItem() {} } }
  };
  // The production helper is bound when present; old code can run before the fix.
  if (vue.includes("from './utils/startupSession'")) {
    bindings.restoreOwnerSession = (await import('../src/utils/startupSession.js')).restoreOwnerSession;
  }
  const options = new Function(...Object.keys(bindings), source)(...Object.values(bindings));
  const page = { ...options.data(), ...options.methods, syncRoute() {}, isSignaturePage: false };
  return { options, page };
}

test('原生登录页不等待会话接口，且不请求管理员会话', async () => {
  const calls = [];
  let resolve;
  const pending = new Promise(done => { resolve = done; });
  const { options, page } = await loadApp(true, portal => { calls.push(portal); return pending; });
  const mounted = options.mounted.call(page);
  try {
    assert.equal(page.authReady, true, '服务器未响应时也必须显示登录页');
    assert.deepEqual(calls, ['owner']);
  } finally { resolve(null); await mounted; }
});

test('启动会话的迟到响应不能覆盖用户刚完成的登录', async () => {
  let resolve;
  const pending = new Promise(done => { resolve = done; });
  const { options, page } = await loadApp(true, () => pending);
  const mounted = options.mounted.call(page);
  const current = { id: 2, name: '当前登录' };
  page.handleLogin(current, 'owner');
  resolve({ id: 1, name: '旧会话' });
  await mounted;
  assert.equal(page.currentUsers.owner, current);
});

test('网页端仍恢复两端会话并保持管理员与业主隔离', async () => {
  const calls = [];
  const { options, page } = await loadApp(false, async portal => { calls.push(portal); return { portal }; });
  assert.equal(page.authReady, false);
  await options.mounted.call(page);
  assert.deepEqual(calls, ['admin', 'owner']);
  assert.equal(page.authReady, true);
  assert.equal(page.currentUsers.admin.portal, 'admin');
  assert.equal(page.currentUsers.owner.portal, 'owner');
});

test('会话恢复超时或网络拒绝后返回未登录，迟到失败不会形成未处理异常', async () => {
  const { restoreOwnerSession } = await import('../src/utils/startupSession.js');
  let reject;
  const pending = new Promise((resolve, fail) => { reject = fail; });
  assert.equal(await restoreOwnerSession(() => pending, 10), null);
  reject(new Error('late network failure'));
  assert.equal(await restoreOwnerSession(async () => { throw new Error('offline'); }), null);
  const user = { id: 3 };
  assert.equal(await restoreOwnerSession(async () => user), user);
});

test('手动退出和组件销毁使启动阶段的旧响应失效', async () => {
  for (const action of ['handleLogout', 'beforeUnmount']) {
    let resolve;
    const pending = new Promise(done => { resolve = done; });
    const { options, page } = await loadApp(true, () => pending);
    const mounted = options.mounted.call(page);
    if (action === 'handleLogout') await page.handleLogout();
    else options.beforeUnmount.call(page);
    resolve({ id: 1 });
    await mounted;
    assert.equal(page.currentUsers.owner, null);
  }
});
